package io.tahawus.lynx.ledger.mapper;

import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.ledger.dto.JournalDto;
import io.tahawus.lynx.ledger.dto.JournalLineDto;
import io.tahawus.lynx.ledger.model.Journal;
import io.tahawus.lynx.ledger.model.JournalLine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps between Journal/JournalLine entities and DTOs.
 */
public final class JournalMapper {

    private JournalMapper() {}

    public static JournalDto toDto(Journal journal) {
        if (journal == null) return null;

        // Collect account IDs and fetch accounts for display
        Set<Long> accountIds = journal.lines.stream()
                .map(line -> line.accountId)
                .collect(Collectors.toSet());

        Map<Long, GeneralLedgerAccount> accountMap = GeneralLedgerAccount.<GeneralLedgerAccount>list("id in ?1", accountIds)
                .stream()
                .collect(Collectors.toMap(a -> a.id, a -> a));

        List<JournalLineDto> lineDtos = journal.lines.stream()
                .map(line -> toLineDto(line, accountMap.get(line.accountId)))
                .toList();

        return new JournalDto(
                journal.id,
                journal.business != null ? journal.business.id : null,
                journal.fiscalPeriod != null ? journal.fiscalPeriod.id : null,
                journal.fiscalPeriod != null ? journal.fiscalPeriod.getDisplayName() : null,
                journal.journalType,
                journal.entryDate,
                journal.postingDate,
                journal.documentId,
                journal.reference,
                journal.description,
                journal.isReversing(),
                journal.reversesJournal != null ? journal.reversesJournal.id : null,
                journal.getTotalDebits(),
                journal.getTotalCredits(),
                journal.isBalanced(),
                lineDtos,
                journal.createdAt
        );
    }

    public static List<JournalDto> toDtoList(List<Journal> journals) {
        if (journals == null) return List.of();
        return journals.stream().map(JournalMapper::toDto).toList();
    }

    public static JournalLineDto toLineDto(JournalLine line, GeneralLedgerAccount account) {
        if (line == null) return null;

        return new JournalLineDto(
                line.id,
                line.lineNumber,
                line.accountId,
                account != null ? account.shortCode : null,
                account != null ? account.name : null,
                line.entryType,
                line.amount,
                line.description
        );
    }
}
