package com.skillmatrix.assessments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * TECHNICIAN-FACING request body for saving a draft.
 * version is required for optimistic-lock conflict detection (409).
 */
public record SaveDraftRequest(
        @NotNull(message = "version is required")
        Long version,

        @Valid
        List<ItemRatingRequest> items
) {
    public record ItemRatingRequest(
            @NotNull(message = "assessmentItemPublicId is required")
            String assessmentItemPublicId,

            /** Optional while in DRAFT — null means "not yet rated". */
            Integer selfRating,

            String comment,

            String evidence
    ) {}
}
