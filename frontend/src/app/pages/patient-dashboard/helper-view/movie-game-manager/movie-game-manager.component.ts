import {
  Component,
  Input,
  Output,
  EventEmitter,
  signal,
  inject,
  DestroyRef,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged, switchMap, catchError, of, tap, finalize } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';

import {
  MovieGameService,
  type TmdbMovie,
  type MovieGameResponse,
  type EditMovieItemEntry,
} from '@/core/services/movie-game.service';
import { gameTitleSchema, gameDescriptionSchema, getFieldErrors } from '@/core/validation/game-schemas';
import { z } from 'zod';

@Component({
  selector: 'app-movie-game-manager',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
  ],
  template: `
    @switch (view()) {

    @case ('list') {
    <!-- Movie Character Quiz List -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-2">
        <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-1" />
          Back
        </button>
        <h2 class="text-2xl font-bold">Movie Character Quiz</h2>
      </div>
      <button z-button (click)="startCreate()">
        <z-icon zType="plus" class="mr-2" />
        New Quiz
      </button>
    </div>

    @if (successMessage()) {
    <div class="mb-4 p-4 rounded-md bg-green-500/10 border border-green-500 text-green-700 text-sm">
      {{ successMessage() }}
    </div>
    }

    @if (movieGames().length > 0) {
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      @for (game of movieGames(); track game.id) {
      <z-card class="p-6">
        <div class="flex items-start justify-between mb-3">
          <div>
            <h3 class="font-semibold text-lg">{{ game.title }}</h3>
            <p class="text-sm text-muted-foreground">{{ game.description }}</p>
          </div>
          <z-badge zType="secondary">{{ game.movieCount }} movies</z-badge>
        </div>
        <p class="text-xs text-muted-foreground mb-4">Created {{ game.createdAt | date:'mediumDate' }}</p>
        <div class="flex gap-2">
          <button z-button zSize="sm" (click)="startEdit(game.id)">
            <z-icon zType="settings" class="mr-1" />
            Edit
          </button>
          <button z-button zType="destructive" zSize="sm" (click)="deleteGame(game.id)">
            <z-icon zType="trash-2" class="mr-1" />
            Delete
          </button>
        </div>
      </z-card>
      }
    </div>
    } @else {
    <z-card class="p-12 text-center">
      <z-icon zType="play" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
      <h3 class="font-semibold mb-2">No movie character quizzes yet</h3>
      <p class="text-muted-foreground mb-4">Create a quiz where the patient names characters from movie posters!</p>
      <button z-button (click)="startCreate()">
        <z-icon zType="plus" class="mr-2" />
        Create Quiz
      </button>
    </z-card>
    }
    }

    @case ('create') {
    <!-- Create / Edit Movie Character Quiz -->
    <div class="flex items-center gap-2 mb-6">
      <button z-button zType="ghost" zSize="sm" (click)="resetAndGoToList()">
        <z-icon zType="arrow-left" class="mr-1" />
        Back
      </button>
      <h2 class="text-2xl font-bold">{{ editingGameId() ? 'Edit' : 'Create' }} Movie Character Quiz</h2>
    </div>

    <!-- Quiz Details -->
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Quiz Details</h3>
      <div class="space-y-4">
        <div>
          <label class="text-sm font-medium mb-1 block">Title <span class="text-muted-foreground font-normal">(max 20)</span></label>
          <input
            class="w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [class]="validationErrors()['title'] ? 'border-red-500' : 'border-border'"
            maxlength="20"
            [value]="gameTitle()"
            (input)="gameTitle.set($any($event.target).value)"
            placeholder="e.g., Classic Movie Characters" />
          @if (validationErrors()['title']) {
            <p class="text-xs text-red-500 mt-1">{{ validationErrors()['title'] }}</p>
          }
        </div>
        <div>
          <label class="text-sm font-medium mb-1 block">Description <span class="text-muted-foreground font-normal">(max 100)</span></label>
          <input
            class="w-full px-3 py-2 border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [class]="validationErrors()['description'] ? 'border-red-500' : 'border-border'"
            maxlength="100"
            [value]="gameDescription()"
            (input)="gameDescription.set($any($event.target).value)"
            placeholder="e.g., Name a character from these famous movies" />
          @if (validationErrors()['description']) {
            <p class="text-xs text-red-500 mt-1">{{ validationErrors()['description'] }}</p>
          }
        </div>
      </div>
    </z-card>

    <!-- Movie Search -->
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Search & Add Movies</h3>
      <p class="text-sm text-muted-foreground mb-4">
        Search for a movie, then enter a <strong>character name</strong> from that movie as the answer.
      </p>

      <div class="relative mb-4">
        <div class="flex items-center gap-2">
          <z-icon zType="search" class="text-muted-foreground" />
          <input
            class="flex-1 px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="searchQuery()"
            (input)="onSearchInput($any($event.target).value)"
            placeholder="Type a movie name... (e.g., The Lion King)" />
        </div>

        @if (isSearching()) {
        <div class="absolute top-full left-0 right-0 mt-1 z-10">
          <z-card class="p-4">
            <div class="flex items-center justify-center py-2">
              <z-icon zType="loader-2" class="animate-spin mr-2" />
              <span class="text-sm text-muted-foreground">Searching...</span>
            </div>
          </z-card>
        </div>
        }

        @if (searchResults().length > 0 && !isSearching()) {
        <div class="absolute top-full left-0 right-0 mt-1 z-10 max-h-80 overflow-y-auto">
          <z-card class="p-2">
            @for (movie of searchResults(); track movie.id) {
            <button
              class="w-full flex items-center gap-3 p-2 rounded-md hover:bg-muted transition-colors text-left"
              (click)="selectMovie(movie)">
              <img
                [src]="getPosterUrl(movie.poster_path, 'w92')"
                [alt]="movie.title"
                class="w-10 h-14 object-cover rounded"
                loading="lazy" />
              <div class="flex-1 min-w-0">
                <p class="font-medium text-sm truncate">{{ movie.title }}</p>
                <p class="text-xs text-muted-foreground">{{ movie.release_date | slice:0:4 }}</p>
              </div>
              <z-icon zType="plus" class="text-primary flex-shrink-0" />
            </button>
            }
          </z-card>
        </div>
        }
      </div>
    </z-card>

    <!-- Selected Movie (for setting the character answer) -->
    @if (pendingMovie()) {
    <z-card class="p-6 mb-6 border-primary border-2">
      <h3 class="font-semibold mb-4">Set the Character Name</h3>
      <div class="flex gap-4">
        <img
          [src]="getPosterUrl(pendingMovie()!.poster_path, 'w185')"
          [alt]="pendingMovie()!.title"
          class="w-24 h-36 object-cover rounded shadow" />
        <div class="flex-1">
          <p class="font-semibold text-lg">{{ pendingMovie()!.title }}</p>
          <p class="text-sm text-muted-foreground mb-3">{{ pendingMovie()!.release_date }}</p>
          <label class="text-sm font-medium mb-1 block">Character Name (answer) <span class="text-muted-foreground font-normal">(max 20)</span></label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary mb-3"
            maxlength="20"
            [value]="pendingAnswer()"
            (input)="pendingAnswer.set($any($event.target).value)"
            placeholder="e.g., Simba, Jack Sparrow, Elsa..." />
          <p class="text-xs text-muted-foreground mb-3">
            Tip: Enter a famous character from this movie. The patient will see the poster and try to name this character.
          </p>
          <div class="flex gap-2">
            <button z-button [disabled]="!pendingAnswer().trim()" (click)="addPendingMovie()">
              <z-icon zType="check" class="mr-1" />
              Add Movie
            </button>
            <button z-button zType="outline" (click)="cancelPending()">Cancel</button>
          </div>
        </div>
      </div>
    </z-card>
    }

    <!-- Added Movies List -->
    @if (addedMovies().length > 0) {
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Movies in Quiz ({{ addedMovies().length }})</h3>
      <div class="space-y-3">
        @for (movie of addedMovies(); track $index; let i = $index) {
        <div class="flex items-center gap-3 p-3 rounded-lg border border-border">
          <img
            [src]="getPosterUrl(movie.posterPath, 'w92')"
            [alt]="movie.originalTitle"
            class="w-10 h-14 object-cover rounded" />
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm truncate">{{ movie.originalTitle }}</p>
            <p class="text-xs text-muted-foreground">Character: <span class="font-semibold text-primary">{{ movie.correctAnswer }}</span></p>
          </div>
          <button z-button zType="ghost" zSize="sm" class="text-destructive" (click)="removeMovie(i)">
            <z-icon zType="x" />
          </button>
        </div>
        }
      </div>
    </z-card>
    }

    <!-- Validation & Submit -->
    @if (errorMessage()) {
    <div class="mb-4 p-4 rounded-md bg-destructive/10 border border-destructive text-destructive text-sm">
      {{ errorMessage() }}
    </div>
    }

    @if (!canSave() && !saving()) {
    <p class="text-sm text-muted-foreground mb-3">
      @if (gameTitle().trim().length === 0) {
      <span class="text-destructive">&#x2022; Enter a quiz title</span><br />
      }
      @if (addedMovies().length < 2) {
      <span class="text-destructive">&#x2022; Add at least 2 movies (currently {{ addedMovies().length }})</span>
      }
    </p>
    }

    <div class="flex gap-3">
      <button z-button [disabled]="!canSave()" (click)="saveGame()">
        @if (saving()) {
        <z-icon zType="loader-2" class="mr-2 animate-spin" />
        {{ editingGameId() ? 'Saving...' : 'Creating...' }}
        } @else {
        <z-icon zType="check" class="mr-2" />
        {{ editingGameId() ? 'Save Changes' : 'Create Quiz' }}
        }
      </button>
      <button z-button zType="outline" (click)="resetAndGoToList()">Cancel</button>
    </div>
    }
    }
  `,
})
export class MovieGameManagerComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly movieGameService = inject(MovieGameService);
  private readonly keycloakService = inject(KeycloakService);
  private readonly alertDialog = inject(ZardAlertDialogService);

  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  // View mode
  view = signal<'list' | 'create'>('list');

  // List state
  movieGames = signal<MovieGameResponse[]>([]);

  // Search state
  searchQuery = signal<string>('');
  searchResults = signal<TmdbMovie[]>([]);
  isSearching = signal<boolean>(false);
  private readonly searchSubject = new Subject<string>();

  // Create/edit form state
  editingGameId = signal<number | null>(null);
  gameTitle = signal<string>('');
  gameDescription = signal<string>('');
  addedMovies = signal<EditMovieItemEntry[]>([]);
  pendingMovie = signal<TmdbMovie | null>(null);
  pendingAnswer = signal<string>('');
  saving = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  validationErrors = signal<Record<string, string>>({});

  ngOnInit(): void {
    this.loadGames();
    this.setupSearch();
  }

  private setupSearch(): void {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.isSearching.set(true)),
        switchMap(query => {
          if (!query.trim()) {
            return of([]);
          }
          return this.movieGameService.searchMovies(query).pipe(
            catchError(() => of([]))
          );
        }),
        tap(results => {
          this.searchResults.set(results);
          this.isSearching.set(false);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  selectMovie(movie: TmdbMovie): void {
    this.pendingMovie.set(movie);
    this.pendingAnswer.set('');
    this.searchResults.set([]);
    this.searchQuery.set('');
  }

  addPendingMovie(): void {
    const movie = this.pendingMovie();
    const answer = this.pendingAnswer().trim();
    if (!movie || !answer) return;

    const item: EditMovieItemEntry = {
      id: null,
      tmdbId: movie.id,
      originalTitle: movie.original_title || movie.title,
      posterPath: movie.poster_path,
      releaseDate: movie.release_date || '',
      correctAnswer: answer,
    };

    this.addedMovies.update(list => [...list, item]);
    this.cancelPending();
  }

  cancelPending(): void {
    this.pendingMovie.set(null);
    this.pendingAnswer.set('');
  }

  removeMovie(index: number): void {
    this.addedMovies.update(list => list.filter((_, i) => i !== index));
  }

  getPosterUrl(posterPath: string, size: string = 'w500'): string {
    return this.movieGameService.getTmdbPosterUrl(posterPath, size);
  }

  canSave(): boolean {
    return (
      this.gameTitle().trim().length > 0 &&
      this.addedMovies().length >= 2 &&
      !this.saving()
    );
  }

  // ─── Create ────────────────────────────────────────────────

  startCreate(): void {
    this.resetForm();
    this.editingGameId.set(null);
    this.view.set('create');
  }

  // ─── Edit ──────────────────────────────────────────────────

  startEdit(gameId: number): void {
    this.resetForm();
    this.editingGameId.set(gameId);
    this.view.set('create');

    // Load existing game details
    this.movieGameService
      .getMovieGameDetail(gameId)
      .pipe(
        tap(detail => {
          this.gameTitle.set(detail.title);
          this.gameDescription.set(detail.description || '');
          this.addedMovies.set(
            detail.movies.map(m => ({
              id: m.id,
              tmdbId: m.tmdbId,
              originalTitle: m.originalTitle,
              posterPath: m.posterPath,
              releaseDate: m.releaseDate || '',
              correctAnswer: m.correctAnswer,
            }))
          );
        }),
        catchError(err => {
          console.error('[MovieGameManager] Failed to load game detail', err);
          this.errorMessage.set('Failed to load game for editing.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Save (create or edit) ─────────────────────────────────

  async saveGame(): Promise<void> {
    if (!this.canSave()) return;

    // Zod validation
    const detailsSchema = z.object({ title: gameTitleSchema, description: gameDescriptionSchema });
    const valResult = detailsSchema.safeParse({ title: this.gameTitle().trim(), description: this.gameDescription().trim() });
    if (!valResult.success) {
      this.validationErrors.set(getFieldErrors(valResult));
      return;
    }
    this.validationErrors.set({});

    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    try {
      const kc = this.keycloakService.getKeycloakInstance();
      if (kc?.refreshToken) {
        await this.keycloakService.updateToken(30);
      }
    } catch (e) {
      console.warn('[MovieGameManager] Token refresh warning:', e);
    }

    const payload = {
      title: this.gameTitle(),
      description: this.gameDescription(),
      movies: this.addedMovies(),
    };

    const request$ = this.editingGameId()
      ? this.movieGameService.editMovieGame(this.editingGameId()!, payload)
      : this.movieGameService.createMovieGame(this.keycloakId, {
          title: payload.title,
          description: payload.description,
          movies: payload.movies.map(m => ({
            tmdbId: m.tmdbId,
            originalTitle: m.originalTitle,
            posterPath: m.posterPath,
            releaseDate: m.releaseDate,
            correctAnswer: m.correctAnswer,
          })),
        });

    request$
      .pipe(
        tap(() => {
          this.successMessage.set(
            this.editingGameId() ? 'Quiz updated successfully!' : 'Quiz created successfully!'
          );
          this.resetForm();
          this.loadGames();
          this.view.set('list');
        }),
        catchError(err => {
          console.error('[MovieGameManager] Save failed', err);
          const status = err?.status;
          let msg = 'Failed to save quiz: ';
          if (status === 401 || status === 403) {
            msg += 'Authentication error. Please log out and log back in.';
          } else if (status === 0) {
            msg += 'Could not reach the server. Check if the API gateway is running.';
          } else {
            msg += err?.error?.error || err?.message || 'Unknown error';
          }
          this.errorMessage.set(msg);
          return of(null);
        }),
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  deleteGame(gameId: number): void {
    const ref = this.alertDialog.confirm({
      zTitle: 'Delete Quiz',
      zDescription: 'Are you sure you want to delete this movie character quiz? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.movieGameService
          .deleteMovieGame(gameId)
          .pipe(
            tap(() => this.loadGames()),
            catchError(err => {
              console.error('[MovieGameManager] Failed to delete', err);
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
        ref.close();
      },
    });
  }

  resetAndGoToList(): void {
    this.resetForm();
    this.view.set('list');
  }

  private resetForm(): void {
    this.editingGameId.set(null);
    this.gameTitle.set('');
    this.gameDescription.set('');
    this.addedMovies.set([]);
    this.pendingMovie.set(null);
    this.pendingAnswer.set('');
    this.searchQuery.set('');
    this.searchResults.set([]);
    this.errorMessage.set('');
    this.validationErrors.set({});
  }

  private loadGames(): void {
    if (!this.keycloakId) return;

    this.movieGameService
      .getPatientMovieGames(this.keycloakId)
      .pipe(
        tap(games => this.movieGames.set(games)),
        catchError(err => {
          console.error('[MovieGameManager] Failed to load games', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
