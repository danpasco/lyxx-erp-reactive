package io.tahawus.lynx.ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload for creating a new FiscalYear.
 */
public record FiscalYearCreateDto(

        @NotNull(message = "Business ID is required")
        Long businessId,

        @NotNull(message = "Year is required")
        Integer year,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {}
