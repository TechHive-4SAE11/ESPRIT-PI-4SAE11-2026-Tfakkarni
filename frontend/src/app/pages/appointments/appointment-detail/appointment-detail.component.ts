import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';

import { Appointment } from '../../../models/appointment.model';
import { Reminder, ReminderStatus, ReminderType } from '../../../models/reminder.model';
import { AppointmentService } from '../../../services/appointment.service';
import { ReminderService } from '../../../services/reminder.service';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';




@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardBadgeComponent],
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.css'],
})
export class AppointmentDetailComponent implements OnInit {
  appointment: Appointment | null = null;
  isLoading = true;
  errorMessage = '';

  reminders: Reminder[] = [];
  remindersLoading = false;
  remindersError = '';

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly reminderService: ReminderService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numericId = +id;
      this.appointmentService.getAppointmentById(numericId).subscribe({
        next: (a: Appointment) => (this.appointment = a),
        error: () => {
          this.errorMessage = 'Rendez-vous introuvable.';
          this.isLoading = false;
        },
        complete: () => (this.isLoading = false),
      });
  
      this.loadReminders(numericId);
    } else {
      this.isLoading = false;
      this.errorMessage = 'Identifiant manquant.';
    }
  }



  typeEmoji(type: Appointment['type']): string {
    return type === 'FOLLOW_UP' ? '👤' : '📅';
  }

  statusBadgeClasses(status: Appointment['status']): string {
    switch (status) {
      case 'SCHEDULED':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'COMPLETED':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  private loadReminders(appointmentId: number): void {
    this.remindersLoading = true;
    this.remindersError = '';
    this.reminderService.getRemindersByAppointment(appointmentId).subscribe({
      next: (list: Reminder[]) => (this.reminders = list ?? []),
      error: () => {
        this.remindersError = 'Impossible de charger les rappels.';
        this.remindersLoading = false;
      },
      complete: () => (this.remindersLoading = false),
    });
  }

  reminderEmoji(type: ReminderType): string {
    switch (type) {
      case 'CONFIRMATION':
        return '🔔';
      case 'PREPARATION':
        return '📝';
      case 'FEEDBACK':
        return '💬';
      default:
        return '🔔';
    }
  }

  reminderStatusClasses(status: ReminderStatus | undefined): string {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300';
      case 'SENT':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'FAILED':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
    }
  }

  goBack(): void {
    this.router.navigate(['/appointments']);
  }

  edit(): void {
    if (this.appointment?.id) {
      this.router.navigate(['/appointments/edit', this.appointment.id]);
    }
  }

  cancelAppointment(): void {
    if (!this.appointment?.id) return;
    if (!window.confirm('Voulez-vous vraiment annuler ce rendez-vous ?')) return;
    this.appointmentService.cancelAppointment(this.appointment.id).subscribe({
      next: () => this.router.navigate(['/appointments']),
      error: () => (this.errorMessage = "L'annulation a échoué."),
    });
  }

  goToCreateReminder(): void {
    if (!this.appointment?.id) return;
    this.router.navigate([
      '/appointments',
      this.appointment.id,
      'reminders',
      'new',
    ]);
  }

  editReminder(reminderId: number): void {
    this.router.navigate(['/reminders', reminderId, 'edit']);
  }

  deleteReminder(reminderId: number): void {
    if (!window.confirm('Supprimer ce rappel ?')) return;
    this.reminderService.deleteReminder(reminderId).subscribe({
      next: () => {
        if (this.appointment?.id) {
          this.loadReminders(this.appointment.id);
        }
      },
      error: () => {
        this.remindersError = 'La suppression du rappel a échoué.';
      },
    });
  }
}