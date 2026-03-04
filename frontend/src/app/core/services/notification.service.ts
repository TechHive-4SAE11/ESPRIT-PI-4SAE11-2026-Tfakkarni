import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface MedicationNotification {
    id: string;
    patientId: string;
    medicationId: number;
    medicationName: string;
    dosage: string;
    frequency: string;
    instructions: string;
    status: string;
    read: boolean;
    pushed: boolean;
    createdAt: string;
    readAt: string | null;
    type: string;
}

export interface NotificationResponse {
    totalNotifications: number;
    unreadCount: number;
    notifications: MedicationNotification[];
    date: string;
    message: string;
}

export interface UnreadCountResponse {
    patientId: string;
    unreadCount: number;
}

@Injectable({
    providedIn: 'root'
})
export class NotificationService {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiBaseUrl}/api/alerts`;

    /**
     * Get today's medication notifications (generates if not existing)
     */
    getNotifications(patientId: string): Observable<NotificationResponse> {
        return this.http.get<NotificationResponse>(
            `${this.baseUrl}/notifications/${patientId}`
        );
    }

    /**
     * Force refresh notifications
     */
    refreshNotifications(patientId: string): Observable<NotificationResponse> {
        return this.http.post<NotificationResponse>(
            `${this.baseUrl}/notifications/${patientId}/refresh`,
            {}
        );
    }

    /**
     * Mark a specific notification as read
     */
    markAsRead(patientId: string, notificationId: string): Observable<any> {
        return this.http.patch(
            `${this.baseUrl}/notifications/${patientId}/${notificationId}/read`,
            {}
        );
    }

    /**
     * Mark all notifications as read
     */
    markAllAsRead(patientId: string): Observable<any> {
        return this.http.patch(
            `${this.baseUrl}/notifications/${patientId}/read-all`,
            {}
        );
    }

    /**
     * Get unread notification count
     */
    getUnreadCount(patientId: string): Observable<UnreadCountResponse> {
        return this.http.get<UnreadCountResponse>(
            `${this.baseUrl}/notifications/${patientId}/count`
        );
    }

    /**
     * Register FCM token for push notifications
     */
    registerFcmToken(patientId: string, fcmToken: string): Observable<any> {
        return this.http.post(`${this.baseUrl}/fcm/register`, {
            patientId,
            fcmToken
        });
    }

    /**
     * Remove FCM token
     */
    removeFcmToken(patientId: string): Observable<any> {
        return this.http.delete(`${this.baseUrl}/fcm/${patientId}`);
    }
}
