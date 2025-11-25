package io.tahawus.lynx.accounts.dto;

import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.model.SubsidiaryType;

/**
 * GeneralLedgerAccount view DTO.
 */
public record GeneralLedgerAccountDto(
        Long id,
        Long businessId,
        Long accountGroupId,
        String accountGroupName,
        AccountType accountType,
        Integer accountNumber,
        String shortCode,
        String name,
        String description,
        SubsidiaryType subsidiaryType,
        Boolean isActive,
        String formattedAccountNumber,
        String fullPath,
        boolean isControllingAccount,
        boolean isPostingAccount
) {}
