package io.tahawus.lynx.documents.dto;

import io.tahawus.lynx.documents.model.DocumentStatus;
import io.tahawus.lynx.documents.model.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for closing entry documents.
 */
public record ClosingEntryDocumentDto(
        Long id,
        Long businessId,
        String documentNumber,
        DocumentType documentType,
        LocalDate documentDate,
        DocumentStatus status,
        String description,
        String notes,
        Long journalId,
        Long reversingJournalId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        Long fiscalYearId,
        Integer fiscalYear,
        List<LineDto> lines,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        boolean balanced
) {
    public record LineDto(
            Long id,
            Long accountId,
            String accountCode,
            String accountName,
            BigDecimal amount,
            String description
    ) {}
}
