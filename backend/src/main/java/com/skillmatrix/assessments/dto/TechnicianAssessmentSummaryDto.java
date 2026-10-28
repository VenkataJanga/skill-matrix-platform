package com.skillmatrix.assessments.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TECHNICIAN-FACING DTO ONLY — row for "My Assessments" list.
 * Represents either an existing header, or a not-yet-started eligible
 * cycle (assessmentPublicId null in that case).
 */
public record TechnicianAssessmentSummaryDto(
        String assessmentPublicId,
        String cyclePublicId,
        String cycleName,
        String applicationCode,
        String applicationName,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        /** NOT_STARTED | DRAFT | SUBMITTED */
        String assessmentStatus,
        int totalItems,
        int ratedItems,
        LocalDateTime updatedAt
) {}
