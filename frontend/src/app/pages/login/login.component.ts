import { Component, OnInit, inject, PLATFORM_ID, NgZone } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';
import { AuthService } from '@/core/auth';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { RecaptchaModule, RecaptchaFormsModule, RECAPTCHA_LANGUAGE } from 'ng-recaptcha';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ZardButtonComponent,
    ZardCardComponent,
    ZardInputDirective,
    RecaptchaModule,
    RecaptchaFormsModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  providers: [
    {
      provide: RECAPTCHA_LANGUAGE,
      useValue: 'en', // Force English locally
    },
  ],
})
export class LoginComponent implements OnInit {
  isLoggedIn = false;
  username = '';
  password = '';
  errorMessage = '';
  isLoading = false;
  recaptchaToken: string | null = null;
  // Google reCAPTCHA v2 site key
  readonly siteKey = '6LfSr30sAAAAAOd0KqmB__bCHLg3T7FtEdkMkvcr';
  private readonly platformId = inject(PLATFORM_ID);

  constructor(
    private readonly authService: AuthService,
    private readonly keycloakService: KeycloakService,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly ngZone: NgZone
  ) { }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.isLoggedIn = this.authService.isLoggedIn();

    if (this.isLoggedIn) {
      this.authService.routeByRole();
    }
  }

  async onSubmit(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password';
      return;
    }

    if (!this.recaptchaToken) {
      this.errorMessage = 'Please confirm you are not a robot';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const tokenEndpoint = `http://localhost:8280/realms/techhive/protocol/openid-connect/token`;
      console.log('[LOGIN] Requesting token from:', tokenEndpoint);

      const body = new URLSearchParams();
      body.set('grant_type', 'password');
      body.set('client_id', 'tfakkarni-frontend');
      body.set('username', this.username);
      body.set('password', this.password);

      const response = await firstValueFrom(
        this.http.post<any>(tokenEndpoint, body.toString(), {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        })
      );

      console.log('[LOGIN] Token response received:', {
        hasAccessToken: !!response?.access_token,
        hasRefreshToken: !!response?.refresh_token,
        tokenType: response?.token_type,
        expiresIn: response?.expires_in,
      });

      if (response?.access_token) {
        console.log('[LOGIN] Setting token on existing Keycloak instance...');

        // Get the already-initialized Keycloak instance (from APP_INITIALIZER)
        // and set tokens directly — do NOT call init() a second time.
        const kc = this.keycloakService.getKeycloakInstance();

        kc.token = response.access_token;
        kc.refreshToken = response.refresh_token;
        kc.authenticated = true;

        // Parse the JWT payload so keycloak-angular can read roles
        const tokenPayload = response.access_token.split('.')[1];
        kc.tokenParsed = JSON.parse(atob(tokenPayload));
        kc.subject = kc.tokenParsed?.['sub'];
        (kc as any).sessionId = kc.tokenParsed?.['session_state'];

        // Populate realmAccess and resourceAccess so getUserRoles() works
        kc.realmAccess = kc.tokenParsed?.['realm_access'] ?? { roles: [] };
        kc.resourceAccess = kc.tokenParsed?.['resource_access'] ?? {};

        // Persist tokens so page reloads keep the session
        this.authService.saveTokens(response.access_token, response.refresh_token);

        if (response.refresh_token) {
          const refreshPayload = response.refresh_token.split('.')[1];
          kc.refreshTokenParsed = JSON.parse(atob(refreshPayload));
        }

        // Set token expiry so automatic refresh works
        if (kc.tokenParsed?.['exp']) {
          kc.tokenParsed['iat'] = kc.tokenParsed['iat'] ?? Math.floor(Date.now() / 1000);
        }

        console.log('[LOGIN] Token set. Authenticated:', kc.authenticated);
        console.log('[LOGIN] realmAccess:', JSON.stringify(kc.realmAccess));
        console.log('[LOGIN] User roles:', this.authService.getUserRoles());

        // Use NgZone.run to ensure navigation happens inside Angular zone
        this.ngZone.run(() => {
          const role = this.authService.getPrimaryRole();
          console.log('[LOGIN] Primary role:', role);
          switch (role) {
            case 'admin':
              this.router.navigate(['/admin']);
              break;
            case 'doctor':
              this.router.navigate(['/doctor']);
              break;
            case 'patient':
              this.router.navigate(['/patient']);
              break;
            default:
              console.error('[LOGIN] No recognized role, going to access-denied');
              this.router.navigate(['/access-denied']);
              break;
          }
        });
      } else {
        console.error('[LOGIN] No access_token in response:', response);
        this.errorMessage = 'Authentication failed: no token received';
      }
    } catch (error: any) {
      console.error('[LOGIN] Error caught:', error);
      console.error('[LOGIN] Error status:', error?.status);
      console.error('[LOGIN] Error statusText:', error?.statusText);
      console.error('[LOGIN] Error URL:', error?.url);
      console.error('[LOGIN] Error body:', error?.error);
      console.error('[LOGIN] Error message:', error?.message);
      console.error('[LOGIN] Error name:', error?.name);

      if (error.error?.error_description) {
        this.errorMessage = error.error.error_description;
      } else if (error.status === 401) {
        this.errorMessage = 'Invalid username or password';
      } else if (error.status === 0) {
        this.errorMessage = `Cannot connect to authentication server (status=0, url=${error?.url}). Check browser console for CORS/network errors.`;
      } else {
        this.errorMessage = `Login failed (status=${error?.status}): ${error?.message || 'Unknown error'}`;
      }
    } finally {
      this.isLoading = false;
    }
  }
}
