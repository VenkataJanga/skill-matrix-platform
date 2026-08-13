package com.skillmatrix.admin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssignmentRequest(

        @NotBlank(message = "userPublicId is required")
        String userPublicId,

        @NotBlank(message = "applicationPublicId is required")
        String applicationPublicId,

        @NotNull(message = "allocationPercentage is required")
        @DecimalMin(value = "0.01", message = "allocationPercentage must be > 0")
        @DecimalMax(value = "100.00", message = "allocationPercentage must be <= 100")
        BigDecimal allocationPercentage,

        @NotNull(message = "effectiveFrom is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {}
