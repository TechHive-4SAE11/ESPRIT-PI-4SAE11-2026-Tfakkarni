import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { environment } from '@/environments/environment';

// ── Interfaces ──────────────────────────────────────────

export interface Meeting {
  id: number;
  roomName: string;
  roomUrl: string;
  status: 'SCHEDULED' | 'ACTIVE' | 'ENDED';
  patientName: string;
  doctorName: string;
  notes: string;
  aiSummary: string;
  scheduledAt: string;
  startedAt: string;
  endedAt: string;
  durationMinutes: number;
  createdAt: string;
}

export interface CreateMeetingRequest {
  patientKeycloakId: string;
  doctorKeycloakId: string;
  scheduledAt?: string;
}

export interface MeetingSummaryResult {
  meetingId: number;
  summary: string;
  durationMinutes: number;
}

export interface MeetingToken {
  token: string;
  roomUrl: string;
  roomName: string;
}

// ── Service ─────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class MeetingService {
  private base = environment.apiBaseUrl + '/api/meetings';

  // Token cache — avoids repeated API calls to Daily.co
  private tokenCache = new Map<string, { data: MeetingToken; expiresAt: number }>();

  constructor(private http: HttpClient) {}

  createMeeting(req: CreateMeetingRequest): Observable<Meeting> {
    return this.http.post<Meeting>(this.base, req).pipe(
      catchError(err => { console.error('Error creating meeting:', err); throw err; })
    );
  }

  getMeeting(id: number): Observable<Meeting> {
    return this.http.get<Meeting>(`${this.base}/${id}`).pipe(
      catchError(err => { console.error('Error fetching meeting:', err); throw err; })
    );
  }

  /**
   * Get meeting token with cache — avoids repeated Daily.co API calls.
   * Cache expires after 10 minutes (tokens are valid for meeting.room-expiry-minutes).
   */
  getMeetingToken(id: number, keycloakId: string, userName: string): Observable<MeetingToken> {
    const cacheKey = `${id}:${keycloakId}`;
    const cached = this.tokenCache.get(cacheKey);

    if (cached && cached.expiresAt > Date.now()) {
      console.log(`[MeetingService] Token cache HIT for meeting ${id}`);
      return of(cached.data);
    }

    return this.http.get<MeetingToken>(
      `${this.base}/${id}/token`,
      { params: { keycloakId, userName } }
    ).pipe(
      tap(data => {
        // Cache token for 10 minutes
        this.tokenCache.set(cacheKey, { data, expiresAt: Date.now() + 10 * 60 * 1000 });
        console.log(`[MeetingService] Token cached for meeting ${id}`);
      }),
      catchError(err => { console.error('Error fetching meeting token:', err); throw err; })
    );
  }

  /**
   * Pre-fetch a token in background (called when meeting is ACTIVE in the list).
   * This makes the join near-instant for the patient.
   */
  prefetchToken(id: number, keycloakId: string, userName: string): void {
    const cacheKey = `${id}:${keycloakId}`;
    const cached = this.tokenCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) return; // already cached

    console.log(`[MeetingService] Pre-fetching token for meeting ${id}`);
    this.getMeetingToken(id, keycloakId, userName).subscribe({
      next: () => console.log(`[MeetingService] Token pre-fetched for meeting ${id}`),
      error: err => console.warn(`[MeetingService] Pre-fetch failed for meeting ${id}:`, err)
    });
  }

  updateNotes(id: number, notes: string): Observable<Meeting> {
    return this.http.put<Meeting>(`${this.base}/${id}/notes`, { notes }).pipe(
      catchError(err => { console.error('Error updating notes:', err); throw err; })
    );
  }

  endMeeting(id: number, notes?: string): Observable<MeetingSummaryResult> {
    return this.http.put<MeetingSummaryResult>(
      `${this.base}/${id}/end`,
      { notes: notes || null }
    ).pipe(
      catchError(err => { console.error('Error ending meeting:', err); throw err; })
    );
  }

  /**
   * Regenerate AI summary for a meeting that has a bad/missing summary.
   */
  deleteMeeting(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`).pipe(
      catchError(err => { console.error('Error deleting meeting:', err); throw err; })
    );
  }

  regenerateSummary(id: number): Observable<MeetingSummaryResult> {
    return this.http.post<MeetingSummaryResult>(
      `${this.base}/${id}/regenerate-summary`, {}
    ).pipe(
      catchError(err => { console.error('Error regenerating summary:', err); throw err; })
    );
  }

  getMeetingsForDoctor(doctorId: string): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${this.base}/doctor/${doctorId}`).pipe(
      catchError(err => { console.error('Error fetching doctor meetings:', err); return of([]); })
    );
  }

  getMeetingsForPatient(patientId: string): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${this.base}/patient/${patientId}`).pipe(
      catchError(err => { console.error('Error fetching patient meetings:', err); return of([]); })
    );
  }

  clearTokenCache(meetingId?: number): void {
    if (meetingId) {
      for (const key of this.tokenCache.keys()) {
        if (key.startsWith(`${meetingId}:`)) this.tokenCache.delete(key);
      }
    } else {
      this.tokenCache.clear();
    }
  }
}
