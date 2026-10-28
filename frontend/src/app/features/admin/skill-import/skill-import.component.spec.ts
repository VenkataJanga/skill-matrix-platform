import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { SkillImportComponent } from './skill-import.component';
import { SkillService } from '../../../core/services/skill.service';
import { SkillImportResult, SkillValidationResult } from '../../../core/models/skill.model';

describe('SkillImportComponent', () => {
  let fixture: ComponentFixture<SkillImportComponent>;
  let component: SkillImportComponent;
  let skillSvc: jasmine.SpyObj<SkillService>;

  beforeEach(async () => {
    skillSvc = jasmine.createSpyObj('SkillService', [
      'downloadTemplate', 'validateImport', 'confirmImport',
    ]);

    await TestBed.configureTestingModule({
      imports: [SkillImportComponent],
      providers: [{ provide: SkillService, useValue: skillSvc }],
    }).compileComponents();

    fixture = TestBed.createComponent(SkillImportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('downloadTemplate should delegate to SkillService', () => {
    component.downloadTemplate();
    expect(skillSvc.downloadTemplate).toHaveBeenCalled();
  });

  it('onFileSelected should set selectedFile and clear previous state', () => {
    const file = new File(['data'], 'skills.xlsx');
    component.validationResult = { valid: false, totalRows: 1, insertRows: 0, updateRows: 0, skipRows: 0, errorRows: 1, rows: [] };
    component.error = 'Some error';

    const event = { target: { files: [file] } } as unknown as Event;
    component.onFileSelected(event);

    expect(component.selectedFile).toBe(file);
    expect(component.validationResult).toBeNull();
    expect(component.error).toBeNull();
  });

  it('clearFile should reset all file/validation/result state', () => {
    component.selectedFile = new File(['x'], 'f.xlsx');
    component.validationResult = { valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [] };
    component.importResult = { success: true, totalRows: 1, insertedRows: 1, updatedRows: 0, skippedRows: 0, errorRows: 0, importHistoryPublicId: null, message: 'ok' };

    component.clearFile();

    expect(component.selectedFile).toBeNull();
    expect(component.validationResult).toBeNull();
    expect(component.importResult).toBeNull();
  });

  // ------------------------------------------------------------------
  // Validate -> preview classification rendering
  // ------------------------------------------------------------------

  it('onValidate should populate validationResult with INSERT/UPDATE/SKIP/ERROR classifications', () => {
    component.selectedFile = new File(['data'], 'skills.xlsx');
    const result: SkillValidationResult = {
      valid: false, totalRows: 4, insertRows: 1, updateRows: 1, skipRows: 1, errorRows: 1,
      rows: [
        { sheetName: '01_COMMON_TECH_SKILLS', rowNumber: 2, skillCode: 'TECH_JAVA', skillName: 'Java', action: 'INSERT', reason: 'New skill', fieldErrors: [] },
        { sheetName: '01_COMMON_TECH_SKILLS', rowNumber: 3, skillCode: 'TECH_SQL', skillName: 'SQL', action: 'UPDATE', reason: 'Will update: description', fieldErrors: [] },
        { sheetName: '01_COMMON_TECH_SKILLS', rowNumber: 4, skillCode: 'TECH_LINUX', skillName: 'Linux', action: 'SKIP', reason: 'Same data already exists', fieldErrors: [] },
        { sheetName: '01_COMMON_TECH_SKILLS', rowNumber: 5, skillCode: null, skillName: null, action: 'ERROR', reason: 'skill_code is required', fieldErrors: ['skill_code is required'] },
      ],
    };
    skillSvc.validateImport.and.returnValue(of(result));

    component.onValidate();

    expect(component.validationResult?.totalRows).toBe(4);
    expect(component.validationResult?.insertRows).toBe(1);
    expect(component.validationResult?.updateRows).toBe(1);
    expect(component.validationResult?.skipRows).toBe(1);
    expect(component.validationResult?.errorRows).toBe(1);
    expect(component.validating).toBeFalse();
  });

  it('onValidate should surface a controlled error message on failure', () => {
    component.selectedFile = new File(['data'], 'wrong.xlsx');
    skillSvc.validateImport.and.returnValue(
      throwError(() => ({ error: { message: 'Incorrect import template. Please use the Skill Import template downloaded from Skill Management.' } }))
    );

    component.onValidate();

    expect(component.error).toContain('Incorrect import template');
    expect(component.validating).toBeFalse();
  });

  // ------------------------------------------------------------------
  // Confirm Import
  // ------------------------------------------------------------------

  it('canConfirm should be false when there are error rows', () => {
    component.validationResult = { valid: false, totalRows: 1, insertRows: 0, updateRows: 0, skipRows: 0, errorRows: 1, rows: [] };
    expect(component.canConfirm).toBeFalse();
  });

  it('canConfirm should be true when there are zero error rows and not currently importing', () => {
    component.validationResult = { valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [] };
    component.importing = false;
    expect(component.canConfirm).toBeTrue();
  });

  it('onConfirmImport should emit importCompleted on success and clear file/validation state', () => {
    component.selectedFile = new File(['data'], 'skills.xlsx');
    component.validationResult = { valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [] };

    const result: SkillImportResult = {
      success: true, totalRows: 1, insertedRows: 1, updatedRows: 0, skippedRows: 0,
      errorRows: 0, importHistoryPublicId: 'pub-h1', message: 'Import successful: 1 inserted, 0 updated, 0 skipped.',
    };
    skillSvc.confirmImport.and.returnValue(of(result));

    let emitted: SkillImportResult | undefined;
    component.importCompleted.subscribe((r) => (emitted = r));

    component.onConfirmImport();

    expect(component.successMessage).toBe(result.message);
    expect(component.selectedFile).toBeNull();
    expect(component.validationResult).toBeNull();
    expect(emitted).toEqual(result);
  });

  it('onConfirmImport should keep the preview visible and show error on failed confirm (no wipe)', () => {
    component.selectedFile = new File(['data'], 'skills.xlsx');
    const validation: SkillValidationResult = {
      valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [],
    };
    component.validationResult = validation;

    const failResult: SkillImportResult = {
      success: false, totalRows: 1, insertedRows: 0, updatedRows: 0, skippedRows: 0,
      errorRows: 1, importHistoryPublicId: null, message: 'Import failed: 1 error row(s) must be fixed.',
    };
    skillSvc.confirmImport.and.returnValue(of(failResult));

    component.onConfirmImport();

    expect(component.error).toBe(failResult.message);
    // Preview must remain visible after a failed confirm
    expect(component.validationResult).toBe(validation);
  });

  it('onConfirmImport should surface a network/server error without destroying the preview', () => {
    component.selectedFile = new File(['data'], 'skills.xlsx');
    const validation: SkillValidationResult = {
      valid: true, totalRows: 1, insertRows: 1, updateRows: 0, skipRows: 0, errorRows: 0, rows: [],
    };
    component.validationResult = validation;
    skillSvc.confirmImport.and.returnValue(throwError(() => ({ error: { message: 'An unexpected error occurred' } })));

    component.onConfirmImport();

    expect(component.error).toBe('An unexpected error occurred');
    expect(component.validationResult).toBe(validation);
  });
});
