package io.tahawus.lynx.tenant.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.ledger.model.FiscalYear;
import io.tahawus.lynx.ledger.model.FiscalYearStatus;
import io.tahawus.lynx.tenant.dto.OnboardingRequest;
import io.tahawus.lynx.tenant.dto.OnboardingResponse;
import io.tahawus.lynx.tenant.model.Tenant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;

/**
 * Service for tenant onboarding (Phase 2).
 *
 * After the tenant schema is created (Phase 1), this service
 * sets up the initial content within the tenant's schema:
 * - Business entity
 * - Default chart of accounts
 * - Initial fiscal year
 *
 * @author Dan Pasco
 */
@ApplicationScoped
public class OnboardingService {

    private static final Logger LOG = Logger.getLogger(OnboardingService.class);

    /**
     * Create initial tenant content.
     *
     * This runs WITHIN the tenant's schema context
     * (X-Tenant-Id header must be set, or called with schema context).
     *
     * @param tenant The tenant entity from public schema
     * @param request Onboarding details
     * @return Response with created entity IDs
     */
    @Transactional
    public OnboardingResponse createTenantContent(Tenant tenant, OnboardingRequest request) {
        LOG.infof("Onboarding tenant: %s", tenant.tenantIdentifier);

        // Create Business
        Business business = createBusiness(request);
        LOG.infof("Business created: %d - %s", business.id, business.legalName);

        // Create initial fiscal year
        FiscalYear fiscalYear = createInitialFiscalYear(business, request);
        LOG.infof("Fiscal year created: %d - %d", fiscalYear.id, fiscalYear.year);

        // TODO: Create default chart of accounts based on industry/template
        // This would call AccountGroupService and GeneralLedgerAccountService
        // to set up standard accounts (Cash, AR, AP, Revenue, etc.)

        LOG.infof("Onboarding complete for tenant: %s", tenant.tenantIdentifier);

        return new OnboardingResponse(
                business.id,
                business.legalName,
                fiscalYear.id,
                fiscalYear.year,
                "Onboarding complete"
        );
    }

    private Business createBusiness(OnboardingRequest request) {
        Business business = new Business();
        business.legalName = request.businessName();
        business.legalName = request.legalName() != null ? request.legalName() : request.businessName();
        business.taxId = request.taxId();
//        business.email = request.email();
//        business.phone = request.phone();
//        business.website = request.website();
//
//        // Address
//        business.addressLine1 = request.addressLine1();
//        business.addressLine2 = request.addressLine2();
//        business.city = request.city();
//        business.stateProvince = request.stateProvince();
//        business.postalCode = request.postalCode();
//        business.country = request.country() != null ? request.country() : "US";

        // Defaults
//        business.baseCurrency = request.baseCurrency() != null ? request.baseCurrency() : "USD";
        business.isActive = true;

        business.persist();
        return business;
    }

    private FiscalYear createInitialFiscalYear(Business business, OnboardingRequest request) {
        int year = request.fiscalYearStart() != null
                ? request.fiscalYearStart().getYear()
                : LocalDate.now().getYear();

        LocalDate startDate = request.fiscalYearStart() != null
                ? request.fiscalYearStart()
                : LocalDate.of(year, 1, 1);

        LocalDate endDate = request.fiscalYearEnd() != null
                ? request.fiscalYearEnd()
                : startDate.plusYears(1).minusDays(1);

        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.business = business;
        fiscalYear.year = year;
        fiscalYear.startDate = startDate;
        fiscalYear.endDate = endDate;
        fiscalYear.status = FiscalYearStatus.OPEN;

        fiscalYear.persist();

        // Create fiscal periods (monthly by default)
//        fiscalYear.generateMonthlyPeriods();

        return fiscalYear;
    }
}
