import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject, switchMap, of } from 'rxjs';
import { ApplicationService } from '../../../core/services/application.service';
import {
  Application,
  ApplicationAssignment,
  ApplicationType,
  AssignmentRequest,
  BulkStatusResponse,
  Bundle,
  DuplicateCheckResponse,
  PagedResponse,
  UserSummary,
} from '../../../core/models/application.model';

@Component({
  selector: 'app-technician-assign',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './technician-assign.component.html',
  styleUrl: './technician-assign.component.scss',
})
export class TechnicianAssignComponent implements OnInit {
  // ---- Paged assignments ----
  pagedAssignments: PagedResponse<ApplicationAssignment> | null = null;
  loadingAssignments = true;
  assignmentError: string | null = null;
  successMessage: string | null = null;

  // ---- Filter state ----
  searchTerm = '';
  filterTypeCode = '';
  filterBundleCode = '';
  filterAppPublicId = '';
  filterTechPublicId = '';
  filterStatus = 'ACTIVE';
  currentPage = 0;
  pageSize = 20;

  // ---- Dropdowns ----
  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  applications: Application[] = [];
  technicians: UserSummary[] = [];

  // ---- New assignment form ----
  form = this.emptyForm();
  duplicateCheck: DuplicateCheckResponse | null = null;
  checkingDuplicate = false;
  submitting = false;
  formError: string | null = null;
  private duplicateCheck$ = new Subject<{u: string; a: string}>();

  // ---- Multi-select / bulk ----
  selectedIds: Set<string> = new Set();
  showBulkConfirm = false;
  bulkResult: BulkStatusResponse | null = null;
  bulkReason = '';

  constructor(private appService: ApplicationService) {}

  ngOnInit(): void {
    this.loadDropdowns();
    this.loadAssignments();
    // Real-time duplicate check with debounce
    this.duplicateCheck$.pipe(
      debounceTime(400),
      distinctUntilChanged((a, b) => a.u === b.u && a.a === b.a),
      switchMap(({ u, a }) => {
        if (!u || !a) return of(null);
        this.checkingDuplicate = true;
        return this.appService.checkDuplicate(u, a);
      })
    ).subscribe({
      next: (result) => {
        this.duplicateCheck = result;
        this.checkingDuplicate = false;
      },
      error: () => { this.checkingDuplicate = false; }
    });
  }

  // ----------------------------------------------------------------
  // Dropdown loading
  // ----------------------------------------------------------------

  private loadDropdowns(): void {
    this.appService.getApplicationTypes().subscribe(t => this.applicationTypes = t);
    this.appService.getBundles().subscribe(b => this.bundles = b);
    this.appService.getTechnicians().subscribe(t => this.technicians = t);
    this.appService.getApplications().subscribe(a => this.applications = a);
  }

  onFormTypeChange(): void {
    this.form.applicationPublicId = '';
    this.duplicateCheck = null;
    this.appService.getApplications(
      this.form.typeCode || undefined,
      this.form.bundleCode || undefined
    ).subscribe(a => this.applications = a);
  }

  onFormBundleChange(): void {
    this.form.applicationPublicId = '';
    this.duplicateCheck = null;
    this.appService.getApplications(
      this.form.typeCode || undefined,
      this.form.bundleCode || undefined
    ).subscribe(a => this.applications = a);
  }

  onTechnicianOrAppChange(): void {
    this.duplicateCheck = null;
    if (this.form.userPublicId && this.form.applicationPublicId) {
      this.duplicateCheck$.next({
        u: this.form.userPublicId,
        a: this.form.applicationPublicId
      });
    }
  }

  // ----------------------------------------------------------------
  // Assignment list
  // ----------------------------------------------------------------

  loadAssignments(): void {
    this.loadingAssignments = true;
    this.appService.getAssignmentsPaged({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      typeCode: this.filterTypeCode || undefined,
      bundleCode: this.filterBundleCode || undefined,
      applicationPublicId: this.filterAppPublicId || undefined,
      technicianPublicId: this.filterTechPublicId || undefined,
      status: this.filterStatus || undefined,
    }).subscribe({
      next: (data) => {
        this.pagedAssignments = data;
        this.loadingAssignments = false;
        this.selectedIds.clear();
      },
      error: () => {
        this.assignmentError = 'Failed to load assignments.';
        this.loadingAssignments = false;
      },
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadAssignments();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadAssignments();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= (this.pagedAssignments?.totalPages ?? 0)) return;
    this.currentPage = page;
    this.loadAssignments();
  }

  get totalPages(): number {
    return this.pagedAssignments?.totalPages ?? 0;
  }

  pageNumbers(): number[] {
    const total = this.totalPages;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages: number[] = [0];
    const start = Math.max(1, this.currentPage - 2);
    const end   = Math.min(total - 2, this.currentPage + 2);
    if (start > 1) pages.push(-1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push(-1);
    pages.push(total - 1);
    return pages;
  }

  // ----------------------------------------------------------------
  // Row selection
  // ----------------------------------------------------------------

  toggleSelect(publicId: string): void {
    if (this.selectedIds.has(publicId)) {
      this.selectedIds.delete(publicId);
    } else {
      this.selectedIds.add(publicId);
    }
  }

  isSelected(publicId: string): boolean {
    return this.selectedIds.has(publicId);
  }

  toggleSelectAll(): void {
    if (!this.pagedAssignments) return;
    if (this.selectedIds.size === this.pagedAssignments.content.length) {
      this.selectedIds.clear();
    } else {
      this.pagedAssignments.content.forEach(a => this.selectedIds.add(a.publicId));
    }
  }

  get allSelected(): boolean {
    return !!(this.pagedAssignments && this.pagedAssignments.content.length > 0 &&
              this.selectedIds.size === this.pagedAssignments.content.length);
  }

  // ----------------------------------------------------------------
  // New assignment
  // ----------------------------------------------------------------

  get isAssignDisabled(): boolean {
    return (
      !this.form.userPublicId ||
      !this.form.applicationPublicId ||
      !this.form.effectiveFrom ||
      this.submitting ||
      this.checkingDuplicate ||
      (this.duplicateCheck?.isDuplicate ?? false)
    );
  }

  isFormValid(): boolean {
    return (
      !!this.form.userPublicId &&
      !!this.form.applicationPublicId &&
      this.form.allocationPercentage > 0 &&
      this.form.allocationPercentage <= 100 &&
      !!this.form.effectiveFrom
    );
  }

  onSubmit(): void {
    if (this.isAssignDisabled) return;
    this.submitting = true;
    this.formError = null;
    this.successMessage = null;

    const req: AssignmentRequest = {
      userPublicId: this.form.userPublicId,
      applicationPublicId: this.form.applicationPublicId,
      allocationPercentage: this.form.allocationPercentage,
      effectiveFrom: this.form.effectiveFrom,
      effectiveTo: this.form.effectiveTo || null,
    };

    this.appService.createAssignment(req).subscribe({
      next: (a) => {
        this.successMessage = `Assigned ${a.fullName} to ${a.applicationName}.`;
        this.submitting = false;
        this.resetForm();
        this.loadAssignments();
      },
      error: (err) => {
        this.formError = err?.error?.message ?? 'Failed to create assignment.';
        this.submitting = false;
      },
    });
  }

  private resetForm(): void {
    this.form = this.emptyForm();
    this.duplicateCheck = null;
    this.appService.getApplications().subscribe(a => this.applications = a);
  }

  // ----------------------------------------------------------------
  // Single deactivate
  // ----------------------------------------------------------------

  deactivateSingle(publicId: string, techName: string, appName: string): void {
    if (!confirm(`Remove ${techName} from ${appName}?`)) return;
    this.appService.updateAssignmentStatus(publicId, { active: false, reason: 'Removed by admin' }).subscribe({
      next: () => {
        this.successMessage = 'Assignment removed.';
        this.loadAssignments();
      },
      error: () => { this.assignmentError = 'Failed to remove assignment.'; },
    });
  }

  // ----------------------------------------------------------------
  // Bulk deactivate
  // ----------------------------------------------------------------

  openBulkConfirm(): void {
    this.bulkResult = null;
    this.bulkReason = '';
    this.showBulkConfirm = true;
  }

  cancelBulk(): void {
    this.showBulkConfirm = false;
  }

  confirmBulk(): void {
    this.showBulkConfirm = false;
    const ids = Array.from(this.selectedIds);
    this.appService.bulkUpdateAssignmentStatus({
      assignmentPublicIds: ids,
      active: false,
      reason: this.bulkReason || 'Bulk removed by admin',
    }).subscribe({
      next: (result) => {
        this.bulkResult = result;
        this.successMessage =
          `Bulk remove: ${result.successCount} succeeded, ${result.failedCount} failed.`;
        this.selectedIds.clear();
        this.loadAssignments();
      },
      error: () => { this.assignmentError = 'Bulk operation failed.'; },
    });
  }

  // ----------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------

  // ----------------------------------------------------------------
  // Reset Assignment Filters (list filters only — does NOT touch form)
  // ----------------------------------------------------------------

  hasActiveAssignmentFilters(): boolean {
    return (
      !!this.searchTerm ||
      !!this.filterTypeCode ||
      !!this.filterBundleCode ||
      !!this.filterAppPublicId ||
      !!this.filterTechPublicId ||
      this.filterStatus !== 'ACTIVE'
    );
  }

  resetAssignmentFilters(): void {
    this.searchTerm        = '';
    this.filterTypeCode    = '';
    this.filterBundleCode  = '';
    this.filterAppPublicId = '';
    this.filterTechPublicId= '';
    this.filterStatus      = 'ACTIVE';
    this.currentPage       = 0;
    this.loadAssignments();
  }

  /** Clear the new assignment form (separate from list filter reset). */
  clearAssignmentForm(): void {
    this.form = this.emptyForm();
    this.duplicateCheck = null;
    this.formError = null;
    this.appService.getApplications().subscribe(a => this.applications = a);
  }

  // ----------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------

  dismissSuccess(): void { this.successMessage = null; }
  dismissError():   void { this.assignmentError = null; }

  private emptyForm() {
    return {
      userPublicId: '',
      typeCode: '',
      bundleCode: '',
      applicationPublicId: '',
      allocationPercentage: 100,
      effectiveFrom: new Date().toISOString().split('T')[0],
      effectiveTo: '',
    };
  }
}
