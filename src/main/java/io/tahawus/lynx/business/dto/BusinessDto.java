package io.tahawus.lynx.business.dto;

import io.tahawus.lynx.business.model.BasisOfAccounting;

/**
 * Main Business view DTO.
 * Returned from GET endpoints.
 */
public record BusinessDto(
        Long id,
        String legalName,
        String taxId,
        BasisOfAccounting basisOfAccounting,
        String entityType,
        Integer fiscalYearStartMonth,
        Long contactId,
        String contactName,
        Boolean isActive,
        boolean hasLogo
) {

    /**
     * Display name for the business.
     * Derived from the associated Contact name.
     */
    public String displayName() {
        return contactName != null ? contactName : "(Unnamed business)";
    }
}
