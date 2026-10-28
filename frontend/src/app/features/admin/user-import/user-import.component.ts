import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';
import {
  UserImportResult,
  UserImportRowPreview,
  UserValidationResult,
} from '../../../core/models/application.model';

@Component({
  selector: 'app-user-import',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-import.component.html',
  styleUrl: './user-import.component.scss',
})
export class UserImportComponent {
  /** Emitted after a successful confirm import so parent can refresh the user list. */
  @Output() importCompleted = new EventEmitter<void>();

  selectedFile: File | null = null;
  validationResult: UserValidationResult | null = null;
  importResult: UserImportResult | null = null;

  validating = false;
  importing  = false;
  error: string | null = null;
  successMessage: string | null = null;

  // ----------------------------------------------------------------
  // Partial-import confirmation modal
  // ----------------------------------------------------------------
  showConfirmModal = false;
  pendingImportMode: 'STRICT' | 'PARTIAL' = 'STRICT';

  constructor(private appService: ApplicationService) {}

  // ----------------------------------------------------------------
  // File selection
  // ----------------------------------------------------------------
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.validationResult = null;
    this.importResult = null;
    this.error = null;
    this.successMessage = null;
    this.showConfirmModal = false;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.validationResult = null;
    this.importResult = null;
    this.error = null;
    this.successMessage = null;
    this.showConfirmModal = false;
  }

  // ----------------------------------------------------------------
  // Validate
  // ----------------------------------------------------------------
  onValidate(): void {
    if (!this.selectedFile) return;
    this.validating = true;
    this.error = null;
    this.importResult = null;
    this.showConfirmModal = false;
    this.appService.validateUserImport(this.selectedFile).subscribe({
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

  // ----------------------------------------------------------------
  // Getters for button states
  // ----------------------------------------------------------------

  /** STRICT confirm: only active when zero error rows. */
  get canConfirmStrict(): boolean {
    return (
      !!this.validationResult &&
      this.validationResult.valid &&
      this.validationResult.totalRows > 0 &&
      !this.importing
    );
  }

  /** PARTIAL import: active when there are errors but at least some valid rows. */
  get canImportPartial(): boolean {
    if (!this.validationResult || this.importing) return false;
    const validRows =
      this.validationResult.insertRows + this.validationResult.updateRows;
    return this.validationResult.errorRows > 0 && validRows > 0;
  }

  // ----------------------------------------------------------------
  // Button click handlers — open modal
  // ----------------------------------------------------------------
  onConfirmImport(): void {
    if (!this.canConfirmStrict) return;
    this.pendingImportMode = 'STRICT';
    this.showConfirmModal = true;
  }

  onImportValidOnly(): void {
    if (!this.canImportPartial) return;
    this.pendingImportMode = 'PARTIAL';
    this.showConfirmModal = true;
  }

  // ----------------------------------------------------------------
  // Modal actions
  // ----------------------------------------------------------------
  cancelModal(): void {
    this.showConfirmModal = false;
  }

  confirmModal(): void {
    this.showConfirmModal = false;
    this.doImport(this.pendingImportMode);
  }

  // ----------------------------------------------------------------
  // Execute import
  // ----------------------------------------------------------------
  private doImport(mode: 'STRICT' | 'PARTIAL'): void {
    if (!this.selectedFile) return;
    this.importing = true;
    this.error = null;
    this.appService.confirmUserImport(this.selectedFile, mode).subscribe({
      next: (result) => {
        this.importResult = result;
        this.importing = false;
        if (result.success) {
          this.successMessage = result.message;
          this.selectedFile = null;
          this.validationResult = null;
          this.importCompleted.emit();
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

  // ----------------------------------------------------------------
  // Modal summary helpers
  // ----------------------------------------------------------------
  get modalValidRowCount(): number {
    if (!this.validationResult) return 0;
    return this.validationResult.insertRows + this.validationResult.updateRows;
  }

  get modalConfirmLabel(): string {
    if (this.pendingImportMode === 'PARTIAL') {
      return `Import ${this.modalValidRowCount} Valid Row${this.modalValidRowCount !== 1 ? 's' : ''}`;
    }
    const total =
      (this.validationResult?.insertRows ?? 0) +
      (this.validationResult?.updateRows ?? 0) +
      (this.validationResult?.skipRows ?? 0);
    return `Confirm Import (${total} rows)`;
  }

  // ----------------------------------------------------------------
  // Error report download (client-side CSV from validation result)
  // ----------------------------------------------------------------
  downloadErrorReport(): void {
    const source = this.validationResult ?? this.buildValidationSnapshot();
    if (!source) return;

    const errorRows = source.rows.filter((r) => r.action === 'ERROR');
    if (errorRows.length === 0) return;

    const headers = [
      'row_number',
      'username',
      'full_name',
      'email',
      'role_code',
      'action',
      'error_reason',
    ];

    const csvLines: string[] = [headers.join(',')];
    for (const row of errorRows) {
      const reason = row.errors?.length
        ? row.errors.join('; ')
        : (row.reason ?? '');
      csvLines.push(
        [
          row.rowNumber,
          this.csvCell(row.username),
          this.csvCell(row.fullName),
          this.csvCell(row.email),
          this.csvCell(row.roleCode),
          row.action,
          this.csvCell(reason),
        ].join(',')
      );
    }

    const blob = new Blob([csvLines.join('\r\n')], {
      type: 'text/csv;charset=utf-8;',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'user_import_error_report.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  /** When import has finished we no longer hold validationResult; reconstruct from importResult rows if needed. */
  private buildValidationSnapshot(): UserValidationResult | null {
    return null; // importResult doesn't carry row detail — download only available pre-import
  }

  get canDownloadErrorReport(): boolean {
    return (
      (!!this.validationResult && this.validationResult.errorRows > 0) ||
      (!!this.importResult && this.importResult.errorRows > 0)
    );
  }

  private csvCell(value: string | null | undefined): string {
    const v = value ?? '';
    const escaped = v.replace(/"/g, '""');
    return escaped.includes(',') || escaped.includes('"') || escaped.includes('\n')
      ? `"${escaped}"`
      : escaped;
  }

  // ----------------------------------------------------------------
  // Row badge helpers
  // ----------------------------------------------------------------
  actionBadgeClass(action: string): string {
    switch (action) {
      case 'INSERT': return 'badge-insert';
      case 'UPDATE': return 'badge-update';
      case 'SKIP':   return 'badge-skip';
      case 'ERROR':  return 'badge-error';
      default:       return '';
    }
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void   { this.error = null; }
}
