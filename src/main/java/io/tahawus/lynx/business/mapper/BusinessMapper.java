package io.tahawus.lynx.business.mapper;

import io.tahawus.lynx.business.dto.BusinessCreateDto;
import io.tahawus.lynx.business.dto.BusinessDto;
import io.tahawus.lynx.business.dto.BusinessLogoDto;
import io.tahawus.lynx.business.dto.BusinessUpdateDto;
import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;

/**
 * Maps between Business entity and DTOs.
 */
public final class BusinessMapper {

    private BusinessMapper() {
    }

    // =============================
    // =     Entity -> DTO         =
    // =============================

    public static BusinessDto toDto(Business business) {
        if (business == null) {
            return null;
        }

        Contact contact = business.contact;

        return new BusinessDto(
                business.id,
                business.legalName,
                business.taxId,
                business.basisOfAccounting,
                business.entityType,
                business.fiscalYearStartMonth,
                contact != null ? contact.id : null,
                contact != null ? contact.name : null,
                business.isActive,
                business.hasLogo()
        );
    }

    public static BusinessLogoDto toLogoDto(Business business) {
        if (business == null || !business.hasLogo()) {
            return null;
        }

        return new BusinessLogoDto(
                business.logoContentType,
                business.logoFileName,
                business.logoData
        );
    }

    // =============================
    // =     DTO -> Entity         =
    // =============================

    /**
     * Creates a new Business entity from a create DTO.
     * Contact must be resolved by the caller.
     */
    public static Business fromCreateDto(BusinessCreateDto dto, Contact contact) {
        Business business = new Business();
        business.contact = contact;
        business.legalName = dto.legalName();
        business.taxId = dto.taxId();
        business.basisOfAccounting = dto.basisOfAccounting();
        business.entityType = dto.entityType();
        business.fiscalYearStartMonth = dto.fiscalYearStartMonth();
        return business;
    }

    /**
     * Applies update DTO to existing Business entity.
     * Only non-null fields are updated.
     * Contact must be resolved by the caller if contactId is provided.
     */
    public static void applyUpdate(Business business, BusinessUpdateDto dto, Contact contact) {
        if (dto == null) {
            return;
        }

        if (dto.legalName() != null) {
            business.legalName = dto.legalName();
        }
        if (dto.taxId() != null) {
            business.taxId = dto.taxId();
        }
        if (dto.basisOfAccounting() != null) {
            business.basisOfAccounting = dto.basisOfAccounting();
        }
        if (dto.entityType() != null) {
            business.entityType = dto.entityType();
        }
        if (dto.fiscalYearStartMonth() != null) {
            business.fiscalYearStartMonth = dto.fiscalYearStartMonth();
        }
        if (dto.contactId() != null) {
            business.contact = contact;
        }
        if (dto.isActive() != null) {
            business.isActive = dto.isActive();
        }
    }
}
