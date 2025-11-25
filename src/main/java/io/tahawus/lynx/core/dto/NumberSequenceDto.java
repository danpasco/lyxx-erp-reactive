package io.tahawus.lynx.core.dto;

import java.time.LocalDateTime;

/**
 * View DTO for NumberSequence.
 * Used for admin/config screens per Business.
 */
public record NumberSequenceDto(
        Long id,
        Long businessId,
        String sequenceKey,
        Long nextNumber,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
