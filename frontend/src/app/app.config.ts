import { APP_INITIALIZER, ApplicationConfig, PLATFORM_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { KeycloakBearerInterceptor, KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideZard } from '@/shared/core/provider/providezard';

/**
 * Initializes Keycloak with the configuration for the esprit-realm.
 * The init runs before the Angular app bootstraps, ensuring the auth
 * state is known before any route guard or component renders.
 * Only runs in the browser (skipped during SSR).
 */
function initializeKeycloak(keycloak: KeycloakService, platformId: object) {
  return () => {
    // Skip Keycloak initialization during SSR
    if (!isPlatformBrowser(platformId)) {
      return Promise.resolve();
    }

    console.log('[KC-INIT] Starting Keycloak initialization...');
    console.log('[KC-INIT] Config:', { url: 'http://localhost:8180', realm: 'techhive', clientId: 'tfakkarni-frontend' });
    console.log('[KC-INIT] silentCheckSsoRedirectUri:', globalThis.location.origin + '/assets/silent-check-sso.html');

    return keycloak
      .init({
        config: {
          url: 'http://localhost:8180',
          realm: 'techhive',
          clientId: 'tfakkarni-frontend',
        },
        initOptions: {
          // check-sso: silently check if the user is already logged in
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri:
            globalThis.location.origin + '/assets/silent-check-sso.html',
          checkLoginIframe: false,
        },
        // Attach the Bearer token to all API calls going to the gateway
        enableBearerInterceptor: true,
        bearerPrefix: 'Bearer',
        // Exclude Keycloak server URLs from the bearer interceptor
        // so login/signup requests aren't intercepted
        bearerExcludedUrls: ['/assets', '/public', 'http://localhost:8180'],
      })
      .then((authenticated) => {
        console.log('[KC-INIT] Keycloak initialized successfully. Authenticated:', authenticated);
        return authenticated;
      })
      .catch((err) => {
        console.error('[KC-INIT] Keycloak init FAILED:', err);
        console.error('[KC-INIT] Error type:', typeof err);
        console.error('[KC-INIT] Error details:', JSON.stringify(err, Object.getOwnPropertyNames(err || {})));
        return false;
      });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withInterceptorsFromDi()),
    provideZard(),
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService, PLATFORM_ID],
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true,
    },
  ],
};
