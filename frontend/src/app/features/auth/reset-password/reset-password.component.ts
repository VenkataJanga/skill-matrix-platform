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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPwd = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return newPwd && confirm && newPwd !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
})
export class ResetPasswordComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  tokenMissing = false;
  private token = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.tokenMissing = !this.token;

    this.form = this.fb.group(
      {
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
    if (this.tokenMissing || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const { newPassword, confirmPassword } = this.form.value;

    this.authService
      .resetPassword({ token: this.token, newPassword, confirmPassword })
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success) {
            this.successMessage = res.message ?? 'Password reset successfully. Redirecting to sign in...';
            setTimeout(() => this.router.navigate(['/login']), 2000);
          } else {
            this.errorMessage = res.message ?? 'Failed to reset password.';
          }
        },
        error: (err) => {
          this.loading = false;
          const msg = err?.error?.message;
          this.errorMessage = msg ?? 'This reset link is invalid or has expired. Please request a new one.';
        },
      });
  }
}
