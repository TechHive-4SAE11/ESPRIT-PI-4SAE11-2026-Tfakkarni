import { Component, Input, OnInit, signal, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormArray, AbstractControl, Validators } from '@angular/forms';
import { catchError, finalize, of, tap } from 'rxjs';
import { z } from 'zod';

import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog/alert-dialog.service';

import { UserInfo } from '@/core/services/user-api.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';
import {
  PrescriptionResponseDTO,
  PrescriptionRequestDTO,
  MedicationRequestDTO
} from '@/core/models/prescription.model';

@Component({
  selector: 'app-prescription-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent
  ],
  templateUrl: './prescription-management.component.html',
})
export class PrescriptionManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly fb = inject(FormBuilder);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly sessionService = inject(SessionService);

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;

  // State signals
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  sessions = signal<SessionResponseDTO[]>([]);
  selectedPrescription = signal<PrescriptionResponseDTO | null>(null);

  // UI state signals
  showCreateDialog = signal(false);
  showViewDialog = signal(false);
  isLoadingPrescriptions = signal(false);
  isLoadingSessions = signal(false);
  isSubmitting = signal(false);
  editingPrescriptionId = signal<number | null>(null);
  errorMessage = signal<string | null>(null);

  prescriptionForm!: FormGroup;

  // Zod Schemas
  private readonly medicationFieldSchemas = {
    medicationName: z.string().min(1, { message: 'Please enter the medication name' }),
    dosage: z.string().min(1, { message: 'Please specify the dosage (e.g., 500mg, 2 tablets)' }),
    frequency: z.string().min(1, { message: 'Please specify how often to take (e.g., 2x/day, every 8 hours)' }),
    duration: z.string().min(1, { message: 'Please specify treatment duration (e.g., 7 days, 2 weeks)' }),
    instructions: z.string().optional()
  };

  private readonly prescriptionSchema = z.object({
    sessionId: z.union([
      z.number(),
      z.string().min(1)
    ]).refine(val => val !== null && val !== '', { message: 'Please select a consultation session' }),
    medications: z.array(z.any()).min(1, { message: 'At least one medication is required' })
  });

  ngOnInit(): void {
    this.prescriptionForm = this.createPrescriptionForm();
    this.loadPrescriptions();
    this.loadSessions();
  }

  get medications(): FormArray {
    return this.prescriptionForm.get('medications') as FormArray;
  }

  // ==================== Form Creation ====================

  private createPrescriptionForm(): FormGroup {
    return this.fb.group({
      sessionId: [null, createZodValidator(this.prescriptionSchema.shape.sessionId)],
      medications: this.fb.array([], [createZodValidator(this.prescriptionSchema.shape.medications)])
    });
  }

  private createMedicationFormGroup(medication?: MedicationRequestDTO): FormGroup {
    return this.fb.group({
      medicationName: [medication?.medicationName || '', createZodValidator(this.medicationFieldSchemas.medicationName)],
      dosage: [medication?.dosage || '', createZodValidator(this.medicationFieldSchemas.dosage)],
      frequency: [medication?.frequency || '', createZodValidator(this.medicationFieldSchemas.frequency)],
      duration: [medication?.duration || '', createZodValidator(this.medicationFieldSchemas.duration)],
      instructions: [medication?.instructions || '', createZodValidator(this.medicationFieldSchemas.instructions)]
    });
  }

  // Helper method used in template for validation
  getMedicationControl(index: number, controlName: string): AbstractControl | null {
    const medicationGroup = this.medications.at(index) as FormGroup;
    return medicationGroup?.get(controlName);
  }

  // Get the first validation error message for a form control
  getErrorMessage(control: AbstractControl | null): string {
    if (!control || !control.errors) {
      return '';
    }
    const errors = control.errors;
    // Zod validator stores the message in the 'zodError' key
    if (errors['zodError']) {
      return errors['zodError'];
    }
    // Fallback to standard Angular validators
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['minlength']) {
      return `Minimum length is ${errors['minlength'].requiredLength}`;
    }
    return 'Invalid value';
  }

  // Check if control should show error (invalid and touched)
  shouldShowError(control: AbstractControl | null): boolean {
    return !!(control && control.invalid && control.touched);
  }

  // ==================== Data Loading ====================

  loadPrescriptions(): void {
    if (!this.patient?.id) {
      console.warn('[PrescriptionManagement] No patient ID available');
      return;
    }

    this.isLoadingPrescriptions.set(true);
    const patientDbId = String(this.patient.id);

    this.prescriptionService.getPrescriptionsByPatient(patientDbId)
      .pipe(
        tap(data => {
          console.log('[PrescriptionManagement] Loaded prescriptions:', data);
          this.prescriptions.set(data);
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading prescriptions:', error);
          this.prescriptions.set([]);
          return of([]);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadSessions(): void {
    if (!this.patient?.id) {
      console.warn('[PrescriptionManagement] No patient ID available');
      return;
    }

    if (!this.doctor) {
      console.warn('[PrescriptionManagement] No doctor info available');
      return;
    }

    this.isLoadingSessions.set(true);
    const currentDoctorDbId = String(this.doctor.id);
    const patientDbId = String(this.patient.id);

    console.log('[PrescriptionManagement] Loading sessions for patient:', patientDbId, 'doctor:', currentDoctorDbId);

    this.medicalFolderService.getMedicalFoldersByPatient(patientDbId)
      .pipe(
        tap(folders => {
          console.log('[PrescriptionManagement] Medical folders:', folders);

          const matchingFolder = folders.find(f => f.idDoctor === currentDoctorDbId);

          if (matchingFolder) {
            console.log('[PrescriptionManagement] Found matching folder:', matchingFolder);
            this.loadSessionsForFolder(matchingFolder.id);
          } else {
            console.warn('[PrescriptionManagement] No matching folder found');
            this.sessions.set([]);
            this.isLoadingSessions.set(false);
          }
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading medical folders:', error);
          this.sessions.set([]);
          this.isLoadingSessions.set(false);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadSessionsForFolder(folderId: number): void {
    this.sessionService.getSessionsWhereNoPrescription(folderId)
      .pipe(
        tap(sessions => {
          console.log('[PrescriptionManagement] Loaded sessions without prescription:', sessions);
          this.sessions.set(sessions);
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading sessions:', error);
          this.sessions.set([]);
          return of([]);
        }),
        finalize(() => this.isLoadingSessions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ==================== Actions ====================

  openCreateDialog(): void {
    console.log('PrescriptionManagement: openCreateDialog called');
    try {
      this.editingPrescriptionId.set(null);
      this.prescriptionForm.reset();
      this.medications.clear();
      this.addMedication();
      this.errorMessage.set(null);
      this.showCreateDialog.set(true);
    } catch (error) {
      console.error('PrescriptionManagement: Error opening create dialog', error);
      this.errorMessage.set('An error occurred while opening the dialog');
    }
  }

  openEditDialog(prescription: PrescriptionResponseDTO): void {
    this.editingPrescriptionId.set(prescription.id);
    this.prescriptionForm.patchValue({
      sessionId: prescription.sessionId
    });

    this.medications.clear();
    if (prescription.medications && prescription.medications.length > 0) {
      prescription.medications.forEach(med => {
        // Map response DTO to request DTO for form
        const requestDto: MedicationRequestDTO = {
          medicationName: med.medicationName,
          dosage: med.dosage,
          frequency: med.frequency,
          duration: med.duration,
          instructions: med.instructions
        };
        this.medications.push(this.createMedicationFormGroup(requestDto));
      });
    } else {
      this.addMedication();
    }

    this.errorMessage.set(null);
    this.showCreateDialog.set(true);
  }

  closeCreateDialog(): void {
    this.showCreateDialog.set(false);
    this.prescriptionForm.reset();
    this.medications.clear();
    this.errorMessage.set(null);
  }

  viewPrescription(prescription: PrescriptionResponseDTO): void {
    this.selectedPrescription.set(prescription);
    this.showViewDialog.set(true);
  }

  closeViewDialog(): void {
    this.showViewDialog.set(false);
    this.selectedPrescription.set(null);
  }

  deletePrescription(id: number): void {
    this.alertDialog.confirm({
      zTitle: 'Delete Prescription',
      zDescription: 'Are you sure you want to delete this prescription? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.prescriptionService.deletePrescription(id)
          .pipe(
            tap(() => {
              this.loadPrescriptions();
              if (this.selectedPrescription()?.id === id) {
                this.closeViewDialog();
              }
            }),
            catchError(error => {
              console.error('[PrescriptionManagement] Error removing prescription:', error);
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    });
  }

  addMedication(): void {
    this.medications.push(this.createMedicationFormGroup());
  }

  removeMedication(index: number): void {
    if (this.medications.length > 1) {
      this.medications.removeAt(index);
    }
  }

  onSubmit(): void {
    // Mark all fields as touched to show validation errors
    this.prescriptionForm.markAllAsTouched();
    this.medications.controls.forEach(control => {
      control.markAllAsTouched();
    });

    if (this.prescriptionForm.invalid) {
      console.error('[PrescriptionManagement] Form is invalid');
      this.errorMessage.set('Please correct the errors below before submitting');
      return;
    }

    if (this.medications.length === 0) {
      this.errorMessage.set('At least one medication is required');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const formValue = this.prescriptionForm.value;
    const request: PrescriptionRequestDTO = {
      sessionId: +formValue.sessionId,
      medications: formValue.medications as MedicationRequestDTO[]
    };

    console.log('[PrescriptionManagement] Submitting prescription:', request);

    const operation$ = this.editingPrescriptionId()
      ? this.prescriptionService.updatePrescription(this.editingPrescriptionId()!, request)
      : this.prescriptionService.createPrescription(request);

    operation$
      .pipe(
        tap(response => {
          console.log('[PrescriptionManagement] Success:', response);
          this.closeCreateDialog();
          this.loadPrescriptions();
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error:', error);
          const errorMsg = error?.error?.error || error?.message || 'Failed to save prescription';
          this.errorMessage.set(errorMsg);
          return of(null);
        }),
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
