package io.tahawus.lynx.operations.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for updating a DisposalTicket.
 * Only OPEN tickets can be updated.
 */
public record DisposalTicketUpdateDto(
        LocalDate ticketDate,
        LocalTime ticketTime,
        Long truckingCompanyId,
        Long oilCompanyId,
        String leaseWellNumber,
        BigDecimal bblProduction,
        BigDecimal bblFlowback,
        BigDecimal bblOther,
        String notes,
        String modifiedBy
) {}
