import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { MyAssessmentsComponent } from './my-assessments.component';
import { TechnicianAssessmentService } from '../../../core/services/technician-assessment.service';
import { TechnicianAssessment, TechnicianAssessmentSummary } from '../../../core/models/assessment.model';

describe('MyAssessmentsComponent', () => {
  let fixture: ComponentFixture<MyAssessmentsComponent>;
  let component: MyAssessmentsComponent;
  let svc: jasmine.SpyObj<TechnicianAssessmentService>;
  let router: jasmine.SpyObj<Router>;

  // Dates computed relative to the real "today" so the fixtures always fall
  // WITHIN the active date window, regardless of when the test suite runs.
  const today = new Date();
  const isoDaysFromNow = (days: number) => {
    const d = new Date(today);
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  };
  const activeStart = isoDaysFromNow(-30);
  const activeEnd = isoDaysFromNow(30);

  const notStarted: TechnicianAssessmentSummary = {
    assessmentPublicId: null, cyclePublicId: 'pub-cycle-1', cycleName: 'ATLAS FY26',
    applicationCode: 'ATLAS_DEZENTRAL', applicationName: 'ATLAS-deZentral',
    cycleStartDate: activeStart, cycleEndDate: activeEnd,
    assessmentStatus: 'NOT_STARTED', totalItems: 0, ratedItems: 0, updatedAt: null,
  };

  const draft: TechnicianAssessmentSummary = {
    assessmentPublicId: 'pub-header-1', cyclePublicId: 'pub-cycle-2', cycleName: 'OTHER FY26',
    applicationCode: 'OTHER', applicationName: 'Other App',
    cycleStartDate: activeStart, cycleEndDate: activeEnd,
    assessmentStatus: 'DRAFT', totalItems: 5, ratedItems: 2, updatedAt: '2026-01-05T00:00:00',
  };

  const startedAssessment: TechnicianAssessment = {
    assessmentPublicId: 'pub-header-new', cycleName: 'ATLAS FY26',
    applicationCode: 'ATLAS_DEZENTRAL', applicationName: 'ATLAS-deZentral',
    cycleStartDate: activeStart, cycleEndDate: activeEnd,
    statusCode: 'DRAFT', statusLabel: 'Draft', submittedAt: null, items: [], version: 0,
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj('TechnicianAssessmentService', [
      'getMyAssessments', 'startAssessment', 'getAssessment', 'saveDraft', 'submit',
    ]);
    router = jasmine.createSpyObj('Router', ['navigate']);
    svc.getMyAssessments.and.returnValue(of([notStarted, draft]));

    await TestBed.configureTestingModule({
      imports: [MyAssessmentsComponent],
      providers: [
        { provide: TechnicianAssessmentService, useValue: svc },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyAssessmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load technician assessments on init', () => {
    expect(component).toBeTruthy();
    expect(svc.getMyAssessments).toHaveBeenCalled();
    expect(component.assessments.length).toBe(2);
  });

  it('should only show technician-specific data (no cross-user leakage in model)', () => {
    // Model contract check: summary never includes another technician's identity field.
    expect((component.assessments[0] as any).userPublicId).toBeUndefined();
    expect((component.assessments[0] as any).technicianPublicId).toBeUndefined();
  });

  it('summary counts should reflect NOT_STARTED/DRAFT/SUBMITTED buckets', () => {
    expect(component.openCount).toBe(1);
    expect(component.draftCount).toBe(1);
    expect(component.submittedCount).toBe(0);
  });

  it('actionLabel should map status to correct button label', () => {
    expect(component.actionLabel(notStarted)).toBe('Start Assessment');
    expect(component.actionLabel(draft)).toBe('Continue');
    expect(component.actionLabel({ ...draft, assessmentStatus: 'SUBMITTED' })).toBe('View');
  });

  it('onAction for NOT_STARTED should call startAssessment and navigate to self-assessment', () => {
    svc.startAssessment.and.returnValue(of(startedAssessment));

    component.onAction(notStarted);

    expect(svc.startAssessment).toHaveBeenCalledWith('pub-cycle-1');
    expect(router.navigate).toHaveBeenCalledWith(['/technician/self-assessment', 'pub-header-new']);
  });

  it('onAction for DRAFT should navigate directly using existing assessmentPublicId', () => {
    component.onAction(draft);
    expect(router.navigate).toHaveBeenCalledWith(['/technician/self-assessment', 'pub-header-1']);
  });

  it('onAction should surface the backend business error message if start fails (e.g. not assigned)', () => {
    svc.startAssessment.and.returnValue(
      throwError(() => ({ error: { message: 'You are not assigned to this application.' } }))
    );

    component.onAction(notStarted);

    expect(component.error).toBe('You are not assigned to this application.');
  });

  it('onAction should fall back to a generic error message when the backend provides none', () => {
    svc.startAssessment.and.returnValue(throwError(() => ({})));

    component.onAction(notStarted);

    expect(component.error).toBe('Failed to start assessment.');
  });

  it('progressLabel should show "rated of total" for started assessments, dash for not started', () => {
    expect(component.progressLabel(notStarted)).toBe('—');
    expect(component.progressLabel(draft)).toBe('2 of 5 rated');
  });

  it('windowState should return ACTIVE when today is within the cycle window', () => {
    expect(component.windowState(notStarted)).toBe('ACTIVE');
    expect(component.windowLabel(notStarted)).toBeNull();
  });

  it('windowState should return NOT_YET_OPEN when the cycle has not started', () => {
    const future = { ...notStarted, cycleStartDate: isoDaysFromNow(10), cycleEndDate: isoDaysFromNow(40) };
    expect(component.windowState(future)).toBe('NOT_YET_OPEN');
    expect(component.windowLabel(future)).toBe('Not Yet Open');
  });

  it('windowState should return EXPIRED when the cycle end date has passed', () => {
    const expired = { ...draft, cycleStartDate: isoDaysFromNow(-60), cycleEndDate: isoDaysFromNow(-1) };
    expect(component.windowState(expired)).toBe('EXPIRED');
    expect(component.windowLabel(expired)).toBe('Expired');
  });

  it('actionLabel should show "Not Yet Open" and disable the action before the window opens', () => {
    const future = { ...notStarted, cycleStartDate: isoDaysFromNow(10), cycleEndDate: isoDaysFromNow(40) };
    expect(component.actionLabel(future)).toBe('Not Yet Open');
    expect(component.isActionDisabled(future)).toBeTrue();
  });

  it('actionLabel should disable "Start Assessment" once the cycle has expired with no draft', () => {
    const expiredNotStarted = { ...notStarted, cycleStartDate: isoDaysFromNow(-60), cycleEndDate: isoDaysFromNow(-1) };
    expect(component.isActionDisabled(expiredNotStarted)).toBeTrue();
  });

  it('actionLabel should allow viewing an expired DRAFT (read-only) rather than disabling entirely', () => {
    const expiredDraft = { ...draft, cycleStartDate: isoDaysFromNow(-60), cycleEndDate: isoDaysFromNow(-1) };
    expect(component.actionLabel(expiredDraft)).toBe('View');
    expect(component.isActionDisabled(expiredDraft)).toBeFalse();
  });

  it('SUBMITTED assessments remain actionable (View) regardless of window state', () => {
    const submittedExpired = { ...draft, assessmentStatus: 'SUBMITTED', cycleStartDate: isoDaysFromNow(-60), cycleEndDate: isoDaysFromNow(-1) };
    expect(component.actionLabel(submittedExpired)).toBe('View');
    expect(component.isActionDisabled(submittedExpired)).toBeFalse();
  });

  it('onAction should be a no-op when the action is disabled (e.g. not yet open)', () => {
    const future = { ...notStarted, cycleStartDate: isoDaysFromNow(10), cycleEndDate: isoDaysFromNow(40) };
    component.onAction(future);
    expect(svc.startAssessment).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
