import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';
import {
  ApplicationMaster,
  ApplicationType,
  Bundle,
  CreateApplicationRequest,
  PagedResponse,
  UpdateApplicationRequest,
  ApplicationValidationResult,
  ApplicationImportResult,
} from '../../../core/models/application.model';

const LIFECYCLE_STATUSES = ['ACTIVE', 'INACTIVE', 'DEPRECATED', 'FUTURE'];

/** Admin -> Application Master (M4 stabilization enhancement — Problem Statement Part A). */
@Component({
  selector: 'app-application-master',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './application-master.component.html',
  styleUrl: './application-master.component.scss',
})
export class ApplicationMasterComponent implements OnInit {
  // ---- List ----
  pagedApplications: PagedResponse<ApplicationMaster> | null = null;
  loading = true;
  error: string | null = null;
  successMessage: string | null = null;

  searchTerm = '';
  filterTypeCode = '';
  filterBundleCode = '';
  filterPortfolioCode = '';
  filterLifecycleStatus = '';
  filterStatus = '';
  sortBy = 'applicationCode';
  sortDirection = 'asc';
  currentPage = 0;
  pageSize = 20;

  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  lifecycleStatuses = LIFECYCLE_STATUSES;

  // ---- Create/Edit form ----
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;
  form = this.emptyForm();

  // ---- Status / delete confirm ----
  confirmDeactivateApp: ApplicationMaster | null = null;
  confirmDeleteApp: ApplicationMaster | null = null;
  deleteBlockedReason: string | null = null;

  // ---- Import ----
  showImportModal = false;
  selectedFile: File | null = null;
  validationResult: ApplicationValidationResult | null = null;
  importResult: ApplicationImportResult | null = null;
  validating = false;
  importing = false;
  importError: string | null = null;

  constructor(private appService: ApplicationService) {}

  ngOnInit(): void {
    this.appService.getApplicationTypes().subscribe((t) => (this.applicationTypes = t));
    this.appService.getBundles().subscribe((b) => (this.bundles = b));
    this.load();
  }

  // ---- List ----

  load(): void {
    this.loading = true;
    this.error = null;
    this.appService.getApplicationsPaged({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      typeCode: this.filterTypeCode || undefined,
      bundleCode: this.filterBundleCode || undefined,
      portfolioCode: this.filterPortfolioCode || undefined,
      lifecycleStatusCode: this.filterLifecycleStatus || undefined,
      status: this.filterStatus || undefined,
      sortBy: this.sortBy,
      sortDirection: this.sortDirection,
    }).subscribe({
      next: (data) => { this.pagedApplications = data; this.loading = false; },
      error: () => { this.error = 'Failed to load applications.'; this.loading = false; },
    });
  }

  onSearch(): void { this.currentPage = 0; this.load(); }
  onFilterChange(): void { this.currentPage = 0; this.load(); }

  onSort(field: string): void {
    this.sortDirection = this.sortBy === field && this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.sortBy = field;
    this.currentPage = 0;
    this.load();
  }

  sortIcon(field: string): string {
    if (this.sortBy !== field) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  hasActiveFilters(): boolean {
    return !!this.searchTerm || !!this.filterTypeCode || !!this.filterBundleCode
        || !!this.filterPortfolioCode || !!this.filterLifecycleStatus || !!this.filterStatus;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterTypeCode = '';
    this.filterBundleCode = '';
    this.filterPortfolioCode = '';
    this.filterLifecycleStatus = '';
    this.filterStatus = '';
    this.currentPage = 0;
    this.load();
  }

  get totalPages(): number { return this.pagedApplications?.totalPages ?? 0; }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.load();
  }

  // ---- Create / Edit form ----

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.formError = null;
    this.showForm = true;
  }

  openEditForm(app: ApplicationMaster): void {
    this.formMode = 'edit';
    this.editingPublicId = app.publicId;
    this.form = {
      applicationTypeCode: app.typeCode,
      bundleCode: app.bundleCode,
      portfolioCode: app.portfolioCode,
      applicationCode: app.applicationCode,
      applicationName: app.applicationName,
      lifecycleStatusCode: app.lifecycleStatusCode ?? 'ACTIVE',
      description: app.description ?? '',
      remarks: app.remarks ?? '',
    };
    this.formError = null;
    this.showForm = true;
  }

  cancelForm(): void { this.showForm = false; this.formError = null; }

  isFormValid(): boolean {
    return !!this.form.applicationTypeCode && !!this.form.bundleCode && !!this.form.portfolioCode
        && !!this.form.applicationCode.trim() && !!this.form.applicationName.trim();
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    if (this.formMode === 'create') {
      const req: CreateApplicationRequest = {
        applicationTypeCode: this.form.applicationTypeCode,
        bundleCode: this.form.bundleCode,
        portfolioCode: this.form.portfolioCode,
        applicationCode: this.form.applicationCode.trim().toUpperCase(),
        applicationName: this.form.applicationName.trim(),
        lifecycleStatusCode: this.form.lifecycleStatusCode || undefined,
        description: this.form.description || undefined,
        remarks: this.form.remarks || undefined,
      };
      this.appService.createApplication(req).subscribe({
        next: (a) => {
          this.successMessage = `Application "${a.applicationCode}" created.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to create application.';
          this.formSubmitting = false;
        },
      });
    } else {
      const req: UpdateApplicationRequest = {
        applicationTypeCode: this.form.applicationTypeCode || undefined,
        bundleCode: this.form.bundleCode || undefined,
        portfolioCode: this.form.portfolioCode || undefined,
        applicationName: this.form.applicationName.trim(),
        lifecycleStatusCode: this.form.lifecycleStatusCode || undefined,
        description: this.form.description || undefined,
        remarks: this.form.remarks || undefined,
      };
      this.appService.updateApplication(this.editingPublicId!, req).subscribe({
        next: (a) => {
          this.successMessage = `Application "${a.applicationCode}" updated.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to update application.';
          this.formSubmitting = false;
        },
      });
    }
  }

  // ---- Activate / Deactivate ----

  activateApp(app: ApplicationMaster): void {
    this.appService.updateApplicationStatus(app.publicId, { active: true }).subscribe({
      next: () => { this.successMessage = `Application "${app.applicationCode}" activated.`; this.load(); },
      error: () => { this.error = 'Failed to activate application.'; },
    });
  }

  requestDeactivate(app: ApplicationMaster): void { this.confirmDeactivateApp = app; }
  cancelDeactivate(): void { this.confirmDeactivateApp = null; }

  confirmDeactivate(): void {
    if (!this.confirmDeactivateApp) return;
    const app = this.confirmDeactivateApp;
    this.confirmDeactivateApp = null;
    this.appService.updateApplicationStatus(app.publicId, { active: false }).subscribe({
      next: () => { this.successMessage = `Application "${app.applicationCode}" deactivated.`; this.load(); },
      error: () => { this.error = 'Failed to deactivate application.'; },
    });
  }

  // ---- Delete ----

  requestDelete(app: ApplicationMaster): void {
    this.confirmDeleteApp = app;
    this.deleteBlockedReason = null;
  }
  cancelDelete(): void { this.confirmDeleteApp = null; this.deleteBlockedReason = null; }

  confirmDelete(): void {
    if (!this.confirmDeleteApp) return;
    const app = this.confirmDeleteApp;
    this.appService.deleteApplication(app.publicId).subscribe({
      next: (result) => {
        if (result.status === 'DELETED') {
          this.successMessage = `Application "${app.applicationCode}" permanently deleted.`;
          this.confirmDeleteApp = null;
          this.load();
        } else {
          this.deleteBlockedReason = result.reason ?? 'Application cannot be deleted — it has business dependencies.';
        }
      },
      error: (err) => {
        this.deleteBlockedReason = err?.error?.message
          ?? 'Hard delete is disabled in this environment. Use Deactivate instead.';
      },
    });
  }

  // ---- Import ----

  openImportModal(): void {
    this.showImportModal = true;
    this.selectedFile = null;
    this.validationResult = null;
    this.importResult = null;
    this.importError = null;
  }

  closeImportModal(): void { this.showImportModal = false; }

  downloadTemplate(): void { this.appService.downloadApplicationTemplate(); }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.validationResult = null;
    this.importResult = null;
    this.importError = null;
  }

  onValidate(): void {
    if (!this.selectedFile) return;
    this.validating = true;
    this.importError = null;
    this.importResult = null;
    this.appService.validateApplicationImport(this.selectedFile).subscribe({
      next: (result) => {
        this.validationResult = result;
        this.validating = false;
      },
      error: (err) => {
        this.importError = err?.error?.message ?? 'Validation failed. Please try again.';
        this.validating = false;
      },
    });
  }

  get canConfirmImport(): boolean {
    return !!this.validationResult && this.validationResult.errorRows === 0
        && this.validationResult.totalRows > 0 && !this.importing;
  }

  onConfirmImport(): void {
    if (!this.selectedFile || !this.canConfirmImport) return;
    this.importing = true;
    this.importError = null;
    this.appService.confirmApplicationImport(this.selectedFile).subscribe({
      next: (result) => {
        this.importResult = result;
        this.importing = false;
        if (result.success) {
          this.successMessage = result.message;
          this.selectedFile = null;
          this.validationResult = null;
          this.load();
        } else {
          this.importError = result.message;
        }
      },
      error: (err) => {
        this.importError = err?.error?.message ?? 'Import failed. Please try again.';
        this.importing = false;
      },
    });
  }

  exportApplications(): void {
    this.appService.exportApplications({
      search: this.searchTerm || undefined,
      typeCode: this.filterTypeCode || undefined,
      bundleCode: this.filterBundleCode || undefined,
      portfolioCode: this.filterPortfolioCode || undefined,
      lifecycleStatusCode: this.filterLifecycleStatus || undefined,
      status: this.filterStatus || undefined,
    });
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void { this.error = null; }

  private emptyForm() {
    return {
      applicationTypeCode: '',
      bundleCode: '',
      portfolioCode: '',
      applicationCode: '',
      applicationName: '',
      lifecycleStatusCode: 'ACTIVE',
      description: '',
      remarks: '',
    };
  }
}
