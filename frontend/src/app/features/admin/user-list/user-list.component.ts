import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApplicationService } from '../../../core/services/application.service';
import { UserImportComponent } from '../user-import/user-import.component';
import {
  BulkDeleteRequest,
  BulkDeleteResponse,
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
  imports: [CommonModule, FormsModule, UserImportComponent],
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

  // ---- Import panel state ----
  showImportPanel = false;

  // ---- Confirm deactivate ----
  confirmDeactivateUser: UserSummary | null = null;

  // ---- Checkbox selection ----
  selectedPublicIds: Set<string> = new Set();

  // ---- Delete confirm modal ----
  showDeleteConfirmModal = false;
  /** When set, we're deleting a single user; otherwise uses selectedPublicIds. */
  deleteSingleTarget: UserSummary | null = null;
  deleteReason = '';
  isDeleting = false;

  // ---- Delete summary modal ----
  showDeleteSummary = false;
  deleteSummary: BulkDeleteResponse | null = null;

  // ---- Admin password reset ----
  confirmResetPasswordUser: UserSummary | null = null;
  isResettingPassword = false;
  showResetResultModal = false;
  resetResultDevLink: string | null = null;
  resetResultUsername = '';

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
        // Clear any selections that are no longer visible
        const visibleIds = new Set(data.content.map(u => u.publicId));
        this.selectedPublicIds = new Set(
          [...this.selectedPublicIds].filter(id => visibleIds.has(id))
        );
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
  // Checkbox selection
  // ----------------------------------------------------------------

  get allSelected(): boolean {
    const users = this.pagedUsers?.content ?? [];
    return users.length > 0 && users.every(u => this.selectedPublicIds.has(u.publicId));
  }

  get someSelected(): boolean {
    return this.selectedPublicIds.size > 0;
  }

  toggleSelectAll(): void {
    const users = this.pagedUsers?.content ?? [];
    if (this.allSelected) {
      users.forEach(u => this.selectedPublicIds.delete(u.publicId));
    } else {
      users.forEach(u => this.selectedPublicIds.add(u.publicId));
    }
  }

  toggleSelect(publicId: string): void {
    if (this.selectedPublicIds.has(publicId)) {
      this.selectedPublicIds.delete(publicId);
    } else {
      this.selectedPublicIds.add(publicId);
    }
  }

  isSelected(publicId: string): boolean {
    return this.selectedPublicIds.has(publicId);
  }

  clearSelection(): void {
    this.selectedPublicIds.clear();
  }

  // ----------------------------------------------------------------
  // Delete — open / cancel modal
  // ----------------------------------------------------------------

  requestDeleteSingle(user: UserSummary): void {
    this.deleteSingleTarget = user;
    this.deleteReason = '';
    this.showDeleteConfirmModal = true;
  }

  requestDeleteSelected(): void {
    this.deleteSingleTarget = null;
    this.deleteReason = '';
    this.showDeleteConfirmModal = true;
  }

  cancelDelete(): void {
    this.showDeleteConfirmModal = false;
    this.deleteSingleTarget = null;
    this.deleteReason = '';
  }

  // ----------------------------------------------------------------
  // Delete — execute
  // ----------------------------------------------------------------

  confirmDelete(): void {
    this.isDeleting = true;

    if (this.deleteSingleTarget) {
      const target = this.deleteSingleTarget;
      this.appService.deleteUser(target.publicId, this.deleteReason || undefined).subscribe({
        next: (result) => {
          this.showDeleteConfirmModal = false;
          this.isDeleting = false;
          this.deleteSingleTarget = null;
          const summary: BulkDeleteResponse = {
            requestedCount: 1,
            deletedCount: result.status === 'DELETED' ? 1 : 0,
            blockedCount: result.status === 'BLOCKED' ? 1 : 0,
            failedCount: result.status === 'FAILED' ? 1 : 0,
            results: result.status !== 'DELETED' ? [result] : [],
          };
          this.deleteSummary = summary;
          this.showDeleteSummary = true;
          this.selectedPublicIds.delete(target.publicId);
          this.load();
        },
        error: (err) => {
          this.isDeleting = false;
          this.showDeleteConfirmModal = false;
          this.error = err?.error?.message ?? 'Failed to delete user.';
        },
      });
    } else {
      const req: BulkDeleteRequest = {
        userPublicIds: [...this.selectedPublicIds],
        reason: this.deleteReason || undefined,
      };
      this.appService.bulkDeleteUsers(req).subscribe({
        next: (summary) => {
          this.showDeleteConfirmModal = false;
          this.isDeleting = false;
          this.deleteSummary = summary;
          this.showDeleteSummary = true;
          this.selectedPublicIds.clear();
          this.load();
        },
        error: (err) => {
          this.isDeleting = false;
          this.showDeleteConfirmModal = false;
          this.error = err?.error?.message ?? 'Failed to delete users.';
        },
      });
    }
  }

  closeSummary(): void {
    this.showDeleteSummary = false;
    this.deleteSummary = null;
  }

  get deleteTargetCount(): number {
    return this.deleteSingleTarget ? 1 : this.selectedPublicIds.size;
  }

  get deleteModalTitle(): string {
    return this.deleteSingleTarget
      ? `Delete User "${this.deleteSingleTarget.username}"?`
      : `Delete ${this.selectedPublicIds.size} Selected Users?`;
  }

  // ----------------------------------------------------------------
  // Download / Export / Import
  // ----------------------------------------------------------------

  downloadTemplate(): void {
    this.appService.downloadUserTemplate();
  }

  exportUsers(): void {
    this.appService.exportUsers({
      search: this.searchTerm || undefined,
      roleCode: this.roleFilter || undefined,
      status: this.statusFilter || undefined,
    });
  }

  toggleImportPanel(): void {
    this.showImportPanel = !this.showImportPanel;
  }

  onImportCompleted(): void {
    this.showImportPanel = false;
    this.successMessage = 'Import completed successfully. User list refreshed.';
    this.load();
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

  // ----------------------------------------------------------------
  // Admin-assisted password reset
  // ----------------------------------------------------------------

  requestResetPassword(user: UserSummary): void {
    this.confirmResetPasswordUser = user;
  }

  cancelResetPassword(): void {
    this.confirmResetPasswordUser = null;
  }

  confirmResetPassword(): void {
    if (!this.confirmResetPasswordUser) return;
    const user = this.confirmResetPasswordUser;
    this.isResettingPassword = true;
    this.appService.adminResetPassword(user.publicId).subscribe({
      next: (result) => {
        this.isResettingPassword = false;
        this.confirmResetPasswordUser = null;
        this.resetResultUsername = result.username;
        this.resetResultDevLink = result.devResetLink ?? null;
        this.showResetResultModal = true;
      },
      error: (err) => {
        this.isResettingPassword = false;
        this.confirmResetPasswordUser = null;
        this.error = err?.error?.message ?? 'Failed to initiate password reset.';
      },
    });
  }

  closeResetResultModal(): void {
    this.showResetResultModal = false;
    this.resetResultDevLink = null;
    this.resetResultUsername = '';
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
