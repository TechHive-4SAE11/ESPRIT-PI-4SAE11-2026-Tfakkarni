import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@/core/auth';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-background text-foreground">
      <header class="border-b border-border px-6 py-4 flex items-center justify-between">
        <h1 class="text-2xl font-bold text-primary">Patient Dashboard</h1>
        <button
          class="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg hover:opacity-90 transition-opacity"
          (click)="logout()">
          Logout
        </button>
      </header>
      <main class="container mx-auto p-8">
        <div class="grid gap-6 md:grid-cols-3">
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">Daily Exercises</h3>
            <p class="text-muted-foreground">Complete your daily cognitive exercises.</p>
          </div>
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">My Progress</h3>
            <p class="text-muted-foreground">Track your cognitive health progress over time.</p>
          </div>
          <div class="p-6 bg-card text-card-foreground rounded-lg border border-border">
            <h3 class="text-lg font-semibold mb-2">Appointments</h3>
            <p class="text-muted-foreground">View upcoming appointments with your doctor.</p>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class PatientDashboardComponent {
  constructor(private readonly authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
