import { HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { from, switchMap, of } from 'rxjs';
import { environment } from '@/environments/environment';

/**
 * Functional HTTP interceptor that attaches the Keycloak Bearer token
 * to outgoing HTTP requests targeting the API gateway.
 *
 * Can be used as an alternative/complement to KeycloakBearerInterceptor.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakService);
  const platformId = inject(PLATFORM_ID);

  // Skip during SSR
  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }

  // Only attach tokens to API requests (adjust the prefix as needed)
  const apiUrls = ['/api', environment.apiBaseUrl];
  const isApiRequest = apiUrls.some((url) => req.url.startsWith(url));

  if (!isApiRequest || !keycloakService.isLoggedIn()) {
    return next(req);
  }

  return from(keycloakService.getToken()).pipe(
    switchMap((token) => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
      return next(authReq);
    })
  );
};
