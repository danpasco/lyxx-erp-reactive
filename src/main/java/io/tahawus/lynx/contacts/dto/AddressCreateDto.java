package io.tahawus.lynx.contacts.dto;

import io.tahawus.lynx.contacts.model.AddressType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Address data for create/update requests.
 */
public record AddressCreateDto(

        @NotNull(message = "Address type is required")
        AddressType type,

        @Size(max = 60, message = "Street must not exceed 60 characters")
        String street,

        @Size(max = 60, message = "Additional street must not exceed 60 characters")
        String additionalStreet,

        @Size(max = 50, message = "City must not exceed 50 characters")
        String city,

        @Size(max = 50, message = "State must not exceed 50 characters")
        String state,

        @Size(max = 30, message = "Postal code must not exceed 30 characters")
        String postalCode,

        @Size(max = 50, message = "Country must not exceed 50 characters")
        String country
) {}
