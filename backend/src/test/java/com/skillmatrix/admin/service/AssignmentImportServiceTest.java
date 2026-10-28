package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.AssignmentImportResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Regression coverage for the Technician Assignment bulk import "Confirm Import
 * fails with 'An unexpected error occurred'" defect (correlation ID
 * abfa9e9a-e7de-4f6c-8640-0d593685cfd4), root-caused to the same
 * missing-import_mode class of defect as Application Master import. Also
 * covers the PARTIAL-processing per-row transaction isolation, template
 * identity validation, and duplicate-assignment re-check at confirm time.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentImportService — unit tests")
class AssignmentImportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationTypeRepository appTypeRepository;
    @Mock private BundleRepository bundleRepository;
    @Mock private UserApplicationMappingRepository mappingRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private ImportExportHistoryRepository historyRepository;
    @Mock private AuditService auditService;

    private AssignmentImportService service;

    private ApplicationType webType;
    private Bundle b06;
    private ApplicationPortfolio portfolio;
    private Application vts;
    private User tech1;
    private User tech2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        service = new AssignmentImportService(
                userRepository, applicationRepository, appTypeRepository, bundleRepository,
                mappingRepository, lookupValueRepository, historyRepository, auditService);

        webType = new ApplicationType();
        webType.setId(1L);
        webType.setTypeCode("WEB");

        b06 = new Bundle();
        b06.setId(1L);
        b06.setBundleCode("B06");

        portfolio = new ApplicationPortfolio();
        portfolio.setId(100L);
        portfolio.setPortfolioCode("B06-WEB");
        portfolio.setApplicationType(webType);
        portfolio.setBundle(b06);

        vts = Application.builder()
                .id(10L).publicId("app-pub-vts").portfolio(portfolio)
                .applicationCode("VTS").applicationName("VTS App").active(true)
                .build();

        tech1 = new User();
        tech1.setId(1L);
        tech1.setPublicId("tech-1-pub");
        tech1.setUsername("10260057");
        tech1.setActive(true);

        tech2 = new User();
        tech2.setId(2L);
        tech2.setPublicId("tech-2-pub");
        tech2.setUsername("10259812");
        tech2.setActive(true);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");

        when(appTypeRepository.findByTypeCode("WEB")).thenReturn(Optional.of(webType));
        when(bundleRepository.findByBundleCode("B06")).thenReturn(Optional.of(b06));
        when(applicationRepository.findByApplicationCode("VTS")).thenReturn(Optional.of(vts));
        when(userRepository.findByUsername("10260057")).thenReturn(Optional.of(tech1));
        when(userRepository.findByUsername("10259812")).thenReturn(Optional.of(tech2));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(lookupValueRepository.findByTypeCodeAndValueCode(eq("IMPORT_EXPORT_STATUS"), anyString()))
                .thenReturn(Optional.empty());
        when(historyRepository.save(any(ImportExportHistory.class)))
                .thenAnswer(inv -> {
                    ImportExportHistory h = inv.getArgument(0);
                    if (h.getPublicId() == null) h.setPublicId("hist-pub-1");
                    return h;
                });
        when(mappingRepository.findActiveByUserPublicIdAndAppPublicId(any(), any()))
                .thenReturn(Optional.empty());
    }

    // ----------------------------------------------------------------
    // Regression: Confirm succeeds for valid rows, sets importMode=PARTIAL
    // ----------------------------------------------------------------

    @Test
    @DisplayName("confirmImport succeeds for two valid technicians (10260057, 10259812) -> VTS and sets importMode=PARTIAL")
    void confirmImport_twoValidTechnicians_succeedsAndSetsImportMode() throws IOException {
        MockMultipartFile file = buildFile(
                row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""),
                row("10259812", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));

        when(mappingRepository.saveAndFlush(any(UserApplicationMapping.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AssignmentImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(2);
        assertThat(result.errorRows()).isEqualTo(0);

        ArgumentCaptor<ImportExportHistory> captor = ArgumentCaptor.forClass(ImportExportHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getImportMode()).isEqualTo("PARTIAL");
    }

    @Test
    @DisplayName("confirmImport populates publicId, allocation, effectiveFrom correctly on insert")
    void confirmImport_populatesFieldsCorrectly() throws IOException {
        MockMultipartFile file = buildFile(row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));

        ArgumentCaptor<UserApplicationMapping> captor = ArgumentCaptor.forClass(UserApplicationMapping.class);
        when(mappingRepository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmImport(file, "admin");

        UserApplicationMapping saved = captor.getValue();
        assertThat(saved.getPublicId()).isNotBlank();
        assertThat(saved.getAllocationPercentage().intValue()).isEqualTo(100);
        assertThat(saved.getEffectiveFrom().toString()).isEqualTo("2026-10-26");
        assertThat(saved.getApplication()).isEqualTo(vts);
        assertThat(saved.getUser()).isEqualTo(tech1);
    }

    // ----------------------------------------------------------------
    // Template identity validation
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Application Master workbook (wrong sheet) is rejected with controlled error, not silently Total=0")
    void validate_rejectsWrongTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("01_APPLICATIONS");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("application_type_code");
            header.createCell(1).setCellValue("application_code");
            wb.write(out);
            MockMultipartFile wrongFile = new MockMultipartFile("file", "Application_Master_Import_Template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                    () -> service.validateOnly(wrongFile));
        }
    }

    @Test
    @DisplayName("Confirm also rejects wrong template before any persistence occurs")
    void confirmImport_rejectsWrongTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("01_APPLICATIONS");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("application_type_code");
            wb.write(out);
            MockMultipartFile wrongFile = new MockMultipartFile("file", "Application_Master_Import_Template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                    () -> service.confirmImport(wrongFile, "admin"));
            verify(mappingRepository, never()).saveAndFlush(any());
        }
    }

    // ----------------------------------------------------------------
    // Validate/Confirm consistency
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Inactive technician is detected during validate, not only confirm")
    void validate_detectsInactiveTechnician() throws IOException {
        tech1.setActive(false);
        MockMultipartFile file = buildFile(row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));
        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isFalse();
        assertThat(validation.errorRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("Inactive application is detected during validate")
    void validate_detectsInactiveApplication() throws IOException {
        vts.setActive(false);
        MockMultipartFile file = buildFile(row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));
        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isFalse();
    }

    @Test
    @DisplayName("Existing identical active assignment classified SKIP on validate, and skip count matches on confirm")
    void existingIdenticalAssignment_classifiedSkip() throws IOException {
        UserApplicationMapping existing = UserApplicationMapping.builder()
                .id(1L).publicId("map-pub-1").user(tech1).application(vts)
                .allocationPercentage(new java.math.BigDecimal("100"))
                .effectiveFrom(java.time.LocalDate.parse("2026-10-26"))
                .active(true).build();
        when(mappingRepository.findActiveByUserPublicIdAndAppPublicId("tech-1-pub", "app-pub-vts"))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = buildFile(row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));
        var validation = service.validateOnly(file);
        assertThat(validation.skipRows()).isEqualTo(1);

        AssignmentImportResult result = service.confirmImport(file, "admin");
        assertThat(result.skippedRows()).isEqualTo(1);
        assertThat(result.insertedRows()).isEqualTo(0);
    }

    @Test
    @DisplayName("Existing assignment with changed allocation classified UPDATE, and confirm updates it")
    void existingChangedAssignment_classifiedUpdate_andConfirmed() throws IOException {
        UserApplicationMapping existing = UserApplicationMapping.builder()
                .id(1L).publicId("map-pub-1").user(tech1).application(vts)
                .allocationPercentage(new java.math.BigDecimal("50"))
                .effectiveFrom(java.time.LocalDate.parse("2026-10-26"))
                .active(true).build();
        when(mappingRepository.findActiveByUserPublicIdAndAppPublicId("tech-1-pub", "app-pub-vts"))
                .thenReturn(Optional.of(existing));
        when(mappingRepository.saveAndFlush(any(UserApplicationMapping.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildFile(row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));
        var validation = service.validateOnly(file);
        assertThat(validation.updateRows()).isEqualTo(1);

        AssignmentImportResult result = service.confirmImport(file, "admin");
        assertThat(result.updatedRows()).isEqualTo(1);
        assertThat(existing.getAllocationPercentage().intValue()).isEqualTo(100);
    }

    // ----------------------------------------------------------------
    // PARTIAL-processing per-row transaction isolation
    // ----------------------------------------------------------------

    @Test
    @DisplayName("One row's persistence failure does not prevent other valid rows from committing (PARTIAL mode)")
    void confirmImport_onePersistenceFailure_othersStillSucceed() throws IOException {
        MockMultipartFile file = buildFile(
                row("10260057", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""),
                row("10259812", "WEB", "B06", "VTS", "100", "2026-10-26", "", ""));

        when(mappingRepository.saveAndFlush(any(UserApplicationMapping.class)))
                .thenThrow(new DataIntegrityViolationException("simulated constraint violation"))
                .thenAnswer(inv -> inv.getArgument(0));

        AssignmentImportResult result = service.confirmImport(file, "admin");

        assertThat(result.insertedRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("PARTIAL_SUCCESS");
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String[] row(String username, String typeCode, String bundleCode, String appCode,
                          String allocationPct, String effectiveFrom, String effectiveTo, String roleOnApp) {
        return new String[] { "", username, typeCode, bundleCode, appCode, allocationPct,
                effectiveFrom, effectiveTo, roleOnApp, "", "" };
    }

    private MockMultipartFile buildFile(String[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet(AssignmentImportService.SHEET_ASSIGNMENTS);
            String[] headers = {"action", "username", "application_type_code", "bundle_code", "application_code",
                    "allocation_pct", "effective_from", "effective_to", "role_on_app_code", "is_primary", "remarks"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            int r = 1;
            for (String[] values : rows) {
                Row dataRow = sheet.createRow(r++);
                for (int i = 0; i < values.length; i++) dataRow.createCell(i).setCellValue(values[i]);
            }
            wb.write(out);
            return new MockMultipartFile("file", "Technician_Assignment_Import_Template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
