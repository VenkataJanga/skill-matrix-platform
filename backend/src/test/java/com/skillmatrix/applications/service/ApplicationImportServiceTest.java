package com.skillmatrix.applications.service;

import com.skillmatrix.applications.dto.ApplicationImportResult;
import com.skillmatrix.audit.service.AuditService;
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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
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
 * Regression coverage for the Application Master bulk import "STRICT confirm
 * fails with 'Column import_mode cannot be null'" defect, plus the broader
 * Validate/Confirm consistency and transactional-failure behavior this fix
 * introduces. See docs note in ApplicationImportService.saveHistory().
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApplicationImportService — unit tests")
class ApplicationImportServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationPortfolioRepository portfolioRepository;
    @Mock private ApplicationTypeRepository appTypeRepository;
    @Mock private BundleRepository bundleRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private ImportExportHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private ApplicationImportService service;

    private ApplicationType webType;
    private Bundle b06;
    private ApplicationPortfolio portfolio;
    private LookupValue activeLifecycle;
    private User adminUser;

    @BeforeEach
    void setUp() {
        service = new ApplicationImportService(
                applicationRepository, portfolioRepository, appTypeRepository,
                bundleRepository, lookupValueRepository, historyRepository,
                userRepository, auditService);

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

        activeLifecycle = new LookupValue();
        activeLifecycle.setId(500L);
        activeLifecycle.setValueCode("ACTIVE");

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");

        when(appTypeRepository.findByTypeCode("WEB")).thenReturn(Optional.of(webType));
        when(bundleRepository.findByBundleCode("B06")).thenReturn(Optional.of(b06));
        when(portfolioRepository.findByPortfolioCodeAndTypeCodeAndBundleCode("B06-WEB", "WEB", "B06"))
                .thenReturn(Optional.of(portfolio));
        when(lookupValueRepository.findByTypeCodeAndValueCode("APPLICATION_LIFECYCLE_STATUS", "ACTIVE"))
                .thenReturn(Optional.of(activeLifecycle));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(lookupValueRepository.findByTypeCodeAndValueCode(eq("IMPORT_EXPORT_STATUS"), anyString()))
                .thenReturn(Optional.empty());
        when(historyRepository.save(any(ImportExportHistory.class)))
                .thenAnswer(inv -> {
                    ImportExportHistory h = inv.getArgument(0);
                    if (h.getPublicId() == null) h.setPublicId("hist-pub-1");
                    return h;
                });
        when(applicationRepository.findByPortfolio_IdAndApplicationCodeIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
    }

    // ----------------------------------------------------------------
    // Regression: STRICT confirm with valid INSERT rows must succeed and
    // populate import_mode (the exact bug from correlation ID 36ddee3d...)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("confirmImport succeeds for valid INSERT rows and sets importMode=STRICT on history")
    void confirmImport_validInsertRows_succeedsAndSetsImportMode() throws IOException {
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));

        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApplicationImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(1);

        ArgumentCaptor<ImportExportHistory> captor = ArgumentCaptor.forClass(ImportExportHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getImportMode()).isEqualTo("STRICT");
    }

    @Test
    @DisplayName("confirmImport handles multiple valid INSERT rows (VTS, WLTP_T, WLTP_V, WLTP_P)")
    void confirmImport_multipleValidInsertRows_allSucceed() throws IOException {
        MockMultipartFile file = buildFile(
                row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"),
                row("WEB", "B06", "B06-WEB", "WLTP_T", "WLTP T App", "ACTIVE", "1"),
                row("WEB", "B06", "B06-WEB", "WLTP_V", "WLTP V App", "ACTIVE", "1"),
                row("WEB", "B06", "B06-WEB", "WLTP_P", "WLTP P App", "ACTIVE", "1"));

        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApplicationImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isTrue();
        assertThat(result.insertedRows()).isEqualTo(4);
        verify(applicationRepository, times(4)).saveAndFlush(any(Application.class));
    }

    @Test
    @DisplayName("confirmImport assigns lifecycle lookup and isActive correctly on insert")
    void confirmImport_assignsLifecycleAndActiveCorrectly() throws IOException {
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        when(applicationRepository.saveAndFlush(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        service.confirmImport(file, "admin");

        Application saved = captor.getValue();
        assertThat(saved.getLifecycleStatusLookup()).isEqualTo(activeLifecycle);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPortfolio()).isEqualTo(portfolio);
        assertThat(saved.getPublicId()).isNotBlank();
    }

    // ----------------------------------------------------------------
    // Validate/Confirm consistency
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Unknown portfolio is detected at validate time (and blocks confirm identically)")
    void validateAndConfirm_detectInvalidPortfolioConsistently() throws IOException {
        when(portfolioRepository.findByPortfolioCodeAndTypeCodeAndBundleCode("BAD-PORT", "WEB", "B06"))
                .thenReturn(Optional.empty());
        MockMultipartFile file = buildFile(row("WEB", "B06", "BAD-PORT", "XYZ", "XYZ App", "ACTIVE", "1"));

        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isFalse();
        assertThat(validation.errorRows()).isEqualTo(1);

        ApplicationImportResult confirmResult = service.confirmImport(file, "admin");
        assertThat(confirmResult.success()).isFalse();
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Unknown lifecycle_status is detected at validate time")
    void validate_detectsInvalidLifecycle() throws IOException {
        when(lookupValueRepository.findByTypeCodeAndValueCode("APPLICATION_LIFECYCLE_STATUS", "BOGUS"))
                .thenReturn(Optional.empty());
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "XYZ", "XYZ App", "BOGUS", "1"));

        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isFalse();
        assertThat(validation.errorRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("Missing mandatory field (application_name) is detected at validate time")
    void validate_detectsMissingMandatoryField() throws IOException {
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "XYZ", "", "ACTIVE", "1"));
        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isFalse();
    }

    @Test
    @DisplayName("Within-file duplicate application_code (identical data) is classified SKIP, not inserted twice")
    void validate_detectsWithinFileDuplicateSkip() throws IOException {
        MockMultipartFile file = buildFile(
                row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"),
                row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));

        var validation = service.validateOnly(file);
        assertThat(validation.valid()).isTrue();
        assertThat(validation.insertRows()).isEqualTo(1);
        assertThat(validation.skipRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("Existing identical row is classified SKIP on validate")
    void validate_existingIdenticalRow_classifiedSkip() throws IOException {
        Application existing = Application.builder()
                .id(1L).publicId("pub-1").portfolio(portfolio)
                .applicationCode("VTS").applicationName("VTS App")
                .lifecycleStatusLookup(activeLifecycle).active(true)
                .build();
        when(applicationRepository.findByPortfolio_IdAndApplicationCodeIgnoreCase(100L, "VTS"))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));
        var validation = service.validateOnly(file);

        assertThat(validation.valid()).isTrue();
        assertThat(validation.skipRows()).isEqualTo(1);
        assertThat(validation.insertRows()).isEqualTo(0);
    }

    @Test
    @DisplayName("Existing row with changed name is classified UPDATE, and confirm updates it")
    void existingChangedRow_classifiedUpdate_andConfirmed() throws IOException {
        Application existing = Application.builder()
                .id(1L).publicId("pub-1").portfolio(portfolio)
                .applicationCode("VTS").applicationName("Old Name")
                .lifecycleStatusLookup(activeLifecycle).active(true)
                .build();
        when(applicationRepository.findByPortfolio_IdAndApplicationCodeIgnoreCase(100L, "VTS"))
                .thenReturn(Optional.of(existing));
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "New Name", "ACTIVE", "1"));

        var validation = service.validateOnly(file);
        assertThat(validation.updateRows()).isEqualTo(1);

        ApplicationImportResult result = service.confirmImport(file, "admin");
        assertThat(result.success()).isTrue();
        assertThat(result.updatedRows()).isEqualTo(1);
        assertThat(existing.getApplicationName()).isEqualTo("New Name");
    }

    // ----------------------------------------------------------------
    // Transactional failure behavior
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Unexpected persistence exception during confirm returns a controlled error response, not a raw 500")
    void confirmImport_persistenceException_returnsControlledError() throws IOException {
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));

        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("Column 'import_mode' cannot be null"));

        ApplicationImportResult result = service.confirmImport(file, "admin");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("VTS");
        assertThat(result.message()).doesNotContain("SQLException");
        assertThat(result.message()).doesNotContain("import_mode");
    }

    @Test
    @DisplayName("Persistence exception still records a FAILED history entry")
    void confirmImport_persistenceException_recordsFailedHistory() throws IOException {
        MockMultipartFile file = buildFile(row("WEB", "B06", "B06-WEB", "VTS", "VTS App", "ACTIVE", "1"));
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("simulated constraint violation"));

        service.confirmImport(file, "admin");

        ArgumentCaptor<ImportExportHistory> captor = ArgumentCaptor.forClass(ImportExportHistory.class);
        verify(historyRepository, atLeastOnce()).save(captor.capture());
        boolean hasFailedEntry = captor.getAllValues().stream()
                .anyMatch(h -> h.getErrorSummary() != null && h.getErrorSummary().contains("VTS"));
        assertThat(hasFailedEntry).isTrue();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String[] row(String typeCode, String bundleCode, String portfolioCode, String appCode,
                          String appName, String lifecycle, String isActive) {
        return new String[] { "", typeCode, bundleCode, portfolioCode, appCode, appName, lifecycle, isActive, "", "" };
    }

    private MockMultipartFile buildFile(String[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet(ApplicationImportService.SHEET_APPLICATIONS);
            String[] headers = {"action", "application_type_code", "bundle_code", "portfolio_code",
                    "application_code", "application_name", "lifecycle_status", "is_active", "description", "remarks"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            int r = 1;
            for (String[] values : rows) {
                Row dataRow = sheet.createRow(r++);
                for (int i = 0; i < values.length; i++) dataRow.createCell(i).setCellValue(values[i]);
            }
            wb.write(out);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
