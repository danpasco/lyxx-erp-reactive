package io.tahawus.lynx.documents.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * JournalEntryDocumentLine - Line item in a manual journal entry.
 *
 * Uses signed amounts for user convenience:
 * - Positive = Debit
 * - Negative = Credit
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "journal_entry_document_line",
        indexes = {
                @Index(name = "idx_jedl_document", columnList = "document_id"),
                @Index(name = "idx_jedl_account", columnList = "account_id")
        }
)
public class JournalEntryDocumentLine extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    public JournalEntryDocument document;

    @Column(name = "account_id", nullable = false)
    public Long accountId;

    /**
     * Signed amount: positive = debit, negative = credit.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal amount;

    @Column(length = 200)
    public String description;

    public boolean isDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount != null ? amount.abs() : BigDecimal.ZERO;
    }
}
