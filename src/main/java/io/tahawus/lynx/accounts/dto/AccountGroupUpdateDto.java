package io.tahawus.lynx.accounts.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload for updating an existing AccountGroup.
 * All fields are optional - null means "no change".
 * Note: accountType and groupNumber cannot be changed after creation.
 */
public record AccountGroupUpdateDto(

        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        Integer displayOrder,

        Boolean isActive
) {}
