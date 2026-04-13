import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Questionnaire, QuestionnaireSubmissionDTO } from '../models/questionnaire.model';
import { CarePlanResponseDTO } from '../models/care-plan.model';

@Injectable({
  providedIn: 'root',
})
export class QuestionnaireService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/questionnaires`;

  constructor(private readonly http: HttpClient) {}

  getAllQuestionnaires(): Observable<Questionnaire[]> {
    return this.http.get<Questionnaire[]>(this.baseUrl);
  }

  submitAndRecommend(submission: QuestionnaireSubmissionDTO): Observable<CarePlanResponseDTO> {
    return this.http.post<CarePlanResponseDTO>(`${this.baseUrl}/submit`, submission);
  }

  getRecommendation(submission: QuestionnaireSubmissionDTO): Observable<CarePlanResponseDTO> {
    return this.http.post<CarePlanResponseDTO>(`${this.baseUrl}/recommend`, submission);
  }
}
