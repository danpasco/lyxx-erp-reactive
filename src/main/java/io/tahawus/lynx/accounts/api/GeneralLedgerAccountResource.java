package io.tahawus.lynx.accounts.api;

import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountCreateDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountDto;
import io.tahawus.lynx.accounts.dto.GeneralLedgerAccountUpdateDto;
import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.model.SubsidiaryType;
import io.tahawus.lynx.accounts.service.GeneralLedgerAccountService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

/**
 * REST API for GeneralLedgerAccount management.
 *
 * GeneralLedgerAccounts are the primary accounts in the chart of accounts.
 * They may be posting accounts (subsidiaryType = NONE) or controlling
 * accounts for subsidiary ledgers (A/R, A/P, Bank, Inventory).
 */
@Path("/gl-accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GeneralLedgerAccountResource {

    @Inject
    GeneralLedgerAccountService glAccountService;

    @GET
    public List<GeneralLedgerAccountDto> list(
            @QueryParam("businessId") Long businessId,
            @QueryParam("accountGroupId") Long accountGroupId,
            @QueryParam("accountType") AccountType accountType,
            @QueryParam("subsidiaryType") SubsidiaryType subsidiaryType,
            @QueryParam("controlling") Boolean controlling) {

        if (businessId == null) {
            throw new BadRequestException("businessId is required");
        }

        List<GeneralLedgerAccountDto> accounts;

        if (accountGroupId != null) {
            accounts = glAccountService.listByAccountGroup(accountGroupId);
        } else if (accountType != null) {
            accounts = glAccountService.listByAccountType(businessId, accountType);
        } else if (subsidiaryType != null) {
            accounts = glAccountService.listBySubsidiaryType(businessId, subsidiaryType);
        } else if (Boolean.TRUE.equals(controlling)) {
            accounts = glAccountService.listControllingAccounts(businessId);
        } else {
            accounts = glAccountService.listByBusiness(businessId);
        }

        return accounts;
    }

    @GET
    @Path("/{id}")
    public GeneralLedgerAccountDto get(@PathParam("id") Long id) {
        return glAccountService.getRequired(id);
    }

    @GET
    @Path("/by-short-code")
    public GeneralLedgerAccountDto getByShortCode(
            @QueryParam("businessId") Long businessId,
            @QueryParam("shortCode") String shortCode) {

        if (businessId == null || shortCode == null) {
            throw new BadRequestException("businessId and shortCode are required");
        }

        return glAccountService.findByShortCode(businessId, shortCode)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    @GET
    @Path("/search")
    public List<GeneralLedgerAccountDto> search(
            @QueryParam("businessId") Long businessId,
            @QueryParam("q") String query) {

        if (businessId == null || query == null || query.isBlank()) {
            throw new BadRequestException("businessId and q are required");
        }

        return glAccountService.search(businessId, query);
    }

    @POST
    public Response create(@Valid GeneralLedgerAccountCreateDto dto, @Context UriInfo uriInfo) {
        GeneralLedgerAccountDto result = glAccountService.create(dto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(result.id().toString()).build())
                .entity(result)
                .build();
    }

    @PUT
    @Path("/{id}")
    public GeneralLedgerAccountDto update(@PathParam("id") Long id, @Valid GeneralLedgerAccountUpdateDto dto) {
        return glAccountService.update(id, dto);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        glAccountService.delete(id);
        return Response.noContent().build();
    }
}
