import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

import { AppointmentService } from '@/services/appointment.service';
import { AuthService } from '@/core/auth';
import { Appointment } from '@/models/appointment.model';

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

    private appointmentService = inject(AppointmentService);
    private authService = inject(AuthService);
    private router = inject(Router);

    ngOnInit(): void {
        this.loadAppointments();
    }

  private loadAppointments(): void {
    const doctorId = this.authService.getKeycloakId();
    console.log('👨‍⚕️ Doctor ID récupéré:', doctorId);

    if (!doctorId) {
      console.warn('⚠️ Aucun ID docteur trouvé');
      this.isLoading.set(false);
      return;
    }

    console.log('🟡 Tentative de chargement des rendez-vous pour le docteur:', doctorId);

    this.appointmentService.getAppointmentsByDoctor(doctorId).subscribe({
      next: (appointments) => {
        console.log('✅ Rendez-vous reçus du backend:', appointments);

        if (!appointments || appointments.length === 0) {
          console.log('📭 Aucun rendez-vous trouvé pour ce docteur');
          this.calendarOptions.update(options => ({ ...options, events: [] }));
        } else {
          const events = appointments.map(apt => this.mapAppointmentToEvent(apt));
          console.log('📅 Événements générés pour le calendrier:', events);

          this.calendarOptions.update(options => ({
            ...options,
            events: events
          }));
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Erreur lors de la récupération des rendez-vous:', err);
        console.error('Status:', err.status);
        console.error('Message:', err.message);
        console.error('URL qui a échoué:', err.url);
        this.isLoading.set(false);
      },
      complete: () => {
        console.log('🏁 Requête terminée');
      }
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
}
