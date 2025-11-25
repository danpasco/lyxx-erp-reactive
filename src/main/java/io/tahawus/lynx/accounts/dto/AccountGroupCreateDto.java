package io.tahawus.lynx.accounts.dto;

import io.tahawus.lynx.accounts.model.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new AccountGroup.
 */
public record AccountGroupCreateDto(

        @NotNull(message = "Business ID is required")
        Long businessId,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Group number is required")
        @Min(value = 0, message = "Group number must be at least 0")
        @Max(value = 99, message = "Group number must not exceed 99")
        Integer groupNumber,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        Integer displayOrder
) {}
