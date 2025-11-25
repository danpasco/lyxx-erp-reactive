package io.tahawus.lynx.ledger.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload for creating a new FiscalPeriod.
 */
public record FiscalPeriodCreateDto(

        @NotNull(message = "Fiscal year ID is required")
        Long fiscalYearId,

        @NotNull(message = "Period number is required")
        @Min(value = 1, message = "Period number must be at least 1")
        @Max(value = 13, message = "Period number must not exceed 13")
        Integer periodNumber,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {}
