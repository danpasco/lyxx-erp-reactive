package io.tahawus.lynx.core.model;

import java.time.LocalDateTime;

/**
 * Contract for auditable entities.
 *
 * Provides a standard interface for audit fields. Most entities will extend
 * {@link AuditableEntity} which provides the default implementation.
 * Entities requiring custom audit behavior can implement this interface directly.
 *
 * @author Dan Pasco
 */
public interface Auditable {

    LocalDateTime getCreatedAt();

    LocalDateTime getModifiedAt();
}
