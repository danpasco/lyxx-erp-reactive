package io.tahawus.lynx.ledger.api;

import io.tahawus.lynx.ledger.dto.FiscalYearCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalYearDto;
import io.tahawus.lynx.ledger.service.FiscalYearService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

/**
 * REST API for FiscalYear management.
 */
@Path("/fiscal-years")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FiscalYearResource {

    @Inject
    FiscalYearService fiscalYearService;

    @GET
    public List<FiscalYearDto> list(@QueryParam("businessId") Long businessId) {
        if (businessId == null) {
            throw new BadRequestException("businessId is required");
        }
        return fiscalYearService.listByBusiness(businessId);
    }

    @GET
    @Path("/{id}")
    public FiscalYearDto get(@PathParam("id") Long id) {
        return fiscalYearService.getRequired(id);
    }

    @POST
    public Response create(@Valid FiscalYearCreateDto dto, @Context UriInfo uriInfo) {
        FiscalYearDto result = fiscalYearService.create(dto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(result.id().toString()).build())
                .entity(result)
                .build();
    }

    @POST
    @Path("/{id}/begin-closing")
    public FiscalYearDto beginClosing(@PathParam("id") Long id) {
        return fiscalYearService.beginClosing(id);
    }

    @POST
    @Path("/{id}/complete-closing")
    public FiscalYearDto completeClosing(@PathParam("id") Long id) {
        return fiscalYearService.completeClosing(id);
    }
}
