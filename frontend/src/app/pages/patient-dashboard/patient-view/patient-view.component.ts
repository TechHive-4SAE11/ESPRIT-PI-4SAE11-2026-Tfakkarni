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
import { AudioGameService, type SpeechLanguage } from '@/core/services/audio-game.service';
import { ThemeService } from '@/core/services/theme.service';
import { StatisticsService } from '@/core/services/statistics.service';
import type { StreakResponse } from '@/core/models/statistics.model';
import { NotificationService, type MedicationNotification, type NotificationResponse } from '@/core/services/notification.service';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { MedicationManagementComponent } from '@/pages/medications/medications.component';
import { PatientDossierViewComponent } from '../patient-dossier-view/patient-dossier-view.component';

function nowTime(): string {
  const d = new Date();
  return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
}

@Component({
  selector: 'app-patient-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, CarePlanListComponent, MedicationManagementComponent, PatientDossierViewComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef            = inject(DestroyRef);
  private readonly gameService           = inject(GameService);
  private readonly movieGameService      = inject(MovieGameService);
  private readonly userApiService        = inject(UserApiService);
  private readonly customGameService     = inject(CustomGameService);
  private readonly router                = inject(Router);
  private readonly authService           = inject(AuthService);
  private readonly audioGameService      = inject(AudioGameService);
  private readonly statisticsService     = inject(StatisticsService);
  readonly themeService                  = inject(ThemeService);
  private readonly notificationService   = inject(NotificationService);

  // ── Service partagé (source unique de vérité pour les médicaments) ─────────
  readonly logState = inject(DailyLogStateService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // ── Expose Math for template expressions ───────────────────────────────────
  readonly Math = Math;

  // ── Navigation ─────────────────────────────────────────────────────────────
  currentPage = signal<string>('Home');

  // ── Game data ──────────────────────────────────────────────────────────────
  games = signal<GameResponse[]>([]);
  movieGames = signal<MovieGameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  customGames = signal<CustomGameResponse[]>([]);

  // ── Loading flags ──────────────────────────────────────────────────────────
  isLoadingGames        = signal(false);
  isLoadingMovieGames   = signal(false);
  isLoadingStats        = signal(false);
  isLoadingCustomGames  = signal<boolean>(false);
  isLoadingStreak       = signal(false);

  // ── Win Streak (Duolingo-style) ────────────────────────────────────────────
  streak = signal<StreakResponse | null>(null);

  // ── Médicaments — lus depuis le service partagé ────────────────────────────
  /** Proxy computed vers les signaux du service partagé */
  readonly todayMedications = computed(() => this.logState.todayMedications());
  readonly isLoadingMeds = computed(() => this.logState.loading());
  readonly medsTakenCount = computed(() => this.logState.medsTakenCount());
  readonly medsTotal = computed(() => this.logState.medsTotal());
  readonly medsProgressPercent = computed(() => this.logState.medsProgressPercent());

  // ── Toast ──────────────────────────────────────────────────────────────────
  updatingMedId = signal<number | null>(null);
  medToastMsg = signal('');
  medToastType = signal<'success' | 'error'>('success');

  // ── User info ──────────────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

  // ── Language preference for TTS ────────────────────────────────────────────
  selectedLanguage = signal<SpeechLanguage>(this.audioGameService.getPreferredLanguage());

  // ── Notifications ──────────────────────────────────────────────────────────
  notifications = signal<MedicationNotification[]>([]);
  unreadNotifCount = signal(0);
  isNotifPanelOpen = signal(false);
  isLoadingNotifs = signal(false);

  // ── Computed ───────────────────────────────────────────────────────────────
  playableGames = computed(() => this.games().filter(g => g.imageCount >= 2));

  ngOnInit(): void {
    if (this.keycloakId) { this.loadData(); }
  }

  setPage(page: string): void { this.currentPage.set(page); }
  playGame(id: number): void { this.router.navigate(['/patient/play', id]); }
  playMovieGame(id: number): void { this.router.navigate(['/patient/play-movie', id]); }
  logout(): void { this.authService.logout(); }

  /** Switch TTS language and persist the preference */
  setLanguage(lang: SpeechLanguage): void {
    this.selectedLanguage.set(lang);
    this.audioGameService.setPreferredLanguage(lang);
  }

  loadData(): void {
    if (!this.keycloakId) return;
    this.loadUserNeonDbId();
    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
    this.loadCustomGames();
    this.loadStreak();
    this.loadAndCacheUserGender();
    // Notifications are loaded inside loadUserNeonDbId() after neon ID is available
    // Médicaments : charger via le service partagé (évite un double-fetch si déjà chargé)
    this.logState.loadTodayLog(this.keycloakId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  // ── Notification Methods ────────────────────────────────────────────────────

  loadNotifications(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.isLoadingNotifs.set(true);
    this.notificationService.getNotifications(neonId.toString())
      .pipe(
        tap((res: NotificationResponse) => {
          this.notifications.set(res.notifications || []);
          this.unreadNotifCount.set(res.unreadCount);
        }),
        catchError(() => {
          console.warn('[PatientView] Failed to load notifications');
          return of(null);
        }),
        finalize(() => this.isLoadingNotifs.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  toggleNotifPanel(): void {
    const isOpen = !this.isNotifPanelOpen();
    this.isNotifPanelOpen.set(isOpen);
    if (isOpen && this.notifications().length === 0) {
      this.loadNotifications();
    }
  }

  closeNotifPanel(): void {
    this.isNotifPanelOpen.set(false);
  }

  markNotifAsRead(notif: MedicationNotification): void {
    const neonId = this.userNeonDbId();
    if (notif.read || !neonId) return;
    this.notificationService.markAsRead(neonId.toString(), notif.id)
      .pipe(
        tap(() => {
          this.notifications.update(list =>
            list.map(n => n.id === notif.id ? { ...n, read: true } : n)
          );
          this.unreadNotifCount.update(c => Math.max(0, c - 1));
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  markAllNotifsAsRead(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.notificationService.markAllAsRead(neonId.toString())
      .pipe(
        tap(() => {
          this.notifications.update(list =>
            list.map(n => ({ ...n, read: true }))
          );
          this.unreadNotifCount.set(0);
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  refreshNotifications(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.isLoadingNotifs.set(true);
    this.notificationService.refreshNotifications(neonId.toString())
      .pipe(
        tap((res: NotificationResponse) => {
          this.notifications.set(res.notifications || []);
          this.unreadNotifCount.set(res.unreadCount);
        }),
        catchError(() => of(null)),
        finalize(() => this.isLoadingNotifs.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
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

  private loadStreak(): void {
    this.isLoadingStreak.set(true);
    this.statisticsService.getStreak(this.keycloakId)
      .pipe(
        tap(s => this.streak.set(s)),
        catchError(err => {
          this.streak.set(null);
          this.showMedToast('Impossible de charger la série', 'error');
          return of(null);
        }),
        finalize(() => this.isLoadingStreak.set(false)),
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
          // Load notifications now that we have the neon DB ID
          this.loadNotifications();
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  /** Fetch user info and cache gender in localStorage for TTS usage */
  private loadAndCacheUserGender(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(user => {
          if (user.gender) {
            this.audioGameService.setCachedGender(user.gender);
          }
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
