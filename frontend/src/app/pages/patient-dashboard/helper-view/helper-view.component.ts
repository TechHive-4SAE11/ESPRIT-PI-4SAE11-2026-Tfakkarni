import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef,
  ChangeDetectionStrategy,
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
import { StatisticsDashboardComponent } from './statistics-dashboard/statistics-dashboard.component';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-helper-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './helper-view.component.html',
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
    StatisticsDashboardComponent,
  ],
})
export class HelperViewComponent implements OnInit {
  private readonly destroyRef        = inject(DestroyRef);
  private readonly gameService       = inject(GameService);
  private readonly keycloakService   = inject(KeycloakService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly userApiService    = inject(UserApiService);
  private readonly alertDialog       = inject(ZardAlertDialogService);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // ── State ─────────────────────────────────────────────────────────────────
  currentPage   = signal<string>('Home');
  games         = signal<GameResponse[]>([]);
  stats         = signal<GameStatsResponse | null>(null);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  userNeonDbId  = signal<number | null>(null);

  // ── Loading ───────────────────────────────────────────────────────────────
  isLoadingPrescriptions = signal(false);
  creating               = signal(false);

  // ── Form (Create / Edit game) ─────────────────────────────────────────────
  editingGameId     = signal<number | null>(null);
  newGameTitle      = signal('');
  newGameDescription = signal('');
  uploadedImages    = signal<{ id?: number; name: string; base64: string; contentType: string; preview: string }[]>([]);
  errorMessage      = signal('');
  successMessage    = signal('');

  // ─────────────────────────────────────────────────────────────────────────

  ngOnInit(): void {
    if (this.keycloakId) this.loadData();
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    this.pageChange.emit(page);
  }

  // ── Data loading ──────────────────────────────────────────────────────────

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
        catchError(err => { console.error('[HelperView] loadGames', err); return of([]); }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private loadStats(): void {
    this.gameService.getPlayerStats(this.keycloakId)
      .pipe(
        tap(s => this.stats.set(s)),
        catchError(err => { console.error('[HelperView] loadStats', err); return of(null); }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private loadPrescriptions(): void {
    this.isLoadingPrescriptions.set(true);
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(u => this.userNeonDbId.set(u.id)),
        switchMap(u => this.prescriptionService.getPrescriptionsByPatient(u.id.toString())),
        tap(p => this.prescriptions.set(p)),
        catchError(err => { console.error('[HelperView] loadPrescriptions', err); return of([]); }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  // ── Game CRUD ─────────────────────────────────────────────────────────────

  deleteGame(gameId: number): void {
    const ref = this.alertDialog.confirm({
      zTitle: 'Supprimer le jeu',
      zDescription: 'Cette action est irréversible. Toutes les images associées seront supprimées.',
      zOkText: 'Supprimer',
      zCancelText: 'Annuler',
      zOkDestructive: true,
      zOnOk: () => {
        this.gameService.deleteGame(gameId)
          .pipe(
            tap(() => this.loadData()),
            catchError(err => { console.error('[HelperView] deleteGame', err); return of(null); }),
            takeUntilDestroyed(this.destroyRef),
          )
          .subscribe();
        ref.close();
      },
    });
  }

  startEdit(gameId: number): void {
    this.resetForm();
    this.editingGameId.set(gameId);
    this.setPage('Create Game');
    this.gameService.getGameDetail(gameId)
      .pipe(
        tap((d: GameDetailResponse) => {
          this.newGameTitle.set(d.title);
          this.newGameDescription.set(d.description || '');
          this.uploadedImages.set(d.images.map(img => ({
            id: img.id,
            name: img.name,
            base64: img.imageBase64,
            contentType: img.contentType,
            preview: `data:${img.contentType};base64,${img.imageBase64}`,
          })));
        }),
        catchError(err => {
          console.error('[HelperView] startEdit', err);
          this.errorMessage.set('Impossible de charger le jeu pour modification.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  canCreateGame(): boolean {
    return this.newGameTitle().trim().length > 0
      && this.uploadedImages().length >= 2
      && !this.creating();
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    for (const file of Array.from(input.files)) {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        this.uploadedImages.update(imgs => [
          ...imgs,
          { name: file.name.replace(/\.[^/.]+$/, ''), base64: result.split(',')[1], contentType: file.type, preview: result },
        ]);
      };
      reader.readAsDataURL(file);
    }
  }

  updateImageName(index: number, name: string): void {
    this.uploadedImages.update(imgs => {
      const arr = [...imgs];
      arr[index] = { ...arr[index], name };
      return arr;
    });
  }

  removeImage(index: number): void {
    this.uploadedImages.update(imgs => imgs.filter((_, i) => i !== index));
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
      if (kc?.refreshToken) await this.keycloakService.updateToken(30);
    } catch (e) { console.warn('[HelperView] token refresh warning', e); }

    // ── Édition ──
    if (this.editingGameId()) {
      const images: EditImageEntry[] = this.uploadedImages().map(img => ({
        id: img.id ?? null,
        name: img.name,
        imageBase64: img.id ? undefined : img.base64,
        contentType:  img.id ? undefined : img.contentType,
      }));
      this.gameService.editGame(this.editingGameId()!, {
        title: this.newGameTitle(),
        description: this.newGameDescription(),
        images,
      }).pipe(
        tap(() => { this.successMessage.set('Jeu mis à jour !'); this.resetForm(); this.loadData(); this.setPage('My Games'); }),
        catchError(err => {
          this.errorMessage.set('Erreur : ' + (err?.error?.error ?? err?.message ?? 'Inconnue'));
          return of(null);
        }),
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      ).subscribe();
      return;
    }

    // ── Création ──
    this.gameService.createGame(this.keycloakId, {
      title: this.newGameTitle(),
      description: this.newGameDescription(),
    }).pipe(
      switchMap(game => this.gameService.uploadImages(game.id, this.uploadedImages().map(img => ({
        name: img.name, imageBase64: img.base64, contentType: img.contentType,
      })))),
      tap(() => { this.successMessage.set('Jeu créé !'); this.resetForm(); this.loadData(); this.setPage('My Games'); }),
      catchError(err => {
        const status = err?.status;
        let msg = 'Erreur de création : ';
        if (status === 401 || status === 403) msg += 'Authentification requise.';
        else if (status === 0) msg += 'Serveur inaccessible.';
        else msg += (err?.error?.error ?? err?.message ?? 'Inconnue');
        this.errorMessage.set(msg);
        return of(null);
      }),
      finalize(() => this.creating.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }
}
