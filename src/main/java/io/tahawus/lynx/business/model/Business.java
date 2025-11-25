package io.tahawus.lynx.business.model;

import io.tahawus.lynx.contacts.model.Contact;
import io.tahawus.lynx.core.model.LynxPanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business - The primary business entity within a tenant.
 *
 * Represents a legal entity with its accounting configuration. This is the
 * existence/identification dependency for most other entities in the system.
 *
 * While the model supports multiple Business entities per tenant, the current
 * implementation restricts this to a singleton per tenant.
 *
 * MULTI-BOOK SUPPORT:
 * To maintain multiple sets of books for the same legal entity, create
 * multiple Business entities with different basisOfAccounting values:
 * - "Acme Corp" (GAAP)
 * - "Acme Corp" (TAX)
 *
 * Each has its own chart of accounts and fiscal years.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "business",
        indexes = {
                @Index(name = "idx_business_tax_id", columnList = "tax_id"),
                @Index(name = "idx_business_legal_name", columnList = "legal_name")
        }
)
public class Business extends LynxPanacheEntity {

    // =============================
    // =        Attributes         =
    // =============================

    /**
     * Official legal name of the business.
     * Used on legal documents, tax filings, etc.
     * Display name comes from the associated Contact.
     */
    @Column(name = "legal_name", length = 200)
    public String legalName;

    /**
     * Tax Identification Number (EIN, TIN, etc.)
     */
    @Column(name = "tax_id", length = 20)
    public String taxId;

    /**
     * Basis of accounting.
     * Used to distinguish multiple books for same legal entity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "basis_of_accounting", length = 20)
    public BasisOfAccounting basisOfAccounting;

    /**
     * Legal entity type (LLC, Corp, Partnership, Sole Proprietor, etc.)
     */
    @Column(name = "entity_type", length = 50)
    public String entityType;

    /**
     * First month of fiscal year (1-12).
     * Used when generating fiscal years and periods.
     * Example: 1 = Calendar year (Jan-Dec), 7 = Jul-Jun fiscal year
     */
    @Column(name = "fiscal_year_start_month")
    public Integer fiscalYearStartMonth;

    /**
     * Contact record for business address and phone information.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    public Contact contact;

    /**
     * Active status. Inactive businesses are hidden from normal operations.
     */
    @Column(name = "is_active", nullable = false)
    public Boolean isActive = true;

    // =============================
    // =       Logo Storage        =
    // =============================

    @Lob
    @Column(name = "logo_data")
    public byte[] logoData;

    @Column(name = "logo_content_type", length = 100)
    public String logoContentType;

    @Column(name = "logo_file_name", length = 255)
    public String logoFileName;

    // =============================
    // =      Audit Fields         =
    // =============================

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "modified_at")
    public LocalDateTime modifiedAt;

    // =============================
    // =   Lifecycle Callbacks     =
    // =============================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    // =============================
    // =      Helper Methods       =
    // =============================

    public boolean hasLogo() {
        return logoData != null && logoData.length > 0;
    }

    public void setLogo(byte[] data, String contentType, String fileName) {
        this.logoData = data;
        this.logoContentType = contentType;
        this.logoFileName = fileName;
    }

    public void clearLogo() {
        this.logoData = null;
        this.logoContentType = null;
        this.logoFileName = null;
    }

    // =============================
    // =      Query Methods        =
    // =============================

    public static List<Business> listAll() {
        return list("order by name");
    }

    public static List<Business> listActive() {
        return list("isActive = true order by name");
    }

    public static Optional<Business> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static Optional<Business> findByTaxId(String taxId) {
        return find("taxId", taxId).firstResultOptional();
    }

    // =============================
    // =        toString           =
    // =============================

    @Override
    public String toString() {
        return "Business{" +
                "id=" + id +
                ", legalName='" + legalName + '\'' +
                ", taxId='" + taxId + '\'' +
                ", basis=" + basisOfAccounting +
                '}';
    }
}
