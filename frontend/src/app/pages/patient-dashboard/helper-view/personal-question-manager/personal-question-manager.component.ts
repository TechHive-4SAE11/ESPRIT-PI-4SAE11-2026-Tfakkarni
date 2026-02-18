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
import { catchError, of, tap, finalize } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';

import {
  PersonalQuestionService,
  type PersonalQuestionGameResponse,
  type EditQuestionItemEntry,
} from '@/core/services/personal-question.service';

@Component({
  selector: 'app-personal-question-manager',
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
    <!-- Personal Questions Game List -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-2">
        <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-1" />
          Back
        </button>
        <h2 class="text-2xl font-bold">Personal Questions</h2>
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

    @if (games().length > 0) {
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      @for (game of games(); track game.id) {
      <z-card class="p-6">
        <div class="flex items-start justify-between mb-3">
          <div>
            <h3 class="font-semibold text-lg">{{ game.title }}</h3>
            <p class="text-sm text-muted-foreground">{{ game.description }}</p>
          </div>
          <z-badge zType="secondary">{{ game.questionCount }} questions</z-badge>
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
      <z-icon zType="brain" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
      <h3 class="font-semibold mb-2">No personal question quizzes yet</h3>
      <p class="text-muted-foreground mb-4">Create a quiz with personal questions to help the patient exercise their memory!</p>
      <button z-button (click)="startCreate()">
        <z-icon zType="plus" class="mr-2" />
        Create Quiz
      </button>
    </z-card>
    }
    }

    @case ('create') {
    <!-- Create / Edit Personal Questions Quiz -->
    <div class="flex items-center gap-2 mb-6">
      <button z-button zType="ghost" zSize="sm" (click)="resetAndGoToList()">
        <z-icon zType="arrow-left" class="mr-1" />
        Back
      </button>
      <h2 class="text-2xl font-bold">{{ editingGameId() ? 'Edit' : 'Create' }} Personal Questions Quiz</h2>
    </div>

    <!-- Quiz Details -->
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Quiz Details</h3>
      <div class="space-y-4">
        <div>
          <label class="text-sm font-medium mb-1 block">Title</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="gameTitle()"
            (input)="gameTitle.set($any($event.target).value)"
            placeholder="e.g., Family & Childhood Memories" />
        </div>
        <div>
          <label class="text-sm font-medium mb-1 block">Description</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="gameDescription()"
            (input)="gameDescription.set($any($event.target).value)"
            placeholder="e.g., Questions about personal life and family" />
        </div>
      </div>
    </z-card>

    <!-- Add New Question -->
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Add Questions</h3>
      <p class="text-sm text-muted-foreground mb-4">
        Write a personal question and provide the correct answer. The patient will type their answer and then
        compare it with yours.
      </p>

      <!-- Suggested questions -->
      <div class="mb-4">
        <p class="text-xs font-medium text-muted-foreground mb-2">Quick add suggestions:</p>
        <div class="flex flex-wrap gap-2">
          @for (suggestion of suggestions; track $index) {
          <button
            class="text-xs px-3 py-1.5 rounded-full border border-border bg-muted hover:bg-primary hover:text-primary-foreground transition-colors"
            (click)="useSuggestion(suggestion)">
            {{ suggestion }}
          </button>
          }
        </div>
      </div>

      <div class="space-y-3">
        <div>
          <label class="text-sm font-medium mb-1 block">Question</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="pendingQuestion()"
            (input)="pendingQuestion.set($any($event.target).value)"
            placeholder="e.g., Where were you born?" />
        </div>
        <div>
          <label class="text-sm font-medium mb-1 block">Correct Answer</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="pendingAnswer()"
            (input)="pendingAnswer.set($any($event.target).value)"
            placeholder="e.g., Tunis, Tunisia" />
        </div>
        <div class="flex gap-2">
          <button z-button [disabled]="!pendingQuestion().trim() || !pendingAnswer().trim()" (click)="addQuestion()">
            <z-icon zType="plus" class="mr-1" />
            Add Question
          </button>
          @if (pendingQuestion().trim() || pendingAnswer().trim()) {
          <button z-button zType="outline" (click)="clearPending()">Clear</button>
          }
        </div>
      </div>
    </z-card>

    <!-- Added Questions List -->
    @if (addedQuestions().length > 0) {
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">Questions in Quiz ({{ addedQuestions().length }})</h3>
      <div class="space-y-3">
        @for (q of addedQuestions(); track $index; let i = $index) {
        <div class="flex items-start gap-3 p-4 rounded-lg border border-border">
          <div class="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 text-primary font-bold text-sm flex-shrink-0 mt-0.5">
            {{ i + 1 }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm">{{ q.questionText }}</p>
            <p class="text-xs text-muted-foreground mt-1">
              Answer: <span class="font-semibold text-primary">{{ q.correctAnswer }}</span>
            </p>
          </div>
          <button z-button zType="ghost" zSize="sm" class="text-destructive flex-shrink-0" (click)="removeQuestion(i)">
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
      @if (addedQuestions().length < 1) {
      <span class="text-destructive">&#x2022; Add at least 1 question</span>
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
export class PersonalQuestionManagerComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly personalQuestionService = inject(PersonalQuestionService);
  private readonly keycloakService = inject(KeycloakService);
  private readonly alertDialog = inject(ZardAlertDialogService);

  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  // View mode
  view = signal<'list' | 'create'>('list');

  // List state
  games = signal<PersonalQuestionGameResponse[]>([]);

  // Create/edit form state
  editingGameId = signal<number | null>(null);
  gameTitle = signal<string>('');
  gameDescription = signal<string>('');
  addedQuestions = signal<EditQuestionItemEntry[]>([]);
  pendingQuestion = signal<string>('');
  pendingAnswer = signal<string>('');
  saving = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');

  // Suggested question templates
  suggestions = [
    'Where were you born?',
    'What is your youngest child\'s name?',
    'What university did you attend?',
    'What is your mother\'s maiden name?',
    'What city did you grow up in?',
    'What was the name of your first pet?',
    'What year did you get married?',
    'What is your favorite childhood memory?',
  ];

  ngOnInit(): void {
    this.loadGames();
  }

  // ─── Question Management ───────────────────────────────

  useSuggestion(suggestion: string): void {
    this.pendingQuestion.set(suggestion);
  }

  addQuestion(): void {
    const question = this.pendingQuestion().trim();
    const answer = this.pendingAnswer().trim();
    if (!question || !answer) return;

    const item: EditQuestionItemEntry = {
      id: null,
      questionText: question,
      correctAnswer: answer,
    };

    this.addedQuestions.update(list => [...list, item]);
    this.clearPending();
  }

  clearPending(): void {
    this.pendingQuestion.set('');
    this.pendingAnswer.set('');
  }

  removeQuestion(index: number): void {
    this.addedQuestions.update(list => list.filter((_, i) => i !== index));
  }

  canSave(): boolean {
    return (
      this.gameTitle().trim().length > 0 &&
      this.addedQuestions().length >= 1 &&
      !this.saving()
    );
  }

  // ─── Create ────────────────────────────────────────────

  startCreate(): void {
    this.resetForm();
    this.editingGameId.set(null);
    this.view.set('create');
  }

  // ─── Edit ──────────────────────────────────────────────

  startEdit(gameId: number): void {
    this.resetForm();
    this.editingGameId.set(gameId);
    this.view.set('create');

    this.personalQuestionService
      .getGameDetail(gameId)
      .pipe(
        tap(detail => {
          this.gameTitle.set(detail.title);
          this.gameDescription.set(detail.description || '');
          this.addedQuestions.set(
            detail.questions.map(q => ({
              id: q.id,
              questionText: q.questionText,
              correctAnswer: q.correctAnswer,
            }))
          );
        }),
        catchError(err => {
          console.error('[PersonalQuestionManager] Failed to load game detail', err);
          this.errorMessage.set('Failed to load game for editing.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Save (create or edit) ─────────────────────────────

  async saveGame(): Promise<void> {
    if (!this.canSave()) return;

    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    try {
      const kc = this.keycloakService.getKeycloakInstance();
      if (kc?.refreshToken) {
        await this.keycloakService.updateToken(30);
      }
    } catch (e) {
      console.warn('[PersonalQuestionManager] Token refresh warning:', e);
    }

    const payload = {
      title: this.gameTitle(),
      description: this.gameDescription(),
      questions: this.addedQuestions(),
    };

    const request$ = this.editingGameId()
      ? this.personalQuestionService.editGame(this.editingGameId()!, payload)
      : this.personalQuestionService.createGame(this.keycloakId, {
          title: payload.title,
          description: payload.description,
          questions: payload.questions.map(q => ({
            questionText: q.questionText,
            correctAnswer: q.correctAnswer,
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
          console.error('[PersonalQuestionManager] Save failed', err);
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
      zDescription: 'Are you sure you want to delete this personal questions quiz? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.personalQuestionService
          .deleteGame(gameId)
          .pipe(
            tap(() => this.loadGames()),
            catchError(err => {
              console.error('[PersonalQuestionManager] Failed to delete', err);
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
    this.addedQuestions.set([]);
    this.pendingQuestion.set('');
    this.pendingAnswer.set('');
    this.errorMessage.set('');
  }

  private loadGames(): void {
    if (!this.keycloakId) return;

    this.personalQuestionService
      .getPatientGames(this.keycloakId)
      .pipe(
        tap(games => this.games.set(games)),
        catchError(err => {
          console.error('[PersonalQuestionManager] Failed to load games', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
