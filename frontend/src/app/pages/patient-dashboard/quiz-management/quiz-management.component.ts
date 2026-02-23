import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap, map, forkJoin } from 'rxjs';

import { QuizService } from '@/core/services/quiz.service';
import { UserApiService } from '@/core/services/user-api.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';

// Interface simple pour les tabs
interface TabItem {
  label: string;
  value: string;
}

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
                  <span class="mr-2">⏳</span>
                  Creating...
                } @else {
                  <span class="mr-2">✓</span>
                  Create
                }
              </button>
              <button z-button zType="outline" (click)="cancelCreate()">Cancel</button>
            </div>
          </div>
        </z-card>
      }

      <!-- Custom Tabs -->
      <div class="border-b border-border mb-4">
        <div class="flex gap-4">
          @for (tab of tabs; track tab.value) {
            <button
              class="px-4 py-2 text-sm font-medium transition-colors relative"
              [class.text-primary]="activeTab() === tab.value"
              [class.text-muted-foreground]="activeTab() !== tab.value"
              (click)="activeTab.set(tab.value)">
              {{ tab.label }}
              @if (activeTab() === tab.value) {
                <span class="absolute bottom-0 left-0 right-0 h-0.5 bg-primary"></span>
              }
            </button>
          }
        </div>
      </div>

      <!-- Quizzes Tab -->
      @if (activeTab() === 'quizzes') {
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
                          <span class="px-2 py-1 bg-secondary text-secondary-foreground rounded-full text-xs">
                            {{ quiz.totalScore }}
                          </span>
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
                            <span class="mr-1">🗑️</span>
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
      }

      <!-- Questions Tab -->
      @if (activeTab() === 'questions') {
        <z-card>
          <div class="p-6">
            <div class="mb-4 flex gap-4">
              <select
                class="px-3 py-2 border border-border rounded-md bg-background text-foreground"
                [(ngModel)]="selectedQuizId"
                (change)="loadQuestionsForQuiz()">
                <option [ngValue]="null">Select a quiz</option>
                @for (quiz of quizzes(); track quiz.id) {
                  <option [ngValue]="quiz.id">{{ quiz.topic }}</option>
                }
              </select>

              @if (selectedQuizId()) {
                <button z-button (click)="showQuestionForm.set(true)">
                  <z-icon zType="plus" class="mr-2" />
                  Add Question
                </button>
              }
            </div>

            @if (selectedQuizId() && questions().length > 0) {
              <table z-table>
                <thead z-table-header>
                <tr z-table-row>
                  <th z-table-head>Question</th>
                  <th z-table-head>Difficulty</th>
                  <th z-table-head>Answers</th>
                  <th z-table-head>Actions</th>
                </tr>
                </thead>
                <tbody z-table-body>
                  @for (question of questions(); track question.id) {
                    <tr z-table-row>
                      <td z-table-cell>{{ question.text }}</td>
                      <td z-table-cell>
                        @if (question.difficultyLevel === 1) {
                          <span class="px-2 py-1 bg-green-100 text-green-700 rounded-full text-xs">
                            Easy
                          </span>
                        } @else if (question.difficultyLevel === 2) {
                          <span class="px-2 py-1 bg-yellow-100 text-yellow-700 rounded-full text-xs">
                            Medium
                          </span>
                        } @else {
                          <span class="px-2 py-1 bg-red-100 text-red-700 rounded-full text-xs">
                            Hard
                          </span>
                        }
                      </td>
                      <td z-table-cell>
                        @if (question.answers) {
                          {{ question.answers.length }}
                        } @else {
                          <span class="text-yellow-600">Loading...</span>
                        }
                      </td>
                      <td z-table-cell>
                        <div class="flex gap-2">
                          <button z-button zType="ghost" zSize="sm" (click)="manageAnswers(question)">
                            <span class="mr-1">📋</span>
                            Manage
                          </button>
                          <button z-button zType="destructive" zSize="sm" (click)="deleteQuestion(question.id!)">
                            <span class="mr-1">🗑️</span>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            } @else if (selectedQuizId()) {
              <p class="text-center text-muted-foreground py-8">No questions for this quiz yet.</p>
            }
          </div>
        </z-card>
      }

      <!-- Question Form Modal -->
      @if (showQuestionForm()) {
        <div class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <z-card class="w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 class="text-xl font-bold mb-4">Add Question</h2>

            <div class="space-y-4">
              <div>
                <label class="text-sm font-medium mb-1 block">Question Text</label>
                <textarea
                  class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
                  [(ngModel)]="newQuestion.text"
                  rows="3"></textarea>
              </div>

              <div>
                <label class="text-sm font-medium mb-1 block">Difficulty Level</label>
                <select
                  class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
                  [(ngModel)]="newQuestion.difficultyLevel">
                  <option [ngValue]="1">Level 1 (Easy)</option>
                  <option [ngValue]="2">Level 2 (Medium)</option>
                  <option [ngValue]="3">Level 3 (Hard)</option>
                </select>
              </div>

              <div>
                <label class="text-sm font-medium mb-1 block">Media Attachment (Optional)</label>
                <input
                  type="url"
                  class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground"
                  [(ngModel)]="newQuestion.mediaAttachment"
                  placeholder="https://example.com/image.jpg" />
              </div>

              <div class="border-t pt-4">
                <h3 class="font-semibold mb-2">Answers</h3>
                <p class="text-sm text-muted-foreground mb-3">Add 4 answers with one correct</p>

                @for (answer of newQuestionAnswers; track i; let i = $index) {
                  <div class="flex gap-2 mb-2">
                    <input
                      type="text"
                      class="flex-1 px-3 py-2 border border-border rounded-md bg-background text-foreground"
                      [(ngModel)]="answer.text"
                      [placeholder]="'Answer ' + (i + 1)" />
                    <label class="flex items-center gap-1">
                      <input
                        type="radio"
                        name="correctAnswer"
                        [checked]="answer.isCorrect"
                        (change)="setCorrectAnswerById(answer.id!)"  />
                      Correct
                    </label>
                  </div>
                }
              </div>

              @if (questionError()) {
                <div class="p-3 bg-destructive/10 text-destructive text-sm rounded">
                  {{ questionError() }}
                </div>
              }

              <div class="flex gap-3">
                <button z-button [disabled]="isSubmittingQuestion()" (click)="submitQuestion()">
                  @if (isSubmittingQuestion()) {
                    <span class="mr-2">⏳</span>
                    Saving...
                  } @else {
                    <span class="mr-2">✓</span>
                    Save Question
                  }
                </button>
                <button z-button zType="outline" (click)="cancelQuestionForm()">Cancel</button>
              </div>
            </div>
          </z-card>
        </div>
      }

      <!-- Answers Management Modal -->
      @if (showAnswersModal()) {
        <div class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <z-card class="w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex justify-between items-center mb-4">
              <h2 class="text-xl font-bold">Manage Answers</h2>
              <button (click)="showAnswersModal.set(false)" class="text-gray-500 hover:text-gray-700">
                <z-icon zType="x" />
              </button>
            </div>

            <p class="text-sm text-muted-foreground mb-4">
              Question: {{ selectedQuestionForAnswers()?.text }}
            </p>

            @if (isLoadingAnswers()) {
              <div class="space-y-3">
                @for (i of [1,2,3,4]; track i) {
                  <div class="h-16 bg-gray-200 animate-pulse rounded"></div>
                }
              </div>
            } @else if (answersForSelectedQuestion().length > 0) {
              <div class="space-y-3">
                @for (answer of answersForSelectedQuestion(); track answer.id) {
                  <div class="flex items-center gap-3 p-3 border rounded-lg">
                    <div class="flex-1">
                      <input
                        type="text"
                        class="w-full px-3 py-2 border rounded"
                        [value]="answer.text"
                        #answerInput
                        (blur)="updateAnswerText(answer.id!, answerInput.value)" />
                    </div>
                    <div class="flex items-center gap-2">
                      <label class="flex items-center gap-1">
                        <input
                          type="radio"
                          name="correctAnswerModal"
                          [checked]="answer.isCorrect"
                          (change)="setCorrectAnswer(answer.id!)" />
                        Correct
                      </label>
                      <button
                        (click)="deleteAnswer(answer.id!)"
                        class="text-red-500 hover:text-red-700">
                        <z-icon zType="trash" />
                      </button>
                    </div>
                  </div>
                }
              </div>
            } @else {
              <p class="text-center py-8 text-muted-foreground">No answers found</p>
            }

            <div class="mt-6 flex justify-end">
              <button z-button (click)="showAnswersModal.set(false)">Close</button>
            </div>
          </z-card>
        </div>
      }

    </div> <!-- Fin du div principal -->
  `
})
export class QuizManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // Tabs
  tabs: TabItem[] = [
    { label: 'Quizzes', value: 'quizzes' },
    { label: 'Questions', value: 'questions' }
  ];

  // State
  quizzes = signal<QuizDTO[]>([]);
  questions = signal<QuestionDTO[]>([]);
  isLoading = signal<boolean>(false);
  isEditing = signal<boolean>(false);
  creating = signal<boolean>(false);
  isSubmittingQuestion = signal<boolean>(false);
  showCreateForm = signal<boolean>(false);
  showQuestionForm = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);
  questionError = signal<string>('');
  selectedQuestionForAnswers = signal<QuestionDTO | null>(null);
  showAnswersModal = signal<boolean>(false);
  answersForQuestion = signal<AnswerDTO[]>([]);
  isLoadingQuestions = signal<boolean>(false);
  answersForSelectedQuestion = signal<AnswerDTO[]>([]);
  isLoadingAnswers = signal<boolean>(false);
  correctAnswerIndex = signal<number>(-1);

  // UI State
  activeTab = signal<string>('quizzes');
  selectedQuizId = signal<number | null>(null);


  // Forms
  newQuiz: Partial<QuizDTO> = {
    topic: '',
    caregiverId: 0
  };

  newQuestion: Partial<QuestionDTO> = {
    text: '',
    difficultyLevel: 1,
    mediaAttachment: ''
  };

  newQuestionAnswers: Partial<AnswerDTO>[] = [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false }
  ];

  // Computed
  averageScore = computed(() => {
    const completedQuizzes = this.quizzes().filter(q => q.totalScore !== null && q.totalScore !== undefined);
    if (completedQuizzes.length === 0) return 0;
    const sum = completedQuizzes.reduce((acc, q) => acc + (q.totalScore ?? 0), 0);
    return sum / completedQuizzes.length;
  });

  recentQuizzes = computed(() => {
    return this.quizzes()
      .filter(q => q.dateTaken)
      .sort((a, b) => {
        const dateA = new Date(a.dateTaken!).getTime();
        const dateB = new Date(b.dateTaken!).getTime();
        return dateB - dateA;
      })
      .slice(0, 5);
  });

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

  loadQuestionsForQuiz(): void {
    const quizId = this.selectedQuizId();
    if (!quizId) {
      this.questions.set([]);
      return;
    }

    this.isLoadingQuestions.set(true);
    this.quizService.getQuestionsByQuizId(quizId)
      .pipe(
        switchMap(questions => {
          // Pour chaque question, charge ses réponses
          const questionsWithAnswers$ = questions.map(question =>
            this.quizService.getAnswersByQuestionId(question.id!).pipe(
              map(answers => ({
                ...question,
                answers: answers
              }))
            )
          );
          return forkJoin(questionsWithAnswers$);
        }),
        tap(questionsWithAnswers => {
          console.log('Questions with answers:', questionsWithAnswers);
          this.questions.set(questionsWithAnswers);
        }),
        catchError(err => {
          console.error('Failed to load questions with answers', err);
          return of([]);
        }),
        finalize(() => this.isLoadingQuestions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  createQuiz(): void {
    if (!this.newQuiz.topic || !this.newQuiz.caregiverId) return;

    this.creating.set(true);

    const observable = this.isEditing() && this.newQuiz.id
      ? this.quizService.updateQuiz(this.newQuiz.id, this.newQuiz as QuizDTO)
      : this.quizService.createQuiz(this.newQuiz as QuizDTO);

    observable.pipe(
      tap(() => {
        this.showCreateForm.set(false);
        this.isEditing.set(false);
        this.newQuiz = { topic: '', caregiverId: this.userNeonDbId() ?? 0 };
        this.loadQuizzes();
      }),
      catchError(err => {
        console.error('[QuizManagement] Failed to save quiz', err);
        return of(null);
      }),
      finalize(() => this.creating.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.isEditing.set(false);
    this.newQuiz = { topic: '', caregiverId: this.userNeonDbId() ?? 0 };
  }

  viewQuiz(quiz: QuizDTO): void {
    this.selectedQuizId.set(quiz.id!);
    this.activeTab.set('questions');
    this.loadQuestionsForQuiz();
  }

  editQuiz(quiz: QuizDTO): void {
    console.log('Edit quiz:', quiz);
    this.newQuiz = {
      id: quiz.id,
      topic: quiz.topic,
      caregiverId: quiz.caregiverId
    };

    // Afficher le formulaire en mode édition
    this.showCreateForm.set(true);
    this.isEditing.set(true);
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

  deleteQuestion(id: number): void {
    if (!confirm('Are you sure you want to delete this question?')) return;

    this.quizService.deleteQuestion(id)
      .pipe(
        tap(() => this.loadQuestionsForQuiz()),
        catchError(err => {
          console.error('[QuizManagement] Failed to delete question', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  manageAnswers(question: QuestionDTO): void {
    console.log('Opening answers for question:', question);

    this.selectedQuestionForAnswers.set(question);
    this.showAnswersModal.set(true);
    this.loadAnswersForQuestion(question.id!);
  }


  loadAnswersForQuestion(questionId: number): void {
    this.isLoadingAnswers.set(true);
    this.quizService.getAnswersByQuestionId(questionId)
      .pipe(
        tap(answers => {
          console.log('Answers loaded:', answers);
          this.answersForSelectedQuestion.set(answers);
        }),
        catchError(err => {
          console.error('Failed to load answers', err);
          return of([]);
        }),
        finalize(() => this.isLoadingAnswers.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  updateAnswer(answer: AnswerDTO): void {
    // Implémente la mise à jour d'une réponse
    this.quizService.updateAnswer(answer.id!, answer)
      .pipe(
        tap(updated => {
          console.log('Answer updated:', updated);
          // Recharger les réponses
          this.loadAnswersForQuestion(this.selectedQuestionForAnswers()!.id!);
        }),
        catchError(err => {
          console.error('Failed to update answer', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }


  deleteAnswer(answerId: number): void {
    if (!confirm('Delete this answer?')) return;

    this.quizService.deleteAnswer(answerId)
      .pipe(
        tap(() => {
          console.log('Answer deleted');
          this.loadAnswersForQuestion(this.selectedQuestionForAnswers()!.id!);
        }),
        catchError(err => {
          console.error('Failed to delete answer', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  updateAnswerText(answerId: number, newText: string): void {
    const answer = this.answersForSelectedQuestion().find(a => a.id === answerId);
    if (answer && answer.text !== newText.trim()) {
      answer.text = newText.trim();
      this.updateAnswer(answer);
    }
  }

  setCorrectAnswer(index: number): void {
    console.log('Setting correct answer index:', index);

    // Met à jour l'index
    this.correctAnswerIndex.set(index);

    // Met à jour SEULEMENT la propriété isCorrect sans toucher au texte
    this.newQuestionAnswers = this.newQuestionAnswers.map((answer, i) => {
      // Garde le texte existant, met seulement à jour isCorrect
      return {
        text: answer.text,  // ← GARDE le texte existant
        isCorrect: i === index
      };
    });

    console.log('Updated answers:', this.newQuestionAnswers);
  }

  setCorrectAnswerById(answerId: number): void {
    console.log('Setting correct answer by ID:', answerId);

    const answers = this.answersForSelectedQuestion();

    // Mettre à jour toutes les réponses
    const updatedAnswers = answers.map(a => ({
      ...a,
      isCorrect: a.id === answerId
    }));

    // Sauvegarder chaque modification
    forkJoin(
      updatedAnswers.map(a => this.quizService.updateAnswer(a.id!, a))
    ).pipe(
      tap(() => {
        console.log('Correct answer updated by ID');
        this.answersForSelectedQuestion.set(updatedAnswers);
      }),
      catchError(err => {
        console.error('Failed to update correct answer', err);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  cancelQuestionForm(): void {
    this.showQuestionForm.set(false);
    this.newQuestion = {
      text: '',
      difficultyLevel: 1,
      mediaAttachment: ''
    };
    this.newQuestionAnswers = [
      { text: '', isCorrect: false },
      { text: '', isCorrect: false },
      { text: '', isCorrect: false },
      { text: '', isCorrect: false }
    ];
    this.correctAnswerIndex.set(-1);
    this.questionError.set('');
  }

  submitQuestion(): void {
    const quizId = this.selectedQuizId();
    if (!quizId) {
      this.questionError.set('No quiz selected');
      return;
    }

    // Log des données avant envoi
    console.log('=== SUBMITTING QUESTION ===');
    console.log('Quiz ID:', quizId);
    console.log('Question:', this.newQuestion);
    console.log('Answers:', this.newQuestionAnswers);

    // Validation stricte
    if (!this.newQuestion.text?.trim()) {
      this.questionError.set('Question text is required');
      return;
    }

    // Vérifier que toutes les réponses ont du texte
    for (let i = 0; i < this.newQuestionAnswers.length; i++) {
      const answer = this.newQuestionAnswers[i];
      if (!answer.text?.trim()) {
        this.questionError.set(`Answer ${i + 1} cannot be empty`);
        return;
      }
    }

    // Vérifier qu'une seule réponse correcte
    const correctIndex = this.correctAnswerIndex();
    if (correctIndex === -1) {
      this.questionError.set('Please select a correct answer');
      return;
    }
    // Met à jour seulement isCorrect, garde les textes
    this.newQuestionAnswers = this.newQuestionAnswers.map((a, i) => ({
      text: a.text,  // ← GARDE le texte
      isCorrect: i === correctIndex
    }));

    this.isSubmittingQuestion.set(true);
    this.questionError.set('');

    const question: QuestionDTO = {
      text: this.newQuestion.text!,
      difficultyLevel: this.newQuestion.difficultyLevel!,
      mediaAttachment: this.newQuestion.mediaAttachment || '',
      quizId: quizId
    };



    this.quizService.createQuestion(question)
      .pipe(
        tap(createdQuestion => {
          console.log('✅ Question created successfully:', createdQuestion);
        }),
        switchMap(createdQuestion => {
          // Créer les réponses une par une au lieu d'utiliser batch
          const answerObservables = this.newQuestionAnswers.map(a => {
            const answerDto: AnswerDTO = {
              text: a.text!.trim(),
              isCorrect: a.isCorrect === true,
              explanation: a.isCorrect ? 'Correct answer' : 'Incorrect answer',
              questionId: createdQuestion.id!
            };
            return this.quizService.createAnswer(answerDto);
          });

          console.log(`📤 Creating ${answerObservables.length} answers individually...`);
          return forkJoin(answerObservables);
        }),
        tap(responses => {
          console.log('✅ All answers created successfully:', responses);
          this.cancelQuestionForm();
          this.loadQuestionsForQuiz();
        }),
        catchError(err => {
          console.error('❌ ERROR in submitQuestion:', err);
          if (err.error) {
            console.error('Server error details:', err.error);
            this.questionError.set(`Server error: ${err.error.message || err.error.error || 'Unknown error'}`);
          } else {
            this.questionError.set('Failed to create question. Check console.');
          }
          return of(null);
        }),
        finalize(() => this.isSubmittingQuestion.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
