import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef,
  computed
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
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

interface Notification {
  message: string;
  type: 'success' | 'error' | 'info';
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
  templateUrl: './quiz-management.component.html'
})
export class QuizManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // ─── STATE / DATA ─────────────────────────────────────────────
  quizzes = signal<QuizDTO[]>([]);
  displayedQuizzes = signal<QuizDTO[]>([]);
  questions = signal<QuestionDTO[]>([]);
  displayedQuestions = signal<QuestionDTO[]>([]);
  weakTopics = signal<string[]>([]);
  avgScore = signal<number>(0);

  userNeonDbId = signal<number | null>(null);
  activeTab = signal<'quizzes' | 'questions'>('quizzes');
  selectedQuizId = signal<number | null>(null);
  selectedQuizTotalPoints = signal<number>(0);

  // ─── LOADING & MODALS ───────────────────────────────────────
  isLoading = signal<boolean>(false);
  isLoadingQuestions = signal<boolean>(false);
  isLoadingAnswers = signal<boolean>(false);
  showCreateForm = signal<boolean>(false);
  showQuestionForm = signal<boolean>(false);
  showAnswersModal = signal<boolean>(false);
  isEditing = signal<boolean>(false);
  creating = signal<boolean>(false);
  isSubmittingQuestion = signal<boolean>(false);
  isEditingQuestion = signal<boolean>(false);

  notification = signal<Notification | null>(null);
  questionError = signal<string>('');

  // ─── FILTERS ────────────────────────────────────────────────
  searchQuizQuery = '';
  minScoreFilter: number | null = null;
  startDateFilter = '';
  endDateFilter = '';
  searchQuestionQuery = '';
  difficultyFilter: number | null = null;

  // ─── FORMS ──────────────────────────────────────────────────
  newQuiz: Partial<QuizDTO> = { topic: '', caregiverId: 0 };
  newQuestion: Partial<QuestionDTO> = { text: '', difficultyLevel: 1, mediaAttachment: '' };
  newQuestionAnswers: Partial<AnswerDTO>[] = [
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false },
    { text: '', isCorrect: false }
  ];
  correctAnswerIndex = signal<number>(-1);

  selectedQuestionForAnswers = signal<QuestionDTO | null>(null);
  answersForSelectedQuestion = signal<AnswerDTO[]>([]);
  newSingleAnswerText = '';

  // ─── LIFECYCLE ──────────────────────────────────────────────
  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserInfo();
    }
  }

  // ─── NOTIFICATIONS ──────────────────────────────────────────
  notify(message: string, type: 'success' | 'error' | 'info'): void {
    this.notification.set({ message, type });
    setTimeout(() => this.notification.set(null), 4000);
  }

  // ─── INITIALIZATION ─────────────────────────────────────────
  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
          this.newQuiz.caregiverId = userInfo.id;
          this.loadQuizzes();
          this.loadCaregiverStats(userInfo.id);
        }),
        catchError(err => {
          this.notify('Failed to load user info', 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadCaregiverStats(caregiverId: number): void {
    // Weak topics
    this.quizService.getWeakTopicsByCaregiver(caregiverId).subscribe(topics => {
      this.weakTopics.set(topics || []);
    });
    // Average score
    this.quizService.getAverageScoreByCaregiver(caregiverId).subscribe(score => {
      this.avgScore.set(score || 0);
    });
  }

  // ─── QUIZ MANAGEMENT ────────────────────────────────────────
  loadQuizzes(): void {
    const caregiverId = this.userNeonDbId();
    if (!caregiverId) return;

    this.isLoading.set(true);
    // Reset filters
    this.searchQuizQuery = '';
    this.minScoreFilter = null;
    this.startDateFilter = '';
    this.endDateFilter = '';

    this.quizService.getQuizzesByCaregiverId(caregiverId)
      .pipe(
        tap(quizzes => {
          this.quizzes.set(quizzes);
          this.displayedQuizzes.set(quizzes);
        }),
        catchError(err => {
          this.notify('Failed to load quizzes', 'error');
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // FILTERS FOR QUIZZES
  onSearchQuiz(query: string): void {
    if (query.trim().length >= 2) {
      this.quizService.searchQuizzesByTopic(query).subscribe(res => {
        // filter by caregiver id since search is global
        const filtered = res.filter(q => q.caregiverId === this.userNeonDbId());
        this.displayedQuizzes.set(filtered);
      });
    } else if (!query.trim()) {
      this.displayedQuizzes.set(this.quizzes());
    }
  }

  filterByMinScore(): void {
    if (this.minScoreFilter === null) return;
    this.quizService.getQuizzesWithMinScore(this.minScoreFilter).subscribe(res => {
      const filtered = res.filter(q => q.caregiverId === this.userNeonDbId());
      this.displayedQuizzes.set(filtered);
      this.notify(`Quizz avec score >= ${this.minScoreFilter} trouvés`, 'info');
    });
  }

  filterByDateRange(): void {
    if (!this.startDateFilter || !this.endDateFilter) {
      this.notify('Veuillez sélectionner une date de début et de fin', 'error');
      return;
    }
    const start = new Date(this.startDateFilter).toISOString();
    const end = new Date(this.endDateFilter).toISOString();
    this.quizService.getQuizzesByDateRange(start, end).subscribe(res => {
      const filtered = res.filter(q => q.caregiverId === this.userNeonDbId());
      this.displayedQuizzes.set(filtered);
      this.notify(`Quizz filtrés par date`, 'info');
    });
  }

  // CRUD QUIZ
  openCreateQuizModal(): void {
    this.isEditing.set(false);
    this.newQuiz = { topic: '', caregiverId: this.userNeonDbId() ?? 0 };
    this.showCreateForm.set(true);
  }

  editQuiz(quiz: QuizDTO): void {
    this.newQuiz = { id: quiz.id, topic: quiz.topic, caregiverId: quiz.caregiverId };
    this.isEditing.set(true);
    this.showCreateForm.set(true);
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
  }

  saveQuiz(): void {
    if (!this.newQuiz.topic || !this.newQuiz.caregiverId) return;
    this.creating.set(true);

    const obs = (this.isEditing() && this.newQuiz.id)
      ? this.quizService.updateQuiz(this.newQuiz.id, this.newQuiz as QuizDTO)
      : this.quizService.createQuiz(this.newQuiz as QuizDTO);

    obs.pipe(
      tap(() => {
        this.notify(this.isEditing() ? 'Quiz modifié' : 'Quiz créé', 'success');
        this.showCreateForm.set(false);
        this.loadQuizzes();
      }),
      catchError(() => {
        this.notify('Erreur de sauvegarde', 'error');
        return of(null);
      }),
      finalize(() => this.creating.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  deleteQuiz(id: number): void {
    if (!confirm('Voulez-vous vraiment supprimer ce quiz ?')) return;
    this.quizService.deleteQuiz(id).subscribe({
      next: () => {
        this.notify('Quiz supprimé', 'success');
        this.loadQuizzes();
        if (this.selectedQuizId() === id) {
          this.selectedQuizId.set(null);
          this.questions.set([]);
          this.displayedQuestions.set([]);
        }
      },
      error: () => this.notify('Erreur de suppression', 'error')
    });
  }

  viewQuiz(quiz: QuizDTO): void {
    this.selectedQuizId.set(quiz.id!);
    this.setActiveTab('questions');
    this.loadSelectedQuizData();
  }

  // ─── QUESTIONS MANAGEMENT ─────────────────────────────────────

  setActiveTab(tab: 'quizzes' | 'questions'): void {
    this.activeTab.set(tab);
  }

  loadSelectedQuizData(): void {
    const quizId = this.selectedQuizId();
    if (!quizId) {
      this.questions.set([]);
      this.displayedQuestions.set([]);
      this.selectedQuizTotalPoints.set(0);
      return;
    }

    this.isLoadingQuestions.set(true);

    // total points
    this.quizService.calculateTotalPoints(quizId).subscribe(points => this.selectedQuizTotalPoints.set(points || 0));

    this.quizService.getQuestionsByQuizId(quizId)
      .pipe(
        switchMap(questions => {
          if (questions.length === 0) return of([]);
          const questionsWithAnswers$ = questions.map(question => {
            if (!question.id) return of({ ...question, answers: [] });
            return this.quizService.getAnswersByQuestionId(question.id).pipe(
              map(answers => ({ ...question, answers: answers || [] })),
              catchError(() => of({ ...question, answers: [] }))
            );
          });
          return forkJoin(questionsWithAnswers$);
        }),
        tap(questionsWithAnswers => {
          this.questions.set(questionsWithAnswers);
          this.displayedQuestions.set(questionsWithAnswers);
        }),
        catchError(() => {
          this.notify('Erreur de chargement des questions', 'error');
          return of([]);
        }),
        finalize(() => this.isLoadingQuestions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // FILTERS FOR QUESTIONS
  onSearchQuestion(query: string): void {
    if (query.trim().length >= 2) {
      this.quizService.searchQuestions(query).subscribe(res => {
        const filtered = res.filter(q => q.quizId === this.selectedQuizId());
        this.displayedQuestions.set(filtered);
      });
    } else if (!query.trim()) {
      this.displayedQuestions.set(this.questions());
    }
  }

  filterQuestionsByDifficulty(): void {
    if (this.difficultyFilter === null) {
      this.displayedQuestions.set(this.questions());
      return;
    }
    const quizId = this.selectedQuizId();
    if (!quizId) return;

    this.quizService.getQuestionsByQuizAndDifficulty(quizId, this.difficultyFilter).subscribe(res => {
      this.displayedQuestions.set(res);
    });
  }

  // CRUD QUESTION
  deleteAllQuestionsInQuiz(): void {
    const quizId = this.selectedQuizId();
    if (!quizId || !confirm('Supprimer TOUTES les questions ? Irréversible.')) return;
    this.quizService.deleteQuestionsByQuizId(quizId).subscribe(() => {
      this.notify('Toutes les questions supprimées', 'success');
      this.loadSelectedQuizData();
    });
  }

  editQuestion(question: QuestionDTO): void {
    this.newQuestion = { id: question.id, text: question.text, difficultyLevel: question.difficultyLevel, mediaAttachment: question.mediaAttachment || '' };
    if (question.answers && question.answers.length > 0) {
      this.newQuestionAnswers = question.answers.map(a => ({ id: a.id, text: a.text, isCorrect: a.isCorrect, explanation: a.explanation }));
      this.correctAnswerIndex.set(question.answers.findIndex(a => a.isCorrect));
    } else {
      this.newQuestionAnswers = [{ text: '', isCorrect: false }, { text: '', isCorrect: false }];
      this.correctAnswerIndex.set(-1);
    }
    this.isEditingQuestion.set(true);
    this.showQuestionForm.set(true);
  }

  deleteQuestion(id: number): void {
    if (!confirm('Supprimer cette question ?')) return;
    this.quizService.deleteQuestion(id).subscribe(() => {
      this.notify('Question supprimée', 'success');
      this.loadSelectedQuizData();
    });
  }

  cancelQuestionForm(): void {
    this.showQuestionForm.set(false);
    this.isEditingQuestion.set(false);
    this.newQuestion = { text: '', difficultyLevel: 1, mediaAttachment: '' };
    this.newQuestionAnswers = [{ text: '', isCorrect: false }, { text: '', isCorrect: false }, { text: '', isCorrect: false }, { text: '', isCorrect: false }];
    this.correctAnswerIndex.set(-1);
    this.questionError.set('');
  }

  submitQuestion(): void {
    const quizId = this.selectedQuizId();
    if (!quizId) return;
    if (!this.newQuestion.text?.trim()) { this.questionError.set('Le texte est requis'); return; }

    const correctIndex = this.correctAnswerIndex();
    if (correctIndex === -1 && !this.isEditingQuestion()) { this.questionError.set('Sélectionnez la bonne réponse'); return; }

    this.isSubmittingQuestion.set(true);

    if (this.isEditingQuestion() && this.newQuestion.id) {
      this.quizService.updateQuestion(this.newQuestion.id, { ...this.newQuestion, quizId } as QuestionDTO).subscribe(() => {
        this.notify('Question modifiée', 'success');
        this.cancelQuestionForm();
        this.loadSelectedQuizData();
        this.isSubmittingQuestion.set(false);
      });
    } else {
      this.quizService.createQuestion({ ...this.newQuestion, quizId } as QuestionDTO).pipe(
        switchMap(createdQ => {
          // BATCH CREATE ANSWERS using backend endpoint !
          const answersToCreate = this.newQuestionAnswers
            .filter(a => a.text?.trim())
            .map((a, i) => ({
              text: a.text!.trim(),
              isCorrect: i === correctIndex,
              explanation: i === correctIndex ? 'Correct' : 'Faux',
              questionId: createdQ.id!
            }));
          if (answersToCreate.length > 0) {
            return this.quizService.createAnswersBatch(answersToCreate);
          }
          return of([]);
        })
      ).subscribe({
        next: () => {
          this.notify('Question + Réponses créées', 'success');
          this.cancelQuestionForm();
          this.loadSelectedQuizData();
          this.isSubmittingQuestion.set(false);
        },
        error: () => {
          this.questionError.set('Erreur création question batch');
          this.isSubmittingQuestion.set(false);
        }
      });
    }
  }

  // Quick form answers
  addAnswer(): void {
    if (this.newQuestionAnswers.length < 6) this.newQuestionAnswers.push({ text: '', isCorrect: false });
  }
  removeAnswer(idx: number): void {
    this.newQuestionAnswers.splice(idx, 1);
    if (this.correctAnswerIndex() === idx) this.correctAnswerIndex.set(-1);
    else if (idx < this.correctAnswerIndex()) this.correctAnswerIndex.set(this.correctAnswerIndex() - 1);
  }
  setCorrectAnswer(idx: number): void { this.correctAnswerIndex.set(idx); }


  // ─── ANSWERS MANAGEMENT (MODAL) ───────────────────────────────
  manageAnswers(question: QuestionDTO): void {
    this.selectedQuestionForAnswers.set(question);
    this.showAnswersModal.set(true);
    this.loadAnswersForSelectedQuestion(question.id!);
  }

  loadAnswersForSelectedQuestion(questionId: number): void {
    this.isLoadingAnswers.set(true);
    this.quizService.getAnswersByQuestionId(questionId).subscribe({
      next: res => {
        this.answersForSelectedQuestion.set(res);
        this.isLoadingAnswers.set(false);
      },
      error: () => this.isLoadingAnswers.set(false)
    });
  }

  addSingleAnswerToQuestion(): void {
    if (!this.newSingleAnswerText.trim()) return;
    const qId = this.selectedQuestionForAnswers()?.id;
    if (!qId) return;

    this.quizService.createAnswer({
      text: this.newSingleAnswerText.trim(),
      isCorrect: false,
      questionId: qId,
      explanation: ''
    }).subscribe(() => {
      this.notify('Choix de réponse ajouté', 'success');
      this.newSingleAnswerText = '';
      this.loadAnswersForSelectedQuestion(qId);
      this.loadSelectedQuizData(); // sync main table
    });
  }

  updateAnswerText(answerId: number, text: string): void {
    const ans = this.answersForSelectedQuestion().find(a => a.id === answerId);
    if (!ans || ans.text === text.trim()) return;
    this.quizService.updateAnswer(answerId, { ...ans, text: text.trim() }).subscribe(() => {
      this.notify('Texte mis à jour', 'info');
      this.loadAnswersForSelectedQuestion(ans.questionId);
    });
  }

  updateAnswerExplanation(answerId: number, explanation: string): void {
    const ans = this.answersForSelectedQuestion().find(a => a.id === answerId);
    if (!ans || ans.explanation === explanation.trim()) return;
    this.quizService.updateAnswer(answerId, { ...ans, explanation: explanation.trim() }).subscribe(() => {
      this.notify('Explication mise à jour', 'info');
      this.loadAnswersForSelectedQuestion(ans.questionId);
    });
  }

  setCorrectAnswerById(answerId: number): void {
    const ans = this.answersForSelectedQuestion().find(a => a.id === answerId);
    if (!ans) return;

    // First ensure old correct answers are put to false
    const qId = ans.questionId;
    const answers = this.answersForSelectedQuestion();

    // In a real scenario we could use a batch update or loop, I'll update the specific target
    const updates = answers.map(a =>
      this.quizService.updateAnswer(a.id!, { ...a, isCorrect: a.id === answerId })
    );

    forkJoin(updates).subscribe(() => {
      this.notify('Bonne réponse définie', 'success');
      this.loadAnswersForSelectedQuestion(qId);
    });
  }

  deleteAnswer(answerId: number): void {
    if (!confirm('Supprimer cette réponse ?')) return;
    const qId = this.answersForSelectedQuestion()[0]?.questionId;
    this.quizService.deleteAnswer(answerId).subscribe(() => {
      this.notify('Réponse supprimée', 'success');
      if (qId) this.loadAnswersForSelectedQuestion(qId);
      this.loadSelectedQuizData(); // sync
    });
  }

  checkAnswerCorrectness(answerId: number): void {
    // API Call to test /is-correct endpoint
    this.quizService.isAnswerCorrect(answerId).subscribe(res => {
      this.notify(`Backend vérifie: ${res.message}`, res.value ? 'success' : 'info');
    });
  }
}
