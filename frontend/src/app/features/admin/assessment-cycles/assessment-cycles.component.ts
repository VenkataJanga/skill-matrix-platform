import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AssessmentCycleService } from '../../../core/services/assessment-cycle.service';
import { RequirementService } from '../../../core/services/requirement.service';
import { ApplicationService } from '../../../core/services/application.service';
import {
  AssessmentCycle,
  CreateAssessmentCycleRequest,
  ReopenAssessmentCycleRequest,
  UpdateAssessmentCycleRequest,
} from '../../../core/models/assessment.model';
import { RequirementVersion } from '../../../core/models/requirement.model';
import { PagedResponse, ApplicationType, Bundle, Application } from '../../../core/models/application.model';

export const QUARTERS = ['Q1', 'Q2', 'Q3', 'Q4'];
export const HALF_YEARS = ['H1', 'H2'];
export const MONTHS = ['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'];
export const MONTH_LABELS: Record<string, string> = {
  JANUARY:'January', FEBRUARY:'February', MARCH:'March', APRIL:'April', MAY:'May', JUNE:'June',
  JULY:'July', AUGUST:'August', SEPTEMBER:'September', OCTOBER:'October', NOVEMBER:'November', DECEMBER:'December',
};

export interface CycleForm {
  typeCode: string;
  bundleCode: string;
  applicationPublicId: string;
  periodTypeCode: string;
  periodValue: string;
  periodYear: number | null;
  customLabel: string;
  cycleName: string;
  requirementVersionPublicId: string;
  cycleStartDate: string;
  cycleEndDate: string;
}

@Component({
  selector: 'app-assessment-cycles',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './assessment-cycles.component.html',
  styleUrl: './assessment-cycles.component.scss',
})
export class AssessmentCyclesComponent implements OnInit {
  pagedCycles: PagedResponse<AssessmentCycle> | null = null;
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  searchTerm = '';
  filterApplicationPublicId = '';
  filterStatusCode = '';
  currentPage = 0;
  pageSize = 20;

  applications: Application[] = [];

  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;

  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  formApplications: Application[] = [];
  formVersions: RequirementVersion[] = [];

  versionsLoading = false;
  versionsError = false;

  /**
   * True once a Requirement Version has been selected AND it has zero ACTIVE
   * skill requirements. Drives the early-warning banner and disables
   * "Save as PLANNED" so Admin cannot create a cycle that would later be
   * rejected by AssessmentCycleService.openCycle() ("Requirement version has
   * zero active skill requirements — cannot open cycle"). That backend
   * Open-time safeguard is unchanged and remains the final authoritative check.
   */
  selectedVersionHasNoActiveRequirements = false;

  duplicateCycle: AssessmentCycle | null = null;

  periodTypes = ['QUARTERLY', 'MONTHLY', 'HALF_YEARLY', 'ANNUAL', 'CUSTOM'];
  periodOptions: { code: string; label: string }[] = [];
  yearOptions: number[] = [];

  form: CycleForm = this.emptyForm();

  confirmOpenCycle: AssessmentCycle | null = null;
  confirmCloseCycle: AssessmentCycle | null = null;

  reopenCycleTarget: AssessmentCycle | null = null;
  reopenReason = '';
  reopenNewEndDate = '';
  reopenSubmitting = false;
  reopenError: string | null = null;

  viewingCycle: AssessmentCycle | null = null;

  constructor(
    private cycleService: AssessmentCycleService,
    private requirementService: RequirementService,
    private appService: ApplicationService
  ) {
    this.yearOptions = this.buildYearOptions();
  }

  ngOnInit(): void {
    this.appService.getApplicationTypes().subscribe((t) => (this.applicationTypes = t));
    this.appService.getBundles().subscribe((b) => (this.bundles = b));
    this.appService.getApplications().subscribe((apps) => (this.applications = apps));
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.cycleService.getCycles({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      applicationPublicId: this.filterApplicationPublicId || undefined,
      statusCode: this.filterStatusCode || undefined,
    }).subscribe({
      next: (data) => { this.pagedCycles = data; this.loading = false; },
      error: () => { this.error = 'Failed to load assessment cycles.'; this.loading = false; },
    });
  }

  onSearch(): void { this.currentPage = 0; this.load(); }
  onFilterChange(): void { this.currentPage = 0; this.load(); }

  hasActiveFilters(): boolean {
    return !!this.searchTerm || !!this.filterApplicationPublicId || !!this.filterStatusCode;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterApplicationPublicId = '';
    this.filterStatusCode = '';
    this.currentPage = 0;
    this.load();
  }

  get totalPages(): number { return this.pagedCycles?.totalPages ?? 0; }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.load();
  }

  private buildYearOptions(): number[] {
    const y = new Date().getFullYear();
    return [y - 1, y, y + 1, y + 2];
  }

  computePeriodOptions(periodTypeCode: string): { code: string; label: string }[] {
    if (periodTypeCode === 'QUARTERLY') return QUARTERS.map((q) => ({ code: q, label: q }));
    if (periodTypeCode === 'MONTHLY') return MONTHS.map((m) => ({ code: m, label: MONTH_LABELS[m] }));
    if (periodTypeCode === 'HALF_YEARLY') return HALF_YEARS.map((h) => ({ code: h, label: h }));
    return [];
  }

  onPeriodTypeChange(): void {
    this.periodOptions = this.computePeriodOptions(this.form.periodTypeCode);
    this.form.periodValue = '';
    this.duplicateCycle = null;
    this.regenerateCycleName();
  }

  onPeriodValueChange(): void { this.duplicateCycle = null; this.regenerateCycleName(); }
  onYearChange(): void { this.duplicateCycle = null; this.regenerateCycleName(); }
  onCustomLabelChange(): void { this.duplicateCycle = null; this.regenerateCycleName(); }

  buildGeneratedName(
    applicationName: string | null,
    periodTypeCode: string,
    periodValue: string,
    year: number | null,
    customLabel: string
  ): string {
    if (!applicationName || !year) return '';
    switch (periodTypeCode) {
      case 'QUARTERLY':
        return periodValue ? `${applicationName} ${periodValue} ${year} Assessment` : '';
      case 'MONTHLY':
        return periodValue ? `${applicationName} ${MONTH_LABELS[periodValue]} ${year} Assessment` : '';
      case 'HALF_YEARLY':
        return periodValue ? `${applicationName} ${periodValue} ${year} Assessment` : '';
      case 'ANNUAL':
        return `${applicationName} ${year} Annual Assessment`;
      case 'CUSTOM':
        return customLabel.trim() ? `${applicationName} ${customLabel.trim()} ${year}` : '';
      default:
        return '';
    }
  }

  private regenerateCycleName(): void {
    const appName = this.formApplications.find((a) => a.publicId === this.form.applicationPublicId)?.applicationName ?? null;
    this.form.cycleName = this.buildGeneratedName(
      appName, this.form.periodTypeCode, this.form.periodValue, this.form.periodYear, this.form.customLabel
    );
  }

  get isAnnual(): boolean { return this.form.periodTypeCode === 'ANNUAL'; }
  get isCustomPeriod(): boolean { return this.form.periodTypeCode === 'CUSTOM'; }

  private emptyForm(): CycleForm {
    return {
      typeCode: '',
      bundleCode: '',
      applicationPublicId: '',
      periodTypeCode: 'QUARTERLY',
      periodValue: '',
      periodYear: new Date().getFullYear(),
      customLabel: '',
      cycleName: '',
      requirementVersionPublicId: '',
      cycleStartDate: '',
      cycleEndDate: '',
    };
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.viewingCycle) this.closeDetails();
    else if (this.confirmOpenCycle) this.cancelOpen();
    else if (this.confirmCloseCycle) this.cancelClose();
    else if (this.reopenCycleTarget) this.cancelReopen();
    else if (this.showForm) this.cancelForm();
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void { this.error = null; }

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.periodOptions = this.computePeriodOptions(this.form.periodTypeCode);
    this.formError = null;
    this.formApplications = [];
    this.formVersions = [];
    this.selectedVersionHasNoActiveRequirements = false;
    this.duplicateCycle = null;
    this.showForm = true;
  }

  openEditForm(cycle: AssessmentCycle): void {
    this.formMode = 'edit';
    this.editingPublicId = cycle.publicId;
    this.form = {
      typeCode: '',
      bundleCode: '',
      applicationPublicId: cycle.applicationPublicId,
      periodTypeCode: cycle.periodTypeCode ?? 'QUARTERLY',
      periodValue: cycle.periodValue ?? '',
      periodYear: cycle.periodYear ?? new Date().getFullYear(),
      customLabel: cycle.periodTypeCode === 'CUSTOM' ? (cycle.periodValue ?? '') : '',
      cycleName: cycle.cycleName,
      requirementVersionPublicId: cycle.requirementVersionPublicId,
      cycleStartDate: cycle.cycleStartDate,
      cycleEndDate: cycle.cycleEndDate,
    };
    this.periodOptions = this.computePeriodOptions(this.form.periodTypeCode);
    this.formError = null;
    this.formApplications = [{ publicId: cycle.applicationPublicId, applicationCode: cycle.applicationCode, applicationName: cycle.applicationName } as Application];
    this.loadEligibleVersions(cycle.applicationPublicId);
    this.duplicateCycle = null;
    this.showForm = true;
  }

  /**
   * "Create Next Cycle" — prefills Application Type/Bundle/Application and computes
   * the NEXT structured period (Period Type/Period/Year) deterministically from the
   * source cycle's own structured period metadata — never by parsing cycleName text.
   * Legacy cycles without period metadata fall back to QUARTERLY/Q1/current year.
   */
  createNextCycle(sourceCycle: AssessmentCycle): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();

    this.form.applicationPublicId = sourceCycle.applicationPublicId;
    const next = this.computeNextPeriod(sourceCycle);
    this.form.periodTypeCode = next.periodTypeCode;
    this.form.periodValue = next.periodValue;
    this.form.periodYear = next.periodYear;
    this.periodOptions = this.computePeriodOptions(this.form.periodTypeCode);

    this.formError = null;
    this.duplicateCycle = null;
    this.formApplications = [{
      publicId: sourceCycle.applicationPublicId,
      applicationCode: sourceCycle.applicationCode,
      applicationName: sourceCycle.applicationName,
    } as Application];
    this.regenerateCycleName();
    this.loadEligibleVersions(sourceCycle.applicationPublicId);
    this.showForm = true;
  }

  /** Computes the next structured period from a source cycle's own period metadata. */
  computeNextPeriod(sourceCycle: AssessmentCycle): { periodTypeCode: string; periodValue: string; periodYear: number } {
    const fallbackYear = new Date().getFullYear();
    const periodType = sourceCycle.periodTypeCode ?? 'QUARTERLY';
    const year = sourceCycle.periodYear ?? fallbackYear;
    const value = sourceCycle.periodValue ?? 'Q1';

    if (periodType === 'QUARTERLY') {
      const idx = QUARTERS.indexOf(value);
      if (idx === -1 || idx === QUARTERS.length - 1) return { periodTypeCode: 'QUARTERLY', periodValue: 'Q1', periodYear: year + 1 };
      return { periodTypeCode: 'QUARTERLY', periodValue: QUARTERS[idx + 1], periodYear: year };
    }
    if (periodType === 'MONTHLY') {
      const idx = MONTHS.indexOf(value);
      if (idx === -1 || idx === MONTHS.length - 1) return { periodTypeCode: 'MONTHLY', periodValue: 'JANUARY', periodYear: year + 1 };
      return { periodTypeCode: 'MONTHLY', periodValue: MONTHS[idx + 1], periodYear: year };
    }
    if (periodType === 'HALF_YEARLY') {
      if (value === 'H1') return { periodTypeCode: 'HALF_YEARLY', periodValue: 'H2', periodYear: year };
      return { periodTypeCode: 'HALF_YEARLY', periodValue: 'H1', periodYear: year + 1 };
    }
    if (periodType === 'ANNUAL') {
      return { periodTypeCode: 'ANNUAL', periodValue: '', periodYear: year + 1 };
    }
    // CUSTOM or unknown — safe default
    return { periodTypeCode: 'QUARTERLY', periodValue: 'Q1', periodYear: fallbackYear };
  }

  /** Application Type changed — clear Bundle, Application AND Requirement Version. */
  onFormTypeChange(): void {
    this.form.bundleCode = '';
    this.form.applicationPublicId = '';
    this.form.requirementVersionPublicId = '';
    this.formVersions = [];
    this.versionsError = false;
    this.formApplications = [];
    this.duplicateCycle = null;
    this.regenerateCycleName();
    this.appService.getApplications(this.form.typeCode || undefined, this.form.bundleCode || undefined)
      .subscribe((apps) => (this.formApplications = apps));
  }

  /** Bundle changed — clear Application AND Requirement Version (Type is left as-is). */
  onFormBundleChange(): void {
    this.form.applicationPublicId = '';
    this.form.requirementVersionPublicId = '';
    this.formVersions = [];
    this.versionsError = false;
    this.formApplications = [];
    this.duplicateCycle = null;
    this.regenerateCycleName();
    this.appService.getApplications(this.form.typeCode || undefined, this.form.bundleCode || undefined)
      .subscribe((apps) => (this.formApplications = apps));
  }

  /** Application changed — clear Requirement Version, fetch fresh eligible versions, regenerate name, revalidate duplicate. */
  onFormApplicationChange(): void {
    this.form.requirementVersionPublicId = '';
    this.formVersions = [];
    this.versionsError = false;
    this.selectedVersionHasNoActiveRequirements = false;
    this.duplicateCycle = null;
    this.regenerateCycleName();
    if (!this.form.applicationPublicId) return;
    this.loadEligibleVersions(this.form.applicationPublicId);
    this.checkDuplicateName();
  }

  /**
   * Requirement Version changed — check whether the selected version has at
   * least one ACTIVE skill requirement (activeRequirementCount > 0). If not,
   * show a warning and disable "Save as PLANNED" (via isFormValid()). This is
   * a UX early-warning only; AssessmentCycleService.openCycle() remains the
   * final authoritative backend safeguard and is unchanged.
   */
  onRequirementVersionChange(): void {
    const selected = this.formVersions.find((v) => v.publicId === this.form.requirementVersionPublicId);
    this.selectedVersionHasNoActiveRequirements = !!selected && selected.activeRequirementCount === 0;
  }

  /**
   * Proactive, advisory-only duplicate-name check using the existing
   * GET /assessment-cycles?applicationPublicId=&search= endpoint. Never blocks
   * submission by itself — the backend 409 on submit remains authoritative.
   */
  checkDuplicateName(): void {
    const name = this.form.cycleName?.trim();
    if (!name || !this.form.applicationPublicId) return;

    this.cycleService.getCycles({
      applicationPublicId: this.form.applicationPublicId,
      search: name,
      size: 50,
    }).subscribe({
      next: (result) => {
        const normalized = name.toLowerCase();
        const match = result.content.find((c) =>
          c.cycleName.trim().toLowerCase() === normalized &&
          c.publicId !== this.editingPublicId
        );
        this.duplicateCycle = match ?? null;
      },
      error: () => { this.duplicateCycle = null; },
    });
  }

  /** Fetches only eligible (active, non-REJECTED) Requirement Versions for the given application. */
  private loadEligibleVersions(applicationPublicId: string): void {
    this.versionsLoading = true;
    this.versionsError = false;
    this.requirementService.getVersionsForApplication(applicationPublicId, true).subscribe({
      next: (versions) => {
        this.formVersions = versions;
        this.versionsLoading = false;
        // Re-evaluate the active-requirements warning now that versions are loaded —
        // covers Edit/Create-Next-Cycle flows where requirementVersionPublicId is
        // already pre-populated before this async response arrives.
        this.onRequirementVersionChange();
      },
      error: () => {
        this.formVersions = [];
        this.versionsError = true;
        this.versionsLoading = false;
      },
    });
  }

  isFormValid(): boolean {
    const periodOk = this.isAnnual
      ? true
      : this.isCustomPeriod
        ? !!this.form.customLabel.trim()
        : !!this.form.periodValue;
    return !!this.form.cycleName.trim()
        && !!this.form.applicationPublicId
        && !!this.form.periodTypeCode
        && periodOk
        && !!this.form.periodYear
        && !!this.form.requirementVersionPublicId
        && !!this.form.cycleStartDate
        && !!this.form.cycleEndDate
        && this.form.cycleEndDate >= this.form.cycleStartDate
        && this.formVersions.length > 0
        && !this.selectedVersionHasNoActiveRequirements;
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    const periodValueToSend = this.isCustomPeriod ? this.form.customLabel.trim() : (this.isAnnual ? null : this.form.periodValue);

    if (this.formMode === 'create') {
      const req: CreateAssessmentCycleRequest = {
        applicationPublicId: this.form.applicationPublicId,
        requirementVersionPublicId: this.form.requirementVersionPublicId,
        cycleName: this.form.cycleName.trim(),
        periodTypeCode: this.form.periodTypeCode,
        periodValue: periodValueToSend,
        periodYear: this.form.periodYear,
        cycleStartDate: this.form.cycleStartDate,
        cycleEndDate: this.form.cycleEndDate,
      };
      this.cycleService.createCycle(req).subscribe({
        next: (c) => {
          this.successMessage = `Assessment cycle "${c.cycleName}" created.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to create assessment cycle.';
          this.formSubmitting = false;
        },
      });
    } else {
      const req: UpdateAssessmentCycleRequest = {
        cycleName: this.form.cycleName.trim(),
        periodTypeCode: this.form.periodTypeCode,
        periodValue: periodValueToSend,
        periodYear: this.form.periodYear,
        cycleStartDate: this.form.cycleStartDate,
        cycleEndDate: this.form.cycleEndDate,
        applicationPublicId: this.form.applicationPublicId,
        requirementVersionPublicId: this.form.requirementVersionPublicId,
      };
      this.cycleService.updateCycle(this.editingPublicId!, req).subscribe({
        next: (c) => {
          this.successMessage = `Assessment cycle "${c.cycleName}" updated.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to update assessment cycle.';
          this.formSubmitting = false;
        },
      });
    }
  }

  cancelForm(): void { this.showForm = false; this.formError = null; }

  // ---- Open / Close ----

  requestOpen(cycle: AssessmentCycle): void { this.confirmOpenCycle = cycle; }
  cancelOpen(): void { this.confirmOpenCycle = null; }

  confirmOpen(): void {
    if (!this.confirmOpenCycle) return;
    const cycle = this.confirmOpenCycle;
    this.confirmOpenCycle = null;
    this.cycleService.openCycle(cycle.publicId).subscribe({
      next: () => { this.successMessage = `Cycle "${cycle.cycleName}" opened.`; this.load(); },
      error: (err) => { this.error = err?.error?.message ?? 'Failed to open cycle.'; },
    });
  }

  requestClose(cycle: AssessmentCycle): void { this.confirmCloseCycle = cycle; }
  cancelClose(): void { this.confirmCloseCycle = null; }

  confirmClose(): void {
    if (!this.confirmCloseCycle) return;
    const cycle = this.confirmCloseCycle;
    this.confirmCloseCycle = null;
    this.cycleService.closeCycle(cycle.publicId).subscribe({
      next: () => { this.successMessage = `Cycle "${cycle.cycleName}" closed.`; this.load(); },
      error: (err) => { this.error = err?.error?.message ?? 'Failed to close cycle.'; },
    });
  }

  // ---- Reopen (CLOSED -> OPEN, M4 stabilization) ----

  canReopen(cycle: AssessmentCycle): boolean {
    return cycle.statusCode === 'CLOSED';
  }

  isClosedCycleExpired(cycle: AssessmentCycle): boolean {
    if (cycle.statusCode !== 'CLOSED' || !cycle.cycleEndDate) return false;
    const today = new Date().toISOString().slice(0, 10);
    return cycle.cycleEndDate < today;
  }

  requestReopen(cycle: AssessmentCycle): void {
    this.reopenCycleTarget = cycle;
    this.reopenReason = '';
    this.reopenNewEndDate = '';
    this.reopenError = null;
  }

  cancelReopen(): void {
    this.reopenCycleTarget = null;
    this.reopenReason = '';
    this.reopenNewEndDate = '';
    this.reopenError = null;
  }

  isReopenValid(): boolean {
    if (!this.reopenCycleTarget || !this.reopenReason.trim()) return false;
    if (this.isClosedCycleExpired(this.reopenCycleTarget) && !this.reopenNewEndDate) return false;
    return true;
  }

  confirmReopen(): void {
    if (!this.reopenCycleTarget || !this.isReopenValid()) return;
    const cycle = this.reopenCycleTarget;
    this.reopenSubmitting = true;
    this.reopenError = null;

    const req: ReopenAssessmentCycleRequest = {
      reason: this.reopenReason.trim(),
      newEndDate: this.reopenNewEndDate || null,
    };

    this.cycleService.reopenCycle(cycle.publicId, req).subscribe({
      next: () => {
        this.successMessage = `Cycle "${cycle.cycleName}" reopened.`;
        this.reopenSubmitting = false;
        this.cancelReopen();
        this.load();
      },
      error: (err) => {
        this.reopenError = err?.error?.message ?? 'Failed to reopen cycle.';
        this.reopenSubmitting = false;
      },
    });
  }

  // ---- Detail view ----

  viewDetails(cycle: AssessmentCycle): void { this.viewingCycle = cycle; }
  closeDetails(): void { this.viewingCycle = null; }

  isExpired(cycle: AssessmentCycle | null): boolean {
    if (!cycle || cycle.statusCode !== 'OPEN' || !cycle.cycleEndDate) return false;
    const today = new Date().toISOString().slice(0, 10);
    return cycle.cycleEndDate < today;
  }
}
