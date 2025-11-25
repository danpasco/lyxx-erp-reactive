package io.tahawus.lynx.contacts.model;

/**
 * Discriminator for Contact subtypes.
 * Used in DTOs to specify whether to create a Person or Organization.
 */
public enum ContactType {
    PERSON,
    ORGANIZATION
}
