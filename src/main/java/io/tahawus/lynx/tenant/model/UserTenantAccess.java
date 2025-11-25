package io.tahawus.lynx.tenant.model;

import io.tahawus.lynx.core.model.LynxPanacheEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Maps users to tenants with specific roles.
 * Stored in public schema since it's cross-tenant.
 */
@Entity
@Table(
        name = "user_tenant_access",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_tenant", columnNames = {"user_id", "tenant_id"})
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_tenant_id", columnList = "tenant_id")
        }
)
public class UserTenantAccess extends LynxPanacheEntity {

    /**
     * User ID from your identity provider (e.g., sub claim in JWT).
     * Example: "auth0|507f1f77bcf86cd799439011"
     */
    @Column(name = "user_id", nullable = false, length = 255)
    public String userId;

    /**
     * Tenant this user has access to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    /**
     * User's role within this tenant.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public TenantRole role;

    /**
     * When access was granted.
     */
    @Column(name = "granted_at", nullable = false)
    public Instant grantedAt;

    /**
     * Who granted this access (user_id).
     */
    @Column(name = "granted_by", length = 255)
    public String grantedBy;

    /**
     * Optional: Access expiration date.
     */
    @Column(name = "expires_at")
    public Instant expiresAt;

    /**
     * Is this access currently active.
     */
    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    // =============================
    // =       Lifecycle           =
    // =============================

    @PrePersist
    public void onPrePersist() {
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }

    // =============================
    // =        Queries            =
    // =============================

    public static List<UserTenantAccess> findByUserId(String userId) {
        return list("userId = ?1 AND isActive = true", userId);
    }

    public static List<UserTenantAccess> findByTenantId(Long tenantId) {
        return list("tenant.id = ?1 AND isActive = true", tenantId);
    }

    public static Optional<UserTenantAccess> findByUserAndTenant(String userId, Long tenantId) {
        return find("userId = ?1 AND tenant.id = ?2 AND isActive = true", userId, tenantId)
                .firstResultOptional();
    }

    public static boolean hasAccess(String userId, Long tenantId, TenantRole minRole) {
        return findByUserAndTenant(userId, tenantId)
                .map(access -> access.role.hasPermission(minRole))
                .orElse(false);
    }

    @Override
    public String toString() {
        return "UserTenantAccess{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", tenantId=" + (tenant != null ? tenant.id : null) +
                ", role=" + role +
                ", isActive=" + isActive +
                '}';
    }
}
