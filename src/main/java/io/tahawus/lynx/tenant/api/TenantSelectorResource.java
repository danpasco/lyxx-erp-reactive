package io.tahawus.lynx.tenant.api;

import io.smallrye.common.annotation.Blocking;
import io.tahawus.lynx.tenant.model.Tenant;
import io.tahawus.lynx.tenant.model.TenantRole;
import io.tahawus.lynx.tenant.model.UserTenantAccess;
import io.tahawus.lynx.tenant.service.TenantSchemaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * API for tenant selection and self-service tenant creation.
 *
 * Does NOT require X-Tenant-Id header - operates on public schema.
 *
 * @author Dan Pasco
 */
@Path("/tenants/select")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class TenantSelectorResource {

    private static final Logger LOG = Logger.getLogger(TenantSelectorResource.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantSchemaService tenantSchemaService;

    /**
     * Get list of tenants the current user has access to.
     */
    @GET
    @PermitAll
    public Response getUserTenants() {
        String userId = jwt.getSubject();

        LOG.infof("Fetching tenants for user: %s", userId);

        List<UserTenantAccess> accessList = UserTenantAccess.findByUserId(userId);

        List<TenantInfo> tenants = accessList.stream()
                .map(access -> new TenantInfo(
                        access.tenant.id,
                        access.tenant.tenantIdentifier,
                        access.tenant.name,
                        access.tenant.description,
                        access.role,
                        access.tenant.isActive
                ))
                .toList();

        return Response.ok(tenants).build();
    }

    /**
     * Request access to a tenant.
     * Creates a pending access request for admin approval.
     */
    @POST
    @Path("/{tenantIdentifier}/request")
    @PermitAll
    @Transactional
    public Response requestTenantAccess(@PathParam("tenantIdentifier") String tenantIdentifier) {
        String userId = jwt.getSubject();

        LOG.infof("User %s requesting access to tenant: %s", userId, tenantIdentifier);

        var tenantOpt = Tenant.findByTenantIdentifier(tenantIdentifier);
        if (tenantOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Tenant not found"))
                    .build();
        }

        Tenant tenant = tenantOpt.get();

        var existing = UserTenantAccess.findByUserAndTenant(userId, tenant.id);
        if (existing.isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("You already have access to this tenant"))
                    .build();
        }

        // TODO: Create access request entity and notification flow
        return Response.ok()
                .entity(new SuccessResponse("Access request submitted. Waiting for admin approval."))
                .build();
    }

    /**
     * Create a new tenant (self-service signup).
     * Creates tenant, schema, and grants admin access to creating user.
     */
    @POST
    @Path("/create")
    @PermitAll
    @Transactional
    public Response createTenantForUser(CreateTenantRequest request) {
        String userId = jwt.getSubject();

        LOG.infof("User %s creating new tenant: %s", userId, request.tenantIdentifier());

        // Validate tenant identifier is unique
        if (Tenant.findByTenantIdentifier(request.tenantIdentifier()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Tenant identifier already exists"))
                    .build();
        }

        try {
            // Create tenant in public schema
            Tenant tenant = new Tenant();
            tenant.tenantIdentifier = request.tenantIdentifier();
            tenant.name = request.name();
            tenant.description = request.description();
            tenant.contactEmail = request.contactEmail();
            tenant.isActive = true;

            tenant.persist();

            // Create schema and run migrations
            tenantSchemaService.createTenantSchema(tenant);

            // Grant admin access to creating user
            UserTenantAccess access = new UserTenantAccess();
            access.userId = userId;
            access.tenant = tenant;
            access.role = TenantRole.ADMIN;
            access.grantedBy = userId;
            access.isActive = true;

            access.persist();

            LOG.infof("Tenant created: %s with admin: %s", tenant.tenantIdentifier, userId);

            return Response.status(Response.Status.CREATED)
                    .entity(new TenantInfo(
                            tenant.id,
                            tenant.tenantIdentifier,
                            tenant.name,
                            tenant.description,
                            TenantRole.ADMIN,
                            tenant.isActive
                    ))
                    .build();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to create tenant: %s", request.tenantIdentifier());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create tenant: " + e.getMessage()))
                    .build();
        }
    }

    // DTOs

    public record TenantInfo(
            Long id,
            String tenantIdentifier,
            String name,
            String description,
            TenantRole role,
            Boolean isActive
    ) {}

    public record CreateTenantRequest(
            String tenantIdentifier,
            String name,
            String description,
            String contactEmail
    ) {}

    public record ErrorResponse(String message) {}

    public record SuccessResponse(String message) {}
}
