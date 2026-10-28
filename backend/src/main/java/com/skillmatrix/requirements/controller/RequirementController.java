package com.skillmatrix.requirements.controller;

import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.requirements.dto.*;
import com.skillmatrix.requirements.service.RequirementImportService;
import com.skillmatrix.requirements.service.RequirementVersionService;
import com.skillmatrix.requirements.service.SkillRequirementService;
import com.skillmatrix.security.UserPrincipal;
import com.skillmatrix.skills.dto.ImportHistoryDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * M3.1 — Application Skill Requirement Mapping.
 * Answers: "Which application requires the skill, is it mandatory,
 * and what proficiency level is expected?"
 *
 * ADMIN only. Reuses existing Skill Master, Application hierarchy,
 * requirement_versions and expected_ratings tables (no new DB tables).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Skill Requirement Mapping", description = "Admin application skill requirement mapping endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RequirementController {

    private final RequirementVersionService requirementVersionService;
    private final SkillRequirementService skillRequirementService;
    private final RequirementImportService requirementImportService;

    // ===================================================================
    // REQUIREMENT VERSIONS
    // ===================================================================

    @GetMapping("/requirement-versions")
    @Operation(summary = "List requirement versions for an application. "
            + "Pass eligibleOnly=true to restrict results to versions that are "
            + "active and not REJECTED — i.e. valid for Assessment Cycle creation.")
    public ResponseEntity<ApiResponse<List<RequirementVersionDto>>> getVersions(
            @RequestParam String applicationPublicId,
            @RequestParam(required = false, defaultValue = "false") boolean eligibleOnly) {
        List<RequirementVersionDto> versions = eligibleOnly
                ? requirementVersionService.getEligibleVersionsForApplication(applicationPublicId)
                : requirementVersionService.getVersionsForApplication(applicationPublicId);
        return ResponseEntity.ok(ApiResponse.ok(versions));
    }

    @GetMapping("/requirement-versions/{publicId}")
    @Operation(summary = "Get a requirement version by publicId")
    public ResponseEntity<ApiResponse<RequirementVersionDto>> getVersion(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(requirementVersionService.getByPublicId(publicId)));
    }

    @PostMapping("/requirement-versions")
    @Operation(summary = "Create a new DRAFT requirement version for an application")
    public ResponseEntity<ApiResponse<RequirementVersionDto>> createVersion(
            @Valid @RequestBody CreateRequirementVersionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                requirementVersionService.createDraftVersion(request, principal.getUsername())));
    }

    // ===================================================================
    // SKILL REQUIREMENTS
    // ===================================================================

    @GetMapping("/skill-requirements")
    @Operation(summary = "List skill requirements with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<SkillRequirementDto>>> getRequirements(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String requirementVersionPublicId,
            @RequestParam(required = false) String applicationPublicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skillGroup,
            @RequestParam(required = false) String mandatory,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "skillCode") String sortBy,
            @RequestParam(defaultValue = "asc")       String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(skillRequirementService.getRequirements(
                page, size, requirementVersionPublicId, applicationPublicId,
                search, skillGroup, mandatory, status, sortBy, sortDirection)));
    }

    @GetMapping("/skill-requirements/{publicId}")
    @Operation(summary = "Get skill requirement detail by publicId")
    public ResponseEntity<ApiResponse<SkillRequirementDto>> getRequirement(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(skillRequirementService.getByPublicId(publicId)));
    }

    @PostMapping("/skill-requirements")
    @Operation(summary = "Create a new skill requirement mapping")
    public ResponseEntity<ApiResponse<SkillRequirementDto>> createRequirement(
            @Valid @RequestBody CreateSkillRequirementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                skillRequirementService.createRequirement(request, principal.getUsername())));
    }

    @PatchMapping("/skill-requirements/{publicId}")
    @Operation(summary = "Update an existing skill requirement mapping")
    public ResponseEntity<ApiResponse<SkillRequirementDto>> updateRequirement(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateSkillRequirementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                skillRequirementService.updateRequirement(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/skill-requirements/{publicId}/status")
    @Operation(summary = "Activate or deactivate a skill requirement mapping")
    public ResponseEntity<ApiResponse<SkillRequirementDto>> updateStatus(
            @PathVariable String publicId,
            @RequestBody RequirementStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                skillRequirementService.updateStatus(publicId, request, principal.getUsername())));
    }

    // ===================================================================
    // EXCEL IMPORT
    // ===================================================================

    @GetMapping("/skill-requirements/template")
    @Operation(summary = "Download Application Skill Requirement Mapping Excel template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return requirementImportService.generateTemplate();
    }

    @PostMapping(value = "/skill-requirements/import/validate", consumes = "multipart/form-data")
    @Operation(summary = "Validate Excel file without importing")
    public ResponseEntity<ApiResponse<RequirementValidationResult>> validateImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(requirementImportService.validateOnly(file)));
    }

    @PostMapping(value = "/skill-requirements/import/confirm", consumes = "multipart/form-data")
    @Operation(summary = "Validate and confirm skill requirement import from Excel")
    public ResponseEntity<ApiResponse<RequirementImportResult>> confirmImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                requirementImportService.confirmImport(file, principal.getUsername())));
    }

    // ===================================================================
    // IMPORT HISTORY
    // ===================================================================

    @GetMapping("/skill-requirements/import/history")
    @Operation(summary = "Get Application Skill Requirement import history with pagination and filters")
    public ResponseEntity<ApiResponse<PagedResponse<ImportHistoryDto>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(ApiResponse.ok(requirementImportService.getHistory(
                page, size, statusCode, uploadedBy, fromDate, toDate)));
    }
}
