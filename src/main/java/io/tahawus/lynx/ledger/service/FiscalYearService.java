package io.tahawus.lynx.ledger.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.ledger.dto.FiscalYearCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalYearDto;
import io.tahawus.lynx.ledger.mapper.FiscalYearMapper;
import io.tahawus.lynx.ledger.model.FiscalPeriod;
import io.tahawus.lynx.ledger.model.FiscalPeriodStatus;
import io.tahawus.lynx.ledger.model.FiscalYear;
import io.tahawus.lynx.ledger.model.FiscalYearStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Service for FiscalYear business logic.
 */
@ApplicationScoped
public class FiscalYearService {

    public List<FiscalYearDto> listByBusiness(Long businessId) {
        Business business = requireBusiness(businessId);
        return FiscalYearMapper.toDtoList(FiscalYear.listByBusiness(business));
    }

    public Optional<FiscalYearDto> get(Long id) {
        return FiscalYear.<FiscalYear>findByIdOptional(id)
                .map(FiscalYearMapper::toDto);
    }

    public FiscalYearDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("FiscalYear not found: " + id));
    }

    @Transactional
    public FiscalYearDto create(FiscalYearCreateDto dto) {
        Business business = requireBusiness(dto.businessId());

        // Check for duplicate year
        if (FiscalYear.findByBusinessAndYear(business, dto.year()).isPresent()) {
            throw new IllegalArgumentException("Fiscal year " + dto.year() + " already exists");
        }

        FiscalYear fiscalYear = FiscalYearMapper.fromCreateDto(dto, business);
        fiscalYear.persist();
        return FiscalYearMapper.toDto(fiscalYear);
    }

    /**
     * Begin closing process for a fiscal year.
     * Changes status from OPEN to CLOSING.
     */
    @Transactional
    public FiscalYearDto beginClosing(Long id) {
        FiscalYear fiscalYear = FiscalYear.findById(id);
        if (fiscalYear == null) {
            throw new NotFoundException("FiscalYear not found: " + id);
        }

        if (fiscalYear.status != FiscalYearStatus.OPEN) {
            throw new IllegalStateException("Fiscal year must be OPEN to begin closing");
        }

        // Check prior year is closed
        Optional<FiscalYear> priorYear = FiscalYear.findByBusinessAndYear(fiscalYear.business, fiscalYear.year - 1);
        if (priorYear.isPresent() && priorYear.get().status != FiscalYearStatus.CLOSED) {
            throw new IllegalStateException("Prior fiscal year " + (fiscalYear.year - 1) + " must be CLOSED first");
        }

        // Check all periods are closed
        List<FiscalPeriod> openPeriods = FiscalPeriod.listOpen(fiscalYear);
        if (!openPeriods.isEmpty()) {
            throw new IllegalStateException("All fiscal periods must be CLOSED before closing the year. " +
                    openPeriods.size() + " period(s) still open.");
        }

        fiscalYear.status = FiscalYearStatus.CLOSING;
        return FiscalYearMapper.toDto(fiscalYear);
    }

    /**
     * Complete closing process for a fiscal year.
     * Changes status from CLOSING to CLOSED.
     * Opening balances should be carried forward before calling this.
     */
    @Transactional
    public FiscalYearDto completeClosing(Long id) {
        FiscalYear fiscalYear = FiscalYear.findById(id);
        if (fiscalYear == null) {
            throw new NotFoundException("FiscalYear not found: " + id);
        }

        if (fiscalYear.status != FiscalYearStatus.CLOSING) {
            throw new IllegalStateException("Fiscal year must be CLOSING to complete closing");
        }

        fiscalYear.status = FiscalYearStatus.CLOSED;
        return FiscalYearMapper.toDto(fiscalYear);
    }

    private Business requireBusiness(Long businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId is required");
        }
        Business business = Business.findById(businessId);
        if (business == null) {
            throw new NotFoundException("Business not found: " + businessId);
        }
        return business;
    }
}
