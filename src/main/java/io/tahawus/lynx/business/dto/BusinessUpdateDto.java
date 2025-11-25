package io.tahawus.lynx.business.dto;

import io.tahawus.lynx.business.model.BasisOfAccounting;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating an existing Business.
 * All fields are optional - null means "no change".
 * To update the display name, update the associated Contact.
 */
public record BusinessUpdateDto(

        @Size(max = 200, message = "Legal name must not exceed 200 characters")
        String legalName,

        @Size(max = 20, message = "Tax ID must not exceed 20 characters")
        String taxId,

        BasisOfAccounting basisOfAccounting,

        @Size(max = 50, message = "Entity type must not exceed 50 characters")
        String entityType,

        Integer fiscalYearStartMonth,

        Long contactId,

        Boolean isActive
) {}
