// src/app/pages/doctor-dashboard/prescription-management/prescription-management.component.ts
import { Component, Input, OnInit, signal, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray, AbstractControl } from '@angular/forms';
import { catchError, finalize, of, tap } from 'rxjs';

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

  // Form
  prescriptionForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly prescriptionService: PrescriptionService,
    private readonly medicalFolderService: MedicalFolderService,
    private readonly sessionService: SessionService
  ) {
    this.prescriptionForm = this.createPrescriptionForm();
  }

  get medications(): FormArray {
    return this.prescriptionForm.get('medications') as FormArray;
  }

  ngOnInit(): void {
    this.loadPrescriptions();
    this.loadSessions();
  }

  // ==================== Form Creation ====================

  private createPrescriptionForm(): FormGroup {
    return this.fb.group({
      sessionId: [null, Validators.required],
      medications: this.fb.array([], [Validators.required, Validators.minLength(1)])
    });
  }

  private createMedicationFormGroup(medication?: MedicationRequestDTO): FormGroup {
    return this.fb.group({
      medicationName: [medication?.medicationName || '', Validators.required],
      dosage: [medication?.dosage || '', Validators.required],
      frequency: [medication?.frequency || '', Validators.required],
      duration: [medication?.duration || '', Validators.required],
      instructions: [medication?.instructions || '']
    });
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
    this.sessionService.getSessionsByMedicalFolder(folderId)
      .pipe(
        tap(sessions => {
          console.log('[PrescriptionManagement] Loaded sessions:', sessions);
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

  // ==================== Dialog Management ====================

  openCreateDialog(): void {
    this.resetForm();
    this.editingPrescriptionId.set(null);
    this.errorMessage.set(null);
    this.addMedication();
    this.showCreateDialog.set(true);
  }

  openEditDialog(prescription: PrescriptionResponseDTO): void {
    this.resetForm();
    this.editingPrescriptionId.set(prescription.id);
    this.errorMessage.set(null);

    this.prescriptionForm.patchValue({
      sessionId: prescription.sessionId
    });

    if (prescription.medications && prescription.medications.length > 0) {
      prescription.medications.forEach(med => {
        this.medications.push(this.createMedicationFormGroup(med));
      });
    } else {
      this.addMedication();
    }

    this.showCreateDialog.set(true);
  }

  closeCreateDialog(): void {
    this.showCreateDialog.set(false);
    this.resetForm();
  }

  viewPrescription(prescription: PrescriptionResponseDTO): void {
    this.selectedPrescription.set(prescription);
    this.showViewDialog.set(true);
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
              this.prescriptions.update(current => current.filter(p => p.id !== id));
            }),
            catchError(error => {
              console.error('[PrescriptionManagement] Error deleting prescription:', error);
              // Optionally show an error message
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          ).subscribe();
      },
    });
  }

  closeViewDialog(): void {
    this.showViewDialog.set(false);
    this.selectedPrescription.set(null);
  }

  // ==================== Form Management ====================

  addMedication(): void {
    this.medications.push(this.createMedicationFormGroup());
  }

  removeMedication(index: number): void {
    if (this.medications.length > 1) {
      this.medications.removeAt(index);
    }
  }

  getMedicationControl(index: number, controlName: string): AbstractControl | null {
    const medicationGroup = this.medications.at(index) as FormGroup;
    return medicationGroup?.get(controlName) || null;
  }

  private resetForm(): void {
    this.prescriptionForm.reset();
    this.medications.clear();
    this.errorMessage.set(null);
  }

  // ==================== Form Submission ====================

  onSubmit(): void {
    if (this.prescriptionForm.invalid) {
      console.error('[PrescriptionManagement] Form is invalid');
      this.prescriptionForm.markAllAsTouched();
      this.errorMessage.set('Please fill in all required fields');
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
