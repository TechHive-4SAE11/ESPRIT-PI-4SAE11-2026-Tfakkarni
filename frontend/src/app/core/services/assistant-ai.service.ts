import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import {
  QuizGenerateRequest,
  GeneratedQuiz,
  EquipmentRecommendRequest,
  EquipmentRecommendResponse,
  VoiceCommandRequest,
  VoiceCommandResponse,
  VideoGenerateRequest,
  VideoGenerateResponse,
  VideoFeedbackRequest,
} from '@/core/models/assistant-ai.model';

@Injectable({
  providedIn: 'root',
})
export class AssistantAIService {
  private readonly baseUrl = 'http://localhost:18089/api/ai';

  constructor(private readonly http: HttpClient) {}

  // ── Quiz Generation ──────────────────────────────────────────────────
  generateQuiz(request: QuizGenerateRequest): Observable<GeneratedQuiz> {
    return this.http.post<GeneratedQuiz>(`${this.baseUrl}/quiz/generate`, request);
  }

  generateQuizFromPatientName(request: {
    patientName: string;
    numberOfQuestions: number;
    difficultyLevel?: number;
    caregiverId: number;
  }): Observable<GeneratedQuiz> {
    return this.http.post<GeneratedQuiz>(`${this.baseUrl}/quiz/generate-from-patient-name`, request);
  }

  // ── Equipment Recommendation ─────────────────────────────────────────
  recommendEquipment(request: EquipmentRecommendRequest): Observable<EquipmentRecommendResponse> {
    return this.http.post<EquipmentRecommendResponse>(`${this.baseUrl}/equipment/recommend`, request);
  }

  recommendEquipmentFromPatientName(patientName: string): Observable<EquipmentRecommendResponse> {
    return this.http.post<EquipmentRecommendResponse>(`${this.baseUrl}/equipment/recommend-from-patient-name`, { patientName });
  }

  // ── Voice Assistant (REST fallback) ──────────────────────────────────
  sendVoiceCommand(request: VoiceCommandRequest): Observable<VoiceCommandResponse> {
    return this.http.post<VoiceCommandResponse>(`${this.baseUrl}/assistant/command`, request);
  }

  getAssistantHealth(): Observable<any> {
    return this.http.get(`${this.baseUrl}/assistant/health`);
  }

  // ── Video Generation ─────────────────────────────────────────────────
  generateVideo(request: VideoGenerateRequest): Observable<VideoGenerateResponse> {
    return this.http.post<VideoGenerateResponse>(`${this.baseUrl}/video/generate`, request);
  }

  getPatientVideos(patientId: number): Observable<VideoGenerateResponse[]> {
    return this.http.get<VideoGenerateResponse[]>(`${this.baseUrl}/video/patient/${patientId}`);
  }

  watchVideo(videoId: number): Observable<VideoGenerateResponse> {
    return this.http.get<VideoGenerateResponse>(`${this.baseUrl}/video/${videoId}/watch`);
  }

  renderVideo(videoId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/video/${videoId}/render`, {});
  }

  submitVideoFeedback(videoId: number, feedback: VideoFeedbackRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/video/${videoId}/feedback`, feedback);
  }

  getVideoFeedback(videoId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/video/${videoId}/feedback`);
  }
}
