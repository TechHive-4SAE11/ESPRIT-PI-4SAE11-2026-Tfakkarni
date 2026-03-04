import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface Diagnostics {
  id: number;
  medicalFolderId: number;
  diseaseName: string;
  stage: string;
  comorbidities: string;
  diagnosisDate: string;
  createdAt: string;
  updatedAt: string;
  attachments?: DiagnosticAttachment[];
}

export interface DiagnosticAttachment {
  id: number;
  diagnosticId: number;
  fileName: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  description?: string;
  fileType?: string;
  createdAt: string;
  updatedAt: string;
  isImage?: boolean;
  isPdf?: boolean;
  formattedFileSize?: string;
}

export interface CreateDiagnosticsRequest {
  medicalFolderId: number;
  diseaseName: string;
  stage?: string;
  comorbidities?: string;
  diagnosisDate: string;
}

export interface UpdateDiagnosticsRequest {
  diseaseName?: string;
  stage?: string;
  comorbidities?: string;
  diagnosisDate?: string;
}

@Injectable({
  providedIn: 'root',
})
export class DiagnosticsService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/diagnostics`;

  constructor(private readonly http: HttpClient) {}

  getByFolder(medicalFolderId: number): Observable<Diagnostics[]> {
    return this.http.get<Diagnostics[]>(this.baseUrl, {
      params: { medicalFolderId: String(medicalFolderId) },
    });
  }

  getById(id: number): Observable<Diagnostics> {
    return this.http.get<Diagnostics>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.post<Diagnostics>(this.baseUrl, data);
  }

  update(id: number, data: UpdateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.put<Diagnostics>(`${this.baseUrl}/${id}`, data);
  }

  patch(id: number, data: UpdateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.patch<Diagnostics>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // File attachment methods
  uploadFile(file: File, description?: string, diagnosticId?: number): Observable<DiagnosticAttachment> {
    const formData = new FormData();
    formData.append('file', file);
    if (description) formData.append('description', description);
    if (diagnosticId) formData.append('diagnosticId', diagnosticId.toString());
    
    return this.http.post<DiagnosticAttachment>(`${environment.apiBaseUrl}/api/diagnostic-attachments/upload`, formData);
  }

  uploadMultipleFiles(files: File[], descriptions?: string[], diagnosticId?: number): Observable<DiagnosticAttachment[]> {
    const formData = new FormData();
    files.forEach((file, index) => {
      formData.append('files', file);
    });
    if (descriptions) {
      descriptions.forEach((desc, index) => {
        formData.append('descriptions', desc);
      });
    }
    if (diagnosticId) formData.append('diagnosticId', diagnosticId.toString());
    
    return this.http.post<DiagnosticAttachment[]>(`${environment.apiBaseUrl}/api/diagnostic-attachments/upload-multiple`, formData);
  }

  downloadFile(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiBaseUrl}/api/diagnostic-attachments/download/${id}`, {
      responseType: 'blob'
    });
  }

  deleteFile(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/api/diagnostic-attachments/${id}`);
  }

  getAttachmentsByDiagnostic(diagnosticId: number): Observable<DiagnosticAttachment[]> {
    return this.http.get<DiagnosticAttachment[]>(`${environment.apiBaseUrl}/api/diagnostic-attachments/diagnostic/${diagnosticId}`);
  }
}
