package io.tahawus.lynx.tenant.dto;

/**
 * Response DTO for tenant onboarding (Phase 2).
 *
 * @author Dan Pasco
 */
public record OnboardingResponse(
        Long businessId,
        String businessName,
        Long fiscalYearId,
        Integer fiscalYear,
        String message
) {}
