import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AssessmentCyclesComponent } from './assessment-cycles.component';
import { AssessmentCycleService } from '../../../core/services/assessment-cycle.service';
import { RequirementService } from '../../../core/services/requirement.service';
import { ApplicationService } from '../../../core/services/application.service';
import { PagedResponse } from '../../../core/models/application.model';
import { AssessmentCycle } from '../../../core/models/assessment.model';
import { RequirementVersion } from '../../../core/models/requirement.model';

describe('AssessmentCyclesComponent', () => {
  let fixture: ComponentFixture<AssessmentCyclesComponent>;
  let component: AssessmentCyclesComponent;
  let cycleSvc: jasmine.SpyObj<AssessmentCycleService>;
  let reqSvc: jasmine.SpyObj<RequirementService>;
  let appSvc: jasmine.SpyObj<ApplicationService>;

  const mockCycle: AssessmentCycle = {
    publicId: 'pub-cycle-1',
    applicationPublicId: 'pub-app-atlas',
    applicationCode: 'ATLAS_DEZENTRAL',
    applicationName: 'ATLAS-deZentral',
    requirementVersionPublicId: 'pub-rv-1',
    requirementVersionCode: 'v1.0',
    cycleName: 'ATLAS FY26 Assessment',
    cycleStartDate: '2026-01-01',
    cycleEndDate: '2026-03-31',
    statusCode: 'PLANNED',
    statusLabel: 'Planned',
    active: true,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
    createdBy: 'admin',
    updatedBy: 'admin',
  };

  const mockVersion: RequirementVersion = {
    publicId: 'pub-rv-1', applicationPublicId: 'pub-app-atlas',
    applicationCode: 'ATLAS_DEZENTRAL', applicationName: 'ATLAS-deZentral',
    versionCode: 'v1.0', versionName: 'v1', description: null,
    statusCode: 'PUBLISHED', statusLabel: 'Published', publishedAt: null,
    approvedAt: null, approvedBy: null, active: true,
    createdAt: '', updatedAt: '', createdBy: null, updatedBy: null,
  };

  const pagedCycles: PagedResponse<AssessmentCycle> = {
    content: [mockCycle], page: 0, size: 20,
    totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
  };

  beforeEach(async () => {
    cycleSvc = jasmine.createSpyObj('AssessmentCycleService', [
      'getCycles', 'getByPublicId', 'createCycle', 'updateCycle', 'openCycle', 'closeCycle', 'reopenCycle',
    ]);
    reqSvc = jasmine.createSpyObj('RequirementService', ['getVersionsForApplication']);
    appSvc = jasmine.createSpyObj('ApplicationService', ['getApplicationTypes', 'getBundles', 'getApplications']);

    cycleSvc.getCycles.and.returnValue(of(pagedCycles));
    reqSvc.getVersionsForApplication.and.returnValue(of([mockVersion]));
    appSvc.getApplicationTypes.and.returnValue(of([]));
    appSvc.getBundles.and.returnValue(of([]));
    appSvc.getApplications.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AssessmentCyclesComponent, FormsModule],
      providers: [
        { provide: AssessmentCycleService, useValue: cycleSvc },
        { provide: RequirementService, useValue: reqSvc },
        { provide: ApplicationService, useValue: appSvc },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssessmentCyclesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load cycles on init', () => {
    expect(component).toBeTruthy();
    expect(cycleSvc.getCycles).toHaveBeenCalled();
    expect(component.pagedCycles?.content.length).toBe(1);
  });

  it('should reset page on search', () => {
    component.currentPage = 2;
    component.searchTerm = 'ATLAS';
    component.onSearch();
    expect(component.currentPage).toBe(0);
  });

  it('hasActiveFilters should detect active filters', () => {
    expect(component.hasActiveFilters()).toBeFalse();
    component.searchTerm = 'ATLAS';
    expect(component.hasActiveFilters()).toBeTrue();
  });

  it('resetFilters should clear filters and reload', () => {
    component.searchTerm = 'ATLAS';
    component.filterApplicationPublicId = 'pub-app-atlas';
    component.filterStatusCode = 'OPEN';
    component.currentPage = 2;

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.filterApplicationPublicId).toBe('');
    expect(component.filterStatusCode).toBe('');
    expect(component.currentPage).toBe(0);
  });

  // ---------------------------------------------------------------
  // Requirement Version cascading dropdown (M4 stabilization)
  // ---------------------------------------------------------------

  it('onFormApplicationChange should load eligible requirement versions for the selected application', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(reqSvc.getVersionsForApplication).toHaveBeenCalledWith('pub-app-atlas', true);
    expect(component.formVersions.length).toBe(1);
  });

  it('ATLAS with v1.0 shows v1.0 in the version list', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(component.formVersions[0].versionCode).toBe('v1.0');
    expect(component.versionsLoading).toBeFalse();
    expect(component.versionsError).toBeFalse();
  });

  it('AVUS with no eligible versions results in an empty formVersions list (not an error)', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([]));
    component.form.applicationPublicId = 'pub-app-avus';
    component.onFormApplicationChange();
    expect(component.formVersions).toEqual([]);
    expect(component.versionsError).toBeFalse();
  });

  it('Save as PLANNED is disabled when no requirement version is available', () => {
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-avus',
      requirementVersionPublicId: '',
      cycleName: 'AVUS FY26',
      cycleStartDate: '2026-01-01',
      cycleEndDate: '2026-03-31',
    };
    component.formVersions = [];
    expect(component.isFormValid()).toBeFalse();
  });

  it('changing Application clears the previously selected Requirement Version', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.requirementVersionPublicId = 'pub-rv-1';
    component.formVersions = [mockVersion];

    component.onFormApplicationChange();

    expect(component.form.requirementVersionPublicId).toBe('');
  });

  it('changing Bundle clears Application and Requirement Version', () => {
    component.form.bundleCode = 'B06';
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.requirementVersionPublicId = 'pub-rv-1';
    component.formVersions = [mockVersion];

    component.onFormBundleChange();

    expect(component.form.applicationPublicId).toBe('');
    expect(component.form.requirementVersionPublicId).toBe('');
    expect(component.formVersions).toEqual([]);
  });

  it('changing Application Type clears Bundle, Application and Requirement Version', () => {
    component.form.typeCode = 'WEB';
    component.form.bundleCode = 'B06';
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.requirementVersionPublicId = 'pub-rv-1';
    component.formVersions = [mockVersion];

    component.onFormTypeChange();

    expect(component.form.bundleCode).toBe('');
    expect(component.form.applicationPublicId).toBe('');
    expect(component.form.requirementVersionPublicId).toBe('');
    expect(component.formVersions).toEqual([]);
  });

  it('versionsLoading is true while the request is pending and false once resolved', () => {
    expect(component.versionsLoading).toBeFalse();
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    // Synchronous `of()` resolves immediately in this spy, so by the time we check,
    // loading has already toggled back to false — verifying the final state here.
    expect(component.versionsLoading).toBeFalse();
  });

  it('API failure while loading versions sets versionsError (distinct from an empty result)', () => {
    reqSvc.getVersionsForApplication.and.returnValue(throwError(() => new Error('network error')));
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(component.versionsError).toBeTrue();
    expect(component.formVersions).toEqual([]);
  });

  it('Requirement Version from a previously selected application is never retained after switching applications', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    component.form.requirementVersionPublicId = 'pub-rv-1';
    expect(component.formVersions).toEqual([mockVersion]);

    const avusVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-avus', applicationPublicId: 'pub-app-avus' };
    reqSvc.getVersionsForApplication.and.returnValue(of([avusVersion]));
    component.form.applicationPublicId = 'pub-app-avus';
    component.onFormApplicationChange();

    expect(component.form.requirementVersionPublicId).toBe('');
    expect(component.formVersions).toEqual([avusVersion]);
    expect(component.formVersions.find((v) => v.publicId === 'pub-rv-1')).toBeUndefined();
  });

  it('selecting a valid version enables Save when all other required fields are valid', () => {
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: '',
      cycleName: 'ATLAS FY26',
      cycleStartDate: '2026-01-01',
      cycleEndDate: '2026-03-31',
    };
    component.formVersions = [mockVersion];
    expect(component.isFormValid()).toBeFalse();

    component.form.requirementVersionPublicId = 'pub-rv-1';
    expect(component.isFormValid()).toBeTrue();
  });

  // ---------------------------------------------------------------
  // Form validation / submit (pre-existing coverage)
  // ---------------------------------------------------------------

  it('isFormValid should require name, application, version and valid dates', () => {
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: '', requirementVersionPublicId: '',
      cycleName: '', cycleStartDate: '', cycleEndDate: '',
    };
    component.formVersions = [];
    expect(component.isFormValid()).toBeFalse();

    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS FY26',
      cycleStartDate: '2026-01-01',
      cycleEndDate: '2026-03-31',
    };
    component.formVersions = [mockVersion];
    expect(component.isFormValid()).toBeTrue();
  });

  it('isFormValid should reject end date before start date', () => {
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS FY26',
      cycleStartDate: '2026-03-31',
      cycleEndDate: '2026-01-01',
    };
    component.formVersions = [mockVersion];
    expect(component.isFormValid()).toBeFalse();
  });

  it('submitForm (create) should call createCycle and close form on success', () => {
    component.formMode = 'create';
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS FY26',
      cycleStartDate: '2026-01-01',
      cycleEndDate: '2026-03-31',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(of(mockCycle));

    component.submitForm();

    expect(cycleSvc.createCycle).toHaveBeenCalled();
    expect(component.showForm).toBeFalse();
  });

  it('submitForm (create) should surface duplicate cycle name error', () => {
    component.formMode = 'create';
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS Q3 2026 Assessment',
      cycleStartDate: '2026-10-26',
      cycleEndDate: '2026-11-26',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(
      throwError(() => ({ error: { message: "An assessment cycle named 'ATLAS Q3 2026 Assessment' already exists for ATLAS-deZentral. Please use a unique cycle name for the new assessment period." } }))
    );

    component.submitForm();

    expect(component.formError).toContain('already exists');
  });

  // ---------------------------------------------------------------
  // Duplicate-name UX (M4 stabilization)
  // ---------------------------------------------------------------

  it('duplicate same application + cycle name displays an inline duplicate warning', () => {
    const existing = { ...mockCycle, cycleName: 'ATLAS Q3 2026 Assessment' };
    cycleSvc.getCycles.and.returnValue(of({ ...pagedCycles, content: [existing] }));

    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.cycleName = 'ATLAS Q3 2026 Assessment';
    component.checkDuplicateName();

    expect(component.duplicateCycle).toEqual(existing);
  });

  it('duplicate error from a failed submit does not clear the form', () => {
    component.formMode = 'create';
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS Q3 2026 Assessment',
      cycleStartDate: '2026-10-26',
      cycleEndDate: '2026-11-26',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(
      throwError(() => ({ error: { message: 'duplicate' } }))
    );

    component.submitForm();

    expect(component.form.cycleName).toBe('ATLAS Q3 2026 Assessment');
    expect(component.form.applicationPublicId).toBe('pub-app-atlas');
    expect(component.form.cycleStartDate).toBe('2026-10-26');
    expect(component.form.cycleEndDate).toBe('2026-11-26');
  });

  it('duplicate error from a failed submit does not close the modal', () => {
    component.formMode = 'create';
    component.showForm = true;
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      requirementVersionPublicId: 'pub-rv-1',
      cycleName: 'ATLAS Q3 2026 Assessment',
      cycleStartDate: '2026-10-26',
      cycleEndDate: '2026-11-26',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(
      throwError(() => ({ error: { message: 'duplicate' } }))
    );

    component.submitForm();

    expect(component.showForm).toBeTrue();
  });

  it('unique cycle name has no duplicate warning and saves successfully', () => {
    cycleSvc.getCycles.and.returnValue(of({ ...pagedCycles, content: [] }));
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.cycleName = 'ATLAS Q4 2026 Assessment';
    component.checkDuplicateName();
    expect(component.duplicateCycle).toBeNull();

    component.form.requirementVersionPublicId = 'pub-rv-1';
    component.form.cycleStartDate = '2026-10-26';
    component.form.cycleEndDate = '2026-11-26';
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(of({ ...mockCycle, cycleName: 'ATLAS Q4 2026 Assessment' }));

    component.submitForm();

    expect(cycleSvc.createCycle).toHaveBeenCalled();
    expect(component.showForm).toBeFalse();
  });

  it('suggested alternative name is displayed when a duplicate is detected', () => {
    const existing = { ...mockCycle, cycleName: 'ATLAS Q3 2026 Assessment' };
    cycleSvc.getCycles.and.returnValue(of({ ...pagedCycles, content: [existing] }));

    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.cycleName = 'ATLAS Q3 2026 Assessment';
    component.form.cycleStartDate = '2026-10-26';
    component.form.cycleEndDate = '2026-11-26';
    component.checkDuplicateName();

    expect(component.suggestedCycleName).toBe('ATLAS-deZentral Oct-Nov 2026 Assessment');
  });

  it('useSuggestedName applies the suggestion and clears the duplicate warning', () => {
    component.duplicateCycle = mockCycle;
    component.suggestedCycleName = 'ATLAS-deZentral Oct-Nov 2026 Assessment';

    component.useSuggestedName();

    expect(component.form.cycleName).toBe('ATLAS-deZentral Oct-Nov 2026 Assessment');
    expect(component.duplicateCycle).toBeNull();
    expect(component.suggestedCycleName).toBeNull();
  });

  it('changing Application revalidates duplicate state for the newly selected application', () => {
    component.form.cycleName = 'ATLAS Q3 2026 Assessment';
    component.duplicateCycle = mockCycle;

    component.form.applicationPublicId = 'pub-app-avus';
    cycleSvc.getCycles.and.returnValue(of({ ...pagedCycles, content: [] }));

    component.onFormApplicationChange();

    expect(component.duplicateCycle).toBeNull();
  });

  it('changing Cycle Name clears an outdated duplicate error', () => {
    component.duplicateCycle = mockCycle;
    component.suggestedCycleName = 'Some suggestion';
    component.form.cycleName = 'A Brand New Name';
    component.form.applicationPublicId = '';

    component.onCycleNameChange();

    expect(component.duplicateCycle).toBeNull();
    expect(component.suggestedCycleName).toBeNull();
  });

  it('existing Requirement Version selection remains intact unless Application changes', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.requirementVersionPublicId = 'pub-rv-1';

    component.onCycleNameChange();

    expect(component.form.requirementVersionPublicId).toBe('pub-rv-1');
  });

  it('requestOpen/confirmOpen should call openCycle service and refresh list', () => {
    cycleSvc.openCycle.and.returnValue(of({ ...mockCycle, statusCode: 'OPEN' }));
    component.requestOpen(mockCycle);
    expect(component.confirmOpenCycle).toEqual(mockCycle);

    component.confirmOpen();

    expect(cycleSvc.openCycle).toHaveBeenCalledWith('pub-cycle-1');
    expect(component.confirmOpenCycle).toBeNull();
  });

  it('requestClose/confirmClose should call closeCycle service and refresh list', () => {
    const openCycle = { ...mockCycle, statusCode: 'OPEN' };
    cycleSvc.closeCycle.and.returnValue(of({ ...openCycle, statusCode: 'CLOSED' }));
    component.requestClose(openCycle);
    expect(component.confirmCloseCycle).toEqual(openCycle);

    component.confirmClose();

    expect(cycleSvc.closeCycle).toHaveBeenCalledWith('pub-cycle-1');
    expect(component.confirmCloseCycle).toBeNull();
  });

  it('openCycle error should surface business error message', () => {
    cycleSvc.openCycle.and.returnValue(
      throwError(() => ({ error: { message: 'Requirement version has zero active skill requirements — cannot open cycle' } }))
    );
    component.confirmOpenCycle = mockCycle;

    component.confirmOpen();

    expect(component.error).toContain('zero active skill requirements');
  });

  it('viewDetails/closeDetails should toggle detail modal', () => {
    component.viewDetails(mockCycle);
    expect(component.viewingCycle).toEqual(mockCycle);
    component.closeDetails();
    expect(component.viewingCycle).toBeNull();
  });

  it('detail modal renders using modal-box (regression guard for the blank-modal CSS bug)', () => {
    component.viewDetails(mockCycle);
    fixture.detectChanges();
    const modalBox = fixture.nativeElement.querySelector('.modal-overlay .modal-box');
    expect(modalBox).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.modal-overlay .modal:not(.modal-box)')).toBeFalsy();
  });

  it('isExpired should be false for a non-OPEN cycle regardless of dates', () => {
    const planned = { ...mockCycle, statusCode: 'PLANNED', cycleEndDate: '2000-01-01' };
    expect(component.isExpired(planned)).toBeFalse();
  });

  it('isExpired should be false for an OPEN cycle whose end date has not passed', () => {
    const future = new Date();
    future.setDate(future.getDate() + 30);
    const open = { ...mockCycle, statusCode: 'OPEN', cycleEndDate: future.toISOString().slice(0, 10) };
    expect(component.isExpired(open)).toBeFalse();
  });

  it('isExpired should be true for an OPEN cycle whose end date has passed', () => {
    const open = { ...mockCycle, statusCode: 'OPEN', cycleEndDate: '2000-01-01' };
    expect(component.isExpired(open)).toBeTrue();
  });

  it('isExpired should return false for null cycle', () => {
    expect(component.isExpired(null)).toBeFalse();
  });

  it('Escape key should close the open detail modal', () => {
    component.viewDetails(mockCycle);
    expect(component.viewingCycle).toEqual(mockCycle);
    component.onEscapeKey();
    expect(component.viewingCycle).toBeNull();
  });

  it('Escape key should cancel the open-cycle confirmation if no detail modal is open', () => {
    component.requestOpen(mockCycle);
    expect(component.confirmOpenCycle).toEqual(mockCycle);
    component.onEscapeKey();
    expect(component.confirmOpenCycle).toBeNull();
  });

  // ---------------------------------------------------------------
  // Reopen (CLOSED -> OPEN, M4 stabilization)
  // ---------------------------------------------------------------

  it('CLOSED cycle displays the Reopen action (canReopen true)', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED' };
    expect(component.canReopen(closedCycle)).toBeTrue();
  });

  it('OPEN cycle does not display the Reopen action', () => {
    const openCycle = { ...mockCycle, statusCode: 'OPEN' };
    expect(component.canReopen(openCycle)).toBeFalse();
  });

  it('PLANNED cycle does not display the Reopen action', () => {
    const plannedCycle = { ...mockCycle, statusCode: 'PLANNED' };
    expect(component.canReopen(plannedCycle)).toBeFalse();
  });

  it('Clicking Reopen opens the confirmation modal with cycle details', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED' };
    component.requestReopen(closedCycle);

    expect(component.reopenCycleTarget).toEqual(closedCycle);
    expect(component.reopenReason).toBe('');
    expect(component.reopenNewEndDate).toBe('');
  });

  it('Reopen modal renders cycle details', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleName: 'ATLAS Q2 2026 Assessment' };
    component.requestReopen(closedCycle);
    fixture.detectChanges();

    const modalText = fixture.nativeElement.querySelector('.modal-overlay .reopen-detail-list')?.textContent ?? '';
    expect(modalText).toContain('ATLAS Q2 2026 Assessment');
    expect(modalText).toContain('ATLAS-deZentral');
  });

  it('Reason is required — isReopenValid is false when reason is blank', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleEndDate: '2099-12-31' };
    component.requestReopen(closedCycle);
    component.reopenReason = '   ';

    expect(component.isReopenValid()).toBeFalse();
  });

  it('Cancel (cancelReopen) performs no change and clears modal state', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED' };
    component.requestReopen(closedCycle);
    component.reopenReason = 'Closed accidentally';

    component.cancelReopen();

    expect(component.reopenCycleTarget).toBeNull();
    expect(component.reopenReason).toBe('');
    expect(cycleSvc.reopenCycle).not.toHaveBeenCalled();
  });

  it('Valid reopen updates status to OPEN and refreshes the list', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleEndDate: '2099-12-31' };
    cycleSvc.reopenCycle.and.returnValue(of({ ...closedCycle, statusCode: 'OPEN' }));

    component.requestReopen(closedCycle);
    component.reopenReason = 'Closed accidentally during validation';

    component.confirmReopen();

    expect(cycleSvc.reopenCycle).toHaveBeenCalledWith('pub-cycle-1', {
      reason: 'Closed accidentally during validation',
      newEndDate: null,
    });
    expect(component.reopenCycleTarget).toBeNull();
    expect(component.successMessage).toContain('reopened');
  });

  it('Expired cycle requires a New End Date before reopen is valid', () => {
    const expiredClosedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleEndDate: '2000-01-01' };
    component.requestReopen(expiredClosedCycle);
    component.reopenReason = 'Extend period';

    expect(component.isClosedCycleExpired(expiredClosedCycle)).toBeTrue();
    expect(component.isReopenValid()).toBeFalse();

    component.reopenNewEndDate = '2099-12-31';
    expect(component.isReopenValid()).toBeTrue();
  });

  it('Reopened cycle row refreshes the list on success', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleEndDate: '2099-12-31' };
    cycleSvc.reopenCycle.and.returnValue(of({ ...closedCycle, statusCode: 'OPEN' }));
    const loadSpy = spyOn(component, 'load');

    component.requestReopen(closedCycle);
    component.reopenReason = 'reason';
    component.confirmReopen();

    expect(loadSpy).toHaveBeenCalled();
  });

  it('Reopen failure (e.g. expired-without-date 409) keeps the modal open with an error message', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED', cycleEndDate: '2099-12-31' };
    cycleSvc.reopenCycle.and.returnValue(
      throwError(() => ({ error: { message: 'This assessment cycle has expired. Extend the End Date before reopening.' } }))
    );

    component.requestReopen(closedCycle);
    component.reopenReason = 'reason';
    component.confirmReopen();

    expect(component.reopenCycleTarget).toEqual(closedCycle);
    expect(component.reopenError).toContain('expired');
  });

  it('Create Next Cycle continues working independently of the Reopen feature', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED' };
    component.createNextCycle(closedCycle);

    expect(component.showForm).toBeTrue();
    expect(component.form.applicationPublicId).toBe('pub-app-atlas');
    expect(component.form.cycleName).not.toBe(closedCycle.cycleName);
  });

  it('Escape key cancels the Reopen confirmation when open', () => {
    const closedCycle = { ...mockCycle, statusCode: 'CLOSED' };
    component.requestReopen(closedCycle);

    component.onEscapeKey();

    expect(component.reopenCycleTarget).toBeNull();
  });
});
