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
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';
import { AuthService } from '@/core/auth';
import { GuessPlaceComponent } from './guess-place/guess-place.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { MedicationManagementComponent } from '@/pages/medications/medications.component';

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, GuessPlaceComponent, PrescriptionListComponent, CarePlanListComponent, MedicationManagementComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly movieGameService = inject(MovieGameService);
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

  // Loading State Signals
  isLoadingGames = signal<boolean>(false);
  isLoadingMovieGames = signal<boolean>(false);
  isLoadingStats = signal<boolean>(false);

  // User Info
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

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

    this.loadUserNeonDbId();
    this.loadGames();
    this.loadMovieGames();
    this.loadStats();
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
}
