import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@/core/auth';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-background text-foreground">
      <header class="border-b border-border px-6 py-4 flex items-center justify-between">
        <h1 class="text-2xl font-bold text-primary">Doctor Dashboard</h1>
        <button
          class="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg hover:opacity-90 transition-opacity"
          (click)="logout()">
          Logout
        </button>
      </header>
      <main class="container mx-auto p-8">
        <div class="grid gap-6 md:grid-cols-3">
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">My Patients</h3>
            <p class="text-muted-foreground">View and manage your assigned patients.</p>
          </div>
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">Cognitive Assessments</h3>
            <p class="text-muted-foreground">Review patient cognitive test results.</p>
          </div>
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">Treatment Plans</h3>
            <p class="text-muted-foreground">Create and manage treatment plans.</p>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class DoctorDashboardComponent {
  constructor(private readonly authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
