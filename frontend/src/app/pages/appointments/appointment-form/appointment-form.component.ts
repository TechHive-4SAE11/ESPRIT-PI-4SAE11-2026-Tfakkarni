import { Component, OnInit, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import { AppointmentService } from '@/services/appointment.service';
import { Appointment } from '@/models/appointment.model';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardInputDirective } from '@/shared/components/input';

const APPOINTMENT_TYPES: { value: Appointment['type']; label: string }[] = [
  { value: 'CONSULTATION', label: 'Consultation' },
  { value: 'FOLLOW_UP', label: 'Suivi' },
];

function endTimeAfterStartTimeValidator(): ValidatorFn {
  return (form: AbstractControl): ValidationErrors | null => {
    const g = form as FormGroup;
    const start = g?.get('startTime')?.value;
    const end = g?.get('endTime')?.value;
    if (!start || !end) return null;
    const startDate = new Date(start).getTime();
    const endDate = new Date(end).getTime();
    return endDate > startDate ? null : { endTimeBeforeStart: true };
  };
}

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
  ],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.css'],
})
export class AppointmentFormComponent implements OnInit, OnDestroy {
  appointmentForm: FormGroup;
  isEditMode = false;
  appointmentId: number | null = null;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  readonly typeOptions = APPOINTMENT_TYPES;
  readonly patientOptions = [
    { value: 'patient123', label: 'Patient 123' },
    { value: 'patient456', label: 'Patient 456' },
  ];
  readonly doctorOptions = [
    { value: 'doctor456', label: 'Dr. Martin' },
    { value: 'doctor789', label: 'Dr. Dupont' },
  ];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.appointmentForm = this.fb.group(
      {
        title: ['', [Validators.required, Validators.minLength(3)]],
        description: [''],
        patientId: ['patient123', Validators.required],
        doctorId: ['doctor456'],
        startTime: ['', Validators.required],
        endTime: ['', Validators.required],
        type: ['CONSULTATION', Validators.required],
        notes: [''],
      },
      { validators: endTimeAfterStartTimeValidator() }
    );
  }

  get isConsultation(): boolean {
    return this.appointmentForm?.get('type')?.value === 'CONSULTATION';
  }

  ngOnInit(): void {
    this.appointmentForm
      .get('type')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((type) => {
        const doctorId = this.appointmentForm.get('doctorId');
        if (type === 'CONSULTATION') {
          doctorId?.setValidators([Validators.required]);
        } else {
          doctorId?.clearValidators();
        }
        doctorId?.updateValueAndValidity();
      });

    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      if (params['id']) {
        this.isEditMode = true;
        this.appointmentId = +params['id'];
        this.loadAppointment();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAppointment(): void {
    if (!this.appointmentId) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.appointmentService.getAppointmentById(this.appointmentId).subscribe({
      next: (appointment) => {
        this.patchFormWithAppointment(appointment);
      },
      error: (err) => {
        this.errorMessage =
          err?.error?.message || err?.message || 'Erreur lors du chargement du rendez-vous.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }

  private patchFormWithAppointment(appointment: Appointment): void {
    const start =
      typeof appointment.startTime === 'string'
        ? (appointment.startTime as string).slice(0, 16)
        : toDatetimeLocal(appointment.startTime);
    const end =
      typeof appointment.endTime === 'string'
        ? (appointment.endTime as string).slice(0, 16)
        : toDatetimeLocal(appointment.endTime);
    this.appointmentForm.patchValue(
      {
        title: appointment.title,
        description: appointment.description ?? '',
        patientId: appointment.patientId,
        doctorId: appointment.doctorId ?? '',
        startTime: start,
        endTime: end,
        type: appointment.type,
        notes: appointment.notes ?? '',
      },
      { emitEvent: true }
    );
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid || this.isSubmitting) return;

    const value = this.appointmentForm.value;
    const payload = {
      title: value.title,
      description: value.description || undefined,
      patientId: value.patientId,
      doctorId: value.doctorId || undefined,
      startTime: toISOString(value.startTime),
      endTime: toISOString(value.endTime),
      type: value.type,
      notes: value.notes || undefined,
      status: 'SCHEDULED' as const,
    };

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const onSuccess = () => {
      this.successMessage = this.isEditMode
        ? 'Rendez-vous mis à jour.'
        : 'Rendez-vous créé. Redirection...';
      this.isSubmitting = false;
      setTimeout(() => this.router.navigate(['/appointments']), 800);
    };

    const onError = (err: { error?: string; message?: string }) => {
      const msg = err?.error ?? err?.message;
      this.errorMessage =
        typeof msg === 'string' ? msg : 'Une erreur est survenue. Réessayez.';
      this.isSubmitting = false;
    };

    if (this.isEditMode && this.appointmentId) {
      this.appointmentService
        .updateAppointment(this.appointmentId, payload as Appointment)
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.appointmentService
        .createAppointment(payload as Appointment)
        .subscribe({ next: onSuccess, error: onError });
    }
  }

  onCancel(): void {
    this.router.navigate(['/appointments']);
  }

  getControlError(controlName: string): string {
    const c = this.appointmentForm.get(controlName);
    if (!c?.touched && !c?.dirty) return '';
    const err = c.errors;
    if (!err) return '';
    if (err['required']) return 'Champ obligatoire.';
    if (err['minlength']) return `Minimum ${err['minlength'].requiredLength} caractères.`;
    if (err['endTimeBeforeStart']) return 'La fin doit être après le début.';
    return '';
  }

  getFormError(): string {
    const err = this.appointmentForm.errors;
    if (err?.['endTimeBeforeStart']) return 'La date de fin doit être après la date de début.';
    return '';
  }
}

function toDatetimeLocal(d: Date | string): string {
  const date = typeof d === 'string' ? new Date(d) : d;
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day}T${h}:${min}`;
}

function toISOString(value: string): string {
  if (!value) return value;
  return value.length === 16 ? `${value}:00` : value;
}
