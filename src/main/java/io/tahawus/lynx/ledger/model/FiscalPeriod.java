package io.tahawus.lynx.ledger.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * FiscalPeriod - Subperiod within a fiscal year (typically months).
 *
 * Soft close: OPEN ↔ CLOSED (reversible while FiscalYear is OPEN)
 *
 * BUSINESS RULES:
 * - Period number unique within fiscal year (1-12 typical, 13 for adjusting)
 * - Periods cannot overlap within fiscal year
 * - Can reopen closed periods while fiscal year is OPEN
 * - Cannot reopen after fiscal year is CLOSED
 *
 * POSTING DATE COMPUTATION:
 * When a journal entry's entryDate falls in a CLOSED period, the postingDate
 * is computed as the first day of the next OPEN period.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "fiscal_period",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fiscal_period_year_number",
                        columnNames = {"fiscal_year_id", "period_number"}
                )
        },
        indexes = {
                @Index(name = "idx_fiscal_period_year", columnList = "fiscal_year_id"),
                @Index(name = "idx_fiscal_period_status", columnList = "status"),
                @Index(name = "idx_fiscal_period_dates", columnList = "start_date, end_date")
        }
)
public class FiscalPeriod extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    public FiscalYear fiscalYear;

    /**
     * Period number within fiscal year (1-12, or 13 for adjusting period).
     */
    @Column(name = "period_number", nullable = false)
    public Integer periodNumber;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public FiscalPeriodStatus status = FiscalPeriodStatus.OPEN;

    // =============================
    // =   Business Logic          =
    // =============================

    /**
     * Can this period accept journal entries?
     * Must be OPEN and fiscal year must be OPEN (or CLOSING for CE entries).
     */
    public boolean canAcceptEntries() {
        return status == FiscalPeriodStatus.OPEN &&
               (fiscalYear.status == FiscalYearStatus.OPEN ||
                fiscalYear.status == FiscalYearStatus.CLOSING);
    }

    /**
     * Check if a date falls within this period.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Get display name (e.g., "January 2024").
     */
    public String getDisplayName() {
        if (periodNumber <= 12) {
            return startDate.getMonth().toString() + " " + fiscalYear.year;
        }
        return "Period " + periodNumber + " " + fiscalYear.year;
    }

    // =============================
    // =   Validation              =
    // =============================

    @PrePersist
    @PreUpdate
    protected void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalStateException("FiscalPeriod start date must be before end date");
        }
        if (periodNumber != null && (periodNumber < 1 || periodNumber > 13)) {
            throw new IllegalStateException("FiscalPeriod number must be between 1 and 13");
        }
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<FiscalPeriod> listByFiscalYear(FiscalYear fiscalYear) {
        return list("fiscalYear = ?1 order by periodNumber", fiscalYear);
    }

    public static Optional<FiscalPeriod> findByFiscalYearAndNumber(FiscalYear fiscalYear, Integer periodNumber) {
        return find("fiscalYear = ?1 and periodNumber = ?2", fiscalYear, periodNumber)
                .firstResultOptional();
    }

    public static Optional<FiscalPeriod> findByDate(FiscalYear fiscalYear, LocalDate date) {
        return find("fiscalYear = ?1 and startDate <= ?2 and endDate >= ?2", fiscalYear, date)
                .firstResultOptional();
    }

    public static List<FiscalPeriod> listOpen(FiscalYear fiscalYear) {
        return list("fiscalYear = ?1 and status = ?2 order by periodNumber",
                fiscalYear, FiscalPeriodStatus.OPEN);
    }

    /**
     * Find the first OPEN period on or after a given date within the business.
     * Used for computing postingDate when entryDate falls in a closed period.
     */
    public static Optional<FiscalPeriod> findFirstOpenOnOrAfter(Business business, LocalDate date) {
        return find("fiscalYear.business = ?1 and status = ?2 and endDate >= ?3 order by startDate asc",
                business, FiscalPeriodStatus.OPEN, date).firstResultOptional();
    }

    @Override
    public String toString() {
        return "FiscalPeriod{" +
                "id=" + id +
                ", fiscalYear=" + (fiscalYear != null ? fiscalYear.year : null) +
                ", periodNumber=" + periodNumber +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }
}
