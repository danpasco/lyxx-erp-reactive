package io.tahawus.lynx.contacts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for updating an existing Contact.
 * All fields are optional - null means "no change".
 * Note: type cannot be changed after creation (Person stays Person).
 *
 * For addresses and telephones, the entire collection is replaced
 * if provided (not null). To keep existing items, include them in the list.
 */
public record ContactUpdateDto(

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
