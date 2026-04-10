import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export interface ChatMessage {
  role: string;
  content: string;
  timestamp: string;
}

export interface ChatResponse {
  answer: string;
  sessionId: number;
}

export interface StressAnalysis {
  stressLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  factors: string[];
  recommendation: string;
}

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/api/ml`;

  sendMessage(userId: number, question: string, sessionId?: number): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/chat?userId=${userId}`, { question, sessionId });
  }

  getChatHistory(userId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/chat/history/${userId}`);
  }

  getStressAnalysis(userId: number): Observable<StressAnalysis> {
    return this.http.get<StressAnalysis>(`${this.apiUrl}/stress/${userId}`);
  }
}
