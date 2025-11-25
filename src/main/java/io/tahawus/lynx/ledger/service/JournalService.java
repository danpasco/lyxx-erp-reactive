package io.tahawus.lynx.ledger.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.ledger.dto.JournalCreateDto;
import io.tahawus.lynx.ledger.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.Optional;

/**
 * JournalService - The ONE entry point for Journal creation.
 *
 * Documents module calls create() to post entries to the ledger.
 * This service handles:
 * - Fiscal period lookup
 * - Posting date computation
 * - Validation (balanced, period open, CE rules)
 * - Persistence
 *
 * @author Dan Pasco
 */
@ApplicationScoped
public class JournalService {

    /**
     * Create a journal entry.
     *
     * This is the ONLY way to create a Journal.
     *
     * @param dto Journal creation data from a document
     * @return The created Journal
     * @throws IllegalStateException if validation fails
     * @throws NotFoundException if business or referenced entities not found
     */
    @Transactional
    public Journal create(JournalCreateDto dto) {
        // Find business
        Business business = Business.findById(dto.businessId());
        if (business == null) {
            throw new NotFoundException("Business not found: " + dto.businessId());
        }

        // Find fiscal period for posting
        FiscalPeriod fiscalPeriod = findFiscalPeriodForPosting(business, dto.entryDate());

        // Validate CE constraint
        if (dto.journalType() == JournalType.CE) {
            if (fiscalPeriod.fiscalYear.status != FiscalYearStatus.CLOSING) {
                throw new IllegalStateException(
                        "Closing entries (CE) only allowed when fiscal year is CLOSING. " +
                        "Current status: " + fiscalPeriod.fiscalYear.status);
            }
        }

        // Validate balance
        if (!dto.isBalanced()) {
            throw new IllegalStateException(
                    "Journal entry must be balanced. Debits: " + dto.getTotalDebits() +
                    ", Credits: " + dto.getTotalCredits());
        }

        // Create journal
        Journal journal = new Journal();
        journal.business = business;
        journal.fiscalPeriod = fiscalPeriod;
        journal.journalType = dto.journalType();
        journal.entryDate = dto.entryDate();
        journal.postingDate = fiscalPeriod.startDate;
        journal.documentId = dto.documentId();
        journal.reference = dto.reference();
        journal.description = dto.description();

        // Handle reversing journal reference
        if (dto.reversesJournalId() != null) {
            Journal reversesJournal = Journal.findById(dto.reversesJournalId());
            if (reversesJournal == null) {
                throw new NotFoundException("Reverses journal not found: " + dto.reversesJournalId());
            }
            journal.reversesJournal = reversesJournal;
        }

        // Add lines
        int lineNumber = 1;
        for (JournalCreateDto.Line line : dto.lines()) {
            journal.addLine(
                    lineNumber++,
                    line.accountId(),
                    line.entryType(),
                    line.amount(),
                    line.description()
            );
        }

        // Persist (triggers @PrePersist validation in Journal)
        journal.persist();

        return journal;
    }

    /**
     * Find the fiscal period for posting.
     * Returns the first OPEN period on or after the entry date.
     */
    private FiscalPeriod findFiscalPeriodForPosting(Business business, java.time.LocalDate entryDate) {
        Optional<FiscalPeriod> periodOpt = FiscalPeriod.findFirstOpenOnOrAfter(business, entryDate);

        if (periodOpt.isEmpty()) {
            throw new IllegalStateException(
                    "No open fiscal period found on or after " + entryDate +
                    ". Ensure fiscal year and periods are set up.");
        }

        FiscalPeriod period = periodOpt.get();

        if (!period.canAcceptEntries()) {
            throw new IllegalStateException(
                    "Fiscal period " + period.getDisplayName() + " cannot accept entries. " +
                    "Period status: " + period.status + ", Year status: " + period.fiscalYear.status);
        }

        return period;
    }
}
