package io.tahawus.lynx.accounts.api;

import io.tahawus.lynx.accounts.dto.AccountGroupCreateDto;
import io.tahawus.lynx.accounts.dto.AccountGroupDto;
import io.tahawus.lynx.accounts.dto.AccountGroupUpdateDto;
import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.service.AccountGroupService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

/**
 * REST API for AccountGroup management.
 *
 * AccountGroups are second-level classifications within AccountTypes.
 * Example: AccountType.ASSET contains groups like "Current Assets", "Fixed Assets"
 */
@Path("/account-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountGroupResource {

    @Inject
    AccountGroupService accountGroupService;

    @GET
    public List<AccountGroupDto> list(
            @QueryParam("businessId") Long businessId,
            @QueryParam("accountType") AccountType accountType) {

        if (businessId == null) {
            throw new BadRequestException("businessId is required");
        }

        if (accountType != null) {
            return accountGroupService.listByType(businessId, accountType);
        }
        return accountGroupService.listByBusiness(businessId);
    }

    @GET
    @Path("/{id}")
    public AccountGroupDto get(@PathParam("id") Long id) {
        return accountGroupService.getRequired(id);
    }

    @POST
    public Response create(@Valid AccountGroupCreateDto dto, @Context UriInfo uriInfo) {
        AccountGroupDto result = accountGroupService.create(dto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(result.id().toString()).build())
                .entity(result)
                .build();
    }

    @PUT
    @Path("/{id}")
    public AccountGroupDto update(@PathParam("id") Long id, @Valid AccountGroupUpdateDto dto) {
        return accountGroupService.update(id, dto);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        accountGroupService.delete(id);
        return Response.noContent().build();
    }
}
