package io.tahawus.lynx.operations.mapper;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.contacts.model.Contact;
import io.tahawus.lynx.operations.dto.DisposalTicketCreateDto;
import io.tahawus.lynx.operations.dto.DisposalTicketDto;
import io.tahawus.lynx.operations.dto.DisposalTicketUpdateDto;
import io.tahawus.lynx.operations.model.DisposalTicket;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DisposalTicketMapper {

    public DisposalTicketDto toDto(DisposalTicket ticket) {
        if (ticket == null) return null;

        return new DisposalTicketDto(
                ticket.id,
                ticket.business != null ? ticket.business.id : null,
                ticket.ticketNumber,
                ticket.ticketDate,
                ticket.ticketTime,
                ticket.status,
                ticket.truckingCompany != null ? ticket.truckingCompany.id : null,
                ticket.truckingCompany != null ? ticket.truckingCompany.name : null,
                ticket.oilCompany != null ? ticket.oilCompany.id : null,
                ticket.oilCompany != null ? ticket.oilCompany.name : null,
                ticket.leaseWellNumber,
                ticket.bblProduction,
                ticket.bblFlowback,
                ticket.bblOther,
                ticket.getTotalBarrels(),
                ticket.notes,
                ticket.invoiceId,
                ticket.createdAt,
                ticket.createdBy,
                ticket.modifiedAt,
                ticket.modifiedBy
        );
    }

    public DisposalTicket fromCreateDto(
            DisposalTicketCreateDto dto,
            Business business,
            Contact truckingCompany,
            Contact oilCompany
    ) {
        DisposalTicket ticket = new DisposalTicket();
        ticket.business = business;
        ticket.ticketDate = dto.ticketDate();
        ticket.ticketTime = dto.ticketTime();
        ticket.truckingCompany = truckingCompany;
        ticket.oilCompany = oilCompany;
        ticket.leaseWellNumber = dto.leaseWellNumber();
        ticket.bblProduction = dto.bblProduction() != null ? dto.bblProduction() : ticket.bblProduction;
        ticket.bblFlowback = dto.bblFlowback() != null ? dto.bblFlowback() : ticket.bblFlowback;
        ticket.bblOther = dto.bblOther() != null ? dto.bblOther() : ticket.bblOther;
        ticket.notes = dto.notes();
        ticket.createdBy = dto.createdBy();
        return ticket;
    }

    public void applyUpdate(
            DisposalTicket ticket,
            DisposalTicketUpdateDto dto,
            Contact truckingCompany,
            Contact oilCompany
    ) {
        if (dto.ticketDate() != null) ticket.ticketDate = dto.ticketDate();
        if (dto.ticketTime() != null) ticket.ticketTime = dto.ticketTime();
        if (truckingCompany != null) ticket.truckingCompany = truckingCompany;
        if (oilCompany != null) ticket.oilCompany = oilCompany;
        if (dto.leaseWellNumber() != null) ticket.leaseWellNumber = dto.leaseWellNumber();
        if (dto.bblProduction() != null) ticket.bblProduction = dto.bblProduction();
        if (dto.bblFlowback() != null) ticket.bblFlowback = dto.bblFlowback();
        if (dto.bblOther() != null) ticket.bblOther = dto.bblOther();
        if (dto.notes() != null) ticket.notes = dto.notes();
        ticket.modifiedBy = dto.modifiedBy();
    }
}
