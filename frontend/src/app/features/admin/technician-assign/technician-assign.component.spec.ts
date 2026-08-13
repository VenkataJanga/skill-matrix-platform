import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { TechnicianAssignComponent } from './technician-assign.component';
import { ApplicationService } from '../../../core/services/application.service';
import {
  ApplicationAssignment,
  DuplicateCheckResponse,
  PagedResponse,
} from '../../../core/models/application.model';

describe('TechnicianAssignComponent', () => {
  let fixture: ComponentFixture<TechnicianAssignComponent>;
  let component: TechnicianAssignComponent;
  let svc: jasmine.SpyObj<ApplicationService>;

  const mockAssignment: ApplicationAssignment = {
    publicId: 'pub-map-1', userPublicId: 'pub-tech', username: 'deepak_mishra',
    fullName: 'Deepak Mishra', applicationPublicId: 'pub-atlas',
    applicationCode: 'ATLAS', applicationName: 'ATLAS-deZentral',
    typeCode: 'WEB', bundleCode: 'B06', allocationPercentage: 100,
    effectiveFrom: '2026-01-01', effectiveTo: null, active: true,
  };

  const emptyPaged: PagedResponse<ApplicationAssignment> = {
    content: [mockAssignment], page: 0, size: 20,
    totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj('ApplicationService', [
      'getApplicationTypes', 'getBundles', 'getTechnicians', 'getApplications',
      'getAssignmentsPaged', 'checkDuplicate', 'createAssignment',
      'updateAssignmentStatus', 'bulkUpdateAssignmentStatus',
    ]);
    svc.getApplicationTypes.and.returnValue(of([]));
    svc.getBundles.and.returnValue(of([]));
    svc.getTechnicians.and.returnValue(of([]));
    svc.getApplications.and.returnValue(of([]));
    svc.getAssignmentsPaged.and.returnValue(of(emptyPaged));

    await TestBed.configureTestingModule({
      imports: [TechnicianAssignComponent, FormsModule],
      providers: [{ provide: ApplicationService, useValue: svc }],
    }).compileComponents();

    fixture = TestBed.createComponent(TechnicianAssignComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load assignments on init', () => {
    expect(component).toBeTruthy();
    expect(svc.getAssignmentsPaged).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({ page: 0, size: 20 })
    );
    expect(component.pagedAssignments?.totalElements).toBe(1);
  });

  it('should reset page on search', () => {
    component.currentPage = 5;
    component.searchTerm = 'ATLAS';
    component.onSearch();
    expect(component.currentPage).toBe(0);
    expect(svc.getAssignmentsPaged).toHaveBeenCalledTimes(2);
  });

  it('should disable Assign button when duplicate exists', () => {
    const dupResponse: DuplicateCheckResponse = {
      isDuplicate: true,
      existingAssignmentPublicId: 'pub-map-1',
      message: 'Already assigned',
    };
    component.duplicateCheck = dupResponse;
    component.form.userPublicId = 'pub-tech';
    component.form.applicationPublicId = 'pub-atlas';
    component.form.effectiveFrom = '2026-01-01';
    expect(component.isAssignDisabled).toBeTrue();
  });

  it('should enable Assign button when no duplicate', () => {
    const noDup: DuplicateCheckResponse = {
      isDuplicate: false,
      existingAssignmentPublicId: null,
      message: 'No duplicate',
    };
    component.duplicateCheck = noDup;
    component.form.userPublicId = 'pub-tech';
    component.form.applicationPublicId = 'pub-atlas';
    component.form.effectiveFrom = '2026-01-01';
    component.form.allocationPercentage = 100;
    expect(component.isAssignDisabled).toBeFalse();
  });

  it('should open bulk confirm dialog with selected IDs', () => {
    component.selectedIds.add('id1');
    component.selectedIds.add('id2');
    component.openBulkConfirm();
    expect(component.showBulkConfirm).toBeTrue();
  });

  it('should cancel bulk confirm and hide dialog', () => {
    component.showBulkConfirm = true;
    component.cancelBulk();
    expect(component.showBulkConfirm).toBeFalse();
  });

  // ---- Reset tests ----

  it('hasActiveAssignmentFilters should return false with default ACTIVE status and no other filters', () => {
    component.searchTerm = '';
    component.filterTypeCode = '';
    component.filterBundleCode = '';
    component.filterAppPublicId = '';
    component.filterTechPublicId = '';
    component.filterStatus = 'ACTIVE';
    expect(component.hasActiveAssignmentFilters()).toBeFalse();
  });

  it('hasActiveAssignmentFilters should return true when search is set', () => {
    component.searchTerm = 'ATLAS';
    expect(component.hasActiveAssignmentFilters()).toBeTrue();
  });

  it('hasActiveAssignmentFilters should return true when status filter is non-default', () => {
    component.filterStatus = 'INACTIVE';
    expect(component.hasActiveAssignmentFilters()).toBeTrue();
  });

  it('resetAssignmentFilters should clear all list filters and reload', () => {
    component.searchTerm = 'ATLAS';
    component.filterTypeCode = 'WEB';
    component.filterBundleCode = 'B06';
    component.filterAppPublicId = 'pub-app';
    component.filterTechPublicId = 'pub-tech';
    component.filterStatus = 'INACTIVE';
    component.currentPage = 2;

    component.resetAssignmentFilters();

    expect(component.searchTerm).toBe('');
    expect(component.filterTypeCode).toBe('');
    expect(component.filterBundleCode).toBe('');
    expect(component.filterAppPublicId).toBe('');
    expect(component.filterTechPublicId).toBe('');
    expect(component.filterStatus).toBe('ACTIVE');
    expect(component.currentPage).toBe(0);
    expect(svc.getAssignmentsPaged).toHaveBeenCalledTimes(2);
  });

  it('resetAssignmentFilters should NOT clear the new assignment form', () => {
    component.form.userPublicId = 'pub-tech';
    component.form.applicationPublicId = 'pub-atlas';
    component.form.allocationPercentage = 80;
    component.filterStatus = 'INACTIVE';

    component.resetAssignmentFilters();

    // Form data must be preserved
    expect(component.form.userPublicId).toBe('pub-tech');
    expect(component.form.applicationPublicId).toBe('pub-atlas');
    expect(component.form.allocationPercentage).toBe(80);
  });

  it('clearAssignmentForm should clear form data but not affect list filters', () => {
    svc.getApplications.and.returnValue(of([]));
    component.form.userPublicId = 'pub-tech';
    component.form.applicationPublicId = 'pub-atlas';
    component.filterTypeCode = 'WEB'; // list filter — should stay

    component.clearAssignmentForm();

    expect(component.form.userPublicId).toBe('');
    expect(component.form.applicationPublicId).toBe('');
    expect(component.filterTypeCode).toBe('WEB'); // unchanged
  });
});
