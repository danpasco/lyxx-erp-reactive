package io.tahawus.lynx.business.model;

/**
 * BasisOfAccounting - Accounting framework used by a Business.
 * <p>
 * Optional field on Business entity to distinguish between different
 * sets of books for the same legal entity.
 * <p>
 * Example: "Acme Corp - US GAAP" vs "Acme Corp - IFRS"
 *
 * @author Dan Pasco
 */
public enum BasisOfAccounting {

    /**
     * US Generally Accepted Accounting Principles
     */
    GAAP,

    /**
     * International Financial Reporting Standards
     */
    IFRS,

    /**
     * Tax basis accounting
     */
    TAX,

    /**
     * Cash basis accounting
     */
    CASH,

    /**
     * Modified accrual basis
     */
    MODIFIED_ACCRUAL
}
