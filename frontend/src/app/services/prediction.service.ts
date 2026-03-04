import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardStats, Prediction } from '../models/prediction.model';

@Injectable({
    providedIn: 'root'
})
export class PredictionService {
    private apiUrl = 'http://localhost:18086/api/medical/predictions';

    constructor(private http: HttpClient) { }

    getDashboardStats(): Observable<DashboardStats> {
        return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard`);
    }

    getPredictionForAppointment(id: number): Observable<Prediction> {
        return this.http.get<Prediction>(`${this.apiUrl}/appointment/${id}`);
    }
}
