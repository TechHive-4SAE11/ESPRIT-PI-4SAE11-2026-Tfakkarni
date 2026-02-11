import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-background text-foreground flex items-center justify-center">
      <div class="text-center space-y-4">
        <h1 class="text-6xl font-bold text-destructive">403</h1>
        <h2 class="text-2xl font-semibold">Access Denied</h2>
        <p class="text-muted-foreground">You do not have permission to view this page.</p>
        <a routerLink="/" class="inline-block px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity">
          Go Home
        </a>
      </div>
    </div>
  `,
})
export class AccessDeniedComponent {}
