import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

/**
 * AuthGuard that checks if the user is authenticated AND holds at least
 * one of the roles specified in the route's `data.roles` array.
 *
 * Usage in route config:
 * ```
 * {
 *   path: 'admin-dashboard',
 *   component: AdminDashboardComponent,
 *   canActivate: [AuthGuard],
 *   data: { roles: ['admin'] }
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class AuthGuard extends KeycloakAuthGuard {
  private platformId = inject(PLATFORM_ID);

  constructor(
    protected override readonly router: Router,
    protected readonly keycloak: KeycloakService
  ) {
    super(router, keycloak);
  }

  async isAccessAllowed(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    // Allow access during SSR (authentication will be checked on client)
    if (!isPlatformBrowser(this.platformId)) {
      return true;
    }

    // If the user is not logged in, redirect to Keycloak login
    if (!this.authenticated) {
      await this.keycloak.login({
        redirectUri: globalThis.location.origin + state.url,
      });
      return false;
    }

    // Get the required roles from the route data
    const requiredRoles: string[] = route.data['roles'] ?? [];

    // If no roles are required, allow access (just needs authentication)
    if (requiredRoles.length === 0) {
      return true;
    }

    // Check if user has at least one of the required roles
    const userRoles = this.keycloak.getUserRoles(true);
    const hasRole = requiredRoles.some((role) => userRoles.includes(role));

    if (!hasRole) {
      return this.router.createUrlTree(['/access-denied']);
    }

    return true;
  }
}
