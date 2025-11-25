package io.tahawus.lynx.documents.dto;

import io.tahawus.lynx.documents.model.DocumentStatus;
import io.tahawus.lynx.documents.model.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for journal entry documents.
 */
public record JournalEntryDocumentDto(
        Long id,
        Long businessId,
        String documentNumber,
        DocumentType documentType,
        LocalDate documentDate,
        DocumentStatus status,
        String description,
        String referenceNumber,
        String notes,
        Long journalId,
        Long reversingJournalId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
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
    ) {
        public boolean isDebit() {
            return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean isCredit() {
            return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
        }
    }
}
