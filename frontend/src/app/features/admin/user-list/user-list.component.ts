import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';
import {
  CreateUserRequest,
  PagedResponse,
  UpdateUserRequest,
  UserDetail,
  UserSummary,
} from '../../../core/models/application.model';

const ROLES = ['ADMIN', 'LEAD_MANAGER', 'TECHNICIAN'];

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
})
export class UserListComponent implements OnInit {
  // ---- Paged data ----
  pagedUsers: PagedResponse<UserSummary> | null = null;
  loading = true;
  error: string | null = null;
  successMessage: string | null = null;

  // ---- Filter / sort state ----
  searchTerm = '';
  roleFilter = '';
  statusFilter = '';
  sortBy = 'username';
  sortDirection = 'asc';
  currentPage = 0;
  pageSize = 20;

  // ---- Form state ----
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  editingPublicId: string | null = null;
  formSubmitting = false;
  formError: string | null = null;

  form = this.emptyForm();

  roles = ROLES;

  // ---- Confirm deactivate ----
  confirmDeactivateUser: UserSummary | null = null;

  constructor(private appService: ApplicationService) {}

  ngOnInit(): void {
    this.load();
  }

  // ----------------------------------------------------------------
  // Data loading
  // ----------------------------------------------------------------

  load(): void {
    this.loading = true;
    this.error = null;
    this.appService.getUsersPaged({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      roleCode: this.roleFilter || undefined,
      status: this.statusFilter || undefined,
      sortBy: this.sortBy,
      sortDirection: this.sortDirection,
    }).subscribe({
      next: (data) => {
        this.pagedUsers = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users.';
        this.loading = false;
      },
    });
  }

  // ----------------------------------------------------------------
  // Search / filter / sort
  // ----------------------------------------------------------------

  onSearch(): void {
    this.currentPage = 0;
    this.load();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  onSort(field: string): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
    this.load();
  }

  sortIcon(field: string): string {
    if (this.sortBy !== field) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  // ----------------------------------------------------------------
  // Pagination
  // ----------------------------------------------------------------

  get totalPages(): number {
    return this.pagedUsers?.totalPages ?? 0;
  }

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
    if (start > 1) pages.push(-1); // ellipsis
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push(-1);
    pages.push(total - 1);
    return pages;
  }

  // ----------------------------------------------------------------
  // Add / Edit form
  // ----------------------------------------------------------------

  openAddForm(): void {
    this.formMode = 'create';
    this.editingPublicId = null;
    this.form = this.emptyForm();
    this.formError = null;
    this.showForm = true;
  }

  openEditForm(user: UserSummary): void {
    this.appService.getUserByPublicId(user.publicId).subscribe({
      next: (detail: UserDetail) => {
        this.formMode = 'edit';
        this.editingPublicId = detail.publicId;
        this.form = {
          username: detail.username,
          fullName: detail.fullName,
          email: detail.email,
          roleCode: detail.primaryRoleCode ?? detail.roles[0] ?? '',
          employeeId: detail.employeeId ?? '',
          managerPublicId: detail.managerPublicId ?? '',
          mustChangePassword: detail.mustChangePassword,
        };
        this.formError = null;
        this.showForm = true;
      },
      error: () => {
        this.error = 'Failed to load user details.';
      },
    });
  }

  cancelForm(): void {
    this.showForm = false;
    this.formError = null;
  }

  isFormValid(): boolean {
    return (
      !!this.form.username.trim() &&
      !!this.form.fullName.trim() &&
      !!this.form.email.trim() &&
      !!this.form.roleCode
    );
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    this.formSubmitting = true;
    this.formError = null;

    if (this.formMode === 'create') {
      const req: CreateUserRequest = {
        username: this.form.username.trim(),
        fullName: this.form.fullName.trim(),
        email: this.form.email.trim(),
        roleCode: this.form.roleCode,
        employeeId: this.form.employeeId || undefined,
        managerPublicId: this.form.managerPublicId || undefined,
        mustChangePassword: this.form.mustChangePassword,
      };
      this.appService.createUser(req).subscribe({
        next: (u) => {
          this.successMessage = `User "${u.username}" created. Default password: Password@123`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to create user.';
          this.formSubmitting = false;
        },
      });
    } else {
      const req: UpdateUserRequest = {
        fullName: this.form.fullName.trim(),
        email: this.form.email.trim(),
        roleCode: this.form.roleCode,
        employeeId: this.form.employeeId || undefined,
        managerPublicId: this.form.managerPublicId || undefined,
        mustChangePassword: this.form.mustChangePassword,
      };
      this.appService.updateUser(this.editingPublicId!, req).subscribe({
        next: (u) => {
          this.successMessage = `User "${u.username}" updated successfully.`;
          this.showForm = false;
          this.formSubmitting = false;
          this.load();
        },
        error: (err) => {
          this.formError = err?.error?.message ?? 'Failed to update user.';
          this.formSubmitting = false;
        },
      });
    }
  }

  // ----------------------------------------------------------------
  // Status toggle
  // ----------------------------------------------------------------

  requestDeactivate(user: UserSummary): void {
    this.confirmDeactivateUser = user;
  }

  cancelDeactivate(): void {
    this.confirmDeactivateUser = null;
  }

  confirmDeactivate(): void {
    if (!this.confirmDeactivateUser) return;
    const user = this.confirmDeactivateUser;
    this.confirmDeactivateUser = null;
    this.appService.updateUserStatus(user.publicId, { active: false, reason: 'Deactivated by admin' }).subscribe({
      next: () => {
        this.successMessage = `User "${user.username}" deactivated.`;
        this.load();
      },
      error: () => {
        this.error = 'Failed to deactivate user.';
      },
    });
  }

  activateUser(user: UserSummary): void {
    this.appService.updateUserStatus(user.publicId, { active: true }).subscribe({
      next: () => {
        this.successMessage = `User "${user.username}" activated.`;
        this.load();
      },
      error: () => {
        this.error = 'Failed to activate user.';
      },
    });
  }

  // ----------------------------------------------------------------
  // Reset / Clear Filters
  // ----------------------------------------------------------------

  hasActiveUserFilters(): boolean {
    return (
      !!this.searchTerm ||
      !!this.roleFilter ||
      !!this.statusFilter ||
      this.sortBy !== 'username' ||
      this.sortDirection !== 'asc'
    );
  }

  resetUserFilters(): void {
    this.searchTerm = '';
    this.roleFilter = '';
    this.statusFilter = '';
    this.sortBy = 'username';
    this.sortDirection = 'asc';
    this.currentPage = 0;
    this.load();
  }

  // ----------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'ADMIN':        return 'badge-admin';
      case 'LEAD_MANAGER': return 'badge-lead';
      case 'TECHNICIAN':   return 'badge-tech';
      default:             return 'badge-default';
    }
  }

  dismissSuccess(): void { this.successMessage = null; }
  dismissError(): void   { this.error = null; }

  private emptyForm() {
    return {
      username: '',
      fullName: '',
      email: '',
      roleCode: '',
      employeeId: '',
      managerPublicId: '',
      mustChangePassword: true,
    };
  }
}
