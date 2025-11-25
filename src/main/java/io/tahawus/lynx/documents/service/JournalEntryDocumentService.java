package io.tahawus.lynx.documents.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.service.NumberSequenceService;
import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.mapper.JournalEntryDocumentMapper;
import io.tahawus.lynx.documents.model.Document;
import io.tahawus.lynx.documents.model.DocumentStatus;
import io.tahawus.lynx.documents.model.DocumentType;
import io.tahawus.lynx.documents.model.JournalEntryDocument;
import io.tahawus.lynx.ledger.model.Journal;
import io.tahawus.lynx.ledger.service.JournalService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JournalEntryDocumentService {

    @Inject
    JournalEntryDocumentMapper mapper;

    @Inject
    NumberSequenceService numberSequenceService;

    @Inject
    JournalService journalService;

    @Transactional
    public JournalEntryDocumentDto create(JournalEntryDocumentCreateDto dto) {
        if (!dto.isBalanced()) {
            throw new IllegalArgumentException("Journal entry must be balanced");
        }

        Business business = Business.findById(dto.businessId());
        if (business == null) {
            throw new NotFoundException("Business not found: " + dto.businessId());
        }

        Long seq = numberSequenceService.getNextNumber(business.id, DocumentType.JOURNAL_ENTRY.getPrefix());
        String documentNumber = NumberSequenceService.format(DocumentType.JOURNAL_ENTRY.getPrefix(), seq, 4);

        JournalEntryDocument entity = mapper.fromCreateDto(dto, business, documentNumber);
        entity.persist();

        return mapper.toDto(entity);
    }

    public Optional<JournalEntryDocumentDto> get(Long id) {
        return JournalEntryDocument.<JournalEntryDocument>findByIdOptional(id)
                .map(mapper::toDto);
    }

    public JournalEntryDocumentDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("Journal entry not found: " + id));
    }

    public List<JournalEntryDocumentDto> listByBusiness(Long businessId) {
        return mapper.toDtoList(JournalEntryDocument.listByBusinessId(businessId));
    }

    public List<JournalEntryDocumentDto> listByBusinessAndStatus(Long businessId, DocumentStatus status) {
        return mapper.toDtoList(JournalEntryDocument.listByBusinessIdAndStatus(businessId, status));
    }

    @Transactional
    public JournalEntryDocumentDto update(Long id, JournalEntryDocumentUpdateDto dto) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        if (!entity.canEdit()) {
            throw new IllegalStateException("Cannot edit in " + entity.status + " status");
        }
        if (!dto.isBalanced()) {
            throw new IllegalArgumentException("Journal entry must be balanced");
        }

        mapper.updateFromDto(entity, dto);
        return mapper.toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        if (!entity.canEdit()) {
            throw new IllegalStateException("Cannot delete in " + entity.status + " status");
        }
        entity.delete();
    }

    @Transactional
    public JournalEntryDocumentDto complete(Long id) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        entity.complete();
        return mapper.toDto(entity);
    }

    @Transactional
    public JournalEntryDocumentDto revert(Long id) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        entity.revert();
        return mapper.toDto(entity);
    }

    @Transactional
    public JournalEntryDocumentDto post(Long id) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        Document.post(entity, journalService);
        return mapper.toDto(entity);
    }

    @Transactional
    public JournalEntryDocumentDto voidDocument(Long id) {
        JournalEntryDocument entity = JournalEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Journal entry not found: " + id);
        }
        // TODO: Implement void via reversing entry
        throw new UnsupportedOperationException("Void not yet implemented");
    }
}
