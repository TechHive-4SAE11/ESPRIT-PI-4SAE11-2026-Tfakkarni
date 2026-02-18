import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { DataPointType, DataPointSummary } from './data-point.service';

// ─── Types ────────────────────────────────────────────────

export interface GameItemEntry {
  dataType: DataPointType;
  dataPointId: number;
}

export interface CreateCustomGameRequest {
  title: string;
  description: string;
  items: GameItemEntry[];
}

export interface CustomGameResponse {
  id: number;
  title: string;
  description: string;
  itemCount: number;
  itemTypes: DataPointType[];
  createdAt: string;
}

export interface CustomGameDetailResponse {
  id: number;
  title: string;
  description: string;
  itemTypes: DataPointType[];
  items: DataPointSummary[];
  createdAt: string;
}

// ── Play types ──

export interface UnifiedPlayItem {
  index: number;
  type: DataPointType;
  itemId: number;
  // PHOTO
  imageBase64?: string;
  imageContentType?: string;
  // MOVIE
  posterUrl?: string;
  movieTitle?: string;
  // QUESTION
  questionText?: string;
  correctAnswer?: string;
  // PLACE
  latitude?: number;
  longitude?: number;
  hint?: string;
  // MCQ choices
  choices?: string[];
}

export interface UnifiedPlayData {
  gameId: number | null;
  title: string;
  totalQuestions: number;
  items: UnifiedPlayItem[];
}

export interface AnswerEntry {
  type: DataPointType;
  itemId: number;
  selectedAnswer: string;
  selfAssessedCorrect?: boolean;
}

export interface UnifiedSubmitRequest {
  gameId: number | null;
  score: number;
  totalQuestions: number;
  durationSeconds: number;
  answers: AnswerEntry[];
}

export interface ItemResult {
  type: DataPointType;
  itemId: number;
  correct: boolean;
  correctAnswer: string;
  selectedAnswer: string;
  label: string;
}

export interface UnifiedPlayResult {
  attemptId: number;
  score: number;
  totalQuestions: number;
  percentage: number;
  durationSeconds: number;
  completedAt: string;
  results: ItemResult[];
}

export interface UnifiedStats {
  totalGamesPlayed: number;
  averageScore: number;
  bestScore: number;
  photoCount: number;
  placeCount: number;
  movieCount: number;
  questionCount: number;
}

// ─── Service ──────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class CustomGameService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/custom`;

  constructor(private http: HttpClient) {}

  // ── CRUD ──

  createGame(keycloakId: string, request: CreateCustomGameRequest): Observable<CustomGameResponse> {
    return this.http.post<CustomGameResponse>(`${this.baseUrl}/${keycloakId}`, request);
  }

  getGames(keycloakId: string): Observable<CustomGameResponse[]> {
    return this.http.get<CustomGameResponse[]>(`${this.baseUrl}/patient/${keycloakId}`);
  }

  getGameDetail(gameId: number): Observable<CustomGameDetailResponse> {
    return this.http.get<CustomGameDetailResponse>(`${this.baseUrl}/${gameId}`);
  }

  deleteGame(gameId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${gameId}`);
  }

  // ── Play ──

  getPlayData(gameId: number): Observable<UnifiedPlayData> {
    return this.http.get<UnifiedPlayData>(`${this.baseUrl}/play/${gameId}`);
  }

  getRandomPlayData(keycloakId: string, limit?: number): Observable<UnifiedPlayData> {
    const params: any = {};
    if (limit) params.limit = limit.toString();
    return this.http.get<UnifiedPlayData>(`${this.baseUrl}/play/random/${keycloakId}`, { params });
  }

  submitResults(keycloakId: string, request: UnifiedSubmitRequest): Observable<UnifiedPlayResult> {
    return this.http.post<UnifiedPlayResult>(`${this.baseUrl}/play/submit`, request, {
      headers: new HttpHeaders({ 'X-User-Id': keycloakId }),
    });
  }

  // ── Stats ──

  getStats(keycloakId: string): Observable<UnifiedStats> {
    return this.http.get<UnifiedStats>(`${this.baseUrl}/stats/${keycloakId}`);
  }
}
