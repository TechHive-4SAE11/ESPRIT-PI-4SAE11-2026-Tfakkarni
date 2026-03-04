import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CalendarStatus {
    connected: boolean;
    googleEmail: string;
    lastSync: Date;
    syncedAppointments: number;
}

@Injectable({ providedIn: 'root' })
export class GoogleCalendarService {

    private readonly apiUrl = 'http://localhost:18086/api/medical/calendar';

    constructor(private http: HttpClient) { }

    getAuthUrl(doctorId: string): Observable<{ url: string }> {
        return this.http.get<{ url: string }>(`${this.apiUrl}/auth-url/${doctorId}`);
    }

    getStatus(doctorId: string): Observable<CalendarStatus> {
        return this.http.get<CalendarStatus>(`${this.apiUrl}/status/${doctorId}`);
    }

    disconnect(doctorId: string): Observable<void> {
        return this.http.post<void>(`${this.apiUrl}/disconnect/${doctorId}`, {});
    }
}
