package io.tahawus.lynx.operations.model;

/**
 * OperationStatus - Lifecycle status of a BusinessOperation.
 *
 * OPEN → PROCESSED → COMPLETED
 *
 * @author Dan Pasco
 */
public enum OperationStatus {

    /**
     * Operation is open and editable.
     * Work is in progress.
     */
    OPEN("Open"),

    /**
     * Operational work is complete.
     * Ready for document creation.
     */
    PROCESSED("Processed"),

    /**
     * Operation is complete.
     * Document(s) have been created.
     */
    COMPLETED("Completed"),

    /**
     * Operation has been voided/cancelled.
     */
    VOIDED("Voided");

    private final String displayName;

    OperationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canEdit() {
        return this == OPEN;
    }

    public boolean canProcess() {
        return this == OPEN;
    }

    public boolean canRevertToOpen() {
        return this == PROCESSED;
    }

    public boolean canComplete() {
        return this == PROCESSED;
    }

    public boolean canVoid() {
        return this == OPEN || this == PROCESSED;
    }
}
