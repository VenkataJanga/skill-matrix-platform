import { ApplicationConfig, APP_INITIALIZER, ErrorHandler } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';
import { GlobalErrorHandler } from './core/handlers/global-error.handler';

function initSessionFactory(authService: AuthService): () => Promise<void> {
  return () => authService.initSession();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    {
      provide: APP_INITIALIZER,
      useFactory: initSessionFactory,
      deps: [AuthService],
      multi: true,
    },
  ],
};
