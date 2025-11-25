package io.tahawus.lynx.accounts.service;

import io.tahawus.lynx.accounts.dto.AccountGroupCreateDto;
import io.tahawus.lynx.accounts.dto.AccountGroupDto;
import io.tahawus.lynx.accounts.dto.AccountGroupUpdateDto;
import io.tahawus.lynx.accounts.mapper.AccountGroupMapper;
import io.tahawus.lynx.accounts.model.AccountGroup;
import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.business.model.Business;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Service for AccountGroup business logic.
 */
@ApplicationScoped
public class AccountGroupService {

    public List<AccountGroupDto> listByBusiness(Long businessId) {
        Business business = requireBusiness(businessId);
        return AccountGroupMapper.toDtoList(AccountGroup.listByBusiness(business));
    }

    public List<AccountGroupDto> listByType(Long businessId, AccountType accountType) {
        Business business = requireBusiness(businessId);
        return AccountGroupMapper.toDtoList(AccountGroup.listByType(business, accountType));
    }

    public Optional<AccountGroupDto> get(Long id) {
        return AccountGroup.<AccountGroup>findByIdOptional(id)
                .map(AccountGroupMapper::toDto);
    }

    public AccountGroupDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("AccountGroup not found: " + id));
    }

    @Transactional
    public AccountGroupDto create(AccountGroupCreateDto dto) {
        Business business = requireBusiness(dto.businessId());

        // Check for duplicate
        if (AccountGroup.findByTypeAndNumber(business, dto.accountType(), dto.groupNumber()).isPresent()) {
            throw new IllegalArgumentException(
                    "AccountGroup with type " + dto.accountType() + " and number " + dto.groupNumber() + " already exists");
        }

        AccountGroup group = AccountGroupMapper.fromCreateDto(dto, business);
        group.persist();
        return AccountGroupMapper.toDto(group);
    }

    @Transactional
    public AccountGroupDto update(Long id, AccountGroupUpdateDto dto) {
        AccountGroup group = AccountGroup.findById(id);
        if (group == null) {
            throw new NotFoundException("AccountGroup not found: " + id);
        }

        AccountGroupMapper.applyUpdate(group, dto);
        return AccountGroupMapper.toDto(group);
    }

    @Transactional
    public void delete(Long id) {
        AccountGroup group = AccountGroup.findById(id);
        if (group == null) {
            throw new NotFoundException("AccountGroup not found: " + id);
        }

        // Check for dependent accounts
        long accountCount = GeneralLedgerAccount.count("accountGroup", group);
        if (accountCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete AccountGroup - it has " + accountCount + " account(s)");
        }

        group.delete();
    }

    private Business requireBusiness(Long businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId is required");
        }
        Business business = Business.findById(businessId);
        if (business == null) {
            throw new NotFoundException("Business not found: " + businessId);
        }
        return business;
    }
}
