package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;

/**
 * Account - Polymorphic interface for all account types.
 *
 * Anything that can be referenced in journal postings implements this interface:
 * - GeneralLedgerAccount (G/L accounts - may be controlling or posting)
 * - ReceivableAccount (Customer A/R subsidiary)
 * - PayableAccount (Vendor A/P subsidiary)
 * - BankAccount (Cash/Bank subsidiary)
 * - InventoryAccount (Item inventory subsidiary)
 *
 * DESIGN PRINCIPLES:
 * - This is DEFINITIONAL only - no transaction data
 * - All account types treated uniformly for posting
 * - Users search by shortCode or name, never by hierarchical path
 * - Full paths maintained internally for reporting
 *
 * PACKAGE BOUNDARY:
 * This lives in 'accounts' package - chart of accounts definitions.
 * Transactional data (journals, balances) lives in 'ledger' package.
 *
 * @author Dan Pasco
 */
public interface Account {

    /**
     * Get the unique identifier.
     */
    Long getId();

    /**
     * Get the business this account belongs to.
     */
    Business getBusiness();

    /**
     * Get the account type (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE).
     */
    AccountType getAccountType();

    /**
     * Get the controlling G/L account for rollups.
     * - For GeneralLedgerAccount: returns itself
     * - For subsidiaries: returns their controlling account
     */
    GeneralLedgerAccount getControllingAccount();

    /**
     * Get the short code for user entry.
     * This is what users type - never the full hierarchical path.
     * Examples: "CASH", "AR-156", "ACME"
     */
    String getShortCode();

    /**
     * Get user-friendly display name.
     * Examples: "CASH - Cash in Bank", "AR-156 - ABC Corp (A/R)"
     */
    String getDisplayName();

    /**
     * Get formatted account number.
     * G/L: TT.GG.AAAA (e.g., "10.15.0100")
     * Subsidiary: TT.GG.AAAA.SS (e.g., "10.15.0100.01")
     */
    String getFormattedAccountNumber();

    /**
     * Get full path with names (for display/reporting).
     * Example: "Assets > Current Assets > Cash > Chase Checking"
     */
    String getFullPath();

    /**
     * Is this account currently active?
     */
    boolean isActive();
}
