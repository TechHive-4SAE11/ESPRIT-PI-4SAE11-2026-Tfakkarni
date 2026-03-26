import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface AIReportPayload {
  differentials?: string[];
  anomalies?: string[];
  riskLevel?: string;
  advice?: string;
  contradictions?: string[];
}

export interface AIReport {
  id: number;
  medicalFolderId: number;
  generatedAt: string;
  reportJson: AIReportPayload | null;
  status: 'PENDING' | 'READY' | 'ERROR';
  errorMessage?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class AIReportService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/ai-reports`;

  constructor(private readonly http: HttpClient) {}

  getByFolderId(folderId: number): Observable<AIReport[]> {
    return this.http.get<AIReport[]>(this.baseUrl, { params: { folderId: String(folderId) } });
  }

  getLatest(folderId: number): Observable<AIReport | null> {
    return this.http.get<AIReport | null>(`${this.baseUrl}/latest`, {
      params: { folderId: String(folderId) },
    });
  }

  generate(folderId: number): Observable<AIReport> {
    return this.http.post<AIReport>(`${this.baseUrl}/generate/${folderId}`, {});
  }
}
