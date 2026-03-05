import { Component, DestroyRef, OnInit, signal, PLATFORM_ID, inject, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, catchError } from 'rxjs';
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
import { QuizService } from '@/core/services/quiz.service';
import { QuizDTO, QuestionDTO } from '@/core/models/quiz.model';
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

        <!-- ══════════════════ HOME / PATIENTS LIST ══════════════════ -->
        @case ('Home') {
          <h2 class="text-2xl font-bold mb-6">Doctor Dashboard</h2>

          <!-- Summary cards -->
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
                  <p class="text-sm text-muted-foreground">Avg Game Score</p>
                  <p class="text-3xl font-bold">{{ avgPatientScore | number:'1.0-0' }}%</p>
                </div>
                <z-icon zType="bar-chart-3" class="text-primary h-8 w-8" />
              </div>
            </z-card>
          </div>

          <!-- Patients table -->
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
                      <th z-table-head>Games Played</th>
                      <th z-table-head>Avg Game Score</th>
                      <th z-table-head>
                        <span class="flex items-center gap-1">
                          🧠 Alzheimer's Risk
                          <span class="text-xs text-muted-foreground font-normal ml-1">(avg quiz)</span>
                        </span>
                      </th>
                      <th z-table-head>Risk Level</th>
                      <th z-table-head>Actions</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (patient of patients(); track patient.keycloakId) {
                      <tr z-table-row>
                        <!-- Name + avatar -->
                        <td z-table-cell class="font-medium">
                          <div class="flex items-center gap-2">
                            <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-violet-500 flex items-center justify-center text-white text-xs font-bold">
                              {{ (patient.firstName.charAt(0) || '') + (patient.lastName.charAt(0) || '') }}
                            </div>
                            {{ patient.firstName }} {{ patient.lastName }}
                          </div>
                        </td>
                        <!-- Email -->
                        <td z-table-cell class="text-muted-foreground">{{ patient.email }}</td>
                        <!-- Games played -->
                        <td z-table-cell>{{ getPatientStat(patient.keycloakId)?.totalGamesPlayed ?? '-' }}</td>
                        <!-- Avg game score -->
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
                        <!-- Alzheimer's risk score (avg quiz = % wrong) -->
                        <td z-table-cell>
                          @if (getQuizScore(patient.keycloakId); as qs) {
                            <div class="flex items-center gap-2">
                              <div class="w-full max-w-[80px] h-2 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                                <div class="h-full rounded-full transition-all"
                                  [style.width.%]="qs"
                                  [class]="qs >= 60 ? 'bg-red-500' : qs >= 30 ? 'bg-amber-400' : 'bg-emerald-500'">
                                </div>
                              </div>
                              <span class="text-sm font-semibold"
                                [class]="qs >= 60 ? 'text-red-600' : qs >= 30 ? 'text-amber-600' : 'text-emerald-600'">
                                {{ qs | number:'1.0-0' }}%
                              </span>
                            </div>
                          } @else {
                            <span class="text-xs text-muted-foreground italic">No quiz yet</span>
                          }
                        </td>
                        <!-- Risk level badge -->
                        <td z-table-cell>
                          @if (getQuizScore(patient.keycloakId); as qs) {
                            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold"
                              [class]="qs >= 60
                                ? 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300'
                                : qs >= 30
                                  ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
                                  : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'">
                              {{ qs >= 60 ? '🔴 High Risk' : qs >= 30 ? '🟡 Moderate' : '🟢 Low Risk' }}
                            </span>
                          } @else {
                            <span class="text-xs text-muted-foreground">—</span>
                          }
                        </td>
                        <!-- Action buttons -->
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

        <!-- ══════════════════ PATIENT PROGRESS ══════════════════ -->
        @case ('Patient Progress') {
          @if (selectedPatient(); as patient) {
            <!-- Back + heading -->
            <div class="flex items-center gap-2 mb-6">
              <button z-button zType="ghost" zSize="sm" (click)="setPage('Home')">
                <z-icon zType="arrow-left" class="mr-1" />
                Back
              </button>
              <h2 class="text-2xl font-bold">
                {{ patient.firstName }} {{ patient.lastName }}'s Progress
              </h2>
            </div>

            <app-patient-analytics [patientKeycloakId]="selectedPatient()!.keycloakId"></app-patient-analytics>

            <!-- ── Game Stats ──────────────────────────────────── -->
            @if (selectedPatientStats(); as stat) {
              <div class="grid gap-4 md:grid-cols-4 mb-6">
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
                  <p class="text-sm text-muted-foreground">Avg Game Score</p>
                  <p class="text-3xl font-bold">{{ stat.averageScore | number:'1.0-0' }}%</p>
                </z-card>
              </div>
            } @else {
              <z-skeleton class="h-32 w-full mb-6" />
            }

            <!-- ── Alzheimer's Risk Quiz Summary ──────────────── -->
            <div class="grid gap-4 md:grid-cols-3 mb-6">
              <!-- Average risk score -->
              <z-card class="p-6">
                <div class="flex items-center justify-between mb-3">
                  <p class="text-sm font-semibold text-slate-600 dark:text-slate-300">🧠 Avg Alzheimer's Risk</p>
                </div>
                @if (selectedPatientAvgRisk() !== null) {
                  <p class="text-4xl font-black mb-1"
                    [class]="selectedPatientAvgRisk()! >= 60 ? 'text-red-500' : selectedPatientAvgRisk()! >= 30 ? 'text-amber-500' : 'text-emerald-500'">
                    {{ selectedPatientAvgRisk()! | number:'1.0-0' }}%
                  </p>
                  <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold"
                    [class]="selectedPatientAvgRisk()! >= 60
                      ? 'bg-red-100 text-red-700'
                      : selectedPatientAvgRisk()! >= 30
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-emerald-100 text-emerald-700'">
                    {{ selectedPatientAvgRisk()! >= 60 ? '🔴 High Risk' : selectedPatientAvgRisk()! >= 30 ? '🟡 Moderate' : '🟢 Low Risk' }}
                  </span>
                } @else {
                  <p class="text-2xl font-bold text-muted-foreground">—</p>
                  <p class="text-xs text-muted-foreground mt-1">No quiz taken yet</p>
                }
              </z-card>

              <!-- Total quizzes taken -->
              <z-card class="p-6">
                <p class="text-sm font-semibold text-slate-600 dark:text-slate-300 mb-3">📝 Quiz Attempts</p>
                @if (selectedPatientQuizCount() !== null) {
                  <p class="text-4xl font-black text-blue-600">{{ selectedPatientQuizCount() }}</p>
                  <p class="text-xs text-muted-foreground mt-1">Total assessments completed</p>
                } @else {
                  <p class="text-2xl font-bold text-muted-foreground">—</p>
                }
              </z-card>

              <!-- Highest level reached (latest quiz) -->
              <z-card class="p-6">
                <p class="text-sm font-semibold text-slate-600 dark:text-slate-300 mb-3">🎯 Last Level Reached</p>
                @if (selectedPatientLastLevel() !== null) {
                  <p class="text-4xl font-black text-violet-600">{{ getLevelLabel(selectedPatientLastLevel()!) }}</p>
                  <p class="text-xs text-muted-foreground mt-1">Level {{ selectedPatientLastLevel() }} of 3</p>
                } @else {
                  <p class="text-2xl font-bold text-muted-foreground">—</p>
                  <p class="text-xs text-muted-foreground mt-1">No quiz taken yet</p>
                }
              </z-card>
            </div>

            <!-- ── Quiz History Table ──────────────────────────── -->
            <z-card class="p-6">
              <h3 class="text-lg font-semibold mb-4">📊 Quiz History</h3>
              @if (isLoadingQuizHistory()) {
                <z-skeleton class="h-40 w-full" />
              } @else if (selectedPatientQuizHistory().length > 0) {
                <table z-table>
                  <thead z-table-header>
                    <tr z-table-row>
                      <th z-table-head>Quiz Topic</th>
                      <th z-table-head>Date</th>
                      <th z-table-head>Risk Score</th>
                      <th z-table-head>Level Reached</th>
                      <th z-table-head>Risk Assessment</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (quiz of selectedPatientQuizHistory(); track quiz.id) {
                      <tr z-table-row>
                        <td z-table-cell class="font-medium">{{ quiz.topic }}</td>
                        <td z-table-cell class="text-muted-foreground text-sm">
                          {{ quiz.dateTaken ? (quiz.dateTaken | date:'dd/MM/yyyy HH:mm') : '—' }}
                        </td>
                        <!-- Risk score bar -->
                        <td z-table-cell>
                          @if (quiz.totalScore !== null && quiz.totalScore !== undefined) {
                            <div class="flex items-center gap-2">
                              <div class="w-20 h-2 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                                <div class="h-full rounded-full"
                                  [style.width.%]="quiz.totalScore"
                                  [class]="quiz.totalScore >= 60 ? 'bg-red-500' : quiz.totalScore >= 30 ? 'bg-amber-400' : 'bg-emerald-500'">
                                </div>
                              </div>
                              <span class="font-bold text-sm"
                                [class]="quiz.totalScore >= 60 ? 'text-red-600' : quiz.totalScore >= 30 ? 'text-amber-600' : 'text-emerald-600'">
                                {{ quiz.totalScore }}%
                              </span>
                            </div>
                          } @else {
                            <span class="text-muted-foreground text-sm">—</span>
                          }
                        </td>
                        <!-- Level reached badge -->
                        <td z-table-cell>
                          @if (quiz.levelReached) {
                            <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold"
                              [class]="quiz.levelReached === 3
                                ? 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300'
                                : quiz.levelReached === 2
                                  ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
                                  : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'">
                              {{ quiz.levelReached === 3 ? '🔴' : quiz.levelReached === 2 ? '🟡' : '🟢' }}
                              Level {{ quiz.levelReached }} — {{ getLevelLabel(quiz.levelReached) }}
                            </span>
                          } @else {
                            <span class="text-muted-foreground text-sm">—</span>
                          }
                        </td>
                        <!-- Risk assessment verdict -->
                        <td z-table-cell>
                          @if (quiz.totalScore !== null && quiz.totalScore !== undefined) {
                            @if (quiz.totalScore >= 60 && quiz.levelReached === 3) {
                              <span class="text-xs font-bold text-red-600 dark:text-red-400">
                                ⚠️ Alzheimer's Risk
                              </span>
                            } @else if (quiz.totalScore < 60) {
                              <span class="text-xs font-bold text-emerald-600 dark:text-emerald-400">
                                ✅ No significant risk
                              </span>
                            } @else {
                              <span class="text-xs text-amber-600 dark:text-amber-400">
                                🟡 Under assessment
                              </span>
                            }
                          } @else {
                            <span class="text-muted-foreground text-sm">—</span>
                          }
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>

                <!-- Risk score legend -->
                <div class="mt-4 p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700">
                  <p class="text-xs font-semibold text-slate-500 mb-1">📖 Risk Score Legend</p>
                  <div class="flex gap-4 text-xs text-slate-500">
                    <span><span class="font-bold text-emerald-600">0–29%</span> — Low risk (mostly correct)</span>
                    <span><span class="font-bold text-amber-600">30–59%</span> — Moderate risk</span>
                    <span><span class="font-bold text-red-600">≥ 60%</span> — High risk (many incorrect answers)</span>
                  </div>
                </div>
              } @else {
                <div class="py-10 text-center text-muted-foreground">
                  <p class="text-4xl mb-3">📭</p>
                  <p class="font-medium">No quiz history found</p>
                  <p class="text-sm mt-1">This patient has not completed any quizzes yet.</p>
                </div>
              }
            </z-card>
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

        <!-- ══════════════════ PRESCRIPTIONS ══════════════════ -->
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
                <z-icon zType="users" class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                <h3 class="text-lg font-semibold mb-2">No Patient Selected</h3>
                <p class="text-muted-foreground mb-4">Please select a patient from the main list to manage their prescriptions.</p>
                <button z-button (click)="setPage('Home')">Go to Patient List</button>
              </div>
            </div>
          }
        }

        <!-- ══════════════════ QUIZ OVERVIEW ══════════════════ -->
        @case ('Quiz Overview') {
          <h2 class="text-2xl font-bold mb-6">🧠 Quiz Overview — All Patients</h2>

          <!-- Risk Distribution Cards -->
          <div class="grid gap-4 md:grid-cols-3 mb-8">
            <z-card class="p-6 border-l-4 border-red-500">
              <p class="text-sm text-muted-foreground mb-1">🔴 High Risk Patients</p>
              <p class="text-4xl font-black text-red-500">{{ quizHighRiskCount() }}</p>
              <p class="text-xs text-muted-foreground mt-1">Risk score ≥ 60%</p>
            </z-card>
            <z-card class="p-6 border-l-4 border-amber-400">
              <p class="text-sm text-muted-foreground mb-1">🟡 Moderate Risk</p>
              <p class="text-4xl font-black text-amber-500">{{ quizModerateRiskCount() }}</p>
              <p class="text-xs text-muted-foreground mt-1">Risk score 30–59%</p>
            </z-card>
            <z-card class="p-6 border-l-4 border-emerald-500">
              <p class="text-sm text-muted-foreground mb-1">🟢 Low Risk</p>
              <p class="text-4xl font-black text-emerald-500">{{ quizLowRiskCount() }}</p>
              <p class="text-xs text-muted-foreground mt-1">Risk score &lt; 30%</p>
            </z-card>
          </div>

          <!-- Patients Quiz Summary Table -->
          <z-card class="p-6">
            <h3 class="text-lg font-semibold mb-4">📊 Alzheimer's Risk per Patient</h3>
            @if (isLoading()) {
              <z-skeleton class="h-40 w-full" />
            } @else if (patients().length > 0) {
              <table z-table>
                <thead z-table-header>
                  <tr z-table-row>
                    <th z-table-head>Patient</th>
                    <th z-table-head>Alzheimer's Risk Score</th>
                    <th z-table-head>Risk Level</th>
                    <th z-table-head>Action</th>
                  </tr>
                </thead>
                <tbody z-table-body>
                  @for (patient of patients(); track patient.keycloakId) {
                    <tr z-table-row>
                      <!-- Name -->
                      <td z-table-cell class="font-medium">
                        <div class="flex items-center gap-2">
                          <div class="w-8 h-8 rounded-full bg-gradient-to-br from-violet-500 to-blue-500 flex items-center justify-center text-white text-xs font-bold">
                            {{ (patient.firstName.charAt(0) || '') + (patient.lastName.charAt(0) || '') }}
                          </div>
                          {{ patient.firstName }} {{ patient.lastName }}
                        </div>
                      </td>
                      <!-- Risk score bar -->
                      <td z-table-cell>
                        @if (getQuizScore(patient.keycloakId); as qs) {
                          <div class="flex items-center gap-3">
                            <div class="w-32 h-3 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                              <div class="h-full rounded-full transition-all"
                                [style.width.%]="qs"
                                [class]="qs >= 60 ? 'bg-red-500' : qs >= 30 ? 'bg-amber-400' : 'bg-emerald-500'">
                              </div>
                            </div>
                            <span class="text-sm font-bold"
                              [class]="qs >= 60 ? 'text-red-600' : qs >= 30 ? 'text-amber-600' : 'text-emerald-600'">
                              {{ qs | number:'1.0-0' }}%
                            </span>
                          </div>
                        } @else {
                          <span class="text-xs text-muted-foreground italic">No quiz yet</span>
                        }
                      </td>
                      <!-- Risk badge -->
                      <td z-table-cell>
                        @if (getQuizScore(patient.keycloakId); as qs) {
                          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold"
                            [class]="qs >= 60
                              ? 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300'
                              : qs >= 30
                                ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
                                : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'">
                            {{ qs >= 60 ? '🔴 High Risk' : qs >= 30 ? '🟡 Moderate' : '🟢 Low Risk' }}
                          </span>
                        } @else {
                          <span class="text-xs text-muted-foreground">—</span>
                        }
                      </td>
                      <!-- View progress -->
                      <td z-table-cell>
                        <button z-button zType="ghost" zSize="sm" (click)="viewPatientProgress(patient)">
                          <z-icon zType="bar-chart-3" class="mr-1" />
                          View Progress
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            } @else {
              <div class="py-10 text-center text-muted-foreground">
                <p class="text-4xl mb-3">📭</p>
                <p class="font-medium">No patients found</p>
              </div>
            }
          </z-card>
        }

        <!-- ══════════════════ QUIZ BANK ══════════════════ -->
        @case ('Quiz Bank') {
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-2xl font-bold">📚 Quiz Question Bank</h2>
          </div>

          <!-- Filter buttons -->
          <div class="flex gap-2 mb-6">
            <button z-button
              [zType]="selectedDifficulty() === 0 ? 'default' : 'outline'"
              zSize="sm"
              (click)="filterQuestions(0)">
              All
            </button>
            <button z-button
              [zType]="selectedDifficulty() === 1 ? 'default' : 'outline'"
              zSize="sm"
              (click)="filterQuestions(1)">
              🟢 Level 1 — Easy
            </button>
            <button z-button
              [zType]="selectedDifficulty() === 2 ? 'default' : 'outline'"
              zSize="sm"
              (click)="filterQuestions(2)">
              🟡 Level 2 — Medium
            </button>
            <button z-button
              [zType]="selectedDifficulty() === 3 ? 'default' : 'outline'"
              zSize="sm"
              (click)="filterQuestions(3)">
              🔴 Level 3 — Hard
            </button>
          </div>

          @if (isLoadingQuizBank()) {
            <z-skeleton class="h-64 w-full" />
          } @else if (filteredQuestions().length > 0) {
            <div class="grid gap-4">
              @for (q of filteredQuestions(); track q.id) {
                <z-card class="p-5">
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-2">
                        <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold"
                          [class]="q.difficultyLevel === 3
                            ? 'bg-red-100 text-red-700'
                            : q.difficultyLevel === 2
                              ? 'bg-amber-100 text-amber-700'
                              : 'bg-emerald-100 text-emerald-700'">
                          {{ q.difficultyLevel === 1 ? '🟢 Easy' : q.difficultyLevel === 2 ? '🟡 Medium' : '🔴 Hard' }}
                        </span>
                        <span class="text-xs text-muted-foreground">Question #{{ q.id }}</span>
                      </div>
                      <p class="font-semibold text-base mb-3">{{ q.text }}</p>
                      <!-- Answers list -->
                      @if (q.answers && q.answers.length > 0) {
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          @for (ans of q.answers; track ans.id) {
                            <div class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm"
                              [class]="ans.isCorrect
                                ? 'bg-emerald-50 border border-emerald-300 text-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300'
                                : 'bg-slate-50 border border-slate-200 dark:bg-slate-800/50 dark:border-slate-700'">
                              <span>{{ ans.isCorrect ? '✅' : '⬜' }}</span>
                              <span>{{ ans.text }}</span>
                            </div>
                          }
                        </div>
                      } @else {
                        <p class="text-sm text-muted-foreground italic">No answers loaded.</p>
                      }
                    </div>
                  </div>
                </z-card>
              }
            </div>
          } @else {
            <div class="py-16 text-center text-muted-foreground">
              <p class="text-5xl mb-4">❓</p>
              <p class="text-lg font-medium">No questions found</p>
              <p class="text-sm mt-1">Try selecting a different difficulty level.</p>
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
export class DoctorDashboardComponent implements OnInit {
  private static readonly PAGE_STORAGE_KEY = 'tfk_doctor_current_page';
  private static readonly PAGE_QUERY_PARAM = 'page';

  currentPage = signal('Home');
  patients = signal<UserInfo[]>([]);
  patientStats = signal<Map<string, GameStatsResponse>>(new Map());
  /** keycloakId → average quiz risk score (0–100, higher = more risk) */
  quizScores = signal<Map<string, number>>(new Map());
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

  // KYC state
  kycStatus = signal<string>('none');
  kycChecking = signal(true);
  kycSkipping = signal(false);

  /** Use doctor's Keycloak ID to match medical-folder doctorId storage */
  doctorIdString = computed(() => {
    const keycloakId = this.currentDoctor()?.keycloakId;
    return keycloakId || null;
  });

  // ── Selected patient quiz data ──────────────────────────────
  selectedPatientAvgRisk = signal<number | null>(null);
  selectedPatientQuizCount = signal<number | null>(null);
  selectedPatientQuizHistory = signal<QuizDTO[]>([]);
  selectedPatientLastLevel = signal<number | null>(null);
  isLoadingQuizHistory = signal(false);

  // ── Quiz Bank data ────────────────────────────────────────────
  allQuestions = signal<QuestionDTO[]>([]);
  filteredQuestions = signal<QuestionDTO[]>([]);
  selectedDifficulty = signal<number>(0);
  isLoadingQuizBank = signal(false);

  // ── Quiz Overview computed counts ────────────────────────────
  quizHighRiskCount = signal<number>(0);
  quizModerateRiskCount = signal<number>(0);
  quizLowRiskCount = signal<number>(0);

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
    {
      label: 'Quiz',
      items: [
        { icon: 'brain', label: 'Quiz Overview', action: () => this.setPage('Quiz Overview') },
        { icon: 'book-open', label: 'Quiz Bank', action: () => this.loadQuizBank() },
      ],
    },
  ];

  doctorKeycloakId = '';

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
    private readonly routerClient: Router,
    private readonly quizService: QuizService,
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
          },
          error: err => {
            console.error('Failed to load doctor info', err);
            this.kycChecking.set(false);
          }
        });
      } else {
        this.kycChecking.set(false);
      }
    }
    this.loadPatients();
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

  /** Average quiz risk score (0–100) for a patient, or null if not loaded */
  getQuizScore(keycloakId: string): number | null {
    return this.quizScores().get(keycloakId) ?? null;
  }

  getLevelLabel(level: number): string {
    return level === 1 ? 'Easy' : level === 2 ? 'Medium' : level === 3 ? 'Hard' : '—';
  }

  viewPatientProgress(patient: UserInfo): void {
    this.selectedPatient.set(patient);
    this.selectedPatientStats.set(null);
    this.selectedPatientAvgRisk.set(null);
    this.selectedPatientQuizCount.set(null);
    this.selectedPatientLastLevel.set(null);
    this.selectedPatientQuizHistory.set([]);

    // Load game stats
    this.gameService.getPlayerStats(patient.keycloakId).subscribe({
      next: stats => this.selectedPatientStats.set(stats),
      error: () => this.selectedPatientStats.set(null),
    });

    // Load quiz data (needs neondb user id)
    this.isLoadingQuizHistory.set(true);
    this.userApiService.getUserByKeycloakId(patient.keycloakId).subscribe({
      next: userInfo => {
        if (userInfo?.id) {
          const neonId = userInfo.id;

          // Average risk score
          this.quizService.getAverageScoreByCaregiver(neonId).pipe(
            catchError(() => of(null))
          ).subscribe(avg => this.selectedPatientAvgRisk.set(avg));

          // Quiz count
          this.quizService.getQuizCountByCaregiver(neonId).pipe(
            catchError(() => of(null))
          ).subscribe(count => this.selectedPatientQuizCount.set(count));

          // Full quiz history (recent 20)
          this.quizService.getRecentQuizzesByCaregiver(neonId, 20).pipe(
            catchError(() => of([]))
          ).subscribe(quizzes => {
            this.selectedPatientQuizHistory.set(quizzes);
            // Extract last level reached from most recent quiz
            const lastQuiz = quizzes.find(q => q.levelReached != null);
            this.selectedPatientLastLevel.set(lastQuiz?.levelReached ?? null);
            this.isLoadingQuizHistory.set(false);
          });
        } else {
          this.isLoadingQuizHistory.set(false);
        }
      },
      error: () => this.isLoadingQuizHistory.set(false),
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

  /** Load all questions for the Quiz Bank page, then navigate to it */
  loadQuizBank(): void {
    this.setPage('Quiz Bank');
    if (this.allQuestions().length > 0) return; // already loaded
    this.isLoadingQuizBank.set(true);
    this.quizService.getAllQuestions().pipe(
      catchError(() => of([]))
    ).subscribe(questions => {
      this.allQuestions.set(questions);
      this.filteredQuestions.set(questions);
      this.isLoadingQuizBank.set(false);
    });
  }

  /** Filter questions by difficulty level (0 = all) */
  filterQuestions(level: number): void {
    this.selectedDifficulty.set(level);
    if (level === 0) {
      this.filteredQuestions.set(this.allQuestions());
    } else {
      this.filteredQuestions.set(this.allQuestions().filter(q => q.difficultyLevel === level));
    }
  }

  private loadPatients(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.userApiService.getUsersByRole('patient').subscribe({
      next: patients => {
        this.patients.set(patients);
        this.isLoading.set(false);

        for (const patient of patients) {
          // 1. Load game stats
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

          // 2. Load quiz risk score (needs neondb user id)
          this.userApiService.getUserByKeycloakId(patient.keycloakId).subscribe({
            next: userInfo => {
              if (userInfo?.id) {
                this.quizService.getAverageScoreByCaregiver(userInfo.id).subscribe({
                  next: avg => {
                    if (avg !== null && avg !== undefined) {
                      const map = new Map(this.quizScores());
                      map.set(patient.keycloakId, avg);
                      this.quizScores.set(map);
                      this.computeRiskCounts();
                    }
                  },
                });
              }
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

  private computeRiskCounts(): void {
    const scores = Array.from(this.quizScores().values());
    this.quizHighRiskCount.set(scores.filter(s => s >= 60).length);
    this.quizModerateRiskCount.set(scores.filter(s => s >= 30 && s < 60).length);
    this.quizLowRiskCount.set(scores.filter(s => s < 30).length);
  }
}
