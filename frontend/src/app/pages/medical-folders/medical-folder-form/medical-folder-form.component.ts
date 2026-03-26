import { Component, inject, Input, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { z } from 'zod';
import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { Z_MODAL_DATA } from '@/shared/components/dialog/dialog.service';
import type { MedicalFolder, CreateMedicalFolderRequest, UpdateMedicalFolderRequest } from '@/core/services/medical-folder.service';
import type { UserInfo } from '@/core/services/user-api.service';
import { UserApiService } from '@/core/services/user-api.service';

export interface MedicalFolderDialogData {
  callbacks?: {
    onSubmit?: (data: CreateMedicalFolderRequest | UpdateMedicalFolderRequest) => void;
    onCancel?: () => void;
  };
}

// ─── Zod Validation Schema ──────────────────────────────────────────────────────
const medicalFolderSchema = z.object({
  patientId: z.string()
    .min(1, { message: 'Patient selection is required' })
    .trim(),
  bloodType: z.string()
    .max(10, { message: 'Blood type must not exceed 10 characters' })
    .optional()
    .or(z.literal(''))
    .transform(v => v?.trim() || undefined),
  height: z.preprocess(
    (val) => (val === '' || val === null ? undefined : val),
    z.coerce.number()
      .min(0, { message: 'Height must be positive' })
      .max(300, { message: 'Height must not exceed 300 cm' })
  ).nullable().optional(),
  weight: z.preprocess(
    (val) => (val === '' || val === null ? undefined : val),
    z.coerce.number()
      .min(0, { message: 'Weight must be positive' })
      .max(1000, { message: 'Weight must not exceed 1000 kg' })
  ).nullable().optional(),
});

@Component({
  selector: 'app-medical-folder-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZardButtonComponent, ZardIconComponent, ZardInputDirective],
  template: `
    <form (ngSubmit)="onSubmit($event)" class="flex flex-col gap-5 p-1">
      <!-- Patient Selection -->
      <div class="space-y-1.5">
        <label for="patientSearch" class="block text-sm font-semibold text-muted-foreground">Patient</label>
        <div class="relative group">
          <input
            id="patientSearch"
            type="text"
            z-input
            class="w-full focus:ring-primary/20"
            [value]="selectedPatientDisplay() || searchPatientInput()"
            (input)="searchPatients($any($event.target).value)"
            (focus)="showPatientDropdown.set(true)"
            (blur)="closeDropdownDelayed()"
            placeholder="Search patient by name or ID..."
          />
          @if (showPatientDropdown() && filteredPatients().length > 0) {
            <div class="absolute top-full left-0 right-0 mt-2 bg-background border border-border rounded-xl shadow-xl z-[100] max-h-56 overflow-y-auto animate-in fade-in zoom-in-95 duration-200 p-1 custom-scrollbar">
              @for (patient of filteredPatients(); track patient.keycloakId) {
                <button
                  type="button"
                  class="w-full text-left px-4 py-2.5 hover:bg-muted rounded-lg text-sm transition-colors mb-0.5 last:mb-0 group/item"
                  (mousedown)="$event.preventDefault()"
                  (click)="selectPatient(patient)"
                >
                  <div class="font-semibold group-hover/item:text-primary transition-colors">{{ patient.firstName }} {{ patient.lastName }}</div>
                  <div class="text-xs text-muted-foreground">{{ patient.email }}</div>
                </button>
              }
            </div>
          }
        </div>
        @if ((form.controls.patientId.touched || formSubmitted()) && form.controls.patientId.errors) {
          <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.patientId.errors['message'] || 'Patient selection is required' }}</p>
        }
      </div>

      <!-- Medical Information -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-muted/30 rounded-2xl border border-border/50">
        <div class="space-y-1.5">
          <label for="bloodType" class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">Blood Type</label>
          <input
            id="bloodType"
            type="text"
            z-input
            class="w-full h-9 text-center font-bold"
            [formControl]="form.controls.bloodType"
            placeholder="e.g., A+"
            maxlength="10"
          />
          @if ((form.controls.bloodType.touched || formSubmitted()) && form.controls.bloodType.errors) {
            <p class="text-destructive text-[10px] mt-1 font-medium">{{ form.controls.bloodType.errors['message'] }}</p>
          }
        </div>

        <div class="space-y-1.5">
          <label for="height" class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">Height (cm)</label>
          <input
            id="height"
            type="number"
            z-input
            class="w-full h-9 text-center"
            [formControl]="form.controls.height"
            placeholder="175"
            min="0"
            max="300"
          />
          @if ((form.controls.height.touched || formSubmitted()) && form.controls.height.errors) {
            <p class="text-destructive text-[10px] mt-1 font-medium">{{ form.controls.height.errors['message'] }}</p>
          }
        </div>

        <div class="space-y-1.5">
          <label for="weight" class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">Weight (kg)</label>
          <input
            id="weight"
            type="number"
            z-input
            class="w-full h-9 text-center"
            [formControl]="form.controls.weight"
            placeholder="70"
            min="0"
            max="1000"
          />
          @if ((form.controls.weight.touched || formSubmitted()) && form.controls.weight.errors) {
            <p class="text-destructive text-[10px] mt-1 font-medium">{{ form.controls.weight.errors['message'] }}</p>
          }
        </div>
      </div>

      <div class="flex gap-3 justify-end pt-5 border-t border-border mt-2">
        <button type="button" z-button zType="outline" class="min-w-[100px]" (click)="onCancel()">Annuler</button>
        <button type="button" z-button [disabled]="(form.invalid && formSubmitted()) || isSubmitting()" class="min-w-[120px]" (click)="onSubmit($event)">
          {{ isSubmitting() ? 'Envoi...' : (folderId() ? 'Enregistrer' : 'Créer') }}
        </button>
      </div>

      @if (form.invalid && formSubmitted()) {
        <div class="mt-2 p-3 bg-destructive/5 border border-destructive/10 rounded-xl text-xs text-destructive animate-in bounce-in-95">
            <p class="font-bold flex items-center mb-2 uppercase tracking-tight">
                <z-icon zType="circle-alert" size="14" class="mr-1.5"></z-icon>
                Veuillez corriger les erreurs suivantes :
            </p>
            <ul class="list-disc list-inside space-y-1 ml-1 opacity-90">
                @if (form.controls.patientId.errors) { <li>Sélectionnez un patient</li> }
                @if (form.controls.bloodType.errors) { <li>{{ form.controls.bloodType.errors['message'] }}</li> }
                @if (form.controls.height.errors) { <li>{{ form.controls.height.errors['message'] }}</li> }
                @if (form.controls.weight.errors) { <li>{{ form.controls.weight.errors['message'] }}</li> }
            </ul>
        </div>
      }
    </form>
  `,
  styles: [`
    :host {
      display: block;
      max-width: 100%;
      overflow-x: hidden;
    }
    form {
      width: 100%;
    }
  `]
})
export class MedicalFolderFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userApiService = inject(UserApiService);
  private readonly modalData = inject<MedicalFolderDialogData | null>(Z_MODAL_DATA, { optional: true });

  @Input() set folder(value: MedicalFolder | null) {
    if (value) {
      this.folderId.set(value.id);
      // Only set patientId - doctorId comes from JWT token
      this.form.patchValue({
        patientId: value.patientId,
        bloodType: value.bloodType || '',
        height: value.height || null,
        weight: value.weight || null
      });
      this.selectedPatientDisplay.set(value.patientId);
    } else {
      this.folderId.set(null);
      this.form.reset();
      this.formSubmitted.set(false);
      this.selectedPatientDisplay.set('');
      this.searchPatientInput.set('');
      this.showPatientDropdown.set(false);
    }
  }

  @Input() doctor: UserInfo | null = null;

  folderId = signal<number | null>(null);
  isSubmitting = signal(false);
  formSubmitted = signal(false);
  patients = signal<UserInfo[]>([]);
  searchPatientInput = signal('');
  selectedPatientDisplay = signal('');
  showPatientDropdown = signal(false);

  onSubmitCallback: ((data: CreateMedicalFolderRequest | UpdateMedicalFolderRequest) => void) | null = null;
  onCancelCallback: (() => void) | null = null;

  filteredPatients = computed(() => {
    const search = this.searchPatientInput().toLowerCase().trim();
    if (!search) return this.patients();
    return this.patients().filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(search) ||
      p.email?.toLowerCase().includes(search) ||
      p.keycloakId?.toLowerCase().includes(search)
    );
  });

  form = this.fb.nonNullable.group({
    patientId: ['', createZodValidator(medicalFolderSchema.shape.patientId)],
    bloodType: ['', createZodValidator(medicalFolderSchema.shape.bloodType)],
    height: [null as number | null, createZodValidator(medicalFolderSchema.shape.height)],
    weight: [null as number | null, createZodValidator(medicalFolderSchema.shape.weight)],
    // doctorId is extracted from JWT token on backend, not needed in form
  });

  ngOnInit(): void {
    // Doctor ID is automatically extracted from JWT token on backend
    // No need to set it in the form
    this.loadPatients();
  }

  private loadPatients(): void {
    this.userApiService.getUsersByRole('patient').subscribe({
      next: patients => this.patients.set(patients),
      error: err => console.error('Failed to load patients', err),
    });
  }

  searchPatients(value: string): void {
    this.searchPatientInput.set(value);
  }

  selectPatient(patient: UserInfo): void {
    this.form.patchValue({ patientId: patient.keycloakId });
    this.selectedPatientDisplay.set(`${patient.firstName} ${patient.lastName}`);
    this.searchPatientInput.set('');
    this.showPatientDropdown.set(false);
  }

  closeDropdownDelayed(): void {
    setTimeout(() => this.showPatientDropdown.set(false), 200);
  }

  onSubmit(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.formSubmitted.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.isSubmitting.set(true);
    const payload = {
      patientId: this.form.getRawValue().patientId,
      bloodType: this.form.getRawValue().bloodType || undefined,
      height: this.form.getRawValue().height || undefined,
      weight: this.form.getRawValue().weight || undefined
    } as CreateMedicalFolderRequest;
    const submitFn = this.modalData?.callbacks?.onSubmit ?? this.onSubmitCallback;

    if (submitFn) {
      try {
        submitFn(payload);
      } catch (err) {
        this.isSubmitting.set(false);
      }
    } else {
      this.isSubmitting.set(false);
    }
  }

  onCancel(): void {
    (this.modalData?.callbacks?.onCancel ?? this.onCancelCallback)?.();
  }
}