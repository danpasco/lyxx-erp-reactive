package io.tahawus.lynx.accounts.mapper;

import io.tahawus.lynx.accounts.dto.AccountGroupCreateDto;
import io.tahawus.lynx.accounts.dto.AccountGroupDto;
import io.tahawus.lynx.accounts.dto.AccountGroupUpdateDto;
import io.tahawus.lynx.accounts.model.AccountGroup;
import io.tahawus.lynx.business.model.Business;

import java.util.List;

/**
 * Maps between AccountGroup entity and DTOs.
 */
public final class AccountGroupMapper {

    private AccountGroupMapper() {
    }

    // =============================
    // =   Entity -> DTO           =
    // =============================

    public static AccountGroupDto toDto(AccountGroup group) {
        if (group == null) {
            return null;
        }

        return new AccountGroupDto(
                group.id,
                group.business != null ? group.business.id : null,
                group.accountType,
                group.groupNumber,
                group.name,
                group.displayOrder,
                group.isActive,
                group.getFormattedNumber(),
                group.getFullPath()
        );
    }

    public static List<AccountGroupDto> toDtoList(List<AccountGroup> groups) {
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .map(AccountGroupMapper::toDto)
                .toList();
    }

    // =============================
    // =   DTO -> Entity           =
    // =============================

    public static AccountGroup fromCreateDto(AccountGroupCreateDto dto, Business business) {
        if (dto == null) {
            return null;
        }

        AccountGroup group = new AccountGroup();
        group.business = business;
        group.accountType = dto.accountType();
        group.groupNumber = dto.groupNumber();
        group.name = dto.name();
        group.displayOrder = dto.displayOrder() != null ? dto.displayOrder() : 0;
        return group;
    }

    public static void applyUpdate(AccountGroup group, AccountGroupUpdateDto dto) {
        if (dto == null) {
            return;
        }

        if (dto.name() != null) {
            group.name = dto.name();
        }
        if (dto.displayOrder() != null) {
            group.displayOrder = dto.displayOrder();
        }
        if (dto.isActive() != null) {
            group.isActive = dto.isActive();
        }
    }
}
