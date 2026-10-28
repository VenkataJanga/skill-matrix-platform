package com.skillmatrix.assessments.dto;

import com.skillmatrix.domain.entity.AssessmentItem;
import com.skillmatrix.domain.entity.TechnicianAssessmentHeader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TECHNICIAN-FACING DTO ONLY — full assessment detail.
 * Contains only {@link TechnicianAssessmentItemDto} items, which are
 * structurally incapable of exposing expected level.
 */
public record TechnicianAssessmentDto(
        String assessmentPublicId,
        String cycleName,
        String applicationCode,
        String applicationName,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        String statusCode,
        String statusLabel,
        LocalDateTime submittedAt,
        List<TechnicianAssessmentItemDto> items,
        Long version
) {
    public static TechnicianAssessmentDto from(TechnicianAssessmentHeader h, List<AssessmentItem> items) {
        var cycle = h.getCycle();
        var app = cycle.getApplication();
        var status = h.getStatusLookup();
        return new TechnicianAssessmentDto(
                h.getPublicId(),
                cycle.getCycleName(),
                app.getApplicationCode(),
                app.getApplicationName(),
                cycle.getCycleStartDate(),
                cycle.getCycleEndDate(),
                status != null ? status.getValueCode() : null,
                status != null ? status.getValueLabel() : null,
                h.getSubmittedAt(),
                items.stream().map(TechnicianAssessmentItemDto::from).toList(),
                h.getVersion()
        );
    }
}
