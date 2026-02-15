import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '@/core/auth';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { GameService, type GameResponse, type GameStatsResponse } from '@/core/services/game.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardSkeletonComponent,
    ZardButtonComponent,
    ZardProgressBarComponent,
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/patient"
    >
      @switch (currentPage()) {
        @case ('Home') {
          <h2 class="text-2xl font-bold mb-6">Welcome Back!</h2>
          <div class="grid gap-4 md:grid-cols-3 mb-8">
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">My Games</p>
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
              <h3 class="text-lg font-semibold mb-4">Your Performance</h3>
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
            <z-card class="p-6 cursor-pointer hover:border-primary transition-colors" (click)="setPage('Play Games')">
              <div class="flex items-center gap-3">
                <z-icon zType="play-circle" class="text-primary h-10 w-10" />
                <div>
                  <h3 class="font-semibold">Play Games</h3>
                  <p class="text-sm text-muted-foreground">Test your memory with exercises</p>
                </div>
              </div>
            </z-card>
          </div>
        }

        @case ('My Games') {
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-2xl font-bold">My Games</h2>
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
                    @if (game.imageCount >= 2) {
                      <button z-button zSize="sm" (click)="playGame(game.id)">
                        <z-icon zType="play" class="mr-1" />
                        Play
                      </button>
                    }
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
              <p class="text-muted-foreground mb-4">Create your first memory game by uploading photos!</p>
              <button z-button (click)="setPage('Create Game')">
                <z-icon zType="plus" class="mr-2" />
                Create Game
              </button>
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

        @case ('Play Games') {
          <h2 class="text-2xl font-bold mb-6">Play Games</h2>

          @if (games().length > 0) {
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              @for (game of playableGames(); track game.id) {
                <z-card class="p-6 hover:border-primary transition-colors cursor-pointer" (click)="playGame(game.id)">
                  <div class="flex items-center gap-3 mb-3">
                    <z-icon zType="brain" class="text-primary h-8 w-8" />
                    <div>
                      <h3 class="font-semibold">{{ game.title }}</h3>
                      <p class="text-sm text-muted-foreground">{{ game.description }}</p>
                    </div>
                  </div>
                  <div class="flex items-center justify-between">
                    <z-badge zType="secondary">{{ game.imageCount }} images</z-badge>
                    <button z-button zSize="sm">
                      <z-icon zType="play" class="mr-1" />
                      Play
                    </button>
                  </div>
                </z-card>
              }
            </div>
          } @else {
            <z-card class="p-12 text-center">
              <z-icon zType="gamepad-2" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 class="font-semibold mb-2">No games available</h3>
              <p class="text-muted-foreground mb-4">Create a game first, then come back to play!</p>
              <button z-button (click)="setPage('Create Game')">
                <z-icon zType="plus" class="mr-2" />
                Create Game
              </button>
            </z-card>
          }
        }

        @case ('My Progress') {
          <h2 class="text-2xl font-bold mb-6">My Progress</h2>

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
              <p class="text-muted-foreground">Play some games to see your progress here!</p>
            </z-card>
          }
        }
      }
    </app-dashboard-layout>
  `,
})
export class PatientDashboardComponent implements OnInit {
  currentPage = signal('Home');
  games = signal<GameResponse[]>([]);
  stats = signal<GameStatsResponse | null>(null);

  // Create game form
  newGameTitle = signal('');
  newGameDescription = signal('');
  uploadedImages = signal<{ name: string; base64: string; contentType: string; preview: string }[]>([]);
  creating = signal(false);

  keycloakId = '';

  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'Navigation',
      items: [
        { icon: 'house', label: 'Home', action: () => this.setPage('Home') },
        { icon: 'gamepad-2', label: 'My Games', action: () => this.setPage('My Games') },
        { icon: 'play-circle', label: 'Play Games', action: () => this.setPage('Play Games') },
        { icon: 'bar-chart-3', label: 'My Progress', action: () => this.setPage('My Progress') },
      ],
    },
    {
      label: 'Actions',
      items: [
        { icon: 'plus-circle', label: 'Create Game', action: () => this.setPage('Create Game') },
      ],
    },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.keycloakService.loadUserProfile().then(profile => {
      this.keycloakId = profile.id ?? '';
      this.loadData();
    }).catch(() => {
      this.keycloakId = '';
    });
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  get playableGames(): () => GameResponse[] {
    return () => this.games().filter(g => g.imageCount >= 2);
  }

  canCreateGame(): boolean {
    return this.newGameTitle().trim().length > 0 && this.uploadedImages().length >= 2 && !this.creating();
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

  createGame(): void {
    if (!this.canCreateGame()) return;
    this.creating.set(true);

    this.gameService.createGame(this.keycloakId, {
      title: this.newGameTitle(),
      description: this.newGameDescription(),
    }).subscribe({
      next: game => {
        // Upload images
        const uploads = this.uploadedImages().map(img => ({
          name: img.name,
          imageBase64: img.base64,
          contentType: img.contentType,
        }));

        this.gameService.uploadImages(game.id, uploads).subscribe({
          next: () => {
            this.creating.set(false);
            this.resetForm();
            this.loadData();
            this.setPage('My Games');
          },
          error: err => {
            console.error('Failed to upload images', err);
            this.creating.set(false);
            // Game was created, reload anyway
            this.loadData();
            this.setPage('My Games');
          },
        });
      },
      error: err => {
        console.error('Failed to create game', err);
        this.creating.set(false);
      },
    });
  }

  resetForm(): void {
    this.newGameTitle.set('');
    this.newGameDescription.set('');
    this.uploadedImages.set([]);
  }

  playGame(gameId: number): void {
    this.router.navigate(['/patient/play', gameId]);
  }

  deleteGame(gameId: number): void {
    this.gameService.deleteGame(gameId).subscribe({
      next: () => this.loadData(),
      error: err => console.error('Failed to delete game', err),
    });
  }

  private loadData(): void {
    if (!this.keycloakId) return;

    this.gameService.getPatientGames(this.keycloakId).subscribe({
      next: games => this.games.set(games),
      error: err => console.error('Failed to load games', err),
    });

    this.gameService.getPlayerStats(this.keycloakId).subscribe({
      next: stats => this.stats.set(stats),
      error: err => console.error('Failed to load stats', err),
    });
  }
}
