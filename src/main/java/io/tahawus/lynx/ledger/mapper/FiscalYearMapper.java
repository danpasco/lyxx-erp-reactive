package io.tahawus.lynx.ledger.mapper;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.ledger.dto.FiscalYearCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalYearDto;
import io.tahawus.lynx.ledger.model.FiscalYear;

import java.util.List;

/**
 * Maps between FiscalYear entity and DTOs.
 */
public final class FiscalYearMapper {

    private FiscalYearMapper() {}

    public static FiscalYearDto toDto(FiscalYear fiscalYear) {
        if (fiscalYear == null) return null;

        return new FiscalYearDto(
                fiscalYear.id,
                fiscalYear.business != null ? fiscalYear.business.id : null,
                fiscalYear.year,
                fiscalYear.startDate,
                fiscalYear.endDate,
                fiscalYear.status,
                fiscalYear.createdAt,
                fiscalYear.modifiedAt
        );
    }

    public static List<FiscalYearDto> toDtoList(List<FiscalYear> fiscalYears) {
        if (fiscalYears == null) return List.of();
        return fiscalYears.stream().map(FiscalYearMapper::toDto).toList();
    }

    public static FiscalYear fromCreateDto(FiscalYearCreateDto dto, Business business) {
        if (dto == null) return null;

        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.business = business;
        fiscalYear.year = dto.year();
        fiscalYear.startDate = dto.startDate();
        fiscalYear.endDate = dto.endDate();
        return fiscalYear;
    }
}
