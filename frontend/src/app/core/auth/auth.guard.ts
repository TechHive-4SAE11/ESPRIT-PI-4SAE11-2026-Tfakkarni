import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { isPlatformBrowser } from '@angular/common';

/**
 * AuthGuard that checks if the user is authenticated AND holds at least
 * one of the roles specified in the route's `data.roles` array.
 *
 * Does NOT extend KeycloakAuthGuard to avoid SSR crashes when the
 * Keycloak instance is not initialized (resourceAccess undefined).
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
export class AuthGuard implements CanActivate {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly keycloak = inject(KeycloakService);

  async canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    // Allow access during SSR — authentication will be checked on the client
    if (!isPlatformBrowser(this.platformId)) {
      return true;
    }

    // Check if the user is logged in
    const isLoggedIn = this.keycloak.isLoggedIn();

    if (!isLoggedIn) {
      // Redirect to login page instead of Keycloak SSO
      return this.router.createUrlTree(['/login']);
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
