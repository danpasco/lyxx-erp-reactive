package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.Optional;

/**
 * AccountGroup - Second-level account classification.
 *
 * Provides groupings within AccountTypes for financial statement presentation.
 *
 * EXAMPLES:
 *   AccountType.ASSET → AccountGroup "Current Assets" (10.10)
 *   AccountType.ASSET → AccountGroup "Fixed Assets" (10.20)
 *   AccountType.EXPENSE → AccountGroup "Operating Expenses" (80.10)
 *
 * FORMAT: TT.GG where TT = AccountType number, GG = group number (00-99)
 *
 * BUSINESS RULES:
 * - groupNumber must be unique within business + accountType
 * - groupNumber range: 0-99 (2 digits when formatted)
 * - Deletion check performed in service layer
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "account_group",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_group_number",
                        columnNames = {"business_id", "account_type", "group_number"}
                )
        },
        indexes = {
                @Index(name = "idx_account_group_business", columnList = "business_id"),
                @Index(name = "idx_account_group_type", columnList = "account_type")
        }
)
public class AccountGroup extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    public AccountType accountType;

    /**
     * Group number within type (0-99).
     * Stored as integer, formatted with 2-digit padding.
     */
    @Column(name = "group_number", nullable = false)
    public Integer groupNumber;

    /**
     * Group name.
     * Examples: "Current Assets", "Fixed Assets", "Operating Expenses"
     */
    @Column(name = "name", nullable = false, length = 100)
    public String name;

    /**
     * Display order for financial statements.
     */
    @Column(name = "display_order", nullable = false)
    public Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    // =============================
    // =   Computed Properties     =
    // =============================

    /**
     * Get formatted group number: TT.GG
     */
    public String getFormattedNumber() {
        return String.format("%02d.%02d", accountType.getNumber(), groupNumber);
    }

    /**
     * Get full path with names.
     * Example: "Assets > Current Assets"
     */
    public String getFullPath() {
        return accountType.getDisplayName() + " > " + name;
    }

    // =============================
    // =   Validation              =
    // =============================

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (groupNumber == null || groupNumber < 0 || groupNumber > 99) {
            throw new IllegalStateException("AccountGroup groupNumber must be between 0 and 99");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<AccountGroup> listByBusiness(Business business) {
        return list("business = ?1 and isActive = true order by accountType, displayOrder", business);
    }

    public static List<AccountGroup> listByType(Business business, AccountType accountType) {
        return list("business = ?1 and accountType = ?2 and isActive = true order by displayOrder",
                business, accountType);
    }

    public static Optional<AccountGroup> findByTypeAndNumber(Business business, AccountType accountType, Integer groupNumber) {
        return find("business = ?1 and accountType = ?2 and groupNumber = ?3",
                business, accountType, groupNumber).firstResultOptional();
    }

    public static Optional<AccountGroup> findByFormattedNumber(Business business, String formattedNumber) {
        String[] parts = formattedNumber.split("\\.");
        if (parts.length != 2) {
            return Optional.empty();
        }

        try {
            int typeNumber = Integer.parseInt(parts[0]);
            int groupNum = Integer.parseInt(parts[1]);
            AccountType type = AccountType.fromNumber(typeNumber);
            return findByTypeAndNumber(business, type, groupNum);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "AccountGroup{" +
                "id=" + id +
                ", formattedNumber='" + getFormattedNumber() + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
