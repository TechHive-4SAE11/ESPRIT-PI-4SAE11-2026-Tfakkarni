import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';

import { AppointmentService } from '@/services/appointment.service';
import { AuthService } from '@/core/auth';
import { Appointment } from '@/models/appointment.model';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { SessionService, SessionResponseDTO } from '@/core/services/session.service';

@Component({
    selector: 'app-doctor-calendar',
    standalone: true,
    imports: [CommonModule, FullCalendarModule],
    templateUrl: './doctor-calendar.component.html',
    styleUrls: ['./doctor-calendar.component.css']
})
export class DoctorCalendarComponent implements OnInit {
    isLoading = signal<boolean>(true);
    calendarOptions = signal<CalendarOptions>({
        plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
        initialView: 'dayGridMonth',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        buttonText: {
            today: "Aujourd'hui",
            month: 'Mois',
            week: 'Semaine',
            day: 'Jour'
        },
        locale: 'fr',
        events: [],
        eventClick: this.handleEventClick.bind(this),
        height: 'auto',
        allDaySlot: true,
        slotMinTime: '08:00:00',
        slotMaxTime: '20:00:00',
    });

    private readonly appointmentService = inject(AppointmentService);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly medicalFolderService = inject(MedicalFolderService);
    private readonly sessionService = inject(SessionService);

    ngOnInit(): void {
        this.loadAllEvents();
    }

  private loadAllEvents(): void {
    const doctorId = this.authService.getKeycloakId();
    console.log('👨‍⚕️ Doctor ID récupéré:', doctorId);

    if (!doctorId) {
      console.warn('⚠️ Aucun ID docteur trouvé');
      this.isLoading.set(false);
      return;
    }

    // Load appointments and sessions in parallel
    const appointments$ = this.appointmentService.getAppointmentsByDoctor(doctorId).pipe(
      catchError(err => { console.error('❌ Erreur rendez-vous:', err); return of([] as Appointment[]); })
    );

    const sessions$ = this.medicalFolderService.getByDoctorId(doctorId).pipe(
      switchMap(folders => {
        if (!folders || folders.length === 0) return of([] as SessionResponseDTO[]);
        return forkJoin(
          folders.map(f => this.sessionService.getSessionsByMedicalFolder(f.id).pipe(
            catchError(() => of([] as SessionResponseDTO[]))
          ))
        ).pipe(
          // flatten the array of arrays
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          switchMap(results => of(results.flat()))
        );
      }),
      catchError(err => { console.error('❌ Erreur sessions:', err); return of([] as SessionResponseDTO[]); })
    );

    forkJoin({ appointments: appointments$, sessions: sessions$ }).subscribe({
      next: ({ appointments, sessions }) => {
        const appointmentEvents = (appointments ?? []).map(apt => this.mapAppointmentToEvent(apt));
        const sessionEvents = (sessions ?? []).map(s => this.mapSessionToEvent(s));
        this.calendarOptions.update(options => ({
          ...options,
          events: [...appointmentEvents, ...sessionEvents],
        }));
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Erreur chargement calendrier:', err);
        this.isLoading.set(false);
      },
    });
  }
    private mapAppointmentToEvent(apt: Appointment): any {
        // Determine color based on appointment type
        let color = '#f97316'; // orange default (AUTRE)
        if (apt.type === 'CONSULTATION') {
            color = '#3b82f6'; // blue
        } else if (apt.type === 'FOLLOW_UP') {
            color = '#22c55e'; // green
        }

        return {
            id: apt.id?.toString(),
          title: `${apt.type} - Patient ${apt.patientId}`,
            start: apt.startTime,
            end: apt.endTime,
            backgroundColor: color,
            borderColor: color,
            textColor: '#ffffff',
            extendedProps: {
                appointmentId: apt.id,
                status: apt.status
            }
        };
    }

    handleEventClick(arg: EventClickArg) {
        const appointmentId = arg.event.extendedProps['appointmentId'];
        if (appointmentId) {
            this.router.navigate(['/appointments', appointmentId]);
        }
    }

    private mapSessionToEvent(session: SessionResponseDTO): object {
        return {
            id: `session-${session.id}`,
            title: `📋 Session #${session.id}`,
            start: session.sessionDate,
            allDay: true,
            backgroundColor: '#8b5cf6', // purple
            borderColor: '#8b5cf6',
            textColor: '#ffffff',
            extendedProps: {
                type: 'session',
                sessionId: session.id,
                notes: session.notes,
            },
        };
    }
}
