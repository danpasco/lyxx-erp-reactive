package io.tahawus.lynx.operations.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a DisposalTicket.
 */
public record DisposalTicketCreateDto(
        @NotNull(message = "Business ID is required")
        Long businessId,

        @NotNull(message = "Ticket date is required")
        LocalDate ticketDate,

        LocalTime ticketTime,
        Long truckingCompanyId,
        Long oilCompanyId,
        String leaseWellNumber,
        BigDecimal bblProduction,
        BigDecimal bblFlowback,
        BigDecimal bblOther,
        String notes,
        String createdBy
) {}
