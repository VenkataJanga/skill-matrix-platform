package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.UserImportResult;
import com.skillmatrix.admin.dto.UserImportRowPreview;
import com.skillmatrix.admin.dto.UserValidationResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.Team;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.entity.UserTeam;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RoleRepository;
import com.skillmatrix.domain.repository.TeamRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.domain.repository.UserTeamRepository;
import com.skillmatrix.skills.dto.ImportHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserImportService {

    static final String TEMPLATE_TYPE = "USER_MASTER_IMPORT";
    static final String ACTION_INSERT = "INSERT";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_SKIP   = "SKIP";
    static final String ACTION_ERROR  = "ERROR";

    private static final String DEFAULT_PASSWORD = "Password@123";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> VALID_ROLE_CODES =
            Set.of("ADMIN", "LEAD_MANAGER", "TECHNICIAN");

    private static final String H_USERNAME         = "username";
    private static final String H_FULL_NAME        = "full_name";
    private static final String H_EMAIL            = "email";
    private static final String H_ROLE_CODE        = "role_code";
    private static final String H_MANAGER_USERNAME = "manager_username";
    private static final String H_TEAM_NAME        = "team_name";
    private static final String H_IS_ACTIVE        = "is_active";
    private static final String H_MUST_CHANGE_PWD  = "must_change_password";
    private static final String H_REMARKS          = "remarks";

    private final UserRepository              userRepository;
    private final RoleRepository              roleRepository;
    private final TeamRepository              teamRepository;
    private final UserTeamRepository          userTeamRepository;
    private final ImportExportHistoryRepository historyRepository;
    private final LookupValueRepository       lookupValueRepository;
    private final PasswordEncoder             passwordEncoder;
    private final AuditService                auditService;

    // ================================================================
    // Template
    // ================================================================
    public ResponseEntity<byte[]> generateTemplate() throws IOException {
        log.info("User master template requested");
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createInstructionsSheet(wb);
            createUserMasterSheet(wb);
            createAllowedValuesSheet(wb);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "user_import_template.xlsx");
        }
    }

    // ================================================================
    // Export
    // ================================================================
    public ResponseEntity<byte[]> exportUsers(String search, String roleCode, String status) throws IOException {
        log.info("User export: search={} role={} status={}", search, roleCode, status);
        Boolean activeFilter = parseActiveFilter(status);
        Page<User> page = userRepository.searchUsers(
                blank(search) ? null : search.trim(),
                blank(roleCode) ? null : roleCode.trim(),
                activeFilter, PageRequest.of(0, Integer.MAX_VALUE));
        List<User> users = page.getContent();
        log.info("Exporting {} users", users.size());

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("users_export");
            XSSFCellStyle hs = buildHeaderStyle(wb);
            String[] cols = {"username","full_name","email","role_code","manager_username",
                    "team_name","is_active","must_change_password","created_at","updated_at"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs); }

            int ri = 1;
            for (User u : users) {
                Row row = sheet.createRow(ri++);
                setCellText(row, 0, u.getUsername());
                setCellText(row, 1, u.getFullName());
                setCellText(row, 2, u.getEmail());
                setCellText(row, 3, u.getPrimaryRole() != null ? u.getPrimaryRole().getRoleCode() : "");
                setCellText(row, 4, u.getManager() != null ? u.getManager().getUsername() : "");
                setCellText(row, 5, resolveTeamNameForExport(u));
                row.createCell(6).setCellValue(u.isActive() ? 1 : 0);
                row.createCell(7).setCellValue(u.isMustChangePassword() ? 1 : 0);
                setCellText(row, 8, u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                setCellText(row, 9, u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "users_export.xlsx");
        }
    }

    // ================================================================
    // Validate only
    // ================================================================
    public UserValidationResult validateOnly(MultipartFile file) throws IOException {
        log.info("User import validation started: file={}", file.getOriginalFilename());
        List<ParsedRow> parsed;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            parsed = parseUserMasterSheet(wb);
        }
        List<UserImportRowPreview> previews = buildPreviews(parsed);
        UserValidationResult result = UserValidationResult.of(previews);
        log.info("Validation done: total={} insert={} update={} skip={} error={}",
                result.totalRows(), result.insertRows(), result.updateRows(), result.skipRows(), result.errorRows());
        return result;
    }

    // ================================================================
    // Confirm import  (importMode = STRICT | PARTIAL)
    // ================================================================
    @Transactional
    public UserImportResult confirmImport(MultipartFile file, String actorUsername,
                                          String importMode) throws IOException {
        final boolean partial = "PARTIAL".equalsIgnoreCase(importMode);
        log.info("User import confirm: file={} mode={} by={}", file.getOriginalFilename(), importMode, actorUsername);

        List<ParsedRow> parsed;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            parsed = parseUserMasterSheet(wb);
        }
        List<UserImportRowPreview> previews = buildPreviews(parsed);
        UserValidationResult validation = UserValidationResult.of(previews);

        // STRICT: any error row blocks the entire import (original behaviour)
        if (!partial && !validation.valid()) {
            log.warn("Import aborted (STRICT): {} errors in file={}", validation.errorRows(), file.getOriginalFilename());
            saveHistory(file.getOriginalFilename(), actorUsername, validation.totalRows(),
                    0, 0, 0, validation.errorRows(), "STRICT", "FAILED",
                    "Blocked: " + validation.errorRows() + " error row(s).");
            return new UserImportResult(false, validation.totalRows(), 0, 0, 0,
                    validation.errorRows(), null,
                    "Import failed: " + validation.errorRows() + " error row(s) must be fixed.",
                    "STRICT", false);
        }

        // PARTIAL: if no valid rows exist at all → fail immediately
        int validRowCount = validation.insertRows() + validation.updateRows();
        if (partial && validRowCount == 0) {
            log.warn("Import aborted (PARTIAL): no valid rows to process in file={}", file.getOriginalFilename());
            saveHistory(file.getOriginalFilename(), actorUsername, validation.totalRows(),
                    0, 0, 0, validation.errorRows(), "PARTIAL", "FAILED",
                    "No valid rows to import; " + validation.errorRows() + " error row(s) found.");
            return new UserImportResult(false, validation.totalRows(), 0, 0,
                    validation.skipRows(), validation.errorRows(), null,
                    "Import failed: no valid rows to process. " + validation.errorRows() + " error row(s) must be fixed.",
                    "PARTIAL", false);
        }

        // Build work sets — ERROR rows are always excluded regardless of mode
        Map<String, ParsedRow> insertMap = new LinkedHashMap<>();
        Map<String, ParsedRow> updateMap = new LinkedHashMap<>();
        int skipped = 0;
        for (int i = 0; i < previews.size(); i++) {
            ParsedRow pr = parsed.get(i);
            switch (previews.get(i).action()) {
                case ACTION_INSERT -> insertMap.put(pr.username, pr);
                case ACTION_UPDATE -> updateMap.put(pr.username, pr);
                case ACTION_SKIP   -> skipped++;
                // ACTION_ERROR — always excluded, never processed
            }
        }

        Map<String, User> dbUsers    = bulkLoadUsers(updateMap.keySet());
        Map<String, Role> roleMap    = bulkLoadRoles(insertMap, updateMap);
        Map<String, User> managerMap = bulkLoadManagers(insertMap, updateMap);
        Map<String, Team> teamMap    = bulkLoadTeams(insertMap, updateMap);

        int inserted = 0, updated = 0;

        for (ParsedRow pr : insertMap.values()) {
            User saved = userRepository.save(buildUserForInsert(pr, roleMap, managerMap, actorUsername));
            assignTeam(saved, pr.teamName, teamMap);
            inserted++;
        }

        for (Map.Entry<String, ParsedRow> e : updateMap.entrySet()) {
            User existing = dbUsers.get(e.getKey());
            if (existing == null) continue;
            applyUpdateFromRow(existing, e.getValue(), roleMap, managerMap, actorUsername);
            userRepository.save(existing);
            assignTeamIfNew(existing, e.getValue().teamName, teamMap);
            updated++;
        }

        int errorRows     = validation.errorRows();
        boolean isPartialSuccess = partial && errorRows > 0;
        String statusCode = isPartialSuccess ? "PARTIAL_SUCCESS" : "SUCCESS";

        String auditDetail = "{\"inserted\":" + inserted + ",\"updated\":" + updated
                + ",\"skipped\":" + skipped + ",\"rejected\":" + errorRows + "}";
        String auditAction = isPartialSuccess ? "USER_IMPORT_PARTIAL" : "USER_IMPORT_CONFIRM";
        String auditMessage = isPartialSuccess
                ? "User Master import completed: " + inserted + " inserted, " + updated
                  + " updated, " + skipped + " skipped, " + errorRows + " rejected."
                : "User Master import completed: " + inserted + " inserted, " + updated
                  + " updated, " + skipped + " skipped.";

        ImportExportHistory history = saveHistory(file.getOriginalFilename(), actorUsername,
                validation.totalRows(), inserted, updated, skipped, errorRows,
                importMode.toUpperCase(), statusCode, null);

        auditService.log(auditAction, AuditService.ENTITY_USER, null, actorUsername,
                null, null, null, auditDetail);
        log.info("{} mode={} inserted={} updated={} skipped={} rejected={} by={}",
                auditAction, importMode, inserted, updated, skipped, errorRows, actorUsername);

        String resultMessage = isPartialSuccess
                ? "Import completed with partial success. " + inserted + " inserted, "
                  + updated + " updated, " + skipped + " skipped, " + errorRows + " rejected."
                : "Import successful: " + inserted + " inserted, " + updated
                  + " updated, " + skipped + " skipped.";

        return new UserImportResult(true, validation.totalRows(), inserted, updated, skipped,
                errorRows, history.getPublicId(), resultMessage,
                importMode.toUpperCase(), isPartialSuccess);
    }

    /** Backward-compatible overload — defaults to STRICT. */
    @Transactional
    public UserImportResult confirmImport(MultipartFile file, String actorUsername) throws IOException {
        return confirmImport(file, actorUsername, "STRICT");
    }

    // ================================================================
    // Import history
    // ================================================================
    public PagedResponse<ImportHistoryDto> getImportHistory(int page, int size, String statusCode, String uploadedBy) {
        String cleanStatus = blank(statusCode) ? null : statusCode.trim();
        String cleanUser   = blank(uploadedBy) ? null : uploadedBy.trim();
        Page<ImportExportHistory> result = historyRepository.searchHistory(
                TEMPLATE_TYPE, cleanStatus, cleanUser, null, null, PageRequest.of(page, size));
        return PagedResponse.of(result.getContent().stream().map(ImportHistoryDto::from).toList(), result);
    }

    // ================================================================
    // SMART_UPSERT core
    // ================================================================
    List<UserImportRowPreview> buildPreviews(List<ParsedRow> parsed) {
        // Phase 1: detect within-file duplicates
        Map<String, List<ParsedRow>> byUsername = new LinkedHashMap<>();
        for (ParsedRow pr : parsed) {
            if (!blank(pr.username)) byUsername.computeIfAbsent(pr.username, k -> new ArrayList<>()).add(pr);
        }
        Set<ParsedRow> duplicateConflict = new HashSet<>();
        Set<ParsedRow> duplicateSkip    = new HashSet<>();
        for (Map.Entry<String, List<ParsedRow>> e : byUsername.entrySet()) {
            List<ParsedRow> group = e.getValue();
            if (group.size() > 1) {
                ParsedRow first = group.get(0);
                boolean allSame = group.stream().skip(1).allMatch(r -> r.sameData(first));
                if (allSame) { for (int i = 1; i < group.size(); i++) duplicateSkip.add(group.get(i)); }
                else duplicateConflict.addAll(group);
            }
        }

        // Phase 2: bulk-load emails & usernames from DB for cross-validation
        Set<String> usernamesToCheck = parsed.stream()
                .filter(pr -> !blank(pr.username) && !duplicateConflict.contains(pr))
                .map(pr -> pr.username).collect(Collectors.toSet());
        Map<String, User> dbUserMap = userRepository.findAllByUsernameIn(usernamesToCheck).stream()
                .collect(Collectors.toMap(User::getUsername, u -> u));

        Set<String> emailsToCheck = parsed.stream()
                .filter(pr -> !blank(pr.email) && !duplicateConflict.contains(pr))
                .map(pr -> pr.email.toLowerCase()).collect(Collectors.toSet());
        Map<String, User> dbEmailMap = userRepository.findAllByEmailIn(emailsToCheck).stream()
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u));

        // Phase 3: build per-row preview
        List<UserImportRowPreview> previews = new ArrayList<>();
        for (ParsedRow pr : parsed) {
            previews.add(buildPreview(pr, dbUserMap, dbEmailMap, duplicateConflict, duplicateSkip));
        }
        return previews;
    }

    private UserImportRowPreview buildPreview(ParsedRow pr, Map<String, User> dbUserMap,
            Map<String, User> dbEmailMap, Set<ParsedRow> conflictSet, Set<ParsedRow> skipSet) {

        if (duplicateConflict(pr, conflictSet)) {
            return UserImportRowPreview.error(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                    "Duplicate username with conflicting values in file",
                    List.of("Conflicting duplicate username: " + pr.username));
        }
        if (skipSet.contains(pr)) {
            return UserImportRowPreview.of(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                    ACTION_SKIP, "Duplicate row — same data as earlier row in file");
        }

        // Required field validation
        List<String> errors = new ArrayList<>();
        if (blank(pr.username))  errors.add("username is required");
        if (blank(pr.fullName))  errors.add("full_name is required");
        if (blank(pr.email))     errors.add("email is required");
        else if (!EMAIL_PATTERN.matcher(pr.email).matches()) errors.add("email format is invalid");
        if (blank(pr.roleCode))  errors.add("role_code is required");
        else if (!VALID_ROLE_CODES.contains(pr.roleCode)) errors.add("role_code must be ADMIN, LEAD_MANAGER, or TECHNICIAN");
        if (blank(pr.isActiveStr)) errors.add("is_active is required (1 or 0)");
        else if (!pr.isActiveStr.equals("0") && !pr.isActiveStr.equals("1")) errors.add("is_active must be 1 or 0");
        if (blank(pr.mustChangePwdStr)) errors.add("must_change_password is required (1 or 0)");
        else if (!pr.mustChangePwdStr.equals("0") && !pr.mustChangePwdStr.equals("1")) errors.add("must_change_password must be 1 or 0");

        if (!errors.isEmpty()) {
            return UserImportRowPreview.error(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                    "Validation error(s)", errors);
        }

        User existingUser = dbUserMap.get(pr.username);

        if (existingUser == null) {
            // New user — check email not already taken by a different user
            User emailOwner = dbEmailMap.get(pr.email.toLowerCase());
            if (emailOwner != null) {
                return UserImportRowPreview.error(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                        "Email already assigned to another user: " + emailOwner.getUsername(),
                        List.of("Email conflict: " + pr.email + " belongs to " + emailOwner.getUsername()));
            }
            return UserImportRowPreview.of(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                    ACTION_INSERT, "New user — will be created");
        }

        // Existing user — check email conflict if changed
        if (!existingUser.getEmail().equalsIgnoreCase(pr.email)) {
            User emailOwner = dbEmailMap.get(pr.email.toLowerCase());
            if (emailOwner != null && !emailOwner.getUsername().equalsIgnoreCase(pr.username)) {
                return UserImportRowPreview.error(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                        "Email already assigned to another user: " + emailOwner.getUsername(),
                        List.of("Email conflict: " + pr.email));
            }
        }

        List<String> changes = detectChanges(pr, existingUser);
        if (changes.isEmpty()) {
            return UserImportRowPreview.of(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                    ACTION_SKIP, "Same data already exists — no change needed");
        }
        return UserImportRowPreview.of(pr.rowNumber, pr.username, pr.fullName, pr.email, pr.roleCode,
                ACTION_UPDATE, "Will update: " + String.join(", ", changes));
    }

    private boolean duplicateConflict(ParsedRow pr, Set<ParsedRow> conflictSet) {
        return conflictSet.contains(pr);
    }

    private List<String> detectChanges(ParsedRow pr, User existing) {
        List<String> changes = new ArrayList<>();
        if (!blank(pr.fullName) && !pr.fullName.equalsIgnoreCase(existing.getFullName())) changes.add("full_name");
        if (!blank(pr.email) && !pr.email.equalsIgnoreCase(existing.getEmail())) changes.add("email");
        if (!blank(pr.roleCode)) {
            String dbRole = existing.getPrimaryRole() != null ? existing.getPrimaryRole().getRoleCode() : null;
            if (!pr.roleCode.equalsIgnoreCase(nullSafe(dbRole))) changes.add("role_code");
        }
        if (!blank(pr.isActiveStr) && parseFlag(pr.isActiveStr) != existing.isActive()) changes.add("is_active");
        if (!blank(pr.mustChangePwdStr) && parseFlag(pr.mustChangePwdStr) != existing.isMustChangePassword()) changes.add("must_change_password");
        if (!blank(pr.managerUsername)) {
            String dbMgr = existing.getManager() != null ? existing.getManager().getUsername() : null;
            if (!pr.managerUsername.equalsIgnoreCase(nullSafe(dbMgr))) changes.add("manager_username");
        }
        return changes;
    }

    // ================================================================
    // Entity builders
    // ================================================================
    private User buildUserForInsert(ParsedRow pr, Map<String, Role> roleMap,
            Map<String, User> managerMap, String actor) {
        Role role = roleMap.get(pr.roleCode);
        User manager = blank(pr.managerUsername) ? null : managerMap.get(pr.managerUsername);
        User user = User.builder()
                .publicId(UUID.randomUUID().toString())
                .username(pr.username)
                .fullName(pr.fullName)
                .email(pr.email)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .primaryRole(role)
                .manager(manager)
                .active(blank(pr.isActiveStr) || parseFlag(pr.isActiveStr))
                .mustChangePassword(!blank(pr.mustChangePwdStr) ? parseFlag(pr.mustChangePwdStr) : true)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        if (role != null) {
            UserRole ur = UserRole.builder().user(user).role(role)
                    .assignedAt(LocalDateTime.now()).assignedBy(actor).active(true).build();
            user.getUserRoles().add(ur);
        }
        return user;
    }

    private void applyUpdateFromRow(User existing, ParsedRow pr, Map<String, Role> roleMap,
            Map<String, User> managerMap, String actor) {
        if (!blank(pr.fullName))       existing.setFullName(pr.fullName);
        if (!blank(pr.email))          existing.setEmail(pr.email);
        if (!blank(pr.isActiveStr))    existing.setActive(parseFlag(pr.isActiveStr));
        if (!blank(pr.mustChangePwdStr)) existing.setMustChangePassword(parseFlag(pr.mustChangePwdStr));
        if (!blank(pr.managerUsername)) {
            existing.setManager(managerMap.get(pr.managerUsername));
        }
        if (!blank(pr.roleCode)) {
            Role role = roleMap.get(pr.roleCode);
            if (role != null) {
                existing.setPrimaryRole(role);
                existing.getUserRoles().forEach(ur -> ur.setActive(false));
                UserRole newRole = UserRole.builder().user(existing).role(role)
                        .assignedAt(LocalDateTime.now()).assignedBy(actor).active(true).build();
                existing.getUserRoles().add(newRole);
            }
        }
        existing.setUpdatedBy(actor);
    }

    // ================================================================
    // Team helpers
    // ================================================================
    private void assignTeam(User user, String teamName, Map<String, Team> teamMap) {
        if (blank(teamName)) return;
        Team team = teamMap.get(teamName.toLowerCase());
        if (team != null) userTeamRepository.save(UserTeam.builder().user(user).team(team).active(true).build());
    }

    private void assignTeamIfNew(User user, String teamName, Map<String, Team> teamMap) {
        if (blank(teamName)) return;
        Team team = teamMap.get(teamName.toLowerCase());
        if (team != null && !userTeamRepository.existsActiveByUserIdAndTeamId(user.getId(), team.getId())) {
            userTeamRepository.save(UserTeam.builder().user(user).team(team).active(true).build());
        }
    }

    private String resolveTeamNameForExport(User u) {
        return userTeamRepository.findFirstActiveByUserId(u.getId())
                .map(ut -> ut.getTeam().getTeamName()).orElse("");
    }

    // ================================================================
    // Bulk pre-load helpers
    // ================================================================
    private Map<String, User> bulkLoadUsers(Set<String> usernames) {
        if (usernames.isEmpty()) return Map.of();
        return userRepository.findAllByUsernameIn(usernames).stream()
                .collect(Collectors.toMap(User::getUsername, u -> u));
    }

    private Map<String, Role> bulkLoadRoles(Map<String, ParsedRow> insertMap, Map<String, ParsedRow> updateMap) {
        Set<String> codes = new HashSet<>();
        insertMap.values().forEach(pr -> { if (!blank(pr.roleCode)) codes.add(pr.roleCode); });
        updateMap.values().forEach(pr -> { if (!blank(pr.roleCode)) codes.add(pr.roleCode); });
        if (codes.isEmpty()) return Map.of();
        return roleRepository.findAllByRoleCodeIn(codes).stream()
                .collect(Collectors.toMap(Role::getRoleCode, r -> r));
    }

    private Map<String, User> bulkLoadManagers(Map<String, ParsedRow> insertMap, Map<String, ParsedRow> updateMap) {
        Set<String> names = new HashSet<>();
        insertMap.values().forEach(pr -> { if (!blank(pr.managerUsername)) names.add(pr.managerUsername); });
        updateMap.values().forEach(pr -> { if (!blank(pr.managerUsername)) names.add(pr.managerUsername); });
        if (names.isEmpty()) return Map.of();
        return userRepository.findAllByUsernameIn(names).stream()
                .collect(Collectors.toMap(User::getUsername, u -> u));
    }

    private Map<String, Team> bulkLoadTeams(Map<String, ParsedRow> insertMap, Map<String, ParsedRow> updateMap) {
        Set<String> names = new HashSet<>();
        insertMap.values().forEach(pr -> { if (!blank(pr.teamName)) names.add(pr.teamName); });
        updateMap.values().forEach(pr -> { if (!blank(pr.teamName)) names.add(pr.teamName); });
        if (names.isEmpty()) return Map.of();
        return teamRepository.findAllByTeamNameIn(names).stream()
                .collect(Collectors.toMap(t -> t.getTeamName().toLowerCase(), t -> t));
    }

    // ================================================================
    // Sheet parser
    // ================================================================
    private List<ParsedRow> parseUserMasterSheet(Workbook wb) {
        org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheet("01_USER_MASTER");
        if (sheet == null) sheet = wb.getSheetAt(0); // fallback
        List<ParsedRow> out = new ArrayList<>();
        if (sheet == null) return out;
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return out;
        Map<String, Integer> hdr = buildHeaderMap(headerRow);
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowBlank(row)) continue;
            ParsedRow pr = new ParsedRow();
            pr.rowNumber       = r + 1;
            pr.username        = normUsername(getByHeader(row, hdr, H_USERNAME));
            pr.fullName        = normOpt(getByHeader(row, hdr, H_FULL_NAME));
            pr.email           = normOpt(getByHeader(row, hdr, H_EMAIL));
            pr.roleCode        = norm(getByHeader(row, hdr, H_ROLE_CODE));
            pr.managerUsername = normOpt(getByHeader(row, hdr, H_MANAGER_USERNAME));
            pr.teamName        = normOpt(getByHeader(row, hdr, H_TEAM_NAME));
            pr.isActiveStr     = normFlag(getByHeader(row, hdr, H_IS_ACTIVE));
            pr.mustChangePwdStr = normFlag(getByHeader(row, hdr, H_MUST_CHANGE_PWD));
            pr.remarks         = normOpt(getByHeader(row, hdr, H_REMARKS));
            out.add(pr);
        }
        return out;
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String raw = cell.getStringCellValue();
            if (raw == null || raw.isBlank()) continue;
            map.putIfAbsent(raw.trim().toLowerCase().replaceAll("\\s+", "_"), c);
        }
        return map;
    }

    private String getByHeader(Row row, Map<String, Integer> hdr, String key) {
        Integer col = hdr.get(key);
        if (col == null) return "";
        return getString(row, col);
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isRowBlank(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!blank(getString(row, i))) return false;
        }
        return true;
    }

    // ================================================================
    // Template sheet builders
    // ================================================================
    private void createInstructionsSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("00_INSTRUCTIONS");
        String[][] rows = {
            {"User Master Import Template - Instructions"},
            {""},
            {"Sheet 01_USER_MASTER: Fill user data here. Do NOT modify column headers."},
            {"Sheet 02_ALLOWED_VALUES: Reference only. Do NOT modify."},
            {""},
            {"Column Rules:"},
            {"username         - REQUIRED. Treated as text (string). Must be unique."},
            {"full_name        - REQUIRED."},
            {"email            - REQUIRED. Must be a valid email address. Must be unique."},
            {"role_code        - REQUIRED. Must be one of: ADMIN, LEAD_MANAGER, TECHNICIAN"},
            {"manager_username - Optional. Must match an existing username if provided."},
            {"team_name        - Optional. Must match an existing team name if provided."},
            {"is_active        - REQUIRED. 1 = Active, 0 = Inactive"},
            {"must_change_password - REQUIRED. 1 = Yes, 0 = No. Recommended: 1 for new users."},
            {"remarks          - Optional. Notes only, not stored in DB."},
            {""},
            {"Import Behavior (SMART_UPSERT):"},
            {"  INSERT  - New username: user will be created."},
            {"  UPDATE  - Existing username with changes: user will be updated."},
            {"  SKIP    - Existing username, same data: no action taken."},
            {"  ERROR   - Invalid data or conflict: row will NOT be imported."},
            {""},
            {"Note: Blank optional fields will NOT overwrite existing DB values on UPDATE."},
        };
        for (int i = 0; i < rows.length; i++) {
            Row r = sheet.createRow(i);
            if (rows[i].length > 0) r.createCell(0).setCellValue(rows[i][0]);
        }
        sheet.autoSizeColumn(0);
    }

    private void createUserMasterSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("01_USER_MASTER");
        XSSFCellStyle headerStyle = buildHeaderStyle(wb);
        // Text cell style for username to prevent numeric conversion
        XSSFCellStyle textStyle = wb.createCellStyle();
        textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));

        String[] cols = {H_USERNAME, H_FULL_NAME, H_EMAIL, H_ROLE_CODE,
                H_MANAGER_USERNAME, H_TEAM_NAME, H_IS_ACTIVE, H_MUST_CHANGE_PWD, "remarks"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }
        // Set username column (col 0) to text format for all data rows
        for (int r = 1; r <= 1000; r++) {
            Row dataRow = sheet.getRow(r);
            if (dataRow == null) dataRow = sheet.createRow(r);
            Cell usernameCell = dataRow.createCell(0);
            usernameCell.setCellStyle(textStyle);
        }
        // Sample row
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("jdoe");
        sample.createCell(1).setCellValue("John Doe");
        sample.createCell(2).setCellValue("jdoe@example.com");
        sample.createCell(3).setCellValue("TECHNICIAN");
        sample.createCell(4).setCellValue("");
        sample.createCell(5).setCellValue("");
        sample.createCell(6).setCellValue("1");
        sample.createCell(7).setCellValue("1");
        sample.createCell(8).setCellValue("");
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private void createAllowedValuesSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("02_ALLOWED_VALUES");
        XSSFCellStyle hs = buildHeaderStyle(wb);
        String[][] data = {
            {"Column", "Allowed Values", "Notes"},
            {"role_code", "ADMIN", "Platform administrator"},
            {"role_code", "LEAD_MANAGER", "Lead/manager role"},
            {"role_code", "TECHNICIAN", "Technician role"},
            {"is_active", "1", "Active user"},
            {"is_active", "0", "Inactive user"},
            {"must_change_password", "1", "User must change password on next login"},
            {"must_change_password", "0", "No forced password change"},
        };
        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data[i][j]);
                if (i == 0) cell.setCellStyle(hs);
            }
        }
        for (int i = 0; i < 3; i++) sheet.autoSizeColumn(i);
    }

    // ================================================================
    // History save
    // ================================================================
    private ImportExportHistory saveHistory(String fileName, String actorUsername,
            int total, int inserted, int updated, int skipped, int failed,
            String importMode, String statusCode, String errorSummary) {
        User user = userRepository.findByUsername(actorUsername).orElseThrow();
        var status = lookupValueRepository.findByTypeCodeAndValueCode("IMPORT_EXPORT_STATUS", statusCode).orElse(null);
        ImportExportHistory h = ImportExportHistory.builder()
                .publicId(UUID.randomUUID().toString())
                .operationType("IMPORT")
                .templateType(TEMPLATE_TYPE)
                .fileName(fileName)
                .totalRows(total)
                .successRows(inserted + updated + skipped)
                .failedRows(failed)
                .insertedRows(inserted)
                .updatedRows(updated)
                .skippedRows(skipped)
                .importMode(importMode != null ? importMode.toUpperCase() : "STRICT")
                .statusLookup(status)
                .errorSummary(errorSummary)
                .initiatedBy(user)
                .completedAt(LocalDateTime.now())
                .build();
        return historyRepository.save(h);
    }

    // ================================================================
    // Utilities
    // ================================================================
    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void setCellText(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "");
    }

    /** Normalize a code: trim + uppercase. Returns null if blank. */
    private String norm(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }

    /** Normalize optional text: trim only. Returns null if blank. */
    private String normOpt(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Normalize username: trim only, preserve case, preserve leading zeros.
     * This is intentional — usernames that look like numbers must remain strings.
     */
    private String normUsername(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Normalize a flag value (is_active / must_change_password): trim, accept 1/0/true/false. */
    private String normFlag(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes")) return "1";
        if (v.equals("0") || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no")) return "0";
        return v; // let validation catch invalid values
    }

    private boolean parseFlag(String s) {
        return "1".equals(s);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private String nullSafe(String s) { return s == null ? "" : s; }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    // ================================================================
    // ParsedRow
    // ================================================================
    static class ParsedRow {
        int    rowNumber;
        String username;
        String fullName;
        String email;
        String roleCode;
        String managerUsername;
        String teamName;
        String isActiveStr;
        String mustChangePwdStr;
        String remarks;

        boolean sameData(ParsedRow other) {
            return Objects.equals(username,        other.username)
                && Objects.equals(fullName,        other.fullName)
                && Objects.equals(email,           other.email)
                && Objects.equals(roleCode,        other.roleCode)
                && Objects.equals(managerUsername, other.managerUsername)
                && Objects.equals(teamName,        other.teamName)
                && Objects.equals(isActiveStr,     other.isActiveStr)
                && Objects.equals(mustChangePwdStr,other.mustChangePwdStr);
        }
    }
}
