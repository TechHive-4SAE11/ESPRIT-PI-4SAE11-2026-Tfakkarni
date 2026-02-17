import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── DTOs ──────────────────────────────────────────────────

export interface CreatePlaceRequest {
  name: string;
  latitude: number;
  longitude: number;
  hint: string;
}

export interface PlaceResponse {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  hint: string;
  createdAt: string;
}

export interface PlaceQuizResponse {
  correctPlaceId: number;
  correctName: string;
  latitude: number;
  longitude: number;
  hint: string;
  choices: string[];
}

// ─── Service ───────────────────────────────────────────────

@Injectable({
  providedIn: 'root',
})
export class PlaceService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/places`;

  constructor(private readonly http: HttpClient) { }

  private userHeaders(keycloakId: string): HttpHeaders {
    return new HttpHeaders({ 'X-User-Id': keycloakId });
  }

  createPlace(keycloakId: string, request: CreatePlaceRequest): Observable<PlaceResponse> {
    return this.http.post<PlaceResponse>(this.baseUrl, request, {
      headers: this.userHeaders(keycloakId),
    });
  }

  getPatientPlaces(keycloakId: string): Observable<PlaceResponse[]> {
    return this.http.get<PlaceResponse[]>(`${this.baseUrl}/patient/${keycloakId}`);
  }

  getPlaceQuiz(keycloakId: string): Observable<PlaceQuizResponse> {
    return this.http.get<PlaceQuizResponse>(`${this.baseUrl}/game/${keycloakId}`);
  }

  deletePlace(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  editPlace(id: number, request: CreatePlaceRequest): Observable<PlaceResponse> {
    return this.http.put<PlaceResponse>(`${this.baseUrl}/${id}`, request);
  }
}
