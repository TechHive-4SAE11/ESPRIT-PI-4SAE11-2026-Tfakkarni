import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── Types ────────────────────────────────────────────────

export interface QuestionItemRequest {
  questionText: string;
  correctAnswer: string;
}

export interface CreatePersonalQuestionGameRequest {
  title: string;
  description: string;
  questions: QuestionItemRequest[];
}

export interface PersonalQuestionGameResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  questionCount: number;
  createdAt: string;
}

export interface QuestionItemDetail {
  id: number;
  questionText: string;
  correctAnswer: string;
}

export interface PersonalQuestionGameDetailResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  questions: QuestionItemDetail[];
  createdAt: string;
}

export interface EditQuestionItemEntry {
  id?: number | null;
  questionText: string;
  correctAnswer: string;
}

export interface EditPersonalQuestionGameRequest {
  title: string;
  description: string;
  questions: EditQuestionItemEntry[];
}

export interface PersonalQuestion {
  itemId: number;
  questionText: string;
  correctAnswer: string;
}

export interface PersonalQuestionPlayData {
  gameId: number;
  title: string;
  description: string;
  questions: PersonalQuestion[];
  totalQuestions: number;
}

export interface PersonalQuestionSubmitRequest {
  score: number;
  totalQuestions: number;
  durationSeconds: number;
}

export interface PersonalQuestionAttemptResponse {
  attemptId: number;
  score: number;
  totalQuestions: number;
  durationSeconds: number;
  percentage: number;
  completedAt: string;
}

// ─── Service ──────────────────────────────────────────────

@Injectable({
  providedIn: 'root',
})
export class PersonalQuestionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/personal`;

  constructor(private readonly http: HttpClient) {}

  private userHeaders(keycloakId: string): HttpHeaders {
    return new HttpHeaders({ 'X-User-Id': keycloakId });
  }

  // ─── CRUD ───────────────────────────────────────────────

  createGame(keycloakId: string, request: CreatePersonalQuestionGameRequest): Observable<PersonalQuestionGameResponse> {
    return this.http.post<PersonalQuestionGameResponse>(this.baseUrl, request, {
      headers: this.userHeaders(keycloakId),
    });
  }

  getPatientGames(keycloakId: string): Observable<PersonalQuestionGameResponse[]> {
    return this.http.get<PersonalQuestionGameResponse[]>(`${this.baseUrl}/patient/${keycloakId}`);
  }

  getGameDetail(gameId: number): Observable<PersonalQuestionGameDetailResponse> {
    return this.http.get<PersonalQuestionGameDetailResponse>(`${this.baseUrl}/${gameId}`);
  }

  editGame(gameId: number, request: EditPersonalQuestionGameRequest): Observable<PersonalQuestionGameResponse> {
    return this.http.put<PersonalQuestionGameResponse>(`${this.baseUrl}/${gameId}`, request);
  }

  deleteGame(gameId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${gameId}`);
  }

  // ─── Gameplay ───────────────────────────────────────────

  getGameForPlay(gameId: number): Observable<PersonalQuestionPlayData> {
    return this.http.get<PersonalQuestionPlayData>(`${this.baseUrl}/play/${gameId}`);
  }

  submitResults(
    gameId: number,
    keycloakId: string,
    request: PersonalQuestionSubmitRequest
  ): Observable<PersonalQuestionAttemptResponse> {
    return this.http.post<PersonalQuestionAttemptResponse>(
      `${this.baseUrl}/play/${gameId}/submit`,
      request,
      { headers: this.userHeaders(keycloakId) }
    );
  }
}
