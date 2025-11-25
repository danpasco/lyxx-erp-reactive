package io.tahawus.lynx.accounts.mapper;

import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountCreateDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountUpdateDto;
import io.tahawus.lynx.accounts.model.AccountGroup;
import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.accounts.model.SubsidiaryType;
import io.tahawus.lynx.business.model.Business;

import java.util.List;

/**
 * Maps between GeneralLedgerAccount entity and DTOs.
 */
public final class GeneralLedgerAccountMapper {

    private GeneralLedgerAccountMapper() {
    }

    // =============================
    // =   Entity -> DTO           =
    // =============================

    public static GeneralLedgerAccountDto toDto(GeneralLedgerAccount account) {
        if (account == null) {
            return null;
        }

        return new GeneralLedgerAccountDto(
                account.id,
                account.business != null ? account.business.id : null,
                account.accountGroup != null ? account.accountGroup.id : null,
                account.accountGroup != null ? account.accountGroup.name : null,
                account.getAccountType(),
                account.accountNumber,
                account.shortCode,
                account.name,
                account.description,
                account.subsidiaryType,
                account.isActive,
                account.getFormattedAccountNumber(),
                account.getFullPath(),
                account.isControllingAccount(),
                account.isPostingAccount()
        );
    }

    public static List<GeneralLedgerAccountDto> toDtoList(List<GeneralLedgerAccount> accounts) {
        if (accounts == null) {
            return List.of();
        }
        return accounts.stream()
                .map(GeneralLedgerAccountMapper::toDto)
                .toList();
    }

    // =============================
    // =   DTO -> Entity           =
    // =============================

    public static GeneralLedgerAccount fromCreateDto(
            GeneralLedgerAccountCreateDto dto,
            Business business,
            AccountGroup accountGroup
    ) {
        if (dto == null) {
            return null;
        }

        GeneralLedgerAccount account = new GeneralLedgerAccount();
        account.business = business;
        account.accountGroup = accountGroup;
        account.accountNumber = dto.accountNumber();
        account.shortCode = dto.shortCode();
        account.name = dto.name();
        account.description = dto.description();
        account.subsidiaryType = dto.subsidiaryType() != null ? dto.subsidiaryType() : SubsidiaryType.NONE;
        return account;
    }

    public static void applyUpdate(GeneralLedgerAccount account, GeneralLedgerAccountUpdateDto dto) {
        if (dto == null) {
            return;
        }

        if (dto.shortCode() != null) {
            account.shortCode = dto.shortCode();
        }
        if (dto.name() != null) {
            account.name = dto.name();
        }
        if (dto.description() != null) {
            account.description = dto.description();
        }
        if (dto.isActive() != null) {
            account.isActive = dto.isActive();
        }
    }
}
