import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── TMDB Types ───────────────────────────────────────────

export interface TmdbMovie {
  id: number;
  original_title: string;
  title: string;
  poster_path: string;
  release_date: string;
  overview: string;
}

// ─── Movie Game Types ─────────────────────────────────────

export interface MovieItemRequest {
  tmdbId: number;
  originalTitle: string;
  posterPath: string;
  releaseDate: string;
  correctAnswer: string;
}

export interface CreateMovieGameRequest {
  title: string;
  description: string;
  movies: MovieItemRequest[];
}

export interface MovieGameResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  movieCount: number;
  createdAt: string;
}

export interface MovieQuestion {
  itemId: number;
  posterUrl: string;
  movieTitle: string;
  releaseDate: string;
  choices: string[];
}

export interface MovieGamePlayData {
  gameId: number;
  title: string;
  description: string;
  questions: MovieQuestion[];
  totalQuestions: number;
}

export interface MovieAnswerEntry {
  itemId: number;
  selectedAnswer: string;
}

export interface MovieGameSubmitRequest {
  answers: MovieAnswerEntry[];
  durationSeconds: number;
}

export interface MovieAnswerResult {
  itemId: number;
  posterUrl: string;
  movieTitle: string;
  correctAnswer: string;
  selectedAnswer: string;
  correct: boolean;
}

export interface MovieGameAttemptResponse {
  attemptId: number;
  score: number;
  totalQuestions: number;
  durationSeconds: number;
  percentage: number;
  results: MovieAnswerResult[];
  completedAt: string;
}

// ─── Edit / Detail Types ──────────────────────────────────

export interface MovieItemDetail {
  id: number;
  tmdbId: number;
  originalTitle: string;
  posterPath: string;
  releaseDate: string;
  correctAnswer: string;
}

export interface MovieGameDetailResponse {
  id: number;
  patientKeycloakId: string;
  title: string;
  description: string;
  movies: MovieItemDetail[];
  createdAt: string;
}

export interface EditMovieItemEntry {
  id?: number | null;
  tmdbId: number;
  originalTitle: string;
  posterPath: string;
  releaseDate: string;
  correctAnswer: string;
}

export interface EditMovieGameRequest {
  title: string;
  description: string;
  movies: EditMovieItemEntry[];
}

@Injectable({
  providedIn: 'root',
})
export class MovieGameService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/movies`;

  constructor(private readonly http: HttpClient) {}

  private userHeaders(keycloakId: string): HttpHeaders {
    return new HttpHeaders({ 'X-User-Id': keycloakId });
  }

  // ─── TMDB Search ───────────────────────────────────────────

  searchMovies(query: string): Observable<TmdbMovie[]> {
    return this.http.get<TmdbMovie[]>(`${this.baseUrl}/tmdb/search`, {
      params: { query },
    });
  }

  getTmdbPosterUrl(posterPath: string, size: string = 'w500'): string {
    return `https://image.tmdb.org/t/p/${size}${posterPath}`;
  }

  // ─── Movie Game CRUD ───────────────────────────────────────

  createMovieGame(keycloakId: string, request: CreateMovieGameRequest): Observable<MovieGameResponse> {
    return this.http.post<MovieGameResponse>(this.baseUrl, request, {
      headers: this.userHeaders(keycloakId),
    });
  }

  getPatientMovieGames(keycloakId: string): Observable<MovieGameResponse[]> {
    return this.http.get<MovieGameResponse[]>(`${this.baseUrl}/patient/${keycloakId}`);
  }

  deleteMovieGame(gameId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${gameId}`);
  }

  getMovieGameDetail(gameId: number): Observable<MovieGameDetailResponse> {
    return this.http.get<MovieGameDetailResponse>(`${this.baseUrl}/${gameId}`);
  }

  editMovieGame(gameId: number, request: EditMovieGameRequest): Observable<MovieGameResponse> {
    return this.http.put<MovieGameResponse>(`${this.baseUrl}/${gameId}`, request);
  }

  // ─── Gameplay ──────────────────────────────────────────────

  getMovieGameForPlay(gameId: number): Observable<MovieGamePlayData> {
    return this.http.get<MovieGamePlayData>(`${this.baseUrl}/play/${gameId}`);
  }

  submitMovieGameAnswers(
    gameId: number,
    keycloakId: string,
    request: MovieGameSubmitRequest
  ): Observable<MovieGameAttemptResponse> {
    return this.http.post<MovieGameAttemptResponse>(
      `${this.baseUrl}/play/${gameId}/submit`,
      request,
      { headers: this.userHeaders(keycloakId) }
    );
  }
}
