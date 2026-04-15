import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  IotService, HeartbeatReading, SleepAnalysisResponse, SleepHistoryResponse,
} from './iot.service';
import { environment } from '@/environments/environment';

describe('IotService', () => {
  let service: IotService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/iot/heartbeat`;
  const testPatientId = 'patient-abc';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        IotService,
      ],
    });
    service = TestBed.inject(IotService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ─── Heartbeat Readings ────────────────────────────────────

  describe('getHeartbeatReadings', () => {
    it('should GET readings without date param', () => {
      const mockReadings: HeartbeatReading[] = [
        { id: 1, patientId: testPatientId, bpm: 72, timestamp: '2026-04-15T10:00:00' },
        { id: 2, patientId: testPatientId, bpm: 75, timestamp: '2026-04-15T10:05:00' },
      ];

      service.getHeartbeatReadings(testPatientId).subscribe(res => {
        expect(res.length).toBe(2);
        expect(res[0].bpm).toBe(72);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testPatientId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('date')).toBeFalse();
      req.flush(mockReadings);
    });

    it('should GET readings with date param', () => {
      service.getHeartbeatReadings(testPatientId, '2026-04-10').subscribe();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testPatientId}` && r.params.get('date') === '2026-04-10'
      );
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  // ─── Sleep Analysis ────────────────────────────────────────

  describe('getSleepAnalysis', () => {
    it('should GET sleep analysis without date', () => {
      const mockAnalysis: SleepAnalysisResponse = {
        patientId: testPatientId,
        date: '2026-04-14',
        timeline: [
          { timestamp: '2026-04-14T22:00:00', bpm: 65, stage: 'LIGHT' },
          { timestamp: '2026-04-14T23:00:00', bpm: 55, stage: 'DEEP' },
          { timestamp: '2026-04-15T01:00:00', bpm: 60, stage: 'REM' },
        ],
        summary: {
          totalSleepMinutes: 420, timeInBedMinutes: 480,
          deepSleepMinutes: 90, lightSleepMinutes: 180,
          remSleepMinutes: 120, awakeMinutes: 30,
          deepSleepPercent: 21, lightSleepPercent: 43,
          remSleepPercent: 29, awakePercent: 7,
          sleepEfficiency: 87, qualityScore: 82,
          awakenings: 3, qualityLabel: 'Good',
        },
        insights: ['Good deep sleep duration', 'Sleep efficiency is above average'],
      };

      service.getSleepAnalysis(testPatientId).subscribe(res => {
        expect(res.summary.qualityLabel).toBe('Good');
        expect(res.timeline.length).toBe(3);
        expect(res.insights.length).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testPatientId}/sleep-analysis`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAnalysis);
    });

    it('should GET sleep analysis with date param', () => {
      service.getSleepAnalysis(testPatientId, '2026-04-06').subscribe();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testPatientId}/sleep-analysis` && r.params.get('date') === '2026-04-06'
      );
      expect(req.request.method).toBe('GET');
      req.flush({
        patientId: testPatientId, date: '2026-04-06',
        timeline: [], summary: {}, insights: [],
      });
    });
  });

  // ─── Sleep History ─────────────────────────────────────────

  describe('getSleepHistory', () => {
    it('should GET 7-day sleep history by default', () => {
      const mockHistory: SleepHistoryResponse = {
        patientId: testPatientId, days: 7,
        entries: [
          {
            date: '2026-04-14',
            summary: {
              totalSleepMinutes: 420, timeInBedMinutes: 480,
              deepSleepMinutes: 90, lightSleepMinutes: 180,
              remSleepMinutes: 120, awakeMinutes: 30,
              deepSleepPercent: 21, lightSleepPercent: 43,
              remSleepPercent: 29, awakePercent: 7,
              sleepEfficiency: 87, qualityScore: 82,
              awakenings: 3, qualityLabel: 'Good',
            },
            insights: ['Good sleep'],
          },
        ],
        weeklySummary: {
          avgQualityScore: 78, avgQualityLabel: 'Good',
          avgTotalSleepMinutes: 400, avgDeepSleepPercent: 20,
          avgEfficiency: 85, totalAwakenings: 15,
          nightsWithData: 5, bestNight: '2026-04-14', bestNightScore: 90,
          worstNight: '2026-04-10', worstNightScore: 60,
          trend: 'IMPROVING', weeklyInsights: ['Sleep quality improving'],
        },
      };

      service.getSleepHistory(testPatientId).subscribe(res => {
        expect(res.days).toBe(7);
        expect(res.entries.length).toBe(1);
        expect(res.weeklySummary.trend).toBe('IMPROVING');
      });

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testPatientId}/sleep-history` && r.params.get('days') === '7'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockHistory);
    });

    it('should pass custom days param', () => {
      service.getSleepHistory(testPatientId, 14).subscribe();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testPatientId}/sleep-history` && r.params.get('days') === '14'
      );
      req.flush({ patientId: testPatientId, days: 14, entries: [], weeklySummary: {} });
    });
  });

  // ─── Record Heartbeat ──────────────────────────────────────

  describe('recordHeartbeat', () => {
    it('should POST a heartbeat reading', () => {
      const reading: Partial<HeartbeatReading> = { patientId: testPatientId, bpm: 130 };
      const mockResponse: HeartbeatReading = {
        id: 100, patientId: testPatientId, bpm: 130, timestamp: '2026-04-15T10:00:00',
      };

      service.recordHeartbeat(reading).subscribe(res => {
        expect(res.id).toBe(100);
        expect(res.bpm).toBe(130);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(reading);
      req.flush(mockResponse);
    });
  });

  // ─── Latest Reading ────────────────────────────────────────

  describe('getLatestReading', () => {
    it('should GET the latest reading', () => {
      const mockReading: HeartbeatReading = {
        id: 50, patientId: testPatientId, bpm: 68, timestamp: '2026-04-15T12:00:00',
      };

      service.getLatestReading(testPatientId).subscribe(res => {
        expect(res).toBeTruthy();
        expect(res!.bpm).toBe(68);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testPatientId}/latest`);
      expect(req.request.method).toBe('GET');
      req.flush(mockReading);
    });
  });

  // ─── Dweet.cc Live BPM ────────────────────────────────────

  describe('getLiveBpmFromDweet', () => {
    it('should return BPM from a successful dweet response', () => {
      const dweetResponse = {
        this: 'succeeded',
        with: [{ content: { bpm: '72' } }],
      };

      service.getLiveBpmFromDweet('tfakkarni-high-1').subscribe(bpm => {
        expect(bpm).toBe(72);
      });

      const req = httpMock.expectOne('/dweet-proxy/get/latest/dweet/for/tfakkarni-high-1');
      expect(req.request.method).toBe('GET');
      req.flush(dweetResponse);
    });

    it('should return null if dweet response failed', () => {
      const dweetResponse = { this: 'failed' };

      service.getLiveBpmFromDweet('test-thing').subscribe(bpm => {
        expect(bpm).toBeNull();
      });

      const req = httpMock.expectOne('/dweet-proxy/get/latest/dweet/for/test-thing');
      req.flush(dweetResponse);
    });

    it('should return null if content has no bpm', () => {
      const dweetResponse = {
        this: 'succeeded',
        with: [{ content: { temperature: 36 } }],
      };

      service.getLiveBpmFromDweet('test-thing').subscribe(bpm => {
        expect(bpm).toBeNull();
      });

      const req = httpMock.expectOne('/dweet-proxy/get/latest/dweet/for/test-thing');
      req.flush(dweetResponse);
    });

    it('should return null if with array is empty', () => {
      const dweetResponse = { this: 'succeeded', with: [] };

      service.getLiveBpmFromDweet('test-thing').subscribe(bpm => {
        expect(bpm).toBeNull();
      });

      const req = httpMock.expectOne('/dweet-proxy/get/latest/dweet/for/test-thing');
      req.flush(dweetResponse);
    });

    it('should URL-encode the thing name', () => {
      service.getLiveBpmFromDweet('my thing').subscribe();

      const req = httpMock.expectOne('/dweet-proxy/get/latest/dweet/for/my%20thing');
      req.flush({ this: 'succeeded', with: [{ content: { bpm: '80' } }] });
    });
  });
});
