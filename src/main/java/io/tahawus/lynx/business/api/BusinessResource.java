package io.tahawus.lynx.business.api;

import io.tahawus.lynx.business.dto.BusinessCreateDto;
import io.tahawus.lynx.business.dto.BusinessDto;
import io.tahawus.lynx.business.dto.BusinessLogoDto;
import io.tahawus.lynx.business.dto.BusinessUpdateDto;
import io.tahawus.lynx.business.service.BusinessService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * REST API for Business management.
 *
 * Endpoints:
 * - GET    /businesses               - List all businesses
 * - GET    /businesses/{id}          - Get business by ID
 * - POST   /businesses               - Create new business
 * - PUT    /businesses/{id}          - Update business
 * - DELETE /businesses/{id}          - Delete business
 * - POST   /businesses/{id}/logo     - Upload logo
 * - GET    /businesses/{id}/logo     - Download logo
 * - DELETE /businesses/{id}/logo     - Remove logo
 */
@Path("/businesses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BusinessResource {

    @Inject
    BusinessService businessService;

    // =============================
    // =      CRUD Operations      =
    // =============================

    @GET
    public List<BusinessDto> list() {
        return businessService.listAll();
    }

    @GET
    @Path("/{id}")
    public BusinessDto getById(@PathParam("id") Long id) {
        return businessService.get(id);
    }

    @POST
    public Response create(@Valid BusinessCreateDto request, @Context UriInfo uriInfo) {
        BusinessDto created = businessService.create(request);

        return Response.created(
                        uriInfo.getAbsolutePathBuilder()
                                .path(Long.toString(created.id()))
                                .build())
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    public BusinessDto update(@PathParam("id") Long id, @Valid BusinessUpdateDto request) {
        return businessService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        businessService.delete(id);
        return Response.noContent().build();
    }

    // =============================
    // =      Logo Management      =
    // =============================

    @POST
    @Path("/{id}/logo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public BusinessDto uploadLogo(@PathParam("id") Long id, @RestForm("file") FileUpload file) {
        if (file == null) {
            throw new BadRequestException("No file uploaded");
        }

        try {
            byte[] logoData = Files.readAllBytes(file.filePath());
            String contentType = file.contentType();
            String fileName = file.fileName();

            return businessService.updateLogo(id, logoData, contentType, fileName);
        } catch (IOException e) {
            throw new InternalServerErrorException("Error reading uploaded file: " + e.getMessage(), e);
        }
    }

    @GET
    @Path("/{id}/logo")
    @Produces({"image/png", "image/jpeg", "image/svg+xml", "image/gif"})
    public Response downloadLogo(@PathParam("id") Long id) {
        BusinessLogoDto logo = businessService.getLogo(id);

        if (logo == null || logo.data() == null) {
            throw new NotFoundException("Business has no logo");
        }

        return Response.ok(logo.data())
                .header("Content-Type", logo.contentType())
                .header("Content-Disposition", "inline; filename=\"" + logo.fileName() + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}/logo")
    public BusinessDto removeLogo(@PathParam("id") Long id) {
        return businessService.removeLogo(id);
    }
}
