package io.tahawus.lynx.ledger.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JournalLine - Individual debit or credit line within a Journal.
 *
 * Each line posts to a GeneralLedgerAccount with a positive amount
 * and an entry type (DEBIT or CREDIT).
 *
 * IMMUTABILITY:
 * Owned by Journal. Cannot be modified after creation.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "journal_line",
        indexes = {
                @Index(name = "idx_journal_line_journal", columnList = "journal_id"),
                @Index(name = "idx_journal_line_account", columnList = "account_id")
        }
)
public class JournalLine extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    public Journal journal;

    /**
     * Line number for ordering within the journal.
     */
    @Column(name = "line_number", nullable = false)
    public Integer lineNumber;

    /**
     * Reference to GeneralLedgerAccount.id.
     * We use ID reference rather than entity to allow flexibility
     * in account resolution (may be GL or subsidiary in future).
     */
    @Column(name = "account_id", nullable = false)
    public Long accountId;

    /**
     * Debit or Credit.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    public EntryType entryType;

    /**
     * Amount (always positive).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal amount;

    /**
     * Optional line-level description/memo.
     */
    @Column(length = 200)
    public String description;

    // =============================
    // =   Helper Methods          =
    // =============================

    /**
     * Is this a debit entry?
     */
    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    /**
     * Is this a credit entry?
     */
    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    /**
     * Get signed amount (positive for debit, negative for credit).
     * Useful for balance calculations.
     */
    public BigDecimal getSignedAmount() {
        return isDebit() ? amount : amount.negate();
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<JournalLine> listByJournal(Journal journal) {
        return list("journal = ?1 order by lineNumber", journal);
    }

    public static List<JournalLine> listByAccountId(Long accountId) {
        return list("accountId = ?1 order by journal.postingDate, journal.id, lineNumber", accountId);
    }

    public static List<JournalLine> listByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        return list("accountId = ?1 and journal.postingDate >= ?2 and journal.postingDate <= ?3 order by journal.postingDate, journal.id, lineNumber",
                accountId, startDate, endDate);
    }

    @Override
    public String toString() {
        return "JournalLine{" +
                "id=" + id +
                ", lineNumber=" + lineNumber +
                ", accountId=" + accountId +
                ", entryType=" + entryType +
                ", amount=" + amount +
                '}';
    }
}
