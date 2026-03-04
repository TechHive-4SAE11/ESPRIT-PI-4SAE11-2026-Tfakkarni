import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  QuizDTO,
  QuestionDTO,
  AnswerDTO,
  ValidationRequestDTO,
  ValidationResponseDTO,
  SubmissionRequestDTO,
  SubmissionResponseDTO,
  CountResponseDTO,
  BooleanResponseDTO
} from '@/core/models/quiz.model';

@Injectable({
  providedIn: 'root',
})
export class QuizService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/quiz`;

  constructor(private readonly http: HttpClient) { }

  // ─── Quiz CRUD ──────────────────────────────────────────────

  createQuiz(quiz: QuizDTO): Observable<QuizDTO> {
    return this.http.post<QuizDTO>(this.baseUrl, quiz);
  }

  updateQuiz(id: number, quiz: QuizDTO): Observable<QuizDTO> {
    return this.http.put<QuizDTO>(`${this.baseUrl}/${id}`, quiz);
  }

  deleteQuiz(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getQuizById(id: number): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.baseUrl}/${id}`);
  }

  getAllQuizzes(): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(this.baseUrl);
  }

  getQuizzesByCaregiverId(caregiverId: number): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(`${this.baseUrl}/caregiver/${caregiverId}`);
  }

  searchQuizzesByTopic(topic: string): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(`${this.baseUrl}/search?topic=${encodeURIComponent(topic)}`);
  }

  getRecentQuizzesByCaregiver(caregiverId: number, limit: number = 5): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(`${this.baseUrl}/caregiver/${caregiverId}/recent?limit=${limit}`);
  }

  getQuizzesByDateRange(startDate: string, endDate: string): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(`${this.baseUrl}/date-range`, {
      params: { startDate, endDate }
    });
  }

  getQuizzesWithMinScore(minScore: number): Observable<QuizDTO[]> {
    return this.http.get<QuizDTO[]>(`${this.baseUrl}/min-score/${minScore}`);
  }

  getQuizCountByCaregiver(caregiverId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/caregiver/${caregiverId}/count`);
  }

  startQuiz(id: number): Observable<QuizDTO> {
    return this.http.post<QuizDTO>(`${this.baseUrl}/${id}/start`, {});
  }

  completeQuiz(id: number, score: number, levelReached?: number): Observable<QuizDTO> {
    return this.http.post<QuizDTO>(`${this.baseUrl}/${id}/complete`, { score, levelReached });
  }

  getAverageScoreByCaregiver(caregiverId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/caregiver/${caregiverId}/average-score`);
  }

  getWeakTopicsByCaregiver(caregiverId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/caregiver/${caregiverId}/weak-topics`);
  }

  // ─── Question CRUD ───────────────────────────────────────────

  createQuestion(question: QuestionDTO): Observable<QuestionDTO> {
    return this.http.post<QuestionDTO>(`${this.baseUrl}/questions`, question);
  }

  updateQuestion(id: number, question: QuestionDTO): Observable<QuestionDTO> {
    return this.http.put<QuestionDTO>(`${this.baseUrl}/questions/${id}`, question);
  }

  deleteQuestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/questions/${id}`);
  }

  getQuestionById(id: number): Observable<QuestionDTO> {
    return this.http.get<QuestionDTO>(`${this.baseUrl}/questions/${id}`);
  }

  getAllQuestions(): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/questions`);
  }

  getQuestionsByQuizId(quizId: number): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/questions/quiz/${quizId}`)
      .pipe(
        tap(questions => console.log('Questions loaded with answers:', questions)),
        catchError(err => {
          console.error('Error loading questions:', err);
          return of([]);
        })
      );
  }

  getQuestionsByDifficultyLevel(level: number): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/questions/difficulty/${level}`);
  }

  deleteQuestionsByQuizId(quizId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/questions/quiz/${quizId}`);
  }

  getQuestionCountByQuizId(quizId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/questions/quiz/${quizId}/count`);
  }

  searchQuestions(keyword: string): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/questions/search?keyword=${encodeURIComponent(keyword)}`);
  }

  getQuestionsByQuizAndDifficulty(quizId: number, level: number): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/questions/quiz/${quizId}/difficulty/${level}`);
  }

  calculateTotalPoints(quizId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/questions/quiz/${quizId}/total-points`);
  }

  // ─── Answer CRUD ────────────────────────────────────────────

  createAnswer(answer: AnswerDTO): Observable<AnswerDTO> {
    return this.http.post<AnswerDTO>(`${this.baseUrl}/answer`, answer);
  }

  updateAnswer(id: number, answer: AnswerDTO): Observable<AnswerDTO> {
    return this.http.put<AnswerDTO>(`${this.baseUrl}/answer/${id}`, answer);
  }

  deleteAnswer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/answer/deleteAnswer/${id}`);
  }

  getAnswerById(id: number): Observable<AnswerDTO> {
    return this.http.get<AnswerDTO>(`${this.baseUrl}/answer/getAnswerById/${id}`);
  }

  getAllAnswers(): Observable<AnswerDTO[]> {
    return this.http.get<AnswerDTO[]>(`${this.baseUrl}/answer`);
  }

  getAnswersByQuestionId(questionId: number): Observable<AnswerDTO[]> {
    return this.http.get<AnswerDTO[]>(`${this.baseUrl}/answer/question/${questionId}`);
  }

  getCorrectAnswerByQuestionId(questionId: number): Observable<AnswerDTO> {
    return this.http.get<AnswerDTO>(`${this.baseUrl}/answer/question/${questionId}/correct`);
  }

  validateAnswer(request: ValidationRequestDTO): Observable<ValidationResponseDTO> {
    return this.http.post<ValidationResponseDTO>(`${this.baseUrl}/answer/validate`, request);
  }

  submitAnswer(request: SubmissionRequestDTO): Observable<SubmissionResponseDTO> {
    return this.http.post<SubmissionResponseDTO>(`${this.baseUrl}/answer/submit`, request);
  }

  createAnswersBatch(answers: AnswerDTO[]): Observable<AnswerDTO[]> {
    return this.http.post<AnswerDTO[]>(`${this.baseUrl}/answer/batch`, answers);
  }

  deleteAnswersByQuestionId(questionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/answer/question/${questionId}`);
  }

  isAnswerCorrect(id: number): Observable<BooleanResponseDTO> {
    return this.http.get<BooleanResponseDTO>(`${this.baseUrl}/answer/${id}/is-correct`);
  }

  getAnswerCountByQuestionId(questionId: number): Observable<CountResponseDTO> {
    return this.http.get<CountResponseDTO>(`${this.baseUrl}/answer/question/${questionId}/count`);
  }

  searchAnswers(keyword: string): Observable<AnswerDTO[]> {
    return this.http.get<AnswerDTO[]>(`${this.baseUrl}/answer/search?keyword=${encodeURIComponent(keyword)}`);
  }
}
