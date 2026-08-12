import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  errorMessage = '';
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Already logged in → redirect
    if (this.authService.isLoggedIn()) {
      const user = this.authService.getCurrentUser();
      if (user) {
        this.authService.redirectAfterLogin(user);
        return;
      }
    }

    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          // If password change required, go to change-password
          if (res.data.mustChangePassword) {
            this.loading = false;
            this.router.navigate(['/change-password']);
            return;
          }
          // Load full user profile then redirect
          this.authService.loadCurrentUser().subscribe({
            next: (meRes) => {
              this.loading = false;
              if (meRes.success && meRes.data) {
                this.authService.redirectAfterLogin(meRes.data);
              }
            },
            error: () => {
              this.loading = false;
              this.errorMessage = 'Failed to load user profile. Please try again.';
            },
          });
        } else {
          this.loading = false;
          this.errorMessage = res.message ?? 'Login failed. Please try again.';
        }
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message;
        if (err.status === 401) {
          this.errorMessage = msg ?? 'Invalid username or password.';
        } else if (err.status === 0) {
          this.errorMessage = 'Cannot connect to server. Please check your connection.';
        } else {
          this.errorMessage = msg ?? 'An unexpected error occurred. Please try again.';
        }
      },
    });
  }
}
