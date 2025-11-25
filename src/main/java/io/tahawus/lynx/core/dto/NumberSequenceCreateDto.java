package io.tahawus.lynx.core.dto;

/**
 * Payload for creating a new NumberSequence for a Business.
 * Often you may rely on "create on first use" instead, but this
 * is useful for explicit admin configuration.
 */
public record NumberSequenceCreateDto(
        Long businessId,
        String sequenceKey,
        Long startNumber
) {}
