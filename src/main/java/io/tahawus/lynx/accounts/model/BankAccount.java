package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.Optional;

/**
 * BankAccount - Bank/Cash subsidiary account.
 *
 * Implements Account interface for cash postings.
 *
 * FORMAT: TT.GG.AAAA.SS
 * Example: "10.10.0100.01" (Assets > Current > Cash Control > Chase Checking)
 *
 * BUSINESS RULES:
 * - Must have controlling GeneralLedgerAccount with subsidiaryType = BANK
 * - subsidiaryNumber range: 0-99 (2 digits when formatted)
 * - shortCode must be unique within business
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "bank_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bank_subsidiary_number",
                        columnNames = {"controlling_account_id", "subsidiary_number"}
                ),
                @UniqueConstraint(
                        name = "uk_bank_short_code",
                        columnNames = {"controlling_account_id", "short_code"}
                )
        },
        indexes = {
                @Index(name = "idx_bank_controlling", columnList = "controlling_account_id"),
                @Index(name = "idx_bank_short_code", columnList = "short_code")
        }
)
public class BankAccount extends AuditableEntity implements Account {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controlling_account_id", nullable = false)
    public GeneralLedgerAccount controllingAccount;

    /**
     * Subsidiary number within controlling account (0-99).
     */
    @Column(name = "subsidiary_number", nullable = false)
    public Integer subsidiaryNumber;

    /**
     * Short code for reverse resolution.
     */
    @Column(name = "short_code", nullable = false, length = 50)
    public String shortCode;

    /**
     * Account name (e.g., "Chase Checking", "Operating Account").
     */
    @Column(nullable = false, length = 200)
    public String name;

    // =============================
    // =   Bank-Specific Fields    =
    // =============================

    @Column(name = "bank_name", length = 200)
    public String bankName;

    @Column(name = "bank_account_number", length = 50)
    public String bankAccountNumber;

    @Column(name = "routing_number", length = 20)
    public String routingNumber;

    /**
     * Bank account type (CHECKING, SAVINGS, MONEY_MARKET, etc.)
     */
    @Column(name = "bank_account_type", length = 20)
    public String bankAccountType;

    @Column(name = "branch_name", length = 200)
    public String branchName;

    @Column(name = "swift_code", length = 20)
    public String swiftCode;

    @Column(length = 50)
    public String iban;

    @Column(length = 3)
    public String currency = "USD";

    @Column(name = "is_primary", nullable = false)
    public Boolean isPrimary = false;

    @Column(length = 1000)
    public String notes;

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
        return controllingAccount.business;
    }

    @Override
    public AccountType getAccountType() {
        return controllingAccount.getAccountType();
    }

    @Override
    public GeneralLedgerAccount getControllingAccount() {
        return controllingAccount;
    }

    @Override
    public String getShortCode() {
        return shortCode;
    }

    @Override
    public String getDisplayName() {
        return shortCode + " - " + name + " (Bank)";
    }

    @Override
    public String getFormattedAccountNumber() {
        return controllingAccount.getFormattedAccountNumber() + "." + String.format("%02d", subsidiaryNumber);
    }

    @Override
    public String getFullPath() {
        return controllingAccount.getFullPath() + " > " + name;
    }

    @Override
    public boolean isActive() {
        return isActive != null && isActive;
    }

    // =============================
    // =   Validation              =
    // =============================

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (subsidiaryNumber == null || subsidiaryNumber < 0 || subsidiaryNumber > 99) {
            throw new IllegalStateException("BankAccount subsidiaryNumber must be between 0 and 99");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<BankAccount> listByBusiness(Business business) {
        return list("controllingAccount.business = ?1 and isActive = true order by isPrimary desc, controllingAccount.accountNumber, subsidiaryNumber",
                business);
    }

    public static List<BankAccount> listByControllingAccount(GeneralLedgerAccount controllingAccount) {
        return list("controllingAccount = ?1 and isActive = true order by subsidiaryNumber", controllingAccount);
    }

    public static Optional<BankAccount> findByShortCode(Business business, String shortCode) {
        return find("controllingAccount.business = ?1 and shortCode = ?2 and isActive = true", business, shortCode)
                .firstResultOptional();
    }

    public static Optional<BankAccount> findPrimary(Business business) {
        return find("controllingAccount.business = ?1 and isPrimary = true and isActive = true", business)
                .firstResultOptional();
    }

    public static List<BankAccount> search(Business business, String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return list("controllingAccount.business = ?1 and isActive = true and (lower(shortCode) like ?2 or lower(name) like ?2 or lower(bankName) like ?2) order by shortCode",
                business, pattern);
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", name='" + name + '\'' +
                ", bankName='" + bankName + '\'' +
                ", formattedNumber='" + getFormattedAccountNumber() + '\'' +
                '}';
    }
}
