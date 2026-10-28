package com.skillmatrix.assessments.service;

import com.skillmatrix.assessments.dto.SaveDraftRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicianAssessmentService unit tests")
class TechnicianAssessmentServiceTest {

    @Mock AssessmentCycleRepository cycleRepository;
    @Mock TechnicianAssessmentHeaderRepository headerRepository;
    @Mock AssessmentItemRepository itemRepository;
    @Mock ExpectedRatingRepository expectedRatingRepository;
    @Mock UserApplicationMappingRepository userApplicationMappingRepository;
    @Mock UserRepository userRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock AuditService auditService;

    /**
     * Fixed clock at 2026-02-15 — falls WITHIN the default openCycle/closedCycle
     * window (2026-01-01 to 2026-03-31) used by all the pre-existing tests below,
     * so those tests remain valid without depending on the real system date.
     * Date-window-specific tests override this via separate AssessmentCycle
     * instances with start/end dates relative to this fixed "today".
     */
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 2, 15);

    @Mock Clock clock;

    @InjectMocks TechnicianAssessmentService service;

    private User technician;
    private User otherTechnician;
    private Application atlasApp;
    private RequirementVersion rv;
    private AssessmentCycle openCycle;
    private AssessmentCycle closedCycle;
    private Skill javaSkill;
    private ExpectedRating er;

    @BeforeEach
    void setUp() {
        // Stub the fixed clock as lenient-by-usage: most tests call at least one
        // validation path that reads LocalDate.now(clock).
        Instant fixedInstant = FIXED_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        technician = User.builder().id(1L).publicId("pub-tech-1").username("tech_john")
                .passwordHash("x").email("john@x.com").fullName("John").active(true).build();
        otherTechnician = User.builder().id(2L).publicId("pub-tech-2").username("tech_jane")
                .passwordHash("x").email("jane@x.com").fullName("Jane").active(true).build();

        atlasApp = Application.builder().id(1L).publicId("pub-app-atlas")
                .applicationCode("ATLAS_DEZENTRAL").applicationName("ATLAS-deZentral").active(true).build();
        rv = RequirementVersion.builder().id(1L).publicId("pub-rv-1")
                .application(atlasApp).versionCode("v1.0").active(true).build();

        LookupValue openStatus = LookupValue.builder().id(1L).valueCode("OPEN").valueLabel("Open").active(true).build();
        LookupValue closedStatus = LookupValue.builder().id(2L).valueCode("CLOSED").valueLabel("Closed").active(true).build();

        openCycle = AssessmentCycle.builder().id(1L).publicId("pub-cycle-1")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS FY26")
                .cycleStartDate(LocalDate.of(2026, 1, 1)).cycleEndDate(LocalDate.of(2026, 3, 31))
                .statusLookup(openStatus).active(true).build();

        closedCycle = AssessmentCycle.builder().id(2L).publicId("pub-cycle-2")
                .application(atlasApp).requirementVersion(rv).cycleName("ATLAS Closed")
                .cycleStartDate(LocalDate.of(2026, 1, 1)).cycleEndDate(LocalDate.of(2026, 3, 31))
                .statusLookup(closedStatus).active(true).build();

        javaSkill = Skill.builder().id(1L).publicId("pub-skill-java")
                .skillCode("TECH_JAVA").skillName("Java").active(true).build();
        er = ExpectedRating.builder().id(1L).publicId("pub-er-1")
                .requirementVersion(rv).skill(javaSkill).expectedLevel(4).mandatory(true).active(true).build();
    }

    // ---------------------------------------------------------------
    // Start assessment
    // ---------------------------------------------------------------

    @Test
    @DisplayName("startAssessment creates header + items when none exists")
    void startAssessment_createsNew() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(openCycle));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(headerRepository.findByCycle_IdAndUser_Id(1L, 1L)).thenReturn(Optional.empty());
        when(headerRepository.save(any())).thenAnswer(inv -> {
            TechnicianAssessmentHeader h = inv.getArgument(0);
            h.setId(100L);
            return h;
        });
        when(expectedRatingRepository.findAllByRequirementVersionId(1L)).thenReturn(List.of(er));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(
                AssessmentItem.builder().id(1L).publicId("pub-item-1").expectedRating(er).active(true).build()));

        var dto = service.startAssessment("pub-cycle-1", "tech_john");

        assertThat(dto.items()).hasSize(1);
        verify(auditService).log(eq("START_ASSESSMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("startAssessment is idempotent — returns existing header on repeat call")
    void startAssessment_idempotent() {
        TechnicianAssessmentHeader existingHeader = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(openCycle));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(headerRepository.findByCycle_IdAndUser_Id(1L, 1L)).thenReturn(Optional.of(existingHeader));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of());

        service.startAssessment("pub-cycle-1", "tech_john");

        verify(headerRepository, never()).save(any());
        verify(auditService, never()).log(eq("START_ASSESSMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("startAssessment rejects a cycle that is not OPEN")
    void startAssessment_cycleNotOpen() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(closedCycle));

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-2", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("startAssessment rejects a technician not assigned to the application")
    void startAssessment_notAssigned() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(openCycle));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-1", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---------------------------------------------------------------
    // Date-window enforcement (M4 stabilization)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("startAssessment allowed when OPEN cycle date window includes today")
    void startAssessment_withinWindow_allowed() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-1")).thenReturn(Optional.of(openCycle));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(headerRepository.findByCycle_IdAndUser_Id(1L, 1L)).thenReturn(Optional.empty());
        when(headerRepository.save(any())).thenAnswer(inv -> {
            TechnicianAssessmentHeader h = inv.getArgument(0);
            h.setId(100L);
            return h;
        });
        when(expectedRatingRepository.findAllByRequirementVersionId(1L)).thenReturn(List.of(er));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of());

        var dto = service.startAssessment("pub-cycle-1", "tech_john");

        assertThat(dto).isNotNull();
    }

    @Test
    @DisplayName("startAssessment rejected when OPEN cycle has not started yet")
    void startAssessment_beforeStart_rejected() {
        AssessmentCycle futureCycle = AssessmentCycle.builder().id(3L).publicId("pub-cycle-future")
                .application(atlasApp).requirementVersion(rv).cycleName("Future Cycle")
                .cycleStartDate(FIXED_TODAY.plusDays(10)).cycleEndDate(FIXED_TODAY.plusDays(40))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-future")).thenReturn(Optional.of(futureCycle));

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-future", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("not started yet");
    }

    @Test
    @DisplayName("startAssessment rejected when OPEN cycle end date has passed")
    void startAssessment_afterEnd_rejected() {
        AssessmentCycle expiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("Expired Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(60)).cycleEndDate(FIXED_TODAY.minusDays(1))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-expired")).thenReturn(Optional.of(expiredCycle));

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-expired", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("ended");
    }

    @Test
    @DisplayName("saveDraft allowed when cycle date window includes today")
    void saveDraft_withinWindow_allowed() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of());
        when(headerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of());

        var dto = service.saveDraft("pub-header-1", req, "tech_john");

        assertThat(dto).isNotNull();
    }

    @Test
    @DisplayName("saveDraft rejected when cycle end date has passed")
    void saveDraft_afterEnd_rejected() {
        AssessmentCycle expiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("Expired Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(60)).cycleEndDate(FIXED_TODAY.minusDays(1))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(expiredCycle).user(technician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of());

        assertThatThrownBy(() -> service.saveDraft("pub-header-1", req, "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("ended");
        verify(headerRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit allowed when cycle date window includes today and mandatory ratings complete")
    void submit_withinWindow_allowed() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();
        AssessmentItem ratedItem = AssessmentItem.builder().id(1L).publicId("pub-item-1")
                .header(header).expectedRating(er).selfRating(4).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(ratedItem));
        when(headerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.submit("pub-header-1", "tech_john");

        assertThat(dto).isNotNull();
    }

    @Test
    @DisplayName("submit rejected when cycle end date has passed")
    void submit_afterEnd_rejected() {
        AssessmentCycle expiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("Expired Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(60)).cycleEndDate(FIXED_TODAY.minusDays(1))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(expiredCycle).user(technician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        assertThatThrownBy(() -> service.submit("pub-header-1", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("ended");
        verify(headerRepository, never()).save(any());
    }

    @Test
    @DisplayName("CLOSED cycle remains rejected for start regardless of dates")
    void startAssessment_closedCycle_rejectedRegardlessOfDates() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-2")).thenReturn(Optional.of(closedCycle));

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-2", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("not currently open");
    }

    @Test
    @DisplayName("PLANNED cycle remains rejected for start regardless of dates")
    void startAssessment_plannedCycle_rejectedRegardlessOfDates() {
        LookupValue plannedStatus = LookupValue.builder().id(5L).valueCode("PLANNED").valueLabel("Planned").active(true).build();
        AssessmentCycle plannedCycle = AssessmentCycle.builder().id(5L).publicId("pub-cycle-planned")
                .application(atlasApp).requirementVersion(rv).cycleName("Planned Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(5)).cycleEndDate(FIXED_TODAY.plusDays(5))
                .statusLookup(plannedStatus).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(cycleRepository.findByPublicId("pub-cycle-planned")).thenReturn(Optional.of(plannedCycle));

        assertThatThrownBy(() -> service.startAssessment("pub-cycle-planned", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("not currently open");
    }

    @Test
    @DisplayName("submitted assessment remains readable after cycle expiry")
    void getAssessment_readableAfterExpiry() {
        AssessmentCycle expiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("Expired Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(60)).cycleEndDate(FIXED_TODAY.minusDays(1))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();
        LookupValue submittedStatus = LookupValue.builder().id(6L).valueCode("SUBMITTED").valueLabel("Submitted").active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(expiredCycle).user(technician)
                .statusLookup(submittedStatus).version(1L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of());

        var dto = service.getAssessment("pub-header-1", "tech_john");

        assertThat(dto).isNotNull();
        assertThat(dto.statusCode()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("DRAFT remains stored after expiry but getAssessment still returns it read-only at the controller/UI layer")
    void getAssessment_draftReadableAfterExpiry() {
        AssessmentCycle expiredCycle = AssessmentCycle.builder().id(4L).publicId("pub-cycle-expired")
                .application(atlasApp).requirementVersion(rv).cycleName("Expired Cycle")
                .cycleStartDate(FIXED_TODAY.minusDays(60)).cycleEndDate(FIXED_TODAY.minusDays(1))
                .statusLookup(openCycle.getStatusLookup()).active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(expiredCycle).user(technician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of());

        var dto = service.getAssessment("pub-header-1", "tech_john");

        assertThat(dto).isNotNull();
    }

    // ---------------------------------------------------------------
    // IDOR protection
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getAssessment blocks access to another technician's assessment (IDOR)")
    void getAssessment_blocksCrossUserAccess() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(otherTechnician).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        assertThatThrownBy(() -> service.getAssessment("pub-header-1", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // Save draft (business validations not related to date window)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("saveDraft persists partial ratings")
    void saveDraft_success() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();
        AssessmentItem item = AssessmentItem.builder().id(1L).publicId("pub-item-1").header(header)
                .expectedRating(er).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(item));
        when(headerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of(
                new SaveDraftRequest.ItemRatingRequest("pub-item-1", 3, "comment", "evidence")));

        var dto = service.saveDraft("pub-header-1", req, "tech_john");

        assertThat(dto).isNotNull();
        verify(itemRepository).saveAll(any());
    }

    @Test
    @DisplayName("saveDraft rejects rating outside 1-5")
    void saveDraft_invalidRating() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();
        AssessmentItem item = AssessmentItem.builder().id(1L).publicId("pub-item-1").header(header)
                .expectedRating(er).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(item));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of(
                new SaveDraftRequest.ItemRatingRequest("pub-item-1", 9, null, null)));

        assertThatThrownBy(() -> service.saveDraft("pub-header-1", req, "tech_john"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("saveDraft blocks another technician's draft (IDOR)")
    void saveDraft_blocksCrossUserAccess() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(otherTechnician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of());

        assertThatThrownBy(() -> service.saveDraft("pub-header-1", req, "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("saveDraft rejects stale version (optimistic lock conflict)")
    void saveDraft_staleVersionConflict() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(2L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of()); // stale version

        assertThatThrownBy(() -> service.saveDraft("pub-header-1", req, "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("saveDraft rejects when assessment already submitted")
    void saveDraft_alreadySubmitted() {
        LookupValue submitted = LookupValue.builder().id(3L).valueCode("SUBMITTED").valueLabel("Submitted").active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician)
                .statusLookup(submitted).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of());

        assertThatThrownBy(() -> service.saveDraft("pub-header-1", req, "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Submit (business validations not related to date window)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("submit blocks when a mandatory skill is unrated")
    void submit_missingMandatoryRating() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();
        AssessmentItem unratedMandatory = AssessmentItem.builder().id(1L).publicId("pub-item-1")
                .header(header).expectedRating(er).selfRating(null).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(unratedMandatory));

        assertThatThrownBy(() -> service.submit("pub-header-1", "tech_john"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("submit succeeds when all mandatory skills are rated")
    void submit_success() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician).version(0L).build();
        AssessmentItem ratedItem = AssessmentItem.builder().id(1L).publicId("pub-item-1")
                .header(header).expectedRating(er).selfRating(4).active(true).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));
        when(userApplicationMappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(1L, 1L)).thenReturn(true);
        when(itemRepository.findAllByHeaderId(100L)).thenReturn(List.of(ratedItem));
        when(headerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.submit("pub-header-1", "tech_john");

        assertThat(dto).isNotNull();
        verify(auditService).log(eq("SUBMIT_ASSESSMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("submit rejects repeat submission of an already-submitted assessment")
    void submit_rejectsRepeatSubmission() {
        LookupValue submitted = LookupValue.builder().id(3L).valueCode("SUBMITTED").valueLabel("Submitted").active(true).build();
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(openCycle).user(technician)
                .statusLookup(submitted).version(1L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        assertThatThrownBy(() -> service.submit("pub-header-1", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("submit rejects submission when cycle is not OPEN")
    void submit_cycleNotOpen() {
        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .id(100L).publicId("pub-header-1").cycle(closedCycle).user(technician).version(0L).build();

        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(headerRepository.findDetailByPublicId("pub-header-1")).thenReturn(Optional.of(header));

        assertThatThrownBy(() -> service.submit("pub-header-1", "tech_john"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // My Assessments list
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getMyAssessments returns NOT_STARTED for eligible cycle with no header")
    void getMyAssessments_notStarted() {
        when(userRepository.findByUsername("tech_john")).thenReturn(Optional.of(technician));
        when(userApplicationMappingRepository.findByUser_IdAndActiveTrue(1L)).thenReturn(List.of(
                UserApplicationMapping.builder().id(1L).user(technician).application(atlasApp).active(true).build()));
        when(cycleRepository.findAllByApplicationIds(List.of(1L))).thenReturn(List.of(openCycle));
        when(headerRepository.findAllByUserId(1L)).thenReturn(List.of());

        var result = service.getMyAssessments("tech_john");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).assessmentStatus()).isEqualTo("NOT_STARTED");
    }
}
