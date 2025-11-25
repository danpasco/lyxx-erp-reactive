package io.tahawus.lynx.ledger.model;

/**
 * Type of fiscal period for hierarchical organization.
 *
 * Examples:
 * - YEAR (top level, entire fiscal year)
 * - QUARTER (3 months)
 * - MONTH (calendar month)
 * - WEEK (7 days)
 * - CUSTOM (user-defined periods)
 */
public enum FiscalPeriodType {
    QUARTER,
    MONTH,
    WEEK,
    CUSTOM;
}
