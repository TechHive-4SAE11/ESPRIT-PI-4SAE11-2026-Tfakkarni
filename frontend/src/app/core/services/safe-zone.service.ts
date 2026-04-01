import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

// ─── DTOs ──────────────────────────────────────────────────

export interface LatLng {
  lat: number;
  lng: number;
}

export interface SafeZoneRequest {
  name: string;
  points: LatLng[];
  active: boolean;
}

export interface SafeZoneResponse {
  id: number;
  patientId: string;
  name: string;
  points: LatLng[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GeofenceAlertRequest {
  patientId: string;
  latitude: number;
  longitude: number;
  safeZoneName: string;
}

export interface GeofenceAlertResponse {
  id: number;
  patientId: string;
  latitude: number;
  longitude: number;
  safeZoneName: string;
  acknowledged: boolean;
  acknowledgedAt: string | null;
  createdAt: string;
}

// ─── Service ───────────────────────────────────────────────

@Injectable({
  providedIn: 'root',
})
export class SafeZoneService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/alerts`;

  createSafeZone(patientId: string, request: SafeZoneRequest): Observable<SafeZoneResponse> {
    return this.http.post<SafeZoneResponse>(
      `${this.baseUrl}/safe-zones/${patientId}`,
      request
    );
  }

  getSafeZones(patientId: string): Observable<SafeZoneResponse[]> {
    return this.http.get<SafeZoneResponse[]>(
      `${this.baseUrl}/safe-zones/${patientId}`
    );
  }

  getActiveSafeZones(patientId: string): Observable<SafeZoneResponse[]> {
    return this.http.get<SafeZoneResponse[]>(
      `${this.baseUrl}/safe-zones/${patientId}/active`
    );
  }

  updateSafeZone(patientId: string, id: number, request: SafeZoneRequest): Observable<SafeZoneResponse> {
    return this.http.put<SafeZoneResponse>(
      `${this.baseUrl}/safe-zones/${patientId}/${id}`,
      request
    );
  }

  deleteSafeZone(patientId: string, id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/safe-zones/${patientId}/${id}`
    );
  }

  reportViolation(request: GeofenceAlertRequest): Observable<GeofenceAlertResponse> {
    return this.http.post<GeofenceAlertResponse>(
      `${this.baseUrl}/geofence-violations`,
      request
    );
  }

  getViolationHistory(patientId: string): Observable<GeofenceAlertResponse[]> {
    return this.http.get<GeofenceAlertResponse[]>(
      `${this.baseUrl}/geofence-violations/${patientId}`
    );
  }

  acknowledgeViolation(id: number): Observable<GeofenceAlertResponse> {
    return this.http.patch<GeofenceAlertResponse>(
      `${this.baseUrl}/geofence-violations/${id}/acknowledge`,
      {}
    );
  }
}
