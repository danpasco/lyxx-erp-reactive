package io.tahawus.lynx.operations.dto;

import io.tahawus.lynx.operations.model.OperationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for DisposalTicket.
 */
public record DisposalTicketDto(
        Long id,
        Long businessId,
        Long ticketNumber,
        LocalDate ticketDate,
        LocalTime ticketTime,
        OperationStatus status,
        Long truckingCompanyId,
        String truckingCompanyName,
        Long oilCompanyId,
        String oilCompanyName,
        String leaseWellNumber,
        BigDecimal bblProduction,
        BigDecimal bblFlowback,
        BigDecimal bblOther,
        BigDecimal totalBarrels,
        String notes,
        Long invoiceId,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime modifiedAt,
        String modifiedBy
) {}
