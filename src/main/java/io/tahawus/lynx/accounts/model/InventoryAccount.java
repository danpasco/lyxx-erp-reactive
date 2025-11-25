package io.tahawus.lynx.accounts.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * InventoryAccount - Inventory item subsidiary account.
 *
 * Implements Account interface for inventory postings.
 *
 * FORMAT: TT.GG.AAAA.SS
 * Example: "10.20.0100.01" (Assets > Inventory > Inventory Control > Widget A)
 *
 * BUSINESS RULES:
 * - Must have controlling GeneralLedgerAccount with subsidiaryType = INVENTORY
 * - subsidiaryNumber range: 0-99 (2 digits when formatted)
 * - shortCode must be unique within business
 *
 * NOTE: This is a placeholder implementation. Inventory-specific fields
 * (SKU, UOM, standard cost, etc.) will be added as the inventory module develops.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "inventory_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_subsidiary_number",
                        columnNames = {"controlling_account_id", "subsidiary_number"}
                ),
                @UniqueConstraint(
                        name = "uk_inventory_short_code",
                        columnNames = {"controlling_account_id", "short_code"}
                )
        },
        indexes = {
                @Index(name = "idx_inventory_controlling", columnList = "controlling_account_id"),
                @Index(name = "idx_inventory_short_code", columnList = "short_code")
        }
)
public class InventoryAccount extends AuditableEntity implements Account {

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
     * Item name.
     */
    @Column(nullable = false, length = 200)
    public String name;

    @Column(length = 500)
    public String description;

    // =============================
    // =   Inventory-Specific      =
    // =============================

    /**
     * Stock Keeping Unit.
     */
    @Column(length = 50)
    public String sku;

    /**
     * Unit of Measure (EACH, BOX, LB, GAL, etc.)
     */
    @Column(length = 20)
    public String uom;

    /**
     * Standard cost per unit.
     */
    @Column(name = "standard_cost", precision = 19, scale = 4)
    public BigDecimal standardCost;

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
        return shortCode + " - " + name + " (Inv)";
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
            throw new IllegalStateException("InventoryAccount subsidiaryNumber must be between 0 and 99");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<InventoryAccount> listByBusiness(Business business) {
        return list("controllingAccount.business = ?1 and isActive = true order by controllingAccount.accountNumber, subsidiaryNumber",
                business);
    }

    public static List<InventoryAccount> listByControllingAccount(GeneralLedgerAccount controllingAccount) {
        return list("controllingAccount = ?1 and isActive = true order by subsidiaryNumber", controllingAccount);
    }

    public static Optional<InventoryAccount> findByShortCode(Business business, String shortCode) {
        return find("controllingAccount.business = ?1 and shortCode = ?2 and isActive = true", business, shortCode)
                .firstResultOptional();
    }

    public static Optional<InventoryAccount> findBySku(Business business, String sku) {
        return find("controllingAccount.business = ?1 and sku = ?2 and isActive = true", business, sku)
                .firstResultOptional();
    }

    public static List<InventoryAccount> search(Business business, String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return list("controllingAccount.business = ?1 and isActive = true and (lower(shortCode) like ?2 or lower(name) like ?2 or lower(sku) like ?2) order by shortCode",
                business, pattern);
    }

    @Override
    public String toString() {
        return "InventoryAccount{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                ", formattedNumber='" + getFormattedAccountNumber() + '\'' +
                '}';
    }
}
