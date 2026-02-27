import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ReminderService } from '../../../services/reminder.service';
import { CreateReminderDTO, ReminderChannel, ReminderType } from '../../../models/reminder.model';
import { AppointmentService } from '../../../services/appointment.service';
import { Appointment } from '../../../models/appointment.model';

import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';

@Component({
  selector: 'app-create-reminder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
  ],
  templateUrl: './create-reminder.component.html',
  styleUrls: ['./create-reminder.component.css'],
})
export class CreateReminderComponent implements OnInit {
  appointmentId!: number;
  appointment: Appointment | null = null;

  form: {
    reminderType: ReminderType | '';
    reminderTime: string;
    channel: ReminderChannel | '';
    patientPhone: string;
    patientEmail: string;
    message: string;
  } = {
    reminderType: '',
    reminderTime: '',
    channel: '',
    patientPhone: '',
    patientEmail: '',
    message: '',
  };

  isSubmitting = false;
  errorMessage = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly reminderService: ReminderService,
    private readonly appointmentService: AppointmentService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('appointmentId');
    if (!id) {
      this.errorMessage = 'Identifiant de rendez-vous manquant.';
      return;
    }
    this.appointmentId = +id;

    this.appointmentService.getAppointmentById(this.appointmentId).subscribe({
      next: (a) => (this.appointment = a),
      error: () => (this.errorMessage = 'Impossible de charger le rendez-vous.'),
    });
  }

  get showPhone(): boolean {
    return this.form.channel === 'SMS';
  }

  get showEmail(): boolean {
    return this.form.channel === 'EMAIL';
  }

  onSubmit(): void {
    if (!this.appointment) {
      this.errorMessage = 'Rendez-vous introuvable.';
      return;
    }

    if (!this.form.reminderType || !this.form.reminderTime || !this.form.channel) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires.';
      return;
    }
    if (this.showPhone && !this.form.patientPhone) {
      this.errorMessage = 'Le téléphone est requis pour un rappel SMS.';
      return;
    }
    if (this.showEmail && !this.form.patientEmail) {
      this.errorMessage = "L'email est requis pour un rappel EMAIL.";
      return;
    }

    const dt = new Date(this.form.reminderTime);
    if (isNaN(dt.getTime()) || dt <= new Date()) {
      this.errorMessage = 'La date/heure doit être dans le futur.';
      return;
    }

    const payload: CreateReminderDTO = {
      appointmentId: this.appointmentId,
      patientId: this.appointment.patientId,
      reminderType: this.form.reminderType as ReminderType,
      reminderTime: dt.toISOString(),
      channel: this.form.channel as ReminderChannel,
      patientPhone: this.form.patientPhone || undefined,
      patientEmail: this.form.patientEmail || undefined,
      message: this.form.message || undefined,
    };

    this.isSubmitting = true;
    this.errorMessage = '';

    this.reminderService.createReminder(this.appointmentId, payload).subscribe({
      next: () => {
        this.router.navigate(['/appointments', this.appointmentId]);
      },
      error: () => {
        this.errorMessage = 'La création du rappel a échoué.';
        this.isSubmitting = false;
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/appointments', this.appointmentId]);
  }
}