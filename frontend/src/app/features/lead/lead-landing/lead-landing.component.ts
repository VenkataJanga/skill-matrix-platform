import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserMe } from '../../../core/models/auth.model';

@Component({
  selector: 'app-lead-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="landing-page">
      <div class="welcome-banner" style="background: linear-gradient(135deg, #004d40, #00897b)">
        <div class="welcome-icon">📊</div>
        <div class="welcome-text">
          <h1>Welcome, {{ user?.fullName }}!</h1>
          <p>You are logged in as <strong>Lead Manager</strong>.</p>
        </div>
      </div>

      <div class="info-grid">
        <a routerLink="/lead/applications" class="info-card info-card-link" style="border-top-color: #00897b">
          <div class="info-card-icon">🗂️</div>
          <h3>Team Applications</h3>
          <p>View applications in your team's scope by type and bundle.</p>
          <span class="badge badge-available">Available</span>
        </a>
        <div class="info-card" style="border-top-color: #00897b">
          <div class="info-card-icon">🔍</div>
          <h3>Review Assessments</h3>
          <p>Review and approve technician self-assessments for your team.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card" style="border-top-color: #00897b">
          <div class="info-card-icon">📈</div>
          <h3>Team Skill Gaps</h3>
          <p>View skill gap reports for your team across applications.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card" style="border-top-color: #00897b">
          <div class="info-card-icon">🎓</div>
          <h3>Training Plans</h3>
          <p>Manage and confirm training recommendations for your team.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card" style="border-top-color: #00897b">
          <div class="info-card-icon">📅</div>
          <h3>Assessment Cycles</h3>
          <p>Manage assessment cycles for your assigned applications.</p>
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
  styleUrls: ['./lead-landing.component.scss'],
})
export class LeadLandingComponent implements OnInit {
  user: UserMe | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((u) => (this.user = u));
  }
}
