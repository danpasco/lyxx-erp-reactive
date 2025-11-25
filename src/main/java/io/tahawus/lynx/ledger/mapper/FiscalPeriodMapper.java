package io.tahawus.lynx.ledger.mapper;

import io.tahawus.lynx.ledger.dto.FiscalPeriodCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalPeriodDto;
import io.tahawus.lynx.ledger.model.FiscalPeriod;
import io.tahawus.lynx.ledger.model.FiscalYear;

import java.util.List;

/**
 * Maps between FiscalPeriod entity and DTOs.
 */
public final class FiscalPeriodMapper {

    private FiscalPeriodMapper() {}

    public static FiscalPeriodDto toDto(FiscalPeriod period) {
        if (period == null) return null;

        return new FiscalPeriodDto(
                period.id,
                period.fiscalYear != null ? period.fiscalYear.id : null,
                period.fiscalYear != null ? period.fiscalYear.year : null,
                period.periodNumber,
                period.startDate,
                period.endDate,
                period.status,
                period.getDisplayName(),
                period.createdAt,
                period.modifiedAt
        );
    }

    public static List<FiscalPeriodDto> toDtoList(List<FiscalPeriod> periods) {
        if (periods == null) return List.of();
        return periods.stream().map(FiscalPeriodMapper::toDto).toList();
    }

    public static FiscalPeriod fromCreateDto(FiscalPeriodCreateDto dto, FiscalYear fiscalYear) {
        if (dto == null) return null;

        FiscalPeriod period = new FiscalPeriod();
        period.fiscalYear = fiscalYear;
        period.periodNumber = dto.periodNumber();
        period.startDate = dto.startDate();
        period.endDate = dto.endDate();
        return period;
    }
}
