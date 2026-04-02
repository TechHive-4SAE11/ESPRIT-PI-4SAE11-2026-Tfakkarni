import { Component, OnInit, Input, inject, signal, DestroyRef, effect, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of, finalize } from 'rxjs';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardPaginationComponent } from '@/shared/components/pagination';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { MedicationResponseDTO, MedicationStatus } from '@/core/models/prescription.model';
import { PagedResponse } from '@/core/models/paged-response.model';
import { MedicationService, UpdateMedicationRequest } from '@/core/services/medication.service';
import { UserApiService, UserInfo } from '@/core/services/user-api.service';
import { ZardDialogService } from '@/shared/components/dialog/dialog.service';

@Component({
  selector: 'app-medication-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardPaginationComponent,
    ZardInputDirective
  ],
  template: `
    <div>
      <div class="mb-6">
        <!-- <h2 class="text-2xl font-bold mb-2">Medications</h2> -->
        <p class="text-muted-foreground">View and manage {{ viewMode === 'patient' ? 'patient' : 'prescribed' }} patient medications</p>
      </div>

      <!-- Filters -->
      <div class="mb-6 flex flex-wrap gap-3">
        <button
          z-button
          [zType]="selectedStatus() === null ? 'default' : 'outline'"
          zSize="sm"
          (click)="filterByStatus(null)"
        >
          All Statuses
        </button>
        @for (status of statuses; track status.value) {
          <button
            z-button
            [zType]="selectedStatus() === status.value ? 'default' : 'outline'"
            zSize="sm"
            (click)="filterByStatus(status.value)"
            class="flex items-center gap-2"
          >
            <z-icon [zType]="status.icon" class="h-4 w-4" />
            {{ status.label }}
          </button>
        }
      </div>

      <!-- Medications List -->
      @if (isLoading()) {
        <div class="space-y-4">
          @for (i of [1,2,3]; track i) {
            <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 animate-pulse">
              <div class="h-6 bg-slate-200 dark:bg-slate-700 rounded w-3/4 mb-3"></div>
              <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-1/2 mb-2"></div>
              <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-2/3"></div>
            </div>
          }
        </div>
      } @else if (medications().length > 0) {
        <div class="space-y-4">
          @for (med of medications(); track med.id) {
            <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 sm:p-6 shadow-sm hover:shadow-md transition-shadow">
              <div class="flex items-start justify-between mb-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2 flex-wrap">
                    <h3 class="text-xl font-bold text-slate-800 dark:text-white">{{ med.medicationName }}</h3>
                    <z-badge [zType]="getStatusBadgeType(med.status)" class="flex items-center gap-1">
                      <z-icon [zType]="getStatusIcon(med.status)" class="h-3 w-3" />
                      <span>{{ getStatusLabel(med.status) }}</span>
                    </z-badge>
                  </div>
                  
                  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm">
                    <div class="flex items-center gap-2">
                      <z-icon zType="pill" class="h-4 w-4 text-slate-400" />
                      <span class="text-slate-500 dark:text-slate-400">Dosage:</span>
                      <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.dosage }}</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <z-icon zType="clock" class="h-4 w-4 text-slate-400" />
                      <span class="text-slate-500 dark:text-slate-400">Frequency:</span>
                      <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.frequency }}</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <z-icon zType="calendar" class="h-4 w-4 text-slate-400" />
                      <span class="text-slate-500 dark:text-slate-400">Duration:</span>
                      <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.duration }}</span>
                    </div>
                    @if (med.startDate) {
                      <div class="flex items-center gap-2">
                        <z-icon zType="play" class="h-4 w-4 text-green-500" />
                        <span class="text-slate-500 dark:text-slate-400">Start:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.startDate | date:'mediumDate' }}</span>
                      </div>
                    }
                    @if (med.endDate) {
                      <div class="flex items-center gap-2">
                        <z-icon zType="calendar" class="h-4 w-4 text-red-500" />
                        <span class="text-slate-500 dark:text-slate-400">End:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.endDate | date:'mediumDate' }}</span>
                      </div>
                    }
                    <div class="flex items-center gap-2">
                      <z-icon zType="calendar" class="h-4 w-4 text-slate-400" />
                      <span class="text-slate-500 dark:text-slate-400">Created:</span>
                      <span class="font-medium text-slate-700 dark:text-slate-200">{{ med.createdAt | date:'short' }}</span>
                    </div>
                    @if (med.sessionDate) {
                      <div class="flex items-center gap-2">
                        <z-icon zType="file" class="h-4 w-4 text-purple-500" />
                        <span class="text-slate-500 dark:text-slate-400">Session:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">
                          {{ med.sessionDate | date:'mediumDate' }}
                          @if (viewMode !== 'doctor' && med.doctorId && doctorNames().has(med.doctorId!)) {
                            <span class="text-slate-400 text-xs ml-1">
                              (Associated to {{ doctorNames().get(med.doctorId!) }})
                            </span>
                          }
                        </span>
                      </div>
                    }
                  </div>
                  
                  @if (med.instructions) {
                    <div class="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800/50">
                      <div class="flex items-start gap-2">
                        <z-icon zType="info" class="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
                        <div>
                          <p class="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">Instructions</p>
                          <p class="text-sm text-blue-800 dark:text-blue-200 whitespace-pre-line">{{ med.instructions }}</p>
                        </div>
                      </div>
                    </div>
                  }
                </div>
                
                @if (viewMode === 'doctor' || doctor) {
                  <div class="ml-4 flex gap-2">
                    @if (med.status === 'ACTIVE') {
                      <button
                        z-button
                        zType="outline"
                        zSize="sm"
                        class="text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700 dark:border-red-900/50 dark:text-red-400 dark:hover:bg-red-900/20"
                        (click)="openDiscontinueDialog(med)"
                      >
                        Discontinue
                      </button>
                    }
                  </div>
                }
              </div>
            </div>
          }
        </div>
        
        @if (totalPages() > 1) {
          <div class="mt-6">
            <z-pagination
              [currentPage]="currentPage()"
              [totalPages]="totalPages()"
              [totalItems]="totalItems()"
              [pageSize]="pageSize()"
              (pageChange)="onPageChange($event)"
            />
          </div>
        }
      } @else {
        <div class="text-center py-16 px-4">
          <z-icon zType="alert-triangle" class="h-12 w-12 mx-auto text-slate-300 dark:text-slate-600 mb-4" />
          <p class="text-slate-600 dark:text-slate-400 text-lg font-medium">No medications found</p>
          <p class="text-slate-400 dark:text-slate-500 text-sm mt-1">
            @if (selectedStatus()) {
              Try selecting a different status filter
            } @else {
              No medications have been prescribed yet
            }
          </p>
        </div>
      }
    </div>
    <!-- Edit Dialog Template -->
    <ng-template #editDialog let-data="data" let-dialogRef="dialogRef">
      <form (ngSubmit)="submitEdit(dialogRef)" class="flex flex-col h-full max-h-[80vh]">
        <div class="space-y-4 mt-2 overflow-y-auto flex-1 px-1">
          <!-- Medication Name -->
          <div>
            <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Medication Name</label>
            <input 
              z-input 
              [(ngModel)]="currentEdit.medicationName" 
              name="medicationName" 
              placeholder="e.g. Donepezil" 
              class="w-full" 
              required
            />
          </div>
          
          <!-- Dosage & Frequency Group -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Dosage</label>
              <input 
                z-input 
                [(ngModel)]="currentEdit.dosage" 
                name="dosage" 
                placeholder="e.g. 10mg" 
                class="w-full" 
                required
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Frequency</label>
              <input 
                z-input 
                [(ngModel)]="currentEdit.frequency" 
                name="frequency" 
                placeholder="e.g. Once daily" 
                class="w-full" 
                required
              />
            </div>
          </div>
          
          <!-- Duration -->
          <div>
            <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Duration</label>
            <input 
              z-input 
              [(ngModel)]="currentEdit.duration" 
              name="duration" 
              placeholder="e.g. 3 months" 
              class="w-full" 
              required
            />
          </div>
          
          <!-- Instructions -->
          <div>
            <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Special Instructions <span class="text-muted-foreground font-normal">(Optional)</span></label>
            <textarea 
              z-input 
              [(ngModel)]="currentEdit.instructions" 
              name="instructions" 
              rows="3" 
              placeholder="Enter any additional instructions..." 
              class="w-full resize-none"
            ></textarea>
          </div>
        </div>

        <!-- Footer Actions -->
        <div class="flex justify-end gap-3 mt-4 pt-4 border-t border-slate-100 dark:border-slate-800 flex-shrink-0">
          <button 
            type="button" 
            z-button 
            zType="ghost" 
            (click)="dialogRef.close()"
            [disabled]="isLoading()"
          >
            Cancel
          </button>
          <button 
            type="submit" 
            z-button 
            [disabled]="!currentEdit.medicationName || !currentEdit.dosage || !currentEdit.frequency || !currentEdit.duration || isLoading()"
          >
            @if (isLoading()) {
              <z-icon zType="loader-2" class="animate-spin mr-2 h-4 w-4" />
              Saving...
            } @else {
              Save Changes
            }
          </button>
        </div>
      </form>
    </ng-template>

    <!-- Discontinue Dialog Template -->
    <ng-template #discontinueDialog let-data="data" let-dialogRef="dialogRef">
      <div class="space-y-4">
        <p class="text-sm text-muted-foreground">
          Are you sure you want to discontinue <strong>{{ data.med.medicationName }}</strong>? This action cannot be undone.
        </p>
        <div>
          <label class="block text-sm font-medium mb-1 text-slate-700 dark:text-slate-300">Reason for discontinuation</label>
          <textarea
            z-input
            [(ngModel)]="discontinueReason"
            name="discontinueReason"
            rows="3"
            placeholder="e.g. Allergic reaction, Treatment completed..."
            class="w-full resize-none"
          ></textarea>
        </div>
        <div class="flex justify-end gap-3 mt-6 pt-4 border-t border-slate-100 dark:border-slate-800">
          <button 
            z-button 
            zType="ghost" 
            (click)="dialogRef.close()"
            [disabled]="isLoading()"
          >
            Cancel
          </button>
          <button 
            z-button 
            zType="destructive" 
            (click)="submitDiscontinue(dialogRef)"
            [disabled]="isLoading()"
          >
            @if (isLoading()) {
              <z-icon zType="loader-2" class="animate-spin mr-2 h-4 w-4" />
              Discontinuing...
            } @else {
              Discontinue Medication
            }
          </button>
        </div>
      </div>
    </ng-template>
  `,
  styles: ``
})
export class MedicationManagementComponent implements OnInit {
  private medicationService = inject(MedicationService);
  private userApiService = inject(UserApiService);
  private destroyRef = inject(DestroyRef);
  private dialogService = inject(ZardDialogService);

  doctorNames = signal<Map<string, string>>(new Map());

  @ViewChild('editDialog') editDialogTemplate!: TemplateRef<any>;
  @ViewChild('discontinueDialog') discontinueDialogTemplate!: TemplateRef<any>;

  // Edit State
  currentEdit: UpdateMedicationRequest = {
    medicationName: '',
    dosage: '',
    frequency: '',
    duration: '',
    instructions: ''
  };
  currentEditId: number | null = null;

  // Discontinue State
  discontinueReason = '';
  currentDiscontinueId: number | null = null;

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;
  @Input() viewMode: 'patient' | 'doctor' = 'patient';

  medications = signal<MedicationResponseDTO[]>([]);
  isLoading = signal<boolean>(false);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalItems = signal<number>(0);
  pageSize = signal<number>(10);
  selectedStatus = signal<MedicationStatus | null>(null);
  private hasLoadedOnce = false;

  statuses = [
    { value: MedicationStatus.ACTIVE, label: 'Active', icon: 'check' as const },
    { value: MedicationStatus.ONGOING, label: 'Ongoing', icon: 'activity' as const },
    { value: MedicationStatus.EXPIRED, label: 'Expired', icon: 'x' as const },
    { value: MedicationStatus.DISCONTINUED, label: 'Discontinued', icon: 'alert-triangle' as const }
  ];

  ngOnInit() {
    console.log('[MedicationManagement] Initialized with patient:', this.patient?.id, 'viewMode:', this.viewMode);
    // Only load if patient is already available at init time
    if (this.patient && !this.hasLoadedOnce) {
      this.hasLoadedOnce = true;
      this.loadMedications();
    }
  }

  loadMedications(): void {
    if (!this.patient?.id) {
      console.warn('[MedicationManagement] No patient ID available');
      return;
    }

    this.isLoading.set(true);
    const userId = this.patient.keycloakId;
    const statusFilter = this.selectedStatus() || undefined;

    console.log('[MedicationManagement] Loading medications - Patient ID:', userId, 'ViewMode:', this.viewMode, 'Page:', this.currentPage(), 'Status Filter:', statusFilter);

    const serviceCall = this.viewMode === 'doctor' && this.doctor
      ? this.medicationService.getMedicationsByDoctorPaginated(
        this.doctor.keycloakId,
        this.currentPage(),
        this.pageSize(),
        'createdAt',
        'DESC',
        statusFilter
      )
      : this.medicationService.getMedicationsByPatientPaginated(
        userId,
        this.currentPage(),
        this.pageSize(),
        'createdAt',
        'DESC',
        statusFilter
      );

    serviceCall.pipe(
      tap((response: PagedResponse<MedicationResponseDTO>) => {
        console.log('[MedicationManagement] Response received:', response);
        console.log('[MedicationManagement] Medications count:', response.content.length);
        this.medications.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalItems.set(response.totalElements);
        const medList = response.content;
        const uniqueDoctorIds = new Set(medList.map(m => m.doctorId).filter(Boolean));
        uniqueDoctorIds.forEach(id => {
          if (id && !this.doctorNames().has(id)) {
            this.userApiService.getUserById(id).subscribe({
              next: (user: UserInfo) => {
                this.doctorNames.update(map => {
                  const newMap = new Map(map);
                  newMap.set(id, `Dr. ${user.firstName} ${user.lastName}`);
                  return newMap;
                });
              },
              error: () => console.warn(`Failed to load doctor name for ID: ${id}`)
            });
          }
        });
      }),
      catchError(err => {
        console.error('[MedicationManagement] Failed to load medications', err);
        this.medications.set([]);
        return of(null);
      }),
      tap(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadMedications();
  }

  filterByStatus(status: MedicationStatus | null): void {
    console.log('[MedicationManagement] Filter changed to:', status);
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadMedications();
  }

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
        return 'secondary';
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

  // --- Edit Functionality ---
  openEditDialog(med: MedicationResponseDTO) {
    this.currentEdit = {
      medicationName: med.medicationName,
      dosage: med.dosage,
      frequency: med.frequency,
      duration: med.duration,
      instructions: med.instructions,
      startDate: med.startDate ? med.startDate.toString() : undefined,
      endDate: med.endDate ? med.endDate.toString() : undefined
    };
    this.currentEditId = med.id;

    this.dialogService.create({
      zTitle: 'Update Medication',
      zDescription: 'Modify details for this medication',
      zContent: this.editDialogTemplate,
      zData: { med },
      zWidth: '100%',
      zCustomClasses: 'sm:max-w-xl',
      zHideFooter: true
    });
  }

  submitEdit(dialogRef: any) {
    if (!this.currentEditId) return;

    this.isLoading.set(true);
    this.medicationService.updateMedication(this.currentEditId, this.currentEdit)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (updatedMed) => {
          this.medications.update(meds =>
            meds.map(m => m.id === updatedMed.id ? updatedMed : m)
          );
          dialogRef.close();
        },
        error: (err) => console.error('Error updating medication', err)
      });
  }

  // --- Discontinue Functionality ---
  openDiscontinueDialog(med: MedicationResponseDTO) {
    this.discontinueReason = '';
    this.currentDiscontinueId = med.id;

    this.dialogService.create({
      zTitle: 'Discontinue Medication',
      zContent: this.discontinueDialogTemplate,
      zData: { med },
      zHideFooter: true
    });
  }

  submitDiscontinue(dialogRef: any) {
    if (!this.currentDiscontinueId) return;

    this.isLoading.set(true);
    this.medicationService.updateMedicationStatus(this.currentDiscontinueId, {
      status: MedicationStatus.DISCONTINUED,
      reason: this.discontinueReason
    }).pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          // Manually construct updated med object or reload list
          this.loadMedications();
          dialogRef.close();
        },
        error: (err) => console.error('Error discontinuing medication', err)
      });
  }
}
