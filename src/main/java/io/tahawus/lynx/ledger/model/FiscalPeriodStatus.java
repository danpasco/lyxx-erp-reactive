package io.tahawus.lynx.ledger.model;

/**
 * FiscalPeriodStatus - Soft close status for fiscal periods.
 *
 * Periods use soft close: OPEN ↔ CLOSED (reversible until FY closes)
 *
 * @author Dan Pasco
 */
public enum FiscalPeriodStatus {

    /**
     * Period accepts journal entries.
     */
    OPEN,

    /**
     * Period is soft-closed. Can be reopened if fiscal year is still OPEN.
     * Once fiscal year is CLOSED, period closure is permanent.
     */
    CLOSED
}
