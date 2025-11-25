package io.tahawus.lynx.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.tahawus.lynx.business.model.Business;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tracks numeric number sequences per Business.
 *
 * Each Business can have many NumberSequence rows, identified by a string sequenceKey.
 * Example keys:
 *   - "SALES_INVOICE"
 *   - "DISPOSAL_TICKET"
 *   - "GENERAL_JOURNAL"
 *
 * This entity is responsible ONLY for:
 *   - Storing the next numeric value (nextNumber)
 *   - Incrementing it atomically within a transaction
 *
 * Formatting (prefix, padding, etc.) is LEFT TO CALLERS.
 *
 * Stored in tenant schema (not public); tenant/schema is selected by multi-tenancy infra.
 */
@Entity
@Table(
        name = "number_sequence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_business_sequence_key",
                        columnNames = {"business_id", "sequence_key"}
                )
        },
        indexes = {
                @Index(name = "idx_number_seq_business", columnList = "business_id"),
                @Index(name = "idx_number_seq_key", columnList = "sequence_key")
        }
)
public class NumberSequence extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // =============================
    // =    Business Reference     =
    // =============================

    /**
     * Reference to Business that owns this sequence.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    // =============================
    // =    Sequence Identity      =
    // =============================

    /**
     * Logical key of the sequence.
     *
     * This identifies WHAT is being numbered, not HOW it's formatted.
     * Example values:
     *   - "SALES_INVOICE"
     *   - "DISPOSAL_TICKET"
     *   - "PURCHASE_ORDER"
     *
     * Formatting (prefix, padding) is handled by consumers of the service.
     */
    @Column(name = "sequence_key", nullable = false, length = 100)
    public String sequenceKey;

    /**
     * Next numeric value to use.
     *
     * IMPORTANT:
     *   - Must be incremented only inside a transaction
     *   - Should be read/updated with PESSIMISTIC_WRITE locking to avoid duplicates
     */
    @Column(name = "next_number", nullable = false)
    public Long nextNumber;

    // =============================
    // =     Audit Fields          =
    // =============================

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "modified_at")
    public LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    // =============================
    // =    Business Methods       =
    // =============================

    /**
     * Generate the next numeric value and increment the sequence.
     *
     * IMPORTANT:
     *   - This method must ONLY be called within an active transaction
     *   - The entity must be loaded with a PESSIMISTIC_WRITE lock to guarantee
     *     gapless, duplicate-free numbers under concurrency.
     *
     * @return the current value of nextNumber, before increment
     */
    public Long generateNextNumber() {
        Long current = nextNumber;
        nextNumber = nextNumber + 1;
        return current;
    }

    /**
     * Create a new sequence instance (not yet persisted).
     *
     * @param business      owner of the sequence
     * @param sequenceKey   logical key (e.g., "SALES_INVOICE")
     * @param startNumber   first number to issue (e.g., 1)
     */
    public static NumberSequence create(Business business, String sequenceKey, long startNumber) {
        NumberSequence sequence = new NumberSequence();
        sequence.business = business;
        sequence.sequenceKey = sequenceKey;
        sequence.nextNumber = startNumber;
        return sequence;
    }

    @Override
    public String toString() {
        return "NumberSequence{" +
                "id=" + id +
                ", businessId=" + (business != null ? business.id : null) +
                ", sequenceKey='" + sequenceKey + '\'' +
                ", nextNumber=" + nextNumber +
                '}';
    }
}
