package com.skillmatrix.applications.controller;

import com.skillmatrix.applications.dto.ApplicationImportResult;
import com.skillmatrix.applications.dto.ApplicationMasterDto;
import com.skillmatrix.applications.dto.ApplicationStatusRequest;
import com.skillmatrix.applications.dto.ApplicationValidationResult;
import com.skillmatrix.applications.dto.CreateApplicationRequest;
import com.skillmatrix.applications.dto.DeleteApplicationResult;
import com.skillmatrix.applications.dto.UpdateApplicationRequest;
import com.skillmatrix.applications.service.ApplicationImportService;
import com.skillmatrix.applications.service.ApplicationMasterService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Admin-only Application Master Management (M4 stabilization enhancement —
 * Problem Statement Part A). Deliberately separate from the read-only
 * {@link ApplicationController} (used by ADMIN/LEAD_MANAGER/TECHNICIAN for
 * hierarchy lookups) so mutation operations remain cleanly ADMIN-gated without
 * affecting existing read-only consumers.
 */
@RestController
@RequestMapping("/api/v1/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Applications", description = "Admin-only Application Master management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminApplicationController {

    private final ApplicationMasterService applicationMasterService;
    private final ApplicationImportService applicationImportService;

    // ===================================================================
    // CRUD
    // ===================================================================

    @GetMapping
    @Operation(summary = "List applications with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationMasterDto>>> getApplications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String bundleCode,
            @RequestParam(required = false) String portfolioCode,
            @RequestParam(required = false) String lifecycleStatusCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "applicationCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(applicationMasterService.getApplications(
                page, size, search, typeCode, bundleCode, portfolioCode, lifecycleStatusCode, status,
                sortBy, sortDirection)));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get application detail by publicId")
    public ResponseEntity<ApiResponse<ApplicationMasterDto>> getApplication(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(applicationMasterService.getByPublicId(publicId)));
    }

    @PostMapping
    @Operation(summary = "Create a new application")
    public ResponseEntity<ApiResponse<ApplicationMasterDto>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                applicationMasterService.createApplication(request, principal.getUsername())));
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update an application")
    public ResponseEntity<ApiResponse<ApplicationMasterDto>> updateApplication(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationMasterService.updateApplication(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/{publicId}/status")
    @Operation(summary = "Activate or deactivate an application")
    public ResponseEntity<ApiResponse<ApplicationMasterDto>> updateStatus(
            @PathVariable String publicId,
            @RequestBody ApplicationStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationMasterService.updateStatus(publicId, request, principal.getUsername())));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Permanently delete an application. Blocked if business dependencies exist, "
            + "or if hard-delete is disabled in this environment.")
    public ResponseEntity<ApiResponse<DeleteApplicationResult>> deleteApplication(
            @PathVariable String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationMasterService.deleteApplication(publicId, principal.getUsername())));
    }

    // ===================================================================
    // IMPORT / EXPORT
    // ===================================================================

    @GetMapping("/template")
    @Operation(summary = "Download blank Excel template for Application Master import")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return applicationImportService.generateTemplate();
    }

    @GetMapping("/export")
    @Operation(summary = "Export applications to Excel with optional filters")
    public ResponseEntity<byte[]> exportApplications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String bundleCode,
            @RequestParam(required = false) String portfolioCode,
            @RequestParam(required = false) String lifecycleStatusCode,
            @RequestParam(required = false) String status) throws IOException {
        return applicationImportService.exportApplications(search, typeCode, bundleCode, portfolioCode, lifecycleStatusCode, status);
    }

    @PostMapping("/import/validate")
    @Operation(summary = "Validate Application Master import Excel file (no DB write)")
    public ResponseEntity<ApiResponse<ApplicationValidationResult>> validateImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(applicationImportService.validateOnly(file)));
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "Confirm and execute Application Master import (SMART_UPSERT, STRICT mode)")
    public ResponseEntity<ApiResponse<ApplicationImportResult>> confirmImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationImportService.confirmImport(file, principal.getUsername())));
    }
}
