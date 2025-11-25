package io.tahawus.lynx.documents.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.service.NumberSequenceService;
import io.tahawus.lynx.documents.dto.*;
import io.tahawus.lynx.documents.mapper.ClosingEntryDocumentMapper;
import io.tahawus.lynx.documents.model.ClosingEntryDocument;
import io.tahawus.lynx.documents.model.Document;
import io.tahawus.lynx.documents.model.DocumentType;
import io.tahawus.lynx.ledger.model.FiscalYear;
import io.tahawus.lynx.ledger.service.JournalService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClosingEntryDocumentService {

    @Inject
    ClosingEntryDocumentMapper mapper;

    @Inject
    NumberSequenceService numberSequenceService;

    @Inject
    JournalService journalService;

    @Transactional
    public ClosingEntryDocumentDto create(ClosingEntryDocumentCreateDto dto) {
        if (!dto.isBalanced()) {
            throw new IllegalArgumentException("Closing entry must be balanced");
        }

        Business business = Business.findById(dto.businessId());
        if (business == null) {
            throw new NotFoundException("Business not found: " + dto.businessId());
        }

        FiscalYear fiscalYear = FiscalYear.findById(dto.fiscalYearId());
        if (fiscalYear == null) {
            throw new NotFoundException("Fiscal year not found: " + dto.fiscalYearId());
        }

        Long seq = numberSequenceService.getNextNumber(business.id, DocumentType.CLOSING_ENTRY.getPrefix());
        String documentNumber = NumberSequenceService.format(DocumentType.CLOSING_ENTRY.getPrefix(), seq, 4);

        ClosingEntryDocument entity = mapper.fromCreateDto(dto, business, fiscalYear, documentNumber);
        entity.persist();

        return mapper.toDto(entity);
    }

    public Optional<ClosingEntryDocumentDto> get(Long id) {
        return ClosingEntryDocument.<ClosingEntryDocument>findByIdOptional(id)
                .map(mapper::toDto);
    }

    public ClosingEntryDocumentDto getRequired(Long id) {
        return get(id).orElseThrow(() -> new NotFoundException("Closing entry not found: " + id));
    }

    public List<ClosingEntryDocumentDto> listByFiscalYear(Long fiscalYearId) {
        return mapper.toDtoList(ClosingEntryDocument.listByFiscalYearId(fiscalYearId));
    }

    @Transactional
    public void delete(Long id) {
        ClosingEntryDocument entity = ClosingEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Closing entry not found: " + id);
        }
        if (!entity.canEdit()) {
            throw new IllegalStateException("Cannot delete in " + entity.status + " status");
        }
        entity.delete();
    }

    @Transactional
    public ClosingEntryDocumentDto complete(Long id) {
        ClosingEntryDocument entity = ClosingEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Closing entry not found: " + id);
        }
        entity.complete();
        return mapper.toDto(entity);
    }

    @Transactional
    public ClosingEntryDocumentDto revert(Long id) {
        ClosingEntryDocument entity = ClosingEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Closing entry not found: " + id);
        }
        entity.revert();
        return mapper.toDto(entity);
    }

    @Transactional
    public ClosingEntryDocumentDto post(Long id) {
        ClosingEntryDocument entity = ClosingEntryDocument.findById(id);
        if (entity == null) {
            throw new NotFoundException("Closing entry not found: " + id);
        }
        // FY CLOSING status check happens in JournalService.create()
        Document.post(entity, journalService);
        return mapper.toDto(entity);
    }
}
