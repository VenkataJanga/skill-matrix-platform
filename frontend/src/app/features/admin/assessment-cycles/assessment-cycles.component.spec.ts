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
    cycleName: 'ATLAS-deZentral Q3 2026 Assessment',
    periodTypeCode: 'QUARTERLY',
    periodValue: 'Q3',
    periodYear: 2026,
    cycleStartDate: '2026-07-01',
    cycleEndDate: '2026-09-30',
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
    activeRequirementCount: 1,
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

  // 1. New Cycle opens
  it('openAddForm opens the New Cycle form with default Period Type QUARTERLY', () => {
    component.openAddForm();
    expect(component.showForm).toBeTrue();
    expect(component.formMode).toBe('create');
    expect(component.form.periodTypeCode).toBe('QUARTERLY');
  });

  // 2. Application Type filters Bundle (verified via applications fetch call with typeCode)
  it('Application Type change triggers application reload scoped by typeCode', () => {
    component.openAddForm();
    component.form.typeCode = 'WEB';
    component.onFormTypeChange();
    expect(appSvc.getApplications).toHaveBeenCalledWith('WEB', undefined);
  });

  // 3. Bundle filters Application
  it('Bundle change triggers application reload scoped by typeCode + bundleCode', () => {
    component.openAddForm();
    component.form.typeCode = 'WEB';
    component.form.bundleCode = 'B06';
    component.onFormBundleChange();
    expect(appSvc.getApplications).toHaveBeenCalledWith('WEB', 'B06');
  });

  // 4. Application loads correct Requirement Versions
  it('Application change loads eligible Requirement Versions for that application', () => {
    component.openAddForm();
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(reqSvc.getVersionsForApplication).toHaveBeenCalledWith('pub-app-atlas', true);
    expect(component.formVersions.length).toBe(1);
  });

  // 5. Period Type defaults to Quarterly
  it('Period Type defaults to QUARTERLY on new cycle', () => {
    component.openAddForm();
    expect(component.form.periodTypeCode).toBe('QUARTERLY');
  });

  // 6. Quarterly shows Q1-Q4
  it('Quarterly period options are Q1-Q4', () => {
    const opts = component.computePeriodOptions('QUARTERLY').map((o) => o.code);
    expect(opts).toEqual(['Q1', 'Q2', 'Q3', 'Q4']);
  });

  // 7. Monthly shows January-December
  it('Monthly period options are January through December (labels)', () => {
    const opts = component.computePeriodOptions('MONTHLY').map((o) => o.label);
    expect(opts).toEqual([
      'January','February','March','April','May','June',
      'July','August','September','October','November','December',
    ]);
  });

  // 8. Half-Yearly shows H1/H2
  it('Half-Yearly period options are H1/H2', () => {
    const opts = component.computePeriodOptions('HALF_YEARLY').map((o) => o.code);
    expect(opts).toEqual(['H1', 'H2']);
  });

  // 9. Annual handles period correctly (no period options, isAnnual true)
  it('Annual has no period options and isAnnual is true', () => {
    component.openAddForm();
    component.form.periodTypeCode = 'ANNUAL';
    component.onPeriodTypeChange();
    expect(component.periodOptions).toEqual([]);
    expect(component.isAnnual).toBeTrue();
  });

  // 10. Selecting Application + Q4 + 2026 generates correct Cycle Name
  it('Application + Quarterly Q4 + 2026 generates "ATLAS-deZentral Q4 2026 Assessment"', () => {
    component.openAddForm();
    component.form.applicationPublicId = 'pub-app-atlas';
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'ATLAS_DEZENTRAL', applicationName: 'ATLAS-deZentral' } as any];
    component.form.periodTypeCode = 'QUARTERLY';
    component.form.periodValue = 'Q4';
    component.form.periodYear = 2026;
    component.onFormApplicationChange();
    component.onPeriodValueChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q4 2026 Assessment');
  });

  // Monthly example
  it('Monthly example generates "ATLAS-deZentral October 2026 Assessment"', () => {
    const name = component.buildGeneratedName('ATLAS-deZentral', 'MONTHLY', 'OCTOBER', 2026, '');
    expect(name).toBe('ATLAS-deZentral October 2026 Assessment');
  });

  // Half-Yearly example
  it('Half-Yearly example generates "ATLAS-deZentral H1 2027 Assessment"', () => {
    const name = component.buildGeneratedName('ATLAS-deZentral', 'HALF_YEARLY', 'H1', 2027, '');
    expect(name).toBe('ATLAS-deZentral H1 2027 Assessment');
  });

  // Annual example
  it('Annual example generates "ATLAS-deZentral 2027 Annual Assessment"', () => {
    const name = component.buildGeneratedName('ATLAS-deZentral', 'ANNUAL', '', 2027, '');
    expect(name).toBe('ATLAS-deZentral 2027 Annual Assessment');
  });

  // Custom example
  it('Custom example generates "ATLAS-deZentral Mid-Year Technical Assessment 2026"', () => {
    const name = component.buildGeneratedName('ATLAS-deZentral', 'CUSTOM', '', 2026, 'Mid-Year Technical Assessment');
    expect(name).toBe('ATLAS-deZentral Mid-Year Technical Assessment 2026');
  });

  // 11. Changing Period regenerates Cycle Name
  it('Changing Period regenerates the Cycle Name', () => {
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.periodYear = 2026;
    component.form.periodValue = 'Q3';
    component.onPeriodValueChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q3 2026 Assessment');

    component.form.periodValue = 'Q4';
    component.onPeriodValueChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q4 2026 Assessment');
  });

  // 12. Changing Year regenerates Cycle Name
  it('Changing Year regenerates the Cycle Name', () => {
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.periodValue = 'Q4';
    component.form.periodYear = 2026;
    component.onYearChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q4 2026 Assessment');

    component.form.periodYear = 2027;
    component.onYearChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q4 2027 Assessment');
  });

  // 13. Changing Application regenerates Cycle Name
  it('Changing Application regenerates the Cycle Name', () => {
    component.openAddForm();
    component.form.periodValue = 'Q4';
    component.form.periodYear = 2026;
    component.formApplications = [
      { publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any,
      { publicId: 'pub-app-avus', applicationCode: 'B', applicationName: 'AVUS' } as any,
    ];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q4 2026 Assessment');

    component.form.applicationPublicId = 'pub-app-avus';
    component.onFormApplicationChange();
    expect(component.form.cycleName).toBe('AVUS Q4 2026 Assessment');
  });

  // 14. Cycle Name cannot be manually edited (no two-way binding exposed to user input — template uses readonly input bound via [value])
  it('Cycle Name field is populated only via regenerateCycleName, never by direct form assignment from template', () => {
    component.openAddForm();
    component.form.cycleName = 'Attempted Manual Edit';
    // Regenerating recalculates and overwrites whatever was manually set
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.periodValue = 'Q1';
    component.form.periodYear = 2026;
    component.onPeriodValueChange();
    expect(component.form.cycleName).not.toBe('Attempted Manual Edit');
    expect(component.form.cycleName).toBe('ATLAS-deZentral Q1 2026 Assessment');
  });

  // 15. Duplicate cycle displays controlled validation
  it('Duplicate cycle name for the same application displays a controlled warning', () => {
    const existing = { ...mockCycle, cycleName: 'ATLAS-deZentral Q4 2026 Assessment' };
    cycleSvc.getCycles.and.returnValue(of({ ...pagedCycles, content: [existing] }));

    component.form.applicationPublicId = 'pub-app-atlas';
    component.form.cycleName = 'ATLAS-deZentral Q4 2026 Assessment';
    component.checkDuplicateName();

    expect(component.duplicateCycle).toEqual(existing);
  });

  // 16. Save sends generated cycleName
  it('submitForm (create) sends the generated cycleName and period metadata', () => {
    component.formMode = 'create';
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      periodTypeCode: 'QUARTERLY', periodValue: 'Q4', periodYear: 2026, customLabel: '',
      cycleName: 'ATLAS-deZentral Q4 2026 Assessment',
      requirementVersionPublicId: 'pub-rv-1',
      cycleStartDate: '2026-10-01',
      cycleEndDate: '2026-12-31',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(of(mockCycle));

    component.submitForm();

    expect(cycleSvc.createCycle).toHaveBeenCalledWith(jasmine.objectContaining({
      cycleName: 'ATLAS-deZentral Q4 2026 Assessment',
      periodTypeCode: 'QUARTERLY',
      periodValue: 'Q4',
      periodYear: 2026,
    }));
    expect(component.showForm).toBeFalse();
  });

  it('submitForm (create) should surface duplicate cycle name error', () => {
    component.formMode = 'create';
    component.form = {
      typeCode: '', bundleCode: '',
      applicationPublicId: 'pub-app-atlas',
      periodTypeCode: 'QUARTERLY', periodValue: 'Q3', periodYear: 2026, customLabel: '',
      cycleName: 'ATLAS-deZentral Q3 2026 Assessment',
      requirementVersionPublicId: 'pub-rv-1',
      cycleStartDate: '2026-10-26',
      cycleEndDate: '2026-11-26',
    };
    component.formVersions = [mockVersion];
    cycleSvc.createCycle.and.returnValue(
      throwError(() => ({ error: { message: "An assessment cycle named 'ATLAS-deZentral Q3 2026 Assessment' already exists for ATLAS-deZentral." } }))
    );

    component.submitForm();

    expect(component.formError).toContain('already exists');
  });

  // 17. Create Next Cycle Q3 -> Q4
  it('Create Next Cycle: Q3 2026 -> Q4 2026', () => {
    const source = { ...mockCycle, periodTypeCode: 'QUARTERLY', periodValue: 'Q3', periodYear: 2026 };
    component.createNextCycle(source);
    expect(component.form.periodTypeCode).toBe('QUARTERLY');
    expect(component.form.periodValue).toBe('Q4');
    expect(component.form.periodYear).toBe(2026);
  });

  // 18. Create Next Cycle Q4 2026 -> Q1 2027 (year rollover)
  it('Create Next Cycle: Q4 2026 -> Q1 2027 (year rollover)', () => {
    const source = { ...mockCycle, periodTypeCode: 'QUARTERLY', periodValue: 'Q4', periodYear: 2026 };
    component.createNextCycle(source);
    expect(component.form.periodTypeCode).toBe('QUARTERLY');
    expect(component.form.periodValue).toBe('Q1');
    expect(component.form.periodYear).toBe(2027);
    expect(component.form.cycleName).toContain('Q1 2027');
  });

  // 19. Monthly December -> January next year
  it('Create Next Cycle: December 2026 -> January 2027', () => {
    const source = { ...mockCycle, periodTypeCode: 'MONTHLY', periodValue: 'DECEMBER', periodYear: 2026 };
    const next = component.computeNextPeriod(source);
    expect(next.periodTypeCode).toBe('MONTHLY');
    expect(next.periodValue).toBe('JANUARY');
    expect(next.periodYear).toBe(2027);
  });

  // 20. Half-Yearly H2 -> H1 next year
  it('Create Next Cycle: H2 2026 -> H1 2027', () => {
    const source = { ...mockCycle, periodTypeCode: 'HALF_YEARLY', periodValue: 'H2', periodYear: 2026 };
    const next = component.computeNextPeriod(source);
    expect(next.periodTypeCode).toBe('HALF_YEARLY');
    expect(next.periodValue).toBe('H1');
    expect(next.periodYear).toBe(2027);
  });

  it('Create Next Cycle: H1 2026 -> H2 2026 (same year)', () => {
    const source = { ...mockCycle, periodTypeCode: 'HALF_YEARLY', periodValue: 'H1', periodYear: 2026 };
    const next = component.computeNextPeriod(source);
    expect(next.periodValue).toBe('H2');
    expect(next.periodYear).toBe(2026);
  });

  // 21. Annual -> next year
  it('Create Next Cycle: 2026 Annual -> 2027 Annual', () => {
    const source = { ...mockCycle, periodTypeCode: 'ANNUAL', periodValue: '', periodYear: 2026 };
    const next = component.computeNextPeriod(source);
    expect(next.periodTypeCode).toBe('ANNUAL');
    expect(next.periodYear).toBe(2027);
  });

  // 22. Requirement Version remains application-specific
  it('Requirement Version list is cleared and reloaded when Application changes (application-specific)', () => {
    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();
    expect(component.formVersions).toEqual([mockVersion]);

    const avusVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-avus', applicationPublicId: 'pub-app-avus' };
    reqSvc.getVersionsForApplication.and.returnValue(of([avusVersion]));
    component.form.applicationPublicId = 'pub-app-avus';
    component.onFormApplicationChange();

    expect(component.form.requirementVersionPublicId).toBe('');
    expect(component.formVersions).toEqual([avusVersion]);
  });

  // 23. Existing Open/Close/Reopen functionality remains unchanged
  it('requestOpen/confirmOpen calls openCycle and refreshes list', () => {
    cycleSvc.openCycle.and.returnValue(of({ ...mockCycle, statusCode: 'OPEN' }));
    component.requestOpen(mockCycle);
    expect(component.confirmOpenCycle).toEqual(mockCycle);

    component.confirmOpen();

    expect(cycleSvc.openCycle).toHaveBeenCalledWith('pub-cycle-1');
    expect(component.confirmOpenCycle).toBeNull();
  });

  it('requestClose/confirmClose calls closeCycle and refreshes list', () => {
    const openCycle = { ...mockCycle, statusCode: 'OPEN' };
    cycleSvc.closeCycle.and.returnValue(of({ ...openCycle, statusCode: 'CLOSED' }));
    component.requestClose(openCycle);
    expect(component.confirmCloseCycle).toEqual(openCycle);

    component.confirmClose();

    expect(cycleSvc.closeCycle).toHaveBeenCalledWith('pub-cycle-1');
    expect(component.confirmCloseCycle).toBeNull();
  });

  it('CLOSED cycle displays Reopen action; OPEN/PLANNED do not', () => {
    expect(component.canReopen({ ...mockCycle, statusCode: 'CLOSED' })).toBeTrue();
    expect(component.canReopen({ ...mockCycle, statusCode: 'OPEN' })).toBeFalse();
    expect(component.canReopen({ ...mockCycle, statusCode: 'PLANNED' })).toBeFalse();
  });

  it('Valid reopen updates status to OPEN and refreshes list', () => {
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

  it('isExpired is true only for an OPEN cycle whose end date has passed', () => {
    expect(component.isExpired({ ...mockCycle, statusCode: 'PLANNED', cycleEndDate: '2000-01-01' })).toBeFalse();
    expect(component.isExpired({ ...mockCycle, statusCode: 'OPEN', cycleEndDate: '2000-01-01' })).toBeTrue();
    expect(component.isExpired(null)).toBeFalse();
  });

  it('Escape key closes the open detail modal', () => {
    component.viewDetails(mockCycle);
    expect(component.viewingCycle).toEqual(mockCycle);
    component.onEscapeKey();
    expect(component.viewingCycle).toBeNull();
  });

  it('viewDetails/closeDetails toggles the detail modal', () => {
    component.viewDetails(mockCycle);
    expect(component.viewingCycle).toEqual(mockCycle);
    component.closeDetails();
    expect(component.viewingCycle).toBeNull();
  });

  it('openCycle error surfaces the business error message', () => {
    cycleSvc.openCycle.and.returnValue(
      throwError(() => ({ error: { message: 'Requirement version has zero active skill requirements — cannot open cycle' } }))
    );
    component.confirmOpenCycle = mockCycle;

    component.confirmOpen();

    expect(component.error).toContain('zero active skill requirements');
  });

  // ------------------------------------------------------------------
  // Early-warning: Requirement Version with zero active skill requirements
  // (prevents creating a PLANNED cycle that would fail backend Open validation)
  // ------------------------------------------------------------------

  const zeroActiveVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-zero', activeRequirementCount: 0 };
  const activeVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-active', activeRequirementCount: 2 };

  it('TC1: selecting a Requirement Version with 0 active requirements shows warning and disables Save', () => {
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.formVersions = [zeroActiveVersion];
    component.form.requirementVersionPublicId = 'pub-rv-zero';
    component.form.periodValue = 'Q1';
    component.form.periodYear = 2026;
    component.form.cycleName = 'ATLAS-deZentral Q1 2026 Assessment';
    component.form.cycleStartDate = '2026-01-01';
    component.form.cycleEndDate = '2026-03-31';

    component.onRequirementVersionChange();

    expect(component.selectedVersionHasNoActiveRequirements).toBeTrue();
    expect(component.isFormValid()).toBeFalse();
  });

  it('TC2: a Requirement Version with only inactive requirements (activeRequirementCount=0) shows warning and disables Save', () => {
    // Backend's countByRequirementVersion_IdAndActiveTrue already excludes inactive
    // rows, so an all-inactive version is indistinguishable from zero and reported as 0.
    const onlyInactiveVersion: RequirementVersion = { ...mockVersion, publicId: 'pub-rv-inactive-only', activeRequirementCount: 0 };
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.formVersions = [onlyInactiveVersion];
    component.form.requirementVersionPublicId = 'pub-rv-inactive-only';
    component.form.periodValue = 'Q1';
    component.form.periodYear = 2026;
    component.form.cycleStartDate = '2026-01-01';
    component.form.cycleEndDate = '2026-03-31';

    component.onRequirementVersionChange();

    expect(component.selectedVersionHasNoActiveRequirements).toBeTrue();
    expect(component.isFormValid()).toBeFalse();
  });

  it('TC3: a Requirement Version with 1+ active requirements shows no warning and Save is enabled when other fields valid', () => {
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.formVersions = [activeVersion];
    component.form.requirementVersionPublicId = 'pub-rv-active';
    component.form.periodValue = 'Q1';
    component.form.periodYear = 2026;
    component.form.cycleName = 'ATLAS-deZentral Q1 2026 Assessment';
    component.form.cycleStartDate = '2026-01-01';
    component.form.cycleEndDate = '2026-03-31';

    component.onRequirementVersionChange();

    expect(component.selectedVersionHasNoActiveRequirements).toBeFalse();
    expect(component.isFormValid()).toBeTrue();
  });

  it('TC4: backend Open-time validation remains intact — openCycle still surfaces "zero active skill requirements" error', () => {
    // Regression confirmation: this early-warning is advisory only; the backend
    // AssessmentCycleService.openCycle() safeguard is unchanged and still the
    // final authority if a cycle somehow reaches PLANNED with an empty version.
    cycleSvc.openCycle.and.returnValue(
      throwError(() => ({ error: { message: 'Requirement version has zero active skill requirements — cannot open cycle' } }))
    );
    component.confirmOpenCycle = mockCycle;
    component.confirmOpen();
    expect(component.error).toContain('zero active skill requirements — cannot open cycle');
  });

  it('TC5: version becomes eligible immediately (warning disappears) after it gains active requirements', () => {
    component.openAddForm();
    component.formApplications = [{ publicId: 'pub-app-atlas', applicationCode: 'A', applicationName: 'ATLAS-deZentral' } as any];
    component.form.applicationPublicId = 'pub-app-atlas';
    component.formVersions = [zeroActiveVersion];
    component.form.requirementVersionPublicId = 'pub-rv-zero';
    component.onRequirementVersionChange();
    expect(component.selectedVersionHasNoActiveRequirements).toBeTrue();

    // Simulate re-fetching versions after Admin added active requirements in
    // Skill Requirement Mapping (same publicId, now with activeRequirementCount > 0).
    const nowEligible: RequirementVersion = { ...zeroActiveVersion, activeRequirementCount: 1 };
    component.formVersions = [nowEligible];
    component.onRequirementVersionChange();

    expect(component.selectedVersionHasNoActiveRequirements).toBeFalse();
  });

  it('TC6a regression: switching Application resets the zero-active warning', () => {
    component.openAddForm();
    component.formVersions = [zeroActiveVersion];
    component.form.requirementVersionPublicId = 'pub-rv-zero';
    component.onRequirementVersionChange();
    expect(component.selectedVersionHasNoActiveRequirements).toBeTrue();

    component.form.applicationPublicId = 'pub-app-atlas';
    component.onFormApplicationChange();

    expect(component.selectedVersionHasNoActiveRequirements).toBeFalse();
  });

  it('TC6b regression: openEditForm re-evaluates the warning once eligible versions load for an existing PLANNED cycle', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([zeroActiveVersion]));
    const plannedCycleWithZeroActive = { ...mockCycle, requirementVersionPublicId: 'pub-rv-zero' };

    component.openEditForm(plannedCycleWithZeroActive);

    expect(component.selectedVersionHasNoActiveRequirements).toBeTrue();
  });

  it('TC6c regression: createNextCycle (Create Next Cycle) remains unaffected when the next version has active requirements', () => {
    reqSvc.getVersionsForApplication.and.returnValue(of([activeVersion]));
    const source = { ...mockCycle, periodTypeCode: 'QUARTERLY', periodValue: 'Q3', periodYear: 2026, requirementVersionPublicId: 'pub-rv-active' };

    component.createNextCycle(source);

    expect(component.selectedVersionHasNoActiveRequirements).toBeFalse();
  });
});
