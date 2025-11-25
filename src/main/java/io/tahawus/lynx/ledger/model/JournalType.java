package io.tahawus.lynx.ledger.model;

/**
 * JournalType - Indicates the source register/document type for a journal entry.
 *
 * @author Dan Pasco
 */
public enum JournalType {

    /**
     * Journal Entry - manual entry, one-to-one with JournalEntry document.
     */
    JE("Journal Entry"),

    /**
     * Closing Entry - year-end closing entries. Only allowed when FiscalYear
     * status is CLOSING. One-to-one with ClosingEntry document.
     */
    CE("Closing Entry"),

    /**
     * Cash Receipts - from CashReceipt documents. May be summarized (many-to-one).
     */
    CR("Cash Receipts"),

    /**
     * Cash Disbursements - from CashDisbursement documents. May be summarized.
     */
    CD("Cash Disbursements"),

    /**
     * Sales Journal - from Invoice, CreditMemo documents.
     */
    SJ("Sales Journal"),

    /**
     * Purchases Journal - from Bill, VendorCredit documents.
     */
    PJ("Purchases Journal");

    private final String displayName;

    JournalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
