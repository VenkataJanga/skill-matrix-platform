package com.skillmatrix.applications.service;

import com.skillmatrix.applications.dto.ApplicationImportResult;
import com.skillmatrix.applications.dto.ApplicationImportRowPreview;
import com.skillmatrix.applications.dto.ApplicationValidationResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.ApplicationPortfolio;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.repository.ApplicationPortfolioRepository;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
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
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application Master bulk Excel import/export (M4 stabilization enhancement —
 * Problem Statement Part A). SMART_UPSERT pattern modeled directly on
 * {@code SkillImportService}. Reuses {@code import_export_history} with
 * templateType="APPLICATION_MASTER" — no new history subsystem introduced.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationImportService {

    static final String SHEET_APPLICATIONS = "01_APPLICATIONS";
    private static final String SHEET_INSTRUCTIONS = "00_INSTRUCTIONS";
    private static final String TEMPLATE_TYPE = "APPLICATION_MASTER";

    static final String ACTION_INSERT = "INSERT";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_SKIP   = "SKIP";
    static final String ACTION_ERROR  = "ERROR";

    private static final String H_ACTION       = "action";
    private static final String H_TYPE_CODE    = "application_type_code";
    private static final String H_BUNDLE_CODE  = "bundle_code";
    private static final String H_PORTFOLIO    = "portfolio_code";
    private static final String H_APP_CODE     = "application_code";
    private static final String H_APP_NAME     = "application_name";
    private static final String H_LIFECYCLE    = "lifecycle_status";
    private static final String H_IS_ACTIVE    = "is_active";
    private static final String H_DESCRIPTION  = "description";
    private static final String H_REMARKS      = "remarks";

    private final ApplicationRepository applicationRepository;
    private final ApplicationPortfolioRepository portfolioRepository;
    private final ApplicationTypeRepository appTypeRepository;
    private final BundleRepository bundleRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ImportExportHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ================================================================
    // Template
    // ================================================================

    public ResponseEntity<byte[]> generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createInstructionsSheet(wb);
            createApplicationsSheet(wb);
            createAllowedValuesSheet(wb);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "Application_Master_Import_Template.xlsx");
        }
    }

    // ================================================================
    // Export
    // ================================================================

    public ResponseEntity<byte[]> exportApplications(String search, String typeCode, String bundleCode,
            String portfolioCode, String lifecycleStatusCode, String status) throws IOException {
        Boolean activeFilter = parseActiveFilter(status);
        List<Application> apps = applicationRepository.searchApplicationsForExport(
                blank(search) ? null : search.trim(),
                blank(typeCode) ? null : typeCode.trim(),
                blank(bundleCode) ? null : bundleCode.trim(),
                blank(portfolioCode) ? null : portfolioCode.trim(),
                blank(lifecycleStatusCode) ? null : lifecycleStatusCode.trim(),
                activeFilter);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("applications_export");
            XSSFCellStyle hs = buildHeaderStyle(wb);
            String[] cols = {"application_code","application_name","application_type_code","bundle_code",
                    "portfolio_code","lifecycle_status","is_active","description","created_at","updated_at"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) { Cell c = hRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs); }

            int ri = 1;
            for (Application a : apps) {
                Row row = sheet.createRow(ri++);
                var p = a.getPortfolio();
                row.createCell(0).setCellValue(a.getApplicationCode());
                row.createCell(1).setCellValue(a.getApplicationName());
                row.createCell(2).setCellValue(p.getApplicationType().getTypeCode());
                row.createCell(3).setCellValue(p.getBundle().getBundleCode());
                row.createCell(4).setCellValue(p.getPortfolioCode());
                row.createCell(5).setCellValue(a.getLifecycleStatusLookup() != null ? a.getLifecycleStatusLookup().getValueCode() : "");
                row.createCell(6).setCellValue(a.isActive() ? 1 : 0);
                row.createCell(7).setCellValue(a.getDescription() != null ? a.getDescription() : "");
                row.createCell(8).setCellValue(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
                row.createCell(9).setCellValue(a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : "");
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return buildExcelResponse(out.toByteArray(), "applications_export.xlsx");
        }
    }

    // ================================================================
    // Validate-only
    // ================================================================

    public ApplicationValidationResult validateOnly(MultipartFile file) throws IOException {
        log.info("Application import validation started: file={}", file.getOriginalFilename());
        List<ApplicationImportRowPreview> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            rows = buildPreviews(parseSheet(wb));
        }
        ApplicationValidationResult result = ApplicationValidationResult.of(rows);
        log.info("Application import validation complete: total={} insert={} update={} skip={} error={}",
                result.totalRows(), result.insertRows(), result.updateRows(), result.skipRows(), result.errorRows());
        return result;
    }

    // ================================================================
    // Confirm import (SMART_UPSERT, STRICT — aborts on any error row)
    // ================================================================

    @Transactional
    public ApplicationImportResult confirmImport(MultipartFile file, String actorUsername) throws IOException {
        log.info("Application import confirm started: file={} by={}", file.getOriginalFilename(), actorUsername);

        List<ParsedRow> parsed;
        List<ApplicationImportRowPreview> previews;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            parsed = parseSheet(wb);
            previews = buildPreviews(parsed);
        }

        ApplicationValidationResult validation = ApplicationValidationResult.of(previews);
        if (!validation.valid()) {
            log.warn("Application import aborted — error rows: file={} errorRows={} by={}",
                    file.getOriginalFilename(), validation.errorRows(), actorUsername);
            saveHistory(file.getOriginalFilename(), actorUsername,
                    validation.totalRows(), 0, 0, 0, validation.errorRows(), "FAILED",
                    "Import blocked: " + validation.errorRows() + " error row(s) found.");
            return new ApplicationImportResult(false, validation.totalRows(), 0, 0, 0,
                    validation.errorRows(), null,
                    "Import failed: " + validation.errorRows() + " error row(s) must be fixed.");
        }

        Map<String, ParsedRow> insertMap = new LinkedHashMap<>();
        Map<String, UpdateCandidate> updateMap = new LinkedHashMap<>();
        int skipped = 0;

        for (int i = 0; i < previews.size(); i++) {
            ApplicationImportRowPreview pv = previews.get(i);
            ParsedRow pr = parsed.get(i);
            switch (pv.action()) {
                case ACTION_INSERT -> insertMap.put(pr.appCode, pr);
                case ACTION_UPDATE -> updateMap.put(pr.appCode, new UpdateCandidate(pr));
                case ACTION_SKIP   -> skipped++;
                default -> { /* ERROR excluded */ }
            }
        }

        int inserted = 0, updated = 0;
        String failingAppCode = null;

        try {
            for (ParsedRow pr : insertMap.values()) {
                failingAppCode = pr.appCode;
                ApplicationPortfolio portfolio = resolvePortfolio(pr.typeCode, pr.bundleCode, pr.portfolioCode);
                Application app = Application.builder()
                        .publicId(UUID.randomUUID().toString())
                        .portfolio(portfolio)
                        .applicationCode(pr.appCode)
                        .applicationName(pr.appName)
                        .description(pr.description)
                        .remarks(pr.remarks)
                        .lifecycleStatusLookup(resolveLifecycle(pr.lifecycleStatus))
                        .active(parseActive(pr.isActiveStr))
                        .createdBy(actorUsername)
                        .updatedBy(actorUsername)
                        .build();
                applicationRepository.saveAndFlush(app);
                inserted++;
            }

            for (UpdateCandidate uc : updateMap.values()) {
                ParsedRow pr = uc.row;
                failingAppCode = pr.appCode;
                ApplicationPortfolio portfolio = resolvePortfolio(pr.typeCode, pr.bundleCode, pr.portfolioCode);
                Application existing = applicationRepository
                        .findByPortfolio_IdAndApplicationCodeIgnoreCase(portfolio.getId(), pr.appCode)
                        .orElse(null);
                if (existing == null) continue;
                if (!blank(pr.appName))         existing.setApplicationName(pr.appName);
                if (!blank(pr.description))     existing.setDescription(pr.description);
                if (!blank(pr.remarks))         existing.setRemarks(pr.remarks);
                if (!blank(pr.lifecycleStatus)) existing.setLifecycleStatusLookup(resolveLifecycle(pr.lifecycleStatus));
                if (!blank(pr.isActiveStr))     existing.setActive(parseActive(pr.isActiveStr));
                existing.setUpdatedBy(actorUsername);
                applicationRepository.saveAndFlush(existing);
                updated++;
            }
        } catch (DataAccessException ex) {
            // Unexpected persistence failure (e.g. constraint violation) after validation
            // passed. Per STRICT semantics: mark the current transaction rollback-only so
            // NO application rows from this import survive (no partial corruption of
            // Application Master), persist a FAILED history record in an independent
            // transaction (so it survives the rollback), and return a controlled,
            // non-leaking error response instead of letting the generic 500 handler fire.
            log.error("Application import persistence failure on application_code={}: {}",
                    failingAppCode, ex.getMessage(), ex);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (NoTransactionException noTx) {
                // Defensive: should never happen in production (this method is always
                // invoked within the @Transactional confirmImport proxy), but avoids
                // masking the original persistence failure in non-proxied contexts
                // (e.g. direct unit test instantiation without a Spring context).
                log.warn("No active transaction aspect found while marking rollback-only; "
                        + "this should only happen outside a Spring-managed transaction.");
            }
            String failureMessage = "Application import failed while saving application '"
                    + failingAppCode + "'. No changes were applied. Please verify the data and try again.";
            saveHistoryNewTransaction(file.getOriginalFilename(), actorUsername,
                    validation.totalRows(), 0, 0, 0, validation.totalRows(), "FAILED", failureMessage);
            return new ApplicationImportResult(false, validation.totalRows(), 0, 0, 0,
                    validation.totalRows(), null, failureMessage);
        }

        ImportExportHistory history = saveHistory(file.getOriginalFilename(), actorUsername,
                validation.totalRows(), inserted, updated, skipped, 0, "SUCCESS", null);

        auditService.log("APPLICATION_IMPORT", "APPLICATION", null, actorUsername, null, null, null,
                "{\"inserted\":" + inserted + ",\"updated\":" + updated + ",\"skipped\":" + skipped
                        + ",\"file\":\"" + file.getOriginalFilename() + "\"}");

        log.info("Application import completed: file={} inserted={} updated={} skipped={} by={}",
                file.getOriginalFilename(), inserted, updated, skipped, actorUsername);

        return new ApplicationImportResult(true, validation.totalRows(), inserted, updated, skipped, 0,
                history.getPublicId(),
                "Import successful: " + inserted + " inserted, " + updated + " updated, " + skipped + " skipped.");
    }

    // ================================================================
    // SMART_UPSERT core
    // ================================================================

    private List<ApplicationImportRowPreview> buildPreviews(List<ParsedRow> parsed) {
        // Phase 1: within-file duplicate detection by applicationCode
        Map<String, List<ParsedRow>> byCode = new LinkedHashMap<>();
        for (ParsedRow pr : parsed) {
            if (!blank(pr.appCode)) byCode.computeIfAbsent(pr.appCode, k -> new ArrayList<>()).add(pr);
        }
        Set<ParsedRow> duplicateConflict = new HashSet<>();
        Set<ParsedRow> duplicateSkip = new HashSet<>();
        for (var e : byCode.entrySet()) {
            List<ParsedRow> group = e.getValue();
            if (group.size() > 1) {
                ParsedRow first = group.get(0);
                boolean allSame = group.stream().skip(1).allMatch(r -> r.sameData(first));
                if (allSame) { for (int i = 1; i < group.size(); i++) duplicateSkip.add(group.get(i)); }
                else duplicateConflict.addAll(group);
            }
        }

        List<ApplicationImportRowPreview> previews = new ArrayList<>();
        for (ParsedRow pr : parsed) {
            previews.add(buildPreview(pr, duplicateConflict, duplicateSkip));
        }
        return previews;
    }

    private ApplicationImportRowPreview buildPreview(ParsedRow pr, Set<ParsedRow> conflictSet, Set<ParsedRow> skipSet) {
        if (conflictSet.contains(pr)) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, "Conflicting duplicate application_code in file: " + pr.appCode,
                    List.of("Duplicate application_code with conflicting values"));
        }
        if (skipSet.contains(pr)) {
            return ApplicationImportRowPreview.of(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, ACTION_SKIP, "Duplicate row — same data as earlier row in file");
        }

        List<String> errors = new ArrayList<>();
        if (blank(pr.appCode))     errors.add("application_code is required");
        if (blank(pr.appName))    errors.add("application_name is required");
        if (blank(pr.typeCode))   errors.add("application_type_code is required");
        if (blank(pr.bundleCode)) errors.add("bundle_code is required");
        if (blank(pr.portfolioCode)) errors.add("portfolio_code is required");
        if (!blank(pr.isActiveStr) && !pr.isActiveStr.equals("0") && !pr.isActiveStr.equals("1"))
            errors.add("is_active must be 1 or 0");

        if (!errors.isEmpty()) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, "Missing/invalid required field(s)", errors);
        }

        // Hierarchy validation
        var typeOpt = appTypeRepository.findByTypeCode(pr.typeCode);
        if (typeOpt.isEmpty()) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, "Unknown application_type_code: " + pr.typeCode,
                    List.of("Unknown application_type_code: " + pr.typeCode));
        }
        var bundleOpt = bundleRepository.findByBundleCode(pr.bundleCode);
        if (bundleOpt.isEmpty()) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, "Unknown bundle_code: " + pr.bundleCode,
                    List.of("Unknown bundle_code: " + pr.bundleCode));
        }
        var portfolioOpt = portfolioRepository.findByPortfolioCodeAndTypeCodeAndBundleCode(
                pr.portfolioCode, pr.typeCode, pr.bundleCode);
        if (portfolioOpt.isEmpty()) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode,
                    "Portfolio '" + pr.portfolioCode + "' does not belong to Type '" + pr.typeCode + "' + Bundle '" + pr.bundleCode + "'",
                    List.of("Hierarchy mismatch: portfolio/type/bundle do not form a valid combination"));
        }
        if (!blank(pr.lifecycleStatus)
                && lookupValueRepository.findByTypeCodeAndValueCode("APPLICATION_LIFECYCLE_STATUS", pr.lifecycleStatus).isEmpty()) {
            return ApplicationImportRowPreview.error(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, "Unknown lifecycle_status: " + pr.lifecycleStatus,
                    List.of("Unknown lifecycle_status: " + pr.lifecycleStatus));
        }

        Long portfolioId = portfolioOpt.get().getId();
        Application existing = applicationRepository
                .findByPortfolio_IdAndApplicationCodeIgnoreCase(portfolioId, pr.appCode).orElse(null);

        if (existing == null) {
            return ApplicationImportRowPreview.of(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, ACTION_INSERT, "New application — will be inserted");
        }

        List<String> changes = detectChanges(pr, existing);
        if (changes.isEmpty()) {
            return ApplicationImportRowPreview.of(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                    pr.portfolioCode, ACTION_SKIP, "Same data already exists — no change needed");
        }
        return ApplicationImportRowPreview.of(pr.rowNumber, pr.appCode, pr.appName, pr.typeCode, pr.bundleCode,
                pr.portfolioCode, ACTION_UPDATE, "Will update: " + String.join(", ", changes));
    }

    private List<String> detectChanges(ParsedRow pr, Application existing) {
        List<String> changes = new ArrayList<>();
        if (!blank(pr.appName) && !pr.appName.equalsIgnoreCase(existing.getApplicationName())) changes.add("application_name");
        if (!blank(pr.description) && !pr.description.equals(nullSafe(existing.getDescription()))) changes.add("description");
        if (!blank(pr.remarks) && !pr.remarks.equals(nullSafe(existing.getRemarks()))) changes.add("remarks");
        if (!blank(pr.lifecycleStatus)) {
            String dbCode = existing.getLifecycleStatusLookup() != null ? existing.getLifecycleStatusLookup().getValueCode() : null;
            if (!pr.lifecycleStatus.equalsIgnoreCase(nullSafe(dbCode))) changes.add("lifecycle_status");
        }
        if (!blank(pr.isActiveStr) && parseActive(pr.isActiveStr) != existing.isActive()) changes.add("is_active");
        return changes;
    }

    // ================================================================
    // Resolution helpers
    // ================================================================

    private ApplicationPortfolio resolvePortfolio(String typeCode, String bundleCode, String portfolioCode) {
        return portfolioRepository.findByPortfolioCodeAndTypeCodeAndBundleCode(portfolioCode, typeCode, bundleCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Portfolio resolution failed post-validation for " + portfolioCode + "/" + typeCode + "/" + bundleCode));
    }

    private com.skillmatrix.domain.entity.LookupValue resolveLifecycle(String code) {
        String c = blank(code) ? "ACTIVE" : code.trim().toUpperCase();
        return lookupValueRepository.findByTypeCodeAndValueCode("APPLICATION_LIFECYCLE_STATUS", c).orElse(null);
    }

    // ================================================================
    // Sheet parser (header-driven, mirrors SkillImportService pattern)
    // ================================================================

    private List<ParsedRow> parseSheet(Workbook wb) {
        List<ParsedRow> out = new ArrayList<>();
        Sheet sheet = wb.getSheet(SHEET_APPLICATIONS);
        if (sheet == null) return out;
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return out;
        Map<String, Integer> hdr = buildHeaderMap(headerRow);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowBlank(row)) continue;

            ParsedRow pr = new ParsedRow();
            pr.rowNumber      = r + 1;
            pr.typeCode       = norm(getByHeader(row, hdr, H_TYPE_CODE));
            pr.bundleCode     = norm(getByHeader(row, hdr, H_BUNDLE_CODE));
            pr.portfolioCode  = norm(getByHeader(row, hdr, H_PORTFOLIO));
            pr.appCode        = norm(getByHeader(row, hdr, H_APP_CODE));
            pr.appName        = normOpt(getByHeader(row, hdr, H_APP_NAME));
            pr.lifecycleStatus = norm(getByHeader(row, hdr, H_LIFECYCLE));
            pr.isActiveStr    = normFlag(getByHeader(row, hdr, H_IS_ACTIVE));
            pr.description    = normOpt(getByHeader(row, hdr, H_DESCRIPTION));
            pr.remarks        = normOpt(getByHeader(row, hdr, H_REMARKS));
            pr.actionHint     = normOpt(getByHeader(row, hdr, H_ACTION));
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
        Sheet sheet = wb.createSheet(SHEET_INSTRUCTIONS);
        String[] lines = {
            "Application Master Import Template - Instructions",
            "",
            "Sheet 01_APPLICATIONS: Fill application data here. Do NOT modify column headers.",
            "Sheet 02_ALLOWED_VALUES: Reference only. Do NOT modify.",
            "",
            "Column Rules:",
            "application_type_code - REQUIRED. Must match an existing Application Type code (e.g. WEB, HOST).",
            "bundle_code           - REQUIRED. Must match an existing Bundle code (e.g. B06).",
            "portfolio_code        - REQUIRED. Must belong to the Type+Bundle combination above (e.g. B06-WEB).",
            "application_code      - REQUIRED. Unique within the resolved portfolio.",
            "application_name      - REQUIRED.",
            "lifecycle_status      - Optional. ACTIVE / INACTIVE / DEPRECATED / FUTURE. Defaults to ACTIVE.",
            "is_active             - Optional. 1 = Active, 0 = Inactive. Defaults to 1.",
            "description           - Optional.",
            "remarks               - Optional.",
            "",
            "Import Behavior (SMART_UPSERT):",
            "  INSERT  - New application_code within the resolved portfolio: application will be created.",
            "  UPDATE  - Existing application with changed valid values: application will be updated.",
            "  SKIP    - Existing application, same data: no action taken.",
            "  ERROR   - Invalid hierarchy/data: row will NOT be imported (blocks the whole import — STRICT mode).",
            "",
            "Note: Blank optional fields will NOT overwrite existing DB values on UPDATE.",
        };
        for (int i = 0; i < lines.length; i++) sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        sheet.autoSizeColumn(0);
    }

    private void createApplicationsSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_APPLICATIONS);
        XSSFCellStyle headerStyle = buildHeaderStyle(wb);
        String[] cols = {H_ACTION, H_TYPE_CODE, H_BUNDLE_CODE, H_PORTFOLIO, H_APP_CODE, H_APP_NAME,
                H_LIFECYCLE, H_IS_ACTIVE, H_DESCRIPTION, H_REMARKS};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = hRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("");
        sample.createCell(1).setCellValue("WEB");
        sample.createCell(2).setCellValue("B06");
        sample.createCell(3).setCellValue("B06-WEB");
        sample.createCell(4).setCellValue("IFA");
        sample.createCell(5).setCellValue("IFA Application");
        sample.createCell(6).setCellValue("ACTIVE");
        sample.createCell(7).setCellValue("1");
        sample.createCell(8).setCellValue("");
        sample.createCell(9).setCellValue("");
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private void createAllowedValuesSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("02_ALLOWED_VALUES");
        XSSFCellStyle hs = buildHeaderStyle(wb);
        String[][] data = {
            {"Column", "Allowed Values", "Notes"},
            {"lifecycle_status", "ACTIVE", "Normal operational application"},
            {"lifecycle_status", "INACTIVE", "No longer in active use"},
            {"lifecycle_status", "DEPRECATED", "Scheduled for retirement"},
            {"lifecycle_status", "FUTURE", "Planned, not yet operational"},
            {"is_active", "1", "Active application"},
            {"is_active", "0", "Inactive application"},
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
                // Application Master import only ever runs in STRICT mode (no PARTIAL
                // toggle exposed on this API) — the import_export_history.import_mode
                // column is NOT NULL, so it must always be populated explicitly.
                .importMode("STRICT")
                .statusLookup(status)
                .errorSummary(errorSummary)
                .initiatedBy(user)
                .completedAt(LocalDateTime.now())
                .build();
        return historyRepository.save(h);
    }

    /**
     * Persists a FAILED import history record in an INDEPENDENT transaction
     * (REQUIRES_NEW) so it survives even when the caller's main import
     * transaction is rolled back due to an unexpected persistence exception.
     * Mirrors the pattern already used by {@code AuditService.log(...)}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportExportHistory saveHistoryNewTransaction(String fileName, String actorUsername,
            int total, int inserted, int updated, int skipped, int failed,
            String statusCode, String errorSummary) {
        return saveHistory(fileName, actorUsername, total, inserted, updated, skipped, failed, statusCode, errorSummary);
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

    private String normFlag(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes")) return "1";
        if (v.equals("0") || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no")) return "0";
        return v;
    }

    private boolean parseActive(String s) {
        if (blank(s)) return true;
        String v = s.trim().toLowerCase();
        return switch (v) {
            case "0", "false", "inactive", "no" -> false;
            default -> true;
        };
    }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private String nullSafe(String s) { return s == null ? "" : s.trim(); }

    // ================================================================
    // ParsedRow / UpdateCandidate — internal normalized row representation
    // ================================================================

    static class ParsedRow {
        int rowNumber;
        String typeCode;
        String bundleCode;
        String portfolioCode;
        String appCode;
        String appName;
        String lifecycleStatus;
        String isActiveStr;
        String description;
        String remarks;
        String actionHint;

        boolean sameData(ParsedRow other) {
            return Objects.equals(typeCode, other.typeCode)
                && Objects.equals(bundleCode, other.bundleCode)
                && Objects.equals(portfolioCode, other.portfolioCode)
                && Objects.equals(appCode, other.appCode)
                && Objects.equals(appName, other.appName)
                && Objects.equals(lifecycleStatus, other.lifecycleStatus)
                && Objects.equals(isActiveStr, other.isActiveStr)
                && Objects.equals(description, other.description)
                && Objects.equals(remarks, other.remarks);
        }
    }

    private static class UpdateCandidate {
        final ParsedRow row;
        UpdateCandidate(ParsedRow row) { this.row = row; }
    }
}
