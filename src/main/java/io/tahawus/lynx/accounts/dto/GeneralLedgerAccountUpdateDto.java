package io.tahawus.lynx.accounts.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload for updating an existing GeneralLedgerAccount.
 * All fields are optional - null means "no change".
 * Note: accountNumber and accountGroupId cannot be changed after creation.
 */
public record GeneralLedgerAccountUpdateDto(

        @Size(max = 50, message = "Short code must not exceed 50 characters")
        String shortCode,

        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        Boolean isActive
) {}
