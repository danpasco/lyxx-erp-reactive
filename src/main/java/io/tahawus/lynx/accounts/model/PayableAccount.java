package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PayableAccount - Vendor Accounts Payable subsidiary.
 *
 * Implements Account interface for A/P postings.
 *
 * FORMAT: TT.GG.AAAA.SS
 * Example: "20.10.0100.01" (Liabilities > Current > A/P Control > XYZ Vendor)
 *
 * BUSINESS RULES:
 * - Must have controlling GeneralLedgerAccount with subsidiaryType = PAYABLE
 * - One PayableAccount per vendor per controlling account
 * - subsidiaryNumber range: 0-99 (2 digits when formatted)
 * - shortCode must be unique within business
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "payable_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payable_subsidiary_number",
                        columnNames = {"controlling_account_id", "subsidiary_number"}
                ),
                @UniqueConstraint(
                        name = "uk_payable_contact",
                        columnNames = {"controlling_account_id", "contact_id"}
                ),
                @UniqueConstraint(
                        name = "uk_payable_short_code",
                        columnNames = {"controlling_account_id", "short_code"}
                )
        },
        indexes = {
                @Index(name = "idx_payable_controlling", columnList = "controlling_account_id"),
                @Index(name = "idx_payable_contact", columnList = "contact_id"),
                @Index(name = "idx_payable_short_code", columnList = "short_code")
        }
)
public class PayableAccount extends AuditableEntity implements Account {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controlling_account_id", nullable = false)
    public GeneralLedgerAccount controllingAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    public Contact contact;

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

    @Column(name = "credit_limit")
    public BigDecimal creditLimit;

    @Column(name = "payment_terms_days")
    public Integer paymentTermsDays;

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
        return shortCode + " - " + contact.name + " (A/P)";
    }

    @Override
    public String getFormattedAccountNumber() {
        return controllingAccount.getFormattedAccountNumber() + "." + String.format("%02d", subsidiaryNumber);
    }

    @Override
    public String getFullPath() {
        return controllingAccount.getFullPath() + " > " + contact.name;
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
            throw new IllegalStateException("PayableAccount subsidiaryNumber must be between 0 and 99");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<PayableAccount> listByBusiness(Business business) {
        return list("controllingAccount.business = ?1 and isActive = true order by controllingAccount.accountNumber, subsidiaryNumber",
                business);
    }

    public static List<PayableAccount> listByControllingAccount(GeneralLedgerAccount controllingAccount) {
        return list("controllingAccount = ?1 and isActive = true order by subsidiaryNumber", controllingAccount);
    }

    public static List<PayableAccount> listByContact(Contact contact) {
        return list("contact = ?1 and isActive = true", contact);
    }

    public static Optional<PayableAccount> findByShortCode(Business business, String shortCode) {
        return find("controllingAccount.business = ?1 and shortCode = ?2 and isActive = true", business, shortCode)
                .firstResultOptional();
    }

    public static List<PayableAccount> search(Business business, String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return list("controllingAccount.business = ?1 and isActive = true and (lower(shortCode) like ?2 or lower(contact.name) like ?2) order by shortCode",
                business, pattern);
    }

    @Override
    public String toString() {
        return "PayableAccount{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", contact=" + (contact != null ? contact.name : null) +
                ", formattedNumber='" + getFormattedAccountNumber() + '\'' +
                '}';
    }
}
