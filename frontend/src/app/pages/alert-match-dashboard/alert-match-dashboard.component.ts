import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { DashboardService } from '../../services/dashboard.service';
import { ZardCardComponent } from '../../shared/components/card';
import { ZardButtonComponent } from '../../shared/components/button';
import { ZardIconComponent } from '../../shared/components/icon';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-alert-match-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  templateUrl: './alert-match-dashboard.component.html',
})
export class AlertMatchDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  // Données principales
  medicalAlert = signal<any>(null);
  compliance = signal<any>(null);
  matchingResult = signal<any>(null);

  // États de chargement
  isLoading = signal<boolean>(false);
  isMatchingLoading = signal<boolean>(false);
  error = signal<string | null>(null);

  // Données pour la liste des médecins et le formulaire
  availableDoctors = signal<any[]>([]);
  selectedDoctor = signal<any>(null);
  showDoctorList = signal<boolean>(false);
  showAppointmentForm = signal<boolean>(false);

  appointmentData = signal({
    patientId: '',
    doctorId: '',
    doctorName: '',
    specialty: '',
    dateTime: '',
    reason: ''
  });

  ngOnInit(): void {
    this.loadDemoData();
    this.loadRealData();
  }

  loadDemoData(): void {
    this.medicalAlert.set({
      severity: 'MODEREE',
      score: 70,
      message: 'Score moyen, suivi recommandé',
      action: 'Planifier suivi'
    });
    this.compliance.set({
      severity: 'INFO',
      score: 75,
      message: 'Bonne observance, continuez !'
    });
  }

  loadRealData(): void {
    const id = 'fec80b9c-6b27-45a3-8cf2-511103bdbec2';
    forkJoin({
      alert: this.dashboardService.getAlerts(id),
      compliance: this.dashboardService.getCompliance(id)
    }).subscribe({
      next: (data) => {
        this.medicalAlert.set(data.alert);
        this.compliance.set(data.compliance);
      },
      error: (err) => console.error('Erreur API:', err)
    });
  }

  triggerMatching(actionRequested: string): void {
    this.isMatchingLoading.set(true);
    this.matchingResult.set(null);
    this.showDoctorList.set(false);
    this.showAppointmentForm.set(false);

    // VRAI APPEL API AVEC FALLBACK MOCK
    this.dashboardService.triggerMatching('fec80b9c-6b27-45a3-8cf2-511103bdbec2')
      .subscribe({
        next: (result) => {
          console.log('✅ API matching succès:', result);
          this.matchingResult.set(result);
          this.isMatchingLoading.set(false);
          this.loadAvailableDoctors(result.specialty, actionRequested);
        },
        error: (err) => {
          console.error('❌ Erreur API matching, utilisation du mock:', err);
          // FALLBACK MOCK
          const mockResult = {
            specialty: 'NEUROLOGIE',
            message: 'Recommandation basée sur vos antécédents '
          };
          this.matchingResult.set(mockResult);
          this.isMatchingLoading.set(false);
          this.loadAvailableDoctors(mockResult.specialty, actionRequested);
        }
      });
  }

  loadAvailableDoctors(specialty: string, actionType: string): void {
    this.dashboardService.getAvailableDoctors(specialty).subscribe({
      next: (doctors) => {
        console.log('✅ Médecins chargés:', doctors);
        this.availableDoctors.set(doctors);
        this.showDoctorList.set(true);
      },
      error: (err) => {
        console.error('❌ Erreur chargement médecins, utilisation du mock:', err);
        // FALLBACK MOCK pour les médecins
        let mockDoctors = [];
        if (specialty === 'NEUROLOGIE') {
          mockDoctors = [
            { id: 1, name: 'Dr. Amine Ben Salem', specialty: 'Neurologue', nextAvailable: 'Demain 14h30' },
            { id: 2, name: 'Dr. Sarra Mansouri', specialty: 'Neurologue', nextAvailable: 'Mercredi 10h00' }
          ];
        } else {
          mockDoctors = [
            { id: 3, name: 'Dr. Amine ', specialty: 'Généraliste', nextAvailable: 'Demain 14h30' },
            { id: 4, name: 'Dr.  Mansouri', specialty: 'Généraliste', nextAvailable: 'Jeudi 9h00' }
          ];
        }
        this.availableDoctors.set(mockDoctors);
        this.showDoctorList.set(true);
      }
    });
  }

  selectDoctor(doctor: any): void {
    this.selectedDoctor.set(doctor);
    this.showDoctorList.set(false);
    this.showAppointmentForm.set(true);

    this.appointmentData.update(data => ({
      ...data,
      patientId: 'fec80b9c-6b27-45a3-8cf2-511103bdbec2',
      doctorId: doctor.id,
      doctorName: doctor.name,
      specialty: this.matchingResult()?.specialty || '',
      dateTime: this.getDefaultDateTime(),
      reason: this.matchingResult()?.message || 'Consultation de suivi'
    }));
  }

  createAppointment(): void {
    const data = {
      patientId: this.appointmentData().patientId,
      doctorId: this.appointmentData().doctorId,
      dateTime: this.appointmentData().dateTime,
      reason: this.appointmentData().reason
    };

    this.dashboardService.createAppointment(data).subscribe({
      next: (res) => {
        alert('✅ Rendez-vous créé avec succès !');
        this.showAppointmentForm.set(false);
        this.showDoctorList.set(false);
      },
      error: (err) => {
        console.error('Erreur création RDV', err);
        alert('❌ Erreur lors de la création du rendez-vous');
      }
    });
  }

  getDefaultDateTime(): string {
    const date = new Date();
    date.setDate(date.getDate() + 2);
    date.setHours(14, 0, 0);
    return date.toISOString().slice(0, 16);
  }

  goBack(): void {
    window.history.back();
  }
}
