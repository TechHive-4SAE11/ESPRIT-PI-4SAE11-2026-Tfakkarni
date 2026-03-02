import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '@/environments/environment';
import { DoctorNotification } from '@/core/models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly base = `${environment.apiBaseUrl}/api/notifications`;

  notifications = signal<DoctorNotification[]>([]);
  unreadCount   = computed(() => this.notifications().filter(n => !n.read).length);

  constructor(private readonly http: HttpClient) {}

  loadNotifications(doctorId: string): Observable<DoctorNotification[]> {
    return this.http.get<DoctorNotification[]>(`${this.base}/doctor/${encodeURIComponent(doctorId)}`)
      .pipe(tap(list => this.notifications.set(list)));
  }

  getUnreadCount(doctorId: string): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/doctor/${encodeURIComponent(doctorId)}/unread-count`);
  }

  markAsRead(notificationId: number): Observable<DoctorNotification> {
    return this.http.put<DoctorNotification>(`${this.base}/${notificationId}/read`, {})
      .pipe(tap(updated => {
        this.notifications.update(list =>
          list.map(n => n.id === updated.id ? { ...n, read: true } : n)
        );
      }));
  }

  markAllAsRead(doctorId: string): Observable<void> {
    return this.http.put<void>(`${this.base}/doctor/${encodeURIComponent(doctorId)}/read-all`, {})
      .pipe(tap(() => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
      }));
  }
}
