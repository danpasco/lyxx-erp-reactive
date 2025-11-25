package io.tahawus.lynx.documents.api;

import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.service.ClosingEntryDocumentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

@Path("/closing-entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClosingEntryDocumentResource {

    @Inject
    ClosingEntryDocumentService service;

    @POST
    public Response create(@Valid ClosingEntryDocumentCreateDto dto, @Context UriInfo uriInfo) {
        ClosingEntryDocumentDto created = service.create(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return Response.ok(service.getRequired(id)).build();
    }

    @GET
    public Response list(@QueryParam("fiscalYearId") Long fiscalYearId) {
        if (fiscalYearId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("fiscalYearId is required")).build();
        }
        return Response.ok(service.listByFiscalYear(fiscalYearId)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/complete")
    public Response complete(@PathParam("id") Long id) {
        return Response.ok(service.complete(id)).build();
    }

    @POST
    @Path("/{id}/revert")
    public Response revert(@PathParam("id") Long id) {
        return Response.ok(service.revert(id)).build();
    }

    @POST
    @Path("/{id}/post")
    public Response post(@PathParam("id") Long id) {
        return Response.ok(service.post(id)).build();
    }

    public record ErrorResponse(String error) {}
}
