package io.tahawus.lynx.tenant.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request DTO for tenant onboarding (Phase 2).
 *
 * Phase 1 (admin): Tenant + schema created via TenantResource.provision()
 * Phase 2 (user): Business setup via OnboardingResource.setup()
 *
 * @author Dan Pasco
 */
public record OnboardingRequest(
        // Tenant identifier (to locate the tenant)
        @NotBlank(message = "Tenant identifier is required")
        String tenantIdentifier,

        // Business information
        @NotBlank(message = "Business name is required")
        String businessName,

        String legalName,
        String taxId,
        String email,
        String phone,
        String website,

        // Address
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String country,

        // Accounting defaults
        String baseCurrency,

        // Fiscal year setup
        LocalDate fiscalYearStart,
        LocalDate fiscalYearEnd,

        // Optional: Chart of accounts template
        String chartOfAccountsTemplate
) {
    /**
     * Default constructor for frameworks.
     */
    public OnboardingRequest {
        if (baseCurrency == null || baseCurrency.isBlank()) {
            baseCurrency = "USD";
        }
        if (country == null || country.isBlank()) {
            country = "US";
        }
    }
}
