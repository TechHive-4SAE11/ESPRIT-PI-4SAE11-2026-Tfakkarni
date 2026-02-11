import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '@/core/auth';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';

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
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  isLoggedIn = false;
  username = '';
  password = '';
  errorMessage = '';
  isLoading = false;
  private readonly platformId = inject(PLATFORM_ID);

  constructor(
    private readonly authService: AuthService,
    private readonly keycloakService: KeycloakService,
    private readonly router: Router,
    private readonly http: HttpClient
  ) {}

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

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const tokenEndpoint = 'http://localhost:8180/realms/techhive/protocol/openid-connect/token';
      console.log('[LOGIN] Requesting token from:', tokenEndpoint);

      const body = new URLSearchParams();
      body.set('grant_type', 'password');
      body.set('client_id', 'tfakkarni-frontend');
      body.set('username', this.username);
      body.set('password', this.password);

      const response = await this.http.post<any>(tokenEndpoint, body.toString(), {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
      }).toPromise();

      console.log('[LOGIN] Token response received:', {
        hasAccessToken: !!response?.access_token,
        hasRefreshToken: !!response?.refresh_token,
        tokenType: response?.token_type,
        expiresIn: response?.expires_in,
      });

      if (response?.access_token) {
        console.log('[LOGIN] Initializing Keycloak with token...');
        await this.keycloakService.init({
          config: {
            url: 'http://localhost:8180',
            realm: 'techhive',
            clientId: 'tfakkarni-frontend',
          },
          initOptions: {
            token: response.access_token,
            refreshToken: response.refresh_token,
            checkLoginIframe: false,
          },
          enableBearerInterceptor: true,
          bearerPrefix: 'Bearer',
        });

        console.log('[LOGIN] Keycloak initialized with token, routing by role...');
        console.log('[LOGIN] User roles:', this.authService.getUserRoles());
        this.authService.routeByRole();
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

  loginWithKeycloakSSO(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.keycloakService.login({
      redirectUri: globalThis.location.origin + '/login',
    }).catch((error: any) => {
      console.error('Keycloak SSO login error:', error);
      this.errorMessage = 'SSO login failed. Please try again.';
    });
  }
}
