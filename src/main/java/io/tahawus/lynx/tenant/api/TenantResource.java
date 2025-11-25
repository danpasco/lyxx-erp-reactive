package io.tahawus.lynx.tenant.api;

import io.smallrye.common.annotation.Blocking;
import io.tahawus.lynx.tenant.model.Tenant;
import io.tahawus.lynx.tenant.service.TenantSchemaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * REST API for managing tenants (admin operations).
 *
 * Operates on the public schema - does NOT require X-Tenant-Id header.
 * All operations should be protected by appropriate admin security.
 *
 * @author Dan Pasco
 */
@Path("tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class TenantResource {

    private static final Logger LOG = Logger.getLogger(TenantResource.class);

    @ConfigProperty(name = "lynx.admin.token", defaultValue = "lynx-admin-secret-2024")
    String adminToken;

    @Inject
    TenantSchemaService tenantSchemaService;

    /**
     * List all tenants.
     */
    @GET
    public List<Tenant> listTenants(@QueryParam("active") Boolean active) {
        LOG.debug("Listing tenants");

        if (Boolean.TRUE.equals(active)) {
            return Tenant.findActiveTenants();
        }
        return Tenant.findAllTenants();
    }

    /**
     * Get a specific tenant by ID.
     */
    @GET
    @Path("/{id}")
    public Tenant getTenant(@PathParam("id") Long id) {
        LOG.debugf("Getting tenant: %d", id);
        return Tenant.<Tenant>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
    }

    /**
     * Get a tenant by identifier.
     */
    @GET
    @Path("/identifier/{tenantIdentifier}")
    public Tenant getTenantByIdentifier(@PathParam("tenantIdentifier") String tenantIdentifier) {
        LOG.debugf("Getting tenant by identifier: %s", tenantIdentifier);
        return Tenant.findByTenantIdentifier(tenantIdentifier)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantIdentifier));
    }

    /**
     * Admin-only: Provision a new tenant.
     * Creates tenant record and schema.
     */
    @POST
    @Path("/provision")
    @Transactional
    public Response provisionTenant(
            @HeaderParam("X-Admin-Token") String token,
            ProvisionTenantRequest request) {

        if (!adminToken.equals(token)) {
            LOG.warn("Unauthorized tenant provisioning attempt");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Invalid admin token"))
                    .build();
        }

        LOG.infof("Admin provisioning tenant: %s", request.tenantIdentifier());

        if (request.tenantIdentifier() == null || request.tenantIdentifier().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tenant identifier is required"))
                    .build();
        }

        if (request.tenantName() == null || request.tenantName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tenant name is required"))
                    .build();
        }

        if (Tenant.findByTenantIdentifier(request.tenantIdentifier()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Tenant identifier already exists"))
                    .build();
        }

        try {
            Tenant tenant = new Tenant();
            tenant.tenantIdentifier = request.tenantIdentifier();
            tenant.name = request.tenantName();
            tenant.description = request.description();
            tenant.contactEmail = request.adminEmail();
            tenant.contactPhone = request.phone();

            tenant.persist();
            LOG.infof("Tenant created with ID: %d", tenant.id);

            tenantSchemaService.createTenantSchema(tenant);
            LOG.infof("Schema created: %s", tenant.schemaName);

            return Response.ok(new ProvisionResponse(
                    tenant.id,
                    tenant.tenantIdentifier,
                    tenant.schemaName,
                    "/setup?tenant=" + tenant.tenantIdentifier,
                    "Tenant provisioned successfully"
            )).build();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to provision tenant: %s", request.tenantIdentifier());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Tenant provisioning failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update an existing tenant.
     * Tenant identifier and schema name are immutable.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateTenant(@PathParam("id") Long id, UpdateTenantRequest request) {
        LOG.infof("Updating tenant: %d", id);

        Tenant tenant = Tenant.<Tenant>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));

        if (request.name() != null) tenant.name = request.name();
        if (request.description() != null) tenant.description = request.description();
        if (request.contactEmail() != null) tenant.contactEmail = request.contactEmail();
        if (request.contactPhone() != null) tenant.contactPhone = request.contactPhone();
        if (request.subscriptionPlan() != null) tenant.subscriptionPlan = request.subscriptionPlan();
        if (request.maxUsers() != null) tenant.maxUsers = request.maxUsers();
        if (request.notes() != null) tenant.notes = request.notes();

        LOG.infof("Tenant updated: %d", id);

        return Response.ok(tenant).build();
    }

    /**
     * Deactivate (soft delete) or hard delete a tenant.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteTenant(
            @PathParam("id") Long id,
            @QueryParam("hard") Boolean hardDelete,
            @HeaderParam("X-Admin-Token") String token) {

        LOG.infof("Delete tenant request: %d (hard=%b)", id, hardDelete);

        Tenant tenant = Tenant.<Tenant>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));

        if (Boolean.TRUE.equals(hardDelete)) {
            // Hard delete requires admin token
            if (!adminToken.equals(token)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Admin token required for hard delete"))
                        .build();
            }

            LOG.warnf("HARD DELETE requested for tenant: %s", tenant.tenantIdentifier);

            try {
                tenantSchemaService.deleteTenantSchema(tenant);
                tenant.delete();
                return Response.noContent().build();

            } catch (Exception e) {
                LOG.errorf(e, "Failed to hard delete tenant: %s", tenant.tenantIdentifier);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("Failed to delete tenant: " + e.getMessage()))
                        .build();
            }
        } else {
            // Soft delete
            tenant.isActive = false;
            LOG.infof("Tenant deactivated: %d", id);
            return Response.ok(tenant).build();
        }
    }

    /**
     * Reactivate a deactivated tenant.
     */
    @POST
    @Path("/{id}/activate")
    @Transactional
    public Response activateTenant(@PathParam("id") Long id) {
        LOG.infof("Activating tenant: %d", id);

        Tenant tenant = Tenant.<Tenant>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));

        tenant.isActive = true;
        LOG.infof("Tenant activated: %d", id);

        return Response.ok(tenant).build();
    }

    /**
     * Check if a tenant identifier is available.
     */
    @GET
    @Path("/check/{tenantIdentifier}")
    public Response checkAvailability(@PathParam("tenantIdentifier") String tenantIdentifier) {
        boolean available = Tenant.findByTenantIdentifier(tenantIdentifier).isEmpty();
        return Response.ok(new AvailabilityResponse(tenantIdentifier, available)).build();
    }

    /**
     * Upgrade a tenant's schema (run pending migrations).
     */
    @POST
    @Path("/{id}/upgrade")
    public Response upgradeSchema(
            @PathParam("id") Long id,
            @HeaderParam("X-Admin-Token") String token) {

        if (!adminToken.equals(token)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Admin token required"))
                    .build();
        }

        Tenant tenant = Tenant.<Tenant>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));

        try {
            tenantSchemaService.upgradeSchema(tenant);
            return Response.ok(new SuccessResponse("Schema upgraded successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Upgrade failed: " + e.getMessage()))
                    .build();
        }
    }

    // DTOs

    public record ProvisionTenantRequest(
            String tenantIdentifier,
            String tenantName,
            String description,
            String adminEmail,
            String phone
    ) {}

    public record UpdateTenantRequest(
            String name,
            String description,
            String contactEmail,
            String contactPhone,
            String subscriptionPlan,
            Integer maxUsers,
            String notes
    ) {}

    public record ProvisionResponse(
            Long tenantId,
            String tenantIdentifier,
            String schemaName,
            String setupUrl,
            String message
    ) {}

    public record ErrorResponse(String message) {}
    public record SuccessResponse(String message) {}
    public record AvailabilityResponse(String tenantIdentifier, boolean available) {}
}
