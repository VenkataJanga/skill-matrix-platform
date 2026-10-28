package com.skillmatrix.assessments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateAssessmentCycleRequest(

        @NotBlank(message = "cycleName is required")
        String cycleName,

        @NotNull(message = "cycleStartDate is required")
        LocalDate cycleStartDate,

        @NotNull(message = "cycleEndDate is required")
        LocalDate cycleEndDate,

        /** Optional — allowed only while no technician assessment header exists yet. */
        String applicationPublicId,

        /** Optional — allowed only while no technician assessment header exists yet. */
        String requirementVersionPublicId
) {}
