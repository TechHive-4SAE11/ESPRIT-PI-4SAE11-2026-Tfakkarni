import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { Router } from '@angular/router';

export type AppRole = 'admin' | 'doctor' | 'patient';

const TOKEN_STORAGE_KEY = 'tfk_tokens';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private platformId = inject(PLATFORM_ID);

  constructor(
    private readonly keycloakService: KeycloakService,
    private readonly router: Router
  ) {}

  /**
   * Returns the list of realm-level roles assigned to the logged-in user.
   * Safe to call during SSR (returns empty array).
   */
  getUserRoles(): string[] {
    if (!isPlatformBrowser(this.platformId)) return [];
    try {
      return this.keycloakService.getUserRoles(true);
    } catch {
      return [];
    }
  }

  /**
   * Checks whether the current user holds a specific role.
   */
  hasRole(role: AppRole): boolean {
    return this.getUserRoles().includes(role);
  }

  /**
   * Returns the highest-priority role the user holds.
   * Priority: admin > doctor > patient
   */
  getPrimaryRole(): AppRole | null {
    const roles = this.getUserRoles();
    if (roles.includes('admin')) return 'admin';
    if (roles.includes('doctor')) return 'doctor';
    if (roles.includes('patient')) return 'patient';
    return null;
  }

  /**
   * After login, inspect the user's role and navigate to the correct dashboard.
   */
  routeByRole(): void {
    const role = this.getPrimaryRole();
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
        this.router.navigate(['/access-denied']);
        break;
    }
  }

  /**
   * Returns the full name from the Keycloak token.
   */
  async getUsername(): Promise<string> {
    // Read from the parsed JWT token directly (loadUserProfile requires a KC session cookie)
    const kc = this.keycloakService.getKeycloakInstance();
    const parsed = kc?.tokenParsed;
    if (parsed) {
      const first = parsed['given_name'] ?? parsed['preferred_username'] ?? '';
      const last = parsed['family_name'] ?? '';
      return (first + ' ' + last).trim() || 'User';
    }
    // Fallback
    try {
      const profile = await this.keycloakService.loadUserProfile();
      return (profile.firstName + ' ' + profile.lastName).trim() || 'User';
    } catch {
      return 'User';
    }
  }

  /**
   * Returns the keycloak user ID (sub) from the parsed JWT token.
   */
  getKeycloakId(): string {
    const kc = this.keycloakService.getKeycloakInstance();
    return kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';
  }

  /**
   * Triggers a Keycloak logout and redirects to the app root.
   */
  logout(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.clearTokens();
    this.keycloakService.logout(globalThis.location.origin);
  }

  /**
   * Whether the user is currently logged in.
   */
  isLoggedIn(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;
    return this.keycloakService.isLoggedIn();
  }

  /**
   * Returns the raw Keycloak token (useful for attaching to HTTP requests).
   */
  getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }

  /** Persist tokens to localStorage so page reloads keep the session. */
  saveTokens(accessToken: string, refreshToken: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify({ accessToken, refreshToken }));
  }

  /** Remove persisted tokens (called on logout). */
  clearTokens(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }

  /** Return stored tokens or null. */
  getSavedTokens(): { accessToken: string; refreshToken: string } | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const raw = localStorage.getItem(TOKEN_STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
