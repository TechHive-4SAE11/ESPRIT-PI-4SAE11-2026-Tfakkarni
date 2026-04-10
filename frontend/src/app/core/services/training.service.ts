import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface Module {
  id: number;
  title: string;
  description: string;
  category: string;
  difficulty: string;
  duration: number;
  videoUrl?: string;
  pdfUrl?: string;
}

export interface Progress {
  completedModules: number;
  totalModules: number;
  percentage: number;
}

@Injectable({ providedIn: 'root' })
export class TrainingService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/api/ml/training`;  // ← URL ABSOLUE

  private getAuthHeaders() {
    const username = 'user';
    const password = '3442ba60-abd4-4c73-968f-a96886ef09c6';
    const base64 = btoa(`${username}:${password}`);
    return { headers: { 'Authorization': `Basic ${base64}` } };
  }

  getModules(): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.apiUrl}/modules`, this.getAuthHeaders());
  }

  getModuleById(id: number): Observable<Module> {
    return this.http.get<Module>(`${this.apiUrl}/modules/${id}`, this.getAuthHeaders());
  }

  getProgress(userId: number): Observable<Progress> {
    return this.http.get<Progress>(`${this.apiUrl}/progress/${userId}`, this.getAuthHeaders());
  }

  completeModule(userId: number, moduleId: number, score: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/complete`, { userId, moduleId, score }, this.getAuthHeaders());
  }

  getRecommendations(userId: number): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.apiUrl}/recommendations/${userId}`, this.getAuthHeaders());
  }

  // --- ADMIN METHODS ---
  getAllModulesAdmin(): Observable<Module[]> {
    return this.http.get<Module[]>(`${environment.apiBaseUrl}/api/ml/admin/training/modules`, this.getAuthHeaders());
  }

  createModule(module: Module): Observable<Module> {
    return this.http.post<Module>(`${environment.apiBaseUrl}/api/ml/admin/training/modules`, module, this.getAuthHeaders());
  }

  updateModule(id: number, module: Module): Observable<Module> {
    return this.http.put<Module>(`${environment.apiBaseUrl}/api/ml/admin/training/modules/${id}`, module, this.getAuthHeaders());
  }

  deleteModule(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/api/ml/admin/training/modules/${id}`, this.getAuthHeaders());
  }
}
