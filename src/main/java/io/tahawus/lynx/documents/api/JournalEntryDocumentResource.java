package io.tahawus.lynx.documents.api;

import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.model.DocumentStatus;
import io.tahawus.lynx.documents.service.JournalEntryDocumentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

@Path("/journal-entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JournalEntryDocumentResource {

    @Inject
    JournalEntryDocumentService service;

    @POST
    public Response create(@Valid JournalEntryDocumentCreateDto dto, @Context UriInfo uriInfo) {
        JournalEntryDocumentDto created = service.create(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return Response.ok(service.getRequired(id)).build();
    }

    @GET
    public Response list(
            @QueryParam("businessId") Long businessId,
            @QueryParam("status") String status
    ) {
        if (businessId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("businessId is required")).build();
        }

        List<JournalEntryDocumentDto> results;
        if (status != null) {
            results = service.listByBusinessAndStatus(businessId, DocumentStatus.valueOf(status));
        } else {
            results = service.listByBusiness(businessId);
        }
        return Response.ok(results).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid JournalEntryDocumentUpdateDto dto) {
        return Response.ok(service.update(id, dto)).build();
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
