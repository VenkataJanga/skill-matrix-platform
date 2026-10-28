package com.skillmatrix.assessments.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request body for CLOSED -> OPEN reopen transition (M4 stabilization).
 * {@code newEndDate} is optional — only required when the cycle's current
 * end date has already passed, in which case the Admin must extend it to a
 * date on/after today before the cycle can be reopened.
 */
public record ReopenAssessmentCycleRequest(
        @NotBlank(message = "Reopen reason is required")
        String reason,
        LocalDate newEndDate
) {
}
