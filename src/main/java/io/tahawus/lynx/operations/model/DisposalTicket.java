package io.tahawus.lynx.operations.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * DisposalTicket - Operational record of a waste disposal transaction.
 *
 * This is a BusinessOperation (like a SalesOrder) that records a customer
 * delivering waste to be disposed of. It captures operational facts about
 * the service provided.
 *
 * LIFECYCLE:
 * 1. OPEN - Ticket created, editable
 * 2. PROCESSED - Operational work complete (waste received, documented)
 * 3. COMPLETED - Invoice created from ticket
 *
 * Document Creation:
 * - invoice(ticket) - creates Invoice from single ticket
 * - invoice(tickets) - creates summary Invoice from multiple tickets
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "disposal_ticket",
        indexes = {
                @Index(name = "idx_ticket_business", columnList = "business_id"),
                @Index(name = "idx_ticket_number", columnList = "ticket_number"),
                @Index(name = "idx_ticket_date", columnList = "ticket_date"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_trucking", columnList = "trucking_company_id"),
                @Index(name = "idx_ticket_oil", columnList = "oil_company_id"),
                @Index(name = "idx_ticket_invoice", columnList = "invoice_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ticket_number", columnNames = {"business_id", "ticket_number"})
        }
)
public class DisposalTicket extends AuditableEntity implements BusinessOperation {

    public static final String SEQUENCE_KEY = "DISPOSAL_TICKET";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    /**
     * Sequential ticket number (auto-generated per business).
     */
    @Column(name = "ticket_number", nullable = false)
    public Long ticketNumber;

    /**
     * Date the waste was delivered.
     */
    @Column(name = "ticket_date", nullable = false)
    public LocalDate ticketDate;

    /**
     * Time the waste was delivered.
     */
    @Column(name = "ticket_time")
    public LocalTime ticketTime;

    /**
     * Current status of the ticket.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public OperationStatus status = OperationStatus.OPEN;

    /**
     * Trucking company that delivered the waste (hauler/transporter).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trucking_company_id")
    public Contact truckingCompany;

    /**
     * Oil/petroleum company that generated the waste (producer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oil_company_id")
    public Contact oilCompany;

    /**
     * Lease or well number where waste originated.
     */
    @Column(name = "lease_well_number", length = 100)
    public String leaseWellNumber;

    /**
     * Barrels of production water.
     */
    @Column(name = "bbl_production", nullable = false, precision = 10, scale = 2)
    public BigDecimal bblProduction = BigDecimal.ZERO;

    /**
     * Barrels of flowback water.
     */
    @Column(name = "bbl_flowback", nullable = false, precision = 10, scale = 2)
    public BigDecimal bblFlowback = BigDecimal.ZERO;

    /**
     * Barrels of other waste types.
     */
    @Column(name = "bbl_other", nullable = false, precision = 10, scale = 2)
    public BigDecimal bblOther = BigDecimal.ZERO;

    /**
     * Reference to the Invoice created from this ticket (if any).
     */
    @Column(name = "invoice_id")
    public Long invoiceId;

    /**
     * Optional notes or additional information.
     */
    @Column(length = 2000)
    public String notes;

    // =============================
    // =   BusinessOperation       =
    // =============================

    @Override
    public Business getBusiness() {
        return business;
    }

    @Override
    public OperationStatus getStatus() {
        return status;
    }

    @Override
    public void validate() {
        if (business == null) {
            throw new IllegalStateException("Business is required");
        }
        if (ticketDate == null) {
            throw new IllegalStateException("Ticket date is required");
        }
        if (ticketNumber == null) {
            throw new IllegalStateException("Ticket number is required");
        }

        BigDecimal total = getTotalBarrels();
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Total barrels must be greater than zero");
        }
    }

    @Override
    public void process() {
        if (!canProcess()) {
            throw new IllegalStateException("Cannot process ticket in " + status + " status");
        }
        validate();
        this.status = OperationStatus.PROCESSED;
    }

    @Override
    public void revertToOpen() {
        if (!canRevertToOpen()) {
            throw new IllegalStateException("Cannot revert ticket in " + status + " status");
        }
        this.status = OperationStatus.OPEN;
    }

    @Override
    public void voidOperation() {
        if (!canVoid()) {
            throw new IllegalStateException("Cannot void ticket in " + status + " status");
        }
        this.status = OperationStatus.VOIDED;
    }

    // =============================
    // =   Document Creation       =
    // =============================

    /**
     * Create an Invoice from a single DisposalTicket.
     *
     * @param ticket The ticket to invoice
     * @param invoiceService Service to create the Invoice
     * @return The created Invoice ID
     */
    public static Long invoice(DisposalTicket ticket, InvoiceCreator invoiceService) {
        if (!ticket.canComplete()) {
            throw new IllegalStateException(
                    "Cannot invoice ticket in " + ticket.status + " status");
        }
        if (ticket.invoiceId != null) {
            throw new IllegalStateException(
                    "Ticket already invoiced. Invoice ID: " + ticket.invoiceId);
        }

        Long invoiceId = invoiceService.createInvoice(ticket);

        ticket.invoiceId = invoiceId;
        ticket.status = OperationStatus.COMPLETED;

        return invoiceId;
    }

    /**
     * Create a summary Invoice from multiple DisposalTickets.
     * All tickets must be for the same customer.
     *
     * @param tickets The tickets to invoice
     * @param invoiceService Service to create the Invoice
     * @return The created Invoice ID
     */
    public static Long invoice(List<DisposalTicket> tickets, InvoiceCreator invoiceService) {
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalArgumentException("At least one ticket is required");
        }

        // Validate all tickets
        for (DisposalTicket ticket : tickets) {
            if (!ticket.canComplete()) {
                throw new IllegalStateException(
                        "Ticket " + ticket.ticketNumber + " cannot be invoiced. Status: " + ticket.status);
            }
            if (ticket.invoiceId != null) {
                throw new IllegalStateException(
                        "Ticket " + ticket.ticketNumber + " already invoiced");
            }
        }

        Long invoiceId = invoiceService.createSummaryInvoice(tickets);

        // Link all tickets to the invoice
        for (DisposalTicket ticket : tickets) {
            ticket.invoiceId = invoiceId;
            ticket.status = OperationStatus.COMPLETED;
        }

        return invoiceId;
    }

    /**
     * Interface for invoice creation.
     * Implemented by the documents module.
     */
    public interface InvoiceCreator {
        Long createInvoice(DisposalTicket ticket);
        Long createSummaryInvoice(List<DisposalTicket> tickets);
    }

    // =============================
    // =   Business Logic          =
    // =============================

    /**
     * Calculate total barrels across all types.
     */
    public BigDecimal getTotalBarrels() {
        BigDecimal production = bblProduction != null ? bblProduction : BigDecimal.ZERO;
        BigDecimal flowback = bblFlowback != null ? bblFlowback : BigDecimal.ZERO;
        BigDecimal other = bblOther != null ? bblOther : BigDecimal.ZERO;
        return production.add(flowback).add(other);
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<DisposalTicket> listByBusiness(Business business) {
        return list("business = ?1 order by ticketNumber desc", business);
    }

    public static List<DisposalTicket> listByBusinessId(Long businessId) {
        return list("business.id = ?1 order by ticketNumber desc", businessId);
    }

    public static List<DisposalTicket> listByStatus(OperationStatus status) {
        return list("status = ?1 order by ticketNumber desc", status);
    }

    public static List<DisposalTicket> listByBusinessAndStatus(Long businessId, OperationStatus status) {
        return list("business.id = ?1 and status = ?2 order by ticketNumber desc",
                businessId, status);
    }

    public static List<DisposalTicket> listProcessed(Long businessId) {
        return listByBusinessAndStatus(businessId, OperationStatus.PROCESSED);
    }

    public static List<DisposalTicket> listReadyToInvoice(Long businessId) {
        return listProcessed(businessId);
    }

    public static Optional<DisposalTicket> findByTicketNumber(Long businessId, Long ticketNumber) {
        return find("business.id = ?1 and ticketNumber = ?2", businessId, ticketNumber)
                .firstResultOptional();
    }

    public static List<DisposalTicket> findByDateRange(Long businessId, LocalDate startDate, LocalDate endDate) {
        return list("business.id = ?1 and ticketDate >= ?2 and ticketDate <= ?3 order by ticketNumber desc",
                businessId, startDate, endDate);
    }

    public static List<DisposalTicket> findByContact(Contact contact) {
        return list("truckingCompany = ?1 or oilCompany = ?1 order by ticketNumber desc", contact);
    }

    public static List<DisposalTicket> findByInvoice(Long invoiceId) {
        return list("invoiceId = ?1 order by ticketNumber desc", invoiceId);
    }

    @Override
    public String toString() {
        return "DisposalTicket{" +
                "id=" + id +
                ", ticketNumber=" + ticketNumber +
                ", ticketDate=" + ticketDate +
                ", status=" + status +
                ", totalBarrels=" + getTotalBarrels() +
                '}';
    }
}
