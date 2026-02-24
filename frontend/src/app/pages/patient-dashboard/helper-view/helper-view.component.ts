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
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { MedicationManagementComponent } from '@/pages/medications/medications.component';

import {
  GameService,
  type GameResponse,
  type GameStatsResponse,
  type GameDetailResponse,
  type EditImageEntry,
} from '@/core/services/game.service';
import { SuiviQuotidienComponent } from './suivi-quotidien/suivi-quotidien.component';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';

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
    PrescriptionListComponent,
    CarePlanListComponent,
    MedicationManagementComponent,
    SuiviQuotidienComponent,
  ],
  templateUrl: './helper-view.component.html',
})
export class HelperViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly gameService = inject(GameService);
  private readonly keycloakService = inject(KeycloakService);
  private readonly userApiService = inject(UserApiService);
  private readonly alertDialog = inject(ZardAlertDialogService);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // State Signals
  currentPage = signal<string>('Home');
  games = signal<GameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

  // Loading Signals
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

    this.loadUserNeonDbId();
    this.loadGames();
    this.loadStats();
  }
  
  private loadUserNeonDbId(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[HelperView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
          this.currentUser.set(userInfo);
        }),
        catchError(err => {
          console.error('[HelperView] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
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
