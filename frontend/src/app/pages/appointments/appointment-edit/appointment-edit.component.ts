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
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import { AppointmentService } from '@/services/appointment.service';
import { Appointment } from '@/models/appointment.model';
import { IdMappingService } from '@/services/id-mapping.service';

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

function toDatetimeLocal(d: Date | string): string {
  const date = typeof d === 'string' ? new Date(d) : d;
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day}T${h}:${min}`;
}

@Component({
  selector: 'app-appointment-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardInputDirective,
  ],
  templateUrl: './appointment-edit.component.html',
  styleUrls: ['./appointment-edit.component.css'],
})
export class AppointmentEditComponent implements OnInit, OnDestroy {
  form: FormGroup;
  appointmentId: number | null = null;
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  readonly typeOptions = TYPES;
  patientSuggestions: { name: string; id: string }[] = [];
  doctorSuggestions: { name: string; id: string }[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private idMappingService: IdMappingService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group(
      {
        title: ['', [Validators.required, Validators.minLength(3)]],
        description: [''],
        patientName: ['', Validators.required],
        patientId: [''],
        doctorName: [''],
        doctorId: [''],
        startTime: ['', Validators.required],
        endTime: ['', Validators.required],
        type: ['CONSULTATION', Validators.required],
        notes: [''],
      },
      { validators: endTimeAfterStartValidator() }
    );
  }

  get isConsultation(): boolean {
    return this.form?.get('type')?.value === 'CONSULTATION';
  }

  ngOnInit(): void {
    this.patientSuggestions = this.idMappingService.getAllNames('patient');
    this.doctorSuggestions = this.idMappingService.getAllNames('doctor');

    this.form.get('patientName')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(name => {
      if (name) {
        const id = this.idMappingService.getIdForName(name, 'patient');
        this.form.get('patientId')?.setValue(id, { emitEvent: false });
      } else {
        this.form.get('patientId')?.setValue('', { emitEvent: false });
      }
    });

    this.form.get('doctorName')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(name => {
      if (name) {
        const id = this.idMappingService.getIdForName(name, 'doctor');
        this.form.get('doctorId')?.setValue(id, { emitEvent: false });
      } else {
        this.form.get('doctorId')?.setValue('', { emitEvent: false });
      }
    });

    this.form
      .get('type')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((type) => {
        const doctorName = this.form.get('doctorName');
        if (type === 'CONSULTATION') {
          doctorName?.setValidators([Validators.required]);
        } else {
          doctorName?.clearValidators();
        }
        doctorName?.updateValueAndValidity();
      });

    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = params['id'];
      if (id) {
        this.appointmentId = +id;
        this.loadAppointment();
      } else {
        this.isLoading = false;
        this.errorMessage = 'Identifiant manquant.';
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
      next: (a) => this.patchForm(a),
      error: () => {
        this.errorMessage = 'Erreur lors du chargement du rendez-vous.';
        this.isLoading = false;
      },
      complete: () => (this.isLoading = false),
    });
  }

  private patchForm(a: Appointment): void {
    const start =
      typeof a.startTime === 'string' ? (a.startTime as string).slice(0, 16) : toDatetimeLocal(a.startTime);
    const end =
      typeof a.endTime === 'string' ? (a.endTime as string).slice(0, 16) : toDatetimeLocal(a.endTime);

    let pName = '';
    if (a.patientId) {
      pName = this.idMappingService.getNameFromId(a.patientId) || a.patientId;
    }

    let dName = '';
    if (a.doctorId) {
      dName = this.idMappingService.getNameFromId(a.doctorId) || a.doctorId;
    }

    this.form.patchValue({
      title: a.title,
      description: a.description ?? '',
      patientName: pName,
      patientId: a.patientId,
      doctorName: dName,
      doctorId: a.doctorId ?? '',
      startTime: start,
      endTime: end,
      type: a.type,
      notes: a.notes ?? '',
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting || !this.appointmentId) return;
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
    this.appointmentService.updateAppointment(this.appointmentId, payload as Appointment).subscribe({
      next: () => {
        this.successMessage = 'Rendez-vous mis à jour. Redirection...';
        this.isSubmitting = false;
        setTimeout(() => this.router.navigate(['/appointments']), 800);
      },
      error: (err) => {
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
