import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SkillManagementComponent } from './skill-management.component';
import { SkillService } from '../../../core/services/skill.service';
import { ApplicationService } from '../../../core/services/application.service';
import {
  SkillCategory,
  SkillImportResult,
  SkillSummary,
} from '../../../core/models/skill.model';
import { PagedResponse } from '../../../core/models/application.model';

describe('SkillManagementComponent', () => {
  let fixture: ComponentFixture<SkillManagementComponent>;
  let component: SkillManagementComponent;
  let skillSvc: jasmine.SpyObj<SkillService>;
  let appSvc: jasmine.SpyObj<ApplicationService>;

  const mockCategory: SkillCategory = {
    publicId: 'pub-cat-1', categoryCode: 'TECHNICAL', categoryName: 'Technical Skills',
    description: null, displayOrder: 1,
  };

  const mockSkill: SkillSummary = {
    publicId: 'pub-skill-1', skillCode: 'TECH_JAVA', skillName: 'Java',
    description: 'Core Java', categoryCode: 'TECHNICAL', categoryName: 'Technical Skills',
    skillTypeCode: 'TECHNICAL', skillTypeLabel: 'Technical', scopeCode: 'GLOBAL', scopeLabel: 'Global',
    applicationTypeCode: null, applicationTypePublicId: null, bundleCode: null, bundlePublicId: null,
    applicationCode: null, applicationPublicId: null, applicationName: null,
    active: true, createdAt: '', updatedAt: '', createdBy: null, updatedBy: null,
  };

  const pagedSkills: PagedResponse<SkillSummary> = {
    content: [mockSkill], page: 0, size: 20,
    totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
  };

  beforeEach(async () => {
    skillSvc = jasmine.createSpyObj('SkillService', [
      'getCategories', 'getSkillsPaged', 'createSkill', 'updateSkill',
      'updateSkillStatus', 'downloadTemplate', 'exportSkills', 'validateImport', 'confirmImport',
      'getImportHistoryPaged',
    ]);
    appSvc = jasmine.createSpyObj('ApplicationService', [
      'getApplicationTypes', 'getBundles',
    ]);

    skillSvc.getCategories.and.returnValue(of([mockCategory]));
    skillSvc.getSkillsPaged.and.returnValue(of(pagedSkills));
    appSvc.getApplicationTypes.and.returnValue(of([]));
    appSvc.getBundles.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [SkillManagementComponent, FormsModule, RouterModule.forRoot([])],
      providers: [
        { provide: SkillService, useValue: skillSvc },
        { provide: ApplicationService, useValue: appSvc },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SkillManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load skills on init', () => {
    expect(component).toBeTruthy();
    expect(skillSvc.getSkillsPaged).toHaveBeenCalled();
    expect(component.pagedSkills?.totalElements).toBe(1);
  });

  // ------------------------------------------------------------------
  // Toolbar: Download Template / Import Skills / Add Skill
  // ------------------------------------------------------------------

  it('downloadTemplate() should delegate to SkillService.downloadTemplate()', () => {
    component.downloadTemplate();
    expect(skillSvc.downloadTemplate).toHaveBeenCalled();
  });

  it('openImportModal() should show the import modal', () => {
    expect(component.showImportModal).toBeFalse();
    component.openImportModal();
    expect(component.showImportModal).toBeTrue();
  });

  it('closeImportModal() should hide the import modal', () => {
    component.showImportModal = true;
    component.closeImportModal();
    expect(component.showImportModal).toBeFalse();
  });

  it('template should render Download Template, Import Skills, Export Skills and + Add Skill buttons', () => {
    const el: HTMLElement = fixture.nativeElement;
    const text = el.textContent ?? '';
    expect(text).toContain('Download Template');
    expect(text).toContain('Import Skills');
    expect(text).toContain('Export Skills');
    expect(text).toContain('+ Add Skill');
  });

  // ------------------------------------------------------------------
  // Export Skills
  // ------------------------------------------------------------------

  it('exportSkills() should delegate to SkillService.exportSkills() with current filters', () => {
    component.searchTerm = 'java';
    component.filterCategory = 'TECHNICAL';
    component.filterScope = 'GLOBAL';
    component.filterStatus = 'ACTIVE';

    component.exportSkills();

    expect(skillSvc.exportSkills).toHaveBeenCalledWith({
      search: 'java',
      categoryCode: 'TECHNICAL',
      scopeCode: 'GLOBAL',
      status: 'ACTIVE',
    });
  });

  it('exportSkills() should pass undefined for empty filters (export all)', () => {
    component.searchTerm = '';
    component.filterCategory = '';
    component.filterScope = '';
    component.filterStatus = '';

    component.exportSkills();

    expect(skillSvc.exportSkills).toHaveBeenCalledWith({
      search: undefined,
      categoryCode: undefined,
      scopeCode: undefined,
      status: undefined,
    });
  });

  it('clicking Export Skills button should call exportSkills()', () => {
    const el: HTMLElement = fixture.nativeElement;
    const exportBtn = Array.from(el.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Export Skills')) as HTMLButtonElement;
    exportBtn.click();

    expect(skillSvc.exportSkills).toHaveBeenCalled();
  });

  it('clicking Import Skills button should open the import modal and render app-skill-import', () => {
    const el: HTMLElement = fixture.nativeElement;
    const importBtn = Array.from(el.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Import Skills')) as HTMLButtonElement;
    importBtn.click();
    fixture.detectChanges();

    expect(component.showImportModal).toBeTrue();
    expect(el.querySelector('app-skill-import')).toBeTruthy();
  });

  // ------------------------------------------------------------------
  // Import completion -> refresh list + success message
  // ------------------------------------------------------------------

  it('onImportCompleted should show success message, close modal and refresh the skill list', () => {
    component.showImportModal = true;
    skillSvc.getSkillsPaged.calls.reset();

    const result: SkillImportResult = {
      success: true, totalRows: 2, insertedRows: 2, updatedRows: 0, skippedRows: 0,
      errorRows: 0, importHistoryPublicId: 'pub-hist-1',
      message: 'Import successful: 2 inserted, 0 updated, 0 skipped.',
    };

    component.onImportCompleted(result);

    expect(component.successMessage).toBe(result.message);
    expect(component.showImportModal).toBeFalse();
    expect(skillSvc.getSkillsPaged).toHaveBeenCalled();
  });

  // ------------------------------------------------------------------
  // + Add Skill must remain fully functional (regression)
  // ------------------------------------------------------------------

  it('openAddForm should reset the form and show it in create mode', () => {
    component.openAddForm();
    expect(component.showForm).toBeTrue();
    expect(component.formMode).toBe('create');
    expect(component.form.skillCode).toBe('');
  });

  it('submitForm (create) should call createSkill and refresh the list on success', () => {
    component.openAddForm();
    component.form.skillCode = 'NEW_SKILL';
    component.form.skillName = 'New Skill';
    component.form.categoryCode = 'TECHNICAL';
    component.form.scopeCode = 'GLOBAL';
    skillSvc.createSkill.and.returnValue(of({ ...mockSkill, skillCode: 'NEW_SKILL' }));

    component.submitForm();

    expect(skillSvc.createSkill).toHaveBeenCalled();
    expect(component.showForm).toBeFalse();
  });

  it('submitForm (create) should surface backend error message on failure', () => {
    component.openAddForm();
    component.form.skillCode = 'DUP_SKILL';
    component.form.skillName = 'Dup Skill';
    component.form.categoryCode = 'TECHNICAL';
    component.form.scopeCode = 'GLOBAL';
    skillSvc.createSkill.and.returnValue(
      throwError(() => ({ error: { message: 'Skill Code DUP_SKILL already exists.' } }))
    );

    component.submitForm();

    expect(component.formError).toContain('already exists');
    expect(component.formSubmitting).toBeFalse();
  });

  it('openEditForm should populate the form from the existing skill', () => {
    component.openEditForm(mockSkill);
    expect(component.formMode).toBe('edit');
    expect(component.editingPublicId).toBe('pub-skill-1');
    expect(component.form.skillCode).toBe('TECH_JAVA');
  });

  // ------------------------------------------------------------------
  // Deactivate / Activate regression
  // ------------------------------------------------------------------

  it('requestDeactivate/confirmDeactivate should call updateSkillStatus(false) and refresh list', () => {
    skillSvc.updateSkillStatus.and.returnValue(of({ ...mockSkill, active: false }));
    component.requestDeactivate(mockSkill);
    expect(component.confirmDeactivateSkill).toBe(mockSkill);

    component.confirmDeactivate();

    expect(skillSvc.updateSkillStatus).toHaveBeenCalledWith('pub-skill-1', { active: false });
  });

  it('activateSkill should call updateSkillStatus(true) and refresh list', () => {
    skillSvc.updateSkillStatus.and.returnValue(of({ ...mockSkill, active: true }));
    component.activateSkill(mockSkill);
    expect(skillSvc.updateSkillStatus).toHaveBeenCalledWith('pub-skill-1', { active: true });
  });

  // ------------------------------------------------------------------
  // Search / filters regression
  // ------------------------------------------------------------------

  it('onSearch should reset page and reload', () => {
    component.currentPage = 3;
    component.searchTerm = 'JAVA';
    component.onSearch();
    expect(component.currentPage).toBe(0);
    expect(skillSvc.getSkillsPaged).toHaveBeenCalled();
  });

  it('hasActiveFilters should detect active filters', () => {
    component.searchTerm = '';
    component.filterCategory = '';
    component.filterScope = '';
    component.filterStatus = '';
    expect(component.hasActiveFilters()).toBeFalse();
    component.searchTerm = 'JAVA';
    expect(component.hasActiveFilters()).toBeTrue();
  });

  it('resetFilters should clear filters and reload', () => {
    component.searchTerm = 'JAVA';
    component.filterCategory = 'TECHNICAL';
    component.filterScope = 'GLOBAL';
    component.filterStatus = 'ACTIVE';
    component.currentPage = 2;

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.filterCategory).toBe('');
    expect(component.filterScope).toBe('');
    expect(component.filterStatus).toBe('');
    expect(component.currentPage).toBe(0);
  });
});
