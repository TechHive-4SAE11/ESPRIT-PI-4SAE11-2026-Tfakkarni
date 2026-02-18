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
import { CarePlanService } from '@/core/services/care-plan.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';
import {
  CarePlanResponseDTO,
  CarePlanRequestDTO,
  CareActivityRequestDTO,
  CareActivityType
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
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly fb = inject(FormBuilder);
  private readonly carePlanService = inject(CarePlanService);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly sessionService = inject(SessionService);

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;

  // State signals
  carePlans = signal<CarePlanResponseDTO[]>([]);
  sessions = signal<SessionResponseDTO[]>([]);
  selectedCarePlan = signal<CarePlanResponseDTO | null>(null);

  isLoadingCarePlans = signal(false);
  isLoadingSessions = signal(false);
  isSubmitting = signal(false);

  editingCarePlanId = signal<number | null>(null);
  showCreateDialog = signal(false);
  showViewDialog = signal(false);
  errorMessage = signal<string | null>(null);

  carePlanForm!: FormGroup;

  // Constants
  CareActivityType = CareActivityType;

  // Computed properties for template
  physicalActivities = computed(() => {
    const plan = this.selectedCarePlan();
    return plan ? plan.activities.filter(a => a.activityType === CareActivityType.PHYSICAL_ACTIVITY) : [];
  });

  nutritionActivities = computed(() => {
    const plan = this.selectedCarePlan();
    return plan ? plan.activities.filter(a => a.activityType === CareActivityType.NUTRITION_PLAN) : [];
  });

  // Zod Schemas
  private readonly activityFieldSchemas = {
    activityName: z.string().min(1, { message: 'Please enter the activity name' }),
    description: z.string().min(1, { message: 'Please provide a description of the activity' }),
    frequency: z.string().min(1, { message: 'Please specify how often (e.g., Daily, 3x/week)' }),
    duration: z.string().min(1, { message: 'Please specify duration (e.g., 30 mins, 1 hour)' })
  };

  private readonly carePlanSchema = z.object({
    sessionId: z.union([
      z.number(),
      z.string().min(1)
    ]).refine(val => val !== null && val !== '', { message: 'Please select a consultation session' }),
    activities: z.array(z.any()).min(1, { message: 'At least one care activity is required' })
  });

  ngOnInit(): void {
    this.carePlanForm = this.createCarePlanForm();
    this.loadCarePlans();
    this.loadSessions();
  }

  get activities(): FormArray {
    return this.carePlanForm.get('activities') as FormArray;
  }

  // Helper method to get activity control
  getActivityControl(index: number, controlName: string): AbstractControl | null {
    const activityGroup = this.activities.at(index) as FormGroup;
    return activityGroup?.get(controlName);
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

  // ==================== Form Creation ====================

  private createCarePlanForm(): FormGroup {
    return this.fb.group({
      sessionId: [null, createZodValidator(this.carePlanSchema.shape.sessionId)],
      activities: this.fb.array([], [createZodValidator(this.carePlanSchema.shape.activities)])
    });
  }

  private createCareActivityFormGroup(activity?: CareActivityRequestDTO): FormGroup {
    const group = this.fb.group({
      activityName: [activity?.activityName || '', createZodValidator(this.activityFieldSchemas.activityName)],
      activityType: [activity?.activityType || CareActivityType.PHYSICAL_ACTIVITY],
      description: [activity?.description || '', createZodValidator(this.activityFieldSchemas.description)],
      frequency: [activity?.frequency || ''],
      duration: [activity?.duration || ''],
      completionStatus: [activity?.completionStatus || 'Pending']
    });

    // Set initial validators
    this.updateActivityValidators(group);

    // Subscribe to changes
    group.get('activityType')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.updateActivityValidators(group);
    });

    return group;
  }

  private updateActivityValidators(group: FormGroup): void {
    const type = group.get('activityType')?.value;
    const freqControl = group.get('frequency');
    const durControl = group.get('duration');

    const requiredValidator = createZodValidator(z.string().min(1, { message: 'Required' }));

    if (type === CareActivityType.PHYSICAL_ACTIVITY) {
      freqControl?.setValidators([requiredValidator]);
      durControl?.setValidators([requiredValidator]);
    } else {
      freqControl?.clearValidators();
      durControl?.clearValidators();
    }
    freqControl?.updateValueAndValidity();
    durControl?.updateValueAndValidity();
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
    if (!this.patient?.id) return;
    if (!this.doctor) return;

    this.isLoadingSessions.set(true);
    const currentDoctorDbId = String(this.doctor.id);
    const patientDbId = String(this.patient.id);

    this.medicalFolderService.getMedicalFoldersByPatient(patientDbId)
      .pipe(
        tap(folders => {
          const matchingFolder = folders.find(f => f.idDoctor === currentDoctorDbId);
          if (matchingFolder) {
            this.loadSessionsForFolder(matchingFolder.id);
          } else {
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
    this.sessionService.getSessionsWhereNoCarePlan(folderId)
      .pipe(
        tap(sessions => this.sessions.set(sessions)),
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
    console.log('CarePlanManagement: openCreateDialog called');
    try {
      this.editingCarePlanId.set(null);
      this.carePlanForm.reset();
      this.activities.clear();
      this.addCareActivity(); // Add one empty activity
      this.showCreateDialog.set(true);
    } catch (error) {
      console.error('CarePlanManagement: Error opening create dialog', error);
      this.errorMessage.set('An error occurred while opening the dialog');
    }
  }

  openEditDialog(carePlan: CarePlanResponseDTO): void {
    this.editingCarePlanId.set(carePlan.id);
    this.carePlanForm.patchValue({
      sessionId: carePlan.sessionId
    });

    this.activities.clear();
    // Correctly using 'activities' from response DTO
    carePlan.activities.forEach(activity => {
      // Create stub DTO from response for form creation
      const requestDto: CareActivityRequestDTO = {
        activityName: activity.activityName,
        activityType: activity.activityType,
        description: activity.description,
        frequency: activity.frequency,
        duration: activity.duration,
        completionStatus: activity.completionStatus
      };
      this.activities.push(this.createCareActivityFormGroup(requestDto));
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
    // Mark all fields as touched to show validation errors
    this.carePlanForm.markAllAsTouched();
    this.activities.controls.forEach(control => {
      control.markAllAsTouched();
    });

    if (this.carePlanForm.invalid) {
      this.errorMessage.set('Please correct the errors below before submitting');
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

  deleteCarePlan(id: number): void {
    this.alertDialog.confirm({
      zTitle: 'Delete Care Plan',
      zDescription: 'Are you sure you want to delete this Care Plan? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.carePlanService.deleteCarePlan(id)
          .pipe(
            tap(() => {
              this.loadCarePlans();
              if (this.selectedCarePlan()?.id === id) {
                this.closeViewDialog();
              }
            }),
            catchError(err => {
              console.error('Failed to delete care plan', err);
              this.errorMessage.set('Failed to delete care plan. Please try again.');
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    });
  }
}
