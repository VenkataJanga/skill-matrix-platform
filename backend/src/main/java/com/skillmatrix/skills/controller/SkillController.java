package com.skillmatrix.skills.controller;

import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.security.UserPrincipal;
import com.skillmatrix.skills.dto.*;
import com.skillmatrix.skills.service.SkillImportService;
import com.skillmatrix.skills.service.SkillMasterService;
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

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Skill Master", description = "Admin skill management and Excel import endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SkillController {

    private final SkillMasterService skillMasterService;
    private final SkillImportService skillImportService;

    // ===================================================================
    // SKILL CATEGORIES
    // ===================================================================

    @GetMapping("/skill-categories")
    @Operation(summary = "Get all active skill categories")
    public ResponseEntity<ApiResponse<List<SkillCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(skillMasterService.getAllActiveCategories()));
    }

    // ===================================================================
    // SKILL MASTER
    // ===================================================================

    @GetMapping("/skills")
    @Operation(summary = "List skills with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<SkillDto>>> getSkills(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String scopeCode,
            @RequestParam(required = false) String applicationTypeCode,
            @RequestParam(required = false) String bundleCode,
            @RequestParam(required = false) String applicationPublicId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "skillCode") String sortBy,
            @RequestParam(defaultValue = "asc")       String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(skillMasterService.getSkills(
                page, size, search, categoryCode, scopeCode,
                applicationTypeCode, bundleCode, applicationPublicId,
                status, sortBy, sortDirection)));
    }

    @GetMapping("/skills/{publicId}")
    @Operation(summary = "Get skill detail by publicId")
    public ResponseEntity<ApiResponse<SkillDto>> getSkill(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(skillMasterService.getSkillByPublicId(publicId)));
    }

    @PostMapping("/skills")
    @Operation(summary = "Create a new skill")
    public ResponseEntity<ApiResponse<SkillDto>> createSkill(
            @Valid @RequestBody CreateSkillRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(skillMasterService.createSkill(request, principal.getUsername())));
    }

    @PatchMapping("/skills/{publicId}")
    @Operation(summary = "Update an existing skill")
    public ResponseEntity<ApiResponse<SkillDto>> updateSkill(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateSkillRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                skillMasterService.updateSkill(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/skills/{publicId}/status")
    @Operation(summary = "Activate or deactivate a skill")
    public ResponseEntity<ApiResponse<SkillDto>> updateSkillStatus(
            @PathVariable String publicId,
            @RequestBody SkillStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                skillMasterService.updateSkillStatus(publicId, request, principal.getUsername())));
    }

    // ===================================================================
    // EXCEL IMPORT
    // ===================================================================

    @GetMapping("/import/skill-template")
    @Operation(summary = "Download Excel import template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return skillImportService.generateTemplate();
    }

    @GetMapping("/skills/export")
    @Operation(summary = "Export skills to Excel with optional filters (read-only)")
    public ResponseEntity<byte[]> exportSkills(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String scopeCode,
            @RequestParam(required = false) String applicationTypeCode,
            @RequestParam(required = false) String bundleCode,
            @RequestParam(required = false) String applicationPublicId,
            @RequestParam(required = false) String status) throws IOException {
        return skillImportService.exportSkills(
                search, categoryCode, scopeCode, applicationTypeCode, bundleCode, applicationPublicId, status);
    }

    @PostMapping(value = "/import/skills/validate", consumes = "multipart/form-data")
    @Operation(summary = "Validate Excel file without importing")
    public ResponseEntity<ApiResponse<SkillValidationResult>> validateImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(skillImportService.validateOnly(file)));
    }

    @PostMapping(value = "/import/skills/confirm", consumes = "multipart/form-data")
    @Operation(summary = "Validate and confirm skill import from Excel")
    public ResponseEntity<ApiResponse<SkillImportResult>> confirmImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                skillImportService.confirmImport(file, principal.getUsername())));
    }

    // ===================================================================
    // IMPORT HISTORY
    // ===================================================================

    @GetMapping("/import/history")
    @Operation(summary = "Get import history with pagination and filters")
    public ResponseEntity<ApiResponse<PagedResponse<ImportHistoryDto>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(ApiResponse.ok(skillImportService.getHistory(
                page, size, templateType, statusCode, uploadedBy, fromDate, toDate)));
    }
}
