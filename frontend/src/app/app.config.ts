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
    console.log('[KC-INIT] Config:', { url: 'http://localhost:8280', realm: 'techhive', clientId: 'tfakkarni-frontend' });
    console.log('[KC-INIT] silentCheckSsoRedirectUri:', globalThis.location.origin + '/assets/silent-check-sso.html');

    return keycloak
      .init({
        config: {
          url: 'http://localhost:8280',
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
        bearerExcludedUrls: ['/assets', '/public', 'http://localhost:8280'],
      })
      .then((authenticated) => {
        console.log('[KC-INIT] Keycloak initialized successfully. Authenticated:', authenticated);

        // If check-sso didn't find a session, try restoring tokens from localStorage
        // (direct password grant doesn't create a KC session cookie)
        if (!authenticated) {
          try {
            const raw = localStorage.getItem('tfk_tokens');
            if (raw) {
              const { accessToken, refreshToken } = JSON.parse(raw);
              if (accessToken) {
                const kc = keycloak.getKeycloakInstance();
                kc.token = accessToken;
                kc.refreshToken = refreshToken;
                kc.authenticated = true;

                const payload = accessToken.split('.')[1];
                kc.tokenParsed = JSON.parse(atob(payload));
                kc.subject = kc.tokenParsed?.['sub'];
                (kc as any).sessionId = kc.tokenParsed?.['session_state'];
                kc.realmAccess = kc.tokenParsed?.['realm_access'] ?? { roles: [] };
                kc.resourceAccess = kc.tokenParsed?.['resource_access'] ?? {};

                if (refreshToken) {
                  const rPayload = refreshToken.split('.')[1];
                  kc.refreshTokenParsed = JSON.parse(atob(rPayload));
                }

                // Check if access token is expired, try refresh
                const exp = kc.tokenParsed?.['exp'] ?? 0;
                if (exp * 1000 < Date.now()) {
                  console.log('[KC-INIT] Stored access token expired, attempting refresh...');
                  return keycloak.updateToken(30)
                    .then(() => {
                      console.log('[KC-INIT] Token refreshed successfully');
                      // Save the new tokens
                      const newKc = keycloak.getKeycloakInstance();
                      localStorage.setItem('tfk_tokens', JSON.stringify({
                        accessToken: newKc.token,
                        refreshToken: newKc.refreshToken,
                      }));
                      return true;
                    })
                    .catch(() => {
                      console.log('[KC-INIT] Token refresh failed, clearing stored tokens');
                      localStorage.removeItem('tfk_tokens');
                      kc.authenticated = false;
                      return false;
                    });
                }

                console.log('[KC-INIT] Restored tokens from storage. Authenticated:', kc.authenticated);
                return true;
              }
            }
          } catch (e) {
            console.error('[KC-INIT] Failed to restore tokens from storage:', e);
            localStorage.removeItem('tfk_tokens');
          }
        }

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
