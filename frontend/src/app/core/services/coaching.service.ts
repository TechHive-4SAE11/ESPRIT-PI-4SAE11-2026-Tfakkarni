import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export type CoachingGoalType =
  | 'COGNITIVE_IMPROVEMENT'
  | 'ACTIVITY_INCREASE'
  | 'MEDICATION_ADHERENCE'
  | 'SOCIAL_ENGAGEMENT'
  | 'NUTRITION'
  | 'OTHER';

export type CoachingGoalStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED';

export type CoachingPriority = 'HIGH' | 'MEDIUM' | 'LOW';

export type CoachingMood = 'EXCELLENT' | 'GOOD' | 'NEUTRAL' | 'LOW';

export type ProgressRecordedByRole = 'HELPER' | 'PATIENT';

export interface CoachingGoal {
  id: number;
  medicalFolderId: number;
  diagnosticId: number | null;
  goalType: CoachingGoalType;
  goalTitle: string;
  actionSteps?: string | null;
  tips?: string | null;
  targetDays?: number | null;
  status: CoachingGoalStatus;
  priority: CoachingPriority;
  outdoorActivity: boolean;
  latitude?: number | null;
  longitude?: number | null;
  createdByDoctorId?: string | null;
  createdAt: string;
  updatedAt: string;
  lastStaleNotificationAt?: string | null;
}

export interface CoachingGoalRequestBody {
  diagnosticId?: number | null;
  goalType: CoachingGoalType;
  goalTitle: string;
  actionSteps?: string | null;
  tips?: string | null;
  targetDays?: number | null;
  priority?: CoachingPriority | null;
  outdoorActivity?: boolean;
  latitude?: number | null;
  longitude?: number | null;
}

export interface CoachingGoalStatusBody {
  status: CoachingGoalStatus;
}

export interface CoachingProgress {
  id: number;
  coachingGoalId: number;
  dateRecorded: string;
  completionPercentage?: number | null;
  mood?: CoachingMood | null;
  energyLevel?: number | null;
  helperNotes?: string | null;
  patientFeedback?: string | null;
  recordedByRole: ProgressRecordedByRole;
  recordedByUserId?: string | null;
  weatherSummary?: string | null;
  weatherFetchedAt?: string | null;
  createdAt: string;
}

export interface CoachingProgressRequestBody {
  dateRecorded?: string | null;
  completionPercentage?: number | null;
  mood?: CoachingMood | null;
  energyLevel?: number | null;
  helperNotes?: string | null;
  patientFeedback?: string | null;
  recordedByRole: ProgressRecordedByRole;
}

export interface CoachingNotification {
  id: number;
  folderId: number;
  goalId?: number | null;
  eventType: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  readAt?: string | null;
}

export interface CoachingSchedulerModeResponse {
  ok: boolean;
  demoMode: boolean;
}

@Injectable({ providedIn: 'root' })
export class CoachingService {
  private readonly notificationsBase = `${environment.apiBaseUrl}/api/medical-folders/coaching-notifications`;
  private readonly schedulerBase = `${environment.apiBaseUrl}/api/medical-folders/coaching/scheduler`;

  private base(folderId: number): string {
    return `${environment.apiBaseUrl}/api/medical-folders/${folderId}/coaching-goals`;
  }

  constructor(private readonly http: HttpClient) {}

  listGoals(folderId: number): Observable<CoachingGoal[]> {
    return this.http.get<CoachingGoal[]>(this.base(folderId));
  }

  getGoal(folderId: number, goalId: number): Observable<CoachingGoal> {
    return this.http.get<CoachingGoal>(`${this.base(folderId)}/${goalId}`);
  }

  createGoal(folderId: number, body: CoachingGoalRequestBody): Observable<CoachingGoal> {
    return this.http.post<CoachingGoal>(this.base(folderId), body);
  }

  updateGoal(folderId: number, goalId: number, body: CoachingGoalRequestBody): Observable<CoachingGoal> {
    return this.http.put<CoachingGoal>(`${this.base(folderId)}/${goalId}`, body);
  }

  patchGoalStatus(folderId: number, goalId: number, status: CoachingGoalStatus): Observable<CoachingGoal> {
    return this.http.patch<CoachingGoal>(`${this.base(folderId)}/${goalId}/status`, { status } satisfies CoachingGoalStatusBody);
  }

  deleteGoal(folderId: number, goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(folderId)}/${goalId}`);
  }

  listProgress(folderId: number, goalId: number): Observable<CoachingProgress[]> {
    return this.http.get<CoachingProgress[]>(`${this.base(folderId)}/${goalId}/progress`);
  }

  addProgress(
    folderId: number,
    goalId: number,
    body: CoachingProgressRequestBody
  ): Observable<CoachingProgress> {
    return this.http.post<CoachingProgress>(`${this.base(folderId)}/${goalId}/progress`, body);
  }

  listMyNotifications(recipientUserId?: string): Observable<CoachingNotification[]> {
    const params = recipientUserId ? new HttpParams().set('recipientUserId', recipientUserId) : undefined;
    return this.http.get<CoachingNotification[]>(`${this.notificationsBase}/my`, { params });
  }

  unreadCountMyNotifications(recipientUserId?: string): Observable<{ count: number }> {
    const params = recipientUserId ? new HttpParams().set('recipientUserId', recipientUserId) : undefined;
    return this.http.get<{ count: number }>(`${this.notificationsBase}/my/unread-count`, { params });
  }

  markMyNotificationAsRead(notificationId: number, recipientUserId?: string): Observable<CoachingNotification> {
    const params = recipientUserId ? new HttpParams().set('recipientUserId', recipientUserId) : undefined;
    return this.http.put<CoachingNotification>(
      `${this.notificationsBase}/my/${notificationId}/read`,
      {},
      { params }
    );
  }

  markAllMyNotificationsAsRead(recipientUserId?: string): Observable<void> {
    const params = recipientUserId ? new HttpParams().set('recipientUserId', recipientUserId) : undefined;
    return this.http.put<void>(`${this.notificationsBase}/my/read-all`, {}, { params });
  }

  getSchedulerMode(): Observable<CoachingSchedulerModeResponse> {
    return this.http.get<CoachingSchedulerModeResponse>(`${this.schedulerBase}/mode`);
  }

  setSchedulerMode(demoMode: boolean): Observable<CoachingSchedulerModeResponse> {
    const params = new HttpParams().set('demo', String(demoMode));
    return this.http.post<CoachingSchedulerModeResponse>(`${this.schedulerBase}/mode`, {}, { params });
  }
}
