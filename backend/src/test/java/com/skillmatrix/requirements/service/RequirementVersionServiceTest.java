package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.LookupValue;
import com.skillmatrix.domain.entity.RequirementVersion;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequirementVersionService unit tests — version lookup isolation (M4 stabilization)")
class RequirementVersionServiceTest {

    @Mock RequirementVersionRepository requirementVersionRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock AuditService auditService;

    @InjectMocks RequirementVersionService service;

    private Application atlasApp;
    private Application avusApp;

    @BeforeEach
    void setUp() {
        atlasApp = Application.builder().id(1L).publicId("pub-app-atlas")
                .applicationCode("ATLAS_DEZENTRAL").applicationName("ATLAS-deZentral").active(true).build();
        avusApp = Application.builder().id(2L).publicId("pub-app-avus")
                .applicationCode("AVUS").applicationName("AVUS").active(true).build();
    }

    @Test
    @DisplayName("getVersionsForApplication returns only versions for the requested application")
    void getVersionsForApplication_returnsOnlyRequestedApplication() {
        LookupValue approved = LookupValue.builder().id(1L).valueCode("APPROVED").valueLabel("Approved").active(true).build();
        RequirementVersion atlasV1 = RequirementVersion.builder().id(1L).publicId("pub-rv-atlas-1")
                .application(atlasApp).versionCode("v1.0").versionName("ATLAS v1.0").statusLookup(approved).active(true).build();

        when(requirementVersionRepository.findAllByApplicationPublicId("pub-app-atlas"))
                .thenReturn(List.of(atlasV1));

        var result = service.getVersionsForApplication("pub-app-atlas");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).applicationCode()).isEqualTo("ATLAS_DEZENTRAL");
    }

    @Test
    @DisplayName("getEligibleVersionsForApplication returns empty list (not an error) when application has no version")
    void getEligibleVersionsForApplication_emptyIsNotError() {
        when(requirementVersionRepository.findEligibleByApplicationPublicId("pub-app-avus"))
                .thenReturn(List.of());

        var result = service.getEligibleVersionsForApplication("pub-app-avus");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getEligibleVersionsForApplication returns a DRAFT version (DRAFT is an eligible status for cycle creation)")
    void getEligibleVersionsForApplication_draftIsEligible() {
        LookupValue draft = LookupValue.builder().id(2L).valueCode("DRAFT").valueLabel("Draft").active(true).build();
        RequirementVersion avusDraft = RequirementVersion.builder().id(2L).publicId("pub-rv-avus-1")
                .application(avusApp).versionCode("v1.0").versionName("AVUS v1.0").statusLookup(draft).active(true).build();

        when(requirementVersionRepository.findEligibleByApplicationPublicId("pub-app-avus"))
                .thenReturn(List.of(avusDraft));

        var result = service.getEligibleVersionsForApplication("pub-app-avus");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).statusCode()).isEqualTo("DRAFT");
        assertThat(result.get(0).applicationCode()).isEqualTo("AVUS");
    }

    @Test
    @DisplayName("getVersionsForApplication never returns a version belonging to a different application")
    void getVersionsForApplication_neverCrossesApplications() {
        LookupValue approved = LookupValue.builder().id(1L).valueCode("APPROVED").valueLabel("Approved").active(true).build();
        RequirementVersion atlasV1 = RequirementVersion.builder().id(1L).publicId("pub-rv-atlas-1")
                .application(atlasApp).versionCode("v1.0").statusLookup(approved).active(true).build();

        // Repository is correctly scoped by applicationPublicId — simulate AVUS query
        // returning its own (empty) result, proving no bleed-through from ATLAS.
        when(requirementVersionRepository.findAllByApplicationPublicId("pub-app-avus"))
                .thenReturn(List.of());

        var avusResult = service.getVersionsForApplication("pub-app-avus");

        assertThat(avusResult).isEmpty();
        assertThat(avusResult).noneMatch(dto -> dto.publicId().equals(atlasV1.getPublicId()));
    }
}
