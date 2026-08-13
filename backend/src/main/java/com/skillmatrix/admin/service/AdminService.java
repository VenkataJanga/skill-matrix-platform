package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.UserSummaryDto;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.UserApplicationMapping;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final UserApplicationMappingRepository mappingRepository;

    // ---------------------------------------------------------------
    // User queries
    // ---------------------------------------------------------------

    public List<UserSummaryDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserSummaryDto::from)
                .toList();
    }

    public List<UserSummaryDto> getAllTechnicians() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserRoles() != null &&
                        u.getUserRoles().stream()
                                .anyMatch(ur -> ur.isActive() &&
                                        "TECHNICIAN".equals(ur.getRole().getRoleCode())))
                .map(UserSummaryDto::from)
                .toList();
    }

    // ---------------------------------------------------------------
    // Assignment queries
    // ---------------------------------------------------------------

    public List<ApplicationAssignmentDto> getAllAssignments() {
        return mappingRepository.findAllActiveMappingsWithDetails().stream()
                .map(ApplicationAssignmentDto::from)
                .toList();
    }

    // ---------------------------------------------------------------
    // Assignment mutations
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationAssignmentDto createAssignment(AssignmentRequest request) {
        var user = userRepository.findByPublicId(request.userPublicId())
                .orElseThrow(() -> new BusinessException("User not found: " + request.userPublicId()));

        var application = applicationRepository.findByPublicId(request.applicationPublicId())
                .orElseThrow(() -> new BusinessException("Application not found: " + request.applicationPublicId()));

        if (mappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(user.getId(), application.getId())) {
            throw new BusinessException("Active assignment already exists for this user and application.");
        }

        UserApplicationMapping mapping = UserApplicationMapping.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .application(application)
                .allocationPercentage(request.allocationPercentage())
                .effectiveFrom(request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now())
                .effectiveTo(request.effectiveTo())
                .active(true)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        return ApplicationAssignmentDto.from(mappingRepository.save(mapping));
    }

    @Transactional
    public ApplicationAssignmentDto updateAssignment(String publicId, AssignmentRequest request) {
        UserApplicationMapping mapping = mappingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Assignment not found: " + publicId));

        mapping.setAllocationPercentage(request.allocationPercentage());
        mapping.setEffectiveFrom(request.effectiveFrom());
        mapping.setEffectiveTo(request.effectiveTo());
        mapping.setUpdatedBy("admin");

        return ApplicationAssignmentDto.from(mappingRepository.save(mapping));
    }

    @Transactional
    public ApplicationAssignmentDto deactivateAssignment(String publicId) {
        UserApplicationMapping mapping = mappingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Assignment not found: " + publicId));

        mapping.setActive(false);
        mapping.setEffectiveTo(LocalDate.now());
        mapping.setUpdatedBy("admin");

        return ApplicationAssignmentDto.from(mappingRepository.save(mapping));
    }
}
