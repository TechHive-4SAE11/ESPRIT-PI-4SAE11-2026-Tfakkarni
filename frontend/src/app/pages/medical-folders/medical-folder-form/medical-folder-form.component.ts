import { Component, inject, Input, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import type { MedicalFolder, CreateMedicalFolderRequest, UpdateMedicalFolderRequest } from '@/core/services/medical-folder.service';
import type { UserInfo } from '@/core/services/user-api.service';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-medical-folder-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZardButtonComponent, ZardIconComponent, ZardInputDirective],
  template: `
    <form (ngSubmit)="onSubmit()" class="flex flex-col gap-4">
      <!-- Doctor Name (Read-only, auto-filled) -->
      <div>
        <label for="doctorName" class="block text-sm font-medium mb-1">Doctor Name</label>
        <input
          id="doctorName"
          type="text"
          z-input
          class="w-full"
          [value]="doctorName()"
          readonly
          placeholder="Doctor name"
        />
      </div>

      <!-- Patient Selection with Dropdown -->
      <div>
        <label for="patientSearch" class="block text-sm font-medium mb-1">Patient</label>
        <div class="relative">
          <input
            id="patientSearch"
            type="text"
            z-input
            class="w-full"
            [value]="selectedPatientDisplay() || searchPatientInput()"
            (input)="searchPatients($any($event.target).value)"
            (focus)="showPatientDropdown.set(true)"
            (blur)="closeDropdownDelayed()"
            placeholder="Search patient by name or ID..."
          />
          @if (showPatientDropdown() && filteredPatients().length > 0) {
            <div class="absolute top-full left-0 right-0 mt-1 bg-card border border-border rounded-md shadow-lg z-10 max-h-48 overflow-y-auto">
              @for (patient of filteredPatients(); track patient.keycloakId) {
                <button
                  type="button"
                  class="w-full text-left px-3 py-2 hover:bg-accent text-sm border-b border-border last:border-b-0"
                  (mousedown)="$event.preventDefault()"
                  (click)="selectPatient(patient)"
                >
                  <div class="font-medium">{{ patient.firstName }} {{ patient.lastName }}</div>
                  <div class="text-xs text-muted-foreground">{{ patient.email }}</div>
                </button>
              }
            </div>
          }
        </div>
        @if (form.controls.patientId.touched && form.controls.patientId.errors) {
          <p class="text-destructive text-sm mt-1">{{ form.controls.patientId.errors['required'] ? 'Patient is required' : '' }}</p>
        }
      </div>

      <div class="flex gap-2 justify-end pt-2">
        <button type="button" z-button zType="outline" (click)="onCancel()">Cancel</button>
        <button type="submit" z-button [disabled]="form.invalid || isSubmitting()">
          {{ isSubmitting() ? 'Creating...' : (folderId() ? 'Update' : 'Create') }}
        </button>
      </div>

      @if (form.invalid && form.touched) {
        <div class="mt-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 rounded text-sm text-red-700">
          <p class="font-medium mb-1">Form validation errors:</p>
          <ul class="list-disc list-inside">
            @if (form.controls.patientId.touched && form.controls.patientId.errors?.['required']) {
              <li>Patient is required</li>
            }
          </ul>
        </div>
      }
    </form>
  `,
})
export class MedicalFolderFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userApiService = inject(UserApiService);

  @Input() set folder(value: MedicalFolder | null) {
    if (value) {
      this.folderId.set(value.id);
      // Only set patientId - doctorId comes from JWT token
      this.form.patchValue({ patientId: value.patientId });
      this.selectedPatientDisplay.set(value.patientId);
    } else {
      this.folderId.set(null);
      this.form.reset();
      this.selectedPatientDisplay.set('');
      this.searchPatientInput.set('');
      this.showPatientDropdown.set(false);
    }
  }

  @Input() doctor: UserInfo | null = null;

  folderId = signal<number | null>(null);
  isSubmitting = signal(false);
  patients = signal<UserInfo[]>([]);
  searchPatientInput = signal('');
  selectedPatientDisplay = signal('');
  showPatientDropdown = signal(false);
  
  onSubmitCallback: ((data: CreateMedicalFolderRequest | UpdateMedicalFolderRequest) => void) | null = null;
  onCancelCallback: (() => void) | null = null;

  doctorName = computed(() => {
    if (!this.doctor) return '';
    return `${this.doctor.firstName} ${this.doctor.lastName}`;
  });

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
    patientId: ['', Validators.required],
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

  onSubmit(): void {
    this.form.markAllAsTouched();
    
    // Log validation state
    console.log('Form submitted', {
      valid: this.form.valid,
      patientId: this.form.value.patientId,
      formValue: this.form.value,
    });
    
    if (this.form.invalid) {
      console.warn('Form is invalid, cannot submit');
      return;
    }
    
    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    
    // Only send patientId - doctorId will be extracted from JWT token on backend
    const payload = { patientId: value.patientId };
    
    if (this.onSubmitCallback) {
      try {
        this.onSubmitCallback(payload as CreateMedicalFolderRequest);
      } catch (err) {
        console.error('Error calling submit callback', err);
        this.isSubmitting.set(false);
      }
    } else {
      console.warn('No submit callback set');
      this.isSubmitting.set(false);
    }
  }

  onCancel(): void {
    if (this.onCancelCallback) this.onCancelCallback();
  }}