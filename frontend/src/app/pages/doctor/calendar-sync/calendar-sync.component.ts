import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GoogleCalendarService, CalendarStatus } from '@/services/google-calendar.service';
import { KeycloakService } from 'keycloak-angular';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-calendar-sync',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="space-y-6">
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <z-icon zType="calendar" class="w-6 h-6" />
        Synchronisation Google Calendar
      </h1>
      
      @if (loading) {
        <div class="text-center py-8">
          <z-icon zType="loader-2" class="w-8 h-8 animate-spin mx-auto text-primary" />
        </div>
      } @else if (status?.connected) {
        <z-card class="p-6 border-green-200 bg-green-50/10">
          <div class="flex items-start justify-between">
            <div class="space-y-2">
              <div class="flex items-center gap-2 text-green-700">
                <z-icon zType="check-circle" class="w-5 h-5" />
                <span class="font-semibold text-lg">Connecté</span>
              </div>
              <p class="text-sm font-medium">Compte : <span class="text-muted-foreground">{{ status?.googleEmail }}</span></p>
              <p class="text-sm">Dernière synchro : <span class="text-muted-foreground">{{ status?.lastSync | date:'short' }}</span></p>
              <p class="text-sm">Rendez-vous synchronisés : <span class="font-semibold">{{ status?.syncedAppointments }}</span></p>
            </div>
            <button z-button zType="outline" class="text-red-600 border-red-200 hover:bg-red-50" (click)="disconnect()">
              <z-icon zType="log-out" class="w-4 h-4 mr-2" />
              Déconnecter
            </button>
          </div>
        </z-card>
      } @else {
        <z-card class="p-8 text-center max-w-xl mx-auto mt-8 border-dashed">
          <div class="rounded-full bg-blue-100 w-16 h-16 flex items-center justify-center mx-auto mb-4">
            <z-icon zType="calendar" class="w-8 h-8 text-blue-600" />
          </div>
          <h2 class="text-xl font-semibold mb-2">Connectez votre agenda</h2>
          <p class="text-muted-foreground mb-6">
            Liez votre compte Google Calendar pour y synchroniser automatiquement tous vos rendez-vous pris sur la plateforme.
          </p>
          <button z-button (click)="connect()" class="w-full sm:w-auto">
            <z-icon zType="calendar-plus" class="w-4 h-4 mr-2" />
            Se connecter avec Google
          </button>
        </z-card>
      }
    </div>
  `
})
export class CalendarSyncComponent implements OnInit {
  loading = true;
  status: CalendarStatus | null = null;
  doctorId: string = '';

  constructor(
    private googleCalendarService: GoogleCalendarService,
    private keycloakService: KeycloakService
  ) { }

  ngOnInit() {
    const kc = this.keycloakService.getKeycloakInstance();
    this.doctorId = kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';

    if (this.doctorId) {
      this.loadStatus();
    } else {
      this.loading = false;
    }
  }

  loadStatus() {
    this.loading = true;
    this.googleCalendarService.getStatus(this.doctorId).subscribe({
      next: (data) => {
        this.status = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement statut Google Calendar', err);
        this.loading = false;
      }
    });
  }

  connect() {
    this.googleCalendarService.getAuthUrl(this.doctorId).subscribe((res) => {
      window.location.href = res.url;
    });
  }

  disconnect() {
    if (confirm('Voulez-vous vraiment déconnecter votre compte Google Calendar ?')) {
      this.googleCalendarService.disconnect(this.doctorId).subscribe(() => {
        this.loadStatus();
      });
    }
  }
}
