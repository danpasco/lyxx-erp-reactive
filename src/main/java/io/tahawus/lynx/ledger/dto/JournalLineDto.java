package io.tahawus.lynx.ledger.dto;

import io.tahawus.lynx.ledger.model.EntryType;

import java.math.BigDecimal;

/**
 * JournalLine view DTO.
 */
public record JournalLineDto(
        Long id,
        Integer lineNumber,
        Long accountId,
        String accountShortCode,
        String accountName,
        EntryType entryType,
        BigDecimal amount,
        String description
) {}
