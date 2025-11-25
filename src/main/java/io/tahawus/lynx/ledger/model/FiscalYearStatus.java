package io.tahawus.lynx.ledger.model;

/**
 * FiscalYearStatus - Lifecycle status for fiscal years.
 *
 * OPEN → CLOSING → CLOSED (one-way progression)
 *
 * @author Dan Pasco
 */
public enum FiscalYearStatus {

    /**
     * Normal operations. Journal entries can be posted to any open FiscalPeriod.
     */
    OPEN,

    /**
     * Year-end close in progress. Only closing entries (JournalType.CE) allowed.
     * All regular activity posts to next fiscal year.
     */
    CLOSING,

    /**
     * Fiscal year is closed. No entries allowed. Opening balances have been
     * carried forward to the next fiscal year.
     */
    CLOSED
}
