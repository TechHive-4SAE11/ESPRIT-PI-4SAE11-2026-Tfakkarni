import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CareActivityResponseDTO,
  CarePlanRequestDTO,
  CarePlanResponseDTO,
} from '@/core/models/care-plan.model';
import { PagedResponse } from '@/core/models/paged-response.model';

@Injectable({
  providedIn: 'root',
})
export class CarePlanService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/care-plans`;

  constructor(private readonly http: HttpClient) {}

  getAllCarePlans(): Observable<CarePlanResponseDTO[]> {
    return this.http.get<CarePlanResponseDTO[]>(this.baseUrl);
  }

  getCarePlanById(id: number): Observable<CarePlanResponseDTO> {
    return this.http.get<CarePlanResponseDTO>(`${this.baseUrl}/${id}`);
  }

  getCarePlansBySession(sessionId: number): Observable<CarePlanResponseDTO[]> {
    return this.http.get<CarePlanResponseDTO[]>(`${this.baseUrl}/session/${sessionId}`);
  }

  getCarePlansByPatient(patientId: string): Observable<CarePlanResponseDTO[]> {
    return this.http.get<CarePlanResponseDTO[]>(`${this.baseUrl}/patient/${patientId}`);
  }
  
  getCarePlansByPatientPaginated(
    patientId: string, 
    page: number = 0, 
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDir: 'ASC' | 'DESC' = 'DESC'
  ): Observable<PagedResponse<CarePlanResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    return this.http.get<PagedResponse<CarePlanResponseDTO>>(
      `${this.baseUrl}/patient/${patientId}/paginated`,
      { params }
    );
  }

  createCarePlan(carePlan: CarePlanRequestDTO): Observable<CarePlanResponseDTO> {
    return this.http.post<CarePlanResponseDTO>(this.baseUrl, carePlan);
  }

  updateCarePlan(id: number, carePlan: CarePlanRequestDTO): Observable<CarePlanResponseDTO> {
    return this.http.put<CarePlanResponseDTO>(`${this.baseUrl}/${id}`, carePlan);
  }

  updateActivityStatus(activityId: number, status: string): Observable<CareActivityResponseDTO> {
    return this.http.patch<CareActivityResponseDTO>(`${this.baseUrl}/activities/${activityId}/status`, { status });
  }

  deleteCarePlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
