package io.tahawus.lynx.tenant.model;

import io.tahawus.lynx.core.model.LynxPanacheEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Tenant entity representing a company/organization in the multi-tenant ERP system.
 * Each tenant gets its own database schema for complete data isolation.
 *
 * Stored in the master/public schema.
 */
@Entity
@Table(
        name = "tenant",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_identifier", columnNames = "tenant_identifier"),
                @UniqueConstraint(name = "uk_schema_name", columnNames = "schema_name")
        },
        indexes = {
                @Index(name = "idx_tenant_identifier", columnList = "tenant_identifier"),
                @Index(name = "idx_tenant_active", columnList = "is_active")
        }
)
public class Tenant extends LynxPanacheEntity {

    /** Unique tenant identifier (e.g., "acme-corp", "xyz-industries"). */
    @Column(name = "tenant_identifier", nullable = false, unique = true, length = 50)
    public String tenantIdentifier;

    /** Company/Organization name. */
    @Column(nullable = false, length = 200)
    public String name;

    /**
     * Database schema name for this tenant.
     * Pattern: lynx_{sanitized-tenant-identifier} or custom name.
     */
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    public String schemaName;

    /** Tenant description or business type. */
    @Column(length = 500)
    public String description;

    /** Primary contact email. */
    @Column(name = "contact_email", length = 100)
    public String contactEmail;

    /** Primary contact phone. */
    @Column(name = "contact_phone", length = 20)
    public String contactPhone;

    /** Tenant status (soft-delete flag). */
    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    /** When this tenant was created. */
    @Column(name = "created_date", nullable = false)
    public Instant createdDate;

    /** When this tenant was last modified. */
    @Column(name = "modified_date")
    public Instant modifiedDate;

    /** Optional: subscription plan or tier. */
    @Column(name = "subscription_plan", length = 50)
    public String subscriptionPlan;

    /** Optional: maximum number of users allowed. */
    @Column(name = "max_users")
    public Integer maxUsers;

    /** Notes or additional information. */
    @Column(length = 1000)
    public String notes;

    // =============================
    // =    Lifecycle Callbacks    =
    // =============================

    @PrePersist
    public void onPrePersist() {
        if (createdDate == null) {
            createdDate = Instant.now();
        }
        modifiedDate = Instant.now();

        // Generate schema name if not set
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = generateSchemaName();
        }

        // Validate schema name
        validateSchemaName();
    }

    @PreUpdate
    public void onPreUpdate() {
        modifiedDate = Instant.now();
    }

    // =============================
    // =         Methods           =
    // =============================

    private String generateSchemaName() {
        // Generate a safe PostgreSQL schema name from tenant identifier
        String safe = tenantIdentifier.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_{2,}", "_");
        return "lynx_" + safe;
    }

    private void validateSchemaName() {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalStateException("Schema name cannot be empty");
        }

        // PostgreSQL identifier max length is 63 characters
        if (schemaName.length() > 63) {
            throw new IllegalArgumentException("Schema name exceeds 63 characters: " + schemaName);
        }

        // Must start with letter or underscore
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name format: " + schemaName);
        }

        // Reserved schema names
        List<String> reserved = List.of(
                "public", "information_schema", "pg_catalog",
                "pg_toast", "pg_temp", "pg_toast_temp"
        );
        if (reserved.contains(schemaName.toLowerCase())) {
            throw new IllegalArgumentException("Schema name is reserved: " + schemaName);
        }
    }

    // =============================
    // =      Panache Queries      =
    // =============================

    public static Optional<Tenant> findByTenantIdentifier(String tenantIdentifier) {
        return find("tenantIdentifier", tenantIdentifier).firstResultOptional();
    }

    public static Optional<Tenant> findBySchemaName(String schemaName) {
        return find("schemaName", schemaName).firstResultOptional();
    }

    public static List<Tenant> findActiveTenants() {
        return list("isActive = true order by name");
    }

    public static List<Tenant> findAllTenants() {
        return listAll();
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", tenantIdentifier='" + tenantIdentifier + '\'' +
                ", name='" + name + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
