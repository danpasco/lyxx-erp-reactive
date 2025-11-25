package io.tahawus.lynx.accounts.service;

import io.tahawus.lynx.accounts.model.*;
import io.tahawus.lynx.business.model.Business;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AccountResolver - Resolves polymorphic Account references.
 *
 * REVERSE RESOLUTION (The UX Magic):
 * Short code or name → Account
 * Examples: "CASH" → GeneralLedgerAccount, "AR-156" → ReceivableAccount
 *
 * FORWARD RESOLUTION:
 * Formatted account number → Account
 * Examples: "10.15.0100" → GeneralLedgerAccount, "10.15.0100.01" → subsidiary
 *
 * SMART RESOLUTION:
 * Detects format and uses appropriate method.
 *
 * @author Dan Pasco
 */
@ApplicationScoped
public class AccountResolver {

    /**
     * SMART RESOLUTION: Auto-detect format and resolve.
     *
     * If input contains dots, tries forward resolution first.
     * Otherwise, tries reverse resolution (short code).
     */
    public Optional<Account> resolve(Business business, String input) {
        if (input == null || input.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmed = input.trim();

        // If it looks like a formatted account number, try forward first
        if (trimmed.contains(".")) {
            Optional<Account> result = resolveByFormattedNumber(business, trimmed);
            if (result.isPresent()) {
                return result;
            }
            // Fall back to short code (some short codes might contain dots)
            return resolveByShortCode(business, trimmed);
        }

        // Otherwise, try reverse (short code)
        return resolveByShortCode(business, trimmed);
    }

    /**
     * REVERSE RESOLUTION: Short code → Account
     *
     * Searches all account types in order:
     * 1. GeneralLedgerAccount (if not controlling)
     * 2. ReceivableAccount
     * 3. PayableAccount
     * 4. BankAccount
     * 5. InventoryAccount
     */
    public Optional<Account> resolveByShortCode(Business business, String shortCode) {
        // Try GeneralLedgerAccount first
        Optional<GeneralLedgerAccount> glAccount = GeneralLedgerAccount.findByShortCode(business, shortCode);
        if (glAccount.isPresent()) {
            GeneralLedgerAccount gl = glAccount.get();
            if (gl.isControllingAccount()) {
                // Can't post to controlling account directly
                return Optional.empty();
            }
            return Optional.of(gl);
        }

        // Try ReceivableAccount
        Optional<ReceivableAccount> receivable = ReceivableAccount.findByShortCode(business, shortCode);
        if (receivable.isPresent()) {
            return Optional.of(receivable.get());
        }

        // Try PayableAccount
        Optional<PayableAccount> payable = PayableAccount.findByShortCode(business, shortCode);
        if (payable.isPresent()) {
            return Optional.of(payable.get());
        }

        // Try BankAccount
        Optional<BankAccount> bank = BankAccount.findByShortCode(business, shortCode);
        if (bank.isPresent()) {
            return Optional.of(bank.get());
        }

        // Try InventoryAccount
        Optional<InventoryAccount> inventory = InventoryAccount.findByShortCode(business, shortCode);
        if (inventory.isPresent()) {
            return Optional.of(inventory.get());
        }

        return Optional.empty();
    }

    /**
     * FORWARD RESOLUTION: Formatted account number → Account
     *
     * Format: TT.GG.AAAA for G/L, TT.GG.AAAA.SS for subsidiary
     */
    public Optional<Account> resolveByFormattedNumber(Business business, String formattedNumber) {
        String[] parts = formattedNumber.split("\\.");

        if (parts.length == 3) {
            // GeneralLedgerAccount: TT.GG.AAAA
            return GeneralLedgerAccount.findByFormattedNumber(business, formattedNumber)
                    .filter(gl -> !gl.isControllingAccount())
                    .map(gl -> (Account) gl);

        } else if (parts.length == 4) {
            // Subsidiary: TT.GG.AAAA.SS
            String controllingFormatted = parts[0] + "." + parts[1] + "." + parts[2];
            int subsidiaryNum;
            try {
                subsidiaryNum = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }

            Optional<GeneralLedgerAccount> controlling =
                    GeneralLedgerAccount.findByFormattedNumber(business, controllingFormatted);

            if (controlling.isEmpty()) {
                return Optional.empty();
            }

            GeneralLedgerAccount ctrl = controlling.get();

            // Find subsidiary based on controlling account's subsidiary type
            return switch (ctrl.subsidiaryType) {
                case RECEIVABLE -> findReceivableByNumber(ctrl, subsidiaryNum);
                case PAYABLE -> findPayableByNumber(ctrl, subsidiaryNum);
                case BANK -> findBankByNumber(ctrl, subsidiaryNum);
                case INVENTORY -> findInventoryByNumber(ctrl, subsidiaryNum);
                case NONE -> Optional.empty();
            };
        }

        return Optional.empty();
    }

    /**
     * SEARCH ALL ACCOUNTS: Find accounts across all types by query string.
     *
     * Searches short codes and names. Results are combined from all account types.
     */
    public List<Account> searchAll(Business business, String query) {
        List<Account> results = new ArrayList<>();

        // Search GeneralLedgerAccounts (posting accounts only)
        List<GeneralLedgerAccount> glResults = GeneralLedgerAccount.searchByShortCode(business, query);
        glResults.addAll(GeneralLedgerAccount.searchByName(business, query));
        glResults.stream()
                .filter(GeneralLedgerAccount::isPostingAccount)
                .distinct()
                .forEach(results::add);

        // Search subsidiaries
        results.addAll(ReceivableAccount.search(business, query));
        results.addAll(PayableAccount.search(business, query));
        results.addAll(BankAccount.search(business, query));
        results.addAll(InventoryAccount.search(business, query));

        return results;
    }

    // =============================
    // =   Helper Methods          =
    // =============================

    private Optional<Account> findReceivableByNumber(GeneralLedgerAccount controlling, int subsidiaryNum) {
        return ReceivableAccount.listByControllingAccount(controlling).stream()
                .filter(r -> r.subsidiaryNumber.equals(subsidiaryNum))
                .findFirst()
                .map(r -> (Account) r);
    }

    private Optional<Account> findPayableByNumber(GeneralLedgerAccount controlling, int subsidiaryNum) {
        return PayableAccount.listByControllingAccount(controlling).stream()
                .filter(p -> p.subsidiaryNumber.equals(subsidiaryNum))
                .findFirst()
                .map(p -> (Account) p);
    }

    private Optional<Account> findBankByNumber(GeneralLedgerAccount controlling, int subsidiaryNum) {
        return BankAccount.listByControllingAccount(controlling).stream()
                .filter(b -> b.subsidiaryNumber.equals(subsidiaryNum))
                .findFirst()
                .map(b -> (Account) b);
    }

    private Optional<Account> findInventoryByNumber(GeneralLedgerAccount controlling, int subsidiaryNum) {
        return InventoryAccount.listByControllingAccount(controlling).stream()
                .filter(i -> i.subsidiaryNumber.equals(subsidiaryNum))
                .findFirst()
                .map(i -> (Account) i);
    }
}
