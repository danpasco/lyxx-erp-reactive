package io.tahawus.lynx.documents.model;

import io.tahawus.lynx.ledger.model.JournalType;

/**
 * DocumentType - Categorizes source documents.
 *
 * Each document type maps to a JournalType which determines
 * which register the resulting journal entry appears in.
 *
 * @author Dan Pasco
 */
public enum DocumentType {

    JOURNAL_ENTRY("JE", "Journal Entry", JournalType.JE),
    CLOSING_ENTRY("CE", "Closing Entry", JournalType.CE),
    INVOICE("INV", "Invoice", JournalType.SJ),
    CREDIT_MEMO("CM", "Credit Memo", JournalType.SJ),
    CASH_RECEIPT("CR", "Cash Receipt", JournalType.CR),
    CASH_DISBURSEMENT("CD", "Cash Disbursement", JournalType.CD),
    BILL("BILL", "Bill", JournalType.PJ),
    VENDOR_CREDIT("VC", "Vendor Credit", JournalType.PJ);

    private final String prefix;
    private final String displayName;
    private final JournalType journalType;

    DocumentType(String prefix, String displayName, JournalType journalType) {
        this.prefix = prefix;
        this.displayName = displayName;
        this.journalType = journalType;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JournalType getJournalType() {
        return journalType;
    }
}
