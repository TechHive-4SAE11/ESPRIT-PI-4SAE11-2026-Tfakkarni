import { Component, Input, OnInit, OnDestroy, signal, computed, DestroyRef, inject, ViewChild, TemplateRef, ViewContainerRef, ApplicationRef, Injector, PLATFORM_ID } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { DomPortalOutlet, TemplatePortal } from '@angular/cdk/portal';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormArray, AbstractControl, Validators } from '@angular/forms';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';
import { z } from 'zod';

import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog/alert-dialog.service';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardPaginationComponent } from '@/shared/components/pagination';
import { PagedResponse } from '@/core/models/paged-response.model';

import { UserInfo } from '@/core/services/user-api.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { PrescriptionTemplateService } from '@/core/services/prescription-template.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';
import { MedicationService } from '@/core/services/medication.service';
import {
  PrescriptionResponseDTO,
  PrescriptionRequestDTO,
  MedicationRequestDTO,
  MedicationStatus
} from '@/core/models/prescription.model';
import {
  PrescriptionTemplateResponseDTO,
  PrescriptionTemplateRequestDTO
} from '@/core/models/prescription-template.model';

import { HttpClient } from '@angular/common/http';
import { environment } from '@/environments/environment';

@Component({
  selector: 'app-prescription-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZardButtonComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardInputDirective,
    ZardPaginationComponent
  ],
  templateUrl: './prescription-management.component.html',
})
export class PrescriptionManagementComponent implements OnInit, OnDestroy {
  private readonly destroyRef = inject(DestroyRef);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly fb = inject(FormBuilder);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly medicationService = inject(MedicationService);
  private readonly templateService = inject(PrescriptionTemplateService);

  // CDK Portal - render dialogs at body level to escape overflow:auto containers
  private readonly appRef = inject(ApplicationRef);
  private readonly injector = inject(Injector);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly platformId = inject(PLATFORM_ID);
  @ViewChild('createDialogTpl') private createDialogTpl!: TemplateRef<any>;
  @ViewChild('viewDialogTpl') private viewDialogTpl!: TemplateRef<any>;
  private createDialogPortal: DomPortalOutlet | null = null;
  private viewDialogPortal: DomPortalOutlet | null = null;
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly sessionService = inject(SessionService);
  private readonly http = inject(HttpClient);

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;
  @Input() setPage!: (page: string) => void;

  // State signals
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  sessions = signal<SessionResponseDTO[]>([]);
  selectedPrescription = signal<PrescriptionResponseDTO | null>(null);
  discontinueReason = signal('');

  // Pagination signals
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalItems = signal<number>(0);
  pageSize = signal<number>(5);
  private currentMedicationToDiscontinue: number | null = null;

  // Template signals
  templates = signal<PrescriptionTemplateResponseDTO[]>([]);
  showSaveTemplateDialog = signal(false);
  showManageTemplates = signal(false);
  selectedTemplate = signal<PrescriptionTemplateResponseDTO | null>(null);
  showViewTemplateDialog = signal(false);
  templateName = signal('');
  templateDescription = signal('');
  isSavingTemplate = signal(false);
  saveTemplatePrescriptionId = signal<number | null>(null);

  // UI state signals
  showCreateDialog = signal(false);
  showViewDialog = signal(false);
  showDiscontinueDialog = signal(false);
  isLoadingPrescriptions = signal(false);
  isLoadingSessions = signal(false);
  isSubmitting = signal(false);
  editingPrescriptionId = signal<number | null>(null);
  errorMessage = signal<string | null>(null);

  // Quick-create session (inline in prescription form)
  showQuickCreateSession = signal(false);
  quickSessionDate = signal(new Date().toISOString().split('T')[0]);
  quickSessionNotes = signal('');
  quickSessionError = signal<string | null>(null);
  isCreatingSession = signal(false);
  todayString = new Date().toISOString().split('T')[0];

  // Track the medical folder ID for quick session creation
  private matchingFolderId = signal<number | null>(null);

  prescriptionForm!: FormGroup;

  // Zod Schemas
  private readonly medicationFieldSchemas = {
    medicationName: z.string()
      .min(1, { message: 'Medication name is required' })
      .max(200, { message: 'Medication name is too long (max 200 characters)' }),
    dosage: z.string()
      .min(1, { message: 'Dosage is required' })
      .refine(
        (val) => val.trim().length >= 2,
        { message: 'Please specify a complete dosage (e.g., 500mg, 2 tablets, 10ml)' }
      ),
    frequency: z.string()
      .min(1, { message: 'Frequency is required' })
      .refine(
        (val) => val.trim().length >= 3,
        { message: 'Please specify intake frequency (e.g., 2x/day, every 8 hours, once daily)' }
      ),
    duration: z.string()
      .min(1, { message: 'Treatment duration is required' })
      .refine(
        (val) => {
          // Match format: "number timeunit" or "ongoing"
          // Supported: "3 days", "2 weeks", "1 month", "ongoing" (English or French)
          const pattern = /^(\d+\s*(days?|jours?|weeks?|semaines?|months?|mois))$|^(ongoing|en cours)$/i;
          return pattern.test(val.trim());
        },
        { message: 'Invalid duration format. Examples: "7 days", "2 weeks", "3 months", or "ongoing"' }
      ),
    instructions: z.string()
      .max(1000, { message: 'Instructions are too long (max 1000 characters)' })
      .optional()
  };

  private readonly prescriptionSchema = z.object({
    sessionId: z.union([
      z.number(),
      z.string().min(1)
    ]).refine(
      val => val !== null && val !== '',
      { message: 'You must select a consultation session before creating a prescription' }
    ),
    medications: z.array(z.any())
      .min(1, { message: 'Please add at least one medication to the prescription' })
      .max(20, { message: 'Too many medications (maximum 20 per prescription)' })
  });

  ngOnInit(): void {
    this.prescriptionForm = this.createPrescriptionForm();
    this.loadPrescriptions();
    this.loadSessions();
    this.loadTemplates();
  }

  get medications(): FormArray {
    return this.prescriptionForm.get('medications') as FormArray;
  }

  // ==================== Medication Status Display Helpers ====================

  getStatusBadgeType(status: MedicationStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case MedicationStatus.ACTIVE:
        return 'default';
      case MedicationStatus.ONGOING:
        return 'secondary';
      case MedicationStatus.EXPIRED:
        return 'destructive';
      case MedicationStatus.DISCONTINUED:
        return 'outline';
      default:
        return 'outline';
    }
  }

  getStatusLabel(status: MedicationStatus): string {
    switch (status) {
      case MedicationStatus.ACTIVE:
        return 'Active';
      case MedicationStatus.ONGOING:
        return 'Ongoing';
      case MedicationStatus.EXPIRED:
        return 'Expired';
      case MedicationStatus.DISCONTINUED:
        return 'Discontinued';
      default:
        return status;
    }
  }

  getStatusIcon(status: MedicationStatus): 'check' | 'activity' | 'x' | 'alert-triangle' | 'info' {
    switch (status) {
      case MedicationStatus.ACTIVE:
        return 'check';
      case MedicationStatus.ONGOING:
        return 'activity';
      case MedicationStatus.EXPIRED:
        return 'x';
      case MedicationStatus.DISCONTINUED:
        return 'alert-triangle';
      default:
        return 'info';
    }
  }

  // ==================== Medication Status Management ====================

  /**
   * Change medication status (e.g., discontinue)
   */
  changeMedicationStatus(medicationId: number, currentStatus: MedicationStatus): void {
    // Show dialog to select new status and reason
    const statusOptions = Object.values(MedicationStatus)
      .filter(s => s !== currentStatus)
      .map(s => ({ value: s, label: this.getStatusLabel(s) }));

    this.alertDialog.confirm({
      zTitle: 'Change Medication Status',
      zDescription: `Select new status for this medication. Current status: ${this.getStatusLabel(currentStatus)}`,
      zOkText: 'Update Status',
      zCancelText: 'Cancel',
      zOnOk: () => {
        // For now, we'll just discontinue. In a full implementation, you'd show a form
        this.discontinueMedication(medicationId);
      }
    });
  }

  /**
   * Open dialog to discontinue a medication
   */
  discontinueMedication(medicationId: number): void {
    this.currentMedicationToDiscontinue = medicationId;
    this.discontinueReason.set('');
    this.showDiscontinueDialog.set(true);
  }

  /**
   * Close discontinue dialog
   */
  closeDiscontinueDialog(): void {
    this.showDiscontinueDialog.set(false);
    this.currentMedicationToDiscontinue = null;
    this.discontinueReason.set('');
  }

  /**
   * Confirm discontinuing the medication
   */
  confirmDiscontinue(): void {
    if (!this.currentMedicationToDiscontinue) return;

    const medicationId = this.currentMedicationToDiscontinue;
    const reason = this.discontinueReason().trim();

    this.medicationService.updateMedicationStatus(medicationId, {
      status: MedicationStatus.DISCONTINUED,
      reason: reason || undefined
    }).pipe(
      tap(() => {
        // Reload prescriptions to show updated status
        this.loadPrescriptions();
        this.closeDiscontinueDialog();
        this.alertDialog.info({
          zTitle: 'Success',
          zDescription: 'Medication has been discontinued successfully.',
          zOkText: 'OK'
        });
      }),
      catchError(error => {
        console.error('Error updating medication status:', error);
        this.alertDialog.info({
          zTitle: 'Error',
          zDescription: 'Failed to discontinue medication. Please try again.',
          zOkText: 'OK'
        });
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
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
    // Zod validator may also store messages under 'required' key with custom message
    if (errors['required']) {
      // If it's a string, it's a Zod custom message - return it
      if (typeof errors['required'] === 'string') {
        return errors['required'];
      }
      // Otherwise it's Angular's required validator
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
    const patientDbId = this.patient.keycloakId || String(this.patient.id);

    this.prescriptionService.getPrescriptionsByPatientPaginated(
      patientDbId,
      this.currentPage(),
      this.pageSize()
    ).pipe(
        tap((response: PagedResponse<PrescriptionResponseDTO>) => {
          console.log('[PrescriptionManagement] Loaded prescriptions:', response);
          this.prescriptions.set(response.content);
          this.totalPages.set(response.totalPages);
          this.totalItems.set(response.totalElements);
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading prescriptions:', error);
          this.prescriptions.set([]);
          return of(null);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadPrescriptions();
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
    // Use keycloakId if available, fallback to id
    const currentDoctorDbId = this.doctor.keycloakId || String(this.doctor.id);
    const patientDbId = this.patient.keycloakId || String(this.patient.id);

    console.log('[PrescriptionManagement] Loading sessions for patient:', patientDbId, 'doctor:', currentDoctorDbId);

    // Try to find the medical folder in medical-service (using the correct endpoint)
    this.sessionService.getSessionsByPatient(patientDbId)
      .pipe(
        switchMap(() => {
          // Use medical-service endpoint which is correctly mapped and exists
          return this.http.get<any[]>(`${environment.apiBaseUrl}/api/medical-folders/patient/${patientDbId}/doctor/${currentDoctorDbId}`);
        }),
        tap(folders => {
          console.log('[PrescriptionManagement] Tracking-service medical folders:', folders);

          const matchingFolder = folders[0]; // The endpoint /patient/{pId}/doctor/{dId} returns a list

          if (matchingFolder) {
            console.log('[PrescriptionManagement] Found matching folder in tracking-service:', matchingFolder);
            this.matchingFolderId.set(matchingFolder.id);
            this.loadSessionsForFolder(matchingFolder.id);
          } else {
            console.warn('[PrescriptionManagement] No matching folder found in tracking-service');
            this.sessions.set([]);
            this.matchingFolderId.set(null);
            this.isLoadingSessions.set(false);
          }
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading tracking folders:', error);
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

  // ==================== Portal Helpers ====================

  private getOrCreatePortal(): DomPortalOutlet | null {
    if (isPlatformBrowser(this.platformId)) {
      return new DomPortalOutlet(document.body, null as any, this.appRef, this.injector);
    }
    return null;
  }

  private attachPortal(templateRef: TemplateRef<any>, outlet: DomPortalOutlet | null): DomPortalOutlet | null {
    if (!outlet) outlet = this.getOrCreatePortal();
    if (!outlet) return null;
    if (outlet.hasAttached()) outlet.detach();
    outlet.attach(new TemplatePortal(templateRef, this.viewContainerRef));
    return outlet;
  }

  private detachPortal(outlet: DomPortalOutlet | null): void {
    if (outlet?.hasAttached()) outlet.detach();
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
      this.createDialogPortal = this.attachPortal(this.createDialogTpl, this.createDialogPortal);
    } catch (error) {
      console.error('PrescriptionManagement: Error opening create dialog', error);
      this.errorMessage.set('Unable to open prescription form. Please try again.');
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
    this.createDialogPortal = this.attachPortal(this.createDialogTpl, this.createDialogPortal);
  }

  closeCreateDialog(): void {
    this.detachPortal(this.createDialogPortal);
    this.showCreateDialog.set(false);
    this.prescriptionForm.reset();
    this.medications.clear();
    this.errorMessage.set(null);
  }

  viewPrescription(prescription: PrescriptionResponseDTO): void {
    this.selectedPrescription.set(prescription);
    this.showViewDialog.set(true);
    this.viewDialogPortal = this.attachPortal(this.viewDialogTpl, this.viewDialogPortal);
  }

  closeViewDialog(): void {
    this.detachPortal(this.viewDialogPortal);
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
      this.errorMessage.set('Please review and fix the highlighted errors before saving the prescription');
      return;
    }

    if (this.medications.length === 0) {
      this.errorMessage.set('Cannot save empty prescription. Please add at least one medication');
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
          const errorMsg = error?.error?.error || error?.error?.message || error?.message || 'Failed to save prescription. Please check your input and try again.';
          this.errorMessage.set(errorMsg);
          return of(null);
        }),
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ==================== Quick-Create Session ====================

  quickCreateSession(): void {
    const date = this.quickSessionDate();
    const notes = this.quickSessionNotes().trim();
    const folderId = this.matchingFolderId();

    if (!date || !notes) {
      this.quickSessionError.set('Please fill in both date and notes.');
      return;
    }
    if (!folderId) {
      this.quickSessionError.set('No medical folder found for this patient. Create a medical folder first.');
      return;
    }

    this.isCreatingSession.set(true);
    this.quickSessionError.set(null);

    // Backend requires LocalDateTime (YYYY-MM-DDTHH:mm:ss)
    const dateTime = date.includes('T') ? date : `${date}T00:00:00`;

    this.sessionService.createSession({
      medicalFolderId: folderId,
      sessionDate: dateTime,
      notes,
    }).pipe(
      tap(created => {
        console.log('[PrescriptionManagement] Quick-created session:', created);
        // Add to sessions list and auto-select it
        this.sessions.update(list => [created, ...list]);
        this.prescriptionForm.patchValue({ sessionId: created.id });
        this.showQuickCreateSession.set(false);
        this.quickSessionNotes.set('');
      }),
      catchError(error => {
        const msg = error?.error?.message || error?.error?.error || 'Failed to create session.';
        this.quickSessionError.set(msg);
        return of(null);
      }),
      finalize(() => this.isCreatingSession.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  // ==================== Template Management ====================

  loadTemplates(): void {
    if (!this.doctor?.id) return;
    const doctorDbId = String(this.doctor.id);
    this.templateService.getTemplatesByDoctor(doctorDbId)
      .pipe(
        tap(data => {
          console.log('[PrescriptionManagement] Loaded templates:', data);
          this.templates.set(data);
        }),
        catchError(error => {
          console.error('[PrescriptionManagement] Error loading templates:', error);
          this.templates.set([]);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadFromTemplate(template: PrescriptionTemplateResponseDTO): void {
    // Clear existing medications and populate from template
    this.medications.clear();
    for (const med of template.medications) {
      const requestDto: MedicationRequestDTO = {
        medicationName: med.medicationName,
        dosage: med.dosage,
        frequency: med.frequency,
        duration: med.duration,
        instructions: med.instructions || ''
      };
      this.medications.push(this.createMedicationFormGroup(requestDto));
    }
    this.errorMessage.set(null);
  }

  openSaveTemplateDialog(prescriptionId: number | null = null): void {
    this.saveTemplatePrescriptionId.set(prescriptionId);
    this.templateName.set('');
    this.templateDescription.set('');
    this.showSaveTemplateDialog.set(true);
  }

  closeSaveTemplateDialog(): void {
    this.showSaveTemplateDialog.set(false);
    this.templateName.set('');
    this.templateDescription.set('');
    this.saveTemplatePrescriptionId.set(null);
  }

  confirmSaveTemplate(): void {
    const name = this.templateName().trim();
    if (!name) return;
    if (!this.doctor?.id) return;

    this.isSavingTemplate.set(true);
    const doctorDbId = String(this.doctor.id);
    const description = this.templateDescription().trim() || undefined;
    const prescriptionId = this.saveTemplatePrescriptionId();

    const operation$ = prescriptionId
      ? this.templateService.createFromPrescription(prescriptionId, name, doctorDbId, description)
      : this.templateService.createTemplate({
        name,
        description,
        doctorId: doctorDbId,
        medications: this.prescriptionForm.value.medications || []
      });

    operation$.pipe(
      tap(() => {
        this.closeSaveTemplateDialog();
        this.loadTemplates();
        this.alertDialog.info({
          zTitle: 'Template Saved',
          zDescription: `Template "${name}" has been saved successfully.`,
          zOkText: 'OK'
        });
      }),
      catchError(error => {
        console.error('[PrescriptionManagement] Error saving template:', error);
        this.alertDialog.info({
          zTitle: 'Error',
          zDescription: 'Failed to save template. Please try again.',
          zOkText: 'OK'
        });
        return of(null);
      }),
      finalize(() => this.isSavingTemplate.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deleteTemplate(id: number): void {
    this.alertDialog.confirm({
      zTitle: 'Delete Template',
      zDescription: 'Are you sure you want to delete this template? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.templateService.deleteTemplate(id)
          .pipe(
            tap(() => this.loadTemplates()),
            catchError(error => {
              console.error('[PrescriptionManagement] Error deleting template:', error);
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    });
  }

  ngOnDestroy(): void {
    this.createDialogPortal?.dispose();
    this.viewDialogPortal?.dispose();
  }
}
