import { Component, DestroyRef, OnInit, OnDestroy, signal, PLATFORM_ID, inject, computed } from '@angular/core';
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
import { DoctorNotificationService } from '@/core/services/doctor-notification.service';
import { type DoctorNotification } from '@/core/models/notification.model';
import { PrescriptionManagementComponent } from './prescription-management/prescription-management.component';
import { SuiviQuotidienComponent } from '@/pages/patient-dashboard/helper-view/suivi-quotidien/suivi-quotidien.component';
import { CarePlanManagementComponent } from './care-plan-management/care-plan-management.component';
import { MedicalFolderListComponent } from '@/pages/medical-folders/medical-folder-list/medical-folder-list.component';
import { DossierAnalyticsComponent } from '@/pages/medical-folders/dossier-analytics/dossier-analytics.component';
import { MedicationManagementComponent } from '../medications/medications.component';
import { PatientAnalyticsComponent } from './patient-analytics/patient-analytics.component';
import { ProfileComponent } from '@/pages/patient-dashboard/helper-view/profile/profile.component';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    ZardButtonComponent,
    ZardProgressBarComponent,
    PrescriptionManagementComponent,
    SuiviQuotidienComponent,
    CarePlanManagementComponent,
    MedicalFolderListComponent,
    DossierAnalyticsComponent,
    MedicationManagementComponent,
    PatientAnalyticsComponent,
    ProfileComponent,
  ],
  template: `
    @if (kycChecking()) {
      <div class="flex items-center justify-center min-h-screen">
        <div class="text-center space-y-4">
          <z-icon zType="loader-2" class="w-8 h-8 animate-spin mx-auto text-primary" />
          <p class="text-muted-foreground">Checking verification status...</p>
        </div>
      </div>
    } @else if (isKycBlocked) {
      <div class="flex items-center justify-center min-h-screen bg-background">
        <z-card class="w-full max-w-md">
          <div class="p-8 text-center space-y-6">
            <div class="mx-auto w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center">
              <z-icon zType="shield" class="w-8 h-8 text-amber-600" />
            </div>
            <div class="space-y-2">
              <h2 class="text-2xl font-bold">Identity Verification Required</h2>
              <p class="text-muted-foreground">
                As a doctor, you need to verify your identity before accessing the platform.
                This helps us ensure the safety of our patients.
              </p>
            </div>

            @if (kycStatus() === 'pending') {
              <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
                Your verification is being processed. Click "Check Status" to refresh.
              </div>
            } @else if (kycStatus() === 'declined') {
              <div class="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                Your verification was declined. Please try again with valid documents.
              </div>
            } @else if (kycStatus() === 'expired') {
              <div class="p-3 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-700">
                Your verification session has expired. Please start a new one.
              </div>
            }

            <div class="space-y-3">
              @if (kycStatus() === 'none' || kycStatus() === 'declined' || kycStatus() === 'expired') {
                <button z-button class="w-full" zSize="lg" (click)="startKycVerification()">
                  <z-icon zType="shield" class="mr-2" />
                  Start Verification
                </button>
              }
              @if (kycStatus() === 'pending') {
                <button z-button class="w-full" zSize="lg" (click)="refreshKycStatus()">
                  <z-icon zType="rotate-ccw" class="mr-2" />
                  Check Status
                </button>
              }
              <button z-button zType="outline" class="w-full" [disabled]="kycSkipping()" (click)="skipKyc()">
                @if (kycSkipping()) {
                  Skipping...
                } @else {
                  Skip KYC (Dev Build)
                }
              </button>
            </div>
          </div>
        </z-card>
      </div>
    } @else {
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/doctor"
    >
      <div class="space-y-4">
      @switch (currentPage()) {
        @case ('Home') {
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-2xl font-bold">Doctor Dashboard</h2>
            <!-- 🔔 Notification Bell -->
            <div class="relative">
              <button z-button zType="ghost" zSize="sm"
                class="relative p-2"
                (click)="toggleNotificationPanel()">
                <z-icon zType="bell" class="h-5 w-5" />
                @if (notifService.unreadCount() > 0) {
                  <span class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1
                               text-[10px] font-bold rounded-full bg-red-500 text-white
                               flex items-center justify-center leading-none">
                    {{ notifService.unreadCount() > 99 ? '99+' : notifService.unreadCount() }}
                  </span>
                }
              </button>

              @if (showNotifications()) {
                <div class="absolute right-0 top-10 z-50 w-96 max-h-[540px] overflow-hidden
                            rounded-2xl shadow-2xl border border-border bg-background
                            flex flex-col"
                  (click)="$event.stopPropagation()">
                  <!-- Panel header -->
                  <div class="flex items-center justify-between px-4 py-3 border-b border-border">
                    <div class="flex items-center gap-2">
                      <z-icon zType="bell" class="h-4 w-4 text-primary" />
                      <span class="font-semibold text-sm">Notifications d'incidents</span>
                      @if (notifService.unreadCount() > 0) {
                        <span class="px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-red-100 text-red-600">
                          {{ notifService.unreadCount() }} non lu(s)
                        </span>
                      }
                    </div>
                    <div class="flex gap-1">
                      @if (notifService.unreadCount() > 0) {
                        <button z-button zType="ghost" zSize="sm"
                          class="text-xs text-primary"
                          (click)="markAllRead()">Tout lire</button>
                      }
                      <button z-button zType="ghost" zSize="sm" (click)="showNotifications.set(false)">
                        <z-icon zType="x" class="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                  <!-- Notification list -->
                  <div class="overflow-y-auto flex-1">
                    @if (notifService.notifications().length === 0) {
                      <div class="flex flex-col items-center justify-center py-12 text-muted-foreground">
                        <z-icon zType="bell" class="h-8 w-8 mb-2 opacity-30" />
                        <p class="text-sm">Aucune notification</p>
                      </div>
                    }
                    @for (notif of notifService.notifications(); track notif.id) {
                      <div
                        class="px-4 py-3 border-b border-border/50 cursor-pointer transition-colors"
                        [class]="notif.read ? 'bg-background hover:bg-muted/30' : 'bg-amber-50/60 dark:bg-amber-900/10 hover:bg-amber-50'"
                        (click)="openNotification(notif)">
                        <div class="flex items-start gap-3">
                          <!-- Severity icon -->
                          <div class="shrink-0 mt-0.5 p-1.5 rounded-lg"
                            [class]="notif.severity === 'GRAVE' ? 'bg-red-100 text-red-600' : 'bg-orange-100 text-orange-600'">
                            <z-icon zType="triangle-alert" class="h-3.5 w-3.5" />
                          </div>
                          <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-1.5 flex-wrap">
                              <span class="font-semibold text-sm truncate">{{ notif.patientName }}</span>
                              <span class="text-[10px] px-1.5 py-0.5 rounded-full font-bold"
                                [class]="notif.severity === 'GRAVE' ? 'bg-red-100 text-red-700' : 'bg-orange-100 text-orange-700'">
                                {{ notif.severity }}
                              </span>
                              @if (!notif.read) {
                                <span class="w-2 h-2 rounded-full bg-red-500 shrink-0"></span>
                              }
                            </div>
                            <p class="text-xs text-muted-foreground mt-0.5 truncate">{{ notif.incidentType }} — {{ notif.description }}</p>
                            <p class="text-[10px] text-muted-foreground mt-1">{{ formatNotifDate(notif.createdAt) }}</p>
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          </div>

          <!-- Notification detail modal -->
          @if (selectedNotif()) {
            <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
              (click)="selectedNotif.set(null)">
              <div class="w-full max-w-md rounded-2xl bg-background shadow-2xl border border-border p-6"
                (click)="$event.stopPropagation()">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <div class="p-2 rounded-lg"
                      [class]="selectedNotif()!.severity === 'GRAVE' ? 'bg-red-100 text-red-600' : 'bg-orange-100 text-orange-600'">
                      <z-icon zType="triangle-alert" class="h-5 w-5" />
                    </div>
                    <div>
                      <h3 class="font-bold text-base">Incident {{ selectedNotif()!.severity }}</h3>
                      <p class="text-xs text-muted-foreground">{{ formatNotifDate(selectedNotif()!.createdAt) }}</p>
                    </div>
                  </div>
                  <button z-button zType="ghost" zSize="sm" (click)="selectedNotif.set(null)">
                    <z-icon zType="x" class="h-4 w-4" />
                  </button>
                </div>

                <div class="space-y-3 text-sm">
                  <div class="grid grid-cols-2 gap-3">
                    <div class="p-3 rounded-xl bg-muted/40">
                      <p class="text-xs text-muted-foreground mb-1">Patient</p>
                      <p class="font-semibold">{{ selectedNotif()!.patientName }}</p>
                    </div>
                    <div class="p-3 rounded-xl bg-muted/40">
                      <p class="text-xs text-muted-foreground mb-1">Type d'incident</p>
                      <p class="font-semibold">{{ selectedNotif()!.incidentType }}</p>
                    </div>
                    <div class="p-3 rounded-xl bg-muted/40">
                      <p class="text-xs text-muted-foreground mb-1">Date</p>
                      <p class="font-semibold">{{ selectedNotif()!.logDate }}</p>
                    </div>
                    @if (selectedNotif()!.occurredAt) {
                      <div class="p-3 rounded-xl bg-muted/40">
                        <p class="text-xs text-muted-foreground mb-1">Heure</p>
                        <p class="font-semibold">{{ selectedNotif()!.occurredAt }}</p>
                      </div>
                    }
                  </div>

                  <div class="p-3 rounded-xl"
                    [class]="selectedNotif()!.severity === 'GRAVE' ? 'bg-red-50 border border-red-200' : 'bg-orange-50 border border-orange-200'">
                    <p class="text-xs font-semibold mb-1"
                      [class]="selectedNotif()!.severity === 'GRAVE' ? 'text-red-700' : 'text-orange-700'">Description</p>
                    <p class="text-sm">{{ selectedNotif()!.description }}</p>
                  </div>

                  @if (selectedNotif()!.location) {
                    <div class="flex items-center gap-2 text-sm text-muted-foreground">
                      <z-icon zType="map-pin" class="h-4 w-4 shrink-0" />
                      <span>{{ selectedNotif()!.location }}</span>
                    </div>
                  }
                  @if (selectedNotif()!.actionTaken) {
                    <div class="flex items-start gap-2 text-sm">
                      <z-icon zType="check" class="h-4 w-4 shrink-0 text-green-600 mt-0.5" />
                      <span class="text-green-700">{{ selectedNotif()!.actionTaken }}</span>
                    </div>
                  }
                </div>

                <div class="mt-5 flex gap-2">
                  <button z-button class="flex-1" (click)="viewPatientFromNotif(selectedNotif()!)">Voir le journal patient</button>
                  <button z-button zType="outline" (click)="selectedNotif.set(null)">Fermer</button>
                </div>
              </div>
            </div>
          }

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
                  <p class="text-sm text-muted-foreground">Games Played</p>
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
                <z-icon zType="bar-chart-3" class="text-primary h-8 w-8" />
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
                             <button z-button zType="ghost" zSize="sm" (click)="viewMedications(patient)">
                               <z-icon zType="pill" class="mr-1" />
                               Meds
                             </button>
                             <button z-button zType="ghost" zSize="sm" (click)="managePrescriptions(patient)">
                               <z-icon zType="file-text" class="mr-1" />
                               Rx
                             </button>
                             <button z-button zType="ghost" zSize="sm" (click)="viewDailyLog(patient)">
                               <z-icon zType="file" class="mr-1" />
                               Journal
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

            <app-patient-analytics [patientKeycloakId]="selectedPatient()!.keycloakId"></app-patient-analytics>
          } @else {
            <p class="text-muted-foreground">Select a patient from the Patients list to view their progress.</p>
          }
        }

        @case ('Daily Log') {
          @if (selectedPatient(); as patient) {
            <app-suivi-quotidien
              [keycloakId]="patient.keycloakId"
              [readOnly]="true"
              (goBack)="setPage('Home')" />
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

        @case ('Mon Profil') {
          <app-profile [keycloakId]="doctorKeycloakId" (goBack)="setPage('Home')" />
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
        @case ('Medications') {
          @if (selectedPatient(); as patient) {
             <div class="flex items-center gap-2 mb-6">
              <button z-button zType="ghost" zSize="sm" (click)="setPage('Home')">
                <z-icon zType="arrow-left" class="mr-1" />
                Back to List
              </button>
            </div>
            
            <app-medication-management [patient]="patient" [doctor]="currentDoctor()" viewMode="patient"></app-medication-management>
          } @else {
             <div class="space-y-4">
               <h2 class="text-2xl font-bold">Medications</h2>
               <div class="p-8 border rounded-lg text-center bg-muted/20">
                 <z-icon zType="pill" class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                 <h3 class="text-lg font-semibold mb-2">No Patient Selected</h3>
                 <p class="text-muted-foreground mb-4">Please select a patient from the main list to view their medications.</p>
                 <button z-button (click)="setPage('Home')">Go to Patient List</button>
               </div>
             </div>
          }
      }
      }
      </div>
    </app-dashboard-layout>
    }
  `,
})
export class DoctorDashboardComponent implements OnInit, OnDestroy {
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

  // Search/Medical Folders
  searchInput = signal('');
  searchResults = signal<MedicalFolder[]>([]);
  searchSelectedFolderId = signal<number | null>(null);
  private readonly searchSubject = new Subject<string>();
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private platformId = inject(PLATFORM_ID);

  // Notifications
  showNotifications = signal(false);
  selectedNotif = signal<DoctorNotification | null>(null);
  private notifInterval: ReturnType<typeof setInterval> | null = null;

  // KYC state
  kycStatus = signal<string>('none');
  kycChecking = signal(true);
  kycSkipping = signal(false);

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
        { icon: 'heart', label: 'Medications', action: () => this.setPage('Medications') },
      ],
    },
    {
      label: 'Compte',
      items: [
        { icon: 'calendar', label: '📅 Calendrier', action: () => this.router.navigate(['/doctor/calendar']) },
        { icon: 'calendar', label: 'Synchronisation Google', action: () => this.router.navigate(['/doctor/calendar-sync']) },
        { icon: 'user', label: 'Mon Profil', action: () => this.setPage('Mon Profil') },
      ],
    },
  ];

  doctorKeycloakId = '';

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
    public readonly notifService: DoctorNotificationService,
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
      // Get keycloak ID for profile
      const kc = this.keycloakService.getKeycloakInstance();
      this.doctorKeycloakId = kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';

      // Load current doctor info
      const doctorKeycloakId = this.authService.getKeycloakId();
      if (doctorKeycloakId) {
        this.userApiService.getUserByKeycloakId(doctorKeycloakId).subscribe({
          next: doctor => {
            this.currentDoctor.set(doctor);
            // Check KYC status from the user record
            const status = doctor.kycStatus ?? 'none';
            this.kycStatus.set(status);
            this.kycChecking.set(false);

            // If pending, try refreshing from Didit
            if (status === 'pending') {
              this.refreshKycStatus();
            }

            // Recharger les notifications avec l'ID correct de la DB
            // (en cas de changement Keycloak, l'ID token peut différer de l'ID DB)
            if (doctor.keycloakId) {
              this.notifService.loadNotifications(doctor.keycloakId).subscribe();
            }
          },
          error: err => {
            console.error('Failed to load doctor info', err);
            this.kycChecking.set(false);
          }
        });
      } else {
        this.kycChecking.set(false);
      }
      this.loadPatients();

      // Load notifications — use Keycloak ID from token
      // Will also reload after currentDoctor() is set (see loadNotificationsForDoctor)
      if (this.doctorKeycloakId) {
        this.loadNotificationsForDoctor(this.doctorKeycloakId);
        this.notifInterval = setInterval(() => {
          this.loadNotificationsForDoctor(this.doctorKeycloakId);
        }, 10_000); // Poll toutes les 10s pour réactivité maximale
      }
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

  ngOnDestroy(): void {
    if (this.notifInterval) clearInterval(this.notifInterval);
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

  viewMedications(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.setPage('Medications');
  }


  viewDailyLog(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.setPage('Daily Log');
  }

  retryLoadPatients(): void {
    this.loadPatients();
  }

  // ── Notification methods ─────────────────────────────────────────────────

  /**
   * Charge les notifications en essayant d'abord l'ID Keycloak du token,
   * puis l'ID stocké dans currentDoctor() si disponible.
   * Après les changements Keycloak, ces deux IDs peuvent différer.
   */
  /**
   * Charge les notifications en essayant intelligemment tous les IDs disponibles.
   * Résoudre le mismatch d'ID Keycloak après reconfiguration.
   */
  loadNotificationsForDoctor(keycloakIdFromToken: string): void {
    const doctorFromDb = this.currentDoctor();
    const fallbackIds: string[] = [];
    if (doctorFromDb?.keycloakId && doctorFromDb.keycloakId !== keycloakIdFromToken) {
      fallbackIds.push(doctorFromDb.keycloakId);
    }
    this.notifService.loadNotificationsSmartly(keycloakIdFromToken, fallbackIds).subscribe();
  }

  /** Appelé depuis suivi-quotidien après ajout d'un incident — recharge immédiatement */
  refreshNotifications(): void {
    // Attendre 2s que le backend async sauvegarde la notification
    setTimeout(() => {
      this.loadNotificationsForDoctor(this.doctorKeycloakId);
    }, 2000);
  }

  toggleNotificationPanel(): void {
    this.showNotifications.update(v => !v);
    if (this.showNotifications()) {
      this.selectedNotif.set(null);
    }
  }

  openNotification(notif: DoctorNotification): void {
    this.selectedNotif.set(notif);
    this.showNotifications.set(false);
    if (!notif.read) {
      this.notifService.markAsRead(notif.id).subscribe();
    }
  }

  markAllRead(): void {
    // Utiliser l'ID de la DB en priorité (plus fiable après changement Keycloak)
    const id = this.currentDoctor()?.keycloakId || this.doctorKeycloakId;
    if (id) {
      this.notifService.markAllAsRead(id).subscribe();
    }
  }

  viewPatientFromNotif(notif: DoctorNotification): void {
    const patient = this.patients().find(p => p.keycloakId === notif.patientKeycloakId);
    if (patient) {
      this.selectedNotif.set(null);
      this.viewDailyLog(patient);
    }
  }

  formatNotifDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - d.getTime();
      const diffMin = Math.floor(diffMs / 60000);
      if (diffMin < 1) return 'À l\'instant';
      if (diffMin < 60) return `Il y a ${diffMin} min`;
      const diffH = Math.floor(diffMin / 60);
      if (diffH < 24) return `Il y a ${diffH}h`;
      return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    } catch { return dateStr; }
  }

  // ─── KYC Methods ──────────────────────────────────────────────

  get isKycBlocked(): boolean {
    const status = this.kycStatus();
    return status !== 'approved' && status !== 'skipped';
  }

  refreshKycStatus(): void {
    const keycloakId = this.authService.getKeycloakId();
    if (!keycloakId) return;

    this.userApiService.getKycStatus(keycloakId).subscribe({
      next: result => {
        this.kycStatus.set(result.kyc_status);
      },
      error: err => console.error('Failed to check KYC status', err),
    });
  }

  startKycVerification(): void {
    const keycloakId = this.authService.getKeycloakId();
    if (!keycloakId) return;

    this.userApiService.startKyc(keycloakId).subscribe({
      next: result => {
        if (result.url) {
          window.open(result.url, '_blank');
          this.kycStatus.set('pending');
        }
      },
      error: err => console.error('Failed to start KYC', err),
    });
  }

  skipKyc(): void {
    const keycloakId = this.authService.getKeycloakId();
    if (!keycloakId) return;

    this.kycSkipping.set(true);
    this.userApiService.skipKyc(keycloakId).subscribe({
      next: () => {
        this.kycStatus.set('skipped');
        this.kycSkipping.set(false);
      },
      error: err => {
        console.error('Failed to skip KYC', err);
        this.kycSkipping.set(false);
      },
    });
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
            error: err => {
              console.warn(`Failed to load game stats for patient ${patient.keycloakId}`, err);
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
    this.totalPatientGames = stats.reduce((sum, s) => sum + s.totalGamesPlayed, 0);
    const withScores = stats.filter(s => s.totalAttempts > 0);
    this.avgPatientScore = withScores.length > 0
      ? withScores.reduce((sum, s) => sum + s.averageScore, 0) / withScores.length
      : 0;
  }
}
