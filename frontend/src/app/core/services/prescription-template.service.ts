// src/app/core/services/prescription-template.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import {
    PrescriptionTemplateRequestDTO,
    PrescriptionTemplateResponseDTO,
} from '@/core/models/prescription-template.model';

@Injectable({
    providedIn: 'root',
})
export class PrescriptionTemplateService {
    private readonly baseUrl = `${environment.apiBaseUrl}/api/prescription-templates`;

    constructor(private readonly http: HttpClient) { }

    getTemplatesByDoctor(doctorId: string): Observable<PrescriptionTemplateResponseDTO[]> {
        return this.http.get<PrescriptionTemplateResponseDTO[]>(`${this.baseUrl}/doctor/${doctorId}`);
    }

    getTemplateById(id: number): Observable<PrescriptionTemplateResponseDTO> {
        return this.http.get<PrescriptionTemplateResponseDTO>(`${this.baseUrl}/${id}`);
    }

    createTemplate(template: PrescriptionTemplateRequestDTO): Observable<PrescriptionTemplateResponseDTO> {
        return this.http.post<PrescriptionTemplateResponseDTO>(this.baseUrl, template);
    }

    createFromPrescription(
        prescriptionId: number,
        name: string,
        doctorId: string,
        description?: string
    ): Observable<PrescriptionTemplateResponseDTO> {
        let url = `${this.baseUrl}/from-prescription/${prescriptionId}?name=${encodeURIComponent(name)}&doctorId=${encodeURIComponent(doctorId)}`;
        if (description) {
            url += `&description=${encodeURIComponent(description)}`;
        }
        return this.http.post<PrescriptionTemplateResponseDTO>(url, null);
    }

    updateTemplate(id: number, template: PrescriptionTemplateRequestDTO): Observable<PrescriptionTemplateResponseDTO> {
        return this.http.put<PrescriptionTemplateResponseDTO>(`${this.baseUrl}/${id}`, template);
    }

    deleteTemplate(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }

    searchTemplates(doctorId: string, query: string): Observable<PrescriptionTemplateResponseDTO[]> {
        return this.http.get<PrescriptionTemplateResponseDTO[]>(
            `${this.baseUrl}/doctor/${doctorId}/search?query=${encodeURIComponent(query)}`
        );
    }
}
