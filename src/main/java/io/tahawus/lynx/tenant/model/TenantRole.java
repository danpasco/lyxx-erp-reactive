package io.tahawus.lynx.tenant.model;

/**
 * Hierarchical roles for tenant access.
 * Higher roles have all permissions of lower roles.
 */
public enum TenantRole {
    /**
     * Read-only access to tenant data
     */
    VIEWER(1),

    /**
     * Can create and edit data, but not manage users or settings
     */
    USER(2),

    /**
     * Can manage users, but not billing or critical settings
     */
    MANAGER(3),

    /**
     * Full control over tenant including billing and deletion
     */
    ADMIN(4),

    /**
     * System-level access (for support/maintenance)
     */
    SUPER_ADMIN(5);

    private final int level;

    TenantRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this role has at least the permission level of the required role
     */
    public boolean hasPermission(TenantRole requiredRole) {
        return this.level >= requiredRole.level;
    }

    /**
     * Get the minimum role required for administrative actions
     */
    public static TenantRole getMinimumAdminRole() {
        return ADMIN;
    }

    /**
     * Get the minimum role required for write operations
     */
    public static TenantRole getMinimumWriteRole() {
        return USER;
    }

    /**
     * Get the minimum role required for read operations
     */
    public static TenantRole getMinimumReadRole() {
        return VIEWER;
    }
}
