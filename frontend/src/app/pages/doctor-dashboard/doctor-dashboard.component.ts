import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/doctor-dashboard"
    >
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
                <z-icon zType="users" class="text-primary h-8 w-8" />
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
              @if (patients().length > 0) {
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
                          <button z-button zType="ghost" zSize="sm" (click)="viewPatientProgress(patient)">
                            <z-icon zType="eye" class="mr-1" />
                            View
                          </button>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <z-skeleton class="h-32 w-full" />
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
      }
    </app-dashboard-layout>
  `,
})
export class DoctorDashboardComponent implements OnInit {
  currentPage = signal('Home');
  patients = signal<UserInfo[]>([]);
  patientStats = signal<Map<string, GameStatsResponse>>(new Map());
  selectedPatient = signal<UserInfo | null>(null);
  selectedPatientStats = signal<GameStatsResponse | null>(null);
  totalPatientGames = 0;
  avgPatientScore = 0;

  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'Navigation',
      items: [
        { icon: 'house', label: 'Home', action: () => this.setPage('Home') },
        { icon: 'users', label: 'Patients', action: () => this.setPage('Home') },
        { icon: 'bar-chart-3', label: 'Patient Progress', action: () => this.setPage('Patient Progress') },
      ],
    },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  setPage(page: string): void {
    this.currentPage.set(page);
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

  private loadPatients(): void {
    this.userApiService.getUsersByRole('patient').subscribe({
      next: patients => {
        this.patients.set(patients);
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
      error: err => console.error('Failed to load patients', err),
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
