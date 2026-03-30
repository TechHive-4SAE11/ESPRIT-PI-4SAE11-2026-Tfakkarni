import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
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

// ── Service ─────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class MeetingService {
  private base = environment.apiBaseUrl + '/api/meetings';

  constructor(private http: HttpClient) {}

  createMeeting(req: CreateMeetingRequest): Observable<Meeting> {
    return this.http.post<Meeting>(this.base, req).pipe(
      catchError(err => {
        console.error('Error creating meeting:', err);
        throw err;
      })
    );
  }

  getMeeting(id: number): Observable<Meeting> {
    return this.http.get<Meeting>(`${this.base}/${id}`).pipe(
      catchError(err => {
        console.error('Error fetching meeting:', err);
        throw err;
      })
    );
  }

  getMeetingToken(
    id: number,
    keycloakId: string,
    userName: string
  ): Observable<{ token: string; roomUrl: string; roomName: string }> {
    return this.http
      .get<{ token: string; roomUrl: string; roomName: string }>(
        `${this.base}/${id}/token`,
        { params: { keycloakId, userName } }
      )
      .pipe(
        catchError(err => {
          console.error('Error fetching meeting token:', err);
          throw err;
        })
      );
  }

  updateNotes(id: number, notes: string): Observable<Meeting> {
    return this.http.put<Meeting>(`${this.base}/${id}/notes`, { notes }).pipe(
      catchError(err => {
        console.error('Error updating notes:', err);
        throw err;
      })
    );
  }

  endMeeting(id: number, notes?: string): Observable<MeetingSummaryResult> {
    return this.http
      .put<MeetingSummaryResult>(`${this.base}/${id}/end`, { notes: notes || null })
      .pipe(
        catchError(err => {
          console.error('Error ending meeting:', err);
          throw err;
        })
      );
  }

  getMeetingsForDoctor(doctorId: string): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${this.base}/doctor/${doctorId}`).pipe(
      catchError(err => {
        console.error('Error fetching doctor meetings:', err);
        return of([]);
      })
    );
  }

  getMeetingsForPatient(patientId: string): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${this.base}/patient/${patientId}`).pipe(
      catchError(err => {
        console.error('Error fetching patient meetings:', err);
        return of([]);
      })
    );
  }
}
