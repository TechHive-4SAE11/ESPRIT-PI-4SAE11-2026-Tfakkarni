import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:18085/api/ml/dashboard';

  getAlerts(keycloakId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/alerts/${keycloakId}`);
  }

  getCompliance(patientId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/compliance/${patientId}`);
  }

  triggerMatching(patientId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/match/${patientId}`);
  }

  getAvailableDoctors(specialty: string): Observable<any[]> {
    // Mock basé sur la spécialité (fallback si API réelle indisponible)
    let mockDoctors = [];

    if (specialty === 'NEUROLOGIE') {
      mockDoctors = [
        { id: 1, name: 'Dr. Sophie Martin', specialty: 'Neurologue', nextAvailable: 'Demain 14h30', address: '15 rue des Lilas, Paris' },
        { id: 2, name: 'Dr. Jean Dupont', specialty: 'Neurologue', nextAvailable: 'Mercredi 10h00', address: '8 avenue de la République, Paris' }
      ];
    } else if (specialty === 'CARDIOLOGIE') {
      mockDoctors = [
        { id: 3, name: 'Dr. Pierre Durand', specialty: 'Cardiologue', nextAvailable: 'Demain 9h00', address: '5 rue Pasteur, Tunis' }
      ];
    } else {
      mockDoctors = [
        { id: 4, name: 'Dr. Amine Ben Salem', specialty: 'Généraliste', nextAvailable: 'Demain 14h30', address: '15 rue de la République, Tunis' },
        { id: 5, name: 'Dr. Sarra Mansouri', specialty: 'Généraliste', nextAvailable: 'Jeudi 9h00', address: '22 boulevard Saint-Germain, Sousse' }
      ];
    }

    return of(mockDoctors);
  }

  createAppointment(data: any): Observable<any> {
    console.log('📅 Rendez-vous créé:', data);
    return of({ success: true, id: Date.now(), message: 'Rendez-vous créé avec succès' });
  }
}
