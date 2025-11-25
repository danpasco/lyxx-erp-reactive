package io.tahawus.lynx.accounts.model;

/**
 * SubsidiaryType - Identifies the type of subsidiary ledger.
 *
 * Used on GeneralLedgerAccount to indicate whether it's a controlling
 * account for a specific subsidiary ledger type.
 *
 * @author Dan Pasco
 */
public enum SubsidiaryType {

    /**
     * No subsidiary ledger (standard G/L account, posts directly)
     */
    NONE,

    /**
     * Accounts Receivable subsidiary (A/R - customer accounts)
     */
    RECEIVABLE,

    /**
     * Accounts Payable subsidiary (A/P - vendor accounts)
     */
    PAYABLE,

    /**
     * Inventory item subsidiary
     */
    INVENTORY,

    /**
     * Bank/Cash account subsidiary
     */
    BANK
}
