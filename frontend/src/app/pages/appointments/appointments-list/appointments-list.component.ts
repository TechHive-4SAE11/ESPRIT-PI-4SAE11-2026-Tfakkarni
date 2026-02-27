import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { AppointmentService } from '../../../services/appointment.service';
import { Appointment } from '@/models/appointment.model';
import { SuggestionService } from '@/services/suggestion.service';
import { SlotSuggestion } from '@/models/suggestion.model';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
 // Vérifier si on est dans le navigateur
const isBrowser = typeof window !== 'undefined';
@Component({
  selector: 'app-appointments-list',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardBadgeComponent],
  templateUrl: './appointments-list.component.html',
  styleUrls: ['./appointments-list.component.css'],
})
export class AppointmentsListComponent implements OnInit, OnDestroy {
  appointments: Appointment[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  selectedAppointmentForReschedule: Appointment | null = null;
  suggestions: SlotSuggestion[] = [];
  showRescheduleModal = false;
  isLoadingSuggestions = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly suggestionService: SuggestionService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (isBrowser) {
      this.loadAppointments();
    }
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((e) => {
        if (e.urlAfterRedirects === '/appointments' || e.urlAfterRedirects.startsWith('/appointments?')) {
          this.loadAppointments();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.appointmentService.getAllAppointments().subscribe({
      next: (appointments) => {
        this.appointments = appointments ?? [];
      },
      error: () => {
        this.errorMessage = "Impossible de charger les rendez-vous pour le moment.";
        this.appointments = [];
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }

  clearSuccessMessage(): void {
    this.successMessage = '';
  }

  typeEmoji(type: Appointment['type']): string {
    return type === 'FOLLOW_UP' ? '👤' : '📅';
  }

  statusBadgeClasses(status: Appointment['status']): string {
    switch (status) {
      case 'SCHEDULED':
        return 'bg-blue-100 text-blue-800';
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800';
      case 'COMPLETED':
        return 'bg-gray-100 text-gray-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  goToNewAppointment(): void {
    this.router.navigate(['/appointments/new']);
  }

  viewAppointment(id: number): void {
    this.router.navigate(['/appointments', id]);
  }

  editAppointment(id: number): void {
    this.router.navigate(['/appointments/edit', id]);
  }

  cancelAppointment(id: number): void {
    const confirmed = window.confirm('Voulez-vous vraiment annuler ce rendez-vous ?');
    if (!confirmed) {
      return;
    }

    this.errorMessage = '';
    this.appointmentService.cancelAppointment(id).subscribe({
      next: () => {
        this.successMessage = 'Rendez-vous annulé.';
        this.loadAppointments();
        setTimeout(() => this.clearSuccessMessage(), 4000);
      },
      error: () => {
        this.errorMessage = "L'annulation a échoué. Réessayez plus tard.";
      },
    });
  }

  openRescheduleModal(appointment: Appointment): void {
    this.selectedAppointmentForReschedule = appointment;
    this.isLoadingSuggestions = true;
    this.showRescheduleModal = true;

    if (!appointment.id) {
      this.isLoadingSuggestions = false;
      return;
    }

    this.suggestionService.getSuggestions(appointment.id, 3).subscribe({
      next: (data) => {
        this.suggestions = data;
        this.isLoadingSuggestions = false;
      },
      error: (err) => {
        console.error('Erreur chargement suggestions', err);
        this.isLoadingSuggestions = false;
      },
    });
  }

  closeRescheduleModal(): void {
    this.showRescheduleModal = false;
    this.selectedAppointmentForReschedule = null;
    this.suggestions = [];
  }

  selectSuggestion(suggestion: SlotSuggestion): void {
    if (!this.selectedAppointmentForReschedule || !this.selectedAppointmentForReschedule.id) {
      return;
    }

    const updatedAppointment: Appointment = {
      ...this.selectedAppointmentForReschedule,
      startTime: new Date(suggestion.start),
      endTime: new Date(suggestion.end),
    };

    this.appointmentService
      .updateAppointment(this.selectedAppointmentForReschedule.id, updatedAppointment)
      .subscribe({
        next: () => {
          this.closeRescheduleModal();
          this.loadAppointments();
        },
        error: (err) => console.error('Erreur replanification', err),
      });
  }
}

