package io.tahawus.lynx.business.dto;

/**
 * DTO for business logo data.
 */
public record BusinessLogoDto(
        String contentType,
        String fileName,
        byte[] data
) {}
