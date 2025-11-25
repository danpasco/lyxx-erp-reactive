package io.tahawus.lynx.tenant;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Request-scoped holder for the current tenant identifier.
 *
 * Extracts tenant from X-Tenant-Id header. Some endpoints (tenant management,
 * onboarding) may operate without a tenant context.
 *
 * @author Dan Pasco
 */
@RequestScoped
public class CurrentTenant {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Inject
    RoutingContext routingContext;

    private String tenantId;
    private boolean resolved = false;

    @PostConstruct
    void init() {
        String header = routingContext.request().getHeader(TENANT_HEADER);

        if (header != null && !header.isBlank()) {
            tenantId = header.trim();
        }
        resolved = true;
    }

    /**
     * Get the tenant identifier from the request header.
     *
     * @return The tenant identifier, or null if not present
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * Check if a tenant context is present.
     */
    public boolean isPresent() {
        return tenantId != null;
    }

    /**
     * Get the tenant identifier, throwing if not present.
     *
     * @return The tenant identifier
     * @throws TenantRequiredException if no tenant header
     */
    public String requireTenantId() {
        if (tenantId == null) {
            throw new TenantRequiredException(
                    "Missing or empty tenant header: " + TENANT_HEADER);
        }
        return tenantId;
    }

    /**
     * Exception thrown when tenant context is required but missing.
     */
    public static class TenantRequiredException extends RuntimeException {
        public TenantRequiredException(String message) {
            super(message);
        }
    }
}
