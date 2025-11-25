package io.tahawus.lynx.operations.api;

import io.tahawus.lynx.operations.dto.DisposalTicketCreateDto;
import io.tahawus.lynx.operations.dto.DisposalTicketDto;
import io.tahawus.lynx.operations.dto.DisposalTicketUpdateDto;
import io.tahawus.lynx.operations.model.DisposalTicket;
import io.tahawus.lynx.operations.model.OperationStatus;
import io.tahawus.lynx.operations.service.DisposalTicketPdfService;
import io.tahawus.lynx.operations.service.DisposalTicketService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Path("/disposal-tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DisposalTicketResource {

    @Inject
    DisposalTicketService service;

    @Inject
    DisposalTicketPdfService pdfService;

    @POST
    public Response create(@Valid DisposalTicketCreateDto dto, @Context UriInfo uriInfo) {
        DisposalTicketDto created = service.create(dto);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.id().toString()).build();
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
            @QueryParam("status") String statusParam
    ) {
        List<DisposalTicketDto> tickets;

        if (businessId != null && statusParam != null) {
            OperationStatus status = OperationStatus.valueOf(statusParam.toUpperCase());
            tickets = service.listByBusinessAndStatus(businessId, status);
        } else if (businessId != null) {
            tickets = service.listByBusiness(businessId);
        } else if (statusParam != null) {
            OperationStatus status = OperationStatus.valueOf(statusParam.toUpperCase());
            tickets = service.listByStatus(status);
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("businessId or status required"))
                    .build();
        }

        return Response.ok(tickets).build();
    }

    @GET
    @Path("/ready-to-invoice")
    public Response listReadyToInvoice(@QueryParam("businessId") Long businessId) {
        if (businessId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("businessId required"))
                    .build();
        }
        return Response.ok(service.listReadyToInvoice(businessId)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid DisposalTicketUpdateDto dto) {
        return Response.ok(service.update(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    // =============================
    // =   Lifecycle Endpoints     =
    // =============================

    /**
     * Process the ticket. OPEN → PROCESSED.
     */
    @POST
    @Path("/{id}/process")
    public Response process(@PathParam("id") Long id) {
        return Response.ok(service.process(id)).build();
    }

    /**
     * Revert to OPEN. PROCESSED → OPEN.
     */
    @POST
    @Path("/{id}/revert")
    public Response revert(@PathParam("id") Long id) {
        return Response.ok(service.revertToOpen(id)).build();
    }

    /**
     * Void the ticket.
     */
    @POST
    @Path("/{id}/void")
    public Response voidTicket(@PathParam("id") Long id) {
        return Response.ok(service.voidTicket(id)).build();
    }

    // =============================
    // =   PDF Generation          =
    // =============================

    @GET
    @Path("/{id}/pdf")
    @Produces("application/pdf")
    public Response downloadPdf(@PathParam("id") Long id) {
        DisposalTicket ticket = DisposalTicket.findById(id);
        if (ticket == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Disposal ticket not found"))
                    .build();
        }

        try {
            byte[] pdfBytes = pdfService.generatePdf(ticket);
            String filename = String.format("disposal-ticket-%d.pdf", ticket.ticketNumber);

            return Response.ok(pdfBytes)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Failed to generate PDF: " + e.getMessage()))
                    .build();
        }
    }

    public record ErrorResponse(String error) {}
}
