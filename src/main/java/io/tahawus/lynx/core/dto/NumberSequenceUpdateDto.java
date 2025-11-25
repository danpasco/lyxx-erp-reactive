package io.tahawus.lynx.core.dto;

/**
 * Payload for updating NumberSequence configuration.
 * Currently only nextNumber is exposed, but this can be expanded later.
 */
public record NumberSequenceUpdateDto(
        Long nextNumber
) {}
