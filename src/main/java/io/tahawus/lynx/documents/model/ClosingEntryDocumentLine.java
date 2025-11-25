package io.tahawus.lynx.documents.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * ClosingEntryDocumentLine - Line item in a closing entry.
 *
 * Same signed amount convention as JournalEntryDocumentLine.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "closing_entry_document_line",
        indexes = {
                @Index(name = "idx_cedl_document", columnList = "document_id"),
                @Index(name = "idx_cedl_account", columnList = "account_id")
        }
)
public class ClosingEntryDocumentLine extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    public ClosingEntryDocument document;

    @Column(name = "account_id", nullable = false)
    public Long accountId;

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
