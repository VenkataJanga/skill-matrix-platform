import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { UserListComponent } from './user-list.component';
import { ApplicationService } from '../../../core/services/application.service';
import { PagedResponse, UserSummary } from '../../../core/models/application.model';

describe('UserListComponent', () => {
  let fixture: ComponentFixture<UserListComponent>;
  let component: UserListComponent;
  let svc: jasmine.SpyObj<ApplicationService>;

  const mockUser: UserSummary = {
    publicId: 'pub-admin', username: 'admin', fullName: 'System Admin',
    email: 'admin@nttdata.com', active: true, roles: ['ADMIN'],
  };

  const mockPaged: PagedResponse<UserSummary> = {
    content: [mockUser], page: 0, size: 20,
    totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj('ApplicationService', [
      'getUsersPaged', 'getUserByPublicId', 'createUser', 'updateUser', 'updateUserStatus',
    ]);
    svc.getUsersPaged.and.returnValue(of(mockPaged));

    await TestBed.configureTestingModule({
      imports: [UserListComponent, FormsModule],
      providers: [{ provide: ApplicationService, useValue: svc }],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load users on init', () => {
    expect(component).toBeTruthy();
    expect(svc.getUsersPaged).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({ page: 0, size: 20 })
    );
    expect(component.pagedUsers?.totalElements).toBe(1);
  });

  it('should reset page to 0 when searching', () => {
    component.currentPage = 3;
    component.searchTerm = 'admin';
    component.onSearch();
    expect(component.currentPage).toBe(0);
    expect(svc.getUsersPaged).toHaveBeenCalledTimes(2);
  });

  it('should reset page to 0 when filter changes', () => {
    component.currentPage = 2;
    component.roleFilter = 'ADMIN';
    component.onFilterChange();
    expect(component.currentPage).toBe(0);
  });

  it('should show form in create mode when openAddForm called', () => {
    component.openAddForm();
    expect(component.showForm).toBeTrue();
    expect(component.formMode).toBe('create');
  });

  it('should disable submit when form is invalid', () => {
    component.openAddForm();
    expect(component.isFormValid()).toBeFalse();
  });

  it('should enable submit when all required fields filled', () => {
    component.openAddForm();
    component.form.username = 'newuser';
    component.form.fullName = 'New User';
    component.form.email = 'new@nttdata.com';
    component.form.roleCode = 'TECHNICIAN';
    expect(component.isFormValid()).toBeTrue();
  });

  it('should show deactivate confirmation dialog', () => {
    component.requestDeactivate(mockUser);
    expect(component.confirmDeactivateUser).toBe(mockUser);
  });

  it('should cancel deactivate and clear confirmation', () => {
    component.confirmDeactivateUser = mockUser;
    component.cancelDeactivate();
    expect(component.confirmDeactivateUser).toBeNull();
  });

  // ---- Reset tests ----

  it('hasActiveUserFilters should return false when no filters active', () => {
    component.searchTerm = '';
    component.roleFilter = '';
    component.statusFilter = '';
    component.sortBy = 'username';
    component.sortDirection = 'asc';
    expect(component.hasActiveUserFilters()).toBeFalse();
  });

  it('hasActiveUserFilters should return true when search is set', () => {
    component.searchTerm = 'admin';
    expect(component.hasActiveUserFilters()).toBeTrue();
  });

  it('resetUserFilters should clear all filters, reset page, and reload', () => {
    component.searchTerm = 'test';
    component.roleFilter = 'ADMIN';
    component.statusFilter = 'ACTIVE';
    component.sortBy = 'email';
    component.sortDirection = 'desc';
    component.currentPage = 3;

    component.resetUserFilters();

    expect(component.searchTerm).toBe('');
    expect(component.roleFilter).toBe('');
    expect(component.statusFilter).toBe('');
    expect(component.sortBy).toBe('username');
    expect(component.sortDirection).toBe('asc');
    expect(component.currentPage).toBe(0);
    // load() called after reset — getUsersPaged should have been called 3 times total
    // (1 on init, 1 from resetUserFilters)
    expect(svc.getUsersPaged).toHaveBeenCalledTimes(2);
  });
});
