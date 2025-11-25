package io.tahawus.lynx.accounts.model;

/**
 * AccountType - Top-level account classification with assigned numbers.
 *
 * Maps to financial statement sections and provides the first segment
 * of formatted account numbers (TT.GG.AAAA).
 *
 * IMMUTABLE: These values and their numbers cannot be changed.
 *
 * @author Dan Pasco
 */
public enum AccountType {

    /**
     * Assets - Things owned by the business
     * Formatted as: 10.GG.AAAA
     */
    ASSET(10, "Assets"),

    /**
     * Liabilities - Obligations owed by the business
     * Formatted as: 20.GG.AAAA
     */
    LIABILITY(20, "Liabilities"),

    /**
     * Equity - Owner's stake in the business
     * Formatted as: 30.GG.AAAA
     */
    EQUITY(30, "Equity"),

    /**
     * Revenue - Income earned from operations
     * Formatted as: 60.GG.AAAA
     */
    REVENUE(60, "Revenue"),

    /**
     * Expenses - Costs incurred in operations
     * Formatted as: 80.GG.AAAA
     */
    EXPENSE(80, "Expenses");

    private final int number;
    private final String displayName;

    AccountType(int number, String displayName) {
        this.number = number;
        this.displayName = displayName;
    }

    public int getNumber() {
        return number;
    }

    public String getFormattedNumber() {
        return String.format("%02d", number);
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AccountType fromNumber(int number) {
        for (AccountType type : values()) {
            if (type.number == number) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AccountType number: " + number);
    }

    @Override
    public String toString() {
        return displayName + " (" + getFormattedNumber() + ")";
    }
}
