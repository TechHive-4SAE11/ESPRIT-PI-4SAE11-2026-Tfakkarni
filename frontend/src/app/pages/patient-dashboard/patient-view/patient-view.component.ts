import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { GameService, type GameResponse, type GameStatsResponse } from '@/core/services/game.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { UserApiService } from '@/core/services/user-api.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { PatientQuizComponent } from '../patient-quiz/patient-quiz.component';
import { PatientEquipmentComponent } from '../patient-equipment/patient-equipment.component';

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, PatientQuizComponent, PatientEquipmentComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly userApiService = inject(UserApiService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // State Signals
  currentPage = signal<string>('Home');
  games = signal<GameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);

  // Loading State Signals
  isLoadingGames = signal<boolean>(false);
  isLoadingStats = signal<boolean>(false);
  isLoadingPrescriptions = signal<boolean>(false);

  // User Info
  userNeonDbId = signal<number | null>(null);

  // Computed Values
  playableGames = computed(() => this.games().filter(g => g.imageCount >= 2));

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadData();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  playGame(gameId: number): void {
    this.router.navigate(['/patient/play', gameId]);
  }

  logout(): void {
    this.authService.logout();
  }

  loadData(): void {
    if (!this.keycloakId) return;

    this.loadGames();
    this.loadStats();
    this.loadPrescriptions();
  }

  private loadGames(): void {
    this.isLoadingGames.set(true);
    this.gameService.getPatientGames(this.keycloakId)
      .pipe(
        tap(games => this.games.set(games)),
        catchError(err => {
          console.error('[PatientView] Failed to load games', err);
          return of([]);
        }),
        finalize(() => this.isLoadingGames.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadStats(): void {
    this.isLoadingStats.set(true);
    this.gameService.getPlayerStats(this.keycloakId)
      .pipe(
        tap(stats => this.stats.set(stats)),
        catchError(err => {
          console.error('[PatientView] Failed to load stats', err);
          return of(null);
        }),
        finalize(() => this.isLoadingStats.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadPrescriptions(): void {
    if (!this.keycloakId) {
      console.warn('[PatientView] No keycloakId provided, skipping prescription load');
      return;
    }

    console.log('[PatientView] Loading prescriptions flow started for:', this.keycloakId);
    this.isLoadingPrescriptions.set(true);

    // Chain: Get User Info -> Extract ID -> Get Prescriptions
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[PatientView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
        }),
        switchMap(userInfo => {
          const neonDbId = userInfo.id.toString();
          console.log('[PatientView] Fetching prescriptions for DB ID:', neonDbId);
          return this.prescriptionService.getPrescriptionsByPatient(neonDbId);
        }),
        tap(prescriptions => {
          console.log('[PatientView] Prescriptions loaded:', prescriptions.length);
          this.prescriptions.set(prescriptions);
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load prescriptions chain', err);
          return of([]);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
