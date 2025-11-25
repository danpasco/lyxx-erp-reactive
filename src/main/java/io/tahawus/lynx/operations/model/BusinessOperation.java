package io.tahawus.lynx.operations.model;

import io.tahawus.lynx.business.model.Business;

/**
 * BusinessOperation - Interface for operational records.
 *
 * Operations are business events with no direct accounting impact.
 * They accumulate facts about business activities. When operational
 * work is complete, they produce Documents which carry accounting impact.
 *
 * LIFECYCLE: OPEN → PROCESSED → COMPLETED
 *
 * - OPEN: Editable, work in progress
 * - PROCESSED: Operational work done, ready for document creation
 * - COMPLETED: Document(s) created, operation closed
 *
 * Document creation is type-specific. Each implementation defines
 * its own methods for creating documents (e.g., DisposalTicket.invoice()).
 *
 * @author Dan Pasco
 */
public interface BusinessOperation {

    /**
     * Get the operation's business.
     */
    Business getBusiness();

    /**
     * Get the current status.
     */
    OperationStatus getStatus();

    /**
     * Can this operation be edited?
     */
    default boolean canEdit() {
        return getStatus().canEdit();
    }

    /**
     * Can this operation be processed?
     */
    default boolean canProcess() {
        return getStatus().canProcess();
    }

    /**
     * Can this operation be reverted to OPEN?
     */
    default boolean canRevertToOpen() {
        return getStatus().canRevertToOpen();
    }

    /**
     * Can this operation be completed?
     */
    default boolean canComplete() {
        return getStatus().canComplete();
    }

    /**
     * Can this operation be voided?
     */
    default boolean canVoid() {
        return getStatus().canVoid();
    }

    /**
     * Validate the operation before processing.
     * @throws IllegalStateException if validation fails
     */
    void validate();

    /**
     * Process the operation. OPEN → PROCESSED.
     * Validates and marks operational work as complete.
     * @throws IllegalStateException if not in OPEN status
     */
    void process();

    /**
     * Revert to OPEN status. PROCESSED → OPEN.
     * @throws IllegalStateException if not in PROCESSED status
     */
    void revertToOpen();

    /**
     * Void the operation. OPEN or PROCESSED → VOIDED.
     * @throws IllegalStateException if already COMPLETED or VOIDED
     */
    void voidOperation();
}
