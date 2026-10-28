package com.skillmatrix.skills.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.entity.SkillCategory;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.*;
import com.skillmatrix.skills.dto.SkillImportRowPreview;
import com.skillmatrix.skills.dto.SkillImportResult;
import com.skillmatrix.skills.dto.SkillValidationResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SkillImportService} SMART_UPSERT behaviour.
 * Uses in-memory XSSFWorkbook to avoid file I/O.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SkillImportService — SMART_UPSERT tests")
class SkillImportServiceTest {

    @Mock SkillRepository skillRepository;
    @Mock SkillCategoryRepository categoryRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock ApplicationTypeRepository appTypeRepository;
    @Mock BundleRepository bundleRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock ImportExportHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock SkillMasterService skillMasterService;
    @Mock AuditService auditService;

    @InjectMocks SkillImportService service;

    private SkillCategory techCat;
    private Skill existingSkill;

    @BeforeEach
    void setUp() {
        techCat = SkillCategory.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .categoryCode("TECHNICAL").categoryName("Technical Skills")
                .active(true).build();

        existingSkill = Skill.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .skillCode("TECH_JAVA").skillName("Java")
                .skillCategory(techCat)
                .description("Core Java skills")
                .active(true).build();
    }

    // ---------------------------------------------------------------
    // Helper: build a workbook with one sheet
    // ---------------------------------------------------------------

    private Workbook buildCommonWorkbook(Object[][] rows) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(SkillImportService.SHEET_COMMON);
        // header row
        Row header = sheet.createRow(0);
        String[] cols = {"skill_code","skill_name","category_code","description","scope","is_active","remarks"};
        for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
        // data rows
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                if (rows[r][c] != null) row.createCell(c).setCellValue(String.valueOf(rows[r][c]));
            }
        }
        return wb;
    }

    private MockMultipartFile toMultipart(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
    }

    // ================================================================
    // Test 1: New skill row → INSERT
    // ================================================================

    @Test
    @DisplayName("New skill_code not in DB is marked INSERT")
    void newSkillMarkedInsert() throws IOException {
        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"NEW_SKILL", "New Skill Name", "TECHNICAL", "A desc", "GLOBAL", "true", ""}
        });
        when(categoryRepository.existsByCategoryCode("TECHNICAL")).thenReturn(true);
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("INSERT");
        assertThat(rows.get(0).skillCode()).isEqualTo("NEW_SKILL");
    }

    // ================================================================
    // Test 2: Existing identical skill → SKIP
    // ================================================================

    @Test
    @DisplayName("Existing skill with identical data is marked SKIP")
    void identicalExistingSkillMarkedSkip() throws IOException {
        // existingSkill has skillName="Java", categoryCode="TECHNICAL", description="Core Java skills"
        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_JAVA", "Java", "TECHNICAL", "Core Java skills", null, "true", ""}
        });
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(existingSkill));
        // scopeCode is null in Excel → no scope change check required
        // For the category check: existingSkill has categoryCode=TECHNICAL, Excel has TECHNICAL → same

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("SKIP");
        assertThat(rows.get(0).reason()).contains("Same data already exists");
    }

    // ================================================================
    // Test 3: Skill exists with null DB description, Excel provides description → UPDATE
    // ================================================================

    @Test
    @DisplayName("Existing skill with null DB description and non-blank Excel description is marked UPDATE")
    void missingDbDescriptionWithExcelDescriptionMarkedUpdate() throws IOException {
        Skill noDescSkill = Skill.builder()
                .id(2L).publicId(UUID.randomUUID().toString())
                .skillCode("TECH_SQL").skillName("SQL")
                .skillCategory(techCat).description(null).active(true).build();

        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_SQL", "SQL", "TECHNICAL", "SQL query skills added", null, "true", ""}
        });
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(noDescSkill));

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("UPDATE");
        assertThat(rows.get(0).reason()).contains("description");
    }

    // ================================================================
    // Test 4: Skill exists with different description → UPDATE
    // ================================================================

    @Test
    @DisplayName("Existing skill with different description is marked UPDATE")
    void differentDescriptionMarkedUpdate() throws IOException {
        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_JAVA", "Java", "TECHNICAL", "Updated Java description", null, "true", ""}
        });
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(existingSkill));

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("UPDATE");
        assertThat(rows.get(0).reason()).contains("description");
    }

    // ================================================================
    // Test 5: Excel blank description does NOT overwrite DB description → SKIP
    // ================================================================

    @Test
    @DisplayName("Blank Excel description does not overwrite DB description — row marked SKIP")
    void blankExcelDescriptionDoesNotOverwriteDb() throws IOException {
        // existingSkill has description="Core Java skills"; Excel provides blank description
        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_JAVA", "Java", "TECHNICAL", "", null, "true", ""}
                // blank description
        });
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(existingSkill));

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        // Blank Excel desc = no change. All other fields match → SKIP
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("SKIP");
    }

    // ================================================================
    // Test 6: Same duplicate rows in Excel → second row SKIP with warning
    // ================================================================

    @Test
    @DisplayName("Same duplicate rows in Excel — first kept, second marked SKIP")
    void duplicateSameRowsFirstKeptSecondSkipped() throws IOException {
        when(categoryRepository.existsByCategoryCode("TECHNICAL")).thenReturn(true);
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());

        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_NEW", "New Tech", "TECHNICAL", "desc", "GLOBAL", "true", ""},
                {"TECH_NEW", "New Tech", "TECHNICAL", "desc", "GLOBAL", "true", ""}
                // exact duplicate
        });

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).action()).isEqualTo("INSERT"); // first row: new skill
        assertThat(rows.get(1).action()).isEqualTo("SKIP");   // duplicate
        assertThat(rows.get(1).reason()).containsIgnoringCase("duplicate");
    }

    // ================================================================
    // Test 7: Conflicting duplicate rows → both ERROR
    // ================================================================

    @Test
    @DisplayName("Conflicting duplicate skill_codes in Excel are both marked ERROR")
    void conflictingDuplicatesAreBothError() throws IOException {
        Workbook wb = buildCommonWorkbook(new Object[][]{
                {"TECH_CONFLICT", "Name A", "TECHNICAL", "desc A", "GLOBAL", "true", ""},
                {"TECH_CONFLICT", "Name B", "TECHNICAL", "desc B", "GLOBAL", "true", ""}
                // same code, different name/desc
        });
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).action()).isEqualTo("ERROR");
        assertThat(rows.get(1).action()).isEqualTo("ERROR");
    }

    // ================================================================
    // Test 8: Confirm import inserts new skills
    // ================================================================

    @Test
    @DisplayName("Confirm import inserts rows marked INSERT")
    void confirmImportInsertNewSkills() throws IOException {
        when(categoryRepository.existsByCategoryCode("TECHNICAL")).thenReturn(true);
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());
        when(skillMasterService.resolveCategory("TECHNICAL")).thenReturn(techCat);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), any())).thenReturn(Optional.empty());
        when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = inv.getArgument(0);
            ((com.skillmatrix.domain.entity.ImportExportHistory) h)
                    .setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildCommonWorkbook(new Object[][]{
                {"TECH_NEW2", "New Skill 2", "TECHNICAL", "desc", "GLOBAL", "true", ""}
        }));

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.updatedRows()).isEqualTo(0);
    }

    // ================================================================
    // Test 9: Confirm import updates existing skills
    // ================================================================

    @Test
    @DisplayName("Confirm import updates rows marked UPDATE")
    void confirmImportUpdatesExistingSkill() throws IOException {
        // existingSkill has description="Core Java skills"; Excel provides different desc
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(existingSkill));
        when(skillMasterService.resolveCategory("TECHNICAL")).thenReturn(techCat);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), any())).thenReturn(Optional.empty());
        when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = inv.getArgument(0);
            ((com.skillmatrix.domain.entity.ImportExportHistory) h)
                    .setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildCommonWorkbook(new Object[][]{
                {"TECH_JAVA", "Java", "TECHNICAL", "Updated Java description", null, "true", ""}
        }));

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.updatedRows()).isEqualTo(1);
        assertThat(result.insertedRows()).isEqualTo(0);
    }

    // ================================================================
    // Test 10: Confirm import skips unchanged skills
    // ================================================================

    @Test
    @DisplayName("Confirm import skips rows marked SKIP")
    void confirmImportSkipsUnchangedSkill() throws IOException {
        // Same data — should be SKIP, nothing saved
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(existingSkill));
        when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = inv.getArgument(0);
            ((com.skillmatrix.domain.entity.ImportExportHistory) h)
                    .setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildCommonWorkbook(new Object[][]{
                {"TECH_JAVA", "Java", "TECHNICAL", "Core Java skills", null, "true", ""}
        }));

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.skippedRows()).isEqualTo(1);
        assertThat(result.insertedRows()).isEqualTo(0);
        assertThat(result.updatedRows()).isEqualTo(0);
    }

    // ================================================================
    // Test 11: Confirm import aborts when error rows exist
    // ================================================================

    @Test
    @DisplayName("Confirm import aborts and returns failure when error rows exist")
    void confirmImportRollsBackOnErrorRows() throws IOException {
        // Conflicting duplicates → ERROR rows → import must be blocked
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());

        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = inv.getArgument(0);
            ((com.skillmatrix.domain.entity.ImportExportHistory) h)
                    .setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildCommonWorkbook(new Object[][]{
                {"TECH_ERR", "Name A", "TECHNICAL", "desc A", "GLOBAL", "true", ""},
                {"TECH_ERR", "Name B", "TECHNICAL", "desc B", "GLOBAL", "true", ""}
        }));

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isFalse();
        assertThat(result.errorRows()).isGreaterThan(0);
        assertThat(result.insertedRows()).isEqualTo(0);
        // saveAll should NOT be called
        verify(skillRepository, never()).saveAll(any());
    }

    // ================================================================
    // Test 13: Excel with action column first does NOT treat action as skill_code
    // ================================================================

    @Test
    @DisplayName("Excel with action column first does not corrupt skill_code")
    void actionColumnFirstDoesNotCorruptSkillCode() throws IOException {
        // Build workbook where action is column 0, skill_code is column 1
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(SkillImportService.SHEET_COMMON);
        // Header: action first, then skill fields
        Row header = sheet.createRow(0);
        String[] cols = {"action","skill_code","skill_name","category_code","description","scope_code","skill_type","is_active"};
        for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
        // Data row
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("INSERT");            // action column — must NOT be used as skill_code
        row.createCell(1).setCellValue("TECH_ACTION_TEST");  // actual skill_code
        row.createCell(2).setCellValue("Action Test Skill");
        row.createCell(3).setCellValue("TECHNICAL");
        row.createCell(4).setCellValue("test desc");
        row.createCell(5).setCellValue("GLOBAL");
        row.createCell(6).setCellValue("TECHNICAL");
        row.createCell(7).setCellValue("true");

        when(categoryRepository.existsByCategoryCode("TECHNICAL")).thenReturn(true);
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of());

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        // skill_code must be the real code, NOT "INSERT"
        assertThat(rows.get(0).skillCode()).isEqualTo("TECH_ACTION_TEST");
        assertThat(rows.get(0).action()).isEqualTo("INSERT");
    }

    // ================================================================
    // Test 14: buildHeaderMap normalizes header names correctly
    // ================================================================

    @Test
    @DisplayName("buildHeaderMap normalizes header names and handles extra spaces")
    void buildHeaderMap_normalizesHeaders() {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("test");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("  Action  ");   // extra spaces
        header.createCell(1).setCellValue("Skill Code");   // space → underscore
        header.createCell(2).setCellValue("SKILL_NAME");   // uppercase → lowercase
        header.createCell(3).setCellValue("scope_code");   // already normalized

        var map = SkillImportService.buildHeaderMap(header);

        assertThat(map).containsEntry("action",     0);
        assertThat(map).containsEntry("skill_code", 1);
        assertThat(map).containsEntry("skill_name", 2);
        assertThat(map).containsEntry("scope_code", 3);
        try { wb.close(); } catch (Exception ignored) {}
    }

    // ================================================================
    // Test 15: Wrong/unrelated template is rejected with controlled message
    // ================================================================

    @Test
    @DisplayName("Workbook without any recognized Skill Import sheet is rejected with a controlled message")
    void wrongTemplateIsRejectedWithControlledMessage() throws IOException {
        // Build a workbook that mimics an unrelated template (e.g. Application Master)
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Applications");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("application_code");
        header.createCell(1).setCellValue("application_name");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("IFA");
        row.createCell(1).setCellValue("IFA Application");

        List<SkillImportRowPreview> rows = service.analyzeRows(wb);
        wb.close();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).action()).isEqualTo("ERROR");
        assertThat(rows.get(0).reason()).contains("Incorrect import template");

        // Confirm must not persist anything either
        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = inv.getArgument(0);
            ((com.skillmatrix.domain.entity.ImportExportHistory) h)
                    .setPublicId(UUID.randomUUID().toString());
            return h;
        });

        XSSFWorkbook wb2 = new XSSFWorkbook();
        Sheet sheet2 = wb2.createSheet("Applications");
        Row header2 = sheet2.createRow(0);
        header2.createCell(0).setCellValue("application_code");
        MockMultipartFile file = toMultipart(wb2);

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Incorrect import template");
        verify(skillRepository, never()).saveAll(any());
    }

    // ================================================================
    // Test 12: Import history counts are correct
    // ================================================================

    @Test
    @DisplayName("Import history records correct inserted/updated/skipped counts")
    void importHistoryCountsAreCorrect() throws IOException {
        // Two rows: one new INSERT, one unchanged SKIP
        Skill otherSkill = Skill.builder()
                .id(3L).publicId(UUID.randomUUID().toString())
                .skillCode("TECH_SPRING").skillName("Spring Boot")
                .skillCategory(techCat).description("Spring framework").active(true).build();

        when(categoryRepository.existsByCategoryCode("TECHNICAL")).thenReturn(true);
        when(skillRepository.findAllBySkillCodeIn(any())).thenReturn(List.of(otherSkill));
        when(skillMasterService.resolveCategory("TECHNICAL")).thenReturn(techCat);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), any())).thenReturn(Optional.empty());
        when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        // Capture the saved history to verify counts
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = (com.skillmatrix.domain.entity.ImportExportHistory) inv.getArgument(0);
            h.setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildCommonWorkbook(new Object[][]{
                // new skill → INSERT
                {"TECH_BRAND_NEW", "Brand New", "TECHNICAL", "desc", "GLOBAL", "true", ""},
                // existing unchanged → SKIP
                {"TECH_SPRING", "Spring Boot", "TECHNICAL", "Spring framework", null, "true", ""}
        }));

        SkillImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.skippedRows()).isEqualTo(1);
        assertThat(result.updatedRows()).isEqualTo(0);
        assertThat(result.errorRows()).isEqualTo(0);
    }

    // ================================================================
    // Export tests (read-only) — Phase 5/6/7 regression coverage
    // ================================================================

    private List<Skill> parseExportedSkillCodes(ResponseEntity<byte[]> response) throws IOException {
        List<Skill> result = new java.util.ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getBody()))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cell = row.getCell(0);
                if (cell == null) continue;
                Skill s = Skill.builder().skillCode(cell.getStringCellValue()).build();
                result.add(s);
            }
        }
        return result;
    }

    @Test
    @DisplayName("exportSkills (no filters) returns all skills as a valid Excel workbook")
    void exportSkillsAllReturnsExcelWorkbook() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingSkill));

        ResponseEntity<byte[]> response = service.exportSkills(null, null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString())
                .contains("spreadsheetml.sheet");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .startsWith("Skill_Master_Export_").endsWith(".xlsx");

        List<Skill> exported = parseExportedSkillCodes(response);
        assertThat(exported).hasSize(1);
        assertThat(exported.get(0).getSkillCode()).isEqualTo("TECH_JAVA");

        // Export must be read-only — no persistence calls of any kind
        verify(skillRepository, never()).save(any());
        verify(skillRepository, never()).saveAll(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("exportSkills respects search filter and delegates to SkillSpecification")
    void exportSkillsWithSearchFilter() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingSkill));

        ResponseEntity<byte[]> response = service.exportSkills("java", null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(skillRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("exportSkills respects category filter")
    void exportSkillsWithCategoryFilter() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingSkill));

        ResponseEntity<byte[]> response = service.exportSkills(null, "TECHNICAL", null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Skill> exported = parseExportedSkillCodes(response);
        assertThat(exported).hasSize(1);
    }

    @Test
    @DisplayName("exportSkills respects scope filter")
    void exportSkillsWithScopeFilter() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingSkill));

        ResponseEntity<byte[]> response = service.exportSkills(null, null, "GLOBAL", null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("exportSkills respects active/inactive status filter")
    void exportSkillsWithStatusFilter() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(existingSkill));

        ResponseEntity<byte[]> response = service.exportSkills(null, null, null, null, null, null, "ACTIVE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("exportSkills with no matching records returns an empty (header-only) workbook, not an error")
    void exportSkillsEmptyResult() throws IOException {
        when(skillRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        ResponseEntity<byte[]> response = service.exportSkills("no-such-skill", null, null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Skill> exported = parseExportedSkillCodes(response);
        assertThat(exported).isEmpty();
    }
}
