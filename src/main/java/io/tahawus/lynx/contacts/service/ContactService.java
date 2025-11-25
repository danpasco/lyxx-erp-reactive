package io.tahawus.lynx.contacts.service;

import io.tahawus.lynx.contacts.dto.ContactCreateDto;
import io.tahawus.lynx.contacts.dto.ContactDto;
import io.tahawus.lynx.contacts.dto.ContactUpdateDto;
import io.tahawus.lynx.contacts.mapper.ContactMapper;
import io.tahawus.lynx.contacts.model.Contact;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Contact operations.
 */
@ApplicationScoped
public class ContactService {

    // =============================
    // =      Query Operations     =
    // =============================

    public List<ContactDto> listAll() {
        List<Contact> contacts = Contact.listAll();
        return ContactMapper.toDtoList(contacts);
    }

    public ContactDto get(Long id) {
        Contact contact = findOrThrow(id);
        return ContactMapper.toDto(contact);
    }

    public Optional<ContactDto> findByName(String name) {
        return Contact.findByName(name)
                .map(ContactMapper::toDto);
    }

    public List<ContactDto> search(String namePattern) {
        List<Contact> contacts = Contact.findContainingName(namePattern);
        return ContactMapper.toDtoList(contacts);
    }

    // =============================
    // =     Write Operations      =
    // =============================

    @Transactional
    public ContactDto create(ContactCreateDto dto) {
        Contact contact = ContactMapper.fromCreateDto(dto);
        contact.persist();
        return ContactMapper.toDto(contact);
    }

    /**
     * Batch create contacts.
     *
     * @param dtos List of contacts to create
     * @return List of created contacts as DTOs
     */
    @Transactional
    public List<ContactDto> create(List<ContactCreateDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }

        List<ContactDto> results = new ArrayList<>(dtos.size());
        for (ContactCreateDto dto : dtos) {
            Contact contact = ContactMapper.fromCreateDto(dto);
            contact.persist();
            results.add(ContactMapper.toDto(contact));
        }
        return results;
    }

    @Transactional
    public ContactDto update(Long id, ContactUpdateDto dto) {
        Contact contact = findOrThrow(id);
        ContactMapper.applyUpdate(contact, dto);
        return ContactMapper.toDto(contact);
    }

    @Transactional
    public void delete(Long id) {
        Contact contact = findOrThrow(id);
        contact.delete();
    }

    // =============================
    // =      Helper Methods       =
    // =============================

    private Contact findOrThrow(Long id) {
        return Contact.findByIdOptional(id)
                .map(obj -> (Contact) obj)
                .orElseThrow(() -> new NotFoundException("Contact not found: " + id));
    }
}
