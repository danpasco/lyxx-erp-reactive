package io.tahawus.lynx.business.dto;

import io.tahawus.lynx.business.model.BasisOfAccounting;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new Business.
 * Contact must be created first; contactId is required.
 */
public record BusinessCreateDto(

        @NotNull(message = "Contact ID is required")
        Long contactId,

        @Size(max = 200, message = "Legal name must not exceed 200 characters")
        String legalName,

        @Size(max = 20, message = "Tax ID must not exceed 20 characters")
        String taxId,

        BasisOfAccounting basisOfAccounting,

        @Size(max = 50, message = "Entity type must not exceed 50 characters")
        String entityType,

        Integer fiscalYearStartMonth
) {}
