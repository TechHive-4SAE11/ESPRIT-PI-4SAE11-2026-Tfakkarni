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
  // Keycloak IDs — populated since fix
  doctorKeycloakId: string;
  patientKeycloakId: string;
  notes: string;
  aiSummary: string;
  transcript: string;
  transcriptSummaries: string;
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

export interface PartialSummaryResult {
  meetingId: number;
  segmentLabel: string;
  summary: string | null;
  transcriptSummaries: string | null;
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

  getMeetingToken(id: number, keycloakId: string, userName: string): Observable<MeetingToken> {
    const cacheKey = `${id}:${keycloakId}`;
    const cached = this.tokenCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) {
      return of(cached.data);
    }
    return this.http.get<MeetingToken>(
      `${this.base}/${id}/token`,
      { params: { keycloakId, userName } }
    ).pipe(
      tap(data => {
        this.tokenCache.set(cacheKey, { data, expiresAt: Date.now() + 10 * 60 * 1000 });
      }),
      catchError(err => { console.error('Error fetching meeting token:', err); throw err; })
    );
  }

  prefetchToken(id: number, keycloakId: string, userName: string): void {
    const cacheKey = `${id}:${keycloakId}`;
    const cached = this.tokenCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) return;
    this.getMeetingToken(id, keycloakId, userName).subscribe({
      error: err => console.warn(`Pre-fetch failed for meeting ${id}:`, err)
    });
  }

  updateNotes(id: number, notes: string): Observable<Meeting> {
    return this.http.put<Meeting>(`${this.base}/${id}/notes`, { notes }).pipe(
      catchError(err => { console.error('Error updating notes:', err); throw err; })
    );
  }

  saveTranscript(id: number, transcript: string,
    requestPartialSummary: boolean,
    segmentLabel: string): Observable<PartialSummaryResult> {
    return this.http.put<PartialSummaryResult>(`${this.base}/${id}/transcript`, {
      transcript, requestPartialSummary, segmentLabel
    }).pipe(catchError(err => { console.error('Error saving transcript:', err); throw err; }));
  }

  endMeeting(id: number, notes?: string): Observable<MeetingSummaryResult> {
    return this.http.put<MeetingSummaryResult>(
      `${this.base}/${id}/end`,
      { notes: notes || null }
    ).pipe(
      catchError(err => { console.error('Error ending meeting:', err); throw err; })
    );
  }

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

  /** Download meeting PDF from backend */
  downloadMeetingPdf(id: number, patientName: string): void {
    const url = `${this.base}/${id}/pdf`;
    const a = document.createElement('a');
    a.href = url;
    a.download = `rapport-reunion-${patientName.replace(/\s+/g, '_')}-${id}.pdf`;
    a.target = '_blank';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
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
