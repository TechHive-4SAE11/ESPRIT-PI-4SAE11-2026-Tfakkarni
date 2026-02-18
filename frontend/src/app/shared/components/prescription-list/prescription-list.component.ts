import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { PrescriptionResponseDTO } from '@/core/models/prescription.model';

@Component({
  selector: 'app-prescription-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent
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
                <div class="flex items-center text-sm text-slate-500 dark:text-slate-400">
                  <z-icon zType="calendar" class="w-4 h-4 mr-1" />
                  {{ prescription.createdAt | date:'medium' }}
                </div>
              </div>
              <z-badge zType="secondary">
                {{ prescription.medications.length || 0 }} medication(s)
              </z-badge>
            </div>
            
            @if (prescription.medications && prescription.medications.length > 0) {
              <div class="space-y-3 mt-4">
                @for (med of prescription.medications; track med.id) {
                  <div class="border-l-4 border-purple-500 pl-4 py-3 bg-slate-50 dark:bg-slate-700/30 rounded-r-lg">
                    <div class="flex items-start justify-between mb-2">
                      <h4 class="font-bold text-base text-slate-800 dark:text-white">{{ med.medicationName }}</h4>
                      <z-icon zType="pill" class="text-purple-500 h-5 w-5" />
                    </div>
                    
                    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm">
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
export class PrescriptionListComponent {
  @Input({ required: true }) prescriptions: PrescriptionResponseDTO[] = [];
  @Input() isLoading = false;
}
