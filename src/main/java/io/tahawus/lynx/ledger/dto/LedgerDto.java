package io.tahawus.lynx.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger view DTO.
 */
public record LedgerDto(
        Long id,
        Long fiscalYearId,
        Integer fiscalYear,
        Long accountId,
        String accountShortCode,
        String accountName,
        BigDecimal openingBalance,
        BigDecimal currentBalance,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
