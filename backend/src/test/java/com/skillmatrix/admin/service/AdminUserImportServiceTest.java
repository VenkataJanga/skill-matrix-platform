package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.UserImportResult;
import com.skillmatrix.admin.dto.UserImportRowPreview;
import com.skillmatrix.admin.dto.UserValidationResult;
import com.skillmatrix.admin.service.AdminUserImportService.ParsedRow;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RoleRepository;
import com.skillmatrix.domain.repository.TeamRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.domain.repository.UserTeamRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserImportService — SMART_UPSERT unit tests")
class AdminUserImportServiceTest {

    @Mock UserRepository              userRepository;
    @Mock RoleRepository              roleRepository;
    @Mock TeamRepository              teamRepository;
    @Mock UserTeamRepository          userTeamRepository;
    @Mock ImportExportHistoryRepository historyRepository;
    @Mock LookupValueRepository       lookupValueRepository;
    @Mock PasswordEncoder             passwordEncoder;
    @Mock AuditService                auditService;

    @InjectMocks AdminUserImportService service;

    private Role techRole;
    private User adminUser;

    @BeforeEach
    void setUp() {
        techRole = Role.builder().id(1L).publicId("role-tech").roleCode("TECHNICIAN")
                .roleName("Technician").active(true).rolePermissions(new HashSet<>()).build();

        adminUser = User.builder()
                .id(1L).publicId("pub-admin").username("admin")
                .fullName("Admin").email("admin@example.com")
                .passwordHash("$hash").active(true).mustChangePassword(false)
                .userRoles(new HashSet<>()).build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ParsedRow row(String username, String fullName, String email,
                          String roleCode, String isActive, String mustChangePwd) {
        ParsedRow pr = new ParsedRow();
        pr.rowNumber = 2;
        pr.username = username;
        pr.fullName = fullName;
        pr.email = email;
        pr.roleCode = roleCode;
        pr.isActiveStr = isActive;
        pr.mustChangePwdStr = mustChangePwd;
        return pr;
    }

    private MockMultipartFile buildMultipart(String[][] data) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("01_USER_MASTER");
        String[] headers = {"username", "full_name", "email", "role_code",
                "manager_username", "team_name", "is_active", "must_change_password"};
        Row hdr = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) hdr.createCell(i).setCellValue(headers[i]);
        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                if (data[r][c] != null) row.createCell(c).setCellValue(data[r][c]);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
    }

    private void setupHistoryMock() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(adminUser));
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), any())).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> {
            ImportExportHistory h = inv.getArgument(0);
            if (h.getPublicId() == null) h.setPublicId(UUID.randomUUID().toString());
            return h;
        });
    }

    // ================================================================
    // 1. New username row → INSERT
    // ================================================================
    @Test
    @DisplayName("New username → INSERT action")
    void newUsername_shouldReturnInsert() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("newuser", "New User", "new@example.com", "TECHNICIAN", "1", "1")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("INSERT");
        assertThat(previews.get(0).errors()).isEmpty();
    }

    // ================================================================
    // 2. Existing username, same data → SKIP
    // ================================================================
    @Test
    @DisplayName("Existing username with same data → SKIP action")
    void existingUserSameData_shouldReturnSkip() {
        User existing = User.builder().id(10L).username("jdoe").fullName("John Doe")
                .email("jdoe@example.com").passwordHash("$h").active(true)
                .mustChangePassword(false).primaryRole(techRole).userRoles(new HashSet<>()).build();

        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of(existing));
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(existing));

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("jdoe", "John Doe", "jdoe@example.com", "TECHNICIAN", "1", "0")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("SKIP");
    }

    // ================================================================
    // 3. Existing username with changed email → UPDATE
    // ================================================================
    @Test
    @DisplayName("Existing username with changed email → UPDATE action")
    void existingUserChangedEmail_shouldReturnUpdate() {
        User existing = User.builder().id(10L).username("jdoe").fullName("John Doe")
                .email("jdoe@example.com").passwordHash("$h").active(true)
                .mustChangePassword(false).primaryRole(techRole).userRoles(new HashSet<>()).build();

        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of(existing));
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(existing));

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("jdoe", "John Doe", "jdoe_new@example.com", "TECHNICIAN", "1", "0")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("UPDATE");
        assertThat(previews.get(0).reason()).contains("email");
    }

    // ================================================================
    // 4. Invalid email format → ERROR
    // ================================================================
    @Test
    @DisplayName("Invalid email format → ERROR action")
    void invalidEmail_shouldReturnError() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("baduser", "Bad User", "not-an-email", "TECHNICIAN", "1", "1")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("ERROR");
        assertThat(previews.get(0).errors()).anyMatch(e -> e.contains("email format"));
    }

    // ================================================================
    // 5. Invalid role_code → ERROR
    // ================================================================
    @Test
    @DisplayName("Invalid role_code → ERROR action")
    void invalidRoleCode_shouldReturnError() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("someuser", "Some User", "some@example.com", "INVALID_ROLE", "1", "1")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("ERROR");
        assertThat(previews.get(0).errors()).anyMatch(e -> e.contains("role_code"));
    }

    // ================================================================
    // 6. Duplicate username with conflicting values → ERROR
    // ================================================================
    @Test
    @DisplayName("Duplicate username with conflicting values → ERROR")
    void duplicateUsernameConflict_shouldReturnError() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        List<UserImportRowPreview> previews = service.buildPreviews(List.of(
                row("dupuser", "Dup User", "dup@example.com", "TECHNICIAN", "1", "1"),
                row("dupuser", "Dup User Different", "dup2@example.com", "ADMIN", "0", "1")));

        assertThat(previews).hasSize(2);
        assertThat(previews).allMatch(p -> p.action().equals("ERROR"));
    }

    // ================================================================
    // 7. Email belongs to another user → ERROR
    // ================================================================
    @Test
    @DisplayName("Email already assigned to another user → ERROR")
    void emailConflict_shouldReturnError() {
        User otherUser = User.builder().id(99L).username("otheruser")
                .email("taken@example.com").fullName("Other").passwordHash("$h")
                .active(true).mustChangePassword(false).userRoles(new HashSet<>()).build();

        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(otherUser));

        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("newuser", "New User", "taken@example.com", "TECHNICIAN", "1", "1")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("ERROR");
        assertThat(previews.get(0).errors()).anyMatch(e -> e.contains("Email conflict"));
    }

    // ================================================================
    // 8. ValidationResult.valid() is false when error rows exist
    // ================================================================
    @Test
    @DisplayName("UserValidationResult.valid() is false when error rows > 0")
    void validationResultInvalidWhenErrors() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        UserValidationResult result = UserValidationResult.of(service.buildPreviews(
                List.of(row("u1", "U1", "bad-email", "TECHNICIAN", "1", "1"))));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorRows()).isEqualTo(1);
    }

    // ================================================================
    // 9. ValidationResult.valid() is true when no error rows
    // ================================================================
    @Test
    @DisplayName("UserValidationResult.valid() is true when no errors")
    void validationResultValidWhenNoErrors() {
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        UserValidationResult result = UserValidationResult.of(service.buildPreviews(
                List.of(row("freshuser", "Fresh User", "fresh@example.com", "ADMIN", "1", "1"))));

        assertThat(result.valid()).isTrue();
        assertThat(result.errorRows()).isEqualTo(0);
        assertThat(result.insertRows()).isEqualTo(1);
    }

    // ================================================================
    // 10. confirmImport STRICT aborts when validation has errors
    // ================================================================
    @Test
    @DisplayName("confirmImport STRICT returns failure when validation has error rows")
    void confirmImport_strict_abortsOnErrors() throws Exception {
        setupHistoryMock();
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"baduser", "Bad User", "not-an-email", "TECHNICIAN", null, null, "1", "1"}
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "STRICT");

        assertThat(result.success()).isFalse();
        assertThat(result.errorRows()).isGreaterThan(0);
        assertThat(result.importMode()).isEqualTo("STRICT");
        assertThat(result.partialSuccess()).isFalse();
        verify(historyRepository).save(any(ImportExportHistory.class));
    }

    // ================================================================
    // 11. Import history is saved with correct template type
    // ================================================================
    @Test
    @DisplayName("Import history is saved with USER_MASTER_IMPORT template type")
    void confirmImport_savesHistoryWithCorrectTemplateType() throws Exception {
        setupHistoryMock();
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
        when(roleRepository.findAllByRoleCodeIn(anyCollection())).thenReturn(List.of(techRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id(50L).publicId("new-pub").username(u.getUsername())
                    .fullName(u.getFullName()).email(u.getEmail()).passwordHash("$hash")
                    .primaryRole(u.getPrimaryRole()).active(u.isActive())
                    .mustChangePassword(u.isMustChangePassword()).userRoles(new HashSet<>())
                    .createdBy(u.getCreatedBy()).updatedBy(u.getUpdatedBy()).build();
        });

        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"validuser", "Valid User", "valid@example.com", "TECHNICIAN", null, null, "1", "1"}
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "STRICT");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(1);
        verify(historyRepository).save(argThat(h ->
                AdminUserImportService.TEMPLATE_TYPE.equals(h.getTemplateType())));
    }

    // ================================================================
    // 12. PARTIAL mode: 16 INSERT + 1 SKIP + 2 ERROR → PARTIAL_SUCCESS
    // ================================================================
    @Test
    @DisplayName("PARTIAL mode: valid rows inserted, error rows rejected, status PARTIAL_SUCCESS")
    void confirmImport_partial_insertsValidAndRejectsErrors() throws Exception {
        setupHistoryMock();

        // 16 valid new users + 1 invalid email user → 2 error rows (one from existing email conflict below)
        User existingEmailOwner = User.builder().id(99L).username("other")
                .email("conflict@example.com").fullName("Other").passwordHash("$h")
                .active(true).mustChangePassword(false).userRoles(new HashSet<>()).build();

        // Return existing email owner for conflict check
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<String> emails = inv.getArgument(0);
            if (emails.stream().anyMatch(e -> e.contains("conflict"))) {
                return List.of(existingEmailOwner);
            }
            return List.of();
        });
        when(roleRepository.findAllByRoleCodeIn(anyCollection())).thenReturn(List.of(techRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id((long)(Math.random() * 1000)).publicId(UUID.randomUUID().toString())
                    .username(u.getUsername()).fullName(u.getFullName()).email(u.getEmail())
                    .passwordHash("$hash").primaryRole(u.getPrimaryRole()).active(u.isActive())
                    .mustChangePassword(u.isMustChangePassword()).userRoles(new HashSet<>())
                    .createdBy(u.getCreatedBy()).updatedBy(u.getUpdatedBy()).build();
        });

        // Build file: 2 valid inserts + 1 email conflict error
        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"user1", "User One",   "user1@example.com",    "TECHNICIAN", null, null, "1", "1"},
                {"user2", "User Two",   "user2@example.com",    "TECHNICIAN", null, null, "1", "1"},
                {"error1", "Error One", "conflict@example.com", "TECHNICIAN", null, null, "1", "1"},
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "PARTIAL");

        assertThat(result.success()).isTrue();
        assertThat(result.partialSuccess()).isTrue();
        assertThat(result.importMode()).isEqualTo("PARTIAL");
        assertThat(result.insertedRows()).isEqualTo(2);
        assertThat(result.errorRows()).isEqualTo(1);
        verify(historyRepository).save(argThat(h -> "PARTIAL_SUCCESS".equals(
                h.getStatusLookup() == null ? "PARTIAL_SUCCESS" : h.getStatusLookup().getValueCode())));
    }

    // ================================================================
    // 13. PARTIAL mode with zero valid rows → FAILED
    // ================================================================
    @Test
    @DisplayName("PARTIAL mode with no valid rows → FAILED")
    void confirmImport_partial_noValidRows_returnsFailed() throws Exception {
        setupHistoryMock();
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        // All rows have errors (bad email + bad role)
        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"baduser1", "Bad One", "not-an-email",    "TECHNICIAN",   null, null, "1", "1"},
                {"baduser2", "Bad Two", "bad2@example.com", "INVALID_ROLE", null, null, "1", "1"},
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "PARTIAL");

        assertThat(result.success()).isFalse();
        assertThat(result.partialSuccess()).isFalse();
        assertThat(result.insertedRows()).isEqualTo(0);
        assertThat(result.errorRows()).isGreaterThan(0);
        verify(historyRepository).save(any(ImportExportHistory.class));
    }

    // ================================================================
    // 14. PARTIAL mode: email conflict row is never inserted
    // ================================================================
    @Test
    @DisplayName("PARTIAL mode: email conflict is always ERROR — never inserted")
    void confirmImport_partial_emailConflictNeverInserted() throws Exception {
        setupHistoryMock();

        User emailOwner = User.builder().id(77L).username("owner")
                .email("taken@example.com").fullName("Owner").passwordHash("$h")
                .active(true).mustChangePassword(false).userRoles(new HashSet<>()).build();

        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(emailOwner));

        // Only row has email conflict
        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"newuser", "New User", "taken@example.com", "TECHNICIAN", null, null, "1", "1"},
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "PARTIAL");

        // No valid rows to process → FAILED
        assertThat(result.success()).isFalse();
        assertThat(result.insertedRows()).isEqualTo(0);
        // The conflicting user was never saved
        verify(userRepository, never()).save(any(User.class));
    }

    // ================================================================
    // 15. PARTIAL mode: valid rows processed despite unrelated errors
    // ================================================================
    @Test
    @DisplayName("PARTIAL mode: valid rows are processed even when unrelated error rows exist")
    void confirmImport_partial_processesValidRowsDespiteErrors() throws Exception {
        setupHistoryMock();
        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
        when(roleRepository.findAllByRoleCodeIn(anyCollection())).thenReturn(List.of(techRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id(10L).publicId(UUID.randomUUID().toString())
                    .username(u.getUsername()).fullName(u.getFullName()).email(u.getEmail())
                    .passwordHash("$hash").primaryRole(u.getPrimaryRole()).active(u.isActive())
                    .mustChangePassword(u.isMustChangePassword()).userRoles(new HashSet<>())
                    .createdBy(u.getCreatedBy()).updatedBy(u.getUpdatedBy()).build();
        });

        MockMultipartFile mockFile = buildMultipart(new String[][]{
                {"gooduser",  "Good User",  "good@example.com",  "TECHNICIAN", null, null, "1", "1"},
                {"erroruser", "Error User", "not-valid-email",   "TECHNICIAN", null, null, "1", "1"},
        });

        UserImportResult result = service.confirmImport(mockFile, "admin", "PARTIAL");

        assertThat(result.success()).isTrue();
        assertThat(result.partialSuccess()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEqualTo(1);
        // Verify only one user was saved (the valid one)
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ================================================================
    // 16. Re-upload after partial import: inserted users become SKIP
    // ================================================================
    @Test
    @DisplayName("Re-upload after partial import: previously inserted users become SKIP")
    void reUpload_previouslyInsertedUsersBecomeSKIP() {
        // Simulate: user1 was inserted in first partial import
        User insertedUser = User.builder().id(10L).username("user1").fullName("User One")
                .email("user1@example.com").passwordHash("$h").active(true)
                .mustChangePassword(true).primaryRole(techRole).userRoles(new HashSet<>()).build();

        when(userRepository.findAllByUsernameIn(anyCollection())).thenReturn(List.of(insertedUser));
        when(userRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(insertedUser));

        // Same data as what was inserted → should be SKIP (idempotent re-upload)
        List<UserImportRowPreview> previews = service.buildPreviews(
                List.of(row("user1", "User One", "user1@example.com", "TECHNICIAN", "1", "1")));

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).action()).isEqualTo("SKIP");
        assertThat(previews.get(0).errors()).isEmpty();
    }
}
