package io.tahawus.lynx.tenant.api;

import io.smallrye.common.annotation.Blocking;
import io.tahawus.lynx.tenant.dto.OnboardingRequest;
import io.tahawus.lynx.tenant.dto.OnboardingResponse;
import io.tahawus.lynx.tenant.model.Tenant;
import io.tahawus.lynx.tenant.service.OnboardingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST endpoint for tenant onboarding (Phase 2).
 *
 * Phase 1 (TenantResource.provision): Creates tenant + schema
 * Phase 2 (this resource): Sets up business content within tenant schema
 *
 * Does NOT require X-Tenant-Id header for the lookup, but the
 * OnboardingService executes within the tenant's schema context.
 *
 * @author Dan Pasco
 */
@Path("onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Blocking
public class OnboardingResource {

    private static final Logger LOG = Logger.getLogger(OnboardingResource.class);

    @Inject
    OnboardingService onboardingService;

    /**
     * Complete business setup (Phase 2).
     *
     * Requires tenant to already exist (created via Phase 1).
     * Creates Business, FiscalYear, and default accounts.
     */
    @POST
    @Path("/setup")
    @Transactional
    public Response setupBusiness(@Valid OnboardingRequest request) {
        LOG.infof("Business setup request for tenant: %s", request.tenantIdentifier());

        // Look up tenant in public schema
        Tenant tenant = Tenant.findByTenantIdentifier(request.tenantIdentifier())
                .orElseThrow(() -> new NotFoundException(
                        "Tenant not found: " + request.tenantIdentifier()));

        if (!tenant.isActive) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tenant is not active"))
                    .build();
        }

        try {
            OnboardingResponse response = onboardingService.createTenantContent(tenant, request);
            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Onboarding failed for tenant: %s", request.tenantIdentifier());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Onboarding failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check onboarding status for a tenant.
     */
    @GET
    @Path("/status/{tenantIdentifier}")
    public Response getOnboardingStatus(@PathParam("tenantIdentifier") String tenantIdentifier) {
        Tenant tenant = Tenant.findByTenantIdentifier(tenantIdentifier)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantIdentifier));

        // TODO: Check if Business exists in tenant schema
        // For now, just return tenant status
        return Response.ok(new OnboardingStatus(
                tenant.tenantIdentifier,
                tenant.isActive,
                false // needsSetup - would check if Business exists
        )).build();
    }

    public record ErrorResponse(String message) {}

    public record OnboardingStatus(
            String tenantIdentifier,
            boolean tenantActive,
            boolean needsSetup
    ) {}
}
