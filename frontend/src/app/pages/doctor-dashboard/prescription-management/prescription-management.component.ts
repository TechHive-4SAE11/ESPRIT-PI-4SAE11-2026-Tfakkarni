// src/app/pages/doctor-dashboard/prescription-management/prescription-management.component.ts
import { Component, Input, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { UserInfo } from '@/core/services/user-api.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { MedicalFolderService, MedicalFolderResponseDTO } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';
import { PrescriptionResponseDTO, PrescriptionRequestDTO, MedicationRequestDTO } from '@/core/models/prescription.model';

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
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h3 class="text-xl font-semibold">Prescriptions for {{ patient.firstName }} {{ patient.lastName }}</h3>
        <button z-button (click)="openCreateDialog()">
          <z-icon zType="plus" class="mr-2" />
          New Prescription
        </button>
      </div>

      <!-- Prescriptions List -->
      @if (prescriptions().length > 0) {
        <div class="grid gap-4">
          @for (prescription of prescriptions(); track prescription.id) {
            <div z-card class="hover:shadow-md transition-shadow">
              <div z-card-content class="p-6">
                <div class="flex items-start justify-between">
                  <div class="flex-1 space-y-3">
                    <!-- Header with date -->
                    <div class="flex items-center gap-4">
                      <div class="flex items-center gap-2 text-sm">
                        <z-icon zType="calendar" class="w-4 h-4 text-muted-foreground" />
                        <span class="font-medium">{{ prescription.createdAt | date:'MMM d, y' }}</span>
                        <span class="text-muted-foreground">at {{ prescription.createdAt | date:'shortTime' }}</span>
                      </div>
                    </div>
                    
                    <!-- Medications summary -->
                    <div class="flex items-center gap-2">
                      <z-icon zType="pill" class="w-5 h-5 text-primary" />
                      <span class="text-sm font-medium">
                        {{ prescription.medications.length }} 
                        {{ prescription.medications.length === 1 ? 'Medication' : 'Medications' }}
                      </span>
                    </div>
                    
                    <!-- Medication list preview -->
                    <div class="flex flex-wrap gap-2">
                      @for (med of prescription.medications.slice(0, 3); track med.id) {
                        <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary">
                          {{ med.medicationName }} — {{ med.dosage }}
                        </span>
                      }
                      @if (prescription.medications.length > 3) {
                        <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-muted text-muted-foreground">
                          +{{ prescription.medications.length - 3 }} more
                        </span>
                      }
                    </div>
                  </div>
                  
                  <!-- Actions -->
                  <div class="flex gap-2 ml-4">
                    <button z-button zType="outline" zSize="sm" (click)="viewPrescription(prescription)" title="View Details">
                      <z-icon zType="eye" class="w-4 h-4" />
                    </button>
                    <button z-button zType="outline" zSize="sm" (click)="openEditDialog(prescription)" title="Edit Prescription">
                      <z-icon zType="save" class="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="text-center p-12 border-2 border-dashed rounded-lg bg-muted/10">
          <z-icon zType="pill" class="w-12 h-12 mx-auto mb-4 text-muted-foreground/50" />
          <p class="text-lg font-medium text-muted-foreground">No prescriptions yet</p>
          <p class="text-sm text-muted-foreground mt-1">Create a prescription to get started</p>
        </div>
      }

      <!-- Create Prescription Dialog -->
      @if (showCreateDialog) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div class="bg-background rounded-lg shadow-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 relative">
            <button class="absolute top-4 right-4 text-muted-foreground hover:text-foreground" (click)="showCreateDialog = false">
              <z-icon zType="x" class="w-5 h-5" />
            </button>
            
            <h2 class="text-xl font-bold mb-4">{{ editingPrescriptionId ? 'Edit Prescription' : 'New Prescription' }}</h2>
            
            <form [formGroup]="prescriptionForm" (ngSubmit)="onSubmit()" class="space-y-4">
              
              <!-- Session Selection -->
              <div class="space-y-2">
                <label class="text-sm font-medium">Session</label>
                <select formControlName="sessionId" class="w-full p-2 border rounded-md bg-background">
                  <option [ngValue]="null" disabled>Select a session</option>
                  @for (session of sessions(); track session.id) {
                    <option [value]="session.id">
                      {{ session.sessionDate | date:'short' }}
                    </option>
                  }
                </select>
                @if (sessions().length === 0) {
                   <p class="text-xs text-destructive">No sessions found. Please create a consultation session first.</p>
                }
              </div>

              <!-- Medications List -->
              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <label class="text-sm font-medium">Medications</label>
                  <button type="button" z-button zType="outline" zSize="sm" (click)="addMedication()">
                    <z-icon zType="plus" class="mr-1" /> Add Med
                  </button>
                </div>

                <div formArrayName="medications" class="space-y-3 max-h-60 overflow-y-auto pr-2">
                  @for (med of medications.controls; track $index) {
                    <div [formGroupName]="$index" class="p-3 border rounded-md space-y-3 relative">
                      <button type="button" class="absolute top-2 right-2 text-muted-foreground hover:text-destructive" (click)="removeMedication($index)">
                        <z-icon zType="x" class="w-4 h-4" />
                      </button>
                      
                      <div class="grid grid-cols-2 gap-2">
                        <input type="text" formControlName="medicationName" placeholder="Medication Name" class="p-2 border rounded-md text-sm w-full" />
                        <input type="text" formControlName="dosage" placeholder="Dosage (e.g. 500mg)" class="p-2 border rounded-md text-sm w-full" />
                      </div>
                      <div class="grid grid-cols-2 gap-2">
                        <input type="text" formControlName="frequency" placeholder="Frequency (e.g. 2x/day)" class="p-2 border rounded-md text-sm w-full" />
                        <input type="text" formControlName="duration" placeholder="Duration (e.g. 7 days)" class="p-2 border rounded-md text-sm w-full" />
                      </div>
                      <textarea formControlName="instructions" placeholder="Special Instructions" rows="2" class="p-2 border rounded-md text-sm w-full"></textarea>
                    </div>
                  }
                </div>
              </div>

              <div class="flex justify-end gap-2 mt-6">
                <button type="button" z-button zType="ghost" (click)="showCreateDialog = false">Cancel</button>
                <button type="submit" z-button [disabled]="prescriptionForm.invalid || isSubmitting">
                  @if (isSubmitting) { <z-icon zType="loader-2" class="animate-spin mr-2" /> }
                  {{ editingPrescriptionId ? 'Update Prescription' : 'Create Prescription' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- View Detail Dialog -->
      @if (showViewDialog && selectedPrescription()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div class="bg-background rounded-lg shadow-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 relative">
             <button class="absolute top-4 right-4 text-muted-foreground hover:text-foreground" (click)="showViewDialog = false">
              <z-icon zType="x" class="w-5 h-5" />
            </button>
            
            <h2 class="text-2xl font-bold mb-6 flex items-center gap-2">
              <z-icon zType="pill" class="w-6 h-6 text-primary" />
              Prescription Details
            </h2>
            
             @let p = selectedPrescription()!;
             <div class="space-y-6">
               <!-- Meta Information -->
               <div class="grid grid-cols-1 gap-4">
                  <div class="bg-muted/30 p-4 rounded-lg">
                    <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
                      <z-icon zType="calendar" class="w-4 h-4" />
                      <span>Prescribed Date</span>
                    </div>
                    <p class="font-semibold text-lg">{{ p.createdAt | date:'medium' }}</p>
                  </div>
               </div>
               
               <!-- Medications Section -->
               <div>
                 <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
                   <z-icon zType="pill" class="w-5 h-5" />
                   Medications ({{ p.medications.length }})
                 </h3>
                 <div class="space-y-3">
                   @for (m of p.medications; track m.id) {
                     <div class="border border-primary/20 bg-primary/5 rounded-lg p-4 space-y-2">
                       <div class="flex items-start justify-between">
                         <div>
                           <h4 class="font-semibold text-lg">{{ m.medicationName }}</h4>
                           <p class="text-sm text-muted-foreground">{{ m.dosage }}</p>
                         </div>
                         <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-primary text-primary-foreground">
                           {{ m.frequency }}
                         </span>
                       </div>
                       
                       <div class="flex items-center gap-2 text-sm">
                         <z-icon zType="clock" class="w-4 h-4 text-muted-foreground" />
                         <span class="text-muted-foreground">Duration:</span>
                         <span class="font-medium">{{ m.duration }}</span>
                       </div>
                       
                       @if (m.instructions) {
                         <div class="bg-background rounded-md p-3 mt-2">
                           <div class="flex items-start gap-2">
                             <z-icon zType="info" class="w-4 h-4 text-primary mt-0.5" />
                             <div class="flex-1">
                               <p class="text-xs font-medium text-muted-foreground mb-1">Special Instructions</p>
                               <p class="text-sm">{{ m.instructions }}</p>
                             </div>
                           </div>
                         </div>
                       }
                     </div>
                   }
                 </div>
               </div>

               <div class="flex justify-end gap-2 pt-4 border-t">
                 <button z-button zType="outline" (click)="showViewDialog = false">Close</button>
                 <button z-button (click)="openEditDialog(p); showViewDialog = false">
                   <z-icon zType="save" class="mr-2 w-4 h-4" />
                   Edit Prescription
                 </button>
               </div>
             </div>
          </div>
        </div>
      }

    </div>
  `
})
export class PrescriptionManagementComponent implements OnInit {
  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;
  
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  sessions = signal<SessionResponseDTO[]>([]);
  
  showCreateDialog = false;
  showViewDialog = false;
  selectedPrescription = signal<PrescriptionResponseDTO | null>(null);
  
  prescriptionForm: FormGroup;
  isSubmitting = false;
  editingPrescriptionId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private prescriptionService: PrescriptionService,
    private medicalFolderService: MedicalFolderService,
    private sessionService: SessionService
  ) {
    this.prescriptionForm = this.fb.group({
      sessionId: [null, Validators.required],
      medications: this.fb.array([], Validators.required) // Need at least one?
    });
  }

  get medications() {
    return this.prescriptionForm.get('medications') as FormArray;
  }

  ngOnInit() {
    this.loadPrescriptions();
    this.loadSessions();
  }

  loadPrescriptions() {
    if (!this.patient?.id) return;
    // Use database ID instead of keycloakId
    const patientDbId = String(this.patient.id);
    this.prescriptionService.getPrescriptionsByPatient(patientDbId).subscribe(
      data => this.prescriptions.set(data)
    );
  }

  loadSessions() {
    // 1. Get Medical Folder for both this patient AND the logged-in doctor
    // 2. Get Sessions for that folder
    if (!this.patient?.id) {
      console.warn('[LoadSessions] No patient database ID');
      return;
    }
    
    if (!this.doctor) {
      console.warn('[LoadSessions] No doctor info available yet');
      return;
    }
    
    // Use database IDs for both patient and doctor (converted to string)
    const currentDoctorDbId = String(this.doctor.id);
    const patientDbId = String(this.patient.id);
    
    console.log('[LoadSessions] Current doctor DB ID:', currentDoctorDbId, 'Doctor:', this.doctor);
    console.log('[LoadSessions] Patient DB ID:', patientDbId, 'Patient:', this.patient);
    
    this.medicalFolderService.getMedicalFoldersByPatient(patientDbId).subscribe({
      next: folders => {
        console.log('[LoadSessions] All medical folders for patient:', folders);
        
        // Filter to only the folder where the doctor matches (using database IDs)
        const matchingFolder = folders.find(f => {
          console.log('[LoadSessions] Comparing folder.idDoctor:', f.idDoctor, 'with current doctor DB ID:', currentDoctorDbId);
          return f.idDoctor === currentDoctorDbId;
        });
        
        if (matchingFolder) {
          console.log('[LoadSessions] Found matching folder:', matchingFolder);
          this.sessionService.getSessionsByMedicalFolder(matchingFolder.id).subscribe({
            next: sessions => {
              console.log('[LoadSessions] Sessions loaded:', sessions);
              this.sessions.set(sessions);
            },
            error: err => {
              console.error('[LoadSessions] Error loading sessions:', err);
              this.sessions.set([]);
            }
          });
        } else {
          console.warn('[LoadSessions] No medical folder found for patient DB ID:', patientDbId, 'and doctor DB ID:', currentDoctorDbId);
          console.warn('[LoadSessions] Available folders:', folders.map(f => ({ id: f.id, idDoctor: f.idDoctor, idPatient: f.idPatient })));
          this.sessions.set([]);
        }
      },
      error: err => {
        console.error('[LoadSessions] Error loading medical folders:', err);
        this.sessions.set([]);
      }
    });
  }

  getSessionNote(sessionId: number): string {
    const s = this.sessions().find(x => x.id === sessionId);
    return s ? `\${new Date(s.sessionDate).toLocaleDateString()} - \${s.notes || 'No notes'}` : `Session #\${sessionId}`;
  }

  openCreateDialog() {
    this.editingPrescriptionId = null;
    this.prescriptionForm.reset();
    this.medications.clear();
    this.addMedication();
    this.showCreateDialog = true;
  }

  openEditDialog(p: PrescriptionResponseDTO) {
    this.editingPrescriptionId = p.id;
    this.prescriptionForm.reset();
    this.medications.clear();

    this.prescriptionForm.patchValue({
      sessionId: p.sessionId
    });

    if (p.medications && p.medications.length > 0) {
      p.medications.forEach(m => {
        const group = this.fb.group({
          medicationName: [m.medicationName, Validators.required],
          dosage: [m.dosage, Validators.required],
          frequency: [m.frequency, Validators.required],
          duration: [m.duration, Validators.required],
          instructions: [m.instructions || '']
        });
        (this.prescriptionForm.get('medications') as FormArray).push(group);
      });
    } else {
      this.addMedication();
    }
    
    this.showCreateDialog = true;
  }

  addMedication() {
    const group = this.fb.group({
      medicationName: ['', Validators.required],
      dosage: ['', Validators.required],
      frequency: ['', Validators.required],
      duration: ['', Validators.required],
      instructions: ['']
    });
    this.medications.push(group);
  }

  removeMedication(index: number) {
    this.medications.removeAt(index);
  }

  viewPrescription(p: PrescriptionResponseDTO) {
    this.selectedPrescription.set(p);
    this.showViewDialog = true;
  }

  onSubmit() {
    if (this.prescriptionForm.invalid) {
      console.error('[Submit] Form is invalid:', this.prescriptionForm.errors);
      console.error('[Submit] Form value:', this.prescriptionForm.value);
      return;
    }

    this.isSubmitting = true;
    const formVal = this.prescriptionForm.value;
    
    const request: PrescriptionRequestDTO = {
      sessionId: +formVal.sessionId,
      medications: formVal.medications as MedicationRequestDTO[]
    };

    console.log('[Submit] Sending request:', JSON.stringify(request, null, 2));

    const obs = this.editingPrescriptionId
      ? this.prescriptionService.updatePrescription(this.editingPrescriptionId, request)
      : this.prescriptionService.createPrescription(request);

    obs.subscribe({
      next: (res) => {
        console.log('[Submit] Success:', res);
        this.isSubmitting = false;
        this.showCreateDialog = false;
        this.loadPrescriptions();
      },
      error: (err) => {
        console.error('[Submit] Error:', err);
        console.error('[Submit] Error response:', err.error);
        this.isSubmitting = false;
      }
    });
  }
}
