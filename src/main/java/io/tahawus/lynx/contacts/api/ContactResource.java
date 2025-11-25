package io.tahawus.lynx.contacts.api;

import io.tahawus.lynx.contacts.dto.ContactCreateDto;
import io.tahawus.lynx.contacts.dto.ContactDto;
import io.tahawus.lynx.contacts.dto.ContactUpdateDto;
import io.tahawus.lynx.contacts.service.ContactService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

/**
 * REST API for Contact management.
 *
 * Endpoints:
 * - GET    /contacts                 - List all contacts
 * - GET    /contacts/{id}            - Get contact by ID
 * - GET    /contacts/search?q=       - Search contacts by name
 * - POST   /contacts                 - Create new contact
 * - POST   /contacts/batch           - Create multiple contacts
 * - PUT    /contacts/{id}            - Update contact
 * - DELETE /contacts/{id}            - Delete contact
 */
@Path("/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContactResource {

    @Inject
    ContactService contactService;

    // =============================
    // =      Query Endpoints      =
    // =============================

    @GET
    public List<ContactDto> list() {
        return contactService.listAll();
    }

    @GET
    @Path("/{id}")
    public ContactDto getById(@PathParam("id") Long id) {
        return contactService.get(id);
    }

    @GET
    @Path("/search")
    public List<ContactDto> search(@QueryParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return contactService.search(query.trim());
    }

    // =============================
    // =      Write Endpoints      =
    // =============================

    @POST
    public Response create(@Valid ContactCreateDto request, @Context UriInfo uriInfo) {
        ContactDto created = contactService.create(request);

        return Response.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(Long.toString(created.id()))
                                .build())
                .entity(created)
                .build();
    }

    @POST
    @Path("/batch")
    public List<ContactDto> createBatch(@Valid List<ContactCreateDto> requests) {
        return contactService.create(requests);
    }

    @PUT
    @Path("/{id}")
    public ContactDto update(@PathParam("id") Long id, @Valid ContactUpdateDto request) {
        return contactService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        contactService.delete(id);
        return Response.noContent().build();
    }
}
