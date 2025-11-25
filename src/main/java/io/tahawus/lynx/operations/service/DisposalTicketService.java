package io.tahawus.lynx.operations.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;
import io.tahawus.lynx.core.service.NumberSequenceService;
import io.tahawus.lynx.operations.dto.DisposalTicketCreateDto;
import io.tahawus.lynx.operations.dto.DisposalTicketDto;
import io.tahawus.lynx.operations.dto.DisposalTicketUpdateDto;
import io.tahawus.lynx.operations.mapper.DisposalTicketMapper;
import io.tahawus.lynx.operations.model.DisposalTicket;
import io.tahawus.lynx.operations.model.OperationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DisposalTicketService {

    @Inject
    DisposalTicketMapper mapper;

    @Inject
    NumberSequenceService numberSequenceService;

    // =============================
    // =   CRUD Operations         =
    // =============================

    @Transactional
    public DisposalTicketDto create(DisposalTicketCreateDto dto) {
        Business business = Business.findById(dto.businessId());
        if (business == null) {
            throw new NotFoundException("Business not found: " + dto.businessId());
        }

        Contact truckingCompany = dto.truckingCompanyId() != null
                ? Contact.findById(dto.truckingCompanyId())
                : null;

        Contact oilCompany = dto.oilCompanyId() != null
                ? Contact.findById(dto.oilCompanyId())
                : null;

        Long nextNumber = numberSequenceService.getNextNumber(
                dto.businessId(), DisposalTicket.SEQUENCE_KEY);

        DisposalTicket ticket = mapper.fromCreateDto(dto, business, truckingCompany, oilCompany);
        ticket.ticketNumber = nextNumber;
        ticket.persist();

        return mapper.toDto(ticket);
    }

    public Optional<DisposalTicketDto> get(Long id) {
        return DisposalTicket.<DisposalTicket>findByIdOptional(id)
                .map(mapper::toDto);
    }

    public DisposalTicketDto getRequired(Long id) {
        return get(id).orElseThrow(() ->
                new NotFoundException("Disposal ticket not found: " + id));
    }

    public List<DisposalTicketDto> listByBusiness(Long businessId) {
        return DisposalTicket.listByBusinessId(businessId).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<DisposalTicketDto> listByStatus(OperationStatus status) {
        return DisposalTicket.listByStatus(status).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<DisposalTicketDto> listByBusinessAndStatus(Long businessId, OperationStatus status) {
        return DisposalTicket.listByBusinessAndStatus(businessId, status).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<DisposalTicketDto> listReadyToInvoice(Long businessId) {
        return DisposalTicket.listReadyToInvoice(businessId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public DisposalTicketDto update(Long id, DisposalTicketUpdateDto dto) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        if (!ticket.canEdit()) {
            throw new IllegalStateException("Cannot edit ticket in " + ticket.status + " status");
        }

        Contact truckingCompany = dto.truckingCompanyId() != null
                ? Contact.findById(dto.truckingCompanyId())
                : null;

        Contact oilCompany = dto.oilCompanyId() != null
                ? Contact.findById(dto.oilCompanyId())
                : null;

        mapper.applyUpdate(ticket, dto, truckingCompany, oilCompany);

        return mapper.toDto(ticket);
    }

    @Transactional
    public void delete(Long id) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        if (!ticket.canEdit()) {
            throw new IllegalStateException("Cannot delete ticket in " + ticket.status + " status");
        }
        ticket.delete();
    }

    // =============================
    // =   Lifecycle Operations    =
    // =============================

    /**
     * Process the ticket. OPEN → PROCESSED.
     * Marks operational work as complete, ready for invoicing.
     */
    @Transactional
    public DisposalTicketDto process(Long id) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        ticket.process();
        return mapper.toDto(ticket);
    }

    /**
     * Revert ticket to OPEN. PROCESSED → OPEN.
     */
    @Transactional
    public DisposalTicketDto revertToOpen(Long id) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        ticket.revertToOpen();
        return mapper.toDto(ticket);
    }

    /**
     * Void the ticket.
     */
    @Transactional
    public DisposalTicketDto voidTicket(Long id) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        ticket.voidOperation();
        return mapper.toDto(ticket);
    }

    // =============================
    // =   Invoicing               =
    // =============================

    /**
     * Invoice a single ticket. PROCESSED → COMPLETED.
     * Creates an Invoice document from the ticket.
     *
     * @param id Ticket ID
     * @param invoiceCreator Service to create Invoice (from documents module)
     * @return The invoice ID
     */
    @Transactional
    public Long invoice(Long id, DisposalTicket.InvoiceCreator invoiceCreator) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            throw new NotFoundException("Disposal ticket not found: " + id);
        }
        return DisposalTicket.invoice(ticket, invoiceCreator);
    }

    /**
     * Invoice multiple tickets as a summary invoice. PROCESSED → COMPLETED.
     * Creates a single Invoice document from multiple tickets.
     *
     * @param ids Ticket IDs
     * @param invoiceCreator Service to create Invoice (from documents module)
     * @return The invoice ID
     */
    @Transactional
    public Long invoiceBatch(List<Long> ids, DisposalTicket.InvoiceCreator invoiceCreator) {
        List<DisposalTicket> tickets = ids.stream()
                .map(id -> {
                    DisposalTicket ticket = DisposalTicket.findById(id);
                    if (ticket == null) {
                        throw new NotFoundException("Disposal ticket not found: " + id);
                    }
                    return ticket;
                })
                .toList();

        return DisposalTicket.invoice(tickets, invoiceCreator);
    }
}
