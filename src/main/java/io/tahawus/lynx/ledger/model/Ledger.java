package io.tahawus.lynx.ledger.model;

import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Ledger - Links a GeneralLedgerAccount to a FiscalYear with opening balance.
 *
 * The ledger is the central record for account balances by fiscal year.
 * Opening balance is set during onboarding or carried forward from prior year close.
 * Current balance is computed on-demand from journal entries.
 *
 * BALANCE COMPUTATION:
 * currentBalance = openingBalance + SUM(journalLines for this account in this fiscal year)
 *
 * YEAR-END CLOSE:
 * When a FiscalYear is closed, balance sheet account closing balances become
 * the opening balances for the next FiscalYear's Ledger records.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "ledger",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ledger_year_account",
                        columnNames = {"fiscal_year_id", "account_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ledger_fiscal_year", columnList = "fiscal_year_id"),
                @Index(name = "idx_ledger_account", columnList = "account_id")
        }
)
public class Ledger extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    public FiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    public GeneralLedgerAccount account;

    /**
     * Opening balance at the start of the fiscal year.
     * Positive = Debit balance, Negative = Credit balance.
     *
     * Set by:
     * - Onboarding (initial setup)
     * - Year-end close (carry forward from prior year)
     */
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    public BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(length = 500)
    public String notes;

    // =============================
    // =   Balance Computation     =
    // =============================

    /**
     * Compute current balance as of today.
     */
    public BigDecimal getCurrentBalance() {
        return getBalanceAsOf(LocalDate.now());
    }

    /**
     * Compute balance as of a specific date.
     * Balance = openingBalance + SUM(journal lines from year start to date)
     */
    public BigDecimal getBalanceAsOf(LocalDate asOfDate) {
        BigDecimal activity = getActivityThrough(asOfDate);
        return openingBalance.add(activity);
    }

    /**
     * Compute net activity (change) from year start through a date.
     */
    public BigDecimal getActivityThrough(LocalDate throughDate) {
        // Sum all journal lines for this account in this fiscal year through the date
        // DEBIT adds, CREDIT subtracts
        List<JournalLine> lines = JournalLine.list(
                "journal.fiscalPeriod.fiscalYear = ?1 and accountId = ?2 and journal.postingDate <= ?3",
                fiscalYear, account.id, throughDate);

        return lines.stream()
                .map(JournalLine::getSignedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Compute activity within a specific fiscal period.
     */
    public BigDecimal getPeriodActivity(FiscalPeriod period) {
        List<JournalLine> lines = JournalLine.list(
                "journal.fiscalPeriod = ?1 and accountId = ?2",
                period, account.id);

        return lines.stream()
                .map(JournalLine::getSignedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Compute closing balance (end of fiscal year).
     */
    public BigDecimal getClosingBalance() {
        return getBalanceAsOf(fiscalYear.endDate);
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<Ledger> listByFiscalYear(FiscalYear fiscalYear) {
        return list("fiscalYear = ?1 order by account.accountGroup.accountType, account.accountGroup.displayOrder, account.accountNumber",
                fiscalYear);
    }

    public static List<Ledger> listByAccount(GeneralLedgerAccount account) {
        return list("account = ?1 order by fiscalYear.year desc", account);
    }

    public static Optional<Ledger> findByFiscalYearAndAccount(FiscalYear fiscalYear, GeneralLedgerAccount account) {
        return find("fiscalYear = ?1 and account = ?2", fiscalYear, account)
                .firstResultOptional();
    }

    public static List<Ledger> listByBusiness(Business business) {
        return list("fiscalYear.business = ?1 order by fiscalYear.year desc, account.accountNumber",
                business);
    }

    @Override
    public String toString() {
        return "Ledger{" +
                "id=" + id +
                ", fiscalYear=" + (fiscalYear != null ? fiscalYear.year : null) +
                ", account=" + (account != null ? account.shortCode : null) +
                ", openingBalance=" + openingBalance +
                '}';
    }
}
