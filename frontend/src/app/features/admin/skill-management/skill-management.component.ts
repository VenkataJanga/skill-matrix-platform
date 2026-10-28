import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SkillService } from '../../../core/services/skill.service';
import { ApplicationService } from '../../../core/services/application.service';
import {
  CreateSkillRequest,
  SkillCategory,
  SkillImportResult,
  SkillSummary,
  UpdateSkillRequest,
} from '../../../core/models/skill.model';
import { PagedResponse } from '../../../core/models/application.model';
import { ApplicationType, Bundle } from '../../../core/models/application.model';
import { SkillImportComponent } from '../skill-import/skill-import.component';

const SKILL_SCOPES = ['GLOBAL', 'APPLICATION_TYPE', 'BUNDLE', 'APPLICATION', 'GENERIC'];
const SKILL_TYPES  = ['TECHNICAL', 'NON_TECHNICAL'];

@Component({
  selector: 'app-skill-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SkillImportComponent],
  templateUrl: './skill-management.component.html',
  styleUrl: './skill-management.component.scss',
})
export class SkillManagementComponent implements OnInit {
  // ---- Paged data ----
  pagedSkills: PagedResponse<SkillSummary> | null = null;
  loading = true;
  error: string | null = null;
  successMessage: string | null = null;

  // ---- Filter state ----
  searchTerm = '';
  filterCategory = '';
  filterScope = '';
  filterStatus = '';
  sortBy = 'skillCode';
  sortDirection = 'asc';
  currentPage = 0;
  pageSize = 20;

  // ---- Dropdown data ----
  categories: SkillCategory[] = [];
  applicationTypes: ApplicationType[] = [];
  bundles: Bundle[] = [];
  scopes = SKILL_SCOPES;
  skillTypes = SKILL_TYPES;

  // ---- Form state ----
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;

  form = this.emptyForm();

  // ---- Deactivate confirm ----
  confirmDeactivateSkill: SkillSummary | null = null;

  // ---- Import modal ----
  showImportModal = false;

  constructor(
    private skillService: SkillService,
    private appService: ApplicationService
  ) {}

  ngOnInit(): void {
    this.loadDropdowns();
    this.load();
  }

  private loadDropdowns(): void {
    this.skillService.getCategories().subscribe(c => this.categories = c);
    this.appService.getApplicationTypes().subscribe(t => this.applicationTypes = t);
    this.appService.getBundles().subscribe(b => this.bundles = b);
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.skillService.getSkillsPaged({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      categoryCode: this.filterCategory || undefined,
      scopeCode: this.filterScope || undefined,
      status: this.filterStatus || undefined,
      sortBy: this.sortBy,
      sortDirection: this.sortDirection,
    }).subscribe({
      next: (data) => {
        this.pagedSkills = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load skills.';
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
    return !!this.searchTerm || !!this.filterCategory || !!this.filterScope || !!this.filterStatus;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterCategory = '';
    this.filterScope = '';
    this.filterStatus = '';
    this.sortBy = 'skillCode';
    this.sortDirection = 'asc';
    this.currentPage = 0;
    this.load();
  }

  get totalPages(): number { return this.pagedSkills?.totalPages ?? 0; }

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
    const end   = Math.min(total - 2, this.currentPage + 2);
    if (start > 1) pages.push(-1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push(-1);
    pages.push(total - 1);
    return pages;
  }

  // ---- Form ----

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.formError = null;
    this.showForm = true;
  }

  openEditForm(skill: SkillSummary): void {
    this.formMode = 'edit';
    this.editingPublicId = skill.publicId;
    this.form = {
      skillCode: skill.skillCode,
      skillName: skill.skillName,
      description: skill.description ?? '',
      categoryCode: skill.categoryCode,
      skillTypeCode: skill.skillTypeCode ?? '',
      scopeCode: skill.scopeCode ?? '',
      applicationTypeCode: skill.applicationTypeCode ?? '',
      bundleCode: skill.bundleCode ?? '',
      applicationPublicId: skill.applicationPublicId ?? '',
    };
    this.formError = null;
    this.showForm = true;
  }

  cancelForm(): void { this.showForm = false; this.formError = null; }

  isFormValid(): boolean {
    return !!this.form.skillCode.trim() && !!this.form.skillName.trim() &&
           !!this.form.categoryCode && !!this.form.scopeCode;
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    if (this.formMode === 'create') {
      const req: CreateSkillRequest = {
        skillCode: this.form.skillCode.trim().toUpperCase(),
        skillName: this.form.skillName.trim(),
        description: this.form.description || undefined,
        categoryCode: this.form.categoryCode,
        skillTypeCode: this.form.skillTypeCode || undefined,
        scopeCode: this.form.scopeCode,
        applicationTypeCode: this.form.applicationTypeCode || undefined,
        bundleCode: this.form.bundleCode || undefined,
        applicationPublicId: this.form.applicationPublicId || undefined,
      };
      this.skillService.createSkill(req).subscribe({
        next: (s) => {
          this.successMessage = `Skill "${s.skillCode}" created.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to create skill.';
          this.formSubmitting = false;
        },
      });
    } else {
      const req: UpdateSkillRequest = {
        skillName: this.form.skillName.trim(),
        description: this.form.description || undefined,
        categoryCode: this.form.categoryCode,
        skillTypeCode: this.form.skillTypeCode || undefined,
        scopeCode: this.form.scopeCode,
        applicationTypeCode: this.form.applicationTypeCode || undefined,
        bundleCode: this.form.bundleCode || undefined,
        applicationPublicId: this.form.applicationPublicId || undefined,
      };
      this.skillService.updateSkill(this.editingPublicId!, req).subscribe({
        next: (s) => {
          this.successMessage = `Skill "${s.skillCode}" updated.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to update skill.';
          this.formSubmitting = false;
        },
      });
    }
  }

  // ---- Status ----

  requestDeactivate(skill: SkillSummary): void { this.confirmDeactivateSkill = skill; }
  cancelDeactivate(): void { this.confirmDeactivateSkill = null; }

  confirmDeactivate(): void {
    if (!this.confirmDeactivateSkill) return;
    const skill = this.confirmDeactivateSkill;
    this.confirmDeactivateSkill = null;
    this.skillService.updateSkillStatus(skill.publicId, { active: false }).subscribe({
      next: () => { this.successMessage = `Skill "${skill.skillCode}" deactivated.`; this.load(); },
      error: () => { this.error = 'Failed to deactivate skill.'; },
    });
  }

  activateSkill(skill: SkillSummary): void {
    this.skillService.updateSkillStatus(skill.publicId, { active: true }).subscribe({
      next: () => { this.successMessage = `Skill "${skill.skillCode}" activated.`; this.load(); },
      error: () => { this.error = 'Failed to activate skill.'; },
    });
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void   { this.error = null; }

  // ---- Import ----

  downloadTemplate(): void { this.skillService.downloadTemplate(); }

  exportSkills(): void {
    this.skillService.exportSkills({
      search: this.searchTerm || undefined,
      categoryCode: this.filterCategory || undefined,
      scopeCode: this.filterScope || undefined,
      status: this.filterStatus || undefined,
    });
  }

  openImportModal(): void { this.showImportModal = true; }
  closeImportModal(): void { this.showImportModal = false; }

  onImportCompleted(result: SkillImportResult): void {
    this.successMessage = result.message;
    this.showImportModal = false;
    this.load();
  }

  private emptyForm() {
    return {
      skillCode: '', skillName: '', description: '',
      categoryCode: '', skillTypeCode: '', scopeCode: '',
      applicationTypeCode: '', bundleCode: '', applicationPublicId: '',
    };
  }
}
