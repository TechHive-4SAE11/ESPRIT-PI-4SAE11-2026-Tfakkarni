import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FollowUpReminder {
  id: number;
  patientKeycloakId: string;
  patientName: string | null;
  reminderDate: string;
  message: string;
  missingCategories: string; // comma-separated: "NUTRITION,MEDICATION,ACTIVITY"
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FollowUpReminderService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:9090/api/follow-up-reminders';

  getReminders(keycloakId: string): Observable<FollowUpReminder[]> {
    return this.http.get<FollowUpReminder[]>(`${this.base}/patient/${keycloakId}`);
  }

  getUnreadReminders(keycloakId: string): Observable<FollowUpReminder[]> {
    return this.http.get<FollowUpReminder[]>(`${this.base}/patient/${keycloakId}/unread`);
  }

  getUnreadCount(keycloakId: string): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/patient/${keycloakId}/count`);
  }

  markAsRead(id: number): Observable<FollowUpReminder> {
    return this.http.patch<FollowUpReminder>(`${this.base}/${id}/read`, {});
  }

  markAllAsRead(keycloakId: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/patient/${keycloakId}/read-all`, {});
  }

  /** Manual trigger — simulates the 22:00 cron job immediately for testing */
  triggerCheck(): Observable<{ message: string; remindersCreated: number }> {
    return this.http.post<{ message: string; remindersCreated: number }>(`${this.base}/check`, {});
  }
}
