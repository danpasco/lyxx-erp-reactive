package io.tahawus.lynx.ledger.model;

/**
 * EntryType - Debit or Credit indicator for journal lines.
 *
 * @author Dan Pasco
 */
public enum EntryType {

    DEBIT,
    CREDIT;

    /**
     * Returns the opposite entry type.
     */
    public EntryType opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
