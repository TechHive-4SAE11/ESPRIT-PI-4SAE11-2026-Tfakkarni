import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { QuizService } from '@/core/services/quiz.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-quiz-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardBadgeComponent,
    ZardTableImports,
    ZardSkeletonComponent
  ],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <h2 class="text-2xl font-bold">Quiz Management</h2>
        <button z-button (click)="showCreateForm.set(true)">
          <z-icon zType="plus" class="mr-2" />
          Create Quiz
        </button>
      </div>

      <!-- Create Quiz Form -->
      @if (showCreateForm()) {
      <z-card class="p-6">
        <h3 class="text-lg font-semibold mb-4">Create New Quiz</h3>
        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium mb-1 block">Topic</label>
            <input
              class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              [(ngModel)]="newQuiz.topic"
              placeholder="e.g., Memory Test, Family Names"
            />
          </div>
          <div class="flex gap-3">
            <button z-button [disabled]="!newQuiz.topic || creating()" (click)="createQuiz()">
              @if (creating()) {
              <z-icon zType="loader-2" class="mr-2 animate-spin" />
              Creating...
              } @else {
              <z-icon zType="check" class="mr-2" />
              Create
              }
            </button>
            <button z-button zType="outline" (click)="cancelCreate()">Cancel</button>
          </div>
        </div>
      </z-card>
      }

      <!-- Stats -->
      <div class="grid gap-4 md:grid-cols-3">
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Total Quizzes</p>
              <p class="text-3xl font-bold">{{ quizzes().length }}</p>
            </div>
            <z-icon zType="brain" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Average Score</p>
              <p class="text-3xl font-bold">{{ averageScore() | number:'1.0-0' }}%</p>
            </div>
            <z-icon zType="target" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Recent Quizzes</p>
              <p class="text-3xl font-bold">{{ recentQuizzes().length }}</p>
            </div>
            <z-icon zType="clock" class="text-primary h-8 w-8" />
          </div>
        </z-card>
      </div>

      <!-- Quiz List -->
      @if (isLoading()) {
      <z-skeleton class="h-32 w-full" />
      } @else if (quizzes().length > 0) {
      <z-card>
        <div class="p-6">
          <table z-table>
            <thead z-table-header>
              <tr z-table-row>
                <th z-table-head>Topic</th>
                <th z-table-head>Score</th>
                <th z-table-head>Date</th>
                <th z-table-head>Questions</th>
                <th z-table-head>Actions</th>
              </tr>
            </thead>
            <tbody z-table-body>
              @for (quiz of quizzes(); track quiz.id) {
              <tr z-table-row>
                <td z-table-cell class="font-medium">{{ quiz.topic }}</td>
                <td z-table-cell>
                  @if (quiz.totalScore !== null && quiz.totalScore !== undefined) {
                  <z-badge zType="secondary">{{ quiz.totalScore }}</z-badge>
                  } @else {
                  <span class="text-muted-foreground">-</span>
                  }
                </td>
                <td z-table-cell class="text-muted-foreground">
                  {{ quiz.dateTaken | date:'short' }}
                </td>
                <td z-table-cell>
                  {{ quiz.questions?.length ?? 0 }}
                </td>
                <td z-table-cell>
                  <div class="flex gap-2">
                    <button z-button zType="ghost" zSize="sm" (click)="viewQuiz(quiz)">
                      <z-icon zType="eye" class="mr-1" />
                      View
                    </button>
                    <button z-button zType="ghost" zSize="sm" (click)="editQuiz(quiz)">
                      <z-icon zType="edit" class="mr-1" />
                      Edit
                    </button>
                    <button z-button zType="destructive" zSize="sm" (click)="deleteQuiz(quiz.id!)">
                      <z-icon zType="trash-2" class="mr-1" />
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
              }
            </tbody>
          </table>
        </div>
      </z-card>
      } @else {
      <z-card class="p-12 text-center">
        <z-icon zType="brain" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 class="font-semibold mb-2">No quizzes yet</h3>
        <p class="text-muted-foreground mb-4">Create your first quiz to get started!</p>
        <button z-button (click)="showCreateForm.set(true)">
          <z-icon zType="plus" class="mr-2" />
          Create Quiz
        </button>
      </z-card>
      }
    </div>
  `,
})
export class QuizManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // State
  quizzes = signal<QuizDTO[]>([]);
  isLoading = signal<boolean>(false);
  creating = signal<boolean>(false);
  showCreateForm = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);

  newQuiz: Partial<QuizDTO> = {
    topic: '',
    caregiverId: 0
  };

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserInfo();
    }
  }

  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
          this.newQuiz.caregiverId = userInfo.id;
          this.loadQuizzes();
        }),
        catchError(err => {
          console.error('[QuizManagement] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadQuizzes(): void {
    const caregiverId = this.userNeonDbId();
    if (!caregiverId) return;

    this.isLoading.set(true);
    this.quizService.getQuizzesByCaregiverId(caregiverId)
      .pipe(
        tap(quizzes => this.quizzes.set(quizzes)),
        catchError(err => {
          console.error('[QuizManagement] Failed to load quizzes', err);
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  createQuiz(): void {
    if (!this.newQuiz.topic || !this.newQuiz.caregiverId) return;

    this.creating.set(true);
    this.quizService.createQuiz(this.newQuiz as QuizDTO)
      .pipe(
        tap(() => {
          this.showCreateForm.set(false);
          this.newQuiz = { topic: '', caregiverId: this.userNeonDbId() ?? 0 };
          this.loadQuizzes();
        }),
        catchError(err => {
          console.error('[QuizManagement] Failed to create quiz', err);
          return of(null);
        }),
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.newQuiz = { topic: '', caregiverId: this.userNeonDbId() ?? 0 };
  }

  viewQuiz(quiz: QuizDTO): void {
    // TODO: Implement view quiz details
    console.log('View quiz:', quiz);
  }

  editQuiz(quiz: QuizDTO): void {
    // TODO: Implement edit quiz
    console.log('Edit quiz:', quiz);
  }

  deleteQuiz(id: number): void {
    if (!confirm('Are you sure you want to delete this quiz?')) return;

    this.quizService.deleteQuiz(id)
      .pipe(
        tap(() => this.loadQuizzes()),
        catchError(err => {
          console.error('[QuizManagement] Failed to delete quiz', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  get averageScore(): () => number {
    return () => {
      const completedQuizzes = this.quizzes().filter(q => q.totalScore !== null && q.totalScore !== undefined);
      if (completedQuizzes.length === 0) return 0;
      const sum = completedQuizzes.reduce((acc, q) => acc + (q.totalScore ?? 0), 0);
      return sum / completedQuizzes.length;
    };
  }

  get recentQuizzes(): () => QuizDTO[] {
    return () => {
      return this.quizzes()
        .filter(q => q.dateTaken)
        .sort((a, b) => {
          const dateA = new Date(a.dateTaken!).getTime();
          const dateB = new Date(b.dateTaken!).getTime();
          return dateB - dateA;
        })
        .slice(0, 5);
    };
  }
}
