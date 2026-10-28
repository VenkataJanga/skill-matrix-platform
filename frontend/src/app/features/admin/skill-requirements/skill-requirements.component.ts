import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RequirementService } from '../../../core/services/requirement.service';
import { ApplicationService } from '../../../core/services/application.service';
import { SkillService } from '../../../core/services/skill.service';
import {
  CreateSkillRequirementRequest,
  RequirementImportResult,
  RequirementValidationResult,
  RequirementVersion,
  SkillRequirement,
  UpdateSkillRequirementRequest,
} from '../../../core/models/requirement.model';
import { PagedResponse, ApplicationType, Bundle, Application } from '../../../core/models/application.model';
import { SkillSummary } from '../../../core/models/skill.model';

const SKILL_GROUPS = ['COMMON_TECH', 'APP_SPECIFIC', 'VW_TOOL'];
const LEVELS = [1, 2, 3, 4, 5];

@Component({
  selector: 'app-skill-requirements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-requirements.component.html',
  styleUrl: './skill-requirements.component.scss',
})
export class SkillRequirementsComponent implements OnInit {
  // ---- Cascading selectors ----
  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  applications: Application[] = [];
  versions: RequirementVersion[] = [];

  selectedTypeCode = '';
  selectedBundleCode = '';
  selectedApplicationPublicId = '';
  selectedVersionPublicId = '';

  // ---- Requirements list ----
  pagedRequirements: PagedResponse<SkillRequirement> | null = null;
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // ---- Requirement Version cascade state ----
  // True once getVersionsForApplication() has resolved successfully for the
  // currently selected application AND returned zero versions. Used to show
  // a normal (non-error) empty state instead of "Failed to load skill
  // requirements." — selecting an Application must never itself produce
  // an error banner.
  noVersionsAvailable = false;
  versionsLoading = false;
  versionsError: string | null = null;

  // Monotonically increasing request sequence. Bumped by every cascade
  // action (Type/Bundle/Application/Version change). Async callbacks
  // capture the sequence value at request time and compare it against the
  // live value before mutating state, so a stale (out-of-order) HTTP
  // response from a previous selection can never overwrite state set by a
  // newer selection (e.g. rapid ATLAS -> VTS -> AVUS switching).
  private requestSeq = 0;

  searchTerm = '';
  filterSkillGroup = '';
  filterMandatory = '';
  filterStatus = '';
  sortBy = 'skillCode';
  sortDirection = 'asc';
  currentPage = 0;
  pageSize = 20;

  skillGroups = SKILL_GROUPS;
  levels = LEVELS;

  // ---- Skill search for Add/Edit form ----
  skillOptions: SkillSummary[] = [];
  skillSearchTerm = '';

  // ---- Add/Edit form ----
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;
  form = this.emptyForm();

  // ---- New version modal ----
  showVersionForm = false;
  newVersionCode = '';
  newVersionName = '';
  versionFormSubmitting = false;
  versionFormError: string | null = null;

  // ---- Deactivate confirm ----
  confirmDeactivateRequirement: SkillRequirement | null = null;

  // ---- Excel import ----
  showImportPanel = false;
  selectedFile: File | null = null;
  validationResult: RequirementValidationResult | null = null;
  importResult: RequirementImportResult | null = null;
  validating = false;
  importing = false;

  constructor(
    private requirementService: RequirementService,
    private appService: ApplicationService,
    private skillService: SkillService
  ) {}

  ngOnInit(): void {
    this.appService.getApplicationTypes().subscribe((t) => (this.applicationTypes = t));
    this.appService.getBundles().subscribe((b) => (this.bundles = b));
  }

  // ---- Cascading selection ----

  onTypeChange(): void {
    this.requestSeq++;
    this.selectedApplicationPublicId = '';
    this.selectedVersionPublicId = '';
    this.applications = [];
    this.versions = [];
    this.pagedRequirements = null;
    this.error = null;
    this.loading = false;
    this.currentPage = 0;
    this.noVersionsAvailable = false;
    this.versionsError = null;
    this.loadApplications();
  }

  onBundleChange(): void {
    this.requestSeq++;
    this.selectedApplicationPublicId = '';
    this.selectedVersionPublicId = '';
    this.applications = [];
    this.versions = [];
    this.pagedRequirements = null;
    this.error = null;
    this.loading = false;
    this.currentPage = 0;
    this.noVersionsAvailable = false;
    this.versionsError = null;
    this.loadApplications();
  }

  private loadApplications(): void {
    if (!this.selectedTypeCode && !this.selectedBundleCode) return;
    this.appService.getApplications(this.selectedTypeCode || undefined, this.selectedBundleCode || undefined)
      .subscribe((apps) => (this.applications = apps));
  }

  /**
   * Required cascade sequence:
   *   Application selected
   *     -> clear old version / requirements / error immediately
   *     -> load Requirement Versions
   *     -> ignore stale response if selection changed since the request was issued
   *     -> if versions exist, select the first/current valid version
   *     -> load requirements (only then)
   *
   * Selecting an Application must NEVER itself produce
   * "Failed to load skill requirements." — the Requirements API is only
   * ever called once a valid Requirement Version publicId is known.
   */
  onApplicationChange(): void {
    const seq = ++this.requestSeq;

    // Immediate state reset — the previous application's version/
    // requirements/error must disappear the instant a new Application is
    // selected, not "eventually" once a response arrives.
    this.selectedVersionPublicId = '';
    this.versions = [];
    this.pagedRequirements = null;
    this.error = null;
    this.loading = false;
    this.currentPage = 0;
    this.noVersionsAvailable = false;
    this.versionsError = null;

    if (!this.selectedApplicationPublicId) {
      return;
    }

    this.versionsLoading = true;
    this.requirementService.getVersionsForApplication(this.selectedApplicationPublicId)
      .subscribe({
        next: (versions) => {
          // A newer Application/Type/Bundle selection has already occurred —
          // this response is stale and must not overwrite current state.
          if (seq !== this.requestSeq) return;

          this.versionsLoading = false;
          this.versions = versions;

          if (versions.length > 0) {
            this.noVersionsAvailable = false;
            this.selectedVersionPublicId = versions[0].publicId;
            this.load();
          } else {
            // Zero Requirement Versions is a normal state, not an error.
            // Do NOT call the Skill Requirements API.
            this.noVersionsAvailable = true;
          }
        },
        error: () => {
          if (seq !== this.requestSeq) return;
          this.versionsLoading = false;
          this.versionsError = 'Failed to load requirement versions.';
        },
      });
  }

  onVersionChange(): void {
    this.requestSeq++;
    this.error = null;
    this.pagedRequirements = null;
    this.currentPage = 0;
    this.load();
  }

  // ---- New version ----

  openVersionForm(): void {
    this.newVersionCode = '';
    this.newVersionName = '';
    this.versionFormError = null;
    this.showVersionForm = true;
  }

  cancelVersionForm(): void {
    this.showVersionForm = false;
  }

  submitVersionForm(): void {
    if (!this.newVersionCode.trim() || !this.newVersionName.trim()) return;
    this.versionFormSubmitting = true;
    this.versionFormError = null;
    this.requirementService.createVersion({
      applicationPublicId: this.selectedApplicationPublicId,
      versionCode: this.newVersionCode.trim(),
      versionName: this.newVersionName.trim(),
    }).subscribe({
      next: (v) => {
        this.versions.unshift(v);
        this.selectedVersionPublicId = v.publicId;
        this.showVersionForm = false;
        this.versionFormSubmitting = false;
        this.load();
      },
      error: (err) => {
        this.versionFormError = err?.error?.message ?? 'Failed to create version.';
        this.versionFormSubmitting = false;
      },
    });
  }

  // ---- Requirements list ----

  /**
   * Defensive guard: never issue a Skill Requirements request with a
   * null/undefined/empty Requirement Version id. This also protects
   * against stale responses overwriting newer state — the current
   * requestSeq is captured at call time and checked again when the
   * response arrives, so a slow response from a previously-selected
   * Application/Version can never clobber state set by a subsequently
   * selected one.
   */
  load(): void {
    if (!this.selectedVersionPublicId) {
      this.pagedRequirements = null;
      this.loading = false;
      return;
    }
    const seq = this.requestSeq;
    this.loading = true;
    this.error = null;
    this.requirementService.getRequirementsPaged({
      page: this.currentPage,
      size: this.pageSize,
      requirementVersionPublicId: this.selectedVersionPublicId,
      search: this.searchTerm || undefined,
      skillGroup: this.filterSkillGroup || undefined,
      mandatory: this.filterMandatory || undefined,
      status: this.filterStatus || undefined,
      sortBy: this.sortBy,
      sortDirection: this.sortDirection,
    }).subscribe({
      next: (data) => {
        if (seq !== this.requestSeq) return;
        this.pagedRequirements = data;
        this.loading = false;
      },
      error: () => {
        if (seq !== this.requestSeq) return;
        this.error = 'Failed to load skill requirements.';
        this.loading = false;
      },
    });
  }

  onSearch(): void { this.currentPage = 0; this.load(); }
  onFilterChange(): void { this.currentPage = 0; this.load(); }

  onSort(field: string): void {
    this.sortBy = field;
    this.sortDirection = this.sortBy === field && this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.currentPage = 0;
    this.load();
  }

  sortIcon(field: string): string {
    if (this.sortBy !== field) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  hasActiveFilters(): boolean {
    return !!this.searchTerm || !!this.filterSkillGroup || !!this.filterMandatory || !!this.filterStatus;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterSkillGroup = '';
    this.filterMandatory = '';
    this.filterStatus = '';
    this.sortBy = 'skillCode';
    this.sortDirection = 'asc';
    this.currentPage = 0;
    this.load();
  }

  get totalPages(): number { return this.pagedRequirements?.totalPages ?? 0; }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.load();
  }

  pageNumbers(): number[] {
    const total = this.totalPages;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages: number[] = [0];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(total - 2, this.currentPage + 2);
    if (start > 1) pages.push(-1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push(-1);
    pages.push(total - 1);
    return pages;
  }

  // ---- Add/Edit form ----

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.formError = null;
    this.skillSearchTerm = '';
    this.skillOptions = [];
    this.showForm = true;
  }

  openEditForm(req: SkillRequirement): void {
    this.formMode = 'edit';
    this.editingPublicId = req.publicId;
    this.form = {
      skillPublicId: req.skillPublicId,
      skillCodeDisplay: req.skillCode,
      expectedLevel: req.expectedLevel,
      mandatory: req.mandatory,
      criticalityCode: req.criticalityCode ?? '',
    };
    this.formError = null;
    this.showForm = true;
  }

  cancelForm(): void { this.showForm = false; this.formError = null; }

  onSkillSearch(): void {
    if (!this.skillSearchTerm || this.skillSearchTerm.length < 2) {
      this.skillOptions = [];
      return;
    }
    this.skillService.getSkillsPaged({ search: this.skillSearchTerm, size: 20, status: 'ACTIVE' })
      .subscribe((res) => (this.skillOptions = res.content));
  }

  selectSkill(skill: SkillSummary): void {
    this.form.skillPublicId = skill.publicId;
    this.form.skillCodeDisplay = skill.skillCode + ' — ' + skill.skillName;
    this.skillOptions = [];
    this.skillSearchTerm = '';
  }

  isFormValid(): boolean {
    return !!this.form.skillPublicId && this.form.expectedLevel >= 1 && this.form.expectedLevel <= 5;
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    if (this.formMode === 'create') {
      const req: CreateSkillRequirementRequest = {
        requirementVersionPublicId: this.selectedVersionPublicId,
        skillPublicId: this.form.skillPublicId,
        expectedLevel: this.form.expectedLevel,
        mandatory: this.form.mandatory,
        criticalityCode: this.form.criticalityCode || undefined,
      };
      this.requirementService.createRequirement(req).subscribe({
        next: (r) => {
          this.successMessage = `Requirement for "${r.skillCode}" created.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to create requirement.';
          this.formSubmitting = false;
        },
      });
    } else {
      const req: UpdateSkillRequirementRequest = {
        expectedLevel: this.form.expectedLevel,
        mandatory: this.form.mandatory,
        criticalityCode: this.form.criticalityCode || undefined,
      };
      this.requirementService.updateRequirement(this.editingPublicId!, req).subscribe({
        next: (r) => {
          this.successMessage = `Requirement for "${r.skillCode}" updated.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to update requirement.';
          this.formSubmitting = false;
        },
      });
    }
  }

  // ---- Status ----

  requestDeactivate(req: SkillRequirement): void { this.confirmDeactivateRequirement = req; }
  cancelDeactivate(): void { this.confirmDeactivateRequirement = null; }

  confirmDeactivate(): void {
    if (!this.confirmDeactivateRequirement) return;
    const req = this.confirmDeactivateRequirement;
    this.confirmDeactivateRequirement = null;
    this.requirementService.updateStatus(req.publicId, { active: false }).subscribe({
      next: () => { this.successMessage = `Requirement for "${req.skillCode}" deactivated.`; this.load(); },
      error: () => { this.error = 'Failed to deactivate requirement.'; },
    });
  }

  activateRequirement(req: SkillRequirement): void {
    this.requirementService.updateStatus(req.publicId, { active: true }).subscribe({
      next: () => { this.successMessage = `Requirement for "${req.skillCode}" activated.`; this.load(); },
      error: () => { this.error = 'Failed to activate requirement.'; },
    });
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void { this.error = null; }

  private emptyForm() {
    return {
      skillPublicId: '',
      skillCodeDisplay: '',
      expectedLevel: 3,
      mandatory: true,
      criticalityCode: '',
    };
  }

  // ---- Excel Import ----

  toggleImportPanel(): void {
    this.showImportPanel = !this.showImportPanel;
    this.selectedFile = null;
    this.validationResult = null;
    this.importResult = null;
  }

  downloadTemplate(): void {
    this.requirementService.downloadTemplate();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.validationResult = null;
    this.importResult = null;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.validationResult = null;
    this.importResult = null;
  }

  onValidate(): void {
    if (!this.selectedFile) return;
    this.validating = true;
    this.error = null;
    this.importResult = null;
    this.requirementService.validateImport(this.selectedFile).subscribe({
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

  get canConfirmImport(): boolean {
    return !!this.validationResult && this.validationResult.errorRows === 0
        && this.validationResult.totalRows > 0 && !this.importing;
  }

  onConfirmImport(): void {
    if (!this.selectedFile || !this.canConfirmImport) return;
    this.importing = true;
    this.error = null;
    this.requirementService.confirmImport(this.selectedFile).subscribe({
      next: (result) => {
        this.importResult = result;
        this.importing = false;
        if (result.success) {
          this.successMessage = result.message;
          this.selectedFile = null;
          this.validationResult = null;
          this.load();
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
}
