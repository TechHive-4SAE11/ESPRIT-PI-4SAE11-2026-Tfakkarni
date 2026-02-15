import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { GameService, type GamePlayData, type AnswerEntry, type GameAttemptResponse } from '@/core/services/game.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';

type GamePhase = 'loading' | 'playing' | 'results';

@Component({
  selector: 'app-play-game',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardButtonComponent,
    ZardProgressBarComponent,
  ],
  template: `
    <div class="min-h-screen bg-background text-foreground">
      <!-- Header -->
      <header class="border-b border-border px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button z-button zType="ghost" zSize="sm" (click)="goBack()">
            <z-icon zType="arrow-left" class="mr-1" />
            Back
          </button>
          <h1 class="text-xl font-bold text-primary">
            @if (gameData()) {
              {{ gameData()!.title }}
            } @else {
              Loading Game...
            }
          </h1>
        </div>

        @if (phase() === 'playing') {
          <div class="flex items-center gap-4">
            <z-badge zType="secondary">
              Question {{ currentIndex() + 1 }} / {{ gameData()!.totalQuestions }}
            </z-badge>
            <span class="text-sm text-muted-foreground flex items-center gap-1">
              <z-icon zType="clock" />
              {{ formatTime(elapsedSeconds()) }}
            </span>
          </div>
        }
      </header>

      <main class="container mx-auto p-8 max-w-4xl">
        @switch (phase()) {
          @case ('loading') {
            <div class="flex flex-col items-center justify-center min-h-[400px]">
              <z-icon zType="loader-2" class="h-12 w-12 animate-spin text-primary mb-4" />
              <p class="text-muted-foreground">Loading game...</p>
            </div>
          }

          @case ('playing') {
            @if (gameData(); as gd) {
              <!-- Progress bar -->
              <z-progress-bar
                [progress]="progressPercent()"
                class="mb-8 h-2"
              />

              <!-- Current question -->
              <z-card class="p-0 overflow-hidden mb-8">
                <div class="relative">
                  <img
                    [src]="'data:' + gd.images[currentIndex()].contentType + ';base64,' + gd.images[currentIndex()].imageBase64"
                    alt="Who is this?"
                    class="w-full h-80 object-contain bg-muted"
                  />
                  <div class="absolute top-4 left-4">
                    <z-badge>
                      <z-icon zType="help-circle" class="mr-1" />
                      Name this image
                    </z-badge>
                  </div>
                </div>
              </z-card>

              <!-- Choices -->
              <h3 class="text-lg font-semibold mb-4 text-center">Who or what is in this image?</h3>
              <div class="grid gap-3 md:grid-cols-2 mb-6">
                @for (choice of gd.choices; track choice) {
                  <button
                    z-button
                    [zType]="selectedAnswer() === choice ? 'default' : 'outline'"
                    class="py-6 text-lg justify-start"
                    (click)="selectAnswer(choice)"
                  >
                    {{ choice }}
                  </button>
                }
              </div>

              <!-- Navigation -->
              <div class="flex items-center justify-between">
                <button z-button zType="outline" [disabled]="currentIndex() === 0" (click)="prevQuestion()">
                  <z-icon zType="chevron-left" class="mr-1" />
                  Previous
                </button>

                @if (currentIndex() < gd.totalQuestions - 1) {
                  <button z-button [disabled]="!selectedAnswer()" (click)="nextQuestion()">
                    Next
                    <z-icon zType="chevron-right" class="ml-1" />
                  </button>
                } @else {
                  <button
                    z-button
                    [disabled]="!allAnswered()"
                    (click)="submitGame()"
                  >
                    @if (submitting()) {
                      <z-icon zType="loader-2" class="mr-2 animate-spin" />
                      Submitting...
                    } @else {
                      <z-icon zType="check-circle" class="mr-2" />
                      Submit Answers
                    }
                  </button>
                }
              </div>
            }
          }

          @case ('results') {
            @if (results(); as res) {
              <!-- Score banner -->
              <z-card class="p-8 text-center mb-8">
                <div class="mb-4">
                  @if (res.percentage >= 80) {
                    <z-icon zType="trophy" class="h-16 w-16 text-yellow-500 mx-auto mb-2" />
                    <h2 class="text-3xl font-bold text-green-600">Excellent!</h2>
                  } @else if (res.percentage >= 50) {
                    <z-icon zType="thumbs-up" class="h-16 w-16 text-blue-500 mx-auto mb-2" />
                    <h2 class="text-3xl font-bold text-blue-600">Good Job!</h2>
                  } @else {
                    <z-icon zType="heart" class="h-16 w-16 text-primary mx-auto mb-2" />
                    <h2 class="text-3xl font-bold">Keep Practicing!</h2>
                  }
                </div>
                <p class="text-5xl font-bold mb-2">{{ res.score }} / {{ res.totalQuestions }}</p>
                <p class="text-muted-foreground text-lg">{{ res.percentage | number:'1.0-0' }}% correct</p>
                <z-progress-bar [progress]="res.percentage" class="max-w-md mx-auto mt-4 h-3" />
                <p class="text-sm text-muted-foreground mt-2">
                  Completed in {{ formatTime(res.durationSeconds) }}
                </p>
              </z-card>

              <!-- Detailed results -->
              <h3 class="text-xl font-semibold mb-4">Detailed Results</h3>
              <div class="space-y-3 mb-8">
                @for (r of res.results; track r.imageId; let i = $index) {
                  <z-card class="p-4">
                    <div class="flex items-center gap-4">
                      <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
                           [class]="r.correct ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'">
                        {{ i + 1 }}
                      </div>
                      <div class="flex-1">
                        <div class="flex items-center gap-2">
                          @if (r.correct) {
                            <z-icon zType="check-circle" class="text-green-600" />
                            <span class="font-medium">{{ r.correctName }}</span>
                            <z-badge zType="secondary" class="text-green-600">Correct!</z-badge>
                          } @else {
                            <z-icon zType="x-circle" class="text-red-500" />
                            <span class="font-medium line-through text-muted-foreground">{{ r.selectedName }}</span>
                            <z-icon zType="arrow-right" class="text-muted-foreground" />
                            <span class="font-medium text-green-600">{{ r.correctName }}</span>
                          }
                        </div>
                      </div>
                    </div>
                  </z-card>
                }
              </div>

              <!-- Actions -->
              <div class="flex gap-3 justify-center">
                <button z-button (click)="replayGame()">
                  <z-icon zType="rotate-ccw" class="mr-2" />
                  Play Again
                </button>
                <button z-button zType="outline" (click)="goBack()">
                  <z-icon zType="arrow-left" class="mr-2" />
                  Back to Dashboard
                </button>
              </div>
            }
          }
        }
      </main>
    </div>
  `,
})
export class PlayGameComponent implements OnInit {
  phase = signal<GamePhase>('loading');
  gameData = signal<GamePlayData | null>(null);
  currentIndex = signal(0);
  answers = signal<Map<number, string>>(new Map());
  selectedAnswer = signal<string | null>(null);
  submitting = signal(false);
  results = signal<GameAttemptResponse | null>(null);
  elapsedSeconds = signal(0);

  private gameId = 0;
  private keycloakId = '';
  private startTime = 0;
  private timerInterval: any;

  progressPercent = computed(() => {
    const gd = this.gameData();
    if (!gd) return 0;
    return ((this.currentIndex() + 1) / gd.totalQuestions) * 100;
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
  ) {}

  ngOnInit(): void {
    this.gameId = Number(this.route.snapshot.paramMap.get('gameId') ?? 0);

    this.keycloakService.loadUserProfile().then(profile => {
      this.keycloakId = profile.id ?? '';
    }).catch(() => {
      this.keycloakId = '';
    });

    this.loadGame();
  }

  allAnswered(): boolean {
    const gd = this.gameData();
    if (!gd) return false;
    return this.answers().size >= gd.totalQuestions;
  }

  selectAnswer(choice: string): void {
    this.selectedAnswer.set(choice);
    const gd = this.gameData();
    if (!gd) return;

    const imageId = gd.images[this.currentIndex()].id;
    this.answers.update(map => {
      const next = new Map(map);
      next.set(imageId, choice);
      return next;
    });
  }

  nextQuestion(): void {
    const gd = this.gameData();
    if (!gd || this.currentIndex() >= gd.totalQuestions - 1) return;
    this.currentIndex.update(i => i + 1);
    // Restore previously selected answer for this question
    const imageId = gd.images[this.currentIndex()].id;
    this.selectedAnswer.set(this.answers().get(imageId) ?? null);
  }

  prevQuestion(): void {
    if (this.currentIndex() <= 0) return;
    this.currentIndex.update(i => i - 1);
    const gd = this.gameData();
    if (!gd) return;
    const imageId = gd.images[this.currentIndex()].id;
    this.selectedAnswer.set(this.answers().get(imageId) ?? null);
  }

  submitGame(): void {
    const gd = this.gameData();
    if (!gd || !this.allAnswered()) return;
    this.submitting.set(true);

    const duration = Math.floor((Date.now() - this.startTime) / 1000);
    const answerEntries: AnswerEntry[] = [];
    this.answers().forEach((selectedName, imageId) => {
      answerEntries.push({ imageId, selectedName });
    });

    this.gameService.submitAnswers(this.gameId, this.keycloakId, {
      miniGameId: this.gameId,
      answers: answerEntries,
      durationSeconds: duration,
    }).subscribe({
      next: res => {
        this.results.set(res);
        this.phase.set('results');
        this.submitting.set(false);
        this.stopTimer();
      },
      error: err => {
        console.error('Failed to submit answers', err);
        this.submitting.set(false);
      },
    });
  }

  replayGame(): void {
    this.answers.set(new Map());
    this.selectedAnswer.set(null);
    this.currentIndex.set(0);
    this.results.set(null);
    this.loadGame();
  }

  goBack(): void {
    this.stopTimer();
    this.router.navigate(['/patient']);
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private loadGame(): void {
    this.phase.set('loading');
    this.gameService.getGameForPlay(this.gameId).subscribe({
      next: data => {
        this.gameData.set(data);
        this.phase.set('playing');
        this.startTimer();
      },
      error: err => {
        console.error('Failed to load game', err);
      },
    });
  }

  private startTimer(): void {
    this.startTime = Date.now();
    this.elapsedSeconds.set(0);
    this.timerInterval = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - this.startTime) / 1000));
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }
}
