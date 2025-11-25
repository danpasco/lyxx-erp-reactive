package io.tahawus.lynx.core.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.smallrye.common.annotation.Blocking;
import io.tahawus.lynx.accounts.model.AccountType;
import io.tahawus.lynx.accounts.model.SubsidiaryType;
import io.tahawus.lynx.contacts.model.AddressType;
import io.tahawus.lynx.contacts.model.Contact;

import io.tahawus.lynx.documents.model.DocumentStatus;
import io.tahawus.lynx.ledger.model.FiscalPeriodStatus;
import io.tahawus.lynx.ledger.model.FiscalPeriodType;
import io.tahawus.lynx.operations.model.OperationStatus;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;

@Path("/enums")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class EnumResource {

    @GET
    @Path("/account-types")
    public AccountType[] getAccountTypes() {
        return AccountType.values();
    }

    @GET
    @Path("/address-types")
    public AddressType[] getAddressTypes() {
        return AddressType.values();
    }

    @GET
    @Path("/contact-types")
    public List<String> getContactTypes() {
        JsonSubTypes jsonSubTypes = Contact.class.getAnnotation(JsonSubTypes.class);
        return Arrays.stream(jsonSubTypes.value())
                .map(JsonSubTypes.Type::name)
                .toList();
    }

    @GET
    @Path("/document-statuses")
    public DocumentStatus[] getDocumentStatuses() {
        return DocumentStatus.values();
    }

    @GET
    @Path("/fiscal-period-status")
    public FiscalPeriodStatus[] getFiscalPeriodStatus() {
        return FiscalPeriodStatus.values();
    }
    @GET
    @Path("/operation-statuses")
    public OperationStatus[] getOperationStatuses() {
        return OperationStatus.values();
    }

    @GET
    @Path("/period-types")
    public FiscalPeriodType[] getPeriodTypes() {
        return FiscalPeriodType.values();
    }

    @GET
    @Path("/subsidiary-types")
    public SubsidiaryType[] getSubsidiaryTypes() {
        return SubsidiaryType.values();
    }

    @GET
    @Path("/ticket-statuses")
    public OperationStatus[] getTicketStatuses() {
        return OperationStatus.values();
    }
}
