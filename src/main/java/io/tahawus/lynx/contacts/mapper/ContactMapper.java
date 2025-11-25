package io.tahawus.lynx.contacts.mapper;

import io.tahawus.lynx.contacts.dto.*;
import io.tahawus.lynx.contacts.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps between Contact entities and DTOs.
 */
public final class ContactMapper {

    private ContactMapper() {
    }

    // =============================
    // =   Contact Entity -> DTO   =
    // =============================

    public static ContactDto toDto(Contact contact) {
        if (contact == null) {
            return null;
        }

        return new ContactDto(
                contact.id,
                contact instanceof Person ? ContactType.PERSON : ContactType.ORGANIZATION,
                contact.name,
                contact.abbreviation,
                contact.email,
                contact.webSite,
                toAddressDtoList(contact.addresses),
                toTelephoneDtoList(contact.phones)
        );
    }

    public static List<ContactDto> toDtoList(List<Contact> contacts) {
        if (contacts == null) {
            return List.of();
        }
        return contacts.stream()
                .map(ContactMapper::toDto)
                .toList();
    }

    // =============================
    // =   DTO -> Contact Entity   =
    // =============================

    public static Contact fromCreateDto(ContactCreateDto dto) {
        if (dto == null) {
            return null;
        }

        Contact contact = dto.type() == ContactType.PERSON ? new Person() : new Organization();
        contact.name = dto.name();
        contact.abbreviation = dto.abbreviation();
        contact.email = dto.email();
        contact.webSite = dto.webSite();

        if (dto.addresses() != null) {
            for (AddressCreateDto addrDto : dto.addresses()) {
                contact.addAddress(toAddressEntity(addrDto));
            }
        }

        if (dto.telephones() != null) {
            for (TelephoneCreateDto telDto : dto.telephones()) {
                contact.addPhone(toTelephoneEntity(telDto));
            }
        }

        return contact;
    }

    public static void applyUpdate(Contact contact, ContactUpdateDto dto) {
        if (dto == null) {
            return;
        }

        if (dto.name() != null) {
            contact.name = dto.name();
        }
        if (dto.abbreviation() != null) {
            contact.abbreviation = dto.abbreviation();
        }
        if (dto.email() != null) {
            contact.email = dto.email();
        }
        if (dto.webSite() != null) {
            contact.webSite = dto.webSite();
        }

        // Replace addresses if provided
        if (dto.addresses() != null) {
            contact.addresses.clear();
            for (AddressCreateDto addrDto : dto.addresses()) {
                contact.addAddress(toAddressEntity(addrDto));
            }
        }

        // Replace telephones if provided
        if (dto.telephones() != null) {
            contact.phones.clear();
            for (TelephoneCreateDto telDto : dto.telephones()) {
                contact.addPhone(toTelephoneEntity(telDto));
            }
        }
    }

    // =============================
    // =   Address Mapping         =
    // =============================

    public static AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressDto(
                address.id,
                address.type,
                address.street,
                address.additionalStreet,
                address.city,
                address.state,
                address.postalCode,
                address.country
        );
    }

    public static List<AddressDto> toAddressDtoList(List<Address> addresses) {
        if (addresses == null) {
            return List.of();
        }
        return addresses.stream()
                .map(ContactMapper::toAddressDto)
                .toList();
    }

    public static Address toAddressEntity(AddressCreateDto dto) {
        if (dto == null) {
            return null;
        }

        Address address = new Address();
        address.type = dto.type();
        address.street = dto.street();
        address.additionalStreet = dto.additionalStreet();
        address.city = dto.city();
        address.state = dto.state();
        address.postalCode = dto.postalCode();
        address.country = dto.country();
        return address;
    }

    // =============================
    // =   Telephone Mapping       =
    // =============================

    public static TelephoneDto toTelephoneDto(Telephone telephone) {
        if (telephone == null) {
            return null;
        }

        return new TelephoneDto(
                telephone.id,
                telephone.type,
                telephone.telephoneNumber,
                telephone.description
        );
    }

    public static List<TelephoneDto> toTelephoneDtoList(List<Telephone> telephones) {
        if (telephones == null) {
            return List.of();
        }
        return telephones.stream()
                .map(ContactMapper::toTelephoneDto)
                .toList();
    }

    public static Telephone toTelephoneEntity(TelephoneCreateDto dto) {
        if (dto == null) {
            return null;
        }

        Telephone telephone = new Telephone();
        telephone.type = dto.type();
        telephone.telephoneNumber = dto.telephoneNumber();
        telephone.description = dto.description();
        return telephone;
    }
}
