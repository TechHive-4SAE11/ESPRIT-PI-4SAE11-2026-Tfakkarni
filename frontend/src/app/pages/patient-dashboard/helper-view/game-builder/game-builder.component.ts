import {
  Component, Input, Output, EventEmitter, signal, inject, DestroyRef, OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap, finalize } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';

import {
  CustomGameService,
  type CustomGameResponse,
} from '@/core/services/custom-game.service';
import {
  DataPointService,
  type DataPointSummary,
  type DataPointType,
} from '@/core/services/data-point.service';
import { MemoryTagService, type TagResponse } from '@/core/services/memory-tag.service';

type View = 'list' | 'build';

@Component({
  selector: 'app-game-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardIconComponent, ZardButtonComponent, ZardBadgeComponent],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-foreground">Custom Games</h2>
          <p class="text-muted-foreground">Assemble data points into memory games</p>
        </div>
        <z-button variant="outline" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-2 h-4 w-4" /> Back
        </z-button>
      </div>

      @switch (view()) {
        @case ('list') {
          <z-button (click)="startBuild()">
            <z-icon zType="plus" class="mr-2 h-4 w-4" /> New Custom Game
          </z-button>

          @if (isLoading()) {
            <div class="text-center py-12 text-muted-foreground">Loading games...</div>
          } @else if (games().length === 0) {
            <div class="text-center py-12">
              <p class="text-4xl mb-3">🎮</p>
              <p class="text-lg font-semibold text-muted-foreground">No custom games yet</p>
              <p class="text-sm text-muted-foreground">Create one by selecting data points from your library</p>
            </div>
          } @else {
            <div class="grid gap-3 sm:grid-cols-2">
              @for (game of games(); track game.id) {
                <z-card class="p-5 relative group">
                  <div class="flex items-start justify-between mb-2">
                    <h3 class="font-semibold text-lg">{{ game.title }}</h3>
                    <button (click)="deleteGame(game.id)"
                      class="opacity-0 group-hover:opacity-100 text-muted-foreground hover:text-red-500 transition-all">
                      <z-icon zType="trash-2" class="h-4 w-4" />
                    </button>
                  </div>
                  @if (game.description) {
                    <p class="text-sm text-muted-foreground mb-3">{{ game.description }}</p>
                  }
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-xs font-medium px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                      {{ game.itemCount }} items
                    </span>
                    @for (t of game.itemTypes; track t) {
                      <span class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">{{ getTypeEmoji(t) }} {{ t }}</span>
                    }
                  </div>
                </z-card>
              }
            </div>
          }
        }

        @case ('build') {
          <!-- Step indicator -->
          <div class="flex items-center gap-2 text-sm">
            <span class="px-3 py-1 rounded-full font-medium"
              [class]="step() === 1 ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'">
              1. Details
            </span>
            <span class="text-muted-foreground">→</span>
            <span class="px-3 py-1 rounded-full font-medium"
              [class]="step() === 2 ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'">
              2. Select Items
            </span>
            <span class="text-muted-foreground">→</span>
            <span class="px-3 py-1 rounded-full font-medium"
              [class]="step() === 3 ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'">
              3. Confirm
            </span>
          </div>

          @switch (step()) {
            @case (1) {
              <z-card class="p-6">
                <h3 class="text-lg font-semibold mb-4">Game Details</h3>
                <div class="space-y-4">
                  <div>
                    <label class="text-sm font-medium block mb-1">Title *</label>
                    <input type="text" [(ngModel)]="gameTitle" placeholder="e.g., Family Memories"
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
                  </div>
                  <div>
                    <label class="text-sm font-medium block mb-1">Description</label>
                    <input type="text" [(ngModel)]="gameDescription" placeholder="Optional description"
                      class="w-full rounded-md border border-border bg-background px-3 py-2 text-sm" />
                  </div>
                  <div class="flex gap-2">
                    <z-button variant="outline" (click)="view.set('list')">Cancel</z-button>
                    <button z-button [disabled]="!gameTitle.trim()" (click)="step.set(2); loadDataPoints()">Next</button>
                  </div>
                </div>
              </z-card>
            }

            @case (2) {
              <z-card class="p-6">
                <h3 class="text-lg font-semibold mb-4">Select Data Points</h3>

                <!-- Type filter -->
                <div class="flex flex-wrap gap-2 mb-3">
                  <button (click)="filterType.set(null)"
                    class="px-3 py-1 rounded-full text-xs font-medium transition-colors"
                    [class]="!filterType() ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'">
                    All
                  </button>
                  @for (t of typeFilters; track t.type) {
                    <button (click)="filterType.set(t.type)"
                      class="px-3 py-1 rounded-full text-xs font-medium transition-colors"
                      [class]="filterType() === t.type ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'">
                      {{ t.emoji }} {{ t.label }}
                    </button>
                  }
                </div>

                <!-- Tag filter -->
                @if (tags().length > 0) {
                  <div class="flex flex-wrap gap-1.5 mb-4">
                    @for (tag of tags(); track tag.id) {
                      <button (click)="toggleFilterTag(tag.id)"
                        class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium border transition-colors"
                        [class]="filterTagIds().has(tag.id) ? 'border-primary bg-primary/10 text-primary' : 'border-border'">
                        <span class="w-1.5 h-1.5 rounded-full" [style.background-color]="tag.color"></span>
                        {{ tag.name }}
                      </button>
                    }
                  </div>
                }

                <!-- Selection count -->
                <div class="flex items-center justify-between mb-3">
                  <p class="text-sm text-muted-foreground">
                    {{ selectedIds().size }} items selected
                  </p>
                  @if (filteredDataPoints().length > 0) {
                    <button (click)="toggleAll()" class="text-xs text-primary hover:underline">
                      {{ allFilteredSelected() ? 'Deselect All' : 'Select All Visible' }}
                    </button>
                  }
                </div>

                <!-- Data point list -->
                @if (isLoadingData()) {
                  <div class="text-center py-8 text-muted-foreground">Loading...</div>
                } @else if (filteredDataPoints().length === 0) {
                  <div class="text-center py-8 text-muted-foreground">
                    No data points match your filter. Upload some in the Data Library first.
                  </div>
                } @else {
                  <div class="max-h-80 overflow-y-auto space-y-2 border border-border rounded-md p-2">
                    @for (item of filteredDataPoints(); track item.id + '-' + item.type) {
                      <button (click)="toggleItem(item)"
                        class="w-full flex items-center gap-3 p-2.5 rounded-lg text-left transition-colors"
                        [class]="isSelected(item) ? 'bg-primary/10 border border-primary' : 'hover:bg-muted border border-transparent'">
                        <!-- Check indicator -->
                        <span class="w-5 h-5 rounded-md border-2 flex items-center justify-center flex-shrink-0"
                          [class]="isSelected(item) ? 'border-primary bg-primary text-primary-foreground' : 'border-border'">
                          @if (isSelected(item)) { <z-icon zType="check" class="h-3 w-3" /> }
                        </span>
                        <!-- Type icon -->
                        <span class="text-sm">{{ getTypeEmoji(item.type) }}</span>
                        <!-- Info -->
                        <div class="flex-1 min-w-0">
                          <p class="text-sm font-medium truncate">{{ item.label }}</p>
                          <p class="text-[11px] text-muted-foreground truncate">{{ item.subtitle }}</p>
                        </div>
                        <!-- Tags -->
                        @for (tag of item.tags; track tag.id) {
                          <span class="w-2 h-2 rounded-full flex-shrink-0" [style.background-color]="tag.color"
                            [title]="tag.name"></span>
                        }
                      </button>
                    }
                  </div>
                }

                <div class="flex gap-2 mt-4">
                  <z-button variant="outline" (click)="step.set(1)">Back</z-button>
                  <button z-button [disabled]="selectedIds().size === 0" (click)="step.set(3)">
                    Next ({{ selectedIds().size }} items)
                  </button>
                </div>
              </z-card>
            }

            @case (3) {
              <z-card class="p-6">
                <h3 class="text-lg font-semibold mb-2">Confirm Game</h3>
                <div class="space-y-3 mb-4">
                  <div>
                    <p class="text-sm text-muted-foreground">Title</p>
                    <p class="font-medium">{{ gameTitle }}</p>
                  </div>
                  @if (gameDescription.trim()) {
                    <div>
                      <p class="text-sm text-muted-foreground">Description</p>
                      <p class="font-medium">{{ gameDescription }}</p>
                    </div>
                  }
                  <div>
                    <p class="text-sm text-muted-foreground">Items ({{ selectedItems().length }})</p>
                    <div class="mt-1 flex flex-wrap gap-1">
                      @for (item of selectedItems(); track item.id + '-' + item.type) {
                        <span class="text-xs px-2 py-1 rounded-md bg-muted">
                          {{ getTypeEmoji(item.type) }} {{ item.label }}
                        </span>
                      }
                    </div>
                  </div>
                </div>
                <div class="flex gap-2">
                  <z-button variant="outline" (click)="step.set(2)">Back</z-button>
                  <button z-button [disabled]="isSaving()" (click)="saveGame()">
                    @if (isSaving()) { Saving... } @else { <z-icon zType="save" class="mr-2 h-4 w-4" /> Create Game }
                  </button>
                </div>
              </z-card>
            }
          }
        }
      }
    </div>
  `,
})
export class GameBuilderComponent implements OnInit {
  @Input({ required: true }) keycloakId!: string;
  @Output() goBack = new EventEmitter<void>();

  private readonly customGameService = inject(CustomGameService);
  private readonly dataPointService = inject(DataPointService);
  private readonly tagService = inject(MemoryTagService);
  private readonly destroyRef = inject(DestroyRef);

  view = signal<View>('list');
  step = signal<number>(1);

  games = signal<CustomGameResponse[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);

  // Build state
  gameTitle = '';
  gameDescription = '';

  // Data point selection
  allDataPoints = signal<DataPointSummary[]>([]);
  filteredDataPoints = signal<DataPointSummary[]>([]);
  tags = signal<TagResponse[]>([]);
  isLoadingData = signal(false);

  filterType = signal<DataPointType | null>(null);
  filterTagIds = signal<Set<number>>(new Set());
  selectedIds = signal<Set<string>>(new Set()); // "TYPE-ID" composite keys

  typeFilters = [
    { type: 'PHOTO' as DataPointType, emoji: '📷', label: 'Photos' },
    { type: 'PLACE' as DataPointType, emoji: '📍', label: 'Places' },
    { type: 'MOVIE' as DataPointType, emoji: '🎬', label: 'Movies' },
    { type: 'QUESTION' as DataPointType, emoji: '❓', label: 'Questions' },
  ];

  ngOnInit() {
    this.loadGames();
    this.loadTags();
  }

  private loadTags() {
    this.tagService.getTags(this.keycloakId).pipe(
      tap(tags => this.tags.set(tags)),
      catchError(() => of([])),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  loadGames() {
    this.isLoading.set(true);
    this.customGameService.getGames(this.keycloakId).pipe(
      tap(games => this.games.set(games)),
      catchError(() => { this.games.set([]); return of([]); }),
      finalize(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  deleteGame(id: number) {
    this.customGameService.deleteGame(id).pipe(
      tap(() => this.games.update(g => g.filter(x => x.id !== id))),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  startBuild() {
    this.gameTitle = '';
    this.gameDescription = '';
    this.selectedIds.set(new Set());
    this.step.set(1);
    this.view.set('build');
  }

  loadDataPoints() {
    this.isLoadingData.set(true);
    this.dataPointService.getAllDataPoints(this.keycloakId).pipe(
      tap(data => {
        this.allDataPoints.set(data);
        this.applyFilter();
      }),
      catchError(() => { this.allDataPoints.set([]); this.filteredDataPoints.set([]); return of([]); }),
      finalize(() => this.isLoadingData.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  applyFilter() {
    let result = this.allDataPoints();
    const type = this.filterType();
    if (type) result = result.filter(i => i.type === type);
    const tagIds = this.filterTagIds();
    if (tagIds.size > 0) {
      result = result.filter(i => i.tags?.some(t => tagIds.has(t.id)));
    }
    this.filteredDataPoints.set(result);
  }

  toggleFilterTag(id: number) {
    const s = new Set(this.filterTagIds());
    if (s.has(id)) s.delete(id); else s.add(id);
    this.filterTagIds.set(s);
    this.applyFilter();
  }

  // Track by changing filterType signal  
  private readonly previousFilterType: DataPointType | null = null;

  private itemKey(item: DataPointSummary): string {
    return `${item.type}-${item.id}`;
  }

  isSelected(item: DataPointSummary): boolean {
    return this.selectedIds().has(this.itemKey(item));
  }

  toggleItem(item: DataPointSummary) {
    const key = this.itemKey(item);
    const s = new Set(this.selectedIds());
    if (s.has(key)) s.delete(key); else s.add(key);
    this.selectedIds.set(s);
  }

  allFilteredSelected(): boolean {
    return this.filteredDataPoints().length > 0 &&
      this.filteredDataPoints().every(i => this.selectedIds().has(this.itemKey(i)));
  }

  toggleAll() {
    if (this.allFilteredSelected()) {
      const s = new Set(this.selectedIds());
      this.filteredDataPoints().forEach(i => s.delete(this.itemKey(i)));
      this.selectedIds.set(s);
    } else {
      const s = new Set(this.selectedIds());
      this.filteredDataPoints().forEach(i => s.add(this.itemKey(i)));
      this.selectedIds.set(s);
    }
  }

  selectedItems(): DataPointSummary[] {
    return this.allDataPoints().filter(i => this.selectedIds().has(this.itemKey(i)));
  }

  saveGame() {
    if (!this.gameTitle.trim() || this.selectedIds().size === 0) return;
    this.isSaving.set(true);

    const items = this.selectedItems().map(i => ({
      dataType: i.type,
      dataPointId: i.id,
    }));

    this.customGameService.createGame(this.keycloakId, {
      title: this.gameTitle.trim(),
      description: this.gameDescription.trim(),
      items,
    }).pipe(
      tap(() => {
        this.view.set('list');
        this.loadGames();
      }),
      catchError(() => of(null)),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  getTypeEmoji(type: string): string {
    const map: Record<string, string> = { PHOTO: '📷', PLACE: '📍', MOVIE: '🎬', QUESTION: '❓' };
    return map[type] || '📄';
  }
}
