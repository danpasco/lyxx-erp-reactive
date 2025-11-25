package io.tahawus.lynx.contacts.dto;

/**
 * Telephone data for responses.
 */
public record TelephoneDto(
        Long id,
        String type,
        String telephoneNumber,
        String description
) {}
