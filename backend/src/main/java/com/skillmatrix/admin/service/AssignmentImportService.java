package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.AssignmentImportResult;
import com.skillmatrix.admin.dto.AssignmentImportRowPreview;
import com.skillmatrix.admin.dto.AssignmentValidationResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.entity.LookupValue;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserApplicationMapping;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Technician Assignment bulk Excel import/export (M4 stabilization enhancement —
 * Problem Statement Part C). SMART_UPSERT pattern modeled directly on
 * {@code SkillImportService}/{@code ApplicationImportService}. Reuses
 * {@code import_export_history} with templateType="TECHNICIAN_ASSIGNMENT" —
 * no new history subsystem introduced.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssignmentImportService {

    static final String SHEET_ASSIGNMENTS = "01_TECHNICIAN_ASSIGNMENTS";
    private static final String SHEET_INSTRUCTIONS = "00_INSTRUCTIONS";
    private static final String TEMPLATE_TYPE = "TECHNICIAN_ASSIGNMENT";
    private static final String LOOKUP_TYPE_ROLE_ON_APP = "ROLE_ON_APPLICATION";

    static final String ACTION_INSERT = "INSERT";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_SKIP   = "SKIP";
    static final String ACTION_ERROR  = "ERROR";

    private static final String H_ACTION        = "action";
    private static final String H_USERNAME      = "username";
    private static final String H_TYPE_CODE     = "application_type_code";
    private static final String H_BUNDLE_CODE   = "bundle_code";
    private static final String H_APP_CODE      = "application_code";
    private static final String H_ALLOCATION    = "allocation_pct";
    private static final String H_EFFECTIVE_FROM = "effective_from";
    private static final String H_EFFECTIVE_TO   = "effective_to";
    private static final String H_ROLE_ON_APP   = "role_on_app_code";
    private static final String H_IS_PRIMARY    = "is_primary";
    private static final String H_REMARKS       = "remarks";

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTypeRepository appTypeRepository;
    private final BundleRepository bundleRepository;
    private final UserApplicationMappingRepository mappingRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ImportExportHistoryRepository historyRepository;
    private final AuditService auditService;

    // ================================================================
    // Template
    // ================================================================

    public ResponseEntity<byte[]> generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createInstructionsSheet(wb);
            createAssignmentsSheet(wb);
            createAllowedValuesSheet(wb);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "Technician_Assignment_Import_Template.xlsx");
        }
    }

    // ================================================================
    // Export
    // ================================================================

    public ResponseEntity<byte[]> exportAssignments(String techPublicId, String typeCode, String bundleCode,
            String appPublicId, String status) throws IOException {
        Boolean activeFilter = parseActiveFilter(status);
        var page = mappingRepository.searchAssignments(
                null, blank(appPublicId) ? null : appPublicId.trim(),
                blank(techPublicId) ? null : techPublicId.trim(),
                blank(typeCode) ? null : typeCode.trim(),
                blank(bundleCode) ? null : bundleCode.trim(),
                activeFilter,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        List<UserApplicationMapping> mappings = page.getContent();

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("assignments_export");
            XSSFCellStyle hs = buildHeaderStyle(wb);
            String[] cols = {"username","technician_name","application_type_code","bundle_code","application_code",
                    "allocation_pct","effective_from","effective_to","role_on_app_code","is_primary","is_active"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs); }

            int ri = 1;
            for (UserApplicationMapping m : mappings) {
                Row row = sheet.createRow(ri++);
                var app = m.getApplication();
                var portfolio = app.getPortfolio();
                var role = m.getRoleOnAppLookup();
                row.createCell(0).setCellValue(m.getUser().getUsername());
                row.createCell(1).setCellValue(m.getUser().getFullName());
                row.createCell(2).setCellValue(portfolio.getApplicationType().getTypeCode());
                row.createCell(3).setCellValue(portfolio.getBundle().getBundleCode());
                row.createCell(4).setCellValue(app.getApplicationCode());
                row.createCell(5).setCellValue(m.getAllocationPercentage() != null ? m.getAllocationPercentage().doubleValue() : 0);
                row.createCell(6).setCellValue(m.getEffectiveFrom() != null ? m.getEffectiveFrom().toString() : "");
                row.createCell(7).setCellValue(m.getEffectiveTo() != null ? m.getEffectiveTo().toString() : "");
                row.createCell(8).setCellValue(role != null ? role.getValueCode() : "");
                row.createCell(9).setCellValue(role != null && "PRIMARY".equals(role.getValueCode()) ? 1 : 0);
                row.createCell(10).setCellValue(m.isActive() ? 1 : 0);
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "assignments_export.xlsx");
        }
    }

    // ================================================================
    // Validate-only
    // ================================================================

    public AssignmentValidationResult validateOnly(MultipartFile file) throws IOException {
        log.info("Assignment import validation started: file={}", file.getOriginalFilename());
        List<AssignmentImportRowPreview> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            assertCorrectTemplate(wb);
            rows = buildPreviews(parseSheet(wb));
        }
        AssignmentValidationResult result = AssignmentValidationResult.of(rows);
        log.info("Assignment import validation complete: total={} insert={} update={} skip={} error={}",
                result.totalRows(), result.insertRows(), result.updateRows(), result.skipRows(), result.errorRows());
        return result;
    }

    // ================================================================
    // Confirm import (SMART_UPSERT, PARTIAL processing — valid rows import,
    // ERROR rows never processed; result reflects PARTIAL_SUCCESS when any
    // error rows were present alongside successfully processed rows).
    // ================================================================

    @Transactional
    public AssignmentImportResult confirmImport(MultipartFile file, String actorUsername) throws IOException {
        log.info("Assignment import confirm started: file={} by={}", file.getOriginalFilename(), actorUsername);

        List<ParsedRow> parsed;
        List<AssignmentImportRowPreview> previews;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            assertCorrectTemplate(wb);
            parsed = parseSheet(wb);
            previews = buildPreviews(parsed);
        }

        AssignmentValidationResult validation = AssignmentValidationResult.of(previews);
        int totalValidRows = validation.insertRows() + validation.updateRows();
        if (totalValidRows == 0) {
            log.warn("Assignment import aborted — no valid rows: file={} errorRows={} by={}",
                    file.getOriginalFilename(), validation.errorRows(), actorUsername);
            saveHistory(file.getOriginalFilename(), actorUsername,
                    validation.totalRows(), 0, 0, validation.skipRows(), validation.errorRows(), "FAILED", null);
            return new AssignmentImportResult(false, validation.totalRows(), 0, 0,
                    validation.skipRows(), validation.errorRows(), null,
                    "Import failed: no valid rows to process. " + validation.errorRows() + " error row(s) found.",
                    "FAILED");
        }

        int inserted = 0, updated = 0, skipped = 0, persistenceFailures = 0;
        for (int i = 0; i < previews.size(); i++) {
            AssignmentImportRowPreview pv = previews.get(i);
            ParsedRow pr = parsed.get(i);
            try {
                if (ACTION_INSERT.equals(pv.action())) {
                    if (processInsertSafely(pr, actorUsername)) inserted++; else persistenceFailures++;
                } else if (ACTION_UPDATE.equals(pv.action())) {
                    if (processUpdateSafely(pr, actorUsername)) updated++; else persistenceFailures++;
                } else if (ACTION_SKIP.equals(pv.action())) {
                    skipped++;
                }
                // ACTION_ERROR rows are never processed
            } catch (DataAccessException ex) {
                log.error("Assignment import persistence failure for username={} application={}: {}",
                        pr.username, pr.appCode, ex.getMessage(), ex);
                persistenceFailures++;
            }
        }

        int errorRows = validation.errorRows() + persistenceFailures;
        boolean partialSuccess = errorRows > 0;
        String statusCode = partialSuccess ? "PARTIAL_SUCCESS" : "SUCCESS";

        ImportExportHistory history = saveHistory(file.getOriginalFilename(), actorUsername,
                validation.totalRows(), inserted, updated, skipped, errorRows, statusCode, null);

        auditService.log("TECHNICIAN_ASSIGNMENT_IMPORT", "ASSIGNMENT", null, actorUsername, null, null, null,
                "{\"inserted\":" + inserted + ",\"updated\":" + updated + ",\"skipped\":" + skipped
                        + ",\"rejected\":" + errorRows + ",\"file\":\"" + file.getOriginalFilename() + "\"}");

        log.info("Assignment import completed: file={} inserted={} updated={} skipped={} rejected={} by={}",
                file.getOriginalFilename(), inserted, updated, skipped, errorRows, actorUsername);

        String message = partialSuccess
                ? "Import completed with partial success. " + inserted + " inserted, " + updated
                        + " updated, " + skipped + " skipped, " + errorRows + " rejected."
                : "Import successful: " + inserted + " inserted, " + updated + " updated, " + skipped + " skipped.";

        return new AssignmentImportResult(true, validation.totalRows(), inserted, updated, skipped, errorRows,
                history.getPublicId(), message, statusCode);
    }

    // ================================================================
    // Row processors
    // ================================================================

    /**
     * Inserts a single assignment row in its OWN independent transaction
     * (REQUIRES_NEW), so that a persistence failure on this row cannot roll
     * back other already-committed rows in the batch — required for the
     * established PARTIAL-processing guarantee (see confirmImport()).
     * Re-checks for an existing active assignment at confirm time (not just
     * relying on the preview computed during Validate), since DB state may
     * have changed between Validate and Confirm; if one now exists, this row
     * is safely downgraded to an UPDATE/SKIP instead of attempting a raw
     * INSERT that could violate the uq_user_application_active unique index.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processInsertSafely(ParsedRow pr, String actorUsername) {
        User user = userRepository.findByUsername(pr.username).orElse(null);
        Application app = resolveApplication(pr);
        if (user == null || app == null) return false;

        UserApplicationMapping existing = mappingRepository
                .findActiveByUserPublicIdAndAppPublicId(user.getPublicId(), app.getPublicId())
                .orElse(null);
        if (existing != null) {
            // DB state changed since Validate (another assignment was created
            // concurrently) — apply as an UPDATE instead of a conflicting INSERT.
            applyUpdate(existing, pr, actorUsername);
            mappingRepository.saveAndFlush(existing);
            return true;
        }

        LookupValue role = resolveRole(pr.roleOnAppCode);
        UserApplicationMapping mapping = UserApplicationMapping.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .application(app)
                .roleOnAppLookup(role)
                .allocationPercentage(pr.allocationPct != null ? pr.allocationPct : BigDecimal.valueOf(100))
                .effectiveFrom(pr.effectiveFrom != null ? pr.effectiveFrom : LocalDate.now())
                .effectiveTo(pr.effectiveTo)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();
        mappingRepository.saveAndFlush(mapping);
        return true;
    }

    /** Independent-transaction counterpart to {@link #processInsertSafely}. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processUpdateSafely(ParsedRow pr, String actorUsername) {
        User user = userRepository.findByUsername(pr.username).orElse(null);
        Application app = resolveApplication(pr);
        if (user == null || app == null) return false;

        UserApplicationMapping existing = mappingRepository
                .findActiveByUserPublicIdAndAppPublicId(user.getPublicId(), app.getPublicId())
                .orElse(null);
        if (existing == null) return false;

        applyUpdate(existing, pr, actorUsername);
        mappingRepository.saveAndFlush(existing);
        return true;
    }

    private void applyUpdate(UserApplicationMapping existing, ParsedRow pr, String actorUsername) {
        if (pr.allocationPct != null) existing.setAllocationPercentage(pr.allocationPct);
        if (pr.effectiveFrom != null) existing.setEffectiveFrom(pr.effectiveFrom);
        if (pr.effectiveTo != null) existing.setEffectiveTo(pr.effectiveTo);
        if (!blank(pr.roleOnAppCode)) existing.setRoleOnAppLookup(resolveRole(pr.roleOnAppCode));
        existing.setUpdatedBy(actorUsername);
    }

    private Application resolveApplication(ParsedRow pr) {
        // Prefer direct application_code lookup; type/bundle are used only for
        // cross-validation during preview (hierarchy consistency check).
        return applicationRepository.findByApplicationCode(pr.appCode).orElse(null);
    }

    private LookupValue resolveRole(String roleCode) {
        if (blank(roleCode)) return null;
        return lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE_ROLE_ON_APP, roleCode.trim().toUpperCase()).orElse(null);
    }

    // ================================================================
    // SMART_UPSERT core
    // ================================================================

    private List<AssignmentImportRowPreview> buildPreviews(List<ParsedRow> parsed) {
        // Phase 1: within-file duplicate detection by username+applicationCode
        Map<String, List<ParsedRow>> byKey = new LinkedHashMap<>();
        for (ParsedRow pr : parsed) {
            if (!blank(pr.username) && !blank(pr.appCode)) {
                byKey.computeIfAbsent(pr.username + "::" + pr.appCode, k -> new ArrayList<>()).add(pr);
            }
        }
        Set<ParsedRow> duplicateConflict = new HashSet<>();
        Set<ParsedRow> duplicateSkip = new HashSet<>();
        for (var e : byKey.entrySet()) {
            List<ParsedRow> group = e.getValue();
            if (group.size() > 1) {
                ParsedRow first = group.get(0);
                boolean allSame = group.stream().skip(1).allMatch(r -> r.sameData(first));
                if (allSame) { for (int i = 1; i < group.size(); i++) duplicateSkip.add(group.get(i)); }
                else duplicateConflict.addAll(group);
            }
        }

        List<AssignmentImportRowPreview> previews = new ArrayList<>();
        for (ParsedRow pr : parsed) {
            previews.add(buildPreview(pr, duplicateConflict, duplicateSkip));
        }
        return previews;
    }

    private AssignmentImportRowPreview buildPreview(ParsedRow pr, Set<ParsedRow> conflictSet, Set<ParsedRow> skipSet) {
        if (conflictSet.contains(pr)) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Conflicting duplicate username+application in file",
                    List.of("Duplicate username/application combination with conflicting values"));
        }
        if (skipSet.contains(pr)) {
            return AssignmentImportRowPreview.of(pr.rowNumber, pr.username, pr.appCode,
                    ACTION_SKIP, "Duplicate row — same data as earlier row in file");
        }

        List<String> errors = new ArrayList<>();
        if (blank(pr.username)) errors.add("username is required");
        if (blank(pr.typeCode)) errors.add("application_type_code is required");
        if (blank(pr.bundleCode)) errors.add("bundle_code is required");
        if (blank(pr.appCode)) errors.add("application_code is required");
        if (pr.allocationPct == null) errors.add("allocation_pct is required");
        else if (pr.allocationPct.compareTo(BigDecimal.ZERO) <= 0 || pr.allocationPct.compareTo(BigDecimal.valueOf(100)) > 0)
            errors.add("allocation_pct must be > 0 and <= 100");
        if (pr.effectiveFrom == null) errors.add("effective_from is required (YYYY-MM-DD)");
        if (pr.effectiveTo != null && pr.effectiveFrom != null && pr.effectiveTo.isBefore(pr.effectiveFrom))
            errors.add("effective_to must be on or after effective_from");
        if (!blank(pr.isPrimaryStr) && !pr.isPrimaryStr.equals("0") && !pr.isPrimaryStr.equals("1"))
            errors.add("is_primary must be 1 or 0");

        if (!errors.isEmpty()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Missing/invalid required field(s)", errors);
        }

        User user = userRepository.findByUsername(pr.username).orElse(null);
        if (user == null) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Unknown username: " + pr.username, List.of("Unknown username: " + pr.username));
        }
        if (!user.isActive()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Technician is inactive: " + pr.username, List.of("Technician is inactive: " + pr.username));
        }

        var typeOpt = appTypeRepository.findByTypeCode(pr.typeCode);
        if (typeOpt.isEmpty()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Unknown application_type_code: " + pr.typeCode, List.of("Unknown application_type_code: " + pr.typeCode));
        }
        var bundleOpt = bundleRepository.findByBundleCode(pr.bundleCode);
        if (bundleOpt.isEmpty()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Unknown bundle_code: " + pr.bundleCode, List.of("Unknown bundle_code: " + pr.bundleCode));
        }

        Application app = applicationRepository.findByApplicationCode(pr.appCode).orElse(null);
        if (app == null) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Unknown application_code: " + pr.appCode, List.of("Unknown application_code: " + pr.appCode));
        }
        if (!app.isActive()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Application is inactive: " + pr.appCode, List.of("Application is inactive: " + pr.appCode));
        }
        var portfolio = app.getPortfolio();
        boolean hierarchyMatches = portfolio.getApplicationType().getTypeCode().equalsIgnoreCase(pr.typeCode)
                && portfolio.getBundle().getBundleCode().equalsIgnoreCase(pr.bundleCode);
        if (!hierarchyMatches) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Application '" + pr.appCode + "' does not belong to Type '" + pr.typeCode + "' + Bundle '" + pr.bundleCode + "'",
                    List.of("Hierarchy mismatch: application does not belong to the specified type/bundle"));
        }

        if (!blank(pr.roleOnAppCode)
                && lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE_ROLE_ON_APP, pr.roleOnAppCode).isEmpty()) {
            return AssignmentImportRowPreview.error(pr.rowNumber, pr.username, pr.appCode,
                    "Unknown role_on_app_code: " + pr.roleOnAppCode, List.of("Unknown role_on_app_code: " + pr.roleOnAppCode));
        }

        UserApplicationMapping existing = mappingRepository
                .findActiveByUserPublicIdAndAppPublicId(user.getPublicId(), app.getPublicId()).orElse(null);

        if (existing == null) {
            return AssignmentImportRowPreview.of(pr.rowNumber, pr.username, pr.appCode,
                    ACTION_INSERT, "New assignment — will be inserted");
        }

        List<String> changes = detectChanges(pr, existing);
        if (changes.isEmpty()) {
            return AssignmentImportRowPreview.of(pr.rowNumber, pr.username, pr.appCode,
                    ACTION_SKIP, "Same data already exists — no change needed");
        }
        return AssignmentImportRowPreview.of(pr.rowNumber, pr.username, pr.appCode,
                ACTION_UPDATE, "Will update: " + String.join(", ", changes));
    }

    private List<String> detectChanges(ParsedRow pr, UserApplicationMapping existing) {
        List<String> changes = new ArrayList<>();
        if (pr.allocationPct != null && pr.allocationPct.compareTo(existing.getAllocationPercentage()) != 0)
            changes.add("allocation_pct");
        if (pr.effectiveFrom != null && !pr.effectiveFrom.equals(existing.getEffectiveFrom()))
            changes.add("effective_from");
        if (pr.effectiveTo != null && !pr.effectiveTo.equals(existing.getEffectiveTo()))
            changes.add("effective_to");
        if (!blank(pr.roleOnAppCode)) {
            String dbRole = existing.getRoleOnAppLookup() != null ? existing.getRoleOnAppLookup().getValueCode() : null;
            if (!pr.roleOnAppCode.equalsIgnoreCase(nullSafe(dbRole))) changes.add("role_on_app_code");
        }
        return changes;
    }

    // ================================================================
    // Template identity validation
    // ================================================================

    /**
     * Verifies the uploaded workbook is actually a Technician Assignment
     * import template (correct sheet + required headers) BEFORE any row
     * processing occurs. Prevents an unrelated workbook (e.g. the
     * Application Master template, which uses sheet "01_APPLICATIONS" with
     * entirely different headers) from silently producing a "Total: 0" or
     * reaching Confirm Import with a generic 500 error.
     */
    private void assertCorrectTemplate(Workbook wb) {
        Sheet sheet = wb.getSheet(SHEET_ASSIGNMENTS);
        if (sheet == null) {
            throw new BusinessException(
                    "Incorrect import template. Please use the Technician Assignment template downloaded from this screen.");
        }
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BusinessException(
                    "Incorrect import template. Please use the Technician Assignment template downloaded from this screen.");
        }
        Map<String, Integer> hdr = buildHeaderMap(headerRow);
        List<String> required = List.of(H_USERNAME, H_TYPE_CODE, H_BUNDLE_CODE, H_APP_CODE,
                H_ALLOCATION, H_EFFECTIVE_FROM);
        for (String requiredHeader : required) {
            if (!hdr.containsKey(requiredHeader)) {
                throw new BusinessException(
                        "Incorrect import template. Please use the Technician Assignment template downloaded from this screen.");
            }
        }
    }

    // ================================================================
    // Sheet parser (header-driven)
    // ================================================================

    private List<ParsedRow> parseSheet(Workbook wb) {
        List<ParsedRow> out = new ArrayList<>();
        Sheet sheet = wb.getSheet(SHEET_ASSIGNMENTS);
        if (sheet == null) return out;
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return out;
        Map<String, Integer> hdr = buildHeaderMap(headerRow);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowBlank(row)) continue;

            ParsedRow pr = new ParsedRow();
            pr.rowNumber     = r + 1;
            pr.username      = normUsername(getByHeader(row, hdr, H_USERNAME));
            pr.typeCode      = norm(getByHeader(row, hdr, H_TYPE_CODE));
            pr.bundleCode    = norm(getByHeader(row, hdr, H_BUNDLE_CODE));
            pr.appCode       = norm(getByHeader(row, hdr, H_APP_CODE));
            pr.allocationPct = parseDecimal(getByHeader(row, hdr, H_ALLOCATION));
            pr.effectiveFrom = parseDate(getByHeader(row, hdr, H_EFFECTIVE_FROM));
            pr.effectiveTo   = parseDate(getByHeader(row, hdr, H_EFFECTIVE_TO));
            pr.roleOnAppCode = norm(getByHeader(row, hdr, H_ROLE_ON_APP));
            pr.isPrimaryStr  = normFlag(getByHeader(row, hdr, H_IS_PRIMARY));
            pr.remarks       = normOpt(getByHeader(row, hdr, H_REMARKS));
            pr.actionHint    = normOpt(getByHeader(row, hdr, H_ACTION));
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

    private BigDecimal parseDecimal(String s) {
        if (blank(s)) return null;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException ex) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (blank(s)) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception ex) { return null; }
    }

    // ================================================================
    // Template sheet builders
    // ================================================================

    private void createInstructionsSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_INSTRUCTIONS);
        String[] lines = {
            "Technician Assignment Import Template - Instructions",
            "",
            "Sheet 01_TECHNICIAN_ASSIGNMENTS: Fill assignment data here. Do NOT modify column headers.",
            "Sheet 02_ALLOWED_VALUES: Reference only. Do NOT modify.",
            "",
            "Column Rules:",
            "username              - REQUIRED. Must be an existing, active technician username.",
            "application_type_code - REQUIRED. Must match the application's Type (e.g. WEB).",
            "bundle_code           - REQUIRED. Must match the application's Bundle (e.g. B06).",
            "application_code      - REQUIRED. Must be an existing, active application.",
            "allocation_pct        - REQUIRED. Numeric, > 0 and <= 100.",
            "effective_from        - REQUIRED. Format YYYY-MM-DD.",
            "effective_to          - Optional. Format YYYY-MM-DD. Must be on/after effective_from.",
            "role_on_app_code      - Optional. PRIMARY / BACKUP / SME / TRAINEE.",
            "is_primary            - Optional, informational only. 1 = Yes, 0 = No (derived from role_on_app_code=PRIMARY).",
            "remarks               - Optional.",
            "",
            "Import Behavior (SMART_UPSERT, PARTIAL processing):",
            "  INSERT  - No existing active assignment for username+application: will be created.",
            "  UPDATE  - Existing active assignment with changed valid fields: will be updated.",
            "  SKIP    - Existing active assignment, same data: no action taken.",
            "  ERROR   - Invalid data: row will NOT be imported (other valid rows still import — PARTIAL mode).",
            "",
            "Note: Duplicate active assignments for the same username+application are never created.",
        };
        for (int i = 0; i < lines.length; i++) sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        sheet.autoSizeColumn(0);
    }

    private void createAssignmentsSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_ASSIGNMENTS);
        XSSFCellStyle headerStyle = buildHeaderStyle(wb);
        String[] cols = {H_ACTION, H_USERNAME, H_TYPE_CODE, H_BUNDLE_CODE, H_APP_CODE, H_ALLOCATION,
                H_EFFECTIVE_FROM, H_EFFECTIVE_TO, H_ROLE_ON_APP, H_IS_PRIMARY, H_REMARKS};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("");
        sample.createCell(1).setCellValue("tech_john");
        sample.createCell(2).setCellValue("WEB");
        sample.createCell(3).setCellValue("B06");
        sample.createCell(4).setCellValue("ATLAS");
        sample.createCell(5).setCellValue(100);
        sample.createCell(6).setCellValue("2026-10-26");
        sample.createCell(7).setCellValue("");
        sample.createCell(8).setCellValue("PRIMARY");
        sample.createCell(9).setCellValue("1");
        sample.createCell(10).setCellValue("");
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private void createAllowedValuesSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("02_ALLOWED_VALUES");
        XSSFCellStyle hs = buildHeaderStyle(wb);
        String[][] data = {
            {"Column", "Allowed Values", "Notes"},
            {"role_on_app_code", "PRIMARY", "Primary technician for this application"},
            {"role_on_app_code", "BACKUP", "Backup technician"},
            {"role_on_app_code", "SME", "Subject matter expert"},
            {"role_on_app_code", "TRAINEE", "Trainee"},
            {"is_primary", "1", "Informational — derived from role_on_app_code=PRIMARY"},
            {"is_primary", "0", "Informational — not primary"},
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
    // Import history
    // ================================================================

    private ImportExportHistory saveHistory(String fileName, String actorUsername,
            int total, int inserted, int updated, int skipped, int failed,
            String statusCode, String errorSummary) {
        var user = userRepository.findByUsername(actorUsername).orElseThrow();
        var status = lookupValueRepository.findByTypeCodeAndValueCode("IMPORT_EXPORT_STATUS", statusCode).orElse(null);
        int success = inserted + updated + skipped;
        ImportExportHistory h = ImportExportHistory.builder()
                .publicId(UUID.randomUUID().toString())
                .operationType("IMPORT")
                .templateType(TEMPLATE_TYPE)
                .fileName(fileName)
                .totalRows(total)
                .successRows(success)
                .failedRows(failed)
                .insertedRows(inserted)
                .updatedRows(updated)
                .skippedRows(skipped)
                // Technician Assignment import uses PARTIAL processing — error rows
                // never block valid rows from being imported (see confirmImport()).
                // The import_export_history.import_mode column is NOT NULL, so it
                // must always be populated explicitly.
                .importMode("PARTIAL")
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

    private String norm(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim().toUpperCase();
    }

    private String normOpt(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private String normUsername(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normFlag(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes")) return "1";
        if (v.equals("0") || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no")) return "0";
        return v;
    }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private String nullSafe(String s) { return s == null ? "" : s.trim(); }

    // ================================================================
    // ParsedRow — internal normalized row representation
    // ================================================================

    static class ParsedRow {
        int rowNumber;
        String username;
        String typeCode;
        String bundleCode;
        String appCode;
        BigDecimal allocationPct;
        LocalDate effectiveFrom;
        LocalDate effectiveTo;
        String roleOnAppCode;
        String isPrimaryStr;
        String remarks;
        String actionHint;

        boolean sameData(ParsedRow other) {
            return Objects.equals(username, other.username)
                && Objects.equals(typeCode, other.typeCode)
                && Objects.equals(bundleCode, other.bundleCode)
                && Objects.equals(appCode, other.appCode)
                && Objects.equals(allocationPct, other.allocationPct)
                && Objects.equals(effectiveFrom, other.effectiveFrom)
                && Objects.equals(effectiveTo, other.effectiveTo)
                && Objects.equals(roleOnAppCode, other.roleOnAppCode);
        }
    }
}
