import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil, Observable } from 'rxjs';

import { AppointmentService } from '@/services/appointment.service';
import { Appointment } from '@/models/appointment.model';
import { AuthService } from '@/core/auth';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardInputDirective } from '@/shared/components/input';

const TYPES: { value: Appointment['type']; label: string }[] = [
  { value: 'CONSULTATION', label: 'Consultation' },
  { value: 'FOLLOW_UP', label: 'Suivi' },
];

function endTimeAfterStartValidator(): ValidatorFn {
  return (form: AbstractControl): ValidationErrors | null => {
    const g = form as FormGroup;
    const start = g?.get('startTime')?.value;
    const end = g?.get('endTime')?.value;
    if (!start || !end) return null;
    return new Date(end).getTime() > new Date(start).getTime()
      ? null
      : { endTimeBeforeStart: true };
  };
}

@Component({
  selector: 'app-appointment-add',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
  ],
  templateUrl: './appointment-add.component.html',
  styleUrls: ['./appointment-add.component.css'],
})
export class AppointmentAddComponent implements OnDestroy, OnInit {
  form: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  previewDates: string[] = [];
  readonly typeOptions = TYPES;
  readonly patientOptions = [
    { value: 'patient123', label: 'Patient 123' },
    { value: 'patient456', label: 'Patient 456' },
  ];
  readonly doctorOptions = [
    { value: 'doctor456', label: 'Dr. Martin' },
    { value: 'doctor789', label: 'Dr. Dupont' },
  ];
  
  // ✅ MODIFICATION ICI : toujours true pour que la section apparaisse
  readonly isCaregiver = true;
  
  private readonly destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group(
      {
        title: ['', [Validators.required, Validators.minLength(3)]],
        description: [''],
        patientId: ['patient123', Validators.required],
        doctorId: ['doctor456'],
        startTime: ['', Validators.required],
        endTime: ['', Validators.required],
        type: ['CONSULTATION', Validators.required],
        notes: [''],
        isRecurring: [false],
        recurringFrequency: ['DAILY'],
        recurringOccurrences: [1, [Validators.min(1), Validators.max(52)]],
      },
      { validators: endTimeAfterStartValidator() }
    );
  }

  get isConsultation(): boolean {
    return this.form?.get('type')?.value === 'CONSULTATION';
  }

  ngOnInit(): void {
    this.form
      .get('type')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((type) => {
        const doctorId = this.form.get('doctorId');
        if (type === 'CONSULTATION') {
          doctorId?.setValidators([Validators.required]);
        } else {
          doctorId?.clearValidators();
        }
        doctorId?.updateValueAndValidity();
      });

    this.form
      .get('isRecurring')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updatePreviewDates());

    this.form
      .get('recurringFrequency')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updatePreviewDates());

    this.form
      .get('recurringOccurrences')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updatePreviewDates());

    this.form
      .get('startTime')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updatePreviewDates());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  updatePreviewDates(): void {
    this.previewDates = [];
    const isRecurring = this.form.get('isRecurring')?.value;
    if (!isRecurring) return;

    const startRaw = this.form.get('startTime')?.value;
    const frequency = this.form.get('recurringFrequency')?.value;
    const occurrences: number = this.form.get('recurringOccurrences')?.value ?? 0;

    if (!startRaw || !frequency || !occurrences || occurrences < 1) {
      return;
    }

    const baseStart = new Date(startRaw);
    if (isNaN(baseStart.getTime())) return;

    for (let i = 0; i < occurrences; i++) {
      const d = new Date(baseStart);
      switch (frequency) {
        case 'DAILY':
          d.setDate(d.getDate() + i);
          break;
        case 'WEEKLY':
          d.setDate(d.getDate() + 7 * i);
          break;
        case 'MONTHLY':
          d.setMonth(d.getMonth() + i);
          break;
      }
      this.previewDates.push(d.toLocaleString());
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting) return;
    
    const v = this.form.value;
    const payload = {
      title: v.title,
      description: v.description || undefined,
      patientId: v.patientId,
      doctorId: v.doctorId || undefined,
      startTime: v.startTime?.length === 16 ? `${v.startTime}:00` : v.startTime,
      endTime: v.endTime?.length === 16 ? `${v.endTime}:00` : v.endTime,
      type: v.type,
      notes: v.notes || undefined,
      status: 'SCHEDULED' as const,
    };
    
    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    
    const isRecurring = v.isRecurring;
    const frequency = v.recurringFrequency;
    const occurrences: number = v.recurringOccurrences ?? 1;

    let request$: Observable<Appointment | Appointment[]>;
    
    if (isRecurring) {
      request$ = this.appointmentService.createRecurringAppointments(payload, frequency, occurrences);
    } else {
      request$ = this.appointmentService.createAppointment(payload as Appointment);
    }

    request$.subscribe({
      next: (result) => {
        const count = Array.isArray(result) ? result.length : 1;
        this.successMessage = isRecurring
          ? `${count} rendez-vous récurrents créés. Redirection...`
          : 'Rendez-vous créé. Redirection...';
        this.isSubmitting = false;
        setTimeout(() => this.router.navigate(['/appointments']), 800);
      },
      error: (err: any) => { 
        const msg = err?.error ?? err?.message;
        this.errorMessage = typeof msg === 'string' ? msg : 'Une erreur est survenue. Réessayez.';
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.router.navigate(['/appointments']);
  }

  getControlError(name: string): string {
    const c = this.form.get(name);
    if (!c?.touched && !c?.dirty) return '';
    const e = c.errors;
    if (!e) return '';
    if (e['required']) return 'Champ obligatoire.';
    if (e['minlength']) return `Minimum ${e['minlength'].requiredLength} caractères.`;
    return '';
  }

  getFormError(): string {
    return this.form.errors?.['endTimeBeforeStart'] ? 'La date de fin doit être après la date de début.' : '';
  }
}