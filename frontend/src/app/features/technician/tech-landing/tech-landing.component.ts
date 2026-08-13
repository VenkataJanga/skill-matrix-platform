import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserMe } from '../../../core/models/auth.model';

@Component({
  selector: 'app-tech-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="landing-page">
      <div class="welcome-banner" style="background: linear-gradient(135deg, #4a148c, #7b1fa2)">
        <div class="welcome-icon">🔧</div>
        <div class="welcome-text">
          <h1>Welcome, {{ user?.fullName }}!</h1>
          <p>You are logged in as <strong>Technician</strong>.</p>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-card" style="border-top-color: #7b1fa2">
          <div class="info-card-icon">📝</div>
          <h3>My Self-Assessment</h3>
          <p>Submit your skill self-assessments for open assessment cycles.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card" style="border-top-color: #7b1fa2">
          <div class="info-card-icon">🎯</div>
          <h3>My Skill Gaps</h3>
          <p>View your approved skill gaps and improvement areas.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <div class="info-card" style="border-top-color: #7b1fa2">
          <div class="info-card-icon">📚</div>
          <h3>Training Recommendations</h3>
          <p>View and track your training recommendations.</p>
          <span class="badge badge-coming-soon">Coming Soon</span>
        </div>
        <a routerLink="/technician/my-applications" class="info-card info-card-link" style="border-top-color: #7b1fa2">
          <div class="info-card-icon">🏭</div>
          <h3>My Applications</h3>
          <p>View applications you are assigned to support.</p>
          <span class="badge badge-available">Available</span>
        </a>
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
  styleUrls: ['./tech-landing.component.scss'],
})
export class TechLandingComponent implements OnInit {
  user: UserMe | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((u) => (this.user = u));
  }
}
