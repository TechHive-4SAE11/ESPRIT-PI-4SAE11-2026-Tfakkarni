import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DailyMonitoringService } from './daily-monitoring.service';
import { environment } from '@/environments/environment';
import type {
  DailyLogResponse,
  NutritionEntryRequest,
  NutritionEntryResponse,
  MedicationIntakeLogRequest,
  MedicationIntakeLogResponse,
  ActivityEntryRequest,
  ActivityEntryResponse,
  IncidentEntryRequest,
  IncidentEntryResponse,
  AvailableMedication,
} from '@/core/models/daily-monitoring.model';

describe('DailyMonitoringService', () => {
  let service: DailyMonitoringService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/daily-monitoring`;
  const PATIENT_ID = 'patient-abc-123';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DailyMonitoringService],
    });
    service = TestBed.inject(DailyMonitoringService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ── Daily log ──────────────────────────────────────────────────────────

  describe('Daily Log', () => {
    const mockLog: DailyLogResponse = {
      id: 1,
      patientKeycloakId: PATIENT_ID,
      logDate: '2026-04-15',
      createdAt: '2026-04-15T08:00:00',
      updatedAt: '2026-04-15T08:00:00',
      nutritionEntries: [],
      medicationIntakes: [],
      activityEntries: [],
      incidentEntries: [],
    };

    it('devrait créer ou retourner le log du jour via POST', () => {
      service.getOrCreateLogForDate(PATIENT_ID, '2026-04-15').subscribe(log => {
        expect(log.id).toBe(1);
        expect(log.patientKeycloakId).toBe(PATIENT_ID);
      });

      const req = httpMock.expectOne(`${base}/patient/${PATIENT_ID}/date/2026-04-15`);
      expect(req.request.method).toBe('POST');
      req.flush(mockLog);
    });

    it('devrait récupérer tous les logs d\'un patient via GET', () => {
      service.getLogsForPatient(PATIENT_ID).subscribe(logs => {
        expect(logs.length).toBe(1);
      });

      const req = httpMock.expectOne(`${base}/patient/${PATIENT_ID}`);
      expect(req.request.method).toBe('GET');
      req.flush([mockLog]);
    });

    it('devrait récupérer un log par ID via GET', () => {
      service.getLogById(1).subscribe(log => {
        expect(log.id).toBe(1);
      });

      const req = httpMock.expectOne(`${base}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLog);
    });
  });

  // ── Available Medications ──────────────────────────────────────────────

  describe('Available Medications', () => {
    it('devrait récupérer les médicaments disponibles', () => {
      const mockMeds: AvailableMedication[] = [
        { id: 100, medicationName: 'Doliprane', dosage: '1000mg', frequency: '2x/jour' },
        { id: 101, medicationName: 'Aspirin', dosage: '500mg' },
      ];

      service.getAvailableMedications(PATIENT_ID).subscribe(meds => {
        expect(meds.length).toBe(2);
        expect(meds[0].medicationName).toBe('Doliprane');
      });

      const req = httpMock.expectOne(`${base}/patient/${PATIENT_ID}/available-medications`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMeds);
    });
  });

  // ── Nutrition CRUD ─────────────────────────────────────────────────────

  describe('Nutrition CRUD', () => {
    const mockNutrition: NutritionEntryResponse = {
      id: 10,
      mealType: 'BREAKFAST',
      description: 'Tartine beurre',
      quantity: 'COMPLET',
      appetite: 'BON',
      hydrationMl: 250,
      entryTime: '08:00',
    };

    it('devrait ajouter une entrée nutrition via POST', () => {
      const dto: NutritionEntryRequest = {
        mealType: 'BREAKFAST',
        description: 'Tartine beurre',
        quantity: 'COMPLET',
        appetite: 'BON',
        hydrationMl: 250,
        entryTime: '08:00',
      };

      service.addNutrition(1, dto).subscribe(res => {
        expect(res.id).toBe(10);
        expect(res.mealType).toBe('BREAKFAST');
      });

      const req = httpMock.expectOne(`${base}/1/nutrition`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(dto);
      req.flush(mockNutrition);
    });

    it('devrait mettre à jour une nutrition via PUT', () => {
      const dto: NutritionEntryRequest = {
        mealType: 'LUNCH',
        description: 'Couscous',
        quantity: 'DEMI',
        appetite: 'MOYEN',
      };

      service.updateNutrition(1, 10, dto).subscribe(res => {
        expect(res.id).toBe(10);
      });

      const req = httpMock.expectOne(`${base}/1/nutrition/10`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockNutrition, mealType: 'LUNCH' });
    });

    it('devrait supprimer une nutrition via DELETE', () => {
      service.deleteNutrition(1, 10).subscribe();

      const req = httpMock.expectOne(`${base}/1/nutrition/10`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ── Medication Intake CRUD ─────────────────────────────────────────────

  describe('Medication Intake CRUD', () => {
    const mockIntake: MedicationIntakeLogResponse = {
      id: 20,
      medicationId: 100,
      medicationName: 'Doliprane',
      dosage: '1000mg',
      frequency: '2x/jour',
      takenAt: '08:30',
      status: 'PRIS',
    };

    it('devrait ajouter une prise de médicament via POST', () => {
      const dto: MedicationIntakeLogRequest = {
        medicationId: 100,
        takenAt: '08:30',
        status: 'PRIS',
        notes: 'Avec eau',
      };

      service.addMedicationIntake(1, dto).subscribe(res => {
        expect(res.id).toBe(20);
        expect(res.status).toBe('PRIS');
        expect(res.medicationName).toBe('Doliprane');
      });

      const req = httpMock.expectOne(`${base}/1/medication-intakes`);
      expect(req.request.method).toBe('POST');
      req.flush(mockIntake);
    });

    it('devrait mettre à jour une prise via PUT', () => {
      const dto: MedicationIntakeLogRequest = {
        medicationId: 100,
        status: 'PRIS',
        takenAt: '09:00',
      };

      service.updateMedicationIntake(1, 20, dto).subscribe(res => {
        expect(res.id).toBe(20);
      });

      const req = httpMock.expectOne(`${base}/1/medication-intakes/20`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockIntake);
    });

    it('devrait supprimer une prise via DELETE', () => {
      service.deleteMedicationIntake(1, 20).subscribe();

      const req = httpMock.expectOne(`${base}/1/medication-intakes/20`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ── Activity CRUD ──────────────────────────────────────────────────────

  describe('Activity CRUD', () => {
    const mockActivity: ActivityEntryResponse = {
      id: 30,
      activityType: 'PHYSIQUE',
      description: 'Marche dans le jardin',
      durationMinutes: 30,
      intensity: 'MODERE',
      startTime: '10:00',
    };

    it('devrait ajouter une activité via POST', () => {
      const dto: ActivityEntryRequest = {
        activityType: 'PHYSIQUE',
        description: 'Marche dans le jardin',
        durationMinutes: 30,
        intensity: 'MODERE',
        startTime: '10:00',
      };

      service.addActivity(1, dto).subscribe(res => {
        expect(res.id).toBe(30);
        expect(res.activityType).toBe('PHYSIQUE');
        expect(res.durationMinutes).toBe(30);
      });

      const req = httpMock.expectOne(`${base}/1/activities`);
      expect(req.request.method).toBe('POST');
      req.flush(mockActivity);
    });

    it('devrait mettre à jour une activité via PUT', () => {
      const dto: ActivityEntryRequest = {
        activityType: 'COGNITIVE',
        description: 'Puzzle',
        durationMinutes: 20,
        intensity: 'FAIBLE',
      };

      service.updateActivity(1, 30, dto).subscribe(res => {
        expect(res.id).toBe(30);
      });

      const req = httpMock.expectOne(`${base}/1/activities/30`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockActivity);
    });

    it('devrait supprimer une activité via DELETE', () => {
      service.deleteActivity(1, 30).subscribe();

      const req = httpMock.expectOne(`${base}/1/activities/30`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ── Incident CRUD ──────────────────────────────────────────────────────

  describe('Incident CRUD', () => {
    const mockIncident: IncidentEntryResponse = {
      id: 40,
      incidentType: 'CHUTE',
      description: 'Chute dans le salon',
      severity: 'GRAVE',
      location: 'Salon',
      actionTaken: 'Glace appliquée',
      injuryDetails: 'Hématome genou',
      occurredAt: '14:30',
    };

    it('devrait ajouter un incident via POST', () => {
      const dto: IncidentEntryRequest = {
        incidentType: 'CHUTE',
        description: 'Chute dans le salon',
        severity: 'GRAVE',
        location: 'Salon',
        actionTaken: 'Glace appliquée',
        injuryDetails: 'Hématome genou',
        occurredAt: '14:30',
      };

      service.addIncident(1, dto).subscribe(res => {
        expect(res.id).toBe(40);
        expect(res.severity).toBe('GRAVE');
        expect(res.incidentType).toBe('CHUTE');
      });

      const req = httpMock.expectOne(`${base}/1/incidents`);
      expect(req.request.method).toBe('POST');
      req.flush(mockIncident);
    });

    it('devrait mettre à jour un incident via PUT', () => {
      const dto: IncidentEntryRequest = {
        incidentType: 'CONFUSION',
        description: 'Confusion passagère',
        severity: 'LEGER',
      };

      service.updateIncident(1, 40, dto).subscribe(res => {
        expect(res.id).toBe(40);
      });

      const req = httpMock.expectOne(`${base}/1/incidents/40`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockIncident);
    });

    it('devrait supprimer un incident via DELETE', () => {
      service.deleteIncident(1, 40).subscribe();

      const req = httpMock.expectOne(`${base}/1/incidents/40`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ── Voice Note ─────────────────────────────────────────────────────────

  describe('Voice Note', () => {
    it('devrait uploader une note vocale via POST multipart avec langue', () => {
      const formData = new FormData();
      formData.append('audio', new Blob(['audio data']), 'recording.webm');

      service.uploadVoiceNote(1, formData, 'fr').subscribe(res => {
        expect(res.text).toBe('Le patient a bien dormi');
      });

      const req = httpMock.expectOne(`${base}/1/voice-note?language=fr`);
      expect(req.request.method).toBe('POST');
      req.flush({ text: 'Le patient a bien dormi' });
    });

    it('devrait uploader sans langue', () => {
      const formData = new FormData();

      service.uploadVoiceNote(1, formData).subscribe();

      const req = httpMock.expectOne(`${base}/1/voice-note`);
      expect(req.request.method).toBe('POST');
      req.flush({ text: 'transcription' });
    });

    it('devrait supprimer la note vocale via DELETE', () => {
      service.deleteVoiceNote(1).subscribe();

      const req = httpMock.expectOne(`${base}/1/voice-note`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('devrait retourner un blob audio via TTS POST', () => {
      service.speakVoiceNote(1, 'ar').subscribe(blob => {
        expect(blob).toBeTruthy();
      });

      const req = httpMock.expectOne(`${base}/1/voice-note/tts?language=ar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['audio']));
    });
  });
});
