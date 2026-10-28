import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkillService } from '../../../core/services/skill.service';
import { SkillImportResult, SkillValidationResult } from '../../../core/models/skill.model';

@Component({
  selector: 'app-skill-import',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skill-import.component.html',
  styleUrl: './skill-import.component.scss',
})
export class SkillImportComponent {
  /** Emitted after a successful Confirm Import, so a host (e.g. Skill Management) can refresh its list. */
  @Output() importCompleted = new EventEmitter<SkillImportResult>();

  selectedFile: File | null = null;
  validationResult: SkillValidationResult | null = null;
  importResult: SkillImportResult | null = null;

  validating = false;
  importing = false;
  error: string | null = null;
  successMessage: string | null = null;

  constructor(private skillService: SkillService) {}

  downloadTemplate(): void {
    this.skillService.downloadTemplate();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = file;
    this.validationResult = null;
    this.importResult = null;
    this.error = null;
    this.successMessage = null;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.validationResult = null;
    this.importResult = null;
    this.error = null;
    this.successMessage = null;
  }

  onValidate(): void {
    if (!this.selectedFile) return;
    this.validating = true;
    this.error = null;
    this.importResult = null;
    this.skillService.validateImport(this.selectedFile).subscribe({
      next: (result) => {
        this.validationResult = result;
        this.validating = false;
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Validation failed. Please try again.';
        this.validating = false;
      },
    });
  }

  get canConfirm(): boolean {
    // Enable confirm only when there are zero ERROR rows
    return !!this.validationResult && this.validationResult.errorRows === 0
        && this.validationResult.totalRows > 0 && !this.importing;
  }

  onConfirmImport(): void {
    if (!this.selectedFile || !this.canConfirm) return;
    this.importing = true;
    this.error = null;
    this.skillService.confirmImport(this.selectedFile).subscribe({
      next: (result) => {
        this.importResult = result;
        this.importing = false;
        if (result.success) {
          this.successMessage = result.message;
          this.selectedFile = null;
          this.validationResult = null;
          this.importCompleted.emit(result);
        } else {
          this.error = result.message;
        }
      },
      error: (err) => {
        this.error = err?.error?.message ?? 'Import failed. Please try again.';
        this.importing = false;
      },
    });
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void   { this.error = null; }
}
