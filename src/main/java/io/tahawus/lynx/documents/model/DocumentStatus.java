package io.tahawus.lynx.documents.model;

/**
 * DocumentStatus - Lifecycle status of a document.
 *
 * OPEN → COMPLETED → POSTED → VOIDED
 *       ↑____↓
 *       (revert)
 *
 * @author Dan Pasco
 */
public enum DocumentStatus {

    OPEN("Open"),
    COMPLETED("Completed"),
    POSTED("Posted"),
    VOIDED("Voided");

    private final String displayName;

    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canEdit() {
        return this == OPEN;
    }

    public boolean canComplete() {
        return this == OPEN;
    }

    public boolean canRevert() {
        return this == COMPLETED;
    }

    public boolean canPost() {
        return this == COMPLETED;
    }

    public boolean canVoid() {
        return this == POSTED;
    }
}
