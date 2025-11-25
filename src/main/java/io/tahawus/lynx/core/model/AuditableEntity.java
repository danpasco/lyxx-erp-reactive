package io.tahawus.lynx.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

/**
 * Base entity with standard audit fields.
 *
 * Provides automatic tracking of:
 * - createdAt: timestamp when entity was first persisted
 * - createdBy: user who created the entity
 * - modifiedAt: timestamp of last update
 * - modifiedBy: user who last modified the entity
 *
 * Implements {@link Auditable} interface. Entities needing custom audit
 * behavior can implement Auditable directly instead of extending this class.
 *
 * Note: createdBy/modifiedBy must be set by the service layer since
 * JPA lifecycle callbacks don't have access to security context.
 *
 * @author Dan Pasco
 */
@MappedSuperclass
public abstract class AuditableEntity extends LynxPanacheEntity implements Auditable {

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "created_by", length = 100, updatable = false)
    public String createdBy;

    @Column(name = "modified_at")
    public LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 100)
    public String modifiedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }
}
