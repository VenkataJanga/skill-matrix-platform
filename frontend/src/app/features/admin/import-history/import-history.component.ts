import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SkillService } from '../../../core/services/skill.service';
import { ImportHistory } from '../../../core/models/skill.model';
import { PagedResponse } from '../../../core/models/application.model';

@Component({
  selector: 'app-import-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './import-history.component.html',
  styleUrl: './import-history.component.scss',
})
export class ImportHistoryComponent implements OnInit {
  pagedHistory: PagedResponse<ImportHistory> | null = null;
  loading = true;
  error: string | null = null;

  filterStatus = '';
  filterUploadedBy = '';
  filterFromDate = '';
  filterToDate = '';
  currentPage = 0;
  pageSize = 20;

  constructor(private skillService: SkillService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.skillService.getImportHistoryPaged({
      page: this.currentPage,
      size: this.pageSize,
      templateType: 'SKILLS_IMPORT',
      statusCode: this.filterStatus || undefined,
      uploadedBy: this.filterUploadedBy || undefined,
      fromDate: this.filterFromDate || undefined,
      toDate: this.filterToDate || undefined,
    }).subscribe({
      next: (data) => {
        this.pagedHistory = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load import history.';
        this.loading = false;
      },
    });
  }

  onFilterChange(): void { this.currentPage = 0; this.load(); }

  hasActiveFilters(): boolean {
    return !!this.filterStatus || !!this.filterUploadedBy || !!this.filterFromDate || !!this.filterToDate;
  }

  resetFilters(): void {
    this.filterStatus = '';
    this.filterUploadedBy = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.currentPage = 0;
    this.load();
  }

  get totalPages(): number { return this.pagedHistory?.totalPages ?? 0; }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.load();
  }

  statusClass(statusCode: string | null): string {
    if (statusCode === 'SUCCESS') return 'status-active';
    if (statusCode === 'FAILED')  return 'status-inactive';
    return 'text-muted';
  }
}
