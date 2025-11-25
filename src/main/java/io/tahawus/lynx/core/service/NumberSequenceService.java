package io.tahawus.lynx.core.service;

import io.tahawus.lynx.business.model.Business;
import io.tahawus.lynx.core.model.NumberSequence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Reactive service for managing numeric number sequences.
 *
 * Responsibilities:
 *   - Gapless, atomic number generation per Business + sequenceKey
 *   - Pessimistic locking to prevent duplicates under concurrency
 *
 * It does NOT:
 *   - Apply prefixes
 *   - Apply padding
 *   - Do any formatting
 *
 * Callers receive a raw Long and can format it however they like.
 */
@ApplicationScoped
public class NumberSequenceService {

    @Inject
    EntityManager em;

    public static String format(String prefix, Long number, int padding) {
        return String.format("%s-%0" + padding + "d", prefix, number);
    }

    @Transactional
    public Long getNextNumber(Long businessId, String sequenceKey) {

        NumberSequence seq = em.createQuery(
                        """
                        SELECT s FROM NumberSequence s
                        WHERE s.business.id = :businessId
                        AND s.sequenceKey = :sequenceKey
                        """,
                        NumberSequence.class
                )
                .setParameter("businessId", businessId)
                .setParameter("sequenceKey", sequenceKey)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    NumberSequence created = new NumberSequence();
                    created.business = em.getReference(Business.class, businessId);
                    created.sequenceKey = sequenceKey;
                    created.nextNumber = 1L;
                    em.persist(created);
                    return created;
                });

        Long next = seq.nextNumber;
        seq.nextNumber = next + 1;

        return next;
    }
}
