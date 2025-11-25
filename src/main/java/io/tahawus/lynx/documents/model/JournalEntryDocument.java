package io.tahawus.lynx.documents.model;

import io.tahawus.lynx.ledger.dto.JournalCreateDto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalEntryDocument - Manual journal entry.
 *
 * For adjustments, accruals, corrections, and other direct GL postings.
 * Uses signed amounts: positive = debit, negative = credit.
 *
 * @author Dan Pasco
 */
@Entity
@Table(name = "journal_entry_document")
@DiscriminatorValue("JOURNAL_ENTRY")
public class JournalEntryDocument extends Document {

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    public List<JournalEntryDocumentLine> lines = new ArrayList<>();

    // =============================
    // =   Line Management         =
    // =============================

    public void addLine(JournalEntryDocumentLine line) {
        lines.add(line);
        line.document = this;
    }

    public JournalEntryDocumentLine addDebit(Long accountId, BigDecimal amount, String description) {
        JournalEntryDocumentLine line = new JournalEntryDocumentLine();
        line.accountId = accountId;
        line.amount = amount;
        line.description = description;
        addLine(line);
        return line;
    }

    public JournalEntryDocumentLine addCredit(Long accountId, BigDecimal amount, String description) {
        JournalEntryDocumentLine line = new JournalEntryDocumentLine();
        line.accountId = accountId;
        line.amount = amount.negate();
        line.description = description;
        addLine(line);
        return line;
    }

    public void removeLine(JournalEntryDocumentLine line) {
        lines.remove(line);
        line.document = null;
    }

    public void clearLines() {
        for (JournalEntryDocumentLine line : new ArrayList<>(lines)) {
            removeLine(line);
        }
    }

    // =============================
    // =   Balance Calculations    =
    // =============================

    public BigDecimal getTotalDebits() {
        return lines.stream()
                .map(l -> l.amount)
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredits() {
        return lines.stream()
                .map(l -> l.amount)
                .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::negate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalance() {
        return lines.stream()
                .map(l -> l.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isBalanced() {
        return getBalance().compareTo(BigDecimal.ZERO) == 0;
    }

    // =============================
    // =   Document Implementation =
    // =============================

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.JOURNAL_ENTRY;
    }

    @Override
    public void validate() {
        super.validate();

        if (lines == null || lines.size() < 2) {
            throw new IllegalStateException("Journal entry must have at least two lines");
        }

        if (!isBalanced()) {
            throw new IllegalStateException(
                    "Journal entry must be balanced. Debits: " + getTotalDebits() +
                    ", Credits: " + getTotalCredits());
        }

        for (int i = 0; i < lines.size(); i++) {
            JournalEntryDocumentLine line = lines.get(i);
            if (line.accountId == null) {
                throw new IllegalStateException("Line " + (i + 1) + ": account is required");
            }
            if (line.amount == null || line.amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalStateException("Line " + (i + 1) + ": amount must be non-zero");
            }
        }
    }

    @Override
    protected JournalCreateDto buildJournalCreateDto() {
        List<JournalCreateDto.Line> journalLines = new ArrayList<>();

        for (JournalEntryDocumentLine line : lines) {
            journalLines.add(JournalCreateDto.Line.fromSigned(
                    line.accountId,
                    line.amount,
                    line.description
            ));
        }

        return new JournalCreateDto(
                business.id,
                documentDate,
                id,
                getDocumentType().getJournalType(),
                documentNumber,
                description != null ? description : "Journal Entry " + documentNumber,
                null,
                journalLines
        );
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<JournalEntryDocument> listByBusinessId(Long businessId) {
        return list("business.id = ?1 order by documentDate desc, id desc", businessId);
    }

    public static List<JournalEntryDocument> listByBusinessIdAndStatus(Long businessId, DocumentStatus status) {
        return list("business.id = ?1 and status = ?2 order by documentDate desc, id desc",
                businessId, status);
    }
}
