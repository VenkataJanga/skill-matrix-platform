package com.skillmatrix.skills.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.SkillCategoryRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.skills.dto.ImportHistoryDto;
import com.skillmatrix.skills.dto.SkillImportResult;
import com.skillmatrix.skills.dto.SkillImportRowPreview;
import com.skillmatrix.skills.dto.SkillValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SkillImportService {

    // ---------------------------------------------------------------
    // Sheet names
    // ---------------------------------------------------------------
    static final String SHEET_COMMON       = "01_COMMON_TECH_SKILLS";
    static final String SHEET_APP_SPECIFIC = "02_APP_SPECIFIC_SKILLS";
    static final String SHEET_VW_TOOLS     = "03_VW_SPECIFIC_TOOLS";
    /** This sheet is documentation only — always skipped. */
    private static final String SHEET_INSTRUCTIONS = "00_INSTRUCTIONS";

    /** Shown when an uploaded workbook does not contain any recognized Skill Import sheet. */
    static final String WRONG_TEMPLATE_MESSAGE =
            "Incorrect import template. Please use the Skill Import template downloaded from Skill Management.";

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------
    static final String ACTION_INSERT = "INSERT";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_SKIP   = "SKIP";
    static final String ACTION_ERROR  = "ERROR";

    // ---------------------------------------------------------------
    // Canonical header keys (after normalization)
    // Header normalization: trim + lowercase + collapse spaces to '_'
    // ---------------------------------------------------------------
    private static final String H_SKILL_CODE   = "skill_code";
    private static final String H_SKILL_NAME   = "skill_name";
    private static final String H_CATEGORY     = "category_code";
    private static final String H_DESCRIPTION  = "description";
    private static final String H_SCOPE        = "scope";          // also accepts scope_code
    private static final String H_SCOPE_CODE   = "scope_code";
    private static final String H_SKILL_TYPE   = "skill_type";     // also accepts skill_type_code
    private static final String H_SKILL_TYPE_C = "skill_type_code";
    private static final String H_IS_ACTIVE    = "is_active";
    private static final String H_APP_TYPE     = "application_type_code";
    private static final String H_BUNDLE       = "bundle_code";
    private static final String H_APP_CODE     = "application_code";
    private static final String H_ACTION       = "action";         // optional column — never a skill_code

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ApplicationTypeRepository appTypeRepository;
    private final BundleRepository bundleRepository;
    private final ApplicationRepository applicationRepository;
    private final ImportExportHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final SkillMasterService skillMasterService;
    private final AuditService auditService;

    // ================================================================
    // Template download
    // ================================================================

    public ResponseEntity<byte[]> generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createCommonSheet(wb);
            createAppSpecificSheet(wb);
            createVwToolsSheet(wb);
            wb.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "skill_import_template.xlsx");
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        }
    }

    // ================================================================
    // Export (read-only) — reuses SkillSpecification/SkillRepository,
    // the same filtering logic as the Skill Management paged list.
    // ================================================================

    public ResponseEntity<byte[]> exportSkills(
            String search, String categoryCode, String scopeCode,
            String appTypeCode, String bundleCode, String appPublicId,
            String status) throws IOException {

        Boolean activeFilter = parseActiveFilter(status);
        String cleanSearch   = blank(search)       ? null : search.trim();
        String cleanCategory = blank(categoryCode) ? null : categoryCode.trim();
        String cleanScope    = blank(scopeCode)    ? null : scopeCode.trim();
        String cleanAppType  = blank(appTypeCode)  ? null : appTypeCode.trim();
        String cleanBundle   = blank(bundleCode)   ? null : bundleCode.trim();
        String cleanApp      = blank(appPublicId)  ? null : appPublicId.trim();

        var spec = SkillSpecification.build(
                cleanSearch, cleanCategory, cleanScope,
                cleanAppType, cleanBundle, cleanApp, activeFilter);

        List<Skill> skills = skillRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "skillCode"));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("skills_export");
            XSSFCellStyle headerStyle = buildExportHeaderStyle(wb);
            String[] cols = {"Skill Code","Skill Name","Description","Category","Scope","Skill Type",
                    "Application Type","Bundle","Application","Status","Created At","Updated At"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int ri = 1;
            for (Skill s : skills) {
                Row row = sheet.createRow(ri++);
                row.createCell(0).setCellValue(s.getSkillCode());
                row.createCell(1).setCellValue(s.getSkillName());
                row.createCell(2).setCellValue(s.getDescription() != null ? s.getDescription() : "");
                row.createCell(3).setCellValue(s.getSkillCategory() != null ? s.getSkillCategory().getCategoryName() : "");
                row.createCell(4).setCellValue(s.getSkillScopeLookup() != null ? s.getSkillScopeLookup().getValueCode() : "");
                row.createCell(5).setCellValue(s.getSkillTypeLookup() != null ? s.getSkillTypeLookup().getValueCode() : "");
                row.createCell(6).setCellValue(s.getApplicationType() != null ? s.getApplicationType().getTypeCode() : "");
                row.createCell(7).setCellValue(s.getBundle() != null ? s.getBundle().getBundleCode() : "");
                row.createCell(8).setCellValue(s.getApplication() != null ? s.getApplication().getApplicationCode() : "");
                row.createCell(9).setCellValue(s.isActive() ? "Active" : "Inactive");
                row.createCell(10).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().format(dtf) : "");
                row.createCell(11).setCellValue(s.getUpdatedAt() != null ? s.getUpdatedAt().format(dtf) : "");
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            String filename = "Skill_Master_Export_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        }
    }

    private XSSFCellStyle buildExportHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    // ================================================================
    // Validate-only (no DB write)
    // ================================================================

    public SkillValidationResult validateOnly(MultipartFile file) throws IOException {
        log.info("Skill import validation started: file={}", file.getOriginalFilename());
        List<SkillImportRowPreview> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            rows = analyzeRows(wb);
        }
        SkillValidationResult result = SkillValidationResult.of(rows);
        log.info("Skill import validation complete: file={} total={} insert={} update={} skip={} error={}",
                file.getOriginalFilename(),
                result.totalRows(), result.insertRows(), result.updateRows(),
                result.skipRows(), result.errorRows());
        return result;
    }

    // ================================================================
    // Confirm import (SMART_UPSERT)
    // ================================================================

    @Transactional
    public SkillImportResult confirmImport(MultipartFile file, String actorUsername) throws IOException {
        log.info("Skill import confirm started: file={} by={}", file.getOriginalFilename(), actorUsername);

        // Parse + analyze (same logic as validateOnly)
        List<SkillImportRowPreview> previews;
        List<ParsedRow> allParsed;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            if (!hasAnyRecognizedSheet(wb)) {
                log.warn("Skill import confirm aborted — unrecognized template: file={} by={}",
                        file.getOriginalFilename(), actorUsername);
                saveHistory(file.getOriginalFilename(), actorUsername,
                        0, 0, 0, 0, 0, "FAILED", WRONG_TEMPLATE_MESSAGE);
                return new SkillImportResult(false, 0, 0, 0, 0, 0, null, WRONG_TEMPLATE_MESSAGE);
            }
            allParsed = parseAllSheets(wb);
            previews  = buildPreviews(allParsed);
        }

        SkillValidationResult validation = SkillValidationResult.of(previews);
        if (!validation.valid()) {
            log.warn("Skill import aborted — error rows: file={} errorRows={} by={}",
                    file.getOriginalFilename(), validation.errorRows(), actorUsername);
            saveHistory(file.getOriginalFilename(), actorUsername,
                    validation.totalRows(), 0, 0, 0, validation.errorRows(), "FAILED",
                    "Import blocked: " + validation.errorRows() + " error row(s) found.");
            return new SkillImportResult(false, validation.totalRows(), 0, 0, 0,
                    validation.errorRows(), null,
                    "Import failed: " + validation.errorRows() + " error row(s) must be fixed.");
        }

        // Separate INSERT and UPDATE rows
        Map<String, ParsedRow> insertMap = new LinkedHashMap<>();
        Map<String, ParsedRow> updateMap = new LinkedHashMap<>();
        Set<String> skipCodes = new LinkedHashSet<>();

        for (int i = 0; i < previews.size(); i++) {
            SkillImportRowPreview pv = previews.get(i);
            ParsedRow pr = allParsed.get(i);
            switch (pv.action()) {
                case ACTION_INSERT -> insertMap.put(pr.skillCode, pr);
                case ACTION_UPDATE -> updateMap.put(pr.skillCode, pr);
                case ACTION_SKIP   -> { if (pr.skillCode != null) skipCodes.add(pr.skillCode); }
                // ERROR should not reach here (validation.valid() == true)
            }
        }

        // Load DB skills for UPDATE
        Map<String, Skill> dbSkills = skillRepository
                .findAllBySkillCodeIn(updateMap.keySet()).stream()
                .collect(Collectors.toMap(Skill::getSkillCode, s -> s));

        List<Skill> toSave = new ArrayList<>();
        int inserted = 0, updated = 0, skipped = validation.skipRows();

        // Build INSERT entities
        for (ParsedRow pr : insertMap.values()) {
            toSave.add(buildInsertFromRow(pr, actorUsername));
            inserted++;
        }

        // Apply UPDATE entities
        for (Map.Entry<String, ParsedRow> e : updateMap.entrySet()) {
            Skill existing = dbSkills.get(e.getKey());
            if (existing != null) {
                applyUpdateFromRow(existing, e.getValue(), actorUsername);
                toSave.add(existing);
                updated++;
            }
        }

        skillRepository.saveAll(toSave);

        ImportExportHistory history = saveHistory(
                file.getOriginalFilename(), actorUsername,
                validation.totalRows(), inserted, updated, skipped, 0, "SUCCESS", null);

        auditService.log("SKILL_IMPORT_CONFIRM", "SKILL", null, actorUsername, null, null, null,
                "{\"inserted\":" + inserted + ",\"updated\":" + updated +
                        ",\"skipped\":" + skipped + ",\"file\":\"" + file.getOriginalFilename() + "\"}");

        log.info("Skill import completed: file={} inserted={} updated={} skipped={} by={}",
                file.getOriginalFilename(), inserted, updated, skipped, actorUsername);

        return new SkillImportResult(true, validation.totalRows(),
                inserted, updated, skipped, 0,
                history.getPublicId(),
                "Import successful: " + inserted + " inserted, " + updated + " updated, " + skipped + " skipped.");
    }

    // ================================================================
    // Import history
    // ================================================================

    public PagedResponse<ImportHistoryDto> getHistory(
            int page, int size, String templateType, String statusCode,
            String uploadedBy, String fromDate, String toDate) {

        LocalDateTime from = fromDate != null && !fromDate.isBlank()
                ? LocalDateTime.parse(fromDate + "T00:00:00") : null;
        LocalDateTime to = toDate != null && !toDate.isBlank()
                ? LocalDateTime.parse(toDate + "T23:59:59") : null;

        String cleanTemplate = blank(templateType) ? null : templateType.trim();
        String cleanStatus   = blank(statusCode)   ? null : statusCode.trim();
        String cleanUser     = blank(uploadedBy)   ? null : uploadedBy.trim();

        Page<ImportExportHistory> result = historyRepository.searchHistory(
                cleanTemplate, cleanStatus, cleanUser, from, to, PageRequest.of(page, size));
        return PagedResponse.of(result.getContent().stream().map(ImportHistoryDto::from).toList(), result);
    }

    // ================================================================
    // SMART_UPSERT core
    // ================================================================

    /** Top-level entry used by validateOnly. */
    List<SkillImportRowPreview> analyzeRows(Workbook wb) {
        if (!hasAnyRecognizedSheet(wb)) {
            return List.of(SkillImportRowPreview.error(
                    "N/A", 0, null, null,
                    WRONG_TEMPLATE_MESSAGE, List.of(WRONG_TEMPLATE_MESSAGE)));
        }
        List<ParsedRow> parsed = parseAllSheets(wb);
        return buildPreviews(parsed);
    }

    /** Returns true if the workbook contains at least one of the expected Skill Import sheets. */
    private boolean hasAnyRecognizedSheet(Workbook wb) {
        return wb.getSheet(SHEET_COMMON) != null
                || wb.getSheet(SHEET_APP_SPECIFIC) != null
                || wb.getSheet(SHEET_VW_TOOLS) != null;
    }

    /** Parse all supported sheets into ParsedRow objects. */
    private List<ParsedRow> parseAllSheets(Workbook wb) {
        List<ParsedRow> out = new ArrayList<>();
        parseSheet(wb, SHEET_COMMON, out);
        parseSheet(wb, SHEET_APP_SPECIFIC, out);
        parseSheet(wb, SHEET_VW_TOOLS, out);
        return out;
    }

    /**
     * Builds row-level previews from already-parsed rows.
     * Separated from parseAllSheets so confirmImport can reuse the same ParsedRow list.
     */
    private List<SkillImportRowPreview> buildPreviews(List<ParsedRow> parsed) {
        // --- Phase 1: detect within-file duplicates ---
        Map<String, List<ParsedRow>> byCode = new LinkedHashMap<>();
        for (ParsedRow pr : parsed) {
            if (!blank(pr.skillCode)) {
                byCode.computeIfAbsent(pr.skillCode, k -> new ArrayList<>()).add(pr);
            }
        }
        Set<ParsedRow> duplicateConflict = new HashSet<>();
        Set<ParsedRow> duplicateSkip    = new HashSet<>();
        for (Map.Entry<String, List<ParsedRow>> e : byCode.entrySet()) {
            List<ParsedRow> group = e.getValue();
            if (group.size() > 1) {
                ParsedRow first = group.get(0);
                boolean allSame = group.stream().skip(1).allMatch(r -> r.sameData(first));
                if (allSame) {
                    for (int i = 1; i < group.size(); i++) duplicateSkip.add(group.get(i));
                } else {
                    duplicateConflict.addAll(group);
                }
            }
        }

        // --- Phase 2: bulk-load DB ---
        Set<String> codesToCheck = parsed.stream()
                .filter(pr -> !blank(pr.skillCode) && !duplicateConflict.contains(pr))
                .map(pr -> pr.skillCode)
                .collect(Collectors.toSet());

        Map<String, Skill> dbMap = skillRepository
                .findAllBySkillCodeIn(codesToCheck).stream()
                .collect(Collectors.toMap(Skill::getSkillCode, s -> s));

        // --- Phase 3: build preview per row ---
        List<SkillImportRowPreview> previews = new ArrayList<>();
        for (ParsedRow pr : parsed) {
            previews.add(buildPreview(pr, dbMap, duplicateConflict, duplicateSkip));
        }
        return previews;
    }

    private SkillImportRowPreview buildPreview(
            ParsedRow pr,
            Map<String, Skill> dbMap,
            Set<ParsedRow> duplicateConflict,
            Set<ParsedRow> duplicateSkip) {

        if (duplicateConflict.contains(pr)) {
            return SkillImportRowPreview.error(pr.sheetName, pr.rowNumber,
                    pr.skillCode, pr.skillName,
                    "Conflicting duplicate skill_code in file: " + pr.skillCode,
                    List.of("Duplicate skill_code with conflicting values"));
        }
        if (duplicateSkip.contains(pr)) {
            return SkillImportRowPreview.of(pr.sheetName, pr.rowNumber,
                    pr.skillCode, pr.skillName,
                    ACTION_SKIP, "Duplicate row — same data as earlier row in file");
        }
        if (blank(pr.skillCode)) {
            return SkillImportRowPreview.error(pr.sheetName, pr.rowNumber,
                    pr.skillCode, pr.skillName,
                    "skill_code is required", List.of("skill_code is required"));
        }

        Skill existing = dbMap.get(pr.skillCode);

        if (existing == null) {
            List<String> fieldErrors = validateRequiredForInsert(pr);
            if (!fieldErrors.isEmpty()) {
                return SkillImportRowPreview.error(pr.sheetName, pr.rowNumber,
                        pr.skillCode, pr.skillName, "Missing required field(s)", fieldErrors);
            }
            if (!blank(pr.categoryCode) && !categoryRepository.existsByCategoryCode(pr.categoryCode)) {
                return SkillImportRowPreview.error(pr.sheetName, pr.rowNumber,
                        pr.skillCode, pr.skillName,
                        "Unknown category_code: " + pr.categoryCode,
                        List.of("Unknown category_code: " + pr.categoryCode));
            }
            return SkillImportRowPreview.of(pr.sheetName, pr.rowNumber,
                    pr.skillCode, pr.skillName, ACTION_INSERT, "New skill — will be inserted");
        }

        List<String> changes = detectChanges(pr, existing);
        if (changes.isEmpty()) {
            return SkillImportRowPreview.of(pr.sheetName, pr.rowNumber,
                    pr.skillCode, pr.skillName, ACTION_SKIP, "Same data already exists — no change needed");
        }
        return SkillImportRowPreview.of(pr.sheetName, pr.rowNumber,
                pr.skillCode, pr.skillName, ACTION_UPDATE, "Will update: " + String.join(", ", changes));
    }

    // ================================================================
    // Header-driven sheet parser
    // ================================================================

    /**
     * Parses one sheet by HEADER NAME (not fixed column index).
     * - Row 0 is the header row; column indices are discovered dynamically.
     * - The "action" column (if present) is read but NEVER treated as skill_code.
     * - Sheet "00_INSTRUCTIONS" is always skipped.
     */
    private void parseSheet(Workbook wb, String sheetName, List<ParsedRow> out) {
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) return;
        if (SHEET_INSTRUCTIONS.equalsIgnoreCase(sheetName)) return;

        // --- Build header map from row 0 ---
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return;
        Map<String, Integer> hdr = buildHeaderMap(headerRow);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowBlank(row)) continue;

            ParsedRow pr = new ParsedRow();
            pr.sheetName    = sheetName;
            pr.rowNumber    = r + 1;
            pr.skillCode    = norm(getByHeader(row, hdr, H_SKILL_CODE));
            pr.skillName    = normOpt(getByHeader(row, hdr, H_SKILL_NAME));
            pr.categoryCode = norm(getByHeader(row, hdr, H_CATEGORY));
            pr.description  = normOpt(getByHeader(row, hdr, H_DESCRIPTION));
            // scope — accept both "scope" and "scope_code"
            String scope = getByHeader(row, hdr, H_SCOPE_CODE);
            if (blank(scope)) scope = getByHeader(row, hdr, H_SCOPE);
            pr.scopeCode    = norm(scope);
            // skill_type — accept both "skill_type" and "skill_type_code"
            String st = getByHeader(row, hdr, H_SKILL_TYPE_C);
            if (blank(st)) st = getByHeader(row, hdr, H_SKILL_TYPE);
            pr.skillTypeCode = norm(st);
            pr.activeStr    = normOpt(getByHeader(row, hdr, H_IS_ACTIVE));
            pr.appTypeCode  = norm(getByHeader(row, hdr, H_APP_TYPE));
            pr.bundleCode   = norm(getByHeader(row, hdr, H_BUNDLE));
            pr.appCode      = norm(getByHeader(row, hdr, H_APP_CODE));
            // action column — optional, read but never used as skill_code
            pr.actionHint   = normOpt(getByHeader(row, hdr, H_ACTION));

            out.add(pr);
        }
    }

    /**
     * Reads row 0 and returns a map from normalized header name to column index.
     * Normalization: trim + lowercase + replace whitespace with '_'.
     */
    static Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String raw = cell.getStringCellValue();
            if (raw == null || raw.isBlank()) continue;
            String key = raw.trim().toLowerCase().replaceAll("\\s+", "_");
            map.putIfAbsent(key, c);
        }
        return map;
    }

    /** Reads a cell value using the header-name map. Returns "" if column not found. */
    private String getByHeader(Row row, Map<String, Integer> hdr, String headerKey) {
        Integer col = hdr.get(headerKey);
        if (col == null) return "";
        return getString(row, col);
    }

    // ================================================================
    // Entity build helpers (use ParsedRow — header-driven, all fields already resolved)
    // ================================================================

    private Skill buildInsertFromRow(ParsedRow pr, String actor) {
        return Skill.builder()
                .publicId(UUID.randomUUID().toString())
                .skillCode(pr.skillCode)
                .skillName(pr.skillName)
                .description(pr.description)
                .skillCategory(skillMasterService.resolveCategory(pr.categoryCode))
                .skillScopeLookup(!blank(pr.scopeCode)
                        ? lookupValueRepository.findByTypeCodeAndValueCode("SKILL_SCOPE", pr.scopeCode).orElse(null)
                        : null)
                .skillTypeLookup(!blank(pr.skillTypeCode)
                        ? lookupValueRepository.findByTypeCodeAndValueCode("SKILL_TYPE", pr.skillTypeCode).orElse(null)
                        : null)
                .applicationType(!blank(pr.appTypeCode)
                        ? appTypeRepository.findByTypeCode(pr.appTypeCode).orElse(null) : null)
                .bundle(!blank(pr.bundleCode)
                        ? bundleRepository.findByBundleCode(pr.bundleCode).orElse(null) : null)
                .application(!blank(pr.appCode)
                        ? applicationRepository.findByApplicationCode(pr.appCode).orElse(null) : null)
                .active(parseActive(pr.activeStr))
                .createdBy(actor)
                .updatedBy(actor)
                .build();
    }

    /**
     * Applies non-blank ParsedRow values to the existing Skill entity.
     * Blank values are ignored (do NOT overwrite DB value with blank).
     */
    private void applyUpdateFromRow(Skill existing, ParsedRow pr, String actor) {
        if (!blank(pr.skillName))    existing.setSkillName(pr.skillName);
        if (!blank(pr.categoryCode)) existing.setSkillCategory(skillMasterService.resolveCategory(pr.categoryCode));
        if (!blank(pr.description))  existing.setDescription(pr.description);
        if (!blank(pr.scopeCode))
            existing.setSkillScopeLookup(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_SCOPE", pr.scopeCode).orElse(null));
        if (!blank(pr.skillTypeCode))
            existing.setSkillTypeLookup(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_TYPE", pr.skillTypeCode).orElse(null));
        if (!blank(pr.activeStr))    existing.setActive(parseActive(pr.activeStr));
        if (!blank(pr.appTypeCode))  existing.setApplicationType(appTypeRepository.findByTypeCode(pr.appTypeCode).orElse(null));
        if (!blank(pr.bundleCode))   existing.setBundle(bundleRepository.findByBundleCode(pr.bundleCode).orElse(null));
        if (!blank(pr.appCode))      existing.setApplication(applicationRepository.findByApplicationCode(pr.appCode).orElse(null));
        existing.setUpdatedBy(actor);
    }

    // ================================================================
    // Validation helpers
    // ================================================================

    private List<String> validateRequiredForInsert(ParsedRow pr) {
        List<String> errors = new ArrayList<>();
        if (blank(pr.skillName))    errors.add("skill_name is required");
        if (blank(pr.categoryCode)) errors.add("category_code is required");
        if (blank(pr.scopeCode))    errors.add("scope / scope_code is required");
        return errors;
    }

    private List<String> detectChanges(ParsedRow pr, Skill existing) {
        List<String> changes = new ArrayList<>();
        if (!blank(pr.skillName) && !pr.skillName.equalsIgnoreCase(existing.getSkillName()))
            changes.add("skill_name");
        if (!blank(pr.categoryCode)) {
            String dbCat = existing.getSkillCategory() != null
                    ? existing.getSkillCategory().getCategoryCode() : null;
            if (!pr.categoryCode.equalsIgnoreCase(nullSafe(dbCat))) changes.add("category_code");
        }
        if (!blank(pr.description)) {
            if (!pr.description.equals(nullSafe(existing.getDescription()))) changes.add("description");
        }
        if (!blank(pr.scopeCode)) {
            String dbScope = existing.getSkillScopeLookup() != null
                    ? existing.getSkillScopeLookup().getValueCode() : null;
            if (!pr.scopeCode.equalsIgnoreCase(nullSafe(dbScope))) changes.add("scope");
        }
        if (!blank(pr.skillTypeCode)) {
            String dbType = existing.getSkillTypeLookup() != null
                    ? existing.getSkillTypeLookup().getValueCode() : null;
            if (!pr.skillTypeCode.equalsIgnoreCase(nullSafe(dbType))) changes.add("skill_type");
        }
        if (!blank(pr.activeStr) && parseActive(pr.activeStr) != existing.isActive())
            changes.add("is_active");
        return changes;
    }

    // ================================================================
    // Import history
    // ================================================================

    private ImportExportHistory saveHistory(String fileName, String actorUsername,
            int total, int inserted, int updated, int skipped, int failed,
            String statusCode, String errorSummary) {
        var user   = userRepository.findByUsername(actorUsername).orElseThrow();
        var status = lookupValueRepository.findByTypeCodeAndValueCode("IMPORT_EXPORT_STATUS", statusCode).orElse(null);
        int success = inserted + updated + skipped;
        ImportExportHistory h = ImportExportHistory.builder()
                .publicId(UUID.randomUUID().toString())
                .operationType("IMPORT")
                .templateType("SKILLS_IMPORT")
                .fileName(fileName)
                .totalRows(total)
                .successRows(success)
                .failedRows(failed)
                .insertedRows(inserted)
                .updatedRows(updated)
                .skippedRows(skipped)
                .statusLookup(status)
                .errorSummary(errorSummary)
                .initiatedBy(user)
                .completedAt(LocalDateTime.now())
                .build();
        return historyRepository.save(h);
    }

    // ================================================================
    // Template sheets
    // ================================================================

    private void createCommonSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_COMMON);
        Row header = sheet.createRow(0);
        String[] cols = {"skill_code","skill_name","category_code","description","scope_code","skill_type","is_active","remarks"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("SQL_QUERY");
        sample.createCell(1).setCellValue("SQL Query and Database Analysis");
        sample.createCell(2).setCellValue("DATABASE");
        sample.createCell(3).setCellValue("Ability to write SQL queries");
        sample.createCell(4).setCellValue("GLOBAL");
        sample.createCell(5).setCellValue("TECHNICAL");
        sample.createCell(6).setCellValue("true");
        sample.createCell(7).setCellValue("");
    }

    private void createAppSpecificSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_APP_SPECIFIC);
        Row header = sheet.createRow(0);
        String[] cols = {"application_type_code","bundle_code","application_code","skill_code","skill_name",
                "category_code","description","scope_code","skill_type","is_active","remarks"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("WEB"); sample.createCell(1).setCellValue("B06");
        sample.createCell(2).setCellValue("ATLAS"); sample.createCell(3).setCellValue("ATLAS_APP_KNOWLEDGE");
        sample.createCell(4).setCellValue("ATLAS Application Knowledge");
        sample.createCell(5).setCellValue("APP_KNOWLEDGE"); sample.createCell(6).setCellValue("Functional knowledge of ATLAS");
        sample.createCell(7).setCellValue("APPLICATION"); sample.createCell(8).setCellValue("TECHNICAL");
        sample.createCell(9).setCellValue("true"); sample.createCell(10).setCellValue("");
    }

    private void createVwToolsSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_VW_TOOLS);
        Row header = sheet.createRow(0);
        String[] cols = {"application_type_code","bundle_code","application_code","skill_code","skill_name",
                "category_code","description","scope_code","skill_type","is_active","remarks"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
        Row sample = sheet.createRow(1);
        for (int i = 0; i < 3; i++) sample.createCell(i).setCellValue("");
        sample.createCell(3).setCellValue("JIRA_SERVICEDESK");
        sample.createCell(4).setCellValue("JIRA Service Management");
        sample.createCell(5).setCellValue("TOOLS");
        sample.createCell(6).setCellValue("JIRA Service Management usage");
        sample.createCell(7).setCellValue("GLOBAL"); sample.createCell(8).setCellValue("NON_TECHNICAL");
        sample.createCell(9).setCellValue("true"); sample.createCell(10).setCellValue("");
    }

    // ================================================================
    // String utilities
    // ================================================================

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()).trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private boolean isRowBlank(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!blank(getString(row, i))) return false;
        }
        return true;
    }

    /** Normalize a code field: trim + uppercase. Returns null if blank. */
    private String norm(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim().toUpperCase();
    }

    /** Normalize an optional field: trim only. Returns null if blank. */
    private String normOpt(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private String nullSafe(String s) { return s == null ? "" : s.trim(); }

    private boolean parseActive(String s) {
        if (blank(s)) return true;
        String v = s.trim().toLowerCase();
        return switch (v) {
            case "false", "0", "inactive", "no" -> false;
            default -> true;
        };
    }

    // ================================================================
    // ParsedRow — internal normalized row (header-driven)
    // ================================================================

    static class ParsedRow {
        String sheetName;
        int    rowNumber;
        String skillCode;      // normalized: UPPER, null if blank
        String skillName;      // normalized: trimmed, null if blank
        String categoryCode;   // normalized: UPPER, null if blank
        String description;    // normalized: trimmed, null if blank
        String scopeCode;      // normalized: UPPER, null if blank
        String skillTypeCode;  // normalized: UPPER, null if blank
        String activeStr;      // raw active string, null if blank
        String appTypeCode;
        String bundleCode;
        String appCode;
        String actionHint;     // value from optional "action" column — informational only

        boolean sameData(ParsedRow other) {
            return Objects.equals(skillCode,     other.skillCode)
                && Objects.equals(skillName,     other.skillName)
                && Objects.equals(categoryCode,  other.categoryCode)
                && Objects.equals(description,   other.description)
                && Objects.equals(scopeCode,     other.scopeCode)
                && Objects.equals(skillTypeCode, other.skillTypeCode)
                && Objects.equals(activeStr,     other.activeStr)
                && Objects.equals(appTypeCode,   other.appTypeCode)
                && Objects.equals(bundleCode,    other.bundleCode)
                && Objects.equals(appCode,       other.appCode);
        }
    }
}
