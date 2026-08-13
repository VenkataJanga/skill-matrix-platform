package com.skillmatrix.applications.dto;

import com.skillmatrix.domain.entity.Bundle;

public record BundleDto(
        String publicId,
        String bundleCode,
        String bundleName,
        String description
) {
    public static BundleDto from(Bundle entity) {
        return new BundleDto(
                entity.getPublicId(),
                entity.getBundleCode(),
                entity.getBundleName(),
                entity.getDescription()
        );
    }
}
