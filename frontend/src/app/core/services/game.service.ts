import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface CreateGameRequest {
  title: string;
  description: string;
}

export interface GameImageUpload {
  name: string;
  imageBase64: string;
  contentType: string;
}

export interface GameResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  imageCount: number;
  createdAt: string;
}

export interface ImageDetail {
  id: number;
  name: string;
  imageBase64: string;
  contentType: string;
  displayOrder: number;
}

export interface GameDetailResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  images: ImageDetail[];
  createdAt: string;
}

export interface GameStatsResponse {
  playerKeycloakId: string;
  totalGamesCreated: number;
  totalGamesPlayed: number;
  averageScore: number;
  bestScore: number;
  totalAttempts: number;
}

export interface OverviewStatsResponse {
  totalGames: number;
  totalAttempts: number;
  totalPlayers: number;
  averageScorePercentage: number;
}

export interface AttemptPoint {
  attemptId: number;
  gameType: string; // "CUSTOM" | "MINI" | "MOVIE" | "PERSONAL"
  gameTitle: string;
  score: number;
  totalQuestions: number;
  percentage: number;
  durationSeconds: number | null;
  completedAt: string;
}

export interface ScoreAnalyticsResponse {
  patientKeycloakId: string;
  totalGamesPlayed: number;
  gamesLast7Days: number;
  averageScore: number;
  averageScoreLast7Days: number;
  bestScore: number;
  scoreHistory: AttemptPoint[];
}

export interface EditGameRequest {
  title: string;
  description: string;
  images: EditImageEntry[];
}

export interface EditImageEntry {
  /** null for new images, non-null for existing */
  id: number | null;
  name: string;
  /** Required only for new images */
  imageBase64?: string;
  /** Required only for new images */
  contentType?: string;
}

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games`;

  constructor(private readonly http: HttpClient) { }

  private userHeaders(keycloakId: string): HttpHeaders {
    return new HttpHeaders({ 'X-User-Id': keycloakId });
  }

  // ─── Game CRUD ──────────────────────────────────────────────

  createGame(keycloakId: string, request: CreateGameRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(this.baseUrl, request, {
      headers: this.userHeaders(keycloakId),
    });
  }

  uploadImages(gameId: number, uploads: GameImageUpload[]): Observable<GameDetailResponse> {
    return this.http.post<GameDetailResponse>(`${this.baseUrl}/${gameId}/images`, uploads);
  }

  getPatientGames(keycloakId: string): Observable<GameResponse[]> {
    return this.http.get<GameResponse[]>(`${this.baseUrl}/patient/${keycloakId}`);
  }

  getGameDetail(gameId: number): Observable<GameDetailResponse> {
    return this.http.get<GameDetailResponse>(`${this.baseUrl}/${gameId}`);
  }

  deleteGame(gameId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${gameId}`);
  }

  editGame(gameId: number, request: EditGameRequest): Observable<GameDetailResponse> {
    return this.http.put<GameDetailResponse>(`${this.baseUrl}/${gameId}`, request);
  }

  getAllGames(): Observable<GameResponse[]> {
    return this.http.get<GameResponse[]>(`${this.baseUrl}/all`);
  }

  // ─── Stats ──────────────────────────────────────────────────

  getPlayerStats(keycloakId: string): Observable<GameStatsResponse> {
    return this.http.get<GameStatsResponse>(`${this.baseUrl}/stats/patient/${keycloakId}`);
  }

  getOverviewStats(): Observable<OverviewStatsResponse> {
    return this.http.get<OverviewStatsResponse>(`${this.baseUrl}/stats/overview`);
  }

  getScoreAnalytics(keycloakId: string): Observable<ScoreAnalyticsResponse> {
    return this.http.get<ScoreAnalyticsResponse>(`${this.baseUrl}/stats/analytics/${keycloakId}`);
  }
}
