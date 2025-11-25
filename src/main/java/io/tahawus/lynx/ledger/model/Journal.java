package io.tahawus.lynx.ledger.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Journal - Immutable journal entry header.
 *
 * All financial transactions are recorded as journal entries. Once created,
 * a journal entry cannot be modified. Corrections require a reversing entry.
 *
 * IMMUTABILITY:
 * - No update operations allowed
 * - Corrections via reversing entries only
 * - createdAt is the permanent timestamp
 *
 * POSTING DATE COMPUTATION:
 * - entryDate: when the economic event occurred
 * - postingDate: first day of the first OPEN FiscalPeriod on or after entryDate
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "journal",
        indexes = {
                @Index(name = "idx_journal_business", columnList = "business_id"),
                @Index(name = "idx_journal_fiscal_period", columnList = "fiscal_period_id"),
                @Index(name = "idx_journal_entry_date", columnList = "entry_date"),
                @Index(name = "idx_journal_posting_date", columnList = "posting_date"),
                @Index(name = "idx_journal_type", columnList = "journal_type"),
                @Index(name = "idx_journal_reverses", columnList = "reverses_journal_id")
        }
)
public class Journal extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    public FiscalPeriod fiscalPeriod;

    /**
     * Source register type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 10)
    public JournalType journalType;

    /**
     * Entry date - when the economic event occurred.
     */
    @Column(name = "entry_date", nullable = false)
    public LocalDate entryDate;

    /**
     * Posting date - first day of the OPEN period on or after entryDate.
     */
    @Column(name = "posting_date", nullable = false)
    public LocalDate postingDate;

    /**
     * Reference to source document (Document.id).
     * Every journal entry originates from a document.
     */
    @Column(name = "document_id", nullable = false)
    public Long documentId;

    /**
     * Human-readable reference (document number, check number, etc.)
     */
    @Column(length = 50)
    public String reference;

    @Column(nullable = false, length = 500)
    public String description;

    /**
     * For reversing entries, points to the journal being reversed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_journal_id")
    public Journal reversesJournal;

    /**
     * Journal lines (detail).
     */
    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber asc")
    public List<JournalLine> lines = new ArrayList<>();

    // =============================
    // =   Computed Properties     =
    // =============================

    /**
     * Is this a reversing entry?
     */
    public boolean isReversing() {
        return reversesJournal != null;
    }

    /**
     * Total debits.
     */
    public BigDecimal getTotalDebits() {
        return lines.stream()
                .filter(line -> line.entryType == EntryType.DEBIT)
                .map(line -> line.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total credits.
     */
    public BigDecimal getTotalCredits() {
        return lines.stream()
                .filter(line -> line.entryType == EntryType.CREDIT)
                .map(line -> line.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Is the entry balanced? (debits = credits)
     */
    public boolean isBalanced() {
        return getTotalDebits().compareTo(getTotalCredits()) == 0;
    }

    // =============================
    // =   Helper Methods          =
    // =============================

    /**
     * Add a line to the journal.
     */
    public JournalLine addLine(int lineNumber, Long accountId, EntryType entryType, BigDecimal amount, String description) {
        JournalLine line = new JournalLine();
        line.journal = this;
        line.lineNumber = lineNumber;
        line.accountId = accountId;
        line.entryType = entryType;
        line.amount = amount;
        line.description = description;
        lines.add(line);
        return line;
    }

    // =============================
    // =   Immutability            =
    // =============================

    /**
     * Prevent updates after creation.
     */
    @PreUpdate
    protected void preventUpdate() {
        throw new IllegalStateException("Journal entries are immutable. Create a reversing entry instead.");
    }

    @PrePersist
    protected void validateOnCreate() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Journal must have at least one line");
        }
        if (!isBalanced()) {
            throw new IllegalStateException("Journal entry must be balanced (debits = credits)");
        }
        if (journalType == JournalType.CE && fiscalPeriod.fiscalYear.status != FiscalYearStatus.CLOSING) {
            throw new IllegalStateException("Closing entries (CE) only allowed when fiscal year is CLOSING");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<Journal> listByFiscalPeriod(FiscalPeriod fiscalPeriod) {
        return list("fiscalPeriod = ?1 order by postingDate, id", fiscalPeriod);
    }

    public static List<Journal> listByFiscalYear(FiscalYear fiscalYear) {
        return list("fiscalPeriod.fiscalYear = ?1 order by postingDate, id", fiscalYear);
    }

    public static List<Journal> listByBusiness(Business business) {
        return list("business = ?1 order by postingDate desc, id desc", business);
    }

    public static List<Journal> listByDocument(Long documentId) {
        return list("documentId = ?1 order by id", documentId);
    }

    public static List<Journal> listByDateRange(Business business, LocalDate startDate, LocalDate endDate) {
        return list("business = ?1 and postingDate >= ?2 and postingDate <= ?3 order by postingDate, id",
                business, startDate, endDate);
    }

    public static Optional<Journal> findReversing(Journal original) {
        return find("reversesJournal = ?1", original).firstResultOptional();
    }

    @Override
    public String toString() {
        return "Journal{" +
                "id=" + id +
                ", journalType=" + journalType +
                ", entryDate=" + entryDate +
                ", postingDate=" + postingDate +
                ", reference='" + reference + '\'' +
                ", balanced=" + isBalanced() +
                ", lines=" + lines.size() +
                '}';
    }
}
