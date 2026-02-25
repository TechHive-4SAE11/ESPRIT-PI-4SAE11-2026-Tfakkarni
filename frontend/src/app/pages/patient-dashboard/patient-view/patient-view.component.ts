import {
  Component, OnInit, signal, Input, Output, EventEmitter,
  inject, DestroyRef, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { GameService, type GameResponse, type GameStatsResponse } from '@/core/services/game.service';
import { MovieGameService, type MovieGameResponse } from '@/core/services/movie-game.service';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';
import { CustomGameService, type CustomGameResponse } from '@/core/services/custom-game.service';
import { DailyLogStateService } from '@/core/services/daily-log-state.service';
import { type IntakeStatus, type MedicationIntakeLogResponse } from '@/core/models/daily-monitoring.model';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { MedicationManagementComponent } from '@/pages/medications/medications.component';

function nowTime(): string {
  const d = new Date();
  return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
}

@Component({
  selector: 'app-patient-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, CarePlanListComponent, MedicationManagementComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly movieGameService = inject(MovieGameService);
  private readonly userApiService = inject(UserApiService);
  private readonly customGameService = inject(CustomGameService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  // ── Service partagé (source unique de vérité pour les médicaments) ─────────
  readonly logState = inject(DailyLogStateService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // ── Navigation ─────────────────────────────────────────────────────────────
  currentPage = signal<string>('Home');

  // ── Game data ──────────────────────────────────────────────────────────────
  games      = signal<GameResponse[]>([]);
  movieGames = signal<MovieGameResponse[]>([]);
  stats      = signal<GameStatsResponse | null>(null);
  customGames = signal<CustomGameResponse[]>([]);

  // ── Loading flags ──────────────────────────────────────────────────────────
  isLoadingGames        = signal(false);
  isLoadingMovieGames   = signal(false);
  isLoadingStats        = signal(false);
  isLoadingCustomGames  = signal<boolean>(false);

  // ── Médicaments — lus depuis le service partagé ────────────────────────────
  /** Proxy computed vers les signaux du service partagé */
  readonly todayMedications    = computed(() => this.logState.todayMedications());
  readonly isLoadingMeds       = computed(() => this.logState.loading());
  readonly medsTakenCount      = computed(() => this.logState.medsTakenCount());
  readonly medsTotal           = computed(() => this.logState.medsTotal());
  readonly medsProgressPercent = computed(() => this.logState.medsProgressPercent());

  // ── Toast ──────────────────────────────────────────────────────────────────
  updatingMedId  = signal<number | null>(null);
  medToastMsg    = signal('');
  medToastType   = signal<'success' | 'error'>('success');

  // ── User info ──────────────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

  // ── Computed ───────────────────────────────────────────────────────────────
  playableGames = computed(() => this.games().filter(g => g.imageCount >= 2));

  ngOnInit(): void {
    if (this.keycloakId) { this.loadData(); }
  }

  setPage(page: string): void { this.currentPage.set(page); }
  playGame(id: number):       void { this.router.navigate(['/patient/play', id]); }
  playMovieGame(id: number):  void { this.router.navigate(['/patient/play-movie', id]); }
  logout(): void { this.authService.logout(); }

  loadData(): void {
    if (!this.keycloakId) return;
    this.loadUserNeonDbId();
    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
    this.loadCustomGames();
    // Médicaments : charger via le service partagé (évite un double-fetch si déjà chargé)
    this.logState.loadTodayLog(this.keycloakId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  // ── Médicaments ────────────────────────────────────────────────────────────

  loadTodayMedications(): void {
    this.logState.refresh(this.keycloakId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  toggleMedication(med: MedicationIntakeLogResponse): void {
    const logId = this.logState.currentLogId;
    if (!logId || this.updatingMedId() !== null) return;

    const newStatus: IntakeStatus = med.status === 'PRIS' ? 'OUBLIE' : 'PRIS';
    this.updatingMedId.set(med.id);

    this.logState.toggleMedication(logId, med, newStatus, newStatus === 'PRIS' ? nowTime() : undefined)
      .pipe(
        tap(result => {
          if (result) {
            this.showMedToast(
              newStatus === 'PRIS' ? 'Médicament marqué comme pris ✓' : 'Médicament marqué comme non pris',
              newStatus === 'PRIS' ? 'success' : 'error'
            );
          } else {
            this.showMedToast('Erreur de mise à jour', 'error');
          }
        }),
        finalize(() => this.updatingMedId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private showMedToast(msg: string, type: 'success' | 'error'): void {
    this.medToastMsg.set(msg);
    this.medToastType.set(type);
    setTimeout(() => this.medToastMsg.set(''), 3000);
  }

  medStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PRIS: 'Pris', OUBLIE: 'Non pris', REFUSE: 'Refusé', EN_RETARD: 'En retard',
    };
    return map[status] ?? status;
  }

  // ── Custom Games ───────────────────────────────────────────────────────────

  playCustomGame(gameId: number): void {
    this.router.navigate(['/patient/play-memory', gameId]);
  }

  playRandomMix(): void {
    this.router.navigate(['/patient/play-memory', 'random']);
  }

  // ── Private loaders ────────────────────────────────────────────────────────

  private loadGames(): void {
    this.isLoadingGames.set(true);
    this.gameService.getPatientGames(this.keycloakId)
      .pipe(
        tap(g => this.games.set(g)),
        catchError(() => of([])),
        finalize(() => this.isLoadingGames.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe();
  }

  private loadMovieGames(): void {
    this.isLoadingMovieGames.set(true);
    this.movieGameService.getPatientMovieGames(this.keycloakId)
      .pipe(
        tap(g => this.movieGames.set(g)),
        catchError(() => of([])),
        finalize(() => this.isLoadingMovieGames.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe();
  }

  private loadStats(): void {
    this.isLoadingStats.set(true);
    this.gameService.getPlayerStats(this.keycloakId)
      .pipe(
        tap(s => this.stats.set(s)),
        catchError(() => of(null)),
        finalize(() => this.isLoadingStats.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe();
  }

  private loadCustomGames(): void {
    this.isLoadingCustomGames.set(true);
    this.customGameService.getGames(this.keycloakId)
      .pipe(
        tap(games => this.customGames.set(games)),
        catchError(err => {
          console.error('[PatientView] Failed to load custom games', err);
          return of([]);
        }),
        finalize(() => this.isLoadingCustomGames.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadUserNeonDbId(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[PatientView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
          this.currentUser.set(userInfo);
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
