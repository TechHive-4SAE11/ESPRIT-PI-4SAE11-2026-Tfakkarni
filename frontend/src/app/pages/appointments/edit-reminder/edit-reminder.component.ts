import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ReminderService } from '../../../services/reminder.service';
import {
  Reminder,
  ReminderChannel,
  ReminderType,
  UpdateReminderDTO,
} from '../../../models/reminder.model';

import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';

@Component({
  selector: 'app-edit-reminder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
  ],
  templateUrl: './edit-reminder.component.html',
  styleUrls: ['./edit-reminder.component.css'],
})
export class EditReminderComponent implements OnInit {
  reminderId!: number;
  reminder: Reminder | null = null;

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
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('reminderId');
    if (!id) {
      this.errorMessage = 'Identifiant de rappel manquant.';
      return;
    }
    this.reminderId = +id;

    this.reminderService.getReminderById(this.reminderId).subscribe({
      next: (r) => {
        this.reminder = r;
        this.form.reminderType = r.reminderType;
        this.form.reminderTime = r.reminderTime
          ? new Date(r.reminderTime).toISOString().slice(0, 16)
          : '';
        this.form.channel = r.channel;
        this.form.patientPhone = r.patientPhone ?? '';
        this.form.patientEmail = r.patientEmail ?? '';
        this.form.message = r.message ?? '';
      },
      error: () => (this.errorMessage = 'Impossible de charger le rappel.'),
    });
  }

  get showPhone(): boolean {
    return this.form.channel === 'SMS';
  }

  get showEmail(): boolean {
    return this.form.channel === 'EMAIL';
  }

  onSubmit(): void {
    if (!this.reminder) {
      this.errorMessage = 'Rappel introuvable.';
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
    if (isNaN(dt.getTime())) {
      this.errorMessage = 'Date/heure invalide.';
      return;
    }

    const payload: UpdateReminderDTO = {
      appointmentId: this.reminder.appointmentId,
      patientId: this.reminder.patientId,
      reminderType: this.form.reminderType as ReminderType,
      reminderTime: dt.toISOString(),
      channel: this.form.channel as ReminderChannel,
      patientPhone: this.form.patientPhone || undefined,
      patientEmail: this.form.patientEmail || undefined,
      message: this.form.message || undefined,
    };

    this.isSubmitting = true;
    this.errorMessage = '';

    this.reminderService.updateReminder(this.reminderId, payload).subscribe({
      next: () => {
        this.router.navigate(['/appointments', this.reminder!.appointmentId]);
      },
      error: () => {
        this.errorMessage = 'La mise à jour du rappel a échoué.';
        this.isSubmitting = false;
      },
    });
  }

  cancel(): void {
    if (this.reminder?.appointmentId) {
      this.router.navigate(['/appointments', this.reminder.appointmentId]);
    } else {
      this.router.navigate(['/appointments']);
    }
  }
}