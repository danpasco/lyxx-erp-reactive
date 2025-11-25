package io.tahawus.lynx.documents.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a closing entry.
 *
 * Note: documentDate is not provided - it's always the last day of the fiscal year.
 */
public record ClosingEntryDocumentCreateDto(
        @NotNull(message = "Business ID is required")
        Long businessId,

        @NotNull(message = "Fiscal year ID is required")
        Long fiscalYearId,

        String description,
        String notes,

        @NotEmpty(message = "At least two lines are required")
        @Valid
        List<LineDto> lines
) {
    public record LineDto(
            @NotNull(message = "Account ID is required")
            Long accountId,

            @NotNull(message = "Amount is required")
            BigDecimal amount,

            String description
    ) {}

    public BigDecimal getBalance() {
        if (lines == null) return BigDecimal.ZERO;
        return lines.stream()
                .map(LineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isBalanced() {
        return getBalance().compareTo(BigDecimal.ZERO) == 0;
    }
}
