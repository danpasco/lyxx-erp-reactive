package io.tahawus.lynx.tenant;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.tahawus.lynx.tenant.model.Tenant;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Hibernate tenant resolver for schema-per-tenant multitenancy.
 *
 * Resolves the tenant identifier from the request header to
 * the actual database schema name.
 *
 * @author Dan Pasco
 */
@RequestScoped
public class HibernateTenantResolver implements TenantResolver {

    private static final Logger LOG = Logger.getLogger(HibernateTenantResolver.class);

    @Inject
    CurrentTenant currentTenant;

    @Override
    public String getDefaultTenantId() {
        return "public";
    }

    @Override
    public String resolveTenantId() {
        try {
            String tenantIdentifier = currentTenant.tenantId();

            if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
                LOG.debug("No tenant identifier, using default schema");
                return getDefaultTenantId();
            }

            // Look up the tenant to get the actual schema name
            return Tenant.findByTenantIdentifier(tenantIdentifier)
                    .map(tenant -> {
                        LOG.debugf("Resolved tenant %s to schema %s",
                                tenantIdentifier, tenant.schemaName);
                        return tenant.schemaName;
                    })
                    .orElseGet(() -> {
                        LOG.warnf("Tenant not found: %s, using default schema", tenantIdentifier);
                        return getDefaultTenantId();
                    });

        } catch (Exception e) {
            LOG.debugf("Error resolving tenant: %s", e.getMessage());
            return getDefaultTenantId();
        }
    }
}
