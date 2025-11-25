package io.tahawus.lynx.ledger.dto;

import io.tahawus.lynx.ledger.model.FiscalYearStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FiscalYear view DTO.
 */
public record FiscalYearDto(
        Long id,
        Long businessId,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        FiscalYearStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
