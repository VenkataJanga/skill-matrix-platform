import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { LoginResponse, UserMe } from '../models/auth.model';
import { ApiResponse } from '../models/api-response.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockLoginResponse: LoginResponse = {
    accessToken: 'test-access-token',
    refreshToken: 'test-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    userPublicId: 'pub-001',
    username: 'lead_manager',
    fullName: 'Demo Lead Manager',
    roles: ['LEAD_MANAGER'],
    mustChangePassword: false,
  };

  const mockUserMe: UserMe = {
    publicId: 'pub-001',
    username: 'lead_manager',
    email: 'lead@example.com',
    fullName: 'Demo Lead Manager',
    roles: ['LEAD_MANAGER'],
    permissions: ['APP_VIEW'],
    mustChangePassword: false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('isLoggedIn', () => {
    it('returns false when no token in sessionStorage', () => {
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('returns true when token is present in sessionStorage', () => {
      sessionStorage.setItem('sm_access_token', 'some-token');
      expect(service.isLoggedIn()).toBeTrue();
    });
  });

  describe('getAccessToken', () => {
    it('returns null when no token stored', () => {
      expect(service.getAccessToken()).toBeNull();
    });

    it('returns stored token', () => {
      sessionStorage.setItem('sm_access_token', 'abc123');
      expect(service.getAccessToken()).toBe('abc123');
    });
  });

  describe('login', () => {
    it('stores tokens on successful login', () => {
      const response: ApiResponse<LoginResponse> = {
        success: true,
        data: mockLoginResponse,
      };

      service.login({ username: 'lead_manager', password: 'Password@123' }).subscribe((res) => {
        expect(res.success).toBeTrue();
        expect(service.getAccessToken()).toBe('test-access-token');
        expect(service.getRefreshToken()).toBe('test-refresh-token');
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush(response);
    });

    it('does not store tokens on failed login', () => {
      const response: ApiResponse<LoginResponse> = {
        success: false,
        message: 'Invalid username or password',
      };

      service.login({ username: 'bad', password: 'bad' }).subscribe((res) => {
        expect(res.success).toBeFalse();
        expect(service.getAccessToken()).toBeNull();
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush(response);
    });
  });

  describe('loadCurrentUser', () => {
    it('updates currentUser$ on success', () => {
      sessionStorage.setItem('sm_access_token', 'token');
      const response: ApiResponse<UserMe> = { success: true, data: mockUserMe };

      service.loadCurrentUser().subscribe(() => {
        expect(service.getCurrentUser()).toEqual(mockUserMe);
      });

      const req = httpMock.expectOne('/api/v1/auth/me');
      expect(req.request.method).toBe('GET');
      req.flush(response);
    });

    it('sets currentUser$ to null on error', () => {
      sessionStorage.setItem('sm_access_token', 'token');

      service.loadCurrentUser().subscribe({
        error: () => {
          expect(service.getCurrentUser()).toBeNull();
        },
      });

      const req = httpMock.expectOne('/api/v1/auth/me');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('logout', () => {
    it('clears tokens and currentUser$ on logout', () => {
      sessionStorage.setItem('sm_access_token', 'token');
      sessionStorage.setItem('sm_refresh_token', 'rtoken');

      service.logout();

      // Flush the fire-and-forget logout request
      const req = httpMock.expectOne('/api/v1/auth/logout');
      req.flush({ success: true });

      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('hasRole', () => {
    it('returns false when no user loaded', () => {
      expect(service.hasRole('ADMIN')).toBeFalse();
    });
  });

  describe('initSession', () => {
    it('resolves immediately if not logged in', async () => {
      await expectAsync(service.initSession()).toBeResolved();
    });
  });
});
