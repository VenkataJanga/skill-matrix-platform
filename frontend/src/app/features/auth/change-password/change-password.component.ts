import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPwd = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return newPwd && confirm && newPwd !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
})
export class ChangePasswordComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group(
      {
        currentPassword: ['', [Validators.required]],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: passwordsMatchValidator }
    );
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const { currentPassword, newPassword, confirmPassword } = this.form.value;

    this.authService.changePassword({ currentPassword, newPassword, confirmPassword }).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.successMessage = 'Password changed successfully. Redirecting...';
          // Reload user and redirect after 2s
          setTimeout(() => {
            this.authService.loadCurrentUser().subscribe({
              next: (meRes) => {
                if (meRes.success && meRes.data) {
                  this.authService.redirectAfterLogin(meRes.data);
                }
              },
              error: () => this.router.navigate(['/login']),
            });
          }, 2000);
        } else {
          this.errorMessage = res.message ?? 'Failed to change password.';
        }
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message;
        this.errorMessage = msg ?? 'An error occurred. Please try again.';
      },
    });
  }

  cancel(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.authService.redirectAfterLogin(user);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
