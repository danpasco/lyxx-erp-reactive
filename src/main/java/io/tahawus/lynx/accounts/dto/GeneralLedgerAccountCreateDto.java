package io.tahawus.lynx.accounts.dto;

import io.tahawus.lynx.accounts.model.SubsidiaryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new GeneralLedgerAccount.
 */
public record GeneralLedgerAccountCreateDto(

        @NotNull(message = "Business ID is required")
        Long businessId,

        @NotNull(message = "Account group ID is required")
        Long accountGroupId,

        @NotNull(message = "Account number is required")
        @Min(value = 0, message = "Account number must be at least 0")
        @Max(value = 9999, message = "Account number must not exceed 9999")
        Integer accountNumber,

        @NotBlank(message = "Short code is required")
        @Size(max = 50, message = "Short code must not exceed 50 characters")
        String shortCode,

        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        SubsidiaryType subsidiaryType
) {}
