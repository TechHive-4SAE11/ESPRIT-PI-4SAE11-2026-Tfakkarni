import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@/core/auth';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
      <!-- Navbar -->
      <header class="border-b border-border/40 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm sticky top-0 z-50">
        <div class="container mx-auto flex items-center justify-between px-6 py-4">
          <div class="flex items-center gap-2">
            <span class="text-2xl">🧠</span>
            <span class="text-xl font-bold text-primary">Tfakkarni</span>
          </div>
          <div class="flex items-center gap-3">
            <a routerLink="/login">
              <button z-button zType="outline">Sign In</button>
            </a>
            <a routerLink="/signup">
              <button z-button>Get Started</button>
            </a>
          </div>
        </div>
      </header>

      <!-- Hero Section -->
      <main class="container mx-auto px-6">
        <section class="flex flex-col items-center text-center py-24 md:py-32">
          <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 text-primary text-sm font-medium mb-8">
            <span>🧠</span>
            <span>Alzheimer's Patient Monitoring Platform</span>
          </div>
          <h1 class="text-5xl md:text-6xl font-bold tracking-tight text-foreground mb-6 max-w-3xl">
            Empowering Memory Through
            <span class="text-primary"> Interactive Games</span>
          </h1>
          <p class="text-lg md:text-xl text-muted-foreground max-w-2xl mb-10">
            Tfakkarni helps patients exercise their memory with personalized photo-based games,
            while doctors monitor progress and provide better care.
          </p>
          <div class="flex items-center gap-4">
            <a routerLink="/signup">
              <button z-button zSize="lg" class="text-lg px-8 py-6">
                Start Free
                <z-icon zType="arrow-right" class="ml-2" />
              </button>
            </a>
            <a routerLink="/login">
              <button z-button zType="outline" zSize="lg" class="text-lg px-8 py-6">
                Sign In
              </button>
            </a>
          </div>
        </section>

        <!-- Features Section -->
        <section class="py-16 md:py-24">
          <h2 class="text-3xl font-bold text-center mb-12">How It Works</h2>
          <div class="grid gap-8 md:grid-cols-3 max-w-5xl mx-auto">
            <!-- Patient Card -->
            <div class="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-sm border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center mb-4">
                <z-icon zType="gamepad-2" class="h-6 w-6 text-blue-600" />
              </div>
              <h3 class="text-xl font-semibold mb-3">For Patients</h3>
              <p class="text-muted-foreground">
                Upload photos of loved ones and familiar places, then play personalized memory games
                to exercise and strengthen your recall.
              </p>
            </div>

            <!-- Doctor Card -->
            <div class="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-sm border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-4">
                <z-icon zType="heart" class="h-6 w-6 text-green-600" />
              </div>
              <h3 class="text-xl font-semibold mb-3">For Doctors</h3>
              <p class="text-muted-foreground">
                Monitor your patients' progress, track game performance over time,
                and use data-driven insights to adjust care plans.
              </p>
            </div>

            <!-- Admin Card -->
            <div class="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-sm border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-12 h-12 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center mb-4">
                <z-icon zType="shield" class="h-6 w-6 text-purple-600" />
              </div>
              <h3 class="text-xl font-semibold mb-3">For Admins</h3>
              <p class="text-muted-foreground">
                Manage the entire platform — users, games, and analytics —
                all from a comprehensive admin dashboard.
              </p>
            </div>
          </div>
        </section>
      </main>

      <!-- Footer -->
      <footer class="border-t border-border/40 bg-white/50 dark:bg-gray-900/50">
        <div class="container mx-auto px-6 py-8 text-center">
          <p class="text-sm text-muted-foreground">&copy; 2026 Tfakkarni. All rights reserved.</p>
        </div>
      </footer>
    </div>
  `,
})
export class LandingComponent implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // If already logged in, redirect to role-based dashboard
    if (this.authService.isLoggedIn()) {
      this.authService.routeByRole();
    }
  }
}
