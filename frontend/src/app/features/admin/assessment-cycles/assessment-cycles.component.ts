import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

@Component({
  selector: 'app-assessment-cycles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assessment-cycles.component.html',
  styleUrl: './assessment-cycles.component.scss',
})
export class AssessmentCyclesComponent implements OnInit {
  // ---- List ----
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

  // ---- Form (create/edit) ----
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;

  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  formApplications: Application[] = [];
  formVersions: RequirementVersion[] = [];

  /** Drives the Requirement Version dropdown's 4-state UI (loading / available / empty / error). */
  versionsLoading = false;
  versionsError = false;

  /** Proactive duplicate-name check (advisory UX only — backend remains authoritative). */
  duplicateCycle: AssessmentCycle | null = null;
  suggestedCycleName: string | null = null;

  /** Optional soft warning when the free-text name implies a quarter outside the selected dates. */
  nameDateMismatchWarning: string | null = null;

  form = this.emptyForm();

  // ---- Open/Close confirm ----
  confirmOpenCycle: AssessmentCycle | null = null;
  confirmCloseCycle: AssessmentCycle | null = null;

  // ---- Reopen (CLOSED -> OPEN, M4 stabilization) ----
  reopenCycleTarget: AssessmentCycle | null = null;
  reopenReason = '';
  reopenNewEndDate = '';
  reopenSubmitting = false;
  reopenError: string | null = null;

  // ---- Detail view ----
  viewingCycle: AssessmentCycle | null = null;

  constructor(
    private cycleService: AssessmentCycleService,
    private requirementService: RequirementService,
    private appService: ApplicationService
  ) {}

  ngOnInit(): void {
    this.appService.getApplicationTypes().subscribe((t) => (this.applicationTypes = t));
    this.appService.getBundles().subscribe((b) => (this.bundles = b));
    this.appService.getApplications().subscribe((apps) => (this.applications = apps));
    this.load();
  }

  // ---- List ----

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

  // ---- Create / Edit form ----

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.formError = null;
    this.formApplications = [];
    this.formVersions = [];
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
    this.nameDateMismatchWarning = null;
    this.showForm = true;
  }

  /**
   * "Create Next Cycle" — prefills Application Type/Bundle/Application/Requirement
   * Version from an expired/closed source cycle, but deliberately leaves Cycle Name
   * (pre-populated with a suggested new name only) and dates blank so the Admin must
   * explicitly choose a new assessment period. Never copies the old name verbatim.
   */
  createNextCycle(sourceCycle: AssessmentCycle): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.form.applicationPublicId = sourceCycle.applicationPublicId;
    this.form.cycleName = this.buildSuggestedName(sourceCycle.applicationName, null, null);
    this.formError = null;
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
    this.nameDateMismatchWarning = null;
    this.formApplications = [{
      publicId: sourceCycle.applicationPublicId,
      applicationCode: sourceCycle.applicationCode,
      applicationName: sourceCycle.applicationName,
    } as Application];
    this.loadEligibleVersions(sourceCycle.applicationPublicId);
    this.showForm = true;
  }

  openEditForm(cycle: AssessmentCycle): void {
    this.formMode = 'edit';
    this.editingPublicId = cycle.publicId;
    this.form = {
      typeCode: '',
      bundleCode: '',
      applicationPublicId: cycle.applicationPublicId,
      requirementVersionPublicId: cycle.requirementVersionPublicId,
      cycleName: cycle.cycleName,
      cycleStartDate: cycle.cycleStartDate,
      cycleEndDate: cycle.cycleEndDate,
    };
    this.formError = null;
    this.formApplications = [{ publicId: cycle.applicationPublicId, applicationCode: cycle.applicationCode, applicationName: cycle.applicationName } as Application];
    this.loadEligibleVersions(cycle.applicationPublicId);
    this.showForm = true;
  }

  cancelForm(): void { this.showForm = false; this.formError = null; }

  /** Application Type changed — clear Bundle, Application AND Requirement Version. */
  onFormTypeChange(): void {
    this.form.bundleCode = '';
    this.form.applicationPublicId = '';
    this.form.requirementVersionPublicId = '';
    this.formVersions = [];
    this.versionsError = false;
    this.formApplications = [];
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
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
    this.suggestedCycleName = null;
    this.appService.getApplications(this.form.typeCode || undefined, this.form.bundleCode || undefined)
      .subscribe((apps) => (this.formApplications = apps));
  }

  /** Application changed — clear the Requirement Version, fetch fresh eligible versions, and revalidate duplicate-name state. */
  onFormApplicationChange(): void {
    this.form.requirementVersionPublicId = '';
    this.formVersions = [];
    this.versionsError = false;
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
    if (!this.form.applicationPublicId) return;
    this.loadEligibleVersions(this.form.applicationPublicId);
    this.checkDuplicateName();
  }

  /** Cycle Name changed — clear any outdated duplicate warning and re-check against the (possibly new) name. */
  onCycleNameChange(): void {
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
    this.checkDuplicateName();
    this.checkNameDateMismatch();
  }

  onDatesChange(): void {
    this.checkNameDateMismatch();
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
        if (match) {
          this.duplicateCycle = match;
          this.suggestedCycleName = this.buildSuggestedName(
            match.applicationName,
            this.form.cycleStartDate || null,
            this.form.cycleEndDate || null
          );
        } else {
          this.duplicateCycle = null;
          this.suggestedCycleName = null;
        }
      },
      // Advisory check — a failed lookup must never block the Admin from proceeding.
      error: () => { this.duplicateCycle = null; this.suggestedCycleName = null; },
    });
  }

  /** Applies the suggested alternate cycle name to the form and clears the warning. */
  useSuggestedName(): void {
    if (!this.suggestedCycleName) return;
    this.form.cycleName = this.suggestedCycleName;
    this.duplicateCycle = null;
    this.suggestedCycleName = null;
    this.checkNameDateMismatch();
  }

  /**
   * Simple, safe name-suggestion rule (no quarter-calculation logic introduced):
   * "{ApplicationName} {StartMonthAbbrev}-{EndMonthAbbrev} {Year} Assessment".
   * Falls back to a generic "(New Period)" suffix when no dates are available yet
   * (e.g. right after clicking "Create Next Cycle" before dates are chosen).
   */
  private buildSuggestedName(applicationName: string, startDate: string | null, endDate: string | null): string {
    if (!startDate || !endDate) {
      return `${applicationName} Assessment (New Period)`;
    }
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const start = new Date(startDate);
    const end = new Date(endDate);
    const startAbbrev = months[start.getUTCMonth()];
    const endAbbrev = months[end.getUTCMonth()];
    const year = end.getUTCFullYear();
    return `${applicationName} ${startAbbrev}-${endAbbrev} ${year} Assessment`;
  }

  /**
   * Optional lightweight warning (never a hard error): if the free-text cycle name
   * contains a quarter marker like "Q3 2026" but the selected dates fall entirely
   * outside that calendar quarter, flag it for the Admin to double-check.
   */
  private checkNameDateMismatch(): void {
    this.nameDateMismatchWarning = null;
    const match = this.form.cycleName?.match(/Q([1-4])\s?(\d{4})/i);
    if (!match || !this.form.cycleStartDate || !this.form.cycleEndDate) return;

    const quarter = parseInt(match[1], 10);
    const year = parseInt(match[2], 10);
    const quarterStartMonth = (quarter - 1) * 3; // 0-indexed
    const quarterStart = new Date(Date.UTC(year, quarterStartMonth, 1));
    const quarterEnd = new Date(Date.UTC(year, quarterStartMonth + 3, 0));

    const selStart = new Date(this.form.cycleStartDate);
    const selEnd = new Date(this.form.cycleEndDate);

    const overlaps = selStart <= quarterEnd && selEnd >= quarterStart;
    if (!overlaps) {
      this.nameDateMismatchWarning =
        `The cycle name indicates Q${quarter} ${year}, but the selected assessment dates are outside that period. Please verify the cycle name.`;
    }
  }

  /** Fetches only eligible (active, non-REJECTED) Requirement Versions for the given application. */
  private loadEligibleVersions(applicationPublicId: string): void {
    this.versionsLoading = true;
    this.versionsError = false;
    this.requirementService.getVersionsForApplication(applicationPublicId, true).subscribe({
      next: (versions) => {
        this.formVersions = versions;
        this.versionsLoading = false;
      },
      error: () => {
        this.formVersions = [];
        this.versionsError = true;
        this.versionsLoading = false;
      },
    });
  }

  isFormValid(): boolean {
    return !!this.form.cycleName.trim()
        && !!this.form.applicationPublicId
        && !!this.form.requirementVersionPublicId
        && !!this.form.cycleStartDate
        && !!this.form.cycleEndDate
        && this.form.cycleEndDate >= this.form.cycleStartDate
        && this.formVersions.length > 0;
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    if (this.formMode === 'create') {
      const req: CreateAssessmentCycleRequest = {
        applicationPublicId: this.form.applicationPublicId,
        requirementVersionPublicId: this.form.requirementVersionPublicId,
        cycleName: this.form.cycleName.trim(),
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

  /** Only CLOSED cycles may be reopened — never shown for PLANNED/OPEN/CANCELLED. */
  canReopen(cycle: AssessmentCycle): boolean {
    return cycle.statusCode === 'CLOSED';
  }

  /** True when a CLOSED cycle's end date has already passed — the Reopen modal
   *  must then require a valid future End Date before allowing the transition. */
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
        // Keep the modal open on failure (e.g. expired-without-new-end-date 409)
        // so the Admin can supply a valid End Date and retry without re-entering the reason.
        this.reopenError = err?.error?.message ?? 'Failed to reopen cycle.';
        this.reopenSubmitting = false;
      },
    });
  }

  // ---- Detail view ----

  viewDetails(cycle: AssessmentCycle): void { this.viewingCycle = cycle; }
  closeDetails(): void { this.viewingCycle = null; }

  /**
   * True when an OPEN cycle's end date has passed. Purely a UI/Admin-clarity
   * indicator — does NOT alter the persisted cycle status (backend remains OPEN
   * until an Admin explicitly closes it).
   */
  isExpired(cycle: AssessmentCycle | null): boolean {
    if (!cycle || cycle.statusCode !== 'OPEN' || !cycle.cycleEndDate) return false;
    const today = new Date().toISOString().slice(0, 10);
    return cycle.cycleEndDate < today;
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

  private emptyForm() {
    return {
      typeCode: '',
      bundleCode: '',
      applicationPublicId: '',
      requirementVersionPublicId: '',
      cycleName: '',
      cycleStartDate: '',
      cycleEndDate: '',
    };
  }
}
