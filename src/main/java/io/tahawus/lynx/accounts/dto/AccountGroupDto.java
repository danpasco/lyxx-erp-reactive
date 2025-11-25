package io.tahawus.lynx.accounts.dto;

import io.tahawus.lynx.accounts.model.AccountType;

/**
 * AccountGroup view DTO.
 */
public record AccountGroupDto(
        Long id,
        Long businessId,
        AccountType accountType,
        Integer groupNumber,
        String name,
        Integer displayOrder,
        Boolean isActive,
        String formattedNumber,
        String fullPath
) {}
