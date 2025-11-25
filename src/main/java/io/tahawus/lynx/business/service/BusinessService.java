package io.tahawus.lynx.business.service;

import io.tahawus.lynx.business.dto.BusinessCreateDto;
import io.tahawus.lynx.business.dto.BusinessDto;
import io.tahawus.lynx.business.dto.BusinessLogoDto;
import io.tahawus.lynx.business.dto.BusinessUpdateDto;
import io.tahawus.lynx.business.mapper.BusinessMapper;
import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

/**
 * Service layer for Business operations.
 *
 * Handles CRUD operations, logo management, and business rule validation.
 * Cross-module deletion checks (e.g., fiscal years, accounts) should be
 * added here rather than in the entity.
 */
@ApplicationScoped
public class BusinessService {

    // =============================
    // =      Query Operations     =
    // =============================

    public List<BusinessDto> listAll() {
        return Business.listAll().stream()
                .map(BusinessMapper::toDto)
                .toList();
    }

    public List<BusinessDto> listActive() {
        return Business.listActive().stream()
                .map(BusinessMapper::toDto)
                .toList();
    }

    public BusinessDto get(Long id) {
        Business business = findOrThrow(id);
        return BusinessMapper.toDto(business);
    }

    public BusinessLogoDto getLogo(Long id) {
        Business business = findOrThrow(id);
        return BusinessMapper.toLogoDto(business);
    }

    // =============================
    // =     Write Operations      =
    // =============================

    @Transactional
    public BusinessDto create(BusinessCreateDto dto) {
        Contact contact = resolveContact(dto.contactId());

        Business business = BusinessMapper.fromCreateDto(dto, contact);
        business.persist();

        return BusinessMapper.toDto(business);
    }

    @Transactional
    public BusinessDto update(Long id, BusinessUpdateDto dto) {
        Business business = findOrThrow(id);
        Contact contact = resolveContact(dto.contactId());

        BusinessMapper.applyUpdate(business, dto, contact);

        return BusinessMapper.toDto(business);
    }

    @Transactional
    public void delete(Long id) {
        Business business = findOrThrow(id);

        // TODO: Add cross-module deletion checks here
        // Example:
        // long fiscalYearCount = FiscalYear.count("business", business);
        // long accountCount = GeneralLedgerAccount.count("business", business);
        // if (fiscalYearCount > 0 || accountCount > 0) {
        //     throw new IllegalStateException("Cannot delete Business - it has fiscal years or accounts");
        // }

        business.delete();
    }

    // =============================
    // =     Logo Operations       =
    // =============================

    @Transactional
    public BusinessDto updateLogo(Long id, byte[] data, String contentType, String fileName) {
        Business business = findOrThrow(id);
        business.setLogo(data, contentType, fileName);
        return BusinessMapper.toDto(business);
    }

    @Transactional
    public BusinessDto removeLogo(Long id) {
        Business business = findOrThrow(id);
        business.clearLogo();
        return BusinessMapper.toDto(business);
    }

    // =============================
    // =      Helper Methods       =
    // =============================

    private Business findOrThrow(Long id) {
        return Business.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Business not found: " + id));
    }

    private Contact resolveContact(Long contactId) {
        if (contactId == null) {
            return null;
        }
        return Contact.findById(contactId);
    }
}
