package com.skillmatrix.applications.service;

import com.skillmatrix.applications.dto.ApplicationDto;
import com.skillmatrix.applications.dto.ApplicationTypeDto;
import com.skillmatrix.applications.dto.BundleDto;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationTypeRepository applicationTypeRepository;
    private final BundleRepository bundleRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public List<ApplicationTypeDto> getAllActiveApplicationTypes() {
        return applicationTypeRepository.findAllByActiveTrue()
                .stream()
                .map(ApplicationTypeDto::from)
                .toList();
    }

    public List<BundleDto> getAllActiveBundles() {
        return bundleRepository.findAllByActiveTrue()
                .stream()
                .map(BundleDto::from)
                .toList();
    }

    /**
     * Returns active applications filtered by typeCode and/or bundleCode.
     * Either filter can be null/blank to skip it.
     */
    public List<ApplicationDto> getFilteredApplications(String typeCode, String bundleCode) {
        String tCode = (typeCode == null || typeCode.isBlank()) ? null : typeCode.trim();
        String bCode = (bundleCode == null || bundleCode.isBlank()) ? null : bundleCode.trim();
        return applicationRepository.findActiveByTypeCodeAndBundleCode(tCode, bCode)
                .stream()
                .map(ApplicationDto::from)
                .toList();
    }

    /**
     * Returns applications assigned to the currently authenticated technician.
     */
    public List<ApplicationDto> getMyApplications(UserPrincipal principal) {
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getUsername()));
        return applicationRepository.findActiveApplicationsByUserId(user.getId())
                .stream()
                .map(ApplicationDto::from)
                .toList();
    }

    /**
     * Returns applications visible to a Lead Manager based on team membership.
     */
    public List<ApplicationDto> getLeadApplications(UserPrincipal principal) {
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getUsername()));
        return applicationRepository.findApplicationsByLeadTeamMembership(user.getId())
                .stream()
                .map(ApplicationDto::from)
                .toList();
    }
}
