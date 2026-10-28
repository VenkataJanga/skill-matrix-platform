import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ResetPasswordComponent } from './reset-password.component';
import { AuthService } from '../../../core/services/auth.service';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  function createComponent(queryToken: string | null) {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['resetPassword']);

    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(queryToken ? { token: queryToken } : {}),
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create', () => {
    createComponent('raw-token-value');
    expect(component).toBeTruthy();
  });

  it('flags tokenMissing when no token query param is present', () => {
    createComponent(null);
    expect(component.tokenMissing).toBeTrue();
  });

  it('does not flag tokenMissing when a token is present', () => {
    createComponent('raw-token-value');
    expect(component.tokenMissing).toBeFalse();
  });

  it('marks form invalid when passwords do not match', () => {
    createComponent('raw-token-value');
    component.form.setValue({ newPassword: 'NewPass8!', confirmPassword: 'Different1!' });
    expect(component.form.errors?.['passwordsMismatch']).toBeTrue();
  });

  it('marks form invalid when new password is too short', () => {
    createComponent('raw-token-value');
    component.form.setValue({ newPassword: 'short', confirmPassword: 'short' });
    expect(component.form.invalid).toBeTrue();
  });

  it('does not call service when token is missing', () => {
    createComponent(null);
    component.form.setValue({ newPassword: 'NewPass8!', confirmPassword: 'NewPass8!' });
    component.onSubmit();
    expect(authServiceSpy.resetPassword).not.toHaveBeenCalled();
  });

  it('calls resetPassword and shows success message, then redirects to login', (done) => {
    createComponent('raw-token-value');
    authServiceSpy.resetPassword.and.returnValue(
      of({ success: true, message: 'Password reset successfully. Please sign in with your new password.' } as any)
    );

    component.form.setValue({ newPassword: 'NewPass8!', confirmPassword: 'NewPass8!' });
    component.onSubmit();

    expect(authServiceSpy.resetPassword).toHaveBeenCalledWith({
      token: 'raw-token-value',
      newPassword: 'NewPass8!',
      confirmPassword: 'NewPass8!',
    });
    expect(component.successMessage).toContain('Password reset successfully');

    setTimeout(() => {
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
      done();
    }, 2100);
  });

  it('shows error message when reset fails (invalid/expired token)', () => {
    createComponent('bad-token');
    authServiceSpy.resetPassword.and.returnValue(
      throwError(() => ({
        error: { message: 'This password reset link is invalid or has expired. Please request a new one.' },
      }))
    );

    component.form.setValue({ newPassword: 'NewPass8!', confirmPassword: 'NewPass8!' });
    component.onSubmit();

    expect(component.errorMessage).toContain('invalid or has expired');
  });
});
