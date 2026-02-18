import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { AddPlaceComponent } from './add-place/add-place.component';
import { MovieGameManagerComponent } from './movie-game-manager/movie-game-manager.component';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';

import {
  GameService,
  type GameResponse,
  type GameStatsResponse,
  type GameDetailResponse,
  type EditImageEntry,
} from '@/core/services/game.service';
import { PrescriptionService } from '@/core/services/prescription.service';
import { PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { SuiviQuotidienComponent } from './suivi-quotidien/suivi-quotidien.component';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-helper-view',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardProgressBarComponent,
    AddPlaceComponent,
    MovieGameManagerComponent,
    ZardTableImports,
    SuiviQuotidienComponent,
    PrescriptionListComponent,
  ],
  template: `
    @switch (currentPage()) {
      @case ('Home') {
        <h2 class="text-2xl font-bold mb-6">Helper Dashboard</h2>
        <p class="text-muted-foreground mb-6">Manage games, track progress, and support the patient's memory exercises.</p>

        <div class="grid gap-4 md:grid-cols-3 mb-8">
          <z-card class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-muted-foreground">Games Created</p>
                <p class="text-3xl font-bold">{{ stats()?.totalGamesCreated ?? 0 }}</p>
              </div>
              <z-icon zType="gamepad-2" class="text-primary h-8 w-8" />
            </div>
          </z-card>
          <z-card class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-muted-foreground">Games Played</p>
                <p class="text-3xl font-bold">{{ stats()?.totalGamesPlayed ?? 0 }}</p>
              </div>
              <z-icon zType="target" class="text-primary h-8 w-8" />
            </div>
          </z-card>
          <z-card class="p-6">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-muted-foreground">Best Score</p>
                <p class="text-3xl font-bold">{{ stats()?.bestScore ?? 0 }}</p>
              </div>
              <z-icon zType="trophy" class="text-primary h-8 w-8" />
            </div>
          </z-card>
        </div>

        @if ((stats()?.totalAttempts ?? 0) > 0) {
          <z-card class="p-6 mb-6">
            <h3 class="text-lg font-semibold mb-4">Patient Performance</h3>
            <div class="flex items-center gap-4">
              <span class="text-sm text-muted-foreground">Average Score</span>
              <z-progress-bar [progress]="stats()?.averageScore ?? 0" class="flex-1 h-3" />
              <span class="text-sm font-semibold">{{ (stats()?.averageScore ?? 0) | number:'1.0-0' }}%</span>
            </div>
          </z-card>
        }

        <div class="grid gap-4 md:grid-cols-2">
          <z-card class="p-6 cursor-pointer hover:border-primary transition-colors" (click)="setPage('Create Game')">
            <div class="flex items-center gap-3">
              <z-icon zType="plus-circle" class="text-primary h-10 w-10" />
              <div>
                <h3 class="font-semibold">Create New Game</h3>
                <p class="text-sm text-muted-foreground">Upload photos of relatives & places</p>
              </div>
            </div>
          </z-card>
          <z-card class="p-6 cursor-pointer hover:border-primary transition-colors" (click)="setPage('My Games')">
            <div class="flex items-center gap-3">
              <z-icon zType="gamepad-2" class="text-primary h-10 w-10" />
              <div>
                <h3 class="font-semibold">Manage Games</h3>
                <p class="text-sm text-muted-foreground">View and manage created games</p>
              </div>
            </div>
          </z-card>
          <z-card class="p-6 cursor-pointer hover:border-primary transition-colors" (click)="setPage('Suivi Quotidien')">
            <div class="flex items-center gap-3">
              <z-icon zType="file" class="text-primary h-10 w-10" />
              <div>
                <h3 class="font-semibold">Suivi Quotidien</h3>
                <p class="text-sm text-muted-foreground">Alimentation, médicaments, activités & incidents</p>
              </div>
            </div>
          </z-card>
          <z-card class="p-6 cursor-pointer hover:border-primary transition-colors" (click)="setPage('Places')">
            <div class="flex items-center gap-3">
              <z-icon zType="map-pin" class="text-primary h-10 w-10" />
              <div>
                <h3 class="font-semibold">Guess the Place</h3>
                <p class="text-sm text-muted-foreground">Manage location-based memory places</p>
              </div>
            </div>
          </z-card>
        </div>
      }

      @case ('My Games') {
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-2xl font-bold">Manage Games</h2>
          <button z-button (click)="setPage('Create Game')">
            <z-icon zType="plus" class="mr-2" />
            New Game
          </button>
        </div>

        @if (games().length > 0) {
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            @for (game of games(); track game.id) {
              <z-card class="p-6">
                <div class="flex items-start justify-between mb-3">
                  <div>
                    <h3 class="font-semibold text-lg">{{ game.title }}</h3>
                    <p class="text-sm text-muted-foreground">{{ game.description }}</p>
                  </div>
                  <z-badge zType="secondary">{{ game.imageCount }} images</z-badge>
                </div>
                <p class="text-xs text-muted-foreground mb-4">Created {{ game.createdAt | date:'mediumDate' }}</p>
                <div class="flex gap-2">
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
            <z-icon zType="gamepad-2" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <h3 class="font-semibold mb-2">No games yet</h3>
            <p class="text-muted-foreground mb-4">Create the first memory game for the patient!</p>
            <button z-button (click)="setPage('Create Game')">
              <z-icon zType="plus" class="mr-2" />
              Create Game
            </button>
          </z-card>
        }
      }

      @case ('Prescriptions') {
        <h2 class="text-2xl font-bold mb-6">My Prescriptions</h2>
        
        @if (prescriptions().length > 0) {
          <table z-table>
            <thead z-table-header>
              <tr z-table-row>
                <th z-table-head>Date</th>
                <th z-table-head>Medications</th>
              </tr>
            </thead>
            <tbody z-table-body>
              @for (prescription of prescriptions(); track prescription.id) {
                <tr z-table-row>
                  <td z-table-cell>{{ prescription.createdAt | date:'mediumDate' }}</td>
                  <td z-table-cell>
                    <div class="space-y-2">
                      @for (med of prescription.medications; track med.id) {
                        <div class="text-sm border-b pb-1 last:border-0 last:pb-0">
                          <span class="font-semibold">{{ med.medicationName }}</span>
                          <span class="text-muted-foreground mx-1">-</span>
                          <span>{{ med.dosage }}</span>
                          <div class="text-xs text-muted-foreground">
                            {{ med.frequency }} for {{ med.duration }}
                            @if (med.instructions) {
                              <span class="block italic text-xs mt-0.5">Note: {{ med.instructions }}</span>
                            }
                          </div>
                        </div>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <z-card class="p-12 text-center">
             <z-icon zType="pill" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
             <h3 class="font-semibold mb-2">No prescriptions found</h3>
             <p class="text-muted-foreground">You don't have any prescriptions yet.</p>
          </z-card>
        }
      }

      @case ('Create Game') {
        <div class="flex items-center gap-2 mb-6">
          <button z-button zType="ghost" zSize="sm" (click)="setPage('My Games')">
            <z-icon zType="arrow-left" class="mr-1" />
            Back
          </button>
          <h2 class="text-2xl font-bold">Create New Game</h2>
        </div>

        <z-card class="p-6 mb-6">
          <h3 class="font-semibold mb-4">Game Details</h3>
          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium mb-1 block">Title</label>
              <input
                class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                [value]="newGameTitle()"
                (input)="newGameTitle.set($any($event.target).value)"
                placeholder="e.g., Family Members"
              />
            </div>
            <div>
              <label class="text-sm font-medium mb-1 block">Description</label>
              <input
                class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                [value]="newGameDescription()"
                (input)="newGameDescription.set($any($event.target).value)"
                placeholder="e.g., Photos of my family members"
              />
            </div>
          </div>
        </z-card>

        <z-card class="p-6 mb-6">
          <h3 class="font-semibold mb-4">Upload Images</h3>
          <p class="text-sm text-muted-foreground mb-4">Add photos and give each one a name (this will be the answer).</p>

          <div class="border-2 border-dashed border-border rounded-lg p-8 text-center mb-4">
            <z-icon zType="upload" class="mx-auto h-8 w-8 text-muted-foreground mb-2" />
            <p class="text-sm text-muted-foreground mb-2">Click to select images</p>
            <input
              type="file"
              accept="image/*"
              multiple
              class="block w-full text-sm text-foreground file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-primary file:text-primary-foreground hover:file:opacity-90"
              (change)="onFilesSelected($event)"
            />
          </div>

          @if (uploadedImages().length > 0) {
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              @for (img of uploadedImages(); track $index; let i = $index) {
                <div class="border border-border rounded-lg p-3">
                  <img [src]="img.preview" alt="Preview" class="w-full h-32 object-cover rounded mb-2" />
                  <input
                    class="w-full px-2 py-1 text-sm border border-border rounded bg-background text-foreground"
                    [value]="img.name"
                    (input)="updateImageName(i, $any($event.target).value)"
                    placeholder="Name (e.g., Grandma Sara)"
                  />
                  <button z-button zType="ghost" zSize="sm" class="mt-1 text-destructive w-full" (click)="removeImage(i)">
                    <z-icon zType="x" class="mr-1" />
                    Remove
                  </button>
                </div>
              }
            </div>
          }
        </z-card>

        @if (errorMessage()) {
          <div class="mb-4 p-4 rounded-md bg-destructive/10 border border-destructive text-destructive text-sm">
            {{ errorMessage() }}
          </div>
        }
        @if (successMessage()) {
          <div class="mb-4 p-4 rounded-md bg-green-500/10 border border-green-500 text-green-700 text-sm">
            {{ successMessage() }}
          </div>
        }

        @if (!canCreateGame() && !creating()) {
          <p class="text-sm text-muted-foreground mb-3">
            @if (newGameTitle().trim().length === 0) {
              <span class="text-destructive">&#x2022; Enter a game title</span><br/>
            }
            @if (uploadedImages().length < 2) {
              <span class="text-destructive">&#x2022; Upload at least 2 images (currently {{ uploadedImages().length }})</span>
            }
          </p>
        }

        <div class="flex gap-3">
          <button
            z-button
            [disabled]="!canCreateGame()"
            (click)="createGame()"
          >
            @if (creating()) {
              <z-icon zType="loader-2" class="mr-2 animate-spin" />
              Creating...
            } @else {
              <z-icon zType="check" class="mr-2" />
              Create Game
            }
          </button>
          <button z-button zType="outline" (click)="resetForm()">Cancel</button>
        </div>
      }

      @case ('Progress') {
        <h2 class="text-2xl font-bold mb-6">Patient Progress</h2>

        @if (stats(); as s) {
          <div class="grid gap-4 md:grid-cols-4 mb-8">
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Total Attempts</p>
              <p class="text-3xl font-bold">{{ s.totalAttempts }}</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Games Played</p>
              <p class="text-3xl font-bold">{{ s.totalGamesPlayed }}</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Average Score</p>
              <p class="text-3xl font-bold">{{ s.averageScore | number:'1.0-0' }}%</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Best Score</p>
              <p class="text-3xl font-bold">{{ s.bestScore }}</p>
            </z-card>
          </div>

          <z-card class="p-6">
            <h3 class="text-lg font-semibold mb-4">Performance</h3>
            <div class="space-y-4">
              <div>
                <div class="flex justify-between text-sm mb-1">
                  <span>Average Accuracy</span>
                  <span>{{ s.averageScore | number:'1.0-0' }}%</span>
                </div>
                <z-progress-bar [progress]="s.averageScore" />
              </div>
            </div>
          </z-card>
        } @else {
          <z-card class="p-12 text-center">
            <z-icon zType="bar-chart-3" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <h3 class="font-semibold mb-2">No progress yet</h3>
            <p class="text-muted-foreground">The patient hasn't played any games yet.</p>
          </z-card>
        }
      }

      @case ('Places') {
        <app-add-place
          [keycloakId]="keycloakId"
          (goBack)="setPage('Home')"
        />
      }

      @case ('Suivi Quotidien') {
        <app-suivi-quotidien
          [keycloakId]="keycloakId"
          (goBack)="setPage('Home')"
        />
      }
    }
  `,
  templateUrl: './helper-view.component.html',
})
export class HelperViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly keycloakService = inject(KeycloakService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly userApiService = inject(UserApiService);
  private readonly alertDialog = inject(ZardAlertDialogService);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // State Signals
  currentPage = signal<string>('Home');
  games = signal<GameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  userNeonDbId = signal<number | null>(null);

  // Loading Signals
  isLoadingPrescriptions = signal<boolean>(false);
  creating = signal<boolean>(false);

  // Form Signals (shared for create & edit)
  editingGameId = signal<number | null>(null);
  newGameTitle = signal<string>('');
  newGameDescription = signal<string>('');
  uploadedImages = signal<{ id?: number; name: string; base64: string; contentType: string; preview: string }[]>([]);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadData();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    this.pageChange.emit(page);
  }

  // ==================== Data Loading ====================

  loadData(): void {
    if (!this.keycloakId) return;

    this.loadGames();
    this.loadStats();
    this.loadPrescriptions();
  }

  private loadGames(): void {
    this.gameService.getPatientGames(this.keycloakId)
      .pipe(
        tap(games => this.games.set(games)),
        catchError(err => {
          console.error('[HelperView] Failed to load games', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadStats(): void {
    this.gameService.getPlayerStats(this.keycloakId)
      .pipe(
        tap(stats => this.stats.set(stats)),
        catchError(err => {
          console.error('[HelperView] Failed to load stats', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadPrescriptions(): void {
    if (!this.keycloakId) {
      console.warn('[HelperView] No keycloakId provided, skipping prescription load');
      return;
    }

    console.log('[HelperView] Loading prescriptions for:', this.keycloakId);
    this.isLoadingPrescriptions.set(true);

    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[HelperView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
        }),
        switchMap(userInfo => {
          const neonDbId = userInfo.id.toString();
          return this.prescriptionService.getPrescriptionsByPatient(neonDbId);
        }),
        tap(prescriptions => {
          console.log('[HelperView] Prescriptions loaded:', prescriptions.length);
          this.prescriptions.set(prescriptions);
        }),
        catchError(err => {
          console.error('[HelperView] Failed to load prescriptions', err);
          return of([]);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  deleteGame(gameId: number): void {
    const ref = this.alertDialog.confirm({
      zTitle: 'Delete Game',
      zDescription: 'Are you sure you want to delete this game? This action cannot be undone and all images will be permanently removed.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.gameService.deleteGame(gameId)
          .pipe(
            tap(() => this.loadData()),
            catchError(err => {
              console.error('[HelperView] Failed to delete game', err);
              return of(null);
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
        ref.close();
      },
    });
  }

  // ==================== Edit Game ====================

  startEdit(gameId: number): void {
    this.resetForm();
    this.editingGameId.set(gameId);
    this.currentPage.set('Create Game');

    this.gameService.getGameDetail(gameId)
      .pipe(
        tap((detail: GameDetailResponse) => {
          this.newGameTitle.set(detail.title);
          this.newGameDescription.set(detail.description || '');
          this.uploadedImages.set(
            detail.images.map(img => ({
              id: img.id,
              name: img.name,
              base64: img.imageBase64,
              contentType: img.contentType,
              preview: `data:${img.contentType};base64,${img.imageBase64}`,
            }))
          );
        }),
        catchError(err => {
          console.error('[HelperView] Failed to load game for editing', err);
          this.errorMessage.set('Failed to load game for editing.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ==================== Game Creation ====================

  canCreateGame(): boolean {
    return this.newGameTitle().trim().length > 0 &&
      this.uploadedImages().length >= 2 &&
      !this.creating();
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;

    for (const file of Array.from(input.files)) {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        const base64 = result.split(',')[1];
        this.uploadedImages.update(images => [
          ...images,
          {
            name: file.name.replace(/\.[^/.]+$/, ''),
            base64,
            contentType: file.type,
            preview: result,
          },
        ]);
      };
      reader.readAsDataURL(file);
    }
  }

  updateImageName(index: number, name: string): void {
    this.uploadedImages.update(images => {
      const updated = [...images];
      updated[index] = { ...updated[index], name };
      return updated;
    });
  }

  removeImage(index: number): void {
    this.uploadedImages.update(images => images.filter((_, i) => i !== index));
  }

  resetForm(): void {
    this.editingGameId.set(null);
    this.newGameTitle.set('');
    this.newGameDescription.set('');
    this.uploadedImages.set([]);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  async createGame(): Promise<void> {
    if (!this.canCreateGame()) return;

    this.creating.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    try {
      const kc = this.keycloakService.getKeycloakInstance();
      if (kc?.refreshToken) {
        await this.keycloakService.updateToken(30);
      }
    } catch (e) {
      console.warn('[HelperView] Token refresh warning:', e);
    }

    // ── Edit Mode ──
    if (this.editingGameId()) {
      const images: EditImageEntry[] = this.uploadedImages().map(img => ({
        id: img.id ?? null,
        name: img.name,
        imageBase64: img.id ? undefined : img.base64,
        contentType: img.id ? undefined : img.contentType,
      }));

      this.gameService.editGame(this.editingGameId()!, {
        title: this.newGameTitle(),
        description: this.newGameDescription(),
        images,
      }).pipe(
        tap(() => {
          this.successMessage.set('Game updated successfully!');
          this.resetForm();
          this.loadData();
          this.setPage('My Games');
        }),
        catchError(err => {
          console.error('[HelperView] Game edit failed', err);
          this.errorMessage.set('Failed to update game: ' + (err?.error?.error || err?.message || 'Unknown error'));
          return of(null);
        }),
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
      return;
    }

    // ── Create Mode ──
    // 1. Create Game
    this.gameService.createGame(this.keycloakId, {
      title: this.newGameTitle(),
      description: this.newGameDescription(),
    }).pipe(
      // 2. Upload Images
      switchMap(game => {
        const uploads = this.uploadedImages().map(img => ({
          name: img.name,
          imageBase64: img.base64,
          contentType: img.contentType,
        }));
        return this.gameService.uploadImages(game.id, uploads);
      }),
      // 3. Handle Success
      tap(() => {
        this.successMessage.set('Game created successfully!');
        this.resetForm();
        this.loadData();
        this.setPage('My Games'); // Auto-navigate to list
      }),
      // 4. Handle Errors
      catchError(err => {
        console.error('[HelperView] Game creation failed', err);
        const status = err?.status;
        let msg = 'Failed to create game: ';

        if (status === 401 || status === 403) {
          msg += 'Authentication error. Please log out and log back in.';
        } else if (status === 0) {
          msg += 'Could not reach the server. Check if the API gateway is running.';
        } else {
          msg += (err?.error?.error || err?.message || 'Unknown error');
        }

        this.errorMessage.set(msg);
        return of(null);
      }),
      finalize(() => this.creating.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
}
