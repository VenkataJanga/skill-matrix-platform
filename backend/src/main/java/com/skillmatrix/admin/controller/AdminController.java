package com.skillmatrix.admin.controller;

import com.skillmatrix.admin.dto.AdminPasswordResetResponse;
import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentImportResult;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.AssignmentStatusRequest;
import com.skillmatrix.admin.dto.AssignmentValidationResult;
import com.skillmatrix.admin.dto.BulkAssignmentRequest;
import com.skillmatrix.admin.dto.BulkAssignmentResult;
import com.skillmatrix.admin.dto.BulkDeleteRequest;
import com.skillmatrix.admin.dto.BulkDeleteResponse;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.CreateUserRequest;
import com.skillmatrix.admin.dto.DeleteUserResult;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.admin.dto.UpdateUserRequest;
import com.skillmatrix.admin.dto.UserDetailDto;
import com.skillmatrix.admin.dto.UserImportResult;
import com.skillmatrix.admin.dto.UserStatusRequest;
import com.skillmatrix.admin.dto.UserSummaryDto;
import com.skillmatrix.admin.dto.UserValidationResult;
import com.skillmatrix.admin.service.AdminAssignmentService;
import com.skillmatrix.admin.service.AdminUserDeleteService;
import com.skillmatrix.admin.service.AdminUserImportService;
import com.skillmatrix.admin.service.AdminUserService;
import com.skillmatrix.admin.service.AssignmentImportService;
import com.skillmatrix.auth.service.PasswordResetService;
import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import com.skillmatrix.skills.dto.ImportHistoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
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
@Tag(name = "Admin", description = "Admin-only user and assignment management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminAssignmentService adminAssignmentService;
    private final AdminUserImportService adminUserImportService;
    private final AdminUserDeleteService adminUserDeleteService;
    private final AssignmentImportService assignmentImportService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;
    private final Environment environment;

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
    // USER IMPORT / EXPORT
    // ===================================================================

    @GetMapping("/users/template")
    @Operation(summary = "Download blank Excel template for user import")
    public ResponseEntity<byte[]> downloadUserTemplate() throws IOException {
        return adminUserImportService.generateTemplate();
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export users to Excel with optional filters")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String status) throws IOException {
        return adminUserImportService.exportUsers(search, roleCode, status);
    }

    @PostMapping("/users/import/validate")
    @Operation(summary = "Validate user import Excel file (no DB write)")
    public ResponseEntity<ApiResponse<UserValidationResult>> validateUserImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(adminUserImportService.validateOnly(file)));
    }

    @PostMapping("/users/import/confirm")
    @Operation(summary = "Confirm and execute user import. importMode=STRICT (default) aborts on any error; PARTIAL imports valid rows and rejects error rows.")
    public ResponseEntity<ApiResponse<UserImportResult>> confirmUserImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "STRICT") String importMode,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserImportService.confirmImport(file, principal.getUsername(), importMode)));
    }

    @DeleteMapping("/users/{publicId}")
    @Operation(summary = "Permanently delete a user. Blocked if protected records exist.")
    public ResponseEntity<ApiResponse<DeleteUserResult>> deleteUser(
            @PathVariable String publicId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeleteUserResult result = adminUserDeleteService.deleteUser(
                publicId, principal.getUsername(), principal.getPublicId(), reason);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/users/bulk-delete")
    @Operation(summary = "Bulk permanently delete users. Each user evaluated independently; blocked users do not prevent deletion of others.")
    public ResponseEntity<ApiResponse<BulkDeleteResponse>> bulkDeleteUsers(
            @jakarta.validation.Valid @RequestBody BulkDeleteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BulkDeleteResponse response = adminUserDeleteService.bulkDeleteUsers(
                request, principal.getUsername(), principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/users/import/history")
    @Operation(summary = "Get user import history")
    public ResponseEntity<ApiResponse<PagedResponse<ImportHistoryDto>>> getUserImportHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String uploadedBy) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserImportService.getImportHistory(page, size, statusCode, uploadedBy)));
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

    @PostMapping("/application-assignments/bulk")
    @Operation(summary = "Multi-Technician Assignment — assign several technicians to one application in a single batch. "
            + "Each technician is processed independently (controlled partial processing); one invalid/duplicate "
            + "technician never blocks unrelated valid technicians in the same batch.")
    public ResponseEntity<ApiResponse<BulkAssignmentResult>> createBulkAssignments(
            @Valid @RequestBody BulkAssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAssignmentService.createBulkAssignments(request, principal.getUsername())));
    }

    // ===================================================================
    // ASSIGNMENT IMPORT / EXPORT
    // ===================================================================

    @GetMapping("/application-assignments/template")
    @Operation(summary = "Download blank Excel template for Technician Assignment import")
    public ResponseEntity<byte[]> downloadAssignmentTemplate() throws IOException {
        return assignmentImportService.generateTemplate();
    }

    @GetMapping("/application-assignments/export")
    @Operation(summary = "Export technician assignments to Excel with optional filters")
    public ResponseEntity<byte[]> exportAssignmentsExcel(
            @RequestParam(required = false) String technicianPublicId,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String bundleCode,
            @RequestParam(required = false) String applicationPublicId,
            @RequestParam(required = false) String status) throws IOException {
        return assignmentImportService.exportAssignments(technicianPublicId, typeCode, bundleCode, applicationPublicId, status);
    }

    @PostMapping("/application-assignments/import/validate")
    @Operation(summary = "Validate Technician Assignment import Excel file (no DB write)")
    public ResponseEntity<ApiResponse<AssignmentValidationResult>> validateAssignmentImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(assignmentImportService.validateOnly(file)));
    }

    @PostMapping("/application-assignments/import/confirm")
    @Operation(summary = "Confirm and execute Technician Assignment import (SMART_UPSERT, controlled PARTIAL processing — "
            + "valid rows always import even if some rows are rejected)")
    public ResponseEntity<ApiResponse<AssignmentImportResult>> confirmAssignmentImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentImportService.confirmImport(file, principal.getUsername())));
    }

    // ===================================================================
    // ADMIN-ASSISTED PASSWORD RESET (Development/Pilot fallback)
    // ===================================================================

    @PostMapping("/users/{publicId}/password-reset")
    @Operation(summary = "Admin-assisted password reset fallback. Generates a secure one-time reset link and "
            + "sets must_change_password=true. The Admin never sees, retrieves, or chooses the user's permanent "
            + "password.")
    public ResponseEntity<ApiResponse<AdminPasswordResetResponse>> adminResetPassword(
            @PathVariable String publicId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User targetUser = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("User not found: " + publicId, HttpStatus.NOT_FOUND));

        String resetLink = passwordResetService.adminInitiateReset(
                targetUser, principal.getUsername(), resolveIp(httpRequest));

        boolean devExpose = List.of(environment.getActiveProfiles()).contains("dev");
        AdminPasswordResetResponse response = AdminPasswordResetResponse.of(
                targetUser.getPublicId(), targetUser.getUsername(), devExpose ? resetLink : null);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
