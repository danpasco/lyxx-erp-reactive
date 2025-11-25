package io.tahawus.lynx.accounts.service;

import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountCreateDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountUpdateDto;
import io.tahawus.lynx.accounts.mapper.GeneralLedgerAccountMapper;
import io.tahawus.lynx.accounts.model.AccountGroup;
import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.model.GeneralLedgerAccount;
import io.tahawus.lynx.accounts.model.SubsidiaryType;
import io.tahawus.lynx.business.model.Business;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Service for GeneralLedgerAccount business logic.
 */
@ApplicationScoped
public class GeneralLedgerAccountService {

    public List<GeneralLedgerAccountDto> listByBusiness(Long businessId) {
        Business business = requireBusiness(businessId);
        return GeneralLedgerAccountMapper.toDtoList(GeneralLedgerAccount.listByBusiness(business));
    }

    public List<GeneralLedgerAccountDto> listByAccountGroup(Long accountGroupId) {
        AccountGroup group = AccountGroup.findById(accountGroupId);
        if (group == null) {
            throw new NotFoundException("AccountGroup not found: " + accountGroupId);
        }
        return GeneralLedgerAccountMapper.toDtoList(GeneralLedgerAccount.listByAccountGroup(group));
    }

    public List<GeneralLedgerAccountDto> listByAccountType(Long businessId, AccountType accountType) {
        Business business = requireBusiness(businessId);
        return GeneralLedgerAccountMapper.toDtoList(GeneralLedgerAccount.listByAccountType(business, accountType));
    }

    public List<GeneralLedgerAccountDto> listControllingAccounts(Long businessId) {
        Business business = requireBusiness(businessId);
        List<GeneralLedgerAccount> accounts = GeneralLedgerAccount.listByBusiness(business);
        return GeneralLedgerAccountMapper.toDtoList(
                accounts.stream().filter(GeneralLedgerAccount::isControllingAccount).toList()
        );
    }

    public List<GeneralLedgerAccountDto> listBySubsidiaryType(Long businessId, SubsidiaryType subsidiaryType) {
        Business business = requireBusiness(businessId);
        List<GeneralLedgerAccount> accounts = GeneralLedgerAccount.listByBusiness(business);
        return GeneralLedgerAccountMapper.toDtoList(
                accounts.stream().filter(a -> a.subsidiaryType == subsidiaryType).toList()
        );
    }

    public Optional<GeneralLedgerAccountDto> get(Long id) {
        return GeneralLedgerAccount.<GeneralLedgerAccount>findByIdOptional(id)
                .map(GeneralLedgerAccountMapper::toDto);
    }

    public GeneralLedgerAccountDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("GeneralLedgerAccount not found: " + id));
    }

    public Optional<GeneralLedgerAccountDto> findByShortCode(Long businessId, String shortCode) {
        Business business = requireBusiness(businessId);
        return GeneralLedgerAccount.findByShortCode(business, shortCode)
                .map(GeneralLedgerAccountMapper::toDto);
    }

    public List<GeneralLedgerAccountDto> search(Long businessId, String query) {
        Business business = requireBusiness(businessId);

        // Search by short code prefix first
        List<GeneralLedgerAccount> results = GeneralLedgerAccount.searchByShortCode(business, query);
        if (results.isEmpty()) {
            // Fall back to name search
            results = GeneralLedgerAccount.searchByName(business, query);
        }
        return GeneralLedgerAccountMapper.toDtoList(results);
    }

    @Transactional
    public GeneralLedgerAccountDto create(GeneralLedgerAccountCreateDto dto) {
        Business business = requireBusiness(dto.businessId());

        AccountGroup accountGroup = AccountGroup.findById(dto.accountGroupId());
        if (accountGroup == null) {
            throw new IllegalArgumentException("AccountGroup not found: " + dto.accountGroupId());
        }

        // Verify accountGroup belongs to same business
        if (!accountGroup.business.id.equals(business.id)) {
            throw new IllegalArgumentException("AccountGroup does not belong to specified business");
        }

        // Check for duplicate short code
        if (GeneralLedgerAccount.findByShortCode(business, dto.shortCode()).isPresent()) {
            throw new IllegalArgumentException("Account with short code '" + dto.shortCode() + "' already exists");
        }

        GeneralLedgerAccount account = GeneralLedgerAccountMapper.fromCreateDto(dto, business, accountGroup);
        account.persist();
        return GeneralLedgerAccountMapper.toDto(account);
    }

    @Transactional
    public GeneralLedgerAccountDto update(Long id, GeneralLedgerAccountUpdateDto dto) {
        GeneralLedgerAccount account = GeneralLedgerAccount.findById(id);
        if (account == null) {
            throw new NotFoundException("GeneralLedgerAccount not found: " + id);
        }

        // Check short code uniqueness if changing
        if (dto.shortCode() != null && !dto.shortCode().equals(account.shortCode)) {
            if (GeneralLedgerAccount.findByShortCode(account.business, dto.shortCode()).isPresent()) {
                throw new IllegalArgumentException("Account with short code '" + dto.shortCode() + "' already exists");
            }
        }

        GeneralLedgerAccountMapper.applyUpdate(account, dto);
        return GeneralLedgerAccountMapper.toDto(account);
    }

    @Transactional
    public void delete(Long id) {
        GeneralLedgerAccount account = GeneralLedgerAccount.findById(id);
        if (account == null) {
            throw new NotFoundException("GeneralLedgerAccount not found: " + id);
        }

        // TODO: Check for journal entries (ledger module)
        // TODO: Check for subsidiary accounts if this is controlling

        account.delete();
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
