import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthService } from '../../../core/services/auth.service';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['forgotPassword']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('marks form invalid when usernameOrEmail is blank', () => {
    component.form.setValue({ usernameOrEmail: '' });
    expect(component.form.invalid).toBeTrue();
  });

  it('does not call service when form is invalid', () => {
    component.form.setValue({ usernameOrEmail: '' });
    component.onSubmit();
    expect(authServiceSpy.forgotPassword).not.toHaveBeenCalled();
  });

  it('shows generic success message on successful response', () => {
    authServiceSpy.forgotPassword.and.returnValue(
      of({
        success: true,
        data: { message: 'If an active account matches the information provided...' },
      } as any)
    );

    component.form.setValue({ usernameOrEmail: 'alice' });
    component.onSubmit();

    expect(component.submitted).toBeTrue();
    expect(component.successMessage).toContain('If an active account matches');
    expect(component.devResetLink).toBeNull();
  });

  it('shows dev reset link only when backend includes it', () => {
    authServiceSpy.forgotPassword.and.returnValue(
      of({
        success: true,
        data: {
          message: 'generic message',
          devResetLink: 'http://localhost:4200/reset-password?token=abc',
        },
      } as any)
    );

    component.form.setValue({ usernameOrEmail: 'alice' });
    component.onSubmit();

    expect(component.devResetLink).toBe('http://localhost:4200/reset-password?token=abc');
  });

  it('shows generic message even when the backend call errors (never reveal account existence)', () => {
    authServiceSpy.forgotPassword.and.returnValue(throwError(() => new Error('network error')));

    component.form.setValue({ usernameOrEmail: 'unknown-user' });
    component.onSubmit();

    expect(component.submitted).toBeTrue();
    expect(component.successMessage).toContain('If an active account matches');
  });
});
