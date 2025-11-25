package io.tahawus.lynx.ledger.mapper;

import io.tahawus.lynx.ledger.dto.LedgerDto;
import io.tahawus.lynx.ledger.model.Ledger;

import java.util.List;

/**
 * Maps between Ledger entity and DTOs.
 */
public final class LedgerMapper {

    private LedgerMapper() {}

    public static LedgerDto toDto(Ledger ledger) {
        if (ledger == null) return null;

        return new LedgerDto(
                ledger.id,
                ledger.fiscalYear != null ? ledger.fiscalYear.id : null,
                ledger.fiscalYear != null ? ledger.fiscalYear.year : null,
                ledger.account != null ? ledger.account.id : null,
                ledger.account != null ? ledger.account.shortCode : null,
                ledger.account != null ? ledger.account.name : null,
                ledger.openingBalance,
                ledger.getCurrentBalance(),
                ledger.notes,
                ledger.createdAt,
                ledger.modifiedAt
        );
    }

    public static List<LedgerDto> toDtoList(List<Ledger> ledgers) {
        if (ledgers == null) return List.of();
        return ledgers.stream().map(LedgerMapper::toDto).toList();
    }
}
