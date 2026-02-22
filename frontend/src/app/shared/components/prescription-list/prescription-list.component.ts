import { Component, Input, computed, OnInit, OnChanges, SimpleChanges, inject, DestroyRef, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of } from 'rxjs';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { PrescriptionResponseDTO, MedicationStatus } from '@/core/models/prescription.model';
import { UserApiService } from '@/core/services/user-api.service';
import { PrescriptionService } from '@/core/services/prescription.service';

@Component({
  selector: 'app-prescription-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardButtonComponent
  ],
  template: `
    @if (isLoading) {
      <div class="space-y-4">
        @for (i of [1,2,3]; track i) {
          <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 animate-pulse">
            <div class="h-6 bg-slate-200 dark:bg-slate-700 rounded w-3/4 mb-3"></div>
            <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-1/2 mb-2"></div>
            <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-2/3"></div>
          </div>
        }
      </div>
    } @else if (prescriptions.length > 0) {
      <div class="space-y-4">
        @for (prescription of prescriptions; track prescription.id) {
          <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 sm:p-6 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-start justify-between mb-4">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <z-icon zType="file-text" class="text-primary h-5 w-5" />
                  <p class="font-semibold text-lg text-slate-800 dark:text-white">
                    Prescription #{{ prescription.id }}
                  </p>
                </div>
                <div class="flex flex-col gap-1 text-sm text-slate-500 dark:text-slate-400">
                  <div class="flex items-center">
                    <z-icon zType="calendar" class="w-4 h-4 mr-1" />
                    {{ prescription.createdAt | date:'medium' }}
                  </div>
                  @if (prescription.doctorId && doctorNames.get(prescription.doctorId)) {
                    <div class="flex items-center text-primary font-medium">
                        <z-icon zType="user" class="w-4 h-4 mr-1" />
                        {{ doctorNames.get(prescription.doctorId) }}
                    </div>
                  }
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button z-button zType="outline" zSize="sm" (click)="downloadPdf(prescription.id!)" title="Download PDF">
                    <z-icon zType="download" class="w-4 h-4" />
                </button>
                <z-badge zType="secondary">
                  {{ prescription.medications.length || 0 }} medication(s)
                </z-badge>
              </div>
            </div>
            
            @if (prescription.medications && prescription.medications.length > 0) {
              <div class="space-y-3 mt-4">
                @for (med of prescription.medications; track med.id) {
                  <div class="border-l-4 border-purple-500 pl-4 py-3 bg-slate-50 dark:bg-slate-700/30 rounded-r-lg">
                    <div class="flex items-start justify-between mb-2">
                      <div class="flex items-center gap-2 flex-wrap">
                        <h4 class="font-bold text-base text-slate-800 dark:text-white">{{ med.medicationName }}</h4>
                        <z-badge [zType]="getStatusBadgeType(med.status)" class="flex items-center gap-1">
                          <z-icon [zType]="getStatusIcon(med.status)" class="h-3 w-3" />
                          <span>{{ getStatusLabel(med.status) }}</span>
                        </z-badge>
                      </div>
                      <z-icon zType="pill" class="text-purple-500 h-5 w-5" />
                    </div>
                    
                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm">
                      <div class="flex items-center gap-2">
                        <z-icon zType="circle" class="h-4 w-4 text-slate-400" />
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
                    </div>
                    
                    @if (med.instructions) {
                      <div class="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800/50">
                        <div class="flex items-start gap-2">
                          <z-icon zType="info" class="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
                          <div>
                            <p class="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">Special Instructions</p>
                            <p class="text-sm text-blue-800 dark:text-blue-200">{{ med.instructions }}</p>
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            } @else {
              <div class="text-center py-4 text-slate-500 dark:text-slate-400 italic flex items-center justify-center gap-2">
                <z-icon zType="circle-alert" class="h-5 w-5" />
                No medications listed
              </div>
            }
          </div>
        }
      </div>
    } @else {
      <div class="text-center py-16 px-4">
        <div class="bg-slate-100 dark:bg-slate-800 rounded-full p-6 w-24 h-24 mx-auto mb-6 flex items-center justify-center">
          <z-icon zType="pill" class="h-12 w-12 text-slate-400" />
        </div>
        <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No prescriptions yet</h2>
        <p class="text-slate-500 dark:text-slate-400 text-lg max-w-md mx-auto">
          Your doctor will add prescriptions here when needed.
        </p>
      </div>
    }
  `
})
export class PrescriptionListComponent implements OnInit, OnChanges {
  @Input({ required: true }) prescriptions: PrescriptionResponseDTO[] = [];
  @Input() isLoading = false;
  
  private userApiService = inject(UserApiService);
  private prescriptionService = inject(PrescriptionService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  
  doctorNames = new Map<string, string>(); // doctorDbId -> Full Name

  // Helper methods for medication status display
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

  ngOnInit() {
      this.fetchDoctorNames();
  }

  ngOnChanges(changes: SimpleChanges) {
      if (changes['prescriptions']) {
          this.fetchDoctorNames();
      }
  }

  fetchDoctorNames() {
      if (!this.prescriptions || this.prescriptions.length === 0) return;

      const uniqueIds = new Set(this.prescriptions.map(p => p.doctorId).filter(id => id && !this.doctorNames.has(id)));
      
      uniqueIds.forEach(id => {
          this.userApiService.getUserById(id).subscribe({
              next: (user) => {
                  if (user) {
                    this.doctorNames.set(id, `Dr. ${user.firstName} ${user.lastName}`);
                  }
              },
              error: (err) => console.error(`Failed to load doctor info for ${id}`, err)
          });
      });
  }

  downloadPdf(id: number): void {
      if (!isPlatformBrowser(this.platformId)) return;

      this.prescriptionService.downloadPrescriptionPdf(id)
        .pipe(
          tap((blob: Blob) => {
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `prescription_${id}.pdf`;
            link.click();
            window.URL.revokeObjectURL(url);
          }),
          catchError(error => {
            console.error('Error downloading PDF', error);
            return of(null);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe();
    }
}
