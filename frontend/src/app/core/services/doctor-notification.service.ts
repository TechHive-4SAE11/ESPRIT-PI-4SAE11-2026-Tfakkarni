import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of, switchMap } from 'rxjs';
import { environment } from '@/environments/environment';
import { DoctorNotification } from '@/core/models/notification.model';

@Injectable({ providedIn: 'root' })
export class DoctorNotificationService {
  private readonly base = `${environment.apiBaseUrl}/api/notifications`;

  notifications = signal<DoctorNotification[]>([]);
  unreadCount   = computed(() => this.notifications().filter(n => !n.read).length);

  constructor(private readonly http: HttpClient) {}

  /**
   * Charge les notifications pour un doctorId.
   * Si aucun résultat, essaie automatiquement avec les autres IDs en DB.
   */
  loadNotifications(doctorId: string): Observable<DoctorNotification[]> {
    if (!doctorId?.trim()) return of([]);

    return this.http.get<DoctorNotification[]>(
      `${this.base}/doctor/${encodeURIComponent(doctorId)}`
    ).pipe(
      tap(list => {
        if (list && list.length > 0) {
          this.notifications.set(list);
          console.log(`[notifications] Chargé ${list.length} notification(s) pour doctorId=${doctorId}`);
        } else {
          console.warn(`[notifications] 0 résultats pour doctorId=${doctorId}`);
        }
      }),
      catchError(err => {
        console.error('[notifications] Erreur loadNotifications:', err);
        return of([]);
      })
    );
  }

  /**
   * Charge par plusieurs IDs à la fois.
   * Utile quand l'ID Keycloak a changé après une reconfiguration Keycloak.
   */
  loadNotificationsByIds(ids: string[]): Observable<DoctorNotification[]> {
    const uniqueIds = [...new Set(ids.filter(id => !!id?.trim()))];
    if (uniqueIds.length === 0) return of([]);

    const params = uniqueIds.map(id => `ids=${encodeURIComponent(id)}`).join('&');
    return this.http.get<DoctorNotification[]>(`${this.base}/by-ids?${params}`).pipe(
      tap(list => {
        if (list && list.length > 0) {
          this.notifications.set(list);
          console.log(`[notifications] Chargé ${list.length} via multi-ids`);
        }
      }),
      catchError(err => {
        console.error('[notifications] Erreur loadByIds:', err);
        return of([]);
      })
    );
  }

  /**
   * Méthode principale — essaie doctorId puis multi-ids si 0 résultat.
   * Résoudra automatiquement le mismatch d'ID Keycloak.
   */
  loadNotificationsSmartly(primaryId: string, fallbackIds: string[] = []): Observable<DoctorNotification[]> {
    if (!primaryId?.trim()) return of([]);

    return this.loadNotifications(primaryId).pipe(
      switchMap(list => {
        if (list && list.length > 0) return of(list);

        // Si 0 résultat avec l'ID principal, essayer les fallbacks
        const allIds = [primaryId, ...fallbackIds].filter(id => !!id?.trim());
        if (allIds.length <= 1) {
          // Dernier recours : charger tous les IDs distincts en DB et chercher
          return this.getDistinctDoctorIds().pipe(
            switchMap(dbIds => {
              console.log('[notifications] IDs en DB:', dbIds);
              if (dbIds.length === 0) return of([]);
              return this.loadNotificationsByIds(dbIds);
            })
          );
        }

        return this.loadNotificationsByIds(allIds);
      })
    );
  }

  /**
   * Récupère tous les doctorKeycloakId distincts en DB (pour debug et résolution d'ID).
   */
  getDistinctDoctorIds(): Observable<string[]> {
    return this.http.get<{ distinctDoctorIds: string[] }>(`${this.base}/debug/doctor-ids`).pipe(
      switchMap(resp => of(resp.distinctDoctorIds || [])),
      catchError(() => of([]))
    );
  }

  getUnreadCount(doctorId: string): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${this.base}/doctor/${encodeURIComponent(doctorId)}/unread-count`
    ).pipe(catchError(() => of({ count: 0 })));
  }

  markAsRead(notificationId: number): Observable<DoctorNotification> {
    return this.http.put<DoctorNotification>(`${this.base}/${notificationId}/read`, {})
      .pipe(
        tap(updated => {
          this.notifications.update(list =>
            list.map(n => n.id === updated.id ? { ...n, read: true } : n)
          );
        }),
        catchError(err => { console.error('markAsRead error:', err); throw err; })
      );
  }

  markAllAsRead(doctorId: string): Observable<void> {
    return this.http.put<void>(
      `${this.base}/doctor/${encodeURIComponent(doctorId)}/read-all`, {}
    ).pipe(
      tap(() => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
      }),
      catchError(err => { console.error('markAllAsRead error:', err); return of(undefined); })
    );
  }
}
