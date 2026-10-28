import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { SelfAssessmentComponent } from './self-assessment.component';
import { TechnicianAssessmentService } from '../../../core/services/technician-assessment.service';
import { TechnicianAssessment } from '../../../core/models/assessment.model';

describe('SelfAssessmentComponent', () => {
  let fixture: ComponentFixture<SelfAssessmentComponent>;
  let component: SelfAssessmentComponent;
  let svc: jasmine.SpyObj<TechnicianAssessmentService>;
  let router: jasmine.SpyObj<Router>;

  // Dates computed relative to the real "today" so the fixture always falls
  // WITHIN the active date window, regardless of when the test suite runs.
  const today = new Date();
  const isoDaysFromNow = (days: number) => {
    const d = new Date(today);
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  };
  const activeStart = isoDaysFromNow(-30);
  const activeEnd = isoDaysFromNow(30);

  const draftAssessment: TechnicianAssessment = {
    assessmentPublicId: 'pub-header-1',
    cycleName: 'ATLAS FY26 Assessment',
    applicationCode: 'ATLAS_DEZENTRAL',
    applicationName: 'ATLAS-deZentral',
    cycleStartDate: activeStart,
    cycleEndDate: activeEnd,
    statusCode: 'DRAFT',
    statusLabel: 'Draft',
    submittedAt: null,
    version: 0,
    items: [
      {
        assessmentItemPublicId: 'pub-item-1', skillPublicId: 'pub-skill-java',
        skillCode: 'TECH_JAVA', skillName: 'Java', categoryCode: 'TECHNICAL',
        categoryName: 'Technical', skillGroup: 'COMMON_TECH', mandatory: true,
        selfRating: null, comment: null, evidence: null,
      },
      {
        assessmentItemPublicId: 'pub-item-2', skillPublicId: 'pub-skill-linux',
        skillCode: 'TECH_LINUX', skillName: 'Linux', categoryCode: 'TECHNICAL',
        categoryName: 'Technical', skillGroup: 'COMMON_TECH', mandatory: false,
        selfRating: null, comment: null, evidence: null,
      },
    ],
  };

  const submittedAssessment: TechnicianAssessment = {
    ...draftAssessment,
    statusCode: 'SUBMITTED',
    statusLabel: 'Submitted',
    submittedAt: '2026-01-10T00:00:00',
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj('TechnicianAssessmentService', [
      'getAssessment', 'saveDraft', 'submit',
    ]);
    router = jasmine.createSpyObj('Router', ['navigate']);
    svc.getAssessment.and.returnValue(of(JSON.parse(JSON.stringify(draftAssessment))));

    await TestBed.configureTestingModule({
      imports: [SelfAssessmentComponent, FormsModule],
      providers: [
        { provide: TechnicianAssessmentService, useValue: svc },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ publicId: 'pub-header-1' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelfAssessmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the assessment by publicId from route', () => {
    expect(component).toBeTruthy();
    expect(svc.getAssessment).toHaveBeenCalledWith('pub-header-1');
    expect(component.assessment?.items.length).toBe(2);
  });

  it('CRITICAL: assessment items must never contain an expectedLevel/expectedRating field', () => {
    const item = component.assessment!.items[0] as any;
    expect(item.expectedLevel).toBeUndefined();
    expect(item.expectedRating).toBeUndefined();
    expect(item.targetLevel).toBeUndefined();
    expect(item.requiredLevel).toBeUndefined();
  });

  it('should group items by skillGroup with human-readable labels', () => {
    const groups = component.groupedItems();
    expect(groups.length).toBe(1);
    expect(groups[0].group).toBe('Common Technical Skills');
    expect(groups[0].items.length).toBe(2);
  });

  it('mandatory progress should count only mandatory items', () => {
    expect(component.mandatoryTotal).toBe(1);
    expect(component.mandatoryCompleted).toBe(0);
    expect(component.canSubmit).toBeFalse();
  });

  it('canSubmit should become true once all mandatory items are rated', () => {
    component.assessment!.items[0].selfRating = 4;
    expect(component.mandatoryCompleted).toBe(1);
    expect(component.canSubmit).toBeTrue();
  });

  it('isReadOnly should be true only when status is SUBMITTED', () => {
    expect(component.isReadOnly).toBeFalse();
    component.assessment!.statusCode = 'SUBMITTED';
    expect(component.isReadOnly).toBeTrue();
  });

  it('saveDraft should call service with current version and item ratings', () => {
    component.assessment!.items[0].selfRating = 3;
    svc.saveDraft.and.returnValue(of({ ...draftAssessment, version: 1 }));

    component.saveDraft();

    expect(svc.saveDraft).toHaveBeenCalledWith('pub-header-1', jasmine.objectContaining({
      version: 0,
      items: jasmine.arrayContaining([
        jasmine.objectContaining({ assessmentItemPublicId: 'pub-item-1', selfRating: 3 }),
      ]),
    }));
    expect(component.successMessage).toBe('Draft saved.');
  });

  it('saveDraft should surface 409 stale-update conflict message', () => {
    svc.saveDraft.and.returnValue(
      throwError(() => ({ error: { message: 'This assessment was updated in another session. Refresh and try again.' } }))
    );

    component.saveDraft();

    expect(component.error).toContain('updated in another session');
  });

  it('requestSubmit should not open confirmation unless mandatory skills are complete', () => {
    component.requestSubmit();
    expect(component.showSubmitConfirm).toBeFalse();

    component.assessment!.items[0].selfRating = 5;
    component.requestSubmit();
    expect(component.showSubmitConfirm).toBeTrue();
  });

  it('confirmSubmit should call submit service and update assessment to SUBMITTED', () => {
    svc.submit.and.returnValue(of(submittedAssessment));

    component.confirmSubmit();

    expect(svc.submit).toHaveBeenCalledWith('pub-header-1');
    expect(component.assessment?.statusCode).toBe('SUBMITTED');
    expect(component.isReadOnly).toBeTrue();
  });

  it('confirmSubmit should surface business validation error (e.g. missing mandatory rating)', () => {
    svc.submit.and.returnValue(
      throwError(() => ({ error: { message: 'Please rate all mandatory skills before submitting.' } }))
    );

    component.confirmSubmit();

    expect(component.error).toContain('rate all mandatory skills');
  });

  it('backToList should navigate to my-assessments', () => {
    component.backToList();
    expect(router.navigate).toHaveBeenCalledWith(['/technician/my-assessments']);
  });

  it('isCycleExpired should be false while the cycle end date is in the future', () => {
    expect(component.isCycleExpired).toBeFalse();
    expect(component.isReadOnly).toBeFalse();
  });

  it('isCycleExpired should be true once the cycle end date has passed, forcing read-only even on a DRAFT', () => {
    component.assessment!.cycleEndDate = isoDaysFromNow(-1);
    expect(component.isCycleExpired).toBeTrue();
    expect(component.isReadOnly).toBeTrue();
  });

  it('saveDraft should be a no-op once the cycle has expired (defense-in-depth alongside backend validation)', () => {
    component.assessment!.cycleEndDate = isoDaysFromNow(-1);
    component.saveDraft();
    expect(svc.saveDraft).not.toHaveBeenCalled();
  });

  it('submit confirmation modal renders using modal-box (regression guard for the blank-modal CSS bug)', () => {
    component.assessment!.items[0].selfRating = 5;
    component.requestSubmit();
    fixture.detectChanges();
    const modalBox = fixture.nativeElement.querySelector('.modal-overlay .modal-box');
    expect(modalBox).toBeTruthy();
  });
});
