package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.ExpectedRating;
import com.skillmatrix.domain.entity.ImportExportHistory;
import com.skillmatrix.domain.entity.RequirementVersion;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.ExpectedRatingRepository;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.requirements.dto.RequirementImportResult;
import com.skillmatrix.requirements.dto.RequirementImportRowPreview;
import com.skillmatrix.requirements.dto.RequirementValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.*;

/**
 * Excel SMART_UPSERT import for Application Skill Requirement Mapping (M3.1).
 * Mirrors SkillImportService's header-driven parsing + SMART_UPSERT pattern.
 *
 * Workbook: 04_APPLICATION_SKILL_REQUIREMENT_MAPPING.xlsx
 * Sheets: 00_INSTRUCTIONS, 01_ALLOWED_VALUES, 02_APPLICATION_SKILL_REQUIREMENTS
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequirementImportService {

    static final String SHEET_INSTRUCTIONS   = "00_INSTRUCTIONS";
    static final String SHEET_ALLOWED_VALUES = "01_ALLOWED_VALUES";
    // NOTE: Excel sheet names are limited to 31 characters.
    // "02_APPLICATION_SKILL_REQUIREMENTS" (33 chars) would be silently
    // trimmed by POI, breaking wb.getSheet() lookups. Use a shortened name.
    static final String SHEET_DATA           = "02_APP_SKILL_REQUIREMENTS";

    static final String ACTION_INSERT = "INSERT";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_SKIP   = "SKIP";
    static final String ACTION_ERROR  = "ERROR";

    private static final String H_APP_TYPE       = "application_type_code";
    private static final String H_BUNDLE         = "bundle_code";
    private static final String H_APP_CODE       = "application_code";
    private static final String H_APP_NAME       = "application_name";
    private static final String H_SKILL_CODE     = "skill_code";
    private static final String H_SKILL_NAME     = "skill_name";
    private static final String H_SKILL_GROUP    = "skill_group";
    private static final String H_EXPECTED_LEVEL = "expected_level";
    private static final String H_MANDATORY_FLAG = "mandatory_flag";
    private static final String H_ROLE_CODE      = "role_code";
    private static final String H_GRADE_CODE     = "grade_code";
    private static final String H_IS_ACTIVE      = "is_active";
    private static final String H_ACTION         = "action";

    private final SkillRepository skillRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTypeRepository appTypeRepository;
    private final BundleRepository bundleRepository;
    private final RequirementVersionRepository requirementVersionRepository;
    private final ExpectedRatingRepository expectedRatingRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ImportExportHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final RequirementVersionService requirementVersionService;
    private final AuditService auditService;

    // ================================================================
    // Template download
    // ================================================================

    public ResponseEntity<byte[]> generateTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createInstructionsSheet(wb);
            createAllowedValuesSheet(wb);
            createDataSheet(wb);
            wb.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment",
                    "04_APPLICATION_SKILL_REQUIREMENT_MAPPING.xlsx");
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        }
    }

    private void createInstructionsSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_INSTRUCTIONS);
        String[] lines = {
            "APPLICATION SKILL REQUIREMENT MAPPING — IMPORT INSTRUCTIONS",
            "",
            "1. Fill in sheet 02_APPLICATION_SKILL_REQUIREMENTS.",
            "2. application_type_code, bundle_code, application_code, skill_code, expected_level, mandatory_flag are mandatory.",
            "3. skill_code must already exist in Skill Master. New skills are NOT created from this sheet.",
            "4. expected_level must be between 1 and 5.",
            "5. mandatory_flag must be Y or N.",
            "6. SMART_UPSERT: new mapping -> INSERT, changed mapping -> UPDATE, identical mapping -> SKIP, invalid row -> ERROR.",
            "7. Validate before confirming import. Confirm is blocked while ERROR rows exist.",
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
    }

    private void createAllowedValuesSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_ALLOWED_VALUES);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("field");
        header.createCell(1).setCellValue("allowed_values");
        String[][] rows = {
            {"application_type_code", "WEB, HOST, BATCH, API, MOBILE"},
            {"bundle_code", "B06, B12, B20"},
            {"skill_group", "COMMON_TECH, APP_SPECIFIC, VW_TOOL"},
            {"expected_level", "1,2,3,4,5"},
            {"mandatory_flag", "Y, N"},
            {"is_active", "1, 0"},
        };
        for (int i = 0; i < rows.length; i++) {
            Row r = sheet.createRow(i + 1);
            r.createCell(0).setCellValue(rows[i][0]);
            r.createCell(1).setCellValue(rows[i][1]);
        }
    }

    private void createDataSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(SHEET_DATA);
        Row header = sheet.createRow(0);
        String[] cols = {"action","application_type_code","bundle_code","application_code","application_name",
                "skill_code","skill_name","skill_group","expected_level","mandatory_flag",
                "role_code","grade_code","is_active","remarks"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
        Row sample = sheet.createRow(1);
        String[] vals = {"","WEB","B06","ATLAS_DEZENTRAL","ATLAS-deZentral",
                "TECH_JAVA","Java","COMMON_TECH","3","Y","","","1",""};
        for (int i = 0; i < vals.length; i++) sample.createCell(i).setCellValue(vals[i]);
    }

    // ================================================================
    // Parsing
    // ================================================================

    private List<ParsedRow> parseDataSheet(Workbook wb) {
        List<ParsedRow> out = new ArrayList<>();
        Sheet sheet = wb.getSheet(SHEET_DATA);
        if (sheet == null) return out;

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return out;
        Map<String, Integer> hdr = buildHeaderMap(headerRow);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowBlank(row)) continue;

            ParsedRow pr = new ParsedRow();
            pr.rowNumber       = r + 1;
            pr.actionHint      = normOpt(getByHeader(row, hdr, H_ACTION));
            pr.appTypeCode     = norm(getByHeader(row, hdr, H_APP_TYPE));
            pr.bundleCode      = norm(getByHeader(row, hdr, H_BUNDLE));
            pr.appCode         = norm(getByHeader(row, hdr, H_APP_CODE));
            pr.appName         = normOpt(getByHeader(row, hdr, H_APP_NAME));
            pr.skillCode       = norm(getByHeader(row, hdr, H_SKILL_CODE));
            pr.skillName       = normOpt(getByHeader(row, hdr, H_SKILL_NAME));
            pr.skillGroup      = norm(getByHeader(row, hdr, H_SKILL_GROUP));
            pr.expectedLevelStr= normOpt(getByHeader(row, hdr, H_EXPECTED_LEVEL));
            pr.mandatoryFlag   = norm(getByHeader(row, hdr, H_MANDATORY_FLAG));
            pr.roleCode        = norm(getByHeader(row, hdr, H_ROLE_CODE));
            pr.gradeCode       = norm(getByHeader(row, hdr, H_GRADE_CODE));
            pr.activeStr       = normOpt(getByHeader(row, hdr, H_IS_ACTIVE));

            try {
                pr.expectedLevel = blank(pr.expectedLevelStr) ? null : Integer.parseInt(pr.expectedLevelStr.trim());
            } catch (NumberFormatException e) {
                pr.expectedLevel = null;
            }

            out.add(pr);
        }
        return out;
    }

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

    private String getByHeader(Row row, Map<String, Integer> hdr, String headerKey) {
        Integer col = hdr.get(headerKey);
        if (col == null) return "";
        return getString(row, col);
    }

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

    private String norm(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim().toUpperCase();
    }

    private String normOpt(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private boolean parseMandatory(String s) {
        return "Y".equalsIgnoreCase(s);
    }

    private boolean parseActive(String s) {
        if (blank(s)) return true;
        String v = s.trim();
        return !("0".equals(v) || "false".equalsIgnoreCase(v));
    }

    static class ParsedRow {
        int rowNumber;
        String actionHint;
        String appTypeCode;
        String bundleCode;
        String appCode;
        String appName;
        String skillCode;
        String skillName;
        String skillGroup;
        String expectedLevelStr;
        Integer expectedLevel;
        String mandatoryFlag;
        String roleCode;
        String gradeCode;
        String activeStr;
    }

    // ================================================================
    // Validation / preview building
    // ================================================================

    public RequirementValidationResult validateOnly(MultipartFile file) throws IOException {
        log.info("Requirement import validation started: file={}", file.getOriginalFilename());
        List<RequirementImportRowPreview> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            List<ParsedRow> parsed = parseDataSheet(wb);
            rows = buildPreviews(parsed);
        }
        RequirementValidationResult result = RequirementValidationResult.of(rows);
        log.info("Requirement import validation complete: file={} total={} insert={} update={} skip={} error={}",
                file.getOriginalFilename(), result.totalRows(), result.insertRows(),
                result.updateRows(), result.skipRows(), result.errorRows());
        return result;
    }

    private List<RequirementImportRowPreview> buildPreviews(List<ParsedRow> parsed) {
        List<RequirementImportRowPreview> previews = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (ParsedRow pr : parsed) {
            previews.add(buildPreview(pr, seenKeys));
        }
        return previews;
    }

    private RequirementImportRowPreview buildPreview(ParsedRow pr, Set<String> seenKeys) {
        List<String> errors = new ArrayList<>();

        if (blank(pr.appTypeCode)) errors.add("application_type_code is required");
        if (blank(pr.bundleCode))  errors.add("bundle_code is required");
        if (blank(pr.appCode))     errors.add("application_code is required");
        if (blank(pr.skillCode))   errors.add("skill_code is required");
        if (blank(pr.expectedLevelStr)) errors.add("expected_level is required");
        else if (pr.expectedLevel == null || pr.expectedLevel < 1 || pr.expectedLevel > 5)
            errors.add("expected_level must be between 1 and 5");
        if (blank(pr.mandatoryFlag)) errors.add("mandatory_flag is required");
        else if (!"Y".equalsIgnoreCase(pr.mandatoryFlag) && !"N".equalsIgnoreCase(pr.mandatoryFlag))
            errors.add("mandatory_flag must be Y or N");

        if (!errors.isEmpty()) {
            return RequirementImportRowPreview.error(SHEET_DATA, pr.rowNumber, pr.appCode, pr.skillCode,
                    pr.skillName, pr.expectedLevel, pr.mandatoryFlag, "Missing/invalid required field(s)", errors);
        }

        var appType = appTypeRepository.findByTypeCode(pr.appTypeCode).orElse(null);
        if (appType == null) {
            return err(pr, "Unknown application_type_code: " + pr.appTypeCode);
        }
        var bundle = bundleRepository.findByBundleCode(pr.bundleCode).orElse(null);
        if (bundle == null) {
            return err(pr, "Unknown bundle_code: " + pr.bundleCode);
        }
        Application app = applicationRepository.findByApplicationCode(pr.appCode).orElse(null);
        if (app == null) {
            return err(pr, "Unknown application_code: " + pr.appCode);
        }
        boolean belongsToTypeAndBundle =
                app.getPortfolio().getApplicationType().getTypeCode().equalsIgnoreCase(pr.appTypeCode)
             && app.getPortfolio().getBundle().getBundleCode().equalsIgnoreCase(pr.bundleCode);
        if (!belongsToTypeAndBundle) {
            return err(pr, "Application " + pr.appCode + " does not belong to " + pr.appTypeCode + " / " + pr.bundleCode);
        }
        if (!app.isActive()) {
            return err(pr, "Application is inactive: " + pr.appCode);
        }
        if (!blank(pr.appName) && !pr.appName.equalsIgnoreCase(app.getApplicationName())) {
            return err(pr, "application_name does not match application_code " + pr.appCode);
        }

        Skill skill = skillRepository.findBySkillCode(pr.skillCode).orElse(null);
        if (skill == null) {
            return err(pr, "Unknown skill_code: " + pr.skillCode);
        }
        if (!skill.isActive()) {
            return err(pr, "Skill is inactive: " + pr.skillCode);
        }
        if (!blank(pr.skillName) && !pr.skillName.equalsIgnoreCase(skill.getSkillName())) {
            return err(pr, "skill_name does not match skill_code " + pr.skillCode);
        }

        // Duplicate-in-file detection: key = application_code + skill_code
        String dupKey = pr.appCode + "::" + pr.skillCode;
        if (!seenKeys.add(dupKey)) {
            return err(pr, "Duplicate application_code + skill_code mapping in file: " + dupKey);
        }

        // SMART_UPSERT against DB — find an existing DRAFT (or any) version for this application
        var versions = requirementVersionRepository.findAllByApplicationId(app.getId());
        ExpectedRating existing = null;
        for (RequirementVersion rv : versions) {
            var found = expectedRatingRepository.findByRequirementVersion_IdAndSkill_Id(rv.getId(), skill.getId());
            if (found.isPresent()) { existing = found.get(); break; }
        }

        if (existing == null) {
            return RequirementImportRowPreview.of(SHEET_DATA, pr.rowNumber, pr.appCode, pr.skillCode, pr.skillName,
                    pr.expectedLevel, pr.mandatoryFlag, ACTION_INSERT, "New mapping — will be inserted");
        }

        boolean levelChanged = !Objects.equals(existing.getExpectedLevel(), pr.expectedLevel);
        boolean mandatoryChanged = existing.isMandatory() != parseMandatory(pr.mandatoryFlag);
        if (levelChanged || mandatoryChanged) {
            return RequirementImportRowPreview.of(SHEET_DATA, pr.rowNumber, pr.appCode, pr.skillCode, pr.skillName,
                    pr.expectedLevel, pr.mandatoryFlag, ACTION_UPDATE, "Mapping will be updated");
        }
        return RequirementImportRowPreview.of(SHEET_DATA, pr.rowNumber, pr.appCode, pr.skillCode, pr.skillName,
                pr.expectedLevel, pr.mandatoryFlag, ACTION_SKIP, "Same data already exists — no change needed");
    }

    private RequirementImportRowPreview err(ParsedRow pr, String message) {
        return RequirementImportRowPreview.error(SHEET_DATA, pr.rowNumber, pr.appCode, pr.skillCode,
                pr.skillName, pr.expectedLevel, pr.mandatoryFlag, message, List.of(message));
    }

    // ================================================================
    // Confirm import (SMART_UPSERT, STRICT — all-valid-or-nothing)
    // ================================================================

    @Transactional
    public RequirementImportResult confirmImport(MultipartFile file, String actorUsername) throws IOException {
        log.info("Requirement import confirm started: file={} by={}", file.getOriginalFilename(), actorUsername);

        List<ParsedRow> allParsed;
        List<RequirementImportRowPreview> previews;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            allParsed = parseDataSheet(wb);
            previews  = buildPreviews(allParsed);
        }

        RequirementValidationResult validation = RequirementValidationResult.of(previews);
        if (!validation.valid()) {
            log.warn("Requirement import aborted — error rows: file={} errorRows={} by={}",
                    file.getOriginalFilename(), validation.errorRows(), actorUsername);
            saveHistory(file.getOriginalFilename(), actorUsername,
                    validation.totalRows(), 0, 0, 0, validation.errorRows(), "FAILED",
                    "Import blocked: " + validation.errorRows() + " error row(s) found.");
            return new RequirementImportResult(false, validation.totalRows(), 0, 0, 0,
                    validation.errorRows(), null,
                    "Import failed: " + validation.errorRows() + " error row(s) must be fixed.");
        }

        int inserted = 0, updated = 0, skipped = validation.skipRows();
        List<ExpectedRating> toSave = new ArrayList<>();
        Map<Long, RequirementVersion> versionCache = new HashMap<>();

        for (int i = 0; i < previews.size(); i++) {
            RequirementImportRowPreview pv = previews.get(i);
            ParsedRow pr = allParsed.get(i);
            if (ACTION_SKIP.equals(pv.action()) || ACTION_ERROR.equals(pv.action())) continue;

            Application app = applicationRepository.findByApplicationCode(pr.appCode).orElseThrow();
            RequirementVersion rv = versionCache.computeIfAbsent(app.getId(),
                    id -> requirementVersionService.getOrCreateDraftVersion(app, actorUsername));
            Skill skill = skillRepository.findBySkillCode(pr.skillCode).orElseThrow();

            if (ACTION_INSERT.equals(pv.action())) {
                ExpectedRating er = ExpectedRating.builder()
                        .publicId(UUID.randomUUID().toString())
                        .requirementVersion(rv)
                        .skill(skill)
                        .expectedLevel(pr.expectedLevel)
                        .mandatory(parseMandatory(pr.mandatoryFlag))
                        .active(parseActive(pr.activeStr))
                        .createdBy(actorUsername)
                        .updatedBy(actorUsername)
                        .build();
                toSave.add(er);
                inserted++;
            } else if (ACTION_UPDATE.equals(pv.action())) {
                ExpectedRating existing = expectedRatingRepository
                        .findByRequirementVersion_IdAndSkill_Id(rv.getId(), skill.getId()).orElseThrow();
                existing.setExpectedLevel(pr.expectedLevel);
                existing.setMandatory(parseMandatory(pr.mandatoryFlag));
                if (!blank(pr.activeStr)) existing.setActive(parseActive(pr.activeStr));
                existing.setUpdatedBy(actorUsername);
                toSave.add(existing);
                updated++;
            }
        }

        expectedRatingRepository.saveAll(toSave);

        ImportExportHistory history = saveHistory(
                file.getOriginalFilename(), actorUsername,
                validation.totalRows(), inserted, updated, skipped, 0, "SUCCESS", null);

        auditService.log("REQUIREMENT_IMPORT_CONFIRM", "SKILL_REQUIREMENT", null, actorUsername, null, null, null,
                "{\"inserted\":" + inserted + ",\"updated\":" + updated +
                        ",\"skipped\":" + skipped + ",\"file\":\"" + file.getOriginalFilename() + "\"}");

        log.info("Requirement import completed: file={} inserted={} updated={} skipped={} by={}",
                file.getOriginalFilename(), inserted, updated, skipped, actorUsername);

        return new RequirementImportResult(true, validation.totalRows(),
                inserted, updated, skipped, 0,
                history.getPublicId(),
                "Import successful: " + inserted + " inserted, " + updated + " updated, " + skipped + " skipped.");
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
                .templateType("APPLICATION_SKILL_REQUIREMENT")
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
                .importMode("STRICT")
                .completedAt(LocalDateTime.now())
                .build();
        return historyRepository.save(h);
    }

    public com.skillmatrix.common.dto.PagedResponse<com.skillmatrix.skills.dto.ImportHistoryDto> getHistory(
            int page, int size, String statusCode, String uploadedBy, String fromDate, String toDate) {

        LocalDateTime from = fromDate != null && !fromDate.isBlank()
                ? LocalDateTime.parse(fromDate + "T00:00:00") : null;
        LocalDateTime to = toDate != null && !toDate.isBlank()
                ? LocalDateTime.parse(toDate + "T23:59:59") : null;

        String cleanStatus = (statusCode == null || statusCode.isBlank()) ? null : statusCode.trim();
        String cleanUser   = (uploadedBy == null || uploadedBy.isBlank()) ? null : uploadedBy.trim();

        var result = historyRepository.searchHistory(
                "APPLICATION_SKILL_REQUIREMENT", cleanStatus, cleanUser, from, to,
                org.springframework.data.domain.PageRequest.of(page, size));
        return com.skillmatrix.common.dto.PagedResponse.of(
                result.getContent().stream().map(com.skillmatrix.skills.dto.ImportHistoryDto::from).toList(), result);
    }
}
