import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─── Types ────────────────────────────────────────────────

export interface TagRequest {
  name: string;
  color: string;
}

export interface TagResponse {
  id: number;
  name: string;
  color: string;
}

// ─── Service ──────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class MemoryTagService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/games/tags`;

  constructor(private http: HttpClient) {}

  getTags(keycloakId: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`${this.baseUrl}/${keycloakId}`);
  }

  searchTags(keycloakId: string, query: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(`${this.baseUrl}/${keycloakId}/search`, {
      params: { query },
    });
  }

  createTag(keycloakId: string, request: TagRequest): Observable<TagResponse> {
    return this.http.post<TagResponse>(`${this.baseUrl}/${keycloakId}`, request);
  }

  updateTag(tagId: number, request: TagRequest): Observable<TagResponse> {
    return this.http.put<TagResponse>(`${this.baseUrl}/${tagId}`, request);
  }

  deleteTag(tagId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tagId}`);
  }
}
