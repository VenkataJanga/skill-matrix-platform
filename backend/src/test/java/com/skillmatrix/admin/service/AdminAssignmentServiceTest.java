package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.AssignmentStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.ApplicationPortfolio;
import com.skillmatrix.domain.entity.ApplicationType;
import com.skillmatrix.domain.entity.Bundle;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserApplicationMapping;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAssignmentService unit tests")
class AdminAssignmentServiceTest {

    @Mock UserApplicationMappingRepository mappingRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock AuditService auditService;

    @InjectMocks AdminAssignmentService service;

    private User techUser;
    private Application atlasApp;
    private UserApplicationMapping activeMapping;

    @BeforeEach
    void setUp() {
        Role techRole = Role.builder()
                .id(2L).publicId(UUID.randomUUID().toString())
                .roleCode("TECHNICIAN").roleName("Technician").active(true).build();

        techUser = User.builder()
                .id(6L).publicId("pub-tech-1")
                .username("deepak_mishra").email("deepak@nttdata.com")
                .fullName("Deepak Mishra").passwordHash("$hash")
                .primaryRole(techRole).active(true).userRoles(new HashSet<>())
                .build();

        ApplicationType webType = ApplicationType.builder()
                .id(1L).typeCode("WEB").typeName("Web Application").active(true).build();
        Bundle b06 = Bundle.builder()
                .id(1L).bundleCode("B06").bundleName("Bundle 06").active(true).build();
        ApplicationPortfolio portfolio = ApplicationPortfolio.builder()
                .id(1L).applicationType(webType).bundle(b06)
                .portfolioCode("B06-WEB").portfolioName("B06 Web").active(true).build();

        atlasApp = Application.builder()
                .id(1L).publicId("pub-atlas")
                .applicationCode("ATLAS").applicationName("ATLAS-deZentral")
                .portfolio(portfolio).active(true).build();

        activeMapping = UserApplicationMapping.builder()
                .id(1L).publicId("pub-map-1")
                .user(techUser).application(atlasApp)
                .allocationPercentage(BigDecimal.valueOf(100))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .active(true).build();
    }

    // ---------------------------------------------------------------
    // Duplicate check
    // ---------------------------------------------------------------

    @Test
    @DisplayName("checkDuplicate returns isDuplicate=true when active mapping exists")
    void checkDuplicate_exists() {
        when(mappingRepository.findActiveByUserPublicIdAndAppPublicId("pub-tech-1", "pub-atlas"))
                .thenReturn(Optional.of(activeMapping));

        DuplicateCheckResponse resp = service.checkDuplicate("pub-tech-1", "pub-atlas");

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.existingAssignmentPublicId()).isEqualTo("pub-map-1");
    }

    @Test
    @DisplayName("checkDuplicate returns isDuplicate=false when no mapping exists")
    void checkDuplicate_notExists() {
        when(mappingRepository.findActiveByUserPublicIdAndAppPublicId("pub-tech-1", "pub-atlas"))
                .thenReturn(Optional.empty());

        DuplicateCheckResponse resp = service.checkDuplicate("pub-tech-1", "pub-atlas");

        assertThat(resp.isDuplicate()).isFalse();
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createAssignment succeeds when no duplicate exists")
    void createAssignment_success() {
        AssignmentRequest req = new AssignmentRequest(
                "pub-tech-1", "pub-atlas",
                BigDecimal.valueOf(100), LocalDate.of(2026, 1, 1), null);

        when(userRepository.findByPublicId("pub-tech-1")).thenReturn(Optional.of(techUser));
        when(applicationRepository.findByPublicId("pub-atlas")).thenReturn(Optional.of(atlasApp));
        when(mappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(6L, 1L)).thenReturn(false);
        when(mappingRepository.save(any())).thenAnswer(inv -> {
            UserApplicationMapping m = inv.getArgument(0);
            m.setId(99L);
            m.setPublicId(UUID.randomUUID().toString());
            return m;
        });

        ApplicationAssignmentDto dto = service.createAssignment(req, "admin");

        assertThat(dto.username()).isEqualTo("deepak_mishra");
        assertThat(dto.applicationCode()).isEqualTo("ATLAS");
        verify(auditService).log(eq("CREATE_ASSIGNMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createAssignment rejects duplicate with HTTP 409")
    void createAssignment_duplicateRejected() {
        AssignmentRequest req = new AssignmentRequest(
                "pub-tech-1", "pub-atlas",
                BigDecimal.valueOf(100), LocalDate.now(), null);

        when(userRepository.findByPublicId("pub-tech-1")).thenReturn(Optional.of(techUser));
        when(applicationRepository.findByPublicId("pub-atlas")).thenReturn(Optional.of(atlasApp));
        when(mappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(6L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createAssignment(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Status update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateAssignmentStatus deactivates and sets effectiveTo")
    void updateAssignmentStatus_deactivate() {
        when(mappingRepository.findByPublicId("pub-map-1")).thenReturn(Optional.of(activeMapping));
        when(mappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationAssignmentDto dto = service.updateAssignmentStatus(
                "pub-map-1", new AssignmentStatusRequest(false, "Removed from project"), "admin");

        assertThat(dto.active()).isFalse();
        assertThat(activeMapping.getEffectiveTo()).isEqualTo(LocalDate.now());
    }

    // ---------------------------------------------------------------
    // Bulk status update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("bulkUpdateStatus processes all valid IDs")
    void bulkUpdateStatus_allSuccess() {
        UserApplicationMapping m2 = UserApplicationMapping.builder()
                .id(2L).publicId("pub-map-2")
                .user(techUser).application(atlasApp)
                .allocationPercentage(BigDecimal.valueOf(80))
                .effectiveFrom(LocalDate.now())
                .active(true).build();

        when(mappingRepository.findByPublicId("pub-map-1")).thenReturn(Optional.of(activeMapping));
        when(mappingRepository.findByPublicId("pub-map-2")).thenReturn(Optional.of(m2));
        when(mappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BulkStatusResponse resp = service.bulkUpdateStatus(
                new BulkStatusRequest(List.of("pub-map-1", "pub-map-2"), false, "Bulk remove"),
                "admin");

        assertThat(resp.requestedCount()).isEqualTo(2);
        assertThat(resp.successCount()).isEqualTo(2);
        assertThat(resp.failedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateStatus reports partial failure for missing IDs")
    void bulkUpdateStatus_partialFailure() {
        when(mappingRepository.findByPublicId("pub-map-1")).thenReturn(Optional.of(activeMapping));
        when(mappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mappingRepository.findByPublicId("bad-id")).thenReturn(Optional.empty());

        BulkStatusResponse resp = service.bulkUpdateStatus(
                new BulkStatusRequest(List.of("pub-map-1", "bad-id"), false, "Bulk remove"),
                "admin");

        assertThat(resp.requestedCount()).isEqualTo(2);
        assertThat(resp.successCount()).isEqualTo(1);
        assertThat(resp.failedCount()).isEqualTo(1);
        assertThat(resp.failures()).hasSize(1);
        assertThat(resp.failures().get(0).publicId()).isEqualTo("bad-id");
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getAssignments returns paginated results")
    void getAssignments_paged() {
        Page<UserApplicationMapping> page = new PageImpl<>(List.of(activeMapping));
        when(mappingRepository.searchAssignments(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ApplicationAssignmentDto> result = service.getAssignments(
                0, 10, null, null, null, null, null, "ACTIVE", "id", "asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).applicationCode()).isEqualTo("ATLAS");
    }
}
