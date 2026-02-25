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
import { PrescriptionService } from '@/core/services/prescription.service';
import { CarePlanService } from '@/core/services/care-plan.service';
import { UserApiService } from '@/core/services/user-api.service';
import { CustomGameService, type CustomGameResponse } from '@/core/services/custom-game.service';
import { DailyLogStateService } from '@/core/services/daily-log-state.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { type CarePlanResponseDTO } from '@/core/models/care-plan.model';
import { type IntakeStatus, type MedicationIntakeLogResponse } from '@/core/models/daily-monitoring.model';
import { AuthService } from '@/core/auth';
import { ThemeService } from '@/core/services/theme.service';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';

function nowTime(): string {
  const d = new Date();
  return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
}

@Component({
  selector: 'app-patient-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, CarePlanListComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef            = inject(DestroyRef);
  private readonly gameService           = inject(GameService);
  private readonly movieGameService      = inject(MovieGameService);
  private readonly prescriptionService   = inject(PrescriptionService);
  private readonly carePlanService       = inject(CarePlanService);
  private readonly userApiService        = inject(UserApiService);
  private readonly customGameService     = inject(CustomGameService);
  private readonly router                = inject(Router);
  private readonly authService           = inject(AuthService);
  readonly themeService                  = inject(ThemeService);

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
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  carePlans = signal<CarePlanResponseDTO[]>([]);

  // ── Loading flags ──────────────────────────────────────────────────────────
  isLoadingGames        = signal(false);
  isLoadingMovieGames   = signal(false);
  isLoadingStats        = signal(false);
  isLoadingCustomGames  = signal<boolean>(false);
  isLoadingPrescriptions = signal(false);
  isLoadingCarePlans    = signal<boolean>(false);

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
    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
    this.loadCustomGames();
    this.loadPrescriptions();
    this.loadCarePlans();
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

  /**
   * Mark all medications as taken at once.
   */
  markAllTaken(): void {
    const logId = this.logState.currentLogId;
    if (!logId || this.updatingMedId() !== null) return;

    const untaken = this.todayMedications().filter(m => m.status !== 'PRIS');
    if (untaken.length === 0) return;

    this.updatingMedId.set(-1); // -1 = bulk updating
    let remaining = untaken.length;
    let hasError = false;

    for (const med of untaken) {
      const now = new Date();
      const takenAt = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

      this.logState.toggleMedication(logId, med, 'PRIS', takenAt)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (result) => {
            remaining--;
            if (!result) hasError = true;
            if (remaining === 0) {
              this.updatingMedId.set(null);
              this.showMedToast(
                hasError ? 'Certains médicaments n\'ont pas pu être mis à jour' : '🎉 Tous les médicaments marqués comme pris !',
                hasError ? 'error' : 'success'
              );
            }
          },
          error: () => {
            remaining--;
            hasError = true;
            if (remaining === 0) {
              this.updatingMedId.set(null);
              this.showMedToast('Erreur de mise à jour', 'error');
            }
          },
        });
    }
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

  private loadPrescriptions(): void {
    if (!this.keycloakId) return;
    this.isLoadingPrescriptions.set(true);
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(u => this.userNeonDbId.set(u.id)),
        switchMap(u => this.prescriptionService.getPrescriptionsByPatient(u.id.toString())),
        tap(p => this.prescriptions.set(p)),
        catchError(() => of([])),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe();
  }

  private loadCarePlans(): void {
    if (!this.keycloakId) return;

    this.isLoadingCarePlans.set(true);

    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        switchMap(userInfo => {
          const neonDbId = userInfo.id.toString();
          return this.carePlanService.getCarePlansByPatient(neonDbId);
        }),
        tap(plans => {
          this.carePlans.set(plans);
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load care plans', err);
          return of([]);
        }),
        finalize(() => this.isLoadingCarePlans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
