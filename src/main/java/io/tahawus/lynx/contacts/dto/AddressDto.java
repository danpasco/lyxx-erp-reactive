package io.tahawus.lynx.contacts.dto;

import io.tahawus.lynx.contacts.model.AddressType;

/**
 * Address data for responses.
 */
public record AddressDto(
        Long id,
        AddressType type,
        String street,
        String additionalStreet,
        String city,
        String state,
        String postalCode,
        String country
) {}
