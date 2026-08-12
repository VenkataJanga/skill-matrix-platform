import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="unauth-page">
      <div class="unauth-card">
        <div class="unauth-icon">🚫</div>
        <h1>Access Denied</h1>
        <p>You do not have permission to view this page.</p>
        <p class="hint">Please contact your administrator if you believe this is an error.</p>
        <div class="actions">
          <button class="btn-primary" (click)="goHome()">Go to My Dashboard</button>
          <button class="btn-secondary" (click)="logout()">Logout</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .unauth-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      padding: 1rem;
    }

    .unauth-card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
      padding: 3rem 2.5rem;
      text-align: center;
      max-width: 420px;
      width: 100%;
    }

    .unauth-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    h1 {
      color: #c62828;
      font-size: 1.75rem;
      margin: 0 0 0.75rem;
    }

    p {
      color: #555;
      margin: 0 0 0.5rem;
      font-size: 0.95rem;
    }

    .hint {
      color: #999;
      font-size: 0.85rem;
      margin-bottom: 2rem;
    }

    .actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
    }

    .btn-primary {
      padding: 0.65rem 1.5rem;
      background: #1565c0;
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      &:hover { background: #0d47a1; }
    }

    .btn-secondary {
      padding: 0.65rem 1.5rem;
      background: #fff;
      color: #c62828;
      border: 1.5px solid #c62828;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      &:hover { background: #ffebee; }
    }
  `],
})
export class UnauthorizedComponent {
  constructor(private authService: AuthService, private router: Router) {}

  goHome(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.authService.redirectAfterLogin(user);
    } else {
      this.router.navigate(['/login']);
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
