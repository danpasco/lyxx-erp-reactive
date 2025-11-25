package io.tahawus.lynx.documents.mapper;

import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.model.ClosingEntryDocument;
import io.tahawus.lynx.documents.model.ClosingEntryDocumentLine;
import io.tahawus.lynx.ledger.model.FiscalYear;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClosingEntryDocumentMapper {

    public ClosingEntryDocumentDto toDto(ClosingEntryDocument entity) {
        if (entity == null) return null;

        Set<Long> accountIds = entity.lines.stream()
                .map(l -> l.accountId)
                .collect(Collectors.toSet());

        Map<Long, GeneralLedgerAccount> accounts = accountIds.isEmpty()
                ? Map.of()
                : GeneralLedgerAccount.<GeneralLedgerAccount>list("id in ?1", accountIds)
                        .stream()
                        .collect(Collectors.toMap(a -> a.id, a -> a));

        List<ClosingEntryDocumentDto.LineDto> lineDtos = entity.lines.stream()
                .map(l -> toLineDto(l, accounts.get(l.accountId)))
                .toList();

        return new ClosingEntryDocumentDto(
                entity.id,
                entity.business != null ? entity.business.id : null,
                entity.documentNumber,
                entity.getDocumentType(),
                entity.documentDate,
                entity.status,
                entity.description,
                entity.notes,
                entity.journal != null ? entity.journal.id : null,
                entity.reversingJournal != null ? entity.reversingJournal.id : null,
                entity.createdAt,
                entity.modifiedAt,
                entity.fiscalYear != null ? entity.fiscalYear.id : null,
                entity.fiscalYear != null ? entity.fiscalYear.year : null,
                lineDtos,
                entity.getTotalDebits(),
                entity.getTotalCredits(),
                entity.isBalanced()
        );
    }

    public List<ClosingEntryDocumentDto> toDtoList(List<ClosingEntryDocument> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }

    private ClosingEntryDocumentDto.LineDto toLineDto(ClosingEntryDocumentLine line, GeneralLedgerAccount account) {
        return new ClosingEntryDocumentDto.LineDto(
                line.id,
                line.accountId,
                account != null ? account.shortCode : null,
                account != null ? account.name : null,
                line.amount,
                line.description
        );
    }

    public ClosingEntryDocument fromCreateDto(
            ClosingEntryDocumentCreateDto dto,
            Business business,
            FiscalYear fiscalYear,
            String documentNumber
    ) {
        ClosingEntryDocument entity = new ClosingEntryDocument();
        entity.business = business;
        entity.documentNumber = documentNumber;
        entity.setFiscalYear(fiscalYear); // Sets documentDate to FY end date
        entity.description = dto.description();
        entity.notes = dto.notes();

        for (ClosingEntryDocumentCreateDto.LineDto lineDto : dto.lines()) {
            ClosingEntryDocumentLine line = new ClosingEntryDocumentLine();
            line.accountId = lineDto.accountId();
            line.amount = lineDto.amount();
            line.description = lineDto.description();
            entity.addLine(line);
        }

        return entity;
    }
}
