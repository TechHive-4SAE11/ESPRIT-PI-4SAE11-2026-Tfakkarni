import {
  Component, OnInit, signal, Input, Output, EventEmitter,
  inject, DestroyRef, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { GameService, type GameResponse, type GameStatsResponse } from '@/core/services/game.service';
import { MovieGameService, type MovieGameResponse } from '@/core/services/movie-game.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { UserApiService } from '@/core/services/user-api.service';
import { DailyMonitoringService } from '@/core/services/daily-monitoring.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { type MedicationIntakeLogResponse } from '@/core/models/daily-monitoring.model';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';

function todayIso(): string { return new Date().toISOString().split('T')[0]; }

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef            = inject(DestroyRef);
  private readonly gameService           = inject(GameService);
  private readonly movieGameService      = inject(MovieGameService);
  private readonly prescriptionService   = inject(PrescriptionService);
  private readonly userApiService        = inject(UserApiService);
  private readonly dailyMonitoringService = inject(DailyMonitoringService);
  private readonly router                = inject(Router);
  private readonly authService           = inject(AuthService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // ── Navigation ─────────────────────────────────────────────────────────
  currentPage = signal<string>('Home');

  // ── Game data ──────────────────────────────────────────────────────────
  games       = signal<GameResponse[]>([]);
  movieGames  = signal<MovieGameResponse[]>([]);
  stats       = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);

  // ── Loading flags ──────────────────────────────────────────────────────
  isLoadingGames        = signal(false);
  isLoadingMovieGames   = signal(false);
  isLoadingStats        = signal(false);
  isLoadingPrescriptions = signal(false);

  // ── Medication tracker ─────────────────────────────────────────────────
  todayMedications    = signal<MedicationIntakeLogResponse[]>([]);
  isLoadingMeds       = signal(false);
  updatingMedId       = signal<number | null>(null);
  dailyLogId          = signal<number | null>(null);
  medToastMsg         = signal('');
  medToastType        = signal<'success' | 'error'>('success');

  // ── User info ──────────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);

  // ── Computed ───────────────────────────────────────────────────────────
  playableGames = computed(() => this.games().filter(g => g.imageCount >= 2));
  medsTakenCount = computed(() => this.todayMedications().filter(m => m.status === 'PRIS').length);
  medsTotal = computed(() => this.todayMedications().length);
  medsProgressPercent = computed(() => {
    const t = this.medsTotal(); return t === 0 ? 0 : Math.round((this.medsTakenCount() / t) * 100);
  });

  ngOnInit(): void {
    if (this.keycloakId) { this.loadData(); }
  }

  setPage(page: string): void { this.currentPage.set(page); }
  playGame(id: number):  void { this.router.navigate(['/patient/play', id]); }
  playMovieGame(id: number): void { this.router.navigate(['/patient/play-movie', id]); }
  logout(): void { this.authService.logout(); }

  loadData(): void {
    if (!this.keycloakId) return;
    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
    this.loadPrescriptions();
    this.loadTodayMedications();
  }

  // ── Medication tracker ─────────────────────────────────────────────────

  loadTodayMedications(): void {
    this.isLoadingMeds.set(true);
    this.dailyMonitoringService.getOrCreateLogForDate(this.keycloakId, todayIso())
      .pipe(
        tap(log => {
          this.dailyLogId.set(log.id);
          this.todayMedications.set(log.medicationIntakes ?? []);
        }),
        catchError(() => of(null)),
        finalize(() => this.isLoadingMeds.set(false)),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
  }

  toggleMedication(med: MedicationIntakeLogResponse): void {
    const logId = this.dailyLogId();
    if (!logId || this.updatingMedId() !== null) return;

    const newStatus = med.status === 'PRIS' ? 'OUBLIE' : 'PRIS';
    this.updatingMedId.set(med.id);

    const dto = { medicationId: med.medicationId, takenAt: med.takenAt, status: newStatus, notes: med.notes };

    this.dailyMonitoringService.updateMedicationIntake(logId, med.id, dto as any)
      .pipe(
        tap(() => {
          this.todayMedications.update(list =>
            list.map(m => m.id === med.id ? { ...m, status: newStatus as any, takenAt: newStatus === 'PRIS' ? this.nowTime() : m.takenAt } : m)
          );
          this.showMedToast(newStatus === 'PRIS' ? 'Médicament marqué comme pris ✓' : 'Médicament marqué comme non pris', newStatus === 'PRIS' ? 'success' : 'error');
        }),
        catchError(() => { this.showMedToast('Erreur de mise à jour', 'error'); return of(null); }),
        finalize(() => this.updatingMedId.set(null)),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
  }

  private nowTime(): string {
    const d = new Date();
    return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
  }

  private showMedToast(msg: string, type: 'success' | 'error'): void {
    this.medToastMsg.set(msg);
    this.medToastType.set(type);
    setTimeout(() => this.medToastMsg.set(''), 3000);
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  medStatusLabel(status: string): string {
    const map: Record<string, string> = { PRIS:'Pris', OUBLIE:'Non pris', REFUSE:'Refusé', EN_RETARD:'En retard' };
    return map[status] ?? status;
  }

  // ── Private loaders ────────────────────────────────────────────────────

  private loadGames(): void {
    this.isLoadingGames.set(true);
    this.gameService.getPatientGames(this.keycloakId)
      .pipe(tap(g => this.games.set(g)), catchError(() => of([])), finalize(() => this.isLoadingGames.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  private loadMovieGames(): void {
    this.isLoadingMovieGames.set(true);
    this.movieGameService.getPatientMovieGames(this.keycloakId)
      .pipe(tap(g => this.movieGames.set(g)), catchError(() => of([])), finalize(() => this.isLoadingMovieGames.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  private loadStats(): void {
    this.isLoadingStats.set(true);
    this.gameService.getPlayerStats(this.keycloakId)
      .pipe(tap(s => this.stats.set(s)), catchError(() => of(null)), finalize(() => this.isLoadingStats.set(false)), takeUntilDestroyed(this.destroyRef))
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
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
  }
}
