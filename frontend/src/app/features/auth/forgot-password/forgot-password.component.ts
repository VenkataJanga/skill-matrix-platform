import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
})
export class ForgotPasswordComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  successMessage = '';
  devResetLink: string | null = null;
  errorMessage = '';

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
    });
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

    this.authService.forgotPassword(this.form.value).subscribe({
      next: (res) => {
        this.loading = false;
        this.submitted = true;
        if (res.success && res.data) {
          this.successMessage = res.data.message;
          this.devResetLink = res.data.devResetLink ?? null;
        } else {
          this.successMessage =
            'If an active account matches the information provided, password reset instructions will be sent to the registered email address.';
        }
      },
      error: () => {
        this.loading = false;
        // Still show the generic message — never reveal backend errors that could leak account existence.
        this.submitted = true;
        this.successMessage =
          'If an active account matches the information provided, password reset instructions will be sent to the registered email address.';
      },
    });
  }

  backToLogin(): void {
    this.router.navigate(['/login']);
  }
}
