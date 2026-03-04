import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface SymptomPrediction {
    condition: string;
    probability: number;
    riskLevel: 'LOW' | 'MODERATE' | 'HIGH';
}

export interface SymptomPilotResponse {
    predictions: SymptomPrediction[];
    isCriticalAlert: boolean;
    alertMessage: string | null;
}

@Injectable({
    providedIn: 'root',
})
export class SymptomPilotService {
    private readonly baseUrl = `${environment.apiBaseUrl}/api/ml/symptom-pilot`;

    constructor(private readonly http: HttpClient) { }

    analyze(symptoms: string): Observable<SymptomPilotResponse> {
        return this.http.post<SymptomPilotResponse>(this.baseUrl, { symptoms });
    }
}
