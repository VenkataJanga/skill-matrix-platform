package com.skillmatrix.assessments.controller;

import com.skillmatrix.assessments.dto.AssessmentCycleDto;
import com.skillmatrix.assessments.dto.CreateAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.ReopenAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.UpdateAssessmentCycleRequest;
import com.skillmatrix.assessments.service.AssessmentCycleService;
import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * M4A — Assessment Cycle Management. ADMIN only.
 */
@RestController
@RequestMapping("/api/v1/admin/assessment-cycles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Assessment Cycles", description = "Admin assessment cycle management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssessmentCycleController {

    private final AssessmentCycleService assessmentCycleService;

    @GetMapping
    @Operation(summary = "List assessment cycles with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<AssessmentCycleDto>>> getCycles(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String applicationPublicId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentCycleService.getCycles(
                page, size, search, applicationPublicId, statusCode, sortBy, sortDirection)));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get assessment cycle detail by publicId")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> getCycle(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentCycleService.getByPublicId(publicId)));
    }

    @PostMapping
    @Operation(summary = "Create a new PLANNED assessment cycle")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> createCycle(
            @Valid @RequestBody CreateAssessmentCycleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                assessmentCycleService.createCycle(request, principal.getUsername())));
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update a PLANNED assessment cycle")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> updateCycle(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateAssessmentCycleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                assessmentCycleService.updateCycle(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/{publicId}/open")
    @Operation(summary = "Open a PLANNED assessment cycle")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> openCycle(
            @PathVariable String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                assessmentCycleService.openCycle(publicId, principal.getUsername())));
    }

    @PatchMapping("/{publicId}/close")
    @Operation(summary = "Close an OPEN assessment cycle")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> closeCycle(
            @PathVariable String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                assessmentCycleService.closeCycle(publicId, principal.getUsername())));
    }

    @PatchMapping("/{publicId}/reopen")
    @Operation(summary = "Reopen a CLOSED assessment cycle (controlled CLOSED -> OPEN transition)")
    public ResponseEntity<ApiResponse<AssessmentCycleDto>> reopenCycle(
            @PathVariable String publicId,
            @Valid @RequestBody ReopenAssessmentCycleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                assessmentCycleService.reopenCycle(publicId, request, principal.getUsername())));
    }
}
