import { Component, OnInit, signal, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { GameService, type GameResponse, type GameStatsResponse } from '@/core/services/game.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { UserApiService } from '@/core/services/user-api.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, GuessPlaceComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-b from-blue-50 to-white dark:from-slate-900 dark:to-slate-800">
      <!-- Top bar -->
      <header class="sticky top-0 z-10 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b border-slate-200 dark:border-slate-700 px-4 py-3 sm:px-6">
        <div class="max-w-2xl mx-auto flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-2xl">🧠</span>
            <span class="text-xl font-bold text-slate-800 dark:text-white">Tfakkarni</span>
          </div>
          <div class="flex items-center gap-2">
            <button
              (click)="switchToHelper.emit()"
              class="text-xs px-3 py-1.5 rounded-full bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-600 transition-colors"
            >
              Switch to Helper
            </button>
            <button
              (click)="logout()"
              class="text-xs px-3 py-1.5 rounded-full bg-red-50 dark:bg-red-900/30 text-red-500 hover:bg-red-100 dark:hover:bg-red-900/50 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main class="max-w-2xl mx-auto px-4 sm:px-6 py-6 pb-28">
        @switch (currentPage()) {
          @case ('Home') {
            <!-- Greeting -->
            <div class="text-center mb-8 pt-4">
              <p class="text-4xl mb-3">👋</p>
              <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
                Hello!
              </h1>
              <p class="text-lg text-slate-500 dark:text-slate-400">
                What would you like to do today?
              </p>
            </div>

            <!-- Main actions - big touch targets -->
            <div class="space-y-4 mb-8">
              <button
                (click)="setPage('Play Games')"
                class="w-full flex items-center gap-5 p-6 sm:p-8 rounded-2xl bg-blue-500 hover:bg-blue-600 active:scale-[0.98] text-white shadow-lg shadow-blue-500/25 transition-all"
              >
                <span class="text-4xl sm:text-5xl">🎮</span>
                <div class="text-left">
                  <p class="text-xl sm:text-2xl font-bold">Play Games</p>
                  <p class="text-blue-100 text-sm sm:text-base">Exercise your memory</p>
                </div>
              </button>

              <button
                (click)="setPage('My Scores')"
                class="w-full flex items-center gap-5 p-6 sm:p-8 rounded-2xl bg-amber-500 hover:bg-amber-600 active:scale-[0.98] text-white shadow-lg shadow-amber-500/25 transition-all"
              >
                <span class="text-4xl sm:text-5xl">🏆</span>
                <div class="text-left">
                  <p class="text-xl sm:text-2xl font-bold">My Scores</p>
                  <p class="text-amber-100 text-sm sm:text-base">See how well you're doing</p>
                </div>
              </button>

              <button
                (click)="setPage('Guess Place')"
                class="w-full flex items-center gap-5 p-6 sm:p-8 rounded-2xl bg-green-500 hover:bg-green-600 active:scale-[0.98] text-white shadow-lg shadow-green-500/25 transition-all"
              >
                <span class="text-4xl sm:text-5xl">📍</span>
                <div class="text-left">
                  <p class="text-xl sm:text-2xl font-bold">Guess the Place</p>
                  <p class="text-green-100 text-sm sm:text-base">Recognize familiar locations</p>
                </div>
              </button>

              <button
                (click)="setPage('My Prescriptions')"
                class="w-full flex items-center gap-5 p-6 sm:p-8 rounded-2xl bg-purple-500 hover:bg-purple-600 active:scale-[0.98] text-white shadow-lg shadow-purple-500/25 transition-all"
              >
                <span class="text-4xl sm:text-5xl">💊</span>
                <div class="text-left">
                  <p class="text-xl sm:text-2xl font-bold">My Prescriptions</p>
                  <p class="text-purple-100 text-sm sm:text-base">View your medications</p>
                </div>
              </button>
            </div>

            <!-- Quick stats -->
            @if (stats(); as s) {
              <div class="grid grid-cols-2 gap-3">
                <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm">
                  <p class="text-3xl sm:text-4xl font-bold text-blue-600 dark:text-blue-400">{{ s.totalGamesPlayed }}</p>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Games Played</p>
                </div>
                <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm">
                  <p class="text-3xl sm:text-4xl font-bold text-amber-600 dark:text-amber-400">{{ s.bestScore }}</p>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Best Score</p>
                </div>
              </div>
            }
          }

          @case ('Play Games') {
            <!-- Back button -->
            <button
              (click)="setPage('Home')"
              class="flex items-center gap-2 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white mb-6 text-lg transition-colors"
            >
              <span class="text-2xl">←</span>
              <span>Back</span>
            </button>

            <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
              🎮 Pick a Game
            </h1>
            <p class="text-lg text-slate-500 dark:text-slate-400 mb-6">Tap a game to start playing</p>

            @if (playableGames().length > 0) {
              <div class="space-y-4">
                @for (game of playableGames(); track game.id) {
                  <button
                    (click)="playGame(game.id)"
                    class="w-full flex items-center gap-4 p-5 sm:p-6 rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 hover:border-blue-400 dark:hover:border-blue-500 active:scale-[0.98] shadow-sm hover:shadow-md transition-all text-left"
                  >
                    <span class="text-3xl sm:text-4xl flex-shrink-0">🧩</span>
                    <div class="flex-1 min-w-0">
                      <p class="text-lg sm:text-xl font-bold text-slate-800 dark:text-white truncate">{{ game.title }}</p>
                      <p class="text-sm text-slate-500 dark:text-slate-400 truncate">{{ game.description }}</p>
                      <p class="text-xs text-slate-400 dark:text-slate-500 mt-1">{{ game.imageCount }} photos</p>
                    </div>
                    <span class="text-3xl text-blue-500 flex-shrink-0">▶</span>
                  </button>
                }
              </div>
            } @else {
              <div class="text-center py-16">
                <p class="text-5xl mb-4">😊</p>
                <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No games yet</h2>
                <p class="text-slate-500 dark:text-slate-400 text-lg">
                  Ask your helper to create some games for you!
                </p>
              </div>
            }
          }

          @case ('My Scores') {
            <!-- Back button -->
            <button
              (click)="setPage('Home')"
              class="flex items-center gap-2 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white mb-6 text-lg transition-colors"
            >
              <span class="text-2xl">←</span>
              <span>Back</span>
            </button>

            <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-6">
              🏆 My Scores
            </h1>

            @if (stats(); as s) {
              <div class="space-y-4">
                <!-- Big score display -->
                <div class="rounded-2xl bg-gradient-to-br from-amber-400 to-orange-500 p-6 sm:p-8 text-center text-white shadow-lg">
                  <p class="text-lg opacity-80 mb-1">Best Score</p>
                  <p class="text-5xl sm:text-6xl font-bold">{{ s.bestScore }}</p>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm">
                    <p class="text-3xl font-bold text-blue-600 dark:text-blue-400">{{ s.totalGamesPlayed }}</p>
                    <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Games Played</p>
                  </div>
                  <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm">
                    <p class="text-3xl font-bold text-green-600 dark:text-green-400">{{ s.averageScore | number:'1.0-0' }}%</p>
                    <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Average</p>
                  </div>
                </div>

                <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm">
                  <p class="text-3xl font-bold text-purple-600 dark:text-purple-400">{{ s.totalAttempts }}</p>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Total Attempts</p>
                </div>

                <!-- Progress bar -->
                @if (s.totalAttempts > 0) {
                  <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 shadow-sm">
                    <p class="text-base font-semibold text-slate-700 dark:text-slate-300 mb-3">Average Accuracy</p>
                    <div class="relative w-full h-6 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
                      <div
                        class="h-full bg-gradient-to-r from-green-400 to-emerald-500 rounded-full transition-all duration-500"
                        [style.width.%]="s.averageScore"
                      ></div>
                    </div>
                    <p class="text-right text-sm text-slate-500 dark:text-slate-400 mt-1">{{ s.averageScore | number:'1.0-0' }}%</p>
                  </div>
                }
              </div>
            } @else {
              <div class="text-center py-16">
                <p class="text-5xl mb-4">🎯</p>
                <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No scores yet</h2>
                <p class="text-slate-500 dark:text-slate-400 text-lg">
                  Play some games to see your scores here!
                </p>
                <button
                  (click)="setPage('Play Games')"
                  class="mt-6 px-8 py-4 rounded-2xl bg-blue-500 hover:bg-blue-600 text-white text-lg font-bold shadow-lg transition-all"
                >
                  🎮 Play Now
                </button>
              </div>
            }
          }

          @case ('Guess Place') {
            <app-guess-place
              [keycloakId]="keycloakId"
              (goBack)="setPage('Home')"
            />
          }

          @case ('My Prescriptions') {
            <!-- Back button -->
            <button
              (click)="setPage('Home')"
              class="flex items-center gap-2 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white mb-6 text-lg transition-colors"
            >
              <span class="text-2xl">←</span>
              <span>Back</span>
            </button>

            <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
              💊 My Prescriptions
            </h1>
            <p class="text-lg text-slate-500 dark:text-slate-400 mb-6">Your current medications</p>

            @if (isLoadingPrescriptions()) {
              <div class="space-y-4">
                @for (i of [1,2,3]; track i) {
                  <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 animate-pulse">
                    <div class="h-6 bg-slate-200 dark:bg-slate-700 rounded w-3/4 mb-3"></div>
                    <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-1/2 mb-2"></div>
                    <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-2/3"></div>
                  </div>
                }
              </div>
            } @else if (prescriptions().length > 0) {
              <div class="space-y-4">
                @for (prescription of prescriptions(); track prescription.id) {
                  <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 sm:p-6 shadow-sm">
                    <div class="flex items-start justify-between mb-4">
                      <div>
                        <p class="text-xs text-slate-400 dark:text-slate-500 mb-1">Prescription #{{ prescription.id }}</p>
                        <p class="text-sm text-slate-500 dark:text-slate-400">{{ prescription.createdAt | date:'medium' }}</p>
                      </div>
                      <span class="text-2xl">📋</span>
                    </div>
                    
                    @if (prescription.medications && prescription.medications.length > 0) {
                      <div class="space-y-3">
                        @for (med of prescription.medications; track med.id) {
                          <div class="border-l-4 border-purple-500 pl-4 py-2">
                            <p class="font-bold text-lg text-slate-800 dark:text-white mb-1">{{ med.medicationName }}</p>
                            <div class="space-y-1 text-sm text-slate-600 dark:text-slate-400">
                              <p><span class="font-semibold">Dosage:</span> {{ med.dosage }}</p>
                              <p><span class="font-semibold">Frequency:</span> {{ med.frequency }}</p>
                              <p><span class="font-semibold">Duration:</span> {{ med.duration }}</p>
                              @if (med.instructions) {
                                <p class="mt-2 p-2 bg-blue-50 dark:bg-blue-900/20 rounded">
                                  <span class="font-semibold">Instructions:</span> {{ med.instructions }}
                                </p>
                              }
                            </div>
                          </div>
                        }
                      </div>
                    } @else {
                      <p class="text-slate-500 dark:text-slate-400 italic">No medications listed</p>
                    }
                  </div>
                }
              </div>
            } @else {
              <div class="text-center py-16">
                <p class="text-5xl mb-4">💊</p>
                <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No prescriptions yet</h2>
                <p class="text-slate-500 dark:text-slate-400 text-lg">
                  Your doctor will add prescriptions here when needed.
                </p>
              </div>
            }
          }
        }
      </main>

      <!-- Bottom nav - fixed, big touch targets -->
      <nav class="fixed bottom-0 left-0 right-0 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm border-t border-slate-200 dark:border-slate-700 z-10">
        <div class="max-w-2xl mx-auto flex">
          <button
            (click)="setPage('Home')"
            class="flex-1 flex flex-col items-center gap-1 py-3 transition-colors"
            [class]="currentPage() === 'Home'
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-slate-400 dark:text-slate-500'"
          >
            <span class="text-2xl">🏠</span>
            <span class="text-xs font-medium">Home</span>
          </button>
          <button
            (click)="setPage('Play Games')"
            class="flex-1 flex flex-col items-center gap-1 py-3 transition-colors"
            [class]="currentPage() === 'Play Games'
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-slate-400 dark:text-slate-500'"
          >
            <span class="text-2xl">🎮</span>
            <span class="text-xs font-medium">Play</span>
          </button>
          <button
            (click)="setPage('My Scores')"
            class="flex-1 flex flex-col items-center gap-1 py-3 transition-colors"
            [class]="currentPage() === 'My Scores'
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-slate-400 dark:text-slate-500'"
          >
            <span class="text-2xl">🏆</span>
            <span class="text-xs font-medium">Scores</span>
          </button>
          <button
            (click)="setPage('Guess Place')"
            class="flex-1 flex flex-col items-center gap-1 py-3 transition-colors"
            [class]="currentPage() === 'Guess Place'
              ? 'text-green-600 dark:text-green-400'
              : 'text-slate-400 dark:text-slate-500'"
          >
            <span class="text-2xl">📍</span>
            <span class="text-xs font-medium">Places</span>
          </button>
          <button
            (click)="setPage('My Prescriptions')"
            class="flex-1 flex flex-col items-center gap-1 py-3 transition-colors"
            [class]="currentPage() === 'My Prescriptions'
              ? 'text-purple-600 dark:text-purple-400'
              : 'text-slate-400 dark:text-slate-500'"
          >
            <span class="text-2xl">💊</span>
            <span class="text-xs font-medium">Rx</span>
          </button>
        </div>
      </nav>
    </div>
  `,
})
export class PatientViewComponent implements OnInit {
  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  currentPage = signal('Home');
  games = signal<GameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  isLoadingPrescriptions = signal(false);
  userNeonDbId = signal<number | null>(null);

  constructor(
    private readonly gameService: GameService,
    private readonly prescriptionService: PrescriptionService,
    private readonly userApiService: UserApiService,
    private readonly router: Router,
    private readonly authService: AuthService,
  ) { }

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadData();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  playableGames(): GameResponse[] {
    return this.games().filter(g => g.imageCount >= 2);
  }

  playGame(gameId: number): void {
    this.router.navigate(['/patient/play', gameId]);
  }

  logout(): void {
    this.authService.logout();
  }

  loadData(): void {
    if (!this.keycloakId) return;

    this.gameService.getPatientGames(this.keycloakId).subscribe({
      next: games => this.games.set(games),
      error: err => console.error('Failed to load games', err),
    });

    this.gameService.getPlayerStats(this.keycloakId).subscribe({
      next: stats => this.stats.set(stats),
      error: err => console.error('Failed to load stats', err),
    });

    this.loadPrescriptions();
  }

  loadPrescriptions(): void {
    if (!this.keycloakId) {
      console.warn('[PRESCRIPTIONS] No keycloakId provided, skipping load');
      return;
    }

    console.log('[PRESCRIPTIONS] Step 1: Fetching user info for keycloakId:', this.keycloakId);
    this.isLoadingPrescriptions.set(true);

    // First, get the user's NeonDB ID from their keycloakId
    this.userApiService.getUserByKeycloakId(this.keycloakId).subscribe({
      next: userInfo => {
        console.log('[PRESCRIPTIONS] Step 2: User info retrieved:', userInfo);
        console.log('[PRESCRIPTIONS] NeonDB ID:', userInfo.id);
        this.userNeonDbId.set(userInfo.id);

        // Now fetch prescriptions using the NeonDB ID
        const neonDbId = userInfo.id.toString();
        console.log('[PRESCRIPTIONS] Step 3: Loading prescriptions for NeonDB ID:', neonDbId);
        console.log('[PRESCRIPTIONS] API URL will be: /api/prescriptions/patient/' + neonDbId);

        this.prescriptionService.getPrescriptionsByPatient(neonDbId).subscribe({
          next: prescriptions => {
            console.log('[PRESCRIPTIONS] Step 4: Successfully loaded prescriptions:', prescriptions);
            console.log('[PRESCRIPTIONS] Number of prescriptions:', prescriptions.length);
            this.prescriptions.set(prescriptions);
            this.isLoadingPrescriptions.set(false);
          },
          error: err => {
            console.error('[PRESCRIPTIONS] Step 4 ERROR: Failed to load prescriptions');
            console.error('[PRESCRIPTIONS] Error status:', err?.status);
            console.error('[PRESCRIPTIONS] Error message:', err?.message);
            console.error('[PRESCRIPTIONS] Error details:', err);
            this.isLoadingPrescriptions.set(false);
          },
        });
      },
      error: err => {
        console.error('[PRESCRIPTIONS] Step 2 ERROR: Failed to fetch user info');
        console.error('[PRESCRIPTIONS] Error status:', err?.status);
        console.error('[PRESCRIPTIONS] Error message:', err?.message);
        console.error('[PRESCRIPTIONS] Error details:', err);
        this.isLoadingPrescriptions.set(false);
      },
    });
  }
}
