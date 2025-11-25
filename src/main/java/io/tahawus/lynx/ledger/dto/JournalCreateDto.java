package io.tahawus.lynx.ledger.dto;

import io.tahawus.lynx.ledger.model.EntryType;
import io.tahawus.lynx.ledger.model.JournalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JournalCreateDto - The ONE way to create a Journal.
 *
 * This DTO is built by documents during posting and passed to
 * JournalService.create(). The ledger module handles everything else:
 * fiscal period lookup, posting date computation, validation, persistence.
 *
 * @author Dan Pasco
 */
public record JournalCreateDto(
        Long businessId,
        LocalDate entryDate,
        Long documentId,
        JournalType journalType,
        String reference,
        String description,
        Long reversesJournalId,
        List<Line> lines
) {
    /**
     * Journal line specification.
     */
    public record Line(
            Long accountId,
            EntryType entryType,
            BigDecimal amount,
            String description
    ) {
        public Line {
            if (accountId == null) {
                throw new IllegalArgumentException("accountId is required");
            }
            if (entryType == null) {
                throw new IllegalArgumentException("entryType is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }

        /**
         * Create a debit line.
         */
        public static Line debit(Long accountId, BigDecimal amount, String description) {
            return new Line(accountId, EntryType.DEBIT, amount, description);
        }

        /**
         * Create a debit line without description.
         */
        public static Line debit(Long accountId, BigDecimal amount) {
            return debit(accountId, amount, null);
        }

        /**
         * Create a credit line.
         */
        public static Line credit(Long accountId, BigDecimal amount, String description) {
            return new Line(accountId, EntryType.CREDIT, amount, description);
        }

        /**
         * Create a credit line without description.
         */
        public static Line credit(Long accountId, BigDecimal amount) {
            return credit(accountId, amount, null);
        }

        /**
         * Create from signed amount (positive=debit, negative=credit).
         */
        public static Line fromSigned(Long accountId, BigDecimal signedAmount, String description) {
            if (signedAmount.compareTo(BigDecimal.ZERO) > 0) {
                return debit(accountId, signedAmount, description);
            } else if (signedAmount.compareTo(BigDecimal.ZERO) < 0) {
                return credit(accountId, signedAmount.negate(), description);
            } else {
                throw new IllegalArgumentException("amount cannot be zero");
            }
        }
    }

    /**
     * Validate the DTO.
     */
    public JournalCreateDto {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId is required");
        }
        if (entryDate == null) {
            throw new IllegalArgumentException("entryDate is required");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (journalType == null) {
            throw new IllegalArgumentException("journalType is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("at least one line is required");
        }
    }

    /**
     * Calculate total debits.
     */
    public BigDecimal getTotalDebits() {
        return lines.stream()
                .filter(l -> l.entryType() == EntryType.DEBIT)
                .map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total credits.
     */
    public BigDecimal getTotalCredits() {
        return lines.stream()
                .filter(l -> l.entryType() == EntryType.CREDIT)
                .map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Is the entry balanced?
     */
    public boolean isBalanced() {
        return getTotalDebits().compareTo(getTotalCredits()) == 0;
    }
}
