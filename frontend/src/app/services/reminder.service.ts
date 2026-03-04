import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Reminder,
  CreateReminderDTO,
  UpdateReminderDTO,
} from '../models/reminder.model';

@Injectable({
  providedIn: 'root',
})
export class ReminderService {
  private readonly apiUrl = 'http://localhost:18086/api/medical/appointments';

  constructor(private readonly http: HttpClient) {}

  getRemindersByAppointment(appointmentId: number): Observable<Reminder[]> {
    return this.http.get<Reminder[]>(
      `${this.apiUrl}/${appointmentId}/reminders`,
    );
  }

  createReminder(
    appointmentId: number,
    data: CreateReminderDTO,
  ): Observable<Reminder> {
    return this.http.post<Reminder>(
      `${this.apiUrl}/${appointmentId}/reminders`,
      { ...data, appointmentId },
    );
  }

  getReminderById(reminderId: number): Observable<Reminder> {
    return this.http.get<Reminder>(
      `${this.apiUrl}/reminders/${reminderId}`,
    );
  }

  updateReminder(
    reminderId: number,
    data: UpdateReminderDTO,
  ): Observable<Reminder> {
    return this.http.put<Reminder>(
      `${this.apiUrl}/reminders/${reminderId}`,
      data,
    );
  }

  deleteReminder(reminderId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/reminders/${reminderId}`,
    );
  }
}