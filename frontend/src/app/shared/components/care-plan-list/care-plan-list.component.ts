import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, tap, of } from 'rxjs';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardPaginationComponent } from '@/shared/components/pagination';
import { CarePlanResponseDTO, CareActivityType, CareActivityResponseDTO } from '@/core/models/care-plan.model';
import { PagedResponse } from '@/core/models/paged-response.model';
import { UserApiService } from '@/core/services/user-api.service';
import { CarePlanService } from '@/core/services/care-plan.service';

@Component({
  selector: 'app-care-plan-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardPaginationComponent
  ],
  template: `
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
    } @else if (carePlans().length > 0) {
      <div class="space-y-4">
        @for (plan of carePlans(); track plan.id) {
          <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 sm:p-6 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-start justify-between mb-4">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <z-icon zType="file-text" class="text-primary h-5 w-5" />
                  <p class="font-semibold text-lg text-slate-800 dark:text-white">
                    Care Plan #{{ plan.id }}
                  </p>
                </div>
                
                <div class="flex flex-col gap-1 text-sm text-slate-500 dark:text-slate-400">
                    <div class="flex items-center">
                        <z-icon zType="calendar" class="w-4 h-4 mr-1" />
                        {{ plan.createdAt | date:'medium' }}
                    </div>
                    @if (plan.doctorId && doctorNames.get(plan.doctorId)) {
                        <div class="flex items-center text-primary font-medium">
                            <z-icon zType="user" class="w-4 h-4 mr-1" />
                            {{ doctorNames.get(plan.doctorId) }}
                        </div>
                    }
                </div>
              </div>
              <z-badge zType="secondary">
                {{ plan.activities.length || 0 }} activity(s)
              </z-badge>
            </div>
            
            @if (plan.activities && plan.activities.length > 0) {
              <div class="space-y-6 mt-4">
                
                <!-- Physical Activities Section -->
                @if (getPhysicalActivities(plan).length > 0) {
                  <div>
                    <h4 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3 flex items-center gap-2">
                       <z-icon zType="activity" class="h-4 w-4" /> Physical Activities
                    </h4>
                    <div class="space-y-3">
                      @for (activity of getPhysicalActivities(plan); track activity.id) {
                        <div class="border-l-4 border-emerald-500 pl-4 py-3 bg-slate-50 dark:bg-slate-700/30 rounded-r-lg">
                          <div class="flex items-start justify-between mb-2">
                              <h4 class="font-bold text-base text-slate-800 dark:text-white">{{ activity.activityName }}</h4>
                              <span class="text-xs px-2 py-0.5 rounded-full font-medium bg-emerald-100 text-emerald-800">
                                  Physical
                              </span>
                          </div>
                          
                          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                            <div class="flex items-center gap-2">
                              <span class="text-slate-500 dark:text-slate-400">Freq:</span>
                              <span class="font-medium text-slate-700 dark:text-slate-200">{{ activity.frequency }}</span>
                            </div>
                            <div class="flex items-center gap-2">
                              <span class="text-slate-500 dark:text-slate-400">Duration:</span>
                              <span class="font-medium text-slate-700 dark:text-slate-200">{{ activity.duration }}</span>
                            </div>
                          </div>
                          
                          @if (activity.description) {
                            <div class="mt-2 text-sm text-slate-600 dark:text-slate-300">
                              {{ activity.description }}
                            </div>
                          }
                        </div>
                      }
                    </div>
                  </div>
                }

                <!-- Nutrition Plans Section -->
                @if (getNutritionActivities(plan).length > 0) {
                  <div>
                    <h4 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3 flex items-center gap-2">
                       <z-icon zType="heart" class="h-4 w-4" /> Nutrition Plans
                    </h4>
                    <div class="space-y-3">
                      @for (activity of getNutritionActivities(plan); track activity.id) {
                        <div class="border-l-4 border-orange-500 pl-4 py-3 bg-slate-50 dark:bg-slate-700/30 rounded-r-lg">
                          <div class="flex items-start justify-between mb-2">
                              <h4 class="font-bold text-base text-slate-800 dark:text-white">{{ activity.activityName }}</h4>
                              <span class="text-xs px-2 py-0.5 rounded-full font-medium bg-orange-100 text-orange-800">
                                  Nutrition
                              </span>
                          </div>
                          
                          @if (activity.description) {
                            <div class="mt-2 p-3 bg-white dark:bg-slate-800 rounded border border-slate-100 dark:border-slate-600/50">
                              <p class="text-xs font-semibold text-slate-500 uppercase mb-1">Description</p>
                              <p class="text-sm text-slate-700 dark:text-slate-300">{{ activity.description }}</p>
                            </div>
                          }
                        </div>
                      }
                    </div>
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
        <p class="text-slate-600 dark:text-slate-400 text-lg font-medium">No care plans found</p>
        <p class="text-slate-400 dark:text-slate-500 text-sm mt-1">This patient has no care plans yet.</p>
      </div>
    }
  `
})
export class CarePlanListComponent implements OnInit, OnChanges {
  @Input() patientId: string | null = null;
  @Input() pageSize = signal<number>(5);
  
  private userApiService = inject(UserApiService);
  private carePlanService = inject(CarePlanService);
  private destroyRef = inject(DestroyRef);
  
  carePlans = signal<CarePlanResponseDTO[]>([]);
  isLoading = signal<boolean>(false);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalItems = signal<number>(0);
  
  doctorNames = new Map<string, string>(); // doctorDbId -> Full Name

  ngOnInit() {
    if (this.patientId) {
      this.loadCarePlans();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['patientId'] && !changes['patientId'].firstChange) {
      this.currentPage.set(0);
      this.loadCarePlans();
    }
  }
  
  loadCarePlans(): void {
    if (!this.patientId) return;
    
    this.isLoading.set(true);
    
    this.carePlanService.getCarePlansByPatientPaginated(
      this.patientId,
      this.currentPage(),
      this.pageSize(),
      'createdAt',
      'DESC'
    ).pipe(
      tap((response: PagedResponse<CarePlanResponseDTO>) => {
        this.carePlans.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalItems.set(response.totalElements);
        this.fetchDoctorNames(response.content);
      }),
      catchError(err => {
        console.error('Failed to load care plans', err);
        this.carePlans.set([]);
        return of(null);
      }),
      tap(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
  
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadCarePlans();
  }

  fetchDoctorNames(carePlans: CarePlanResponseDTO[]) {
    if (!carePlans || carePlans.length === 0) return;

    const uniqueIds = new Set(carePlans.map(p => p.doctorId).filter(id => id && !this.doctorNames.has(id)));
    
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

  getPhysicalActivities(plan: CarePlanResponseDTO): CareActivityResponseDTO[] {
    return plan.activities.filter(a => a.activityType === CareActivityType.PHYSICAL_ACTIVITY);
  }

  getNutritionActivities(plan: CarePlanResponseDTO): CareActivityResponseDTO[] {
    return plan.activities.filter(a => a.activityType === CareActivityType.NUTRITION_PLAN);
  }
}
