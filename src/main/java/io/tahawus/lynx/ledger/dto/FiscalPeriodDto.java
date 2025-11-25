package io.tahawus.lynx.ledger.dto;

import io.tahawus.lynx.ledger.model.FiscalPeriodStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FiscalPeriod view DTO.
 */
public record FiscalPeriodDto(
        Long id,
        Long fiscalYearId,
        Integer fiscalYear,
        Integer periodNumber,
        LocalDate startDate,
        LocalDate endDate,
        FiscalPeriodStatus status,
        String displayName,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
