package io.tahawus.lynx.ledger.service;

import io.tahawus.lynx.ledger.dto.FiscalPeriodCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalPeriodDto;
import io.tahawus.lynx.ledger.mapper.FiscalPeriodMapper;
import io.tahawus.lynx.ledger.model.FiscalPeriod;
import io.tahawus.lynx.ledger.model.FiscalPeriodStatus;
import io.tahawus.lynx.ledger.model.FiscalYear;
import io.tahawus.lynx.ledger.model.FiscalYearStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for FiscalPeriod business logic.
 */
@ApplicationScoped
public class FiscalPeriodService {

    public List<FiscalPeriodDto> listByFiscalYear(Long fiscalYearId) {
        FiscalYear fiscalYear = requireFiscalYear(fiscalYearId);
        return FiscalPeriodMapper.toDtoList(FiscalPeriod.listByFiscalYear(fiscalYear));
    }

    public Optional<FiscalPeriodDto> get(Long id) {
        return FiscalPeriod.<FiscalPeriod>findByIdOptional(id)
                .map(FiscalPeriodMapper::toDto);
    }

    public FiscalPeriodDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("FiscalPeriod not found: " + id));
    }

    @Transactional
    public FiscalPeriodDto create(FiscalPeriodCreateDto dto) {
        FiscalYear fiscalYear = requireFiscalYear(dto.fiscalYearId());

        // Check for duplicate period number
        if (FiscalPeriod.findByFiscalYearAndNumber(fiscalYear, dto.periodNumber()).isPresent()) {
            throw new IllegalArgumentException("Period " + dto.periodNumber() + " already exists in fiscal year " + fiscalYear.year);
        }

        FiscalPeriod period = FiscalPeriodMapper.fromCreateDto(dto, fiscalYear);
        period.persist();
        return FiscalPeriodMapper.toDto(period);
    }

    /**
     * Create standard monthly periods (1-12) for a fiscal year.
     */
    @Transactional
    public List<FiscalPeriodDto> createMonthlyPeriods(Long fiscalYearId) {
        FiscalYear fiscalYear = requireFiscalYear(fiscalYearId);

        // Check no periods exist
        if (!FiscalPeriod.listByFiscalYear(fiscalYear).isEmpty()) {
            throw new IllegalStateException("Fiscal year already has periods");
        }

        List<FiscalPeriod> periods = new ArrayList<>();
        LocalDate periodStart = fiscalYear.startDate;

        for (int i = 1; i <= 12 && !periodStart.isAfter(fiscalYear.endDate); i++) {
            YearMonth ym = YearMonth.from(periodStart);
            LocalDate periodEnd = ym.atEndOfMonth();

            // Don't extend past fiscal year end
            if (periodEnd.isAfter(fiscalYear.endDate)) {
                periodEnd = fiscalYear.endDate;
            }

            FiscalPeriod period = new FiscalPeriod();
            period.fiscalYear = fiscalYear;
            period.periodNumber = i;
            period.startDate = periodStart;
            period.endDate = periodEnd;
            period.persist();
            periods.add(period);

            periodStart = periodEnd.plusDays(1);
        }

        return FiscalPeriodMapper.toDtoList(periods);
    }

    /**
     * Close a fiscal period (soft close).
     */
    @Transactional
    public FiscalPeriodDto close(Long id) {
        FiscalPeriod period = FiscalPeriod.findById(id);
        if (period == null) {
            throw new NotFoundException("FiscalPeriod not found: " + id);
        }

        if (period.status == FiscalPeriodStatus.CLOSED) {
            throw new IllegalStateException("Period is already closed");
        }

        period.status = FiscalPeriodStatus.CLOSED;
        return FiscalPeriodMapper.toDto(period);
    }

    /**
     * Reopen a fiscal period (only if fiscal year is still OPEN).
     */
    @Transactional
    public FiscalPeriodDto reopen(Long id) {
        FiscalPeriod period = FiscalPeriod.findById(id);
        if (period == null) {
            throw new NotFoundException("FiscalPeriod not found: " + id);
        }

        if (period.status == FiscalPeriodStatus.OPEN) {
            throw new IllegalStateException("Period is already open");
        }

        if (period.fiscalYear.status != FiscalYearStatus.OPEN) {
            throw new IllegalStateException("Cannot reopen period - fiscal year is " + period.fiscalYear.status);
        }

        period.status = FiscalPeriodStatus.OPEN;
        return FiscalPeriodMapper.toDto(period);
    }

    private FiscalYear requireFiscalYear(Long fiscalYearId) {
        if (fiscalYearId == null) {
            throw new IllegalArgumentException("fiscalYearId is required");
        }
        FiscalYear fiscalYear = FiscalYear.findById(fiscalYearId);
        if (fiscalYear == null) {
            throw new NotFoundException("FiscalYear not found: " + fiscalYearId);
        }
        return fiscalYear;
    }
}
