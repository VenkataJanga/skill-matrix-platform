package com.skillmatrix.skills.dto;

import com.skillmatrix.domain.entity.ImportExportHistory;

import java.time.LocalDateTime;

public record ImportHistoryDto(
        String publicId,
        String operationType,
        String templateType,
        String fileName,
        Integer totalRows,
        Integer successRows,
        Integer failedRows,
        String statusCode,
        String statusLabel,
        String initiatedByUsername,
        String initiatedByFullName,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static ImportHistoryDto from(ImportExportHistory h) {
        var status = h.getStatusLookup();
        var user   = h.getInitiatedBy();
        return new ImportHistoryDto(
                h.getPublicId(),
                h.getOperationType(),
                h.getTemplateType(),
                h.getFileName(),
                h.getTotalRows(),
                h.getSuccessRows(),
                h.getFailedRows(),
                status != null ? status.getValueCode()  : null,
                status != null ? status.getValueLabel() : null,
                user.getUsername(),
                user.getFullName(),
                h.getStartedAt(),
                h.getCompletedAt()
        );
    }
}
