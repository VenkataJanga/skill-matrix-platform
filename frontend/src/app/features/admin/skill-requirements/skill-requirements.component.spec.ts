import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { SkillRequirementsComponent } from './skill-requirements.component';
import { RequirementService } from '../../../core/services/requirement.service';
import { ApplicationService } from '../../../core/services/application.service';
import { SkillService } from '../../../core/services/skill.service';
import { PagedResponse } from '../../../core/models/application.model';
import {
  RequirementValidationResult,
  RequirementVersion,
  SkillRequirement,
} from '../../../core/models/requirement.model';

describe('SkillRequirementsComponent', () => {
  let fixture: ComponentFixture<SkillRequirementsComponent>;
  let component: SkillRequirementsComponent;
  let reqSvc: jasmine.SpyObj<RequirementService>;
  let appSvc: jasmine.SpyObj<ApplicationService>;
  let skillSvc: jasmine.SpyObj<SkillService>;

  const mockVersion: RequirementVersion = {
    publicId: 'pub-rv-1', applicationPublicId: 'pub-app-atlas',
    applicationCode: 'ATLAS_DEZENTRAL', applicationName: 'ATLAS-deZentral',
    versionCode: 'v1.0', versionName: 'Draft v1', description: null,
    statusCode: 'DRAFT', statusLabel: 'Draft', publishedAt: null,
    approvedAt: null, approvedBy: null, active: true,
    createdAt: '', updatedAt: '', createdBy: null, updatedBy: null,
    activeRequirementCount: 1,
  };

  const mockRequirement: SkillRequirement = {
    publicId: 'pub-er-1', requirementVersionPublicId: 'pub-rv-1', versionCode: 'v1.0',
    applicationPublicId: 'pub-app-atlas', applicationCode: 'ATLAS_DEZENTRAL',
    applicationName: 'ATLAS-deZentral', skillPublicId: 'pub-skill-java',
    skillCode: 'TECH_JAVA', skillName: 'Java', skillGroup: 'COMMON_TECH',
    categoryCode: 'TECHNICAL', categoryName: 'Technical Skills',
    expectedLevel: 3, mandatory: true, criticalityCode: null, criticalityLabel: null,
    roleCode: null, roleLabel: null, gradeCode: null, gradeLabel: null,
    active: true, createdAt: '', updatedAt: '', createdBy: null, updatedBy: null,
  };

  const pagedReq: PagedResponse<SkillRequirement> = {
    content: [mockRequirement], page: 0, size: 20,
    totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
  };

  beforeEach(async () => {
    reqSvc = jasmine.createSpyObj('RequirementService', [
      'getVersionsForApplication', 'createVersion', 'getRequirementsPaged',
      'createRequirement', 'updateRequirement', 'updateStatus',
      'downloadTemplate', 'validateImport', 'confirmImport',
    ]);
    appSvc = jasmine.createSpyObj('ApplicationService', [
      'getApplicationTypes', 'getBundles', 'getApplications',
    ]);
    skillSvc = jasmine.createSpyObj('SkillService', ['getSkillsPaged']);

    appSvc.getApplicationTypes.and.returnValue(of([]));
    appSvc.getBundles.and.returnValue(of([]));
    appSvc.getApplications.and.returnValue(of([]));
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));
    reqSvc.getRequirementsPaged.and.returnValue(of(pagedReq));

    await TestBed.configureTestingModule({
      imports: [SkillRequirementsComponent, FormsModule],
      providers: [
        { provide: RequirementService, useValue: reqSvc },
        { provide: ApplicationService, useValue: appSvc },
        { provide: SkillService, useValue: skillSvc },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SkillRequirementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load application types and bundles on init', () => {
    expect(component).toBeTruthy();
    expect(appSvc.getApplicationTypes).toHaveBeenCalled();
    expect(appSvc.getBundles).toHaveBeenCalled();
  });

  it('should load applications when type is selected', () => {
    component.selectedTypeCode = 'WEB';
    component.onTypeChange();
    expect(appSvc.getApplications).toHaveBeenCalledWith('WEB', undefined);
  });

  it('should load applications when bundle is selected', () => {
    component.selectedBundleCode = 'B06';
    component.onBundleChange();
    expect(appSvc.getApplications).toHaveBeenCalledWith(undefined, 'B06');
  });

  it('should load versions and requirements when application is selected', () => {
    component.selectedApplicationPublicId = 'pub-app-atlas';
    component.onApplicationChange();
    expect(reqSvc.getVersionsForApplication).toHaveBeenCalledWith('pub-app-atlas');
    expect(component.versions.length).toBe(1);
    expect(component.selectedVersionPublicId).toBe('pub-rv-1');
    expect(reqSvc.getRequirementsPaged).toHaveBeenCalled();
  });

  // ------------------------------------------------------------------
  // Regression tests: Application-change cascade stabilization
  // (skill-requirements "Failed to load skill requirements" defect)
  // ------------------------------------------------------------------

  it('onApplicationChange should clear stale version/requirements/error state immediately, before any async response', () => {
    // Simulate a prior error and stale selection left over from a previous application.
    component.error = 'Failed to load skill requirements.';
    component.selectedVersionPublicId = 'pub-rv-OLD';
    component.pagedRequirements = pagedReq;
    component.versions = [mockVersion];

    // Use a never-resolving observable so we can assert synchronous state
    // changes BEFORE the async response arrives.
    reqSvc.getVersionsForApplication.and.returnValue({ subscribe: () => ({}) } as any);

    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(component.error).toBeNull();
    expect(component.selectedVersionPublicId).toBe('');
    expect(component.versions).toEqual([]);
    expect(component.pagedRequirements).toBeNull();
  });

  it('Requirements API should NOT be called before Requirement Versions API resolves', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));
    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(reqSvc.getVersionsForApplication).toHaveBeenCalledBefore(reqSvc.getRequirementsPaged);
  });

  it('zero Requirement Versions should show "no versions available" state, not an error, and must NOT call Requirements API', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([]));
    reqSvc.getRequirementsPaged.calls.reset();

    component.selectedApplicationPublicId = 'pub-app-novers';
    component.onApplicationChange();

    expect(component.noVersionsAvailable).toBeTrue();
    expect(component.error).toBeNull();
    expect(component.versionsError).toBeNull();
    expect(reqSvc.getRequirementsPaged).not.toHaveBeenCalled();
  });

  it('valid Requirement Version with zero requirements (HTTP 200 empty list) must NOT set error', () => {
    const emptyPage: PagedResponse<SkillRequirement> = {
      content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false,
    };
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));
    reqSvc.getRequirementsPaged.and.returnValue(of(emptyPage));

    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(component.error).toBeNull();
    expect(component.pagedRequirements?.content.length).toBe(0);
  });

  it('Requirements API failure should still show the error banner', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));
    reqSvc.getRequirementsPaged.and.returnValue(throwError(() => ({ status: 500 })));

    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(component.error).toBe('Failed to load skill requirements.');
  });

  it('changing Application after a prior error must clear that error immediately', () => {
    component.error = 'Failed to load skill requirements.';
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));

    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(component.error).toBeNull();
  });

  it('rapid Application switching must not let a stale (out-of-order) response overwrite the latest selection', () => {
    // First selection (ATLAS) — its versions call will resolve LATE.
    let atlasResolve: (versions: RequirementVersion[]) => void;
    const atlasObservable = {
      subscribe: (handlers: any) => {
        atlasResolve = (versions) => handlers.next(versions);
        return {};
      },
    };
    reqSvc.getVersionsForApplication.and.returnValue(atlasObservable as any);
    component.selectedApplicationPublicId = 'pub-app-atlas';
    component.onApplicationChange();

    // Second selection (VTS) — resolves immediately with a different version.
    const vtsVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-vts', applicationPublicId: 'pub-app-vts' };
    reqSvc.getVersionsForApplication.and.returnValue(of([vtsVersion]));
    component.selectedApplicationPublicId = 'pub-app-vts';
    component.onApplicationChange();

    expect(component.selectedVersionPublicId).toBe('pub-rv-vts');

    // Now the stale ATLAS response finally arrives — it must be ignored.
    atlasResolve!([mockVersion]);

    expect(component.selectedVersionPublicId).toBe('pub-rv-vts');
    expect(component.versions).toEqual([vtsVersion]);
  });

  it('load() must never issue an HTTP call when selectedVersionPublicId is empty', () => {
    reqSvc.getRequirementsPaged.calls.reset();
    component.selectedVersionPublicId = '';
    component.load();
    expect(reqSvc.getRequirementsPaged).not.toHaveBeenCalled();
    expect(component.pagedRequirements).toBeNull();
  });

  it('onVersionChange should clear previous error before reloading requirements', () => {
    component.error = 'Failed to load skill requirements.';
    component.selectedVersionPublicId = 'pub-rv-1';
    reqSvc.getRequirementsPaged.and.returnValue(of(pagedReq));

    component.onVersionChange();

    expect(component.error).toBeNull();
    expect(component.pagedRequirements?.content.length).toBe(1);
  });

  it('should load requirements list for selected version', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.load();
    expect(component.pagedRequirements?.totalElements).toBe(1);
    expect(component.pagedRequirements?.content[0].skillCode).toBe('TECH_JAVA');
  });

  it('should reset page on search', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.currentPage = 3;
    component.searchTerm = 'JAVA';
    component.onSearch();
    expect(component.currentPage).toBe(0);
  });

  it('should apply skillGroup/mandatory/status filters', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.filterSkillGroup = 'COMMON_TECH';
    component.filterMandatory = 'Y';
    component.filterStatus = 'ACTIVE';
    component.onFilterChange();
    expect(reqSvc.getRequirementsPaged).toHaveBeenCalledWith(
      jasmine.objectContaining({
        skillGroup: 'COMMON_TECH', mandatory: 'Y', status: 'ACTIVE',
      })
    );
  });

  it('hasActiveFilters should detect active filters', () => {
    component.searchTerm = '';
    component.filterSkillGroup = '';
    component.filterMandatory = '';
    component.filterStatus = '';
    expect(component.hasActiveFilters()).toBeFalse();
    component.searchTerm = 'JAVA';
    expect(component.hasActiveFilters()).toBeTrue();
  });

  it('resetFilters should clear filters and reload', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.searchTerm = 'JAVA';
    component.filterSkillGroup = 'COMMON_TECH';
    component.filterMandatory = 'Y';
    component.filterStatus = 'ACTIVE';
    component.currentPage = 2;

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.filterSkillGroup).toBe('');
    expect(component.filterMandatory).toBe('');
    expect(component.filterStatus).toBe('');
    expect(component.currentPage).toBe(0);
  });

  it('isFormValid should be false when no skill selected', () => {
    component.form.skillPublicId = '';
    component.form.expectedLevel = 3;
    expect(component.isFormValid()).toBeFalse();
  });

  it('isFormValid should be true for valid level and selected skill', () => {
    component.form.skillPublicId = 'pub-skill-java';
    component.form.expectedLevel = 3;
    expect(component.isFormValid()).toBeTrue();
  });

  it('openEditForm should populate form from existing requirement', () => {
    component.openEditForm(mockRequirement);
    expect(component.formMode).toBe('edit');
    expect(component.editingPublicId).toBe('pub-er-1');
    expect(component.form.skillPublicId).toBe('pub-skill-java');
    expect(component.form.expectedLevel).toBe(3);
  });

  it('submitForm (create) should call createRequirement and refresh list on success', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.formMode = 'create';
    component.form.skillPublicId = 'pub-skill-java';
    component.form.expectedLevel = 4;
    component.form.mandatory = true;
    reqSvc.createRequirement.and.returnValue(of(mockRequirement));

    component.submitForm();

    expect(reqSvc.createRequirement).toHaveBeenCalled();
    expect(component.showForm).toBeFalse();
  });

  it('submitForm (create) should surface duplicate (409) error message', () => {
    component.selectedVersionPublicId = 'pub-rv-1';
    component.formMode = 'create';
    component.form.skillPublicId = 'pub-skill-java';
    component.form.expectedLevel = 3;
    reqSvc.createRequirement.and.returnValue(
      throwError(() => ({ error: { message: 'A requirement already exists for skill TECH_JAVA in version v1.0' } }))
    );

    component.submitForm();

    expect(component.formError).toContain('already exists');
    expect(component.formSubmitting).toBeFalse();
  });

  it('submitForm (edit) should call updateRequirement', () => {
    component.formMode = 'edit';
    component.editingPublicId = 'pub-er-1';
    component.form.skillPublicId = 'pub-skill-java';
    component.form.expectedLevel = 5;
    component.form.mandatory = false;
    reqSvc.updateRequirement.and.returnValue(of({ ...mockRequirement, expectedLevel: 5, mandatory: false }));

    component.submitForm();

    expect(reqSvc.updateRequirement).toHaveBeenCalledWith('pub-er-1', jasmine.objectContaining({ expectedLevel: 5, mandatory: false }));
  });

  it('downloadTemplate should delegate to service', () => {
    component.downloadTemplate();
    expect(reqSvc.downloadTemplate).toHaveBeenCalled();
  });

  it('onValidate should populate validationResult with INSERT/UPDATE/SKIP/ERROR rows', () => {
    const file = new File(['data'], 'test.xlsx');
    component.selectedFile = file;
    const result: RequirementValidationResult = {
      valid: false, totalRows: 4, insertRows: 1, updateRows: 1, skipRows: 1, errorRows: 1,
      rows: [
        { sheetName: 's', rowNumber: 2, applicationCode: 'ATLAS', skillCode: 'TECH_JAVA', skillName: 'Java', expectedLevel: 3, mandatoryFlag: 'Y', action: 'INSERT', reason: '', fieldErrors: [] },
        { sheetName: 's', rowNumber: 3, applicationCode: 'ATLAS', skillCode: 'TECH_SQL', skillName: 'SQL', expectedLevel: 3, mandatoryFlag: 'Y', action: 'UPDATE', reason: '', fieldErrors: [] },
        { sheetName: 's', rowNumber: 4, applicationCode: 'ATLAS', skillCode: 'TECH_LINUX', skillName: 'Linux', expectedLevel: 2, mandatoryFlag: 'N', action: 'SKIP', reason: '', fieldErrors: [] },
        { sheetName: 's', rowNumber: 5, applicationCode: 'ATLAS', skillCode: 'TECH_BAD', skillName: null, expectedLevel: 9, mandatoryFlag: 'Y', action: 'ERROR', reason: 'invalid', fieldErrors: ['expected_level must be between 1 and 5'] },
      ],
    };
    reqSvc.validateImport.and.returnValue(of(result));

    component.onValidate();

    expect(component.validationResult?.totalRows).toBe(4);
    expect(component.validationResult?.errorRows).toBe(1);
  });

  it('canConfirmImport should be false when validation has error rows', () => {
    component.validationResult = {
      valid: false, totalRows: 1, insertRows: 0, updateRows: 0, skipRows: 0, errorRows: 1, rows: [],
    };
    expect(component.canConfirmImport).toBeFalse();
  });

  it('canConfirmImport should be true when no error rows exist', () => {
    component.validationResult = {
      valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [],
    };
    component.importing = false;
    expect(component.canConfirmImport).toBeTrue();
  });
});
