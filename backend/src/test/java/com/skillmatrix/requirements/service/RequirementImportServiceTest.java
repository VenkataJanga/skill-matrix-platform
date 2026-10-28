package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.*;
import com.skillmatrix.requirements.dto.RequirementImportResult;
import com.skillmatrix.requirements.dto.RequirementImportRowPreview;
import com.skillmatrix.requirements.dto.RequirementValidationResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequirementImportService — SMART_UPSERT tests")
class RequirementImportServiceTest {

    @Mock SkillRepository skillRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock ApplicationTypeRepository appTypeRepository;
    @Mock BundleRepository bundleRepository;
    @Mock RequirementVersionRepository requirementVersionRepository;
    @Mock ExpectedRatingRepository expectedRatingRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock ImportExportHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock RequirementVersionService requirementVersionService;
    @Mock AuditService auditService;

    @InjectMocks RequirementImportService service;

    private ApplicationType webType;
    private Bundle b06;
    private ApplicationPortfolio portfolio;
    private Application atlasApp;
    private Skill javaSkill;
    private RequirementVersion draftVersion;
    private User adminUser;

    @BeforeEach
    void setUp() {
        webType = ApplicationType.builder().id(1L).publicId(UUID.randomUUID().toString())
                .typeCode("WEB").typeName("Web Application").active(true).build();
        b06 = Bundle.builder().id(1L).publicId(UUID.randomUUID().toString())
                .bundleCode("B06").bundleName("Bundle 06").active(true).build();
        portfolio = ApplicationPortfolio.builder().id(1L).publicId(UUID.randomUUID().toString())
                .applicationType(webType).bundle(b06)
                .portfolioCode("B06-WEB").portfolioName("Bundle 06 Web").active(true).build();
        atlasApp = Application.builder().id(1L).publicId(UUID.randomUUID().toString())
                .portfolio(portfolio).applicationCode("ATLAS_DEZENTRAL").applicationName("ATLAS-deZentral")
                .active(true).build();
        javaSkill = Skill.builder().id(1L).publicId(UUID.randomUUID().toString())
                .skillCode("TECH_JAVA").skillName("Java").active(true).build();
        draftVersion = RequirementVersion.builder().id(1L).publicId(UUID.randomUUID().toString())
                .application(atlasApp).versionCode("DRAFT-1").versionName("Draft").active(true).build();
        adminUser = User.builder().id(1L).username("admin").passwordHash("x")
                .email("a@b.com").fullName("Admin").active(true).build();
    }

    // ---------------------------------------------------------------
    // Workbook helpers
    // ---------------------------------------------------------------

    private Workbook buildDataWorkbook(Object[][] rows) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(RequirementImportService.SHEET_DATA);
        Row header = sheet.createRow(0);
        String[] cols = {"action","application_type_code","bundle_code","application_code","application_name",
                "skill_code","skill_name","skill_group","expected_level","mandatory_flag",
                "role_code","grade_code","is_active","remarks"};
        for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
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

    private void stubValidLookups() {
        when(appTypeRepository.findByTypeCode("WEB")).thenReturn(Optional.of(webType));
        when(bundleRepository.findByBundleCode("B06")).thenReturn(Optional.of(b06));
        when(applicationRepository.findByApplicationCode("ATLAS_DEZENTRAL")).thenReturn(Optional.of(atlasApp));
        when(skillRepository.findBySkillCode("TECH_JAVA")).thenReturn(Optional.of(javaSkill));
    }

    // ================================================================
    // Test: new mapping -> INSERT
    // ================================================================

    @Test
    @DisplayName("New application+skill mapping is marked INSERT")
    void newMapping_insert() throws IOException {
        stubValidLookups();
        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of());

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.insertRows()).isEqualTo(1);
        assertThat(result.rows().get(0).action()).isEqualTo("INSERT");
    }

    // ================================================================
    // Test: same imported mapping -> SKIP
    // ================================================================

    @Test
    @DisplayName("Identical existing mapping is marked SKIP")
    void sameMapping_skip() throws IOException {
        stubValidLookups();
        ExpectedRating existing = ExpectedRating.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .requirementVersion(draftVersion).skill(javaSkill)
                .expectedLevel(3).mandatory(true).active(true).build();

        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of(draftVersion));
        when(expectedRatingRepository.findByRequirementVersion_IdAndSkill_Id(1L, 1L))
                .thenReturn(Optional.of(existing));

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.skipRows()).isEqualTo(1);
        assertThat(result.rows().get(0).action()).isEqualTo("SKIP");
    }

    // ================================================================
    // Test: changed expected level -> UPDATE
    // ================================================================

    @Test
    @DisplayName("Changed expected level is marked UPDATE")
    void changedLevel_update() throws IOException {
        stubValidLookups();
        ExpectedRating existing = ExpectedRating.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .requirementVersion(draftVersion).skill(javaSkill)
                .expectedLevel(3).mandatory(true).active(true).build();

        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of(draftVersion));
        when(expectedRatingRepository.findByRequirementVersion_IdAndSkill_Id(1L, 1L))
                .thenReturn(Optional.of(existing));

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "4", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.updateRows()).isEqualTo(1);
        assertThat(result.rows().get(0).action()).isEqualTo("UPDATE");
    }

    // ================================================================
    // Test: changed mandatory flag -> UPDATE
    // ================================================================

    @Test
    @DisplayName("Changed mandatory flag is marked UPDATE")
    void changedMandatory_update() throws IOException {
        stubValidLookups();
        ExpectedRating existing = ExpectedRating.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .requirementVersion(draftVersion).skill(javaSkill)
                .expectedLevel(3).mandatory(true).active(true).build();

        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of(draftVersion));
        when(expectedRatingRepository.findByRequirementVersion_IdAndSkill_Id(1L, 1L))
                .thenReturn(Optional.of(existing));

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "N", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.updateRows()).isEqualTo(1);
    }

    // ================================================================
    // Test: invalid row -> ERROR (unknown skill_code)
    // ================================================================

    @Test
    @DisplayName("Unknown skill_code produces ERROR row")
    void unknownSkill_error() throws IOException {
        when(appTypeRepository.findByTypeCode("WEB")).thenReturn(Optional.of(webType));
        when(bundleRepository.findByBundleCode("B06")).thenReturn(Optional.of(b06));
        when(applicationRepository.findByApplicationCode("ATLAS_DEZENTRAL")).thenReturn(Optional.of(atlasApp));
        when(skillRepository.findBySkillCode("TECH_UNKNOWN")).thenReturn(Optional.empty());

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_UNKNOWN", "Unknown", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows().get(0).reason()).contains("Unknown skill_code");
    }

    // ================================================================
    // Test: invalid expected_level -> ERROR
    // ================================================================

    @Test
    @DisplayName("expected_level out of range 1-5 produces ERROR row")
    void invalidExpectedLevel_error() throws IOException {
        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "9", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows().get(0).fieldErrors()).anyMatch(e -> e.contains("expected_level"));
    }

    // ================================================================
    // Test: invalid mandatory_flag -> ERROR
    // ================================================================

    @Test
    @DisplayName("mandatory_flag not Y/N produces ERROR row")
    void invalidMandatoryFlag_error() throws IOException {
        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "MAYBE", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows().get(0).fieldErrors()).anyMatch(e -> e.contains("mandatory_flag"));
    }

    // ================================================================
    // Test: application/type/bundle mismatch -> ERROR
    // ================================================================

    @Test
    @DisplayName("Application not belonging to type/bundle produces ERROR row")
    void appTypeBundleMismatch_error() throws IOException {
        ApplicationType hostType = ApplicationType.builder().id(2L).publicId(UUID.randomUUID().toString())
                .typeCode("HOST").typeName("Host Application").active(true).build();
        when(appTypeRepository.findByTypeCode("HOST")).thenReturn(Optional.of(hostType));
        when(bundleRepository.findByBundleCode("B06")).thenReturn(Optional.of(b06));
        when(applicationRepository.findByApplicationCode("ATLAS_DEZENTRAL")).thenReturn(Optional.of(atlasApp));

        // atlasApp belongs to WEB/B06, but row claims HOST/B06 -> mismatch
        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "HOST", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.valid()).isFalse();
        assertThat(result.rows().get(0).reason()).contains("does not belong to");
    }

    // ================================================================
    // Test: confirm import -> new mapping INSERT persisted
    // ================================================================

    @Test
    @DisplayName("Confirm import inserts new mapping and records history")
    void confirmImport_insertsNewMapping() throws IOException {
        stubValidLookups();
        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of());
        when(requirementVersionService.getOrCreateDraftVersion(eq(atlasApp), eq("admin"))).thenReturn(draftVersion);
        when(expectedRatingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = (ImportExportHistory) inv.getArgument(0);
            h.setPublicId(UUID.randomUUID().toString());
            return h;
        });

        MockMultipartFile file = toMultipart(buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        }));

        RequirementImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.updatedRows()).isEqualTo(0);
        verify(auditService).log(eq("REQUIREMENT_IMPORT_CONFIRM"), any(), any(), any(), any(), any(), any(), any());
    }

    // ================================================================
    // Test: confirm import -> error rows block import (no writes)
    // ================================================================

    @Test
    @DisplayName("Confirm import blocks when error rows exist and performs no writes")
    void confirmImport_blockedByErrors() throws IOException {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = (ImportExportHistory) inv.getArgument(0);
            h.setPublicId(UUID.randomUUID().toString());
            return h;
        });

        // expected_level invalid -> ERROR row, blocks the whole import
        MockMultipartFile file = toMultipart(buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "9", "Y", "", "", "1", ""}
        }));

        RequirementImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isFalse();
        assertThat(result.errorRows()).isGreaterThan(0);
        assertThat(result.insertedRows()).isEqualTo(0);
        verify(expectedRatingRepository, never()).saveAll(any());
    }

    // ================================================================
    // Test: duplicate mapping inside Excel detected
    // ================================================================

    @Test
    @DisplayName("Duplicate application_code + skill_code within the file is marked ERROR")
    void duplicateInFile_error() throws IOException {
        stubValidLookups();
        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of());

        Workbook wb = buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""},
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "4", "Y", "", "", "1", ""}
        });
        RequirementValidationResult result = service.validateOnly(toMultipart(wb));

        assertThat(result.valid()).isFalse();
        assertThat(result.rows().get(1).reason()).contains("Duplicate");
    }

    // ================================================================
    // Test: import history counts correct
    // ================================================================

    @Test
    @DisplayName("Import history records correct inserted/skipped counts")
    void importHistory_correctCounts() throws IOException {
        stubValidLookups();
        ExpectedRating existingSpring = ExpectedRating.builder()
                .id(2L).publicId(UUID.randomUUID().toString())
                .requirementVersion(draftVersion).skill(javaSkill)
                .expectedLevel(3).mandatory(true).active(true).build();

        when(requirementVersionRepository.findAllByApplicationId(1L)).thenReturn(List.of(draftVersion));
        when(expectedRatingRepository.findByRequirementVersion_IdAndSkill_Id(1L, 1L))
                .thenReturn(Optional.of(existingSpring));
        when(expectedRatingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(historyRepository.save(any())).thenAnswer(inv -> {
            var h = (ImportExportHistory) inv.getArgument(0);
            h.setPublicId(UUID.randomUUID().toString());
            return h;
        });

        // Same data as existing -> SKIP
        MockMultipartFile file = toMultipart(buildDataWorkbook(new Object[][]{
                {"", "WEB", "B06", "ATLAS_DEZENTRAL", "ATLAS-deZentral", "TECH_JAVA", "Java", "COMMON_TECH", "3", "Y", "", "", "1", ""}
        }));

        RequirementImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.skippedRows()).isEqualTo(1);
        assertThat(result.insertedRows()).isEqualTo(0);
        assertThat(result.updatedRows()).isEqualTo(0);
    }
}
