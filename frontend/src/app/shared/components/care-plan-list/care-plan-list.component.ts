import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { CarePlanResponseDTO } from '@/core/models/care-plan.model';

@Component({
  selector: 'app-care-plan-list',
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
    } @else if (carePlans.length > 0) {
      <div class="space-y-4">
        @for (plan of carePlans; track plan.id) {
          <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 sm:p-6 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-start justify-between mb-4">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <z-icon zType="file-text" class="text-primary h-5 w-5" />
                  <p class="font-semibold text-lg text-slate-800 dark:text-white">
                    Care Plan #{{ plan.id }}
                  </p>
                </div>
                <div class="flex items-center text-sm text-slate-500 dark:text-slate-400">
                  <z-icon zType="calendar" class="w-4 h-4 mr-1" />
                  {{ plan.createdAt | date:'medium' }}
                </div>
              </div>
              <z-badge zType="secondary">
                {{ plan.activities.length || 0 }} activity(s)
              </z-badge>
            </div>
            
            @if (plan.activities && plan.activities.length > 0) {
              <div class="space-y-3 mt-4">
                @for (activity of plan.activities; track activity.id) {
                  <div class="border-l-4 border-emerald-500 pl-4 py-3 bg-slate-50 dark:bg-slate-700/30 rounded-r-lg">
                    <div class="flex items-start justify-between mb-2">
                      <h4 class="font-bold text-base text-slate-800 dark:text-white">{{ activity.activityName }}</h4>
                      <z-icon zType="activity" class="text-emerald-500 h-5 w-5" />
                    </div>
                    
                    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm">
                      <div class="flex items-center gap-2">
                        <z-icon zType="clock" class="h-4 w-4 text-slate-400" />
                        <span class="text-slate-500 dark:text-slate-400">Frequency:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">{{ activity.frequency }}</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <z-icon zType="calendar" class="h-4 w-4 text-slate-400" />
                        <span class="text-slate-500 dark:text-slate-400">Duration:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">{{ activity.duration }}</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <z-icon zType="check" class="h-4 w-4 text-slate-400" />
                        <span class="text-slate-500 dark:text-slate-400">Status:</span>
                        <span class="font-medium text-slate-700 dark:text-slate-200">{{ activity.completionStatus || 'Pending' }}</span>
                      </div>
                    </div>
                    
                    @if (activity.description) {
                      <div class="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800/50">
                        <div class="flex items-start gap-2">
                          <z-icon zType="info" class="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
                          <div>
                            <p class="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">Description</p>
                            <p class="text-sm text-blue-800 dark:text-blue-200">{{ activity.description }}</p>
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            } @else {
              <div class="text-center py-4 text-slate-500 dark:text-slate-400 italic flex items-center justify-center gap-2">
                <z-icon zType="triangle-alert" class="h-5 w-5" />
                No activities listed
              </div>
            }
          </div>
        }
      </div>
    } @else {
      <div class="text-center py-10 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-dashed border-slate-300 dark:border-slate-700">
        <z-icon zType="file-text" class="h-12 w-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
        <h3 class="text-lg font-medium text-slate-900 dark:text-white">No Care Plans</h3>
        <p class="text-slate-500 dark:text-slate-400">There are no care plans assigned yet.</p>
      </div>
    }
  `
})
export class CarePlanListComponent {
  @Input() carePlans: CarePlanResponseDTO[] = [];
  @Input() isLoading = false;
}
