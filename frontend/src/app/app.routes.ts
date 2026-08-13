import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Public routes
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./features/unauthorized/unauthorized.component').then(
        (m) => m.UnauthorizedComponent
      ),
  },

  // Protected routes inside MainLayout
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/layout/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    children: [
      // Change password — any authenticated user
      {
        path: 'change-password',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/auth/change-password/change-password.component').then(
            (m) => m.ChangePasswordComponent
          ),
      },

      // -------------------------------------------------------
      // ADMIN routes
      // -------------------------------------------------------
      {
        path: 'admin',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/admin-landing/admin-landing.component').then(
            (m) => m.AdminLandingComponent
          ),
      },
      {
        path: 'admin/users',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/user-list/user-list.component').then(
            (m) => m.UserListComponent
          ),
      },
      {
        path: 'admin/assign',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/technician-assign/technician-assign.component').then(
            (m) => m.TechnicianAssignComponent
          ),
      },

      // -------------------------------------------------------
      // LEAD MANAGER routes
      // -------------------------------------------------------
      {
        path: 'lead',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['LEAD_MANAGER'] },
        loadComponent: () =>
          import('./features/lead/lead-landing/lead-landing.component').then(
            (m) => m.LeadLandingComponent
          ),
      },
      {
        path: 'lead/applications',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['LEAD_MANAGER'] },
        loadComponent: () =>
          import('./features/lead/team-applications/team-applications.component').then(
            (m) => m.TeamApplicationsComponent
          ),
      },

      // -------------------------------------------------------
      // TECHNICIAN routes
      // -------------------------------------------------------
      {
        path: 'technician',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['TECHNICIAN'] },
        loadComponent: () =>
          import('./features/technician/tech-landing/tech-landing.component').then(
            (m) => m.TechLandingComponent
          ),
      },
      {
        path: 'technician/my-applications',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['TECHNICIAN'] },
        loadComponent: () =>
          import('./features/technician/my-applications/my-applications.component').then(
            (m) => m.MyApplicationsComponent
          ),
      },
    ],
  },

  // Catch-all → login
  { path: '**', redirectTo: '/login' },
];
