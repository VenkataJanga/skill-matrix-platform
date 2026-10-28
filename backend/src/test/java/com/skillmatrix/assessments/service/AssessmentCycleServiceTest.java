package com.skillmatrix.assessments.service;

import com.skillmatrix.assessments.dto.CreateAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.ReopenAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.UpdateAssessmentCycleRequest;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentCycleService unit tests")
class AssessmentCycleServiceTest {

    @Mock AssessmentCycleRepository cycleRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock RequirementVersionRepository requirementVersionRepository;
    @Mock ExpectedRatingRepository expectedRatingRepository;
    @Mock TechnicianAssessmentHeaderRepository headerRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock AuditService auditService;

    /** Fixed "today" = 2026-10-26, within the openCycle/plannedCycle windows below. */
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 26);

    @Mock Clock clock;

    @InjectMocks AssessmentCycleService service;

    private Application atlasApp;
    private RequirementVersion rv;
    private AssessmentCycle plannedCycle;
    private AssessmentCycle openCycle;
    private AssessmentCycle closedCycle;
    private AssessmentCycle closedExpiredCycle;
    private LookupValue closedStatus;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = FIXED_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        atlasApp = Application.builder().id(1L).publicId("pub-app-atlas")
                .applicationCode("ATLAS_DEZENTRAL").applicationName("ATLAS-deZentral").active(true).build();
        rv = RequirementVersion.builder().id(1L).publicId("pub-rv-1")
                .application(atlasApp).versionCode("v1.0").versionName("v1").active(true).build();

        LookupValue planned = LookupValue.builder().id(1L).valueCode("PLANNED").valueLabel("Planned").active(true).build();
        LookupValue open = LookupValue.builder().id(2L).valueCode("OPEN").valueLabel("Open").active(true).build();
        closedStatus = LookupValue.builder().id(3L).valueCode("CLOSED").valueLabel("Closed").active(true).build();

        plannedCycle = AssessmentCycle.builder().id(1L).publicId("pub-cycle-1")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS FY26")
                .cycleStartDate(LocalDate.of(2026, 1, 1)).cycleEndDate(LocalDate.of(2026, 3, 31))
                .statusLookup(planned).active(true).build();

        openCycle = AssessmentCycle.builder().id(2L).publicId("pub-cycle-2")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Open")
                .cycleStartDate(LocalDate.of(2026, 1, 1)).cycleEndDate(LocalDate.of(2026, 3, 31))
                .statusLookup(open).active(true).build();

        // CLOSED cycle whose window still includes FIXED_TODAY (current/future window).
        closedCycle = AssessmentCycle.builder().id(3L).publicId("pub-cycle-closed")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q2 2026 Assessment")
                .cycleStartDate(FIXED_TODAY.minusDays(1)).cycleEndDate(FIXED_TODAY.plusDays(30))
                .statusLookup(closedStatus).active(true).build();

        // CLOSED cycle whose end date has already passed relative to FIXED_TODAY.
        closedExpiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-closed-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q1 2026 Assessment")
                .cycleStartDate(LocalDate.of(2026, 1, 1)).cycleEndDate(FIXED_TODAY.minusDays(5))
                .statusLookup(closedStatus).active(true).build();
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createCycle succeeds for valid application and requirement version")
    void createCycle_success() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS FY26", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS FY26")).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> {
            AssessmentCycle c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        var dto = service.createCycle(req, "admin");

        assertThat(dto.cycleName()).isEqualTo("ATLAS FY26");
        verify(auditService).log(eq("CREATE_ASSESSMENT_CYCLE"), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Duplicate cycle name (M4 stabilization)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createCycle rejects an exact duplicate cycle name for the same application with a friendly 409 message")
    void createCycle_exactDuplicateName_rejected() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS Q3 2026 Assessment", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        AssessmentCycle existing = AssessmentCycle.builder().id(5L).publicId("pub-cycle-q3")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q3 2026 Assessment")
                .cycleStartDate(LocalDate.of(2026, 7, 1)).cycleEndDate(LocalDate.of(2026, 8, 31))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS Q3 2026 Assessment"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createCycle(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("ATLAS Q3 2026 Assessment")
                .hasMessageContaining("ATLAS-deZentral")
                .hasMessageContaining("unique cycle name");
    }

    @Test
    @DisplayName("createCycle rejects a duplicate name that only differs by leading/trailing whitespace")
    void createCycle_trimmedDuplicateName_rejected() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "  ATLAS Q3 2026 Assessment  ", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        AssessmentCycle existing = AssessmentCycle.builder().id(5L).publicId("pub-cycle-q3")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q3 2026 Assessment")
                .cycleStartDate(LocalDate.of(2026, 7, 1)).cycleEndDate(LocalDate.of(2026, 8, 31))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS Q3 2026 Assessment"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createCycle(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("createCycle rejects a case-insensitive duplicate name")
    void createCycle_caseInsensitiveDuplicateName_rejected() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "atlas q3 2026 assessment", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        AssessmentCycle existing = AssessmentCycle.builder().id(5L).publicId("pub-cycle-q3")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q3 2026 Assessment")
                .cycleStartDate(LocalDate.of(2026, 7, 1)).cycleEndDate(LocalDate.of(2026, 8, 31))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "atlas q3 2026 assessment"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createCycle(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("createCycle succeeds with a unique cycle name even when an expired OPEN cycle exists for the same application")
    void createCycle_uniqueNameWithExistingExpiredCycle_succeeds() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS Q4 2026 Assessment", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS Q4 2026 Assessment"))
                .thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> {
            AssessmentCycle c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });

        var dto = service.createCycle(req, "admin");

        assertThat(dto.cycleName()).isEqualTo("ATLAS Q4 2026 Assessment");
    }

    @Test
    @DisplayName("createCycle rejects reuse of the exact same name as an existing CLOSED cycle")
    void createCycle_sameNameAsClosedCycle_rejected() {
        LookupValue closed = LookupValue.builder().id(3L).valueCode("CLOSED").valueLabel("Closed").active(true).build();
        AssessmentCycle closedCycle = AssessmentCycle.builder().id(6L).publicId("pub-cycle-closed")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q3 2026 Assessment")
                .cycleStartDate(LocalDate.of(2026, 7, 1)).cycleEndDate(LocalDate.of(2026, 8, 31))
                .statusLookup(closed).active(true).build();

        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS Q3 2026 Assessment", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS Q3 2026 Assessment"))
                .thenReturn(Optional.of(closedCycle));

        assertThatThrownBy(() -> service.createCycle(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("historical cycle is never deleted or overwritten when a new differently-named cycle is created")
    void createCycle_neverDeletesOrOverwritesHistoricalCycle() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS Q4 2026 Assessment", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26));

        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));
        when(cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(1L, "ATLAS Q4 2026 Assessment"))
                .thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createCycle(req, "admin");

        // createCycle only ever calls save() for the new cycle — it never calls
        // delete/deleteById on the repository, so any existing historical cycle
        // for this application remains completely untouched.
        verify(cycleRepository, never()).delete(any());
        verify(cycleRepository, never()).deleteById(any());
        verify(cycleRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createCycle rejects invalid application")
    void createCycle_invalidApplication() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "bad-app", "pub-rv-1", "Cycle", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        when(applicationRepository.findByPublicId("bad-app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCycle(req, "admin")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createCycle rejects requirement version belonging to another application")
    void createCycle_requirementVersionMismatch() {
        Application other = Application.builder().id(2L).publicId("pub-app-other")
                .applicationCode("OTHER").applicationName("Other").active(true).build();
        RequirementVersion otherRv = RequirementVersion.builder().id(2L).publicId("pub-rv-other")
                .application(other).versionCode("v1.0").active(true).build();

        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-other", "Cycle", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-other")).thenReturn(Optional.of(otherRv));

        assertThatThrownBy(() -> service.createCycle(req, "admin")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createCycle rejects end date before start date")
    void createCycle_invalidDates() {
        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "Cycle", LocalDate.of(2026, 3, 31), LocalDate.of(2026, 1, 1));
        when(applicationRepository.findByPublicId("pub-app-atlas")).thenReturn(Optional.of(atlasApp));
        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(rv));

        assertThatThrownBy(() -> service.createCycle(req, "admin")).isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------
    // Open
    // ---------------------------------------------------------------

    @Test
    @DisplayName("openCycle fails when requirement version has zero active skill requirements")
    void openCycle_emptyRequirementVersion() {
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));
        when(expectedRatingRepository.findAllByRequirementVersionId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.openCycle("pub-cycle-1", "admin")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("openCycle succeeds when requirement version has active skill requirements")
    void openCycle_success() {
        Skill skill = Skill.builder().id(1L).publicId(UUID.randomUUID().toString())
                .skillCode("TECH_JAVA").skillName("Java").active(true).build();
        ExpectedRating er = ExpectedRating.builder().id(1L).publicId(UUID.randomUUID().toString())
                .requirementVersion(rv).skill(skill).expectedLevel(3).mandatory(true).active(true).build();

        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));
        when(expectedRatingRepository.findAllByRequirementVersionId(1L)).thenReturn(List.of(er));
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.openCycle("pub-cycle-1", "admin");

        assertThat(dto).isNotNull();
        verify(auditService).log(eq("OPEN_ASSESSMENT_CYCLE"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("openCycle rejects a cycle that is not PLANNED")
    void openCycle_notPlanned() {
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(openCycle));

        assertThatThrownBy(() -> service.openCycle("pub-cycle-2", "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Close
    // ---------------------------------------------------------------

    @Test
    @DisplayName("closeCycle succeeds for an OPEN cycle")
    void closeCycle_success() {
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(openCycle));
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("CLOSED"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.closeCycle("pub-cycle-2", "admin");

        assertThat(dto).isNotNull();
        verify(auditService).log(eq("CLOSE_ASSESSMENT_CYCLE"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("closeCycle rejects a cycle that is not OPEN")
    void closeCycle_notOpen() {
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));

        assertThatThrownBy(() -> service.closeCycle("pub-cycle-1", "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Update — lock application/requirement version after first header
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateCycle rejects application change once a technician assessment header exists")
    void updateCycle_lockedAfterHeaderExists() {
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));
        when(headerRepository.existsByCycle_Id(1L)).thenReturn(true);

        UpdateAssessmentCycleRequest req = new UpdateAssessmentCycleRequest(
                "ATLAS FY26 Renamed", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                "pub-app-other", null);

        assertThatThrownBy(() -> service.updateCycle("pub-cycle-1", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("updateCycle allows name/date changes while PLANNED and no header exists")
    void updateCycle_success() {
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));
        when(headerRepository.existsByCycle_Id(1L)).thenReturn(false);
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateAssessmentCycleRequest req = new UpdateAssessmentCycleRequest(
                "ATLAS FY26 Renamed", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30), null, null);

        var dto = service.updateCycle("pub-cycle-1", req, "admin");

        assertThat(dto.cycleName()).isEqualTo("ATLAS FY26 Renamed");
    }

    @Test
    @DisplayName("updateCycle rejects edits to a non-PLANNED cycle")
    void updateCycle_notPlanned() {
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(openCycle));

        UpdateAssessmentCycleRequest req = new UpdateAssessmentCycleRequest(
                "Name", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, null);

        assertThatThrownBy(() -> service.updateCycle("pub-cycle-2", req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------
    // Reopen (CLOSED -> OPEN, M4 stabilization)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can reopen a valid CLOSED cycle whose window still includes today")
    void reopenCycle_validClosedCycle_succeeds() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("Closed accidentally during validation", null);

        var dto = service.reopenCycle("pub-cycle-closed", req, "admin");

        assertThat(dto).isNotNull();
    }

    @Test
    @DisplayName("reopenCycle results in status OPEN")
    void reopenCycle_resultingStatusIsOpen() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        LookupValue openStatus = LookupValue.builder().id(2L).valueCode("OPEN").valueLabel("Open").active(true).build();
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.of(openStatus));
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("Reopening for continued testing", null);

        var dto = service.reopenCycle("pub-cycle-closed", req, "admin");

        assertThat(dto.statusCode()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("reopenCycle requires a non-blank reason")
    void reopenCycle_reasonRequired() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));

        ReopenAssessmentCycleRequest blankReason = new ReopenAssessmentCycleRequest("   ", null);

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-closed", blankReason, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("OPEN cycle cannot be reopened")
    void reopenCycle_openCycleRejected() {
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(openCycle));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-2", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("PLANNED cycle cannot be reopened")
    void reopenCycle_plannedCycleRejected() {
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(plannedCycle));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-1", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Expired CLOSED cycle without a new end date is rejected")
    void reopenCycle_expiredWithoutNewEndDate_rejected() {
        when(cycleRepository.findByPublicId("pub-cycle-closed-expired")).thenReturn(Optional.of(closedExpiredCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(4L)).thenReturn(0L);

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-closed-expired", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Expired CLOSED cycle with a valid future new end date can reopen")
    void reopenCycle_expiredWithValidNewEndDate_succeeds() {
        when(cycleRepository.findByPublicId("pub-cycle-closed-expired")).thenReturn(Optional.of(closedExpiredCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(4L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("Extending for remaining technicians", FIXED_TODAY.plusDays(30));

        var dto = service.reopenCycle("pub-cycle-closed-expired", req, "admin");

        assertThat(dto.cycleEndDate()).isEqualTo(FIXED_TODAY.plusDays(30));
    }

    @Test
    @DisplayName("Invalid new end date (before today) is rejected")
    void reopenCycle_invalidNewEndDateBeforeToday_rejected() {
        when(cycleRepository.findByPublicId("pub-cycle-closed-expired")).thenReturn(Optional.of(closedExpiredCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(4L)).thenReturn(0L);

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", FIXED_TODAY.minusDays(1));

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-closed-expired", req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("reopenCycle never deletes or regenerates technician assessment items")
    void reopenCycle_neverTouchesAssessmentItems() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        service.reopenCycle("pub-cycle-closed", req, "admin");

        // reopenCycle only touches the AssessmentCycle repository — it must never
        // reach into header/item repositories to delete or regenerate assessment data.
        verifyNoInteractions(headerRepository);
        verifyNoInteractions(expectedRatingRepository);
    }

    @Test
    @DisplayName("Application remains unchanged after reopen")
    void reopenCycle_applicationUnchanged() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        var dto = service.reopenCycle("pub-cycle-closed", req, "admin");

        assertThat(dto.applicationPublicId()).isEqualTo("pub-app-atlas");
    }

    @Test
    @DisplayName("Requirement Version remains unchanged after reopen")
    void reopenCycle_requirementVersionUnchanged() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        var dto = service.reopenCycle("pub-cycle-closed", req, "admin");

        assertThat(dto.requirementVersionPublicId()).isEqualTo("pub-rv-1");
    }

    @Test
    @DisplayName("Application/Requirement Version cannot be edited (via updateCycle) once technician headers exist, even after a reopen")
    void updateCycle_applicationVersionLockedAfterReopenAndHeaderExists() {
        // Reopened cycles are OPEN again, and updateCycle only permits edits while PLANNED anyway —
        // this test documents that the existing lock is unaffected by the reopen feature: an OPEN
        // cycle (whether freshly opened or reopened) can never have its Application/Requirement
        // Version changed via updateCycle, regardless of header existence.
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(
                AssessmentCycle.builder().id(3L).publicId("pub-cycle-closed")
                        .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Q2 2026 Assessment")
                        .cycleStartDate(FIXED_TODAY.minusDays(1)).cycleEndDate(FIXED_TODAY.plusDays(30))
                        .statusLookup(LookupValue.builder().id(2L).valueCode("OPEN").valueLabel("Open").active(true).build())
                        .active(true).build()));

        UpdateAssessmentCycleRequest req = new UpdateAssessmentCycleRequest(
                "New Name", FIXED_TODAY, FIXED_TODAY.plusDays(30), "pub-app-other", null);

        assertThatThrownBy(() -> service.updateCycle("pub-cycle-closed", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("reopenCycle is rejected when downstream finalized approval records exist for the cycle")
    void reopenCycle_rejectedWhenDownstreamApprovalsExist() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(1L);

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("reason", null);

        assertThatThrownBy(() -> service.reopenCycle("pub-cycle-closed", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("downstream");
    }

    @Test
    @DisplayName("Audit event ASSESSMENT_CYCLE_REOPENED is generated on successful reopen")
    void reopenCycle_auditEventGenerated() {
        when(cycleRepository.findByPublicId("pub-cycle-closed")).thenReturn(Optional.of(closedCycle));
        when(cycleRepository.countFinalizedApprovalsForCycle(3L)).thenReturn(0L);
        when(lookupValueRepository.findByTypeCodeAndValueCode(any(), eq("OPEN"))).thenReturn(Optional.empty());
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenAssessmentCycleRequest req = new ReopenAssessmentCycleRequest("Closed accidentally", null);

        service.reopenCycle("pub-cycle-closed", req, "admin");

        verify(auditService).log(eq("ASSESSMENT_CYCLE_REOPENED"), any(), any(), any(), any(), any(), any(), any());
    }
}
