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
import { MovieGameService, type MovieGameResponse } from '@/core/services/movie-game.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { CarePlanService } from '@/core/services/care-plan.service';
import { UserApiService } from '@/core/services/user-api.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { type CarePlanResponseDTO } from '@/core/models/care-plan.model';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { PatientDossierViewComponent } from '../patient-dossier-view/patient-dossier-view.component';

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, CarePlanListComponent, PatientDossierViewComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly movieGameService = inject(MovieGameService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly carePlanService = inject(CarePlanService);
  private readonly userApiService = inject(UserApiService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // State Signals
  currentPage = signal<string>('Home');
  games = signal<GameResponse[]>([]);
  movieGames = signal<MovieGameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  carePlans = signal<CarePlanResponseDTO[]>([]);

  // Loading State Signals
  isLoadingGames = signal<boolean>(false);
  isLoadingMovieGames = signal<boolean>(false);
  isLoadingStats = signal<boolean>(false);
  isLoadingPrescriptions = signal<boolean>(false);
  isLoadingCarePlans = signal<boolean>(false);

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

  playMovieGame(gameId: number): void {
    this.router.navigate(['/patient/play-movie', gameId]);
  }

  logout(): void {
    this.authService.logout();
  }

  loadData(): void {
    if (!this.keycloakId) return;

    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
    this.loadPrescriptions();
    this.loadCarePlans();
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

  private loadMovieGames(): void {
    this.isLoadingMovieGames.set(true);
    this.movieGameService.getPatientMovieGames(this.keycloakId)
      .pipe(
        tap(games => this.movieGames.set(games)),
        catchError(err => {
          console.error('[PatientView] Failed to load movie games', err);
          return of([]);
        }),
        finalize(() => this.isLoadingMovieGames.set(false)),
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

  private loadCarePlans(): void {
    if (!this.keycloakId) {
      return;
    }

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
