import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface MedicalFolder {
  id: number;
  patientId: string;
  doctorId: string;
  bloodType?: string;
  height?: number;
  weight?: number;
  createdAt: string;
  updatedAt: string;
}

export interface MedicalFolderPage {
  content: MedicalFolder[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface MedicalFolderStats {
  total: number;
  thisMonth: number;
  thisWeek: number;
  patientCount: number;
}

export interface CreateMedicalFolderRequest {
  patientId: string;
  doctorId?: string;
  bloodType?: string;
  height?: number;
  weight?: number;
}

export interface UpdateMedicalFolderRequest {
  patientId?: string;
  doctorId?: string;
  bloodType?: string;
  height?: number;
  weight?: number;
}

/** @deprecated Use MedicalFolder */
export type MedicalFolderResponseDTO = MedicalFolder;

@Injectable({
  providedIn: 'root',
})
export class MedicalFolderService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medical-folders`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Get paginated medical folders.
   * @param params page (0-based), size, sort (e.g. 'createdAt,desc'), optional search (patientId filter)
   */
  getPage(params: {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
  } = {}): Observable<MedicalFolderPage> {
    const { page = 0, size = 10, sort = 'createdAt,desc', search } = params;
    let queryParams: Record<string, string> = { page: String(page), size: String(size), sort };
    if (search != null && search.trim() !== '') {
      queryParams['search'] = search.trim();
    }
    return this.http.get<MedicalFolderPage>(this.baseUrl, { params: queryParams });
  }

  /** Get aggregate stats (total, thisMonth, thisWeek, patientCount). */
  getStats(): Observable<MedicalFolderStats> {
    return this.http.get<MedicalFolderStats>(`${this.baseUrl}/stats`);
  }

  getAll(): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(this.baseUrl);
  }

  getById(id: number): Observable<MedicalFolder> {
    return this.http.get<MedicalFolder>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.post<MedicalFolder>(this.baseUrl, data);
  }

  update(id: number, data: UpdateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.put<MedicalFolder>(`${this.baseUrl}/${id}`, data);
  }

  patch(id: number, data: UpdateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.patch<MedicalFolder>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Get folders by patient (if backend supports it) */
  getMedicalFoldersByPatient(patientId: string): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  /** Get folders for a specific doctor */
  getByDoctorId(doctorId: string): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(`${this.baseUrl}/doctor/${doctorId}`);
  }

  getConsolidatedPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}
