package io.tahawus.lynx.contacts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Telephone data for create/update requests.
 */
public record TelephoneCreateDto(

        @Size(max = 20, message = "Type must not exceed 20 characters")
        String type,

        @NotBlank(message = "Telephone number is required")
        @Size(max = 30, message = "Telephone number must not exceed 30 characters")
        String telephoneNumber,

        @Size(max = 100, message = "Description must not exceed 100 characters")
        String description
) {}
