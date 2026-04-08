import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@/environments/environment';

export interface HeartbeatReading {
  id: number;
  patientId: string;
  bpm: number;
  timestamp: string;
}

export interface SleepStageEntry {
  timestamp: string;
  bpm: number;
  stage: 'AWAKE' | 'LIGHT' | 'DEEP' | 'REM';
}

export interface SleepSummary {
  totalSleepMinutes: number;
  timeInBedMinutes: number;
  deepSleepMinutes: number;
  lightSleepMinutes: number;
  remSleepMinutes: number;
  awakeMinutes: number;
  deepSleepPercent: number;
  lightSleepPercent: number;
  remSleepPercent: number;
  awakePercent: number;
  sleepEfficiency: number;
  qualityScore: number;
  awakenings: number;
  qualityLabel: string;
}

export interface SleepAnalysisResponse {
  patientId: string;
  date: string;
  timeline: SleepStageEntry[];
  summary: SleepSummary;
  insights: string[];
}

@Injectable({
  providedIn: 'root'
})
export class IotService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/iot/heartbeat`;

  getHeartbeatReadings(patientId: string, date?: string): Observable<HeartbeatReading[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<HeartbeatReading[]>(`${this.baseUrl}/${patientId}`, { params });
  }

  getSleepAnalysis(patientId: string, date?: string): Observable<SleepAnalysisResponse> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<SleepAnalysisResponse>(`${this.baseUrl}/${patientId}/sleep-analysis`, { params });
  }

  recordHeartbeat(reading: Partial<HeartbeatReading>): Observable<HeartbeatReading> {
    return this.http.post<HeartbeatReading>(this.baseUrl, reading);
  }

  getLatestReading(patientId: string): Observable<HeartbeatReading | null> {
    return this.http.get<HeartbeatReading>(`${this.baseUrl}/${patientId}/latest`);
  }

  /**
   * Poll dweet.cc for live BPM from the IoT bracelet.
   * Uses the Angular dev-server proxy to avoid CORS.
   */
  getLiveBpmFromDweet(thingName: string): Observable<number | null> {
    return this.http
      .get<any>(`/dweet-proxy/get/latest/dweet/for/${encodeURIComponent(thingName)}`)
      .pipe(
        map(response => {
          if (response?.this !== 'succeeded') return null;
          const content = response?.with?.[0]?.content;
          if (!content?.bpm) return null;
          return Number.parseInt(content.bpm, 10);
        }),
      );
  }
}
