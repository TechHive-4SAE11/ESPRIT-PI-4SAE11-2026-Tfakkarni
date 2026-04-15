import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DailyLogStateService } from './daily-log-state.service';
import { environment } from '@/environments/environment';
import type { DailyLogResponse, MedicationIntakeLogResponse } from '@/core/models/daily-monitoring.model';

describe('DailyLogStateService', () => {
  let service: DailyLogStateService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/daily-monitoring`;
  const PATIENT_ID = 'patient-abc-123';

  /** Build a today ISO string the same way the service does */
  function todayIso(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  const mockMedIntake: MedicationIntakeLogResponse = {
    id: 20,
    medicationId: 100,
    medicationName: 'Doliprane',
    dosage: '1000mg',
    frequency: '2x/jour',
    status: 'OUBLIE',
  };

  const mockLog: DailyLogResponse = {
    id: 1,
    patientKeycloakId: PATIENT_ID,
    logDate: todayIso(),
    createdAt: '2026-04-15T08:00:00',
    updatedAt: '2026-04-15T08:00:00',
    nutritionEntries: [],
    medicationIntakes: [mockMedIntake],
    activityEntries: [],
    incidentEntries: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DailyLogStateService],
    });
    service = TestBed.inject(DailyLogStateService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ── loadTodayLog ───────────────────────────────────────────────────────

  describe('loadTodayLog', () => {
    it('devrait charger le log du jour et stocker dans le signal', () => {
      service.loadTodayLog(PATIENT_ID).subscribe(log => {
        expect(log).toBeTruthy();
        expect(log!.id).toBe(1);
      });

      const req = httpMock.expectOne(
        `${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockLog);

      // Signal should now contain the log
      expect(service.todayLog()).toBeTruthy();
      expect(service.todayLog()!.id).toBe(1);
      expect(service.loading()).toBeFalse();
    });

    it('devrait retourner le cache si même patient et pas forceRefresh', () => {
      // First load
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);

      // Second load — should NOT make another HTTP call
      service.loadTodayLog(PATIENT_ID).subscribe(log => {
        expect(log).toBeTruthy();
        expect(log!.id).toBe(1);
      });

      httpMock.expectNone(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`);
    });

    it('devrait forcer un rechargement si forceRefresh=true', () => {
      // First load
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);

      // Force refresh
      service.loadTodayLog(PATIENT_ID, true).subscribe();
      const req2 = httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`);
      expect(req2.request.method).toBe('POST');
      req2.flush(mockLog);
    });

    it('devrait gérer les erreurs sans planter', () => {
      service.loadTodayLog(PATIENT_ID).subscribe(log => {
        expect(log).toBeNull();
      });

      const req = httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`);
      req.error(new ProgressEvent('error'));

      expect(service.loading()).toBeFalse();
    });
  });

  // ── Computed helpers ───────────────────────────────────────────────────

  describe('Computed helpers', () => {
    beforeEach(() => {
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);
    });

    it('todayMedications devrait retourner la liste des prises', () => {
      expect(service.todayMedications().length).toBe(1);
      expect(service.todayMedications()[0].medicationName).toBe('Doliprane');
    });

    it('medsTakenCount devrait compter les médicaments PRIS', () => {
      expect(service.medsTakenCount()).toBe(0); // status = OUBLIE
    });

    it('medsTotal devrait retourner le nombre total', () => {
      expect(service.medsTotal()).toBe(1);
    });

    it('medsProgressPercent devrait retourner 0 si aucun PRIS', () => {
      expect(service.medsProgressPercent()).toBe(0);
    });
  });

  // ── updateMedicationStatus (optimistic) ────────────────────────────────

  describe('updateMedicationStatus', () => {
    beforeEach(() => {
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);
    });

    it('devrait mettre à jour le statut d\'un médicament de manière optimiste', () => {
      service.updateMedicationStatus(20, 'PRIS', '09:00');

      const updated = service.todayMedications().find(m => m.id === 20);
      expect(updated).toBeTruthy();
      expect(updated!.status).toBe('PRIS');
      expect(updated!.takenAt).toBe('09:00');
    });

    it('medsTakenCount devrait se mettre à jour après changement de statut', () => {
      expect(service.medsTakenCount()).toBe(0);

      service.updateMedicationStatus(20, 'PRIS');

      expect(service.medsTakenCount()).toBe(1);
      expect(service.medsProgressPercent()).toBe(100);
    });

    it('ne devrait rien faire si todayLog est null', () => {
      service.setLog(null as unknown as DailyLogResponse);
      // Should not throw
      expect(() => service.updateMedicationStatus(20, 'PRIS')).not.toThrow();
    });
  });

  // ── toggleMedication (API call + optimistic update) ────────────────────

  describe('toggleMedication', () => {
    beforeEach(() => {
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);
    });

    it('devrait appeler PUT et mettre à jour le signal', () => {
      const med = service.todayMedications()[0];
      const updatedIntake: MedicationIntakeLogResponse = {
        ...med,
        status: 'PRIS',
        takenAt: '09:30',
      };

      service.toggleMedication(1, med, 'PRIS', '09:30').subscribe(res => {
        expect(res).toBeTruthy();
        expect(res!.status).toBe('PRIS');
      });

      const req = httpMock.expectOne(`${base}/1/medication-intakes/20`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.status).toBe('PRIS');
      req.flush(updatedIntake);

      // Signal should be updated
      expect(service.todayMedications()[0].status).toBe('PRIS');
    });

    it('devrait retourner null en cas d\'erreur serveur', () => {
      const med = service.todayMedications()[0];

      service.toggleMedication(1, med, 'PRIS').subscribe(res => {
        expect(res).toBeNull();
      });

      const req = httpMock.expectOne(`${base}/1/medication-intakes/20`);
      req.error(new ProgressEvent('error'), { status: 500 });
    });
  });

  // ── setLog ─────────────────────────────────────────────────────────────

  describe('setLog', () => {
    it('devrait permettre de forcer un log dans le state', () => {
      const customLog: DailyLogResponse = {
        ...mockLog,
        id: 99,
        medicationIntakes: [
          { ...mockMedIntake, id: 50, status: 'PRIS' },
          { ...mockMedIntake, id: 51, status: 'OUBLIE' },
        ],
      };

      service.setLog(customLog);

      expect(service.todayLog()!.id).toBe(99);
      expect(service.medsTotal()).toBe(2);
      expect(service.medsTakenCount()).toBe(1);
      expect(service.medsProgressPercent()).toBe(50);
    });
  });

  // ── refresh ────────────────────────────────────────────────────────────

  describe('refresh', () => {
    it('devrait recharger depuis le serveur avec forceRefresh', () => {
      // Load initially
      service.loadTodayLog(PATIENT_ID).subscribe();
      httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`).flush(mockLog);

      // Refresh
      service.refresh().subscribe();
      const req = httpMock.expectOne(`${base}/patient/${encodeURIComponent(PATIENT_ID)}/date/${todayIso()}`);
      expect(req.request.method).toBe('POST');
      req.flush(mockLog);
    });

    it('devrait retourner null si aucun patient chargé', () => {
      service.refresh().subscribe(result => {
        expect(result).toBeNull();
      });
      httpMock.expectNone(`${base}`);
    });
  });
});
