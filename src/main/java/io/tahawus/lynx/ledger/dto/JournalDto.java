package io.tahawus.lynx.ledger.dto;

import io.tahawus.lynx.ledger.model.JournalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal view DTO.
 */
public record JournalDto(
        Long id,
        Long businessId,
        Long fiscalPeriodId,
        String fiscalPeriodName,
        JournalType journalType,
        LocalDate entryDate,
        LocalDate postingDate,
        Long documentId,
        String reference,
        String description,
        boolean isReversing,
        Long reversesJournalId,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        boolean isBalanced,
        List<JournalLineDto> lines,
        LocalDateTime createdAt
) {}
