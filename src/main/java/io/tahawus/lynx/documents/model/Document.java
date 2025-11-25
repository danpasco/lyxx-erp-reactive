package io.tahawus.lynx.documents.model;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.AuditableEntity;
import io.tahawus.lynx.ledger.dto.JournalCreateDto;
import io.tahawus.lynx.ledger.model.Journal;
import io.tahawus.lynx.ledger.service.JournalService;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Document - Abstract base class for all source documents.
 *
 * All journal entries originate from documents. Documents post to the ledger
 * via the static post() methods which call JournalService.create().
 *
 * LIFECYCLE: OPEN → COMPLETED → POSTED → VOIDED
 *
 * POSTING:
 * - Document.post(document) - single document, single journal
 * - Document.post(documents) - multiple documents, summary journal
 *
 * Subclasses implement buildJournalCreateDto() to map their content
 * to debit/credit entries.
 *
 * @author Dan Pasco
 */
@Entity
@Table(
        name = "document",
        indexes = {
                @Index(name = "idx_document_business", columnList = "business_id"),
                @Index(name = "idx_document_type", columnList = "document_type"),
                @Index(name = "idx_document_status", columnList = "status"),
                @Index(name = "idx_document_number", columnList = "document_number"),
                @Index(name = "idx_document_date", columnList = "document_date"),
                @Index(name = "idx_document_journal", columnList = "journal_id")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "document_type", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Document extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    public Business business;

    @Column(name = "document_number", nullable = false, length = 50)
    public String documentNumber;

    @Column(name = "document_type", nullable = false, insertable = false, updatable = false, length = 30)
    @Enumerated(EnumType.STRING)
    public DocumentType documentType;

    @Column(name = "document_date", nullable = false)
    public LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public DocumentStatus status = DocumentStatus.OPEN;

    @Column(length = 500)
    public String description;

    @Column(name = "reference_number", length = 50)
    public String referenceNumber;

    @Column(length = 2000)
    public String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    public Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversing_journal_id")
    public Journal reversingJournal;

    // =============================
    // =   Lifecycle Methods       =
    // =============================

    public boolean canEdit() {
        return status.canEdit();
    }

    public boolean canComplete() {
        return status.canComplete();
    }

    public boolean canRevert() {
        return status.canRevert();
    }

    public boolean canPost() {
        return status.canPost();
    }

    public boolean canVoid() {
        return status.canVoid();
    }

    public void complete() {
        if (!canComplete()) {
            throw new IllegalStateException("Cannot complete document in " + status + " status");
        }
        validate();
        this.status = DocumentStatus.COMPLETED;
    }

    public void revert() {
        if (!canRevert()) {
            throw new IllegalStateException("Cannot revert document in " + status + " status");
        }
        this.status = DocumentStatus.OPEN;
    }

    // =============================
    // =   Abstract Contract       =
    // =============================

    /**
     * Validate the document. Called before complete() and post().
     */
    public void validate() {
        if (business == null) {
            throw new IllegalStateException("Business is required");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalStateException("Document number is required");
        }
        if (documentDate == null) {
            throw new IllegalStateException("Document date is required");
        }
    }

    /**
     * Get the document type.
     */
    public abstract DocumentType getDocumentType();

    /**
     * Build the JournalCreateDto for posting.
     * Each subclass implements this to map its content to DR/CR entries.
     */
    protected abstract JournalCreateDto buildJournalCreateDto();

    // =============================
    // =   Static Post Methods     =
    // =============================

    /**
     * Post a single document.
     *
     * @param document The document to post
     * @param journalService The journal service (injected by caller)
     * @return The created Journal
     */
    public static Journal post(Document document, JournalService journalService) {
        if (!document.canPost()) {
            throw new IllegalStateException(
                    "Cannot post document in " + document.status + " status");
        }
        if (document.journal != null) {
            throw new IllegalStateException(
                    "Document already posted to journal " + document.journal.id);
        }

        document.validate();

        JournalCreateDto dto = document.buildJournalCreateDto();
        Journal journal = journalService.create(dto);

        document.journal = journal;
        document.status = DocumentStatus.POSTED;

        return journal;
    }

    /**
     * Post multiple documents as a summary journal entry.
     * All documents must be for the same business and journal type.
     *
     * @param documents The documents to post
     * @param journalService The journal service (injected by caller)
     * @return The created Journal
     */
    public static Journal post(List<? extends Document> documents, JournalService journalService) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("At least one document is required");
        }

        // Validate all documents
        Document first = documents.get(0);
        Long businessId = first.business.id;
        DocumentType docType = first.getDocumentType();

        List<JournalCreateDto.Line> allLines = new ArrayList<>();
        StringBuilder refs = new StringBuilder();
        StringBuilder descs = new StringBuilder();

        for (Document doc : documents) {
            if (!doc.canPost()) {
                throw new IllegalStateException(
                        "Cannot post document " + doc.documentNumber + " in " + doc.status + " status");
            }
            if (doc.journal != null) {
                throw new IllegalStateException(
                        "Document " + doc.documentNumber + " already posted");
            }
            if (!doc.business.id.equals(businessId)) {
                throw new IllegalStateException(
                        "All documents must be for the same business");
            }
            if (doc.getDocumentType() != docType) {
                throw new IllegalStateException(
                        "All documents must be the same type for summary posting");
            }

            doc.validate();

            JournalCreateDto docDto = doc.buildJournalCreateDto();
            allLines.addAll(docDto.lines());

            if (refs.length() > 0) refs.append(", ");
            refs.append(doc.documentNumber);

            if (descs.length() > 0) descs.append("; ");
            if (doc.description != null) descs.append(doc.description);
        }

        // Use first document's date as entry date
        LocalDate entryDate = first.documentDate;

        // Build summary DTO
        JournalCreateDto summaryDto = new JournalCreateDto(
                businessId,
                entryDate,
                first.id, // Primary document ID
                docType.getJournalType(),
                refs.toString(),
                descs.length() > 0 ? descs.toString() : "Summary posting",
                null, // not a reversal
                allLines
        );

        Journal journal = journalService.create(summaryDto);

        // Link all documents to the journal
        for (Document doc : documents) {
            doc.journal = journal;
            doc.status = DocumentStatus.POSTED;
        }

        return journal;
    }

    // =============================
    // =   Query Methods           =
    // =============================

    public static List<Document> listByBusiness(Business business) {
        return list("business = ?1 order by documentDate desc, id desc", business);
    }

    public static List<Document> listByBusinessAndStatus(Business business, DocumentStatus status) {
        return list("business = ?1 and status = ?2 order by documentDate desc, id desc",
                business, status);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", documentNumber='" + documentNumber + '\'' +
                ", status=" + status +
                '}';
    }
}
