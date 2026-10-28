package com.skillmatrix.assessments.controller;

import com.skillmatrix.assessments.dto.SaveDraftRequest;
import com.skillmatrix.assessments.dto.TechnicianAssessmentDto;
import com.skillmatrix.assessments.dto.TechnicianAssessmentSummaryDto;
import com.skillmatrix.assessments.service.TechnicianAssessmentService;
import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M4B — Technician Self-Assessment. TECHNICIAN only.
 *
 * Technician identity is ALWAYS derived from the authenticated security
 * context ({@code @AuthenticationPrincipal}) — never from a request
 * parameter/body/path. This is the IDOR protection boundary.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('TECHNICIAN')")
@Tag(name = "Technician Assessments", description = "Technician self-assessment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianAssessmentController {

    private final TechnicianAssessmentService technicianAssessmentService;

    @GetMapping("/api/v1/technician/assessments")
    @Operation(summary = "List assessments eligible/started/submitted for the current technician")
    public ResponseEntity<ApiResponse<List<TechnicianAssessmentSummaryDto>>> getMyAssessments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianAssessmentService.getMyAssessments(principal.getUsername())));
    }

    @GetMapping("/api/v1/technician/assessments/{assessmentPublicId}")
    @Operation(summary = "Get assessment detail owned by the current technician")
    public ResponseEntity<ApiResponse<TechnicianAssessmentDto>> getAssessment(
            @PathVariable String assessmentPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianAssessmentService.getAssessment(assessmentPublicId, principal.getUsername())));
    }

    @PostMapping("/api/v1/technician/assessment-cycles/{cyclePublicId}/start")
    @Operation(summary = "Start (or resume, idempotently) an assessment for an OPEN cycle")
    public ResponseEntity<ApiResponse<TechnicianAssessmentDto>> startAssessment(
            @PathVariable String cyclePublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianAssessmentService.startAssessment(cyclePublicId, principal.getUsername())));
    }

    @PatchMapping("/api/v1/technician/assessments/{assessmentPublicId}/draft")
    @Operation(summary = "Save draft ratings/comments for an in-progress assessment")
    public ResponseEntity<ApiResponse<TechnicianAssessmentDto>> saveDraft(
            @PathVariable String assessmentPublicId,
            @Valid @RequestBody SaveDraftRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianAssessmentService.saveDraft(assessmentPublicId, request, principal.getUsername())));
    }

    @PostMapping("/api/v1/technician/assessments/{assessmentPublicId}/submit")
    @Operation(summary = "Submit the final self-assessment")
    public ResponseEntity<ApiResponse<TechnicianAssessmentDto>> submit(
            @PathVariable String assessmentPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianAssessmentService.submit(assessmentPublicId, principal.getUsername())));
    }
}
