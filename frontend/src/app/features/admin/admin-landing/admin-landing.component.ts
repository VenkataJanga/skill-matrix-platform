import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserMe } from '../../../core/models/auth.model';

@Component({
  selector: 'app-admin-landing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="landing-page">
      <div class="welcome-banner">
        <div class="welcome-icon">🏠</div>
        <div class="welcome-text">
          <h1>Welcome, {{ user?.fullName }}!</h1>
          <p>You are logged in as <strong>Administrator</strong>.</p>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-card">
          <div class="info-card-icon">👥</div>
          <h3>User Management</h3>
          <p>Manage platform users, roles, and access permissions.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card">
          <div class="info-card-icon">📋</div>
          <h3>Skill Catalogue</h3>
          <p>Define and manage the skill catalogue for the platform.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card">
          <div class="info-card-icon">📊</div>
          <h3>Reports & Analytics</h3>
          <p>View platform-wide skill gap reports and analytics.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card">
          <div class="info-card-icon">⚙️</div>
          <h3>System Settings</h3>
          <p>Configure lookup values and system-level settings.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
      </div>

      <div class="user-detail-card">
        <h3>Your Profile</h3>
        <div class="detail-row" *ngFor="let role of user?.roles">
          <span class="detail-label">Role</span>
          <span class="badge badge-role">{{ role }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Email</span>
          <span>{{ user?.email }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Employee ID</span>
          <span>{{ user?.employeeId ?? '—' }}</span>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./admin-landing.component.scss'],
})
export class AdminLandingComponent implements OnInit {
  user: UserMe | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((u) => (this.user = u));
  }
}
