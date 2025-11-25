package io.tahawus.lynx.core.mapper;

import io.tahawus.lynx.tenant.CurrentTenant;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps TenantRequiredException to HTTP 400 Bad Request.
 */
@Provider
public class TenantRequiredExceptionMapper
        implements ExceptionMapper<CurrentTenant.TenantRequiredException> {

    @Override
    public Response toResponse(CurrentTenant.TenantRequiredException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
    }

    public record ErrorResponse(String error) {}
}
