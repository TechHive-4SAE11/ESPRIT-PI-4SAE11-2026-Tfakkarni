import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { TagResponse } from './memory-tag.service';

// ─── Types ────────────────────────────────────────────────

export type DataPointType = 'PHOTO' | 'PLACE' | 'MOVIE' | 'QUESTION';

export interface CreatePhotoRequest {
  name: string;
  imageBase64: string;
  contentType: string;
  tagIds: number[];
}

export interface CreatePlaceRequest {
  name: string;
  latitude: number;
  longitude: number;
  hint?: string;
  tagIds: number[];
}

export interface CreateMovieMemoryRequest {
  tmdbId: number;
  originalTitle: string;
  posterPath: string;
  releaseDate: string;
  correctAnswer: string;
  tagIds: number[];
}

export interface CreateQuestionRequest {
  questionText: string;
  correctAnswer: string;
  tagIds: number[];
}

export interface DataPointSummary {
  id: number;
  type: DataPointType;
  label: string;
  subtitle: string;
  tags: TagResponse[];
  createdAt: string;
  imagePreview?: string;
  posterPath?: string;
}

export interface DataPointCounts {
  PHOTO: number;
  PLACE: number;
  MOVIE: number;
  QUESTION: number;
}

// ─── Service ──────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class DataPointService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/data`;

  constructor(private http: HttpClient) {}

  // ── Photos ──

  createPhoto(keycloakId: string, request: CreatePhotoRequest): Observable<DataPointSummary> {
    return this.http.post<DataPointSummary>(`${this.baseUrl}/photos/${keycloakId}`, request);
  }

  deletePhoto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/photos/${id}`);
  }

  // ── Places ──

  createPlace(keycloakId: string, request: CreatePlaceRequest): Observable<DataPointSummary> {
    return this.http.post<DataPointSummary>(`${this.baseUrl}/places/${keycloakId}`, request);
  }

  deletePlace(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/places/${id}`);
  }

  // ── Movies ──

  createMovie(keycloakId: string, request: CreateMovieMemoryRequest): Observable<DataPointSummary> {
    return this.http.post<DataPointSummary>(`${this.baseUrl}/movies/${keycloakId}`, request);
  }

  deleteMovie(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/movies/${id}`);
  }

  // ── Questions ──

  createQuestion(keycloakId: string, request: CreateQuestionRequest): Observable<DataPointSummary> {
    return this.http.post<DataPointSummary>(`${this.baseUrl}/questions/${keycloakId}`, request);
  }

  deleteQuestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/questions/${id}`);
  }

  // ── Listing ──

  getAllDataPoints(
    keycloakId: string,
    types?: DataPointType[],
    tagIds?: number[]
  ): Observable<DataPointSummary[]> {
    let params = new HttpParams();
    if (types && types.length > 0) {
      types.forEach((t) => (params = params.append('types', t)));
    }
    if (tagIds && tagIds.length > 0) {
      tagIds.forEach((id) => (params = params.append('tagIds', id.toString())));
    }
    return this.http.get<DataPointSummary[]>(`${this.baseUrl}/${keycloakId}`, { params });
  }

  getCounts(keycloakId: string): Observable<DataPointCounts> {
    return this.http.get<DataPointCounts>(`${this.baseUrl}/${keycloakId}/counts`);
  }
}
