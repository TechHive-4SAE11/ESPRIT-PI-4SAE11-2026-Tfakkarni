import { Component, Input, OnInit, signal, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray, AbstractControl } from '@angular/forms';
import { catchError, finalize, of, tap } from 'rxjs';

import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';

import { UserInfo } from '@/core/services/user-api.service';
import { CarePlanService } from '@/core/services/care-plan.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';
import {
  CarePlanResponseDTO,
  CarePlanRequestDTO,
  CareActivityRequestDTO
} from '@/core/models/care-plan.model';

@Component({
  selector: 'app-care-plan-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent
  ],
  templateUrl: './care-plan-management.component.html',
})
export class CarePlanManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;

  // State signals
  carePlans = signal<CarePlanResponseDTO[]>([]);
  sessions = signal<SessionResponseDTO[]>([]);
  selectedCarePlan = signal<CarePlanResponseDTO | null>(null);

  // UI state signals
  showCreateDialog = signal(false);
  showViewDialog = signal(false);
  isLoadingCarePlans = signal(false);
  isLoadingSessions = signal(false);
  isSubmitting = signal(false);
  editingCarePlanId = signal<number | null>(null);
  errorMessage = signal<string | null>(null);

  // Form
  carePlanForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly carePlanService: CarePlanService,
    private readonly medicalFolderService: MedicalFolderService,
    private readonly sessionService: SessionService
  ) {
    this.carePlanForm = this.createCarePlanForm();
  }

  get activities(): FormArray {
    return this.carePlanForm.get('activities') as FormArray;
  }

  ngOnInit(): void {
    this.loadCarePlans();
    this.loadSessions();
  }

  // ==================== Form Creation ====================

  private createCarePlanForm(): FormGroup {
    return this.fb.group({
      sessionId: [null, Validators.required],
      activities: this.fb.array([], [Validators.required, Validators.minLength(1)])
    });
  }

  private createCareActivityFormGroup(activity?: CareActivityRequestDTO): FormGroup {
    return this.fb.group({
      activityName: [activity?.activityName || '', Validators.required],
      description: [activity?.description || '', Validators.required],
      frequency: [activity?.frequency || '', Validators.required],
      duration: [activity?.duration || '', Validators.required],
      completionStatus: [activity?.completionStatus || 'Pending']
    });
  }

  // ==================== Data Loading ====================

  loadCarePlans(): void {
    if (!this.patient?.id) {
      console.warn('[CarePlanManagement] No patient ID available');
      return;
    }

    this.isLoadingCarePlans.set(true);
    this.carePlanService.getCarePlansByPatient(this.patient.id.toString())
      .pipe(
        tap(plans => this.carePlans.set(plans)),
        catchError(err => {
          console.error('[CarePlanManagement] Failed to load care plans', err);
          this.errorMessage.set('Failed to load care plans');
          return of([]);
        }),
        finalize(() => this.isLoadingCarePlans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadSessions(): void {
    if (!this.patient?.id) {
      console.warn('[CarePlanManagement] No patient ID available');
      return;
    }

    if (!this.doctor) {
      console.warn('[CarePlanManagement] No doctor info available');
      return;
    }

    this.isLoadingSessions.set(true);
    const currentDoctorDbId = String(this.doctor.id);
    const patientDbId = String(this.patient.id);

    console.log('[CarePlanManagement] Loading sessions for patient:', patientDbId, 'doctor:', currentDoctorDbId);

    this.medicalFolderService.getMedicalFoldersByPatient(patientDbId)
      .pipe(
        tap(folders => {
          console.log('[CarePlanManagement] Medical folders:', folders);

          const matchingFolder = folders.find(f => f.idDoctor === currentDoctorDbId);

          if (matchingFolder) {
            console.log('[CarePlanManagement] Found matching folder:', matchingFolder);
            this.loadSessionsForFolder(matchingFolder.id);
          } else {
            console.warn('[CarePlanManagement] No matching folder found');
            this.sessions.set([]);
            this.isLoadingSessions.set(false);
          }
        }),
        catchError(error => {
          console.error('[CarePlanManagement] Error loading medical folders:', error);
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
          console.log('[CarePlanManagement] Loaded sessions:', sessions);
          this.sessions.set(sessions);
        }),
        catchError(error => {
          console.error('[CarePlanManagement] Error loading sessions:', error);
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
    this.editingCarePlanId.set(null);
    this.carePlanForm.reset();
    this.activities.clear();
    this.addCareActivity(); // Add one empty activity
    this.showCreateDialog.set(true);
  }

  openEditDialog(carePlan: CarePlanResponseDTO): void {
    this.editingCarePlanId.set(carePlan.id);
    this.carePlanForm.patchValue({
      sessionId: carePlan.sessionId
    });

    this.activities.clear();
    carePlan.activities.forEach(activity => {
      this.activities.push(this.createCareActivityFormGroup(activity));
    });

    this.showCreateDialog.set(true);
  }

  closeCreateDialog(): void {
    this.showCreateDialog.set(false);
    this.carePlanForm.reset();
    this.activities.clear();
    this.errorMessage.set(null);
  }

  addCareActivity(): void {
    this.activities.push(this.createCareActivityFormGroup());
  }

  removeCareActivity(index: number): void {
    this.activities.removeAt(index);
  }

  viewCarePlan(carePlan: CarePlanResponseDTO): void {
    this.selectedCarePlan.set(carePlan);
    this.showViewDialog.set(true);
  }

  closeViewDialog(): void {
    this.showViewDialog.set(false);
    this.selectedCarePlan.set(null);
  }

  saveCarePlan(): void {
    if (this.carePlanForm.invalid) {
      this.carePlanForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const carePlanData: CarePlanRequestDTO = this.carePlanForm.value;
    const id = this.editingCarePlanId();

    const request$ = id
      ? this.carePlanService.updateCarePlan(id, carePlanData)
      : this.carePlanService.createCarePlan(carePlanData);

    request$.pipe(
      tap(() => {
        this.loadCarePlans();
        this.closeCreateDialog();
      }),
      catchError(err => {
        console.error('Failed to save care plan', err);
        this.errorMessage.set('Failed to save care plan. Please try again.');
        return of(null);
      }),
      finalize(() => this.isSubmitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
}
