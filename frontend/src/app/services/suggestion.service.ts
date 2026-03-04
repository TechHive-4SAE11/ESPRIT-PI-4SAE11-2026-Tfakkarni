import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SlotSuggestion } from '@/models/suggestion.model';

@Injectable({
  providedIn: 'root',
})
export class SuggestionService {
  private apiUrl = 'http://localhost:18086/api/medical/appointments';

  constructor(private http: HttpClient) {}

  getSuggestions(
    appointmentId: number,
    count: number = 3
  ): Observable<SlotSuggestion[]> {
    return this.http.get<SlotSuggestion[]>(
      `${this.apiUrl}/${appointmentId}/suggestions?count=${count}`
    );
  }
}

