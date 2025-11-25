package io.tahawus.lynx.contacts.dto;

import io.tahawus.lynx.contacts.model.ContactType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating a new Contact.
 */
public record ContactCreateDto(

        @NotNull(message = "Contact type is required")
        ContactType type,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 60, message = "Abbreviation must not exceed 60 characters")
        String abbreviation,

        @Size(max = 75, message = "Email must not exceed 75 characters")
        String email,

        @Size(max = 200, message = "Website must not exceed 200 characters")
        String webSite,

        @Valid
        List<AddressCreateDto> addresses,

        @Valid
        List<TelephoneCreateDto> telephones
) {}
