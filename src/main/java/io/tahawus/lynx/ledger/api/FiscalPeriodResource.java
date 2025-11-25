package io.tahawus.lynx.ledger.api;

import io.tahawus.lynx.ledger.dto.FiscalPeriodCreateDto;
import io.tahawus.lynx.ledger.dto.FiscalPeriodDto;
import io.tahawus.lynx.ledger.service.FiscalPeriodService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

/**
 * REST API for FiscalPeriod management.
 */
@Path("/fiscal-periods")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FiscalPeriodResource {

    @Inject
    FiscalPeriodService fiscalPeriodService;

    @GET
    public List<FiscalPeriodDto> list(@QueryParam("fiscalYearId") Long fiscalYearId) {
        if (fiscalYearId == null) {
            throw new BadRequestException("fiscalYearId is required");
        }
        return fiscalPeriodService.listByFiscalYear(fiscalYearId);
    }

    @GET
    @Path("/{id}")
    public FiscalPeriodDto get(@PathParam("id") Long id) {
        return fiscalPeriodService.getRequired(id);
    }

    @POST
    public Response create(@Valid FiscalPeriodCreateDto dto, @Context UriInfo uriInfo) {
        FiscalPeriodDto result = fiscalPeriodService.create(dto);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(result.id().toString()).build())
                .entity(result)
                .build();
    }

    /**
     * Create standard monthly periods for a fiscal year.
     */
    @POST
    @Path("/initialize-monthly")
    public List<FiscalPeriodDto> initializeMonthly(@QueryParam("fiscalYearId") Long fiscalYearId) {
        if (fiscalYearId == null) {
            throw new BadRequestException("fiscalYearId is required");
        }
        return fiscalPeriodService.createMonthlyPeriods(fiscalYearId);
    }

    @POST
    @Path("/{id}/close")
    public FiscalPeriodDto close(@PathParam("id") Long id) {
        return fiscalPeriodService.close(id);
    }

    @POST
    @Path("/{id}/reopen")
    public FiscalPeriodDto reopen(@PathParam("id") Long id) {
        return fiscalPeriodService.reopen(id);
    }
}
