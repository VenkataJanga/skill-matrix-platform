package com.skillmatrix.applications.dto;

import com.skillmatrix.domain.entity.ApplicationType;

public record ApplicationTypeDto(
        String publicId,
        String typeCode,
        String typeName,
        String description
) {
    public static ApplicationTypeDto from(ApplicationType entity) {
        return new ApplicationTypeDto(
                entity.getPublicId(),
                entity.getTypeCode(),
                entity.getTypeName(),
                entity.getDescription()
        );
    }
}
