import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { Router } from '@angular/router';

export type AppRole = 'admin' | 'doctor' | 'patient';

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
   */
  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles(true); // realmRoles only
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
    const profile = await this.keycloakService.loadUserProfile();
    return profile.firstName + ' ' + profile.lastName;
  }

  /**
   * Triggers a Keycloak logout and redirects to the app root.
   */
  logout(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.keycloakService.logout(globalThis.location.origin);
  }

  /**
   * Whether the user is currently logged in.
   */
  isLoggedIn(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  /**
   * Returns the raw Keycloak token (useful for attaching to HTTP requests).
   */
  getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }
}
