package io.tahawus.lynx.contacts.dto;

import io.tahawus.lynx.contacts.model.ContactType;

import java.util.List;

/**
 * Main Contact view DTO.
 * Returned from GET endpoints.
 */
public record ContactDto(
        Long id,
        ContactType type,
        String name,
        String abbreviation,
        String email,
        String webSite,
        List<AddressDto> addresses,
        List<TelephoneDto> telephones
) {}
