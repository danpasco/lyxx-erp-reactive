package io.tahawus.lynx.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Custom base entity that uses IDENTITY generation strategy
 * to work with PostgreSQL BIGSERIAL columns.
 *
 * This replaces PanacheEntity which uses SEQUENCE by default.
 */
@MappedSuperclass
public class LynxPanacheEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
}
