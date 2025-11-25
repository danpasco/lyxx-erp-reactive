package io.tahawus.lynx.documents.mapper;

import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.model.JournalEntryDocument;
import io.tahawus.lynx.documents.model.JournalEntryDocumentLine;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JournalEntryDocumentMapper {

    public JournalEntryDocumentDto toDto(JournalEntryDocument entity) {
        if (entity == null) return null;

        // Fetch account details for lines
        Set<Long> accountIds = entity.lines.stream()
                .map(l -> l.accountId)
                .collect(Collectors.toSet());

        Map<Long, GeneralLedgerAccount> accounts = accountIds.isEmpty()
                ? Map.of()
                : GeneralLedgerAccount.<GeneralLedgerAccount>list("id in ?1", accountIds)
                        .stream()
                        .collect(Collectors.toMap(a -> a.id, a -> a));

        List<JournalEntryDocumentDto.LineDto> lineDtos = entity.lines.stream()
                .map(l -> toLineDto(l, accounts.get(l.accountId)))
                .toList();

        return new JournalEntryDocumentDto(
                entity.id,
                entity.business != null ? entity.business.id : null,
                entity.documentNumber,
                entity.getDocumentType(),
                entity.documentDate,
                entity.status,
                entity.description,
                entity.referenceNumber,
                entity.notes,
                entity.journal != null ? entity.journal.id : null,
                entity.reversingJournal != null ? entity.reversingJournal.id : null,
                entity.createdAt,
                entity.modifiedAt,
                lineDtos,
                entity.getTotalDebits(),
                entity.getTotalCredits(),
                entity.isBalanced()
        );
    }

    public List<JournalEntryDocumentDto> toDtoList(List<JournalEntryDocument> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }

    private JournalEntryDocumentDto.LineDto toLineDto(JournalEntryDocumentLine line, GeneralLedgerAccount account) {
        return new JournalEntryDocumentDto.LineDto(
                line.id,
                line.accountId,
                account != null ? account.shortCode : null,
                account != null ? account.name : null,
                line.amount,
                line.description
        );
    }

    public JournalEntryDocument fromCreateDto(JournalEntryDocumentCreateDto dto, Business business, String documentNumber) {
        JournalEntryDocument entity = new JournalEntryDocument();
        entity.business = business;
        entity.documentNumber = documentNumber;
        entity.documentDate = dto.documentDate();
        entity.description = dto.description();
        entity.referenceNumber = dto.referenceNumber();
        entity.notes = dto.notes();

        for (JournalEntryDocumentCreateDto.LineDto lineDto : dto.lines()) {
            JournalEntryDocumentLine line = new JournalEntryDocumentLine();
            line.accountId = lineDto.accountId();
            line.amount = lineDto.amount();
            line.description = lineDto.description();
            entity.addLine(line);
        }

        return entity;
    }

    public void updateFromDto(JournalEntryDocument entity, JournalEntryDocumentUpdateDto dto) {
        entity.documentDate = dto.documentDate();
        entity.description = dto.description();
        entity.referenceNumber = dto.referenceNumber();
        entity.notes = dto.notes();

        entity.clearLines();
        for (JournalEntryDocumentUpdateDto.LineDto lineDto : dto.lines()) {
            JournalEntryDocumentLine line = new JournalEntryDocumentLine();
            line.accountId = lineDto.accountId();
            line.amount = lineDto.amount();
            line.description = lineDto.description();
            entity.addLine(line);
        }
    }
}
