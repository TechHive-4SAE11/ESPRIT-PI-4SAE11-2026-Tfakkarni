import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment } from '../models/appointment.model';
import { environment } from '@/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = `${environment.apiBaseUrl}/api/medical/appointments`;

  constructor(private http: HttpClient) {}

  getAllAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(this.apiUrl);
  }

  getAppointmentById(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.apiUrl}/${id}`);
  }

  createAppointment(appointment: Appointment): Observable<Appointment> {
    return this.http.post<Appointment>(this.apiUrl, appointment);
  }

  createRecurringAppointments(
    appointmentData: any,
    frequency: string,
    occurrences: number
  ): Observable<Appointment[]> {
    const payload = {
      appointmentRequest: appointmentData,
      frequency,
      numberOfOccurrences: occurrences,
    };
    return this.http.post<Appointment[]>(`${this.apiUrl}/recurring`, payload);
  }

  

  updateAppointment(id: number, appointment: Appointment): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.apiUrl}/${id}`, appointment);
  }

  cancelAppointment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAppointmentsByPatient(patientId: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiUrl}/patient/${patientId}`);
  }

  getAppointmentsByDoctor(doctorId: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiUrl}/doctor/${doctorId}`);
  }

  /** Mark as no-show (updates medical folder attendance rules). */
  markNoShow(appointmentId: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/${appointmentId}/no-show`, {});
  }

  /** Mark as completed (breaks no-show streak). */
  markCompleted(appointmentId: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/${appointmentId}/complete`, {});
  }
}
