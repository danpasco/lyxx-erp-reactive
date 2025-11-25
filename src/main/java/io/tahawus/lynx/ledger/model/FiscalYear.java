package io.tahawus.lynx.ledger.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * FiscalYear - Annual accounting period with lifecycle status.
 *
 * Status lifecycle: OPEN → CLOSING → CLOSED (one-way)
 *
 * BUSINESS RULES:
 * - One fiscal year per business per year number
 * - Cannot close until prior year is CLOSED
 * - Cannot close until all FiscalPeriods are CLOSED
 * - During CLOSING, only CE (closing entries) allowed
 * - After CLOSED, balance sheet closing balances become next year's opening balances
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "fiscal_year",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fiscal_year_business_year", columnNames = {"business_id", "year"})
        },
        indexes = {
                @Index(name = "idx_fiscal_year_business", columnList = "business_id"),
                @Index(name = "idx_fiscal_year_status", columnList = "status")
        }
)
public class FiscalYear extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    /**
     * Fiscal year number (e.g., 2024).
     * This is the year the fiscal year is named for, which may differ from
     * calendar year if fiscal year crosses calendar boundary.
     */
    @Column(nullable = false)
    public Integer year;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public FiscalYearStatus status = FiscalYearStatus.OPEN;

    // =============================
    // =   Business Logic          =
    // =============================

    /**
     * Can this fiscal year accept regular (non-closing) journal entries?
     */
    public boolean canAcceptEntries() {
        return status == FiscalYearStatus.OPEN;
    }

    /**
     * Can this fiscal year accept closing entries?
     */
    public boolean canAcceptClosingEntries() {
        return status == FiscalYearStatus.CLOSING;
    }

    /**
     * Check if a date falls within this fiscal year.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // =============================
    // =   Validation              =
    // =============================

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalStateException("FiscalYear start date must be before end date");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<FiscalYear> listByBusiness(Business business) {
        return list("business = ?1 order by year desc", business);
    }

    public static Optional<FiscalYear> findByBusinessAndYear(Business business, Integer year) {
        return find("business = ?1 and year = ?2", business, year).firstResultOptional();
    }

    public static Optional<FiscalYear> findByDate(Business business, LocalDate date) {
        return find("business = ?1 and startDate <= ?2 and endDate >= ?2", business, date)
                .firstResultOptional();
    }

    public static Optional<FiscalYear> findCurrent(Business business) {
        return findByDate(business, LocalDate.now());
    }

    public static Optional<FiscalYear> findFirstOpen(Business business) {
        return find("business = ?1 and status = ?2 order by year asc",
                business, FiscalYearStatus.OPEN).firstResultOptional();
    }

    @Override
    public String toString() {
        return "FiscalYear{" +
                "id=" + id +
                ", year=" + year +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }
}
