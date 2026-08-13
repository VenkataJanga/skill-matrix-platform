package com.skillmatrix.admin.controller;

import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.AssignmentStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.CreateUserRequest;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.admin.dto.UpdateUserRequest;
import com.skillmatrix.admin.dto.UserDetailDto;
import com.skillmatrix.admin.dto.UserStatusRequest;
import com.skillmatrix.admin.dto.UserSummaryDto;
import com.skillmatrix.admin.service.AdminAssignmentService;
import com.skillmatrix.admin.service.AdminUserService;
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

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only user and assignment management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminAssignmentService adminAssignmentService;

    // ===================================================================
    // USER MANAGEMENT
    // ===================================================================

    @GetMapping("/users")
    @Operation(summary = "List users with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<UserSummaryDto>>> getUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String roleCode,
            @RequestParam(required = false)    String status,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc")      String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.getUsers(page, size, search, roleCode, status, sortBy, sortDirection)));
    }

    @GetMapping("/users/{publicId}")
    @Operation(summary = "Get full user profile by publicId")
    public ResponseEntity<ApiResponse<UserDetailDto>> getUser(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getUserByPublicId(publicId)));
    }

    @PostMapping("/users")
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserDetailDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminUserService.createUser(request, principal.getUsername())));
    }

    @PutMapping("/users/{publicId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserDetailDto>> updateUser(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.updateUser(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/users/{publicId}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<ApiResponse<UserDetailDto>> updateUserStatus(
            @PathVariable String publicId,
            @RequestBody UserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.updateUserStatus(publicId, request, principal.getUsername())));
    }

    // ===================================================================
    // ASSIGNMENT MANAGEMENT
    // ===================================================================

    @GetMapping("/application-assignments")
    @Operation(summary = "List assignments with pagination, search and filters")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationAssignmentDto>>> getAssignments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String applicationPublicId,
            @RequestParam(required = false)    String technicianPublicId,
            @RequestParam(required = false)    String typeCode,
            @RequestParam(required = false)    String bundleCode,
            @RequestParam(required = false)    String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.ok(adminAssignmentService.getAssignments(
                page, size, search, applicationPublicId, technicianPublicId,
                typeCode, bundleCode, status, sortBy, sortDirection)));
    }

    @GetMapping("/application-assignments/check-duplicate")
    @Operation(summary = "Check if an active assignment already exists for user + application")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkDuplicate(
            @RequestParam String userPublicId,
            @RequestParam String applicationPublicId) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAssignmentService.checkDuplicate(userPublicId, applicationPublicId)));
    }

    @PostMapping("/application-assignments")
    @Operation(summary = "Create a new technician assignment")
    public ResponseEntity<ApiResponse<ApplicationAssignmentDto>> createAssignment(
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminAssignmentService.createAssignment(request, principal.getUsername())));
    }

    @PutMapping("/application-assignments/{publicId}")
    @Operation(summary = "Update an existing assignment")
    public ResponseEntity<ApiResponse<ApplicationAssignmentDto>> updateAssignment(
            @PathVariable String publicId,
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAssignmentService.updateAssignment(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/application-assignments/{publicId}/status")
    @Operation(summary = "Activate or deactivate a single assignment")
    public ResponseEntity<ApiResponse<ApplicationAssignmentDto>> updateAssignmentStatus(
            @PathVariable String publicId,
            @RequestBody AssignmentStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAssignmentService.updateAssignmentStatus(publicId, request, principal.getUsername())));
    }

    @PatchMapping("/application-assignments/bulk-status")
    @Operation(summary = "Bulk activate or deactivate assignments")
    public ResponseEntity<ApiResponse<BulkStatusResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAssignmentService.bulkUpdateStatus(request, principal.getUsername())));
    }
}
