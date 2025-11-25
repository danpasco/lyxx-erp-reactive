package io.tahawus.lynx.tenant.service;

import io.agroal.api.AgroalDataSource;
import io.tahawus.lynx.tenant.model.Tenant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service for managing tenant database schemas.
 *
 * Handles:
 * - Schema creation
 * - Flyway migrations per schema
 * - Schema deletion (hard delete)
 *
 * @author Dan Pasco
 */
@ApplicationScoped
public class TenantSchemaService {

    private static final Logger LOG = Logger.getLogger(TenantSchemaService.class);

    @Inject
    AgroalDataSource dataSource;

    /**
     * Create a new schema for a tenant and run migrations.
     *
     * @param tenant The tenant entity (must have schemaName set)
     */
    public void createTenantSchema(Tenant tenant) {
        String schemaName = tenant.schemaName;

        LOG.infof("Creating schema for tenant: %s -> %s", tenant.tenantIdentifier, schemaName);

        // Create the schema
        createSchema(schemaName);

        // Run Flyway migrations
        migrateSchema(schemaName);

        LOG.infof("Schema created and migrated: %s", schemaName);
    }

    /**
     * Create a PostgreSQL schema.
     */
    private void createSchema(String schemaName) {
        // Validate schema name to prevent SQL injection
        validateSchemaName(schemaName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // CREATE SCHEMA IF NOT EXISTS is safe and idempotent
            String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            stmt.execute(sql);

            LOG.infof("Schema created: %s", schemaName);

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to create schema: %s", schemaName);
            throw new TenantSchemaException("Failed to create schema: " + schemaName, e);
        }
    }

    /**
     * Run Flyway migrations on a specific schema.
     */
    private void migrateSchema(String schemaName) {
        LOG.infof("Running migrations on schema: %s", schemaName);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .load();

            flyway.migrate();

            LOG.infof("Migrations complete for schema: %s", schemaName);

        } catch (Exception e) {
            LOG.errorf(e, "Migration failed for schema: %s", schemaName);
            throw new TenantSchemaException("Migration failed for schema: " + schemaName, e);
        }
    }

    /**
     * Delete a tenant's schema (DANGEROUS - use with caution).
     *
     * @param tenant The tenant whose schema should be deleted
     */
    public void deleteTenantSchema(Tenant tenant) {
        String schemaName = tenant.schemaName;

        LOG.warnf("DELETING schema: %s (tenant: %s)", schemaName, tenant.tenantIdentifier);

        validateSchemaName(schemaName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // CASCADE drops all objects in the schema
            String sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
            stmt.execute(sql);

            LOG.warnf("Schema deleted: %s", schemaName);

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to delete schema: %s", schemaName);
            throw new TenantSchemaException("Failed to delete schema: " + schemaName, e);
        }
    }

    /**
     * Check if a schema exists.
     */
    public boolean schemaExists(String schemaName) {
        validateSchemaName(schemaName);

        String sql = """
            SELECT EXISTS (
                SELECT 1 FROM information_schema.schemata 
                WHERE schema_name = ?
            )
            """;

        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            LOG.errorf(e, "Failed to check schema existence: %s", schemaName);
            throw new TenantSchemaException("Failed to check schema: " + schemaName, e);
        }
    }

    /**
     * Re-run migrations on an existing schema (for upgrades).
     */
    public void upgradeSchema(Tenant tenant) {
        String schemaName = tenant.schemaName;

        LOG.infof("Upgrading schema: %s", schemaName);

        if (!schemaExists(schemaName)) {
            throw new TenantSchemaException("Schema does not exist: " + schemaName);
        }

        migrateSchema(schemaName);
    }

    /**
     * Validate schema name to prevent SQL injection.
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be empty");
        }

        // Must match PostgreSQL identifier rules
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        // Max 63 characters
        if (schemaName.length() > 63) {
            throw new IllegalArgumentException("Schema name too long: " + schemaName);
        }

        // Block reserved names
        String lower = schemaName.toLowerCase();
        if (lower.equals("public") ||
            lower.startsWith("pg_") ||
            lower.equals("information_schema")) {
            throw new IllegalArgumentException("Reserved schema name: " + schemaName);
        }
    }

    /**
     * Exception for schema operations.
     */
    public static class TenantSchemaException extends RuntimeException {
        public TenantSchemaException(String message) {
            super(message);
        }

        public TenantSchemaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
