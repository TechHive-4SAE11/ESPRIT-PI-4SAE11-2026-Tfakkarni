import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '@/core/auth';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-background text-foreground flex items-center justify-center">
      <div class="text-center space-y-6 max-w-md">
        <h1 class="text-4xl font-bold text-primary">Tfakkarni</h1>
        <p class="text-lg text-muted-foreground">
          Alzheimer's Patient Monitoring Platform
        </p>

        @if (!isLoggedIn) {
          <button
            class="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity text-lg"
            (click)="login()">
            Sign In with Keycloak
          </button>
        } @else {
          <p class="text-muted-foreground">Redirecting to your dashboard...</p>
        }
      </div>
    </div>
  `,
})
export class HomeComponent implements OnInit {
  isLoggedIn = false;
  private platformId = inject(PLATFORM_ID);

  constructor(
    private authService: AuthService,
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Skip auth check during SSR
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.isLoggedIn = this.authService.isLoggedIn();

    // If the user is not logged in, redirect to login page
    if (!this.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }

    // If the user is already logged in, route them to their role-based dashboard
    this.authService.routeByRole();
  }

  login(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    
    this.keycloakService.login({
      redirectUri: globalThis.location.origin + '/home',
    });
  }
}
