package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.Optional;

/**
 * GeneralLedgerAccount - General Ledger account definition.
 *
 * Implements Account interface. This is the primary account type in the
 * chart of accounts. Subsidiary accounts (A/R, A/P, Bank, Inventory)
 * reference a GeneralLedgerAccount as their controlling account.
 *
 * FORMAT: TT.GG.AAAA
 * Example: "10.15.0100" (Assets > Current Assets > Cash in Bank)
 *
 * CONTROLLING vs POSTING:
 * - If subsidiaryType != NONE, this is a controlling account for subsidiaries
 * - Controlling accounts cannot receive direct postings
 * - If subsidiaryType == NONE, this is a regular posting account
 *
 * BUSINESS RULES:
 * - shortCode must be unique within business (for reverse lookup)
 * - accountNumber must be unique within accountGroup
 * - accountNumber range: 0-9999 (4 digits when formatted)
 * - Deletion check performed in service layer
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "general_ledger_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gl_account_number",
                        columnNames = {"account_group_id", "account_number"}
                ),
                @UniqueConstraint(
                        name = "uk_gl_short_code",
                        columnNames = {"business_id", "short_code"}
                )
        },
        indexes = {
                @Index(name = "idx_gl_business", columnList = "business_id"),
                @Index(name = "idx_gl_account_group", columnList = "account_group_id"),
                @Index(name = "idx_gl_short_code", columnList = "short_code")
        }
)
public class GeneralLedgerAccount extends AuditableEntity implements Account {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_group_id", nullable = false)
    public AccountGroup accountGroup;

    /**
     * Account number within group (0-9999).
     * Stored as integer, formatted with 4-digit padding.
     */
    @Column(name = "account_number", nullable = false)
    public Integer accountNumber;

    /**
     * Short code for reverse resolution (USER-FRIENDLY).
     * Examples: "CASH", "AR-CTRL", "REVENUE"
     * This is what users type during data entry.
     */
    @Column(name = "short_code", nullable = false, length = 50)
    public String shortCode;

    /**
     * Account name.
     */
    @Column(nullable = false, length = 200)
    public String name;

    /**
     * Optional description.
     */
    @Column(length = 500)
    public String description;

    /**
     * Subsidiary type this account controls (NONE if regular posting account).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subsidiary_type", nullable = false, length = 20)
    public SubsidiaryType subsidiaryType = SubsidiaryType.NONE;

    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    // =============================
    // =   Account Interface       =
    // =============================

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Business getBusiness() {
        return business;
    }

    @Override
    public AccountType getAccountType() {
        return accountGroup.accountType;
    }

    @Override
    public GeneralLedgerAccount getControllingAccount() {
        return this; // G/L account is its own controlling account
    }

    @Override
    public String getShortCode() {
        return shortCode;
    }

    @Override
    public String getDisplayName() {
        return shortCode + " - " + name;
    }

    @Override
    public String getFormattedAccountNumber() {
        return accountGroup.getFormattedNumber() + "." + String.format("%04d", accountNumber);
    }

    @Override
    public String getFullPath() {
        return accountGroup.getFullPath() + " > " + name;
    }

    @Override
    public boolean isActive() {
        return isActive != null && isActive;
    }

    // =============================
    // =   Business Logic          =
    // =============================

    /**
     * Is this a controlling account for subsidiaries?
     * Controlling accounts cannot receive direct postings.
     */
    public boolean isControllingAccount() {
        return subsidiaryType != SubsidiaryType.NONE;
    }

    /**
     * Can this account receive direct postings?
     */
    public boolean isPostingAccount() {
        return subsidiaryType == SubsidiaryType.NONE;
    }

    // =============================
    // =   Validation              =
    // =============================

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (accountNumber == null || accountNumber < 0 || accountNumber > 9999) {
            throw new IllegalStateException("GeneralLedgerAccount accountNumber must be between 0 and 9999");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<GeneralLedgerAccount> listByBusiness(Business business) {
        return list("business = ?1 and isActive = true order by accountGroup.accountType, accountGroup.displayOrder, accountNumber",
                business);
    }

    public static List<GeneralLedgerAccount> listByAccountGroup(AccountGroup accountGroup) {
        return list("accountGroup = ?1 and isActive = true order by accountNumber", accountGroup);
    }

    public static List<GeneralLedgerAccount> listByAccountType(Business business, AccountType accountType) {
        return list("business = ?1 and accountGroup.accountType = ?2 and isActive = true order by accountGroup.displayOrder, accountNumber",
                business, accountType);
    }

    public static Optional<GeneralLedgerAccount> findByShortCode(Business business, String shortCode) {
        return find("business = ?1 and shortCode = ?2 and isActive = true", business, shortCode)
                .firstResultOptional();
    }

    public static List<GeneralLedgerAccount> searchByShortCode(Business business, String prefix) {
        return list("business = ?1 and lower(shortCode) like ?2 and isActive = true order by shortCode",
                business, prefix.toLowerCase() + "%");
    }

    public static List<GeneralLedgerAccount> searchByName(Business business, String pattern) {
        return list("business = ?1 and lower(name) like ?2 and isActive = true order by name",
                business, "%" + pattern.toLowerCase() + "%");
    }

    public static Optional<GeneralLedgerAccount> findByFormattedNumber(Business business, String formattedNumber) {
        String[] parts = formattedNumber.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String groupFormatted = parts[0] + "." + parts[1];
            int accountNum = Integer.parseInt(parts[2]);

            return AccountGroup.findByFormattedNumber(business, groupFormatted)
                    .flatMap(group -> find("accountGroup = ?1 and accountNumber = ?2", group, accountNum)
                            .firstResultOptional());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "GeneralLedgerAccount{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", formattedNumber='" + getFormattedAccountNumber() + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
