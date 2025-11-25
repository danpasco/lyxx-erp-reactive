package io.tahawus.lynx.documents.model;

import io.tahawus.lynx.ledger.dto.JournalCreateDto;
import io.tahawus.lynx.ledger.model.FiscalYear;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ClosingEntryDocument - Year-end closing entry.
 *
 * Used to close revenue/expense accounts to Retained Earnings.
 *
 * RULES:
 * - Can be created anytime
 * - Entry date is always last day of fiscal year (enforced)
 * - Can only POST when fiscal year is in CLOSING status (checked by JournalService)
 *
 * @author Dan Pasco
 */
@Entity
@Table(name = "closing_entry_document")
@DiscriminatorValue("CLOSING_ENTRY")
public class ClosingEntryDocument extends Document {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    public FiscalYear fiscalYear;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    public List<ClosingEntryDocumentLine> lines = new ArrayList<>();

    // =============================
    // =   Line Management         =
    // =============================

    public void addLine(ClosingEntryDocumentLine line) {
        lines.add(line);
        line.document = this;
    }

    public ClosingEntryDocumentLine addDebit(Long accountId, BigDecimal amount, String description) {
        ClosingEntryDocumentLine line = new ClosingEntryDocumentLine();
        line.accountId = accountId;
        line.amount = amount;
        line.description = description;
        addLine(line);
        return line;
    }

    public ClosingEntryDocumentLine addCredit(Long accountId, BigDecimal amount, String description) {
        ClosingEntryDocumentLine line = new ClosingEntryDocumentLine();
        line.accountId = accountId;
        line.amount = amount.negate();
        line.description = description;
        addLine(line);
        return line;
    }

    public void removeLine(ClosingEntryDocumentLine line) {
        lines.remove(line);
        line.document = null;
    }

    public void clearLines() {
        for (ClosingEntryDocumentLine line : new ArrayList<>(lines)) {
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
        return DocumentType.CLOSING_ENTRY;
    }

    @Override
    public void validate() {
        super.validate();

        if (fiscalYear == null) {
            throw new IllegalStateException("Fiscal year is required for closing entries");
        }

        if (lines == null || lines.size() < 2) {
            throw new IllegalStateException("Closing entry must have at least two lines");
        }

        if (!isBalanced()) {
            throw new IllegalStateException(
                    "Closing entry must be balanced. Debits: " + getTotalDebits() +
                    ", Credits: " + getTotalCredits());
        }

        for (int i = 0; i < lines.size(); i++) {
            ClosingEntryDocumentLine line = lines.get(i);
            if (line.accountId == null) {
                throw new IllegalStateException("Line " + (i + 1) + ": account is required");
            }
            if (line.amount == null || line.amount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalStateException("Line " + (i + 1) + ": amount must be non-zero");
            }
        }
    }

    /**
     * Set fiscal year and enforce entry date = last day of fiscal year.
     */
    public void setFiscalYear(FiscalYear fiscalYear) {
        this.fiscalYear = fiscalYear;
        this.documentDate = fiscalYear.endDate;
    }

    @Override
    protected JournalCreateDto buildJournalCreateDto() {
        List<JournalCreateDto.Line> journalLines = new ArrayList<>();

        for (ClosingEntryDocumentLine line : lines) {
            journalLines.add(JournalCreateDto.Line.fromSigned(
                    line.accountId,
                    line.amount,
                    line.description
            ));
        }

        return new JournalCreateDto(
                business.id,
                fiscalYear.endDate, // Always last day of fiscal year
                id,
                getDocumentType().getJournalType(),
                documentNumber,
                description != null ? description : "Closing Entry - FY" + fiscalYear.year,
                null,
                journalLines
        );
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<ClosingEntryDocument> listByFiscalYear(FiscalYear fiscalYear) {
        return list("fiscalYear = ?1 order by id desc", fiscalYear);
    }

    public static List<ClosingEntryDocument> listByFiscalYearId(Long fiscalYearId) {
        return list("fiscalYear.id = ?1 order by id desc", fiscalYearId);
    }
}
