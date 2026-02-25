import { Component, DestroyRef, OnInit, signal, PLATFORM_ID, inject, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { debounceTime, switchMap, map } from 'rxjs/operators';
import { AuthService } from '@/core/auth';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';
import { GameService, type GameStatsResponse } from '@/core/services/game.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import type { MedicalFolder } from '@/core/services/medical-folder.service';
import { PrescriptionManagementComponent } from './prescription-management/prescription-management.component';
import { CarePlanManagementComponent } from './care-plan-management/care-plan-management.component';
import { MedicalFolderListComponent } from '@/pages/medical-folders/medical-folder-list/medical-folder-list.component';
import { DossierAnalyticsComponent } from '@/pages/medical-folders/dossier-analytics/dossier-analytics.component';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    ZardButtonComponent,
    ZardProgressBarComponent,
    PrescriptionManagementComponent,
    CarePlanManagementComponent,
    MedicalFolderListComponent,
    DossierAnalyticsComponent,
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/doctor"
    >
      <div class="space-y-4">
      @switch (currentPage()) {
        @case ('Home') {
          <h2 class="text-2xl font-bold mb-6">Doctor Dashboard</h2>
          <div class="grid gap-4 md:grid-cols-3 mb-8">
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">My Patients</p>
                  <p class="text-3xl font-bold">{{ patients().length }}</p>
                </div>
                <z-icon zType="user" class="text-primary h-8 w-8" />
              </div>
            </z-card>
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Active Games</p>
                  <p class="text-3xl font-bold">{{ totalPatientGames }}</p>
                </div>
                <z-icon zType="gamepad-2" class="text-primary h-8 w-8" />
              </div>
            </z-card>
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Avg Patient Score</p>
                  <p class="text-3xl font-bold">{{ avgPatientScore | number:'1.0-0' }}%</p>
                </div>
                <z-icon zType="trending-up" class="text-primary h-8 w-8" />
              </div>
            </z-card>
          </div>

          <z-card>
            <div class="p-6">
              <h3 class="text-lg font-semibold mb-4">Patients Overview</h3>
              @if (isLoading()) {
                <z-skeleton class="h-32 w-full" />
              } @else if (error()) {
                <div class="p-8 text-center text-red-500 bg-red-50 dark:bg-red-900/20 rounded-lg">
                  <p class="font-semibold text-lg mb-2">Error loading patients</p>
                  <p class="mb-4">{{ error() }}</p>
                  <button z-button (click)="retryLoadPatients()">Retry</button>
                </div>
              } @else if (patients().length > 0) {
                <table z-table>
                  <thead z-table-header>
                    <tr z-table-row>
                      <th z-table-head>Patient</th>
                      <th z-table-head>Email</th>
                      <th z-table-head>Games Created</th>
                      <th z-table-head>Games Played</th>
                      <th z-table-head>Avg Score</th>
                      <th z-table-head>Actions</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (patient of patients(); track patient.keycloakId) {
                      <tr z-table-row>
                        <td z-table-cell class="font-medium">{{ patient.firstName }} {{ patient.lastName }}</td>
                        <td z-table-cell class="text-muted-foreground">{{ patient.email }}</td>
                        <td z-table-cell>{{ getPatientStat(patient.keycloakId)?.totalGamesCreated ?? '-' }}</td>
                        <td z-table-cell>{{ getPatientStat(patient.keycloakId)?.totalGamesPlayed ?? '-' }}</td>
                        <td z-table-cell>
                          @if (getPatientStat(patient.keycloakId); as stat) {
                            <div class="flex items-center gap-2">
                              <z-progress-bar [progress]="stat.averageScore" class="w-16 h-2" />
                              <span class="text-sm">{{ stat.averageScore | number:'1.0-0' }}%</span>
                            </div>
                          } @else {
                            <span class="text-muted-foreground">-</span>
                          }
                        </td>
                        <td z-table-cell>
                          <div class="flex gap-2">
                             <button z-button zType="ghost" zSize="sm" (click)="viewPatientProgress(patient)">
                               <z-icon zType="bar-chart-3" class="mr-1" />
                               Progress
                             </button>
                             <button z-button zType="ghost" zSize="sm" (click)="managePrescriptions(patient)">
                               <z-icon zType="pill" class="mr-1" />
                               Rx
                             </button>
                             <button z-button zType="ghost" zSize="sm" (click)="manageCarePlans(patient)">
                               <z-icon zType="activity" class="mr-1" />
                               Plan
                             </button>
                          </div>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <div class="p-12 text-center text-muted-foreground bg-slate-50 dark:bg-slate-800/50 rounded-lg">
                  <z-icon zType="user" class="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p class="text-lg font-medium">No patients found</p>
                  <p class="text-sm mt-1">Patients assigned to you will appear here.</p>
                </div>
              }
            </div>
          </z-card>
        }

        @case ('Patient Progress') {
          @if (selectedPatient()) {
            <div class="flex items-center gap-2 mb-6">
              <button z-button zType="ghost" zSize="sm" (click)="setPage('Home')">
                <z-icon zType="arrow-left" class="mr-1" />
                Back
              </button>
              <h2 class="text-2xl font-bold">{{ selectedPatient()!.firstName }} {{ selectedPatient()!.lastName }}'s Progress</h2>
            </div>

            @if (selectedPatientStats(); as stat) {
              <div class="grid gap-4 md:grid-cols-4 mb-8">
                <z-card class="p-6">
                  <p class="text-sm text-muted-foreground">Games Created</p>
                  <p class="text-3xl font-bold">{{ stat.totalGamesCreated }}</p>
                </z-card>
                <z-card class="p-6">
                  <p class="text-sm text-muted-foreground">Games Played</p>
                  <p class="text-3xl font-bold">{{ stat.totalGamesPlayed }}</p>
                </z-card>
                <z-card class="p-6">
                  <p class="text-sm text-muted-foreground">Total Attempts</p>
                  <p class="text-3xl font-bold">{{ stat.totalAttempts }}</p>
                </z-card>
                <z-card class="p-6">
                  <p class="text-sm text-muted-foreground">Average Score</p>
                  <p class="text-3xl font-bold">{{ stat.averageScore | number:'1.0-0' }}%</p>
                </z-card>
              </div>

              <z-card class="p-6">
                <h3 class="text-lg font-semibold mb-4">Performance Overview</h3>
                <div class="space-y-4">
                  <div>
                    <div class="flex justify-between text-sm mb-1">
                      <span>Average Score</span>
                      <span>{{ stat.averageScore | number:'1.0-0' }}%</span>
                    </div>
                    <z-progress-bar [progress]="stat.averageScore" />
                  </div>
                  <div>
                    <div class="flex justify-between text-sm mb-1">
                      <span>Best Score</span>
                      <span>{{ stat.bestScore }}</span>
                    </div>
                    <z-progress-bar [progress]="stat.totalGamesCreated > 0 ? (stat.bestScore / stat.totalGamesCreated * 100) : 0" />
                  </div>
                </div>
              </z-card>
            } @else {
              <z-skeleton class="h-48 w-full" />
            }
          } @else {
            <p class="text-muted-foreground">Select a patient from the Patients list to view their progress.</p>
          }
        }

        @case ('Prescriptions') {
          @if (selectedPatient(); as patient) {
             <div class="flex items-center gap-2 mb-6">
              <button z-button zType="ghost" zSize="sm" (click)="setPage('Home')">
                <z-icon zType="arrow-left" class="mr-1" />
                Back to List
              </button>
            </div>
            
            <app-prescription-management [patient]="patient" [doctor]="currentDoctor()"></app-prescription-management>
          } @else {
             <div class="space-y-4">
               <h2 class="text-2xl font-bold">Manage Prescriptions</h2>
               <div class="p-8 border rounded-lg text-center bg-muted/20">
                 <z-icon zType="user" class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                 <h3 class="text-lg font-semibold mb-2">No Patient Selected</h3>
                 <p class="text-muted-foreground mb-4">Please select a patient from the main list to manage their prescriptions.</p>
                 <button z-button (click)="setPage('Home')">Go to Patient List</button>
               </div>
             </div>
          }
        }

        @case ('CarePlans') {
          @if (selectedPatient(); as patient) {
             <div class="flex items-center gap-2 mb-6">
              <button z-button zType="ghost" zSize="sm" (click)="setPage('Home')">
                <z-icon zType="arrow-left" class="mr-1" />
                Back to List
              </button>
            </div>
            
            <app-care-plan-management [patient]="patient" [doctor]="currentDoctor()"></app-care-plan-management>
          } @else {
             <div class="space-y-4">
               <h2 class="text-2xl font-bold">Manage Care Plans</h2>
               <div class="p-8 border rounded-lg text-center bg-muted/20">
                 <z-icon zType="users" class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                 <h3 class="text-lg font-semibold mb-2">No Patient Selected</h3>
                 <p class="text-muted-foreground mb-4">Please select a patient from the main list to manage their care plans.</p>
                 <button z-button (click)="setPage('Home')">Go to Patient List</button>
               </div>
             </div>
          }
        }

        @case ('Medical Folders') {
          <app-medical-folder-list
            [initialFolderId]="searchSelectedFolderId()"
            [doctorId]="doctorIdString()"
            [doctor]="currentDoctor()"
            (detailClosed)="searchSelectedFolderId.set(null)"
          />
        }
        @case ('Dossier Analytics') {
          <app-dossier-analytics />
        }
      }
      </div>
    </app-dashboard-layout>
  `,
})
export class DoctorDashboardComponent implements OnInit {
  private static readonly PAGE_STORAGE_KEY = 'tfk_doctor_current_page';
  private static readonly PAGE_QUERY_PARAM = 'page';

  currentPage = signal('Home');
  patients = signal<UserInfo[]>([]);
  patientStats = signal<Map<string, GameStatsResponse>>(new Map());
  selectedPatient = signal<UserInfo | null>(null);
  selectedPatientStats = signal<GameStatsResponse | null>(null);
  currentDoctor = signal<UserInfo | null>(null);
  isLoading = signal(true);
  error = signal<string | null>(null);
  totalPatientGames = 0;
  avgPatientScore = 0;

  searchInput = signal('');
  searchResults = signal<MedicalFolder[]>([]);
  searchSelectedFolderId = signal<number | null>(null);
  private readonly searchSubject = new Subject<string>();
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);

  /** Use doctor's Keycloak ID to match medical-folder doctorId storage */
  doctorIdString = computed(() => {
    const keycloakId = this.currentDoctor()?.keycloakId;
    return keycloakId || null;
  });

  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'Navigation',
      items: [
        { icon: 'house', label: 'Home', action: () => this.setPage('Home') },
        { icon: 'users', label: 'Patients', action: () => this.setPage('Home') },
        { icon: 'folder', label: 'Medical Folders', action: () => this.setPage('Medical Folders') },
        { icon: 'activity', label: 'Dossier Analytics', action: () => this.setPage('Dossier Analytics') },
        { icon: 'bar-chart-3', label: 'Patient Progress', action: () => this.setPage('Patient Progress') },
        { icon: 'pill', label: 'Prescriptions', action: () => this.setPage('Prescriptions') },
        { icon: 'activity', label: 'Care Plans', action: () => this.setPage('CarePlans') },
      ],
    },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
  ) {
  }

  ngOnInit(): void {
    console.log('[doctor-dashboard] ngOnInit started');
    this.restoreCurrentPage();
    console.log('[doctor-dashboard] After restore, currentPage:', this.currentPage());
    
    this.searchSubject
      .pipe(
        debounceTime(300),
        switchMap((term) => {
          if (!term.trim()) {
            this.searchResults.set([]);
            return of({ term: '', folders: [] as MedicalFolder[] });
          }
          return this.medicalFolderService.getAll().pipe(
            map((folders) => ({ term: term.trim().toLowerCase(), folders })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ term, folders }) => {
        if (!term) {
          this.searchResults.set([]);
          return;
        }
        const filtered = folders.filter(
          (f) =>
            f.patientId?.toLowerCase().includes(term) ||
            f.doctorId?.toLowerCase().includes(term),
        );
        this.searchResults.set(filtered.slice(0, 5));
      });

    if (isPlatformBrowser(this.platformId)) {
      const doctorKeycloakId = this.authService.getKeycloakId();
      if (doctorKeycloakId) {
        this.userApiService.getUserByKeycloakId(doctorKeycloakId).subscribe({
          next: doctor => this.currentDoctor.set(doctor),
          error: err => console.error('Failed to load doctor info', err)
        });
      }
      this.loadPatients();
    }
  }

  onSearchInput(value: string): void {
    this.searchInput.set(value);
    this.searchSubject.next(value);
  }

  openFolderFromSearch(folder: MedicalFolder): void {
    this.searchSelectedFolderId.set(folder.id);
    this.searchResults.set([]);
    this.searchInput.set('');
    this.setPage('Medical Folders');
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(DoctorDashboardComponent.PAGE_STORAGE_KEY, page);
    }

    const queryPage = this.toQueryPage(page);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryPage ? { [DoctorDashboardComponent.PAGE_QUERY_PARAM]: queryPage } : { [DoctorDashboardComponent.PAGE_QUERY_PARAM]: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private restoreCurrentPage(): void {
    console.log('[doctor-dashboard] restoreCurrentPage called');
    const queryPage = this.fromQueryPage(this.route.snapshot.queryParamMap.get(DoctorDashboardComponent.PAGE_QUERY_PARAM));
    console.log('[doctor-dashboard] queryPage from route:', queryPage, 'currentPage before set:', this.currentPage());
    if (queryPage) {
      this.currentPage.set(queryPage);
      console.log('[doctor-dashboard] Set currentPage from route query to:', queryPage, 'currentPage after set:', this.currentPage());
      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem(DoctorDashboardComponent.PAGE_STORAGE_KEY, queryPage);
      }
      return;
    }

    if (!isPlatformBrowser(this.platformId)) {
      console.log('[doctor-dashboard] Not in browser, skipping localStorage restore');
      return;
    }

    const savedPage = localStorage.getItem(DoctorDashboardComponent.PAGE_STORAGE_KEY);
    console.log('[doctor-dashboard] savedPage from localStorage:', savedPage);
    if (!savedPage) {
      console.log('[doctor-dashboard] No saved page in localStorage');
      return;
    }

    const validPages = new Set([
      'Home',
      'Patient Progress',
      'Prescriptions',
      'CarePlans',
      'Medical Folders',
      'Dossier Analytics',
    ]);

    if (validPages.has(savedPage)) {
      this.currentPage.set(savedPage);
      console.log('[doctor-dashboard] Set currentPage from localStorage to:', savedPage, 'currentPage after set:', this.currentPage());
      const savedQueryPage = this.toQueryPage(savedPage);
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: savedQueryPage ? { [DoctorDashboardComponent.PAGE_QUERY_PARAM]: savedQueryPage } : { [DoctorDashboardComponent.PAGE_QUERY_PARAM]: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  private toQueryPage(page: string): string | null {
    switch (page) {
      case 'Medical Folders':
        return 'medical-folders';
      case 'Dossier Analytics':
        return 'dossier-analytics';
      case 'Patient Progress':
        return 'patient-progress';
      case 'Prescriptions':
        return 'prescriptions';
      case 'CarePlans':
        return 'careplans';
      case 'Home':
      default:
        return null;
    }
  }

  private fromQueryPage(queryPage: string | null): string | null {
    switch (queryPage) {
      case 'medical-folders':
        return 'Medical Folders';
      case 'dossier-analytics':
        return 'Dossier Analytics';
      case 'patient-progress':
        return 'Patient Progress';
      case 'prescriptions':
        return 'Prescriptions';
      case 'careplans':
        return 'CarePlans';
      case 'home':
        return 'Home';
      default:
        return null;
    }
  }

  getPatientStat(keycloakId: string): GameStatsResponse | undefined {
    return this.patientStats().get(keycloakId);
  }

  viewPatientProgress(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.gameService.getPlayerStats(patient.keycloakId).subscribe({
      next: stats => this.selectedPatientStats.set(stats),
      error: () => this.selectedPatientStats.set(null),
    });
    this.setPage('Patient Progress');
  }

  managePrescriptions(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.setPage('Prescriptions');
  }
  manageCarePlans(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.setPage('CarePlans');
  }


  retryLoadPatients(): void {
    this.loadPatients();
  }

  private loadPatients(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.userApiService.getUsersByRole('patient').subscribe({
      next: patients => {
        this.patients.set(patients);
        this.isLoading.set(false);
        // Load stats for each patient
        for (const patient of patients) {
          this.gameService.getPlayerStats(patient.keycloakId).subscribe({
            next: stats => {
              const map = new Map(this.patientStats());
              map.set(patient.keycloakId, stats);
              this.patientStats.set(map);
              this.computeAggregates();
            },
          });
        }
      },
      error: err => {
        console.error('Failed to load patients', err);
        this.error.set('Unable to load patients. Please check the backend connection.');
        this.isLoading.set(false);
      },
    });
  }

  private computeAggregates(): void {
    const stats = Array.from(this.patientStats().values());
    this.totalPatientGames = stats.reduce((sum, s) => sum + s.totalGamesCreated, 0);
    const withScores = stats.filter(s => s.totalAttempts > 0);
    this.avgPatientScore = withScores.length > 0
      ? withScores.reduce((sum, s) => sum + s.averageScore, 0) / withScores.length
      : 0;
  }
}
