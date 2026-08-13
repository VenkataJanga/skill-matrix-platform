package com.skillmatrix.applications.controller;

import com.skillmatrix.applications.dto.ApplicationDto;
import com.skillmatrix.applications.dto.ApplicationTypeDto;
import com.skillmatrix.applications.dto.BundleDto;
import com.skillmatrix.applications.service.ApplicationService;
import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Application hierarchy lookup endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/application-types")
    @Operation(summary = "Get all active application types")
    public ResponseEntity<ApiResponse<List<ApplicationTypeDto>>> getApplicationTypes() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getAllActiveApplicationTypes()));
    }

    @GetMapping("/bundles")
    @Operation(summary = "Get all active bundles")
    public ResponseEntity<ApiResponse<List<BundleDto>>> getBundles() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getAllActiveBundles()));
    }

    @GetMapping("/applications")
    @Operation(summary = "Get active applications filtered by typeCode and/or bundleCode")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getApplications(
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String bundleCode) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getFilteredApplications(typeCode, bundleCode)));
    }

    @GetMapping("/technician/my-applications")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Get applications assigned to the authenticated technician")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getMyApplications(principal)));
    }

    @GetMapping("/lead/applications")
    @PreAuthorize("hasRole('LEAD_MANAGER')")
    @Operation(summary = "Get applications visible to the authenticated Lead Manager")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getLeadApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getLeadApplications(principal)));
    }
}
