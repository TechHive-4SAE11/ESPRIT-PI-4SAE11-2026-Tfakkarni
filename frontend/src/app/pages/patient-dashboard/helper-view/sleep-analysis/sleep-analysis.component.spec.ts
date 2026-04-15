import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SleepAnalysisComponent } from './sleep-analysis.component';
import { IotService, SleepAnalysisResponse, SleepHistoryResponse } from '@/core/services/iot.service';
import { environment } from '@/environments/environment';

describe('SleepAnalysisComponent', () => {
  let component: SleepAnalysisComponent;
  let fixture: ComponentFixture<SleepAnalysisComponent>;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/iot/heartbeat`;
  const testKeycloakId = 'patient-sleep-test';

  const mockSleepAnalysis: SleepAnalysisResponse = {
    patientId: testKeycloakId,
    date: '2026-04-06',
    timeline: [
      { timestamp: '2026-04-06T22:00:00', bpm: 65, stage: 'LIGHT' },
      { timestamp: '2026-04-06T23:00:00', bpm: 55, stage: 'DEEP' },
      { timestamp: '2026-04-07T01:00:00', bpm: 60, stage: 'REM' },
      { timestamp: '2026-04-07T03:00:00', bpm: 70, stage: 'AWAKE' },
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

  const mockSleepHistory: SleepHistoryResponse = {
    patientId: testKeycloakId, days: 7,
    entries: [
      {
        date: '2026-04-06', insights: ['Stable'],
        summary: { ...mockSleepAnalysis.summary },
      },
    ],
    weeklySummary: {
      avgQualityScore: 78, avgQualityLabel: 'Good',
      avgTotalSleepMinutes: 400, avgDeepSleepPercent: 20,
      avgEfficiency: 85, totalAwakenings: 15,
      nightsWithData: 5, bestNight: '2026-04-06', bestNightScore: 90,
      worstNight: '2026-04-03', worstNightScore: 60,
      trend: 'IMPROVING', weeklyInsights: ['Sleep improving'],
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SleepAnalysisComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SleepAnalysisComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    component.keycloakId = testKeycloakId;
  });

  afterEach(() => {
    component.stopLiveTracking();
    httpMock.verify();
  });

  // ─── Initialization ────────────────────────────────────────

  describe('initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should have correct default state', () => {
      expect(component.isLoading()).toBeFalse();
      expect(component.isLiveTracking()).toBeFalse();
      expect(component.liveBpm()).toBeNull();
      expect(component.analysis()).toBeNull();
      expect(component.error()).toBeNull();
      expect(component.selectedDate()).toBe('2026-04-06');
    });

    it('should load analysis and history on init', () => {
      fixture.detectChanges(); // triggers ngOnInit

      const analysisReq = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-analysis`
      );
      analysisReq.flush(mockSleepAnalysis);

      const historyReq = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-history`
      );
      historyReq.flush(mockSleepHistory);

      expect(component.analysis()).toBeTruthy();
      expect(component.analysis()!.summary.qualityLabel).toBe('Good');
      expect(component.sleepHistory()).toBeTruthy();
    });
  });

  // ─── loadAnalysis ──────────────────────────────────────────

  describe('loadAnalysis', () => {
    it('should set loading state during fetch', () => {
      component.loadAnalysis();
      expect(component.isLoading()).toBeTrue();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-analysis`
      );
      req.flush(mockSleepAnalysis);

      expect(component.isLoading()).toBeFalse();
    });

    it('should set error when analysis returns empty timeline', () => {
      component.loadAnalysis();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-analysis`
      );
      req.flush({ ...mockSleepAnalysis, timeline: [] });

      expect(component.error()).toContain('No heartbeat data');
      expect(component.analysis()).toBeNull();
    });

    it('should set error on HTTP failure', () => {
      component.loadAnalysis();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-analysis`
      );
      req.flush('Error', { status: 500, statusText: 'Server Error' });

      expect(component.error()).toContain('Failed to load');
      expect(component.analysis()).toBeNull();
    });

    it('should not load if keycloakId is empty', () => {
      component.keycloakId = '';
      component.loadAnalysis();
      httpMock.expectNone(r => r.url.includes('sleep-analysis'));
    });
  });

  // ─── loadHistory ───────────────────────────────────────────

  describe('loadHistory', () => {
    it('should load 7-day sleep history', () => {
      component.loadHistory();
      expect(component.isLoadingHistory()).toBeTrue();

      const req = httpMock.expectOne(r =>
        r.url === `${baseUrl}/${testKeycloakId}/sleep-history`
      );
      req.flush(mockSleepHistory);

      expect(component.isLoadingHistory()).toBeFalse();
      expect(component.sleepHistory()).toBeTruthy();
      expect(component.sleepHistory()!.entries.length).toBe(1);
    });

    it('should not load if keycloakId is empty', () => {
      component.keycloakId = '';
      component.loadHistory();
      httpMock.expectNone(r => r.url.includes('sleep-history'));
      expect(component.isLoadingHistory()).toBeFalse();
    });
  });

  // ─── Helper Methods ────────────────────────────────────────

  describe('formatMinutes', () => {
    it('should format 420 minutes as 7h 0m', () => {
      expect(component.formatMinutes(420)).toBe('7h 0m');
    });

    it('should format 90 minutes as 1h 30m', () => {
      expect(component.formatMinutes(90)).toBe('1h 30m');
    });

    it('should format 0 minutes as 0h 0m', () => {
      expect(component.formatMinutes(0)).toBe('0h 0m');
    });
  });

  describe('qualityEmoji', () => {
    beforeEach(() => {
      component.loadAnalysis();
      const req = httpMock.expectOne(r => r.url.includes('sleep-analysis'));
      req.flush(mockSleepAnalysis);
    });

    it('should return correct emoji for Good quality', () => {
      expect(component.qualityEmoji()).toBe('😊');
    });
  });

  describe('qualityBadgeClass', () => {
    it('should return emerald class for Excellent', () => {
      component.loadAnalysis();
      const req = httpMock.expectOne(r => r.url.includes('sleep-analysis'));
      req.flush({
        ...mockSleepAnalysis,
        summary: { ...mockSleepAnalysis.summary, qualityLabel: 'Excellent' },
      });

      expect(component.qualityBadgeClass()).toContain('emerald');
    });
  });

  // ─── Live BPM Methods ─────────────────────────────────────

  describe('liveBpmStatus', () => {
    it('should return "No data" when no BPM', () => {
      expect(component.liveBpmStatus()).toBe('No data');
    });

    it('should return "Normal" for normal BPM', () => {
      component.liveBpm.set(72);
      expect(component.liveBpmStatus()).toBe('Normal');
    });

    it('should return "Elevated!" for high BPM', () => {
      component.liveBpm.set(125);
      expect(component.liveBpmStatus()).toBe('Elevated!');
    });

    it('should return "Too Low!" for low BPM', () => {
      component.liveBpm.set(35);
      expect(component.liveBpmStatus()).toBe('Too Low!');
    });

    it('should return "High Normal" for 101-120 BPM', () => {
      component.liveBpm.set(110);
      expect(component.liveBpmStatus()).toBe('High Normal');
    });

    it('should return "Low Normal" for 40-59 BPM', () => {
      component.liveBpm.set(55);
      expect(component.liveBpmStatus()).toBe('Low Normal');
    });
  });

  describe('bpmToBarHeight', () => {
    it('should return minimum 10 for very low BPM', () => {
      expect(component.bpmToBarHeight(30)).toBe(10);
    });

    it('should return 100 for very high BPM', () => {
      expect(component.bpmToBarHeight(200)).toBe(100);
    });

    it('should return a scaled value for normal BPM', () => {
      const height = component.bpmToBarHeight(90);
      expect(height).toBeGreaterThan(10);
      expect(height).toBeLessThan(100);
    });
  });

  describe('bpmToBarColor', () => {
    it('should return red for elevated BPM', () => {
      expect(component.bpmToBarColor(130)).toBe('bg-red-400');
    });

    it('should return orange for low BPM', () => {
      expect(component.bpmToBarColor(35)).toBe('bg-orange-400');
    });

    it('should return green for normal BPM', () => {
      expect(component.bpmToBarColor(72)).toBe('bg-green-400');
    });
  });

  describe('liveBpmClass', () => {
    it('should return muted class when no BPM', () => {
      expect(component.liveBpmClass()).toContain('muted');
    });

    it('should return green class for normal BPM', () => {
      component.liveBpm.set(72);
      expect(component.liveBpmClass()).toContain('green');
    });

    it('should return red class for elevated BPM', () => {
      component.liveBpm.set(130);
      expect(component.liveBpmClass()).toContain('red');
    });

    it('should return orange class for low BPM', () => {
      component.liveBpm.set(35);
      expect(component.liveBpmClass()).toContain('orange');
    });
  });

  // ─── History Quality Helpers ───────────────────────────────

  describe('historyQualityEmoji', () => {
    it('should return star for Excellent', () => {
      expect(component.historyQualityEmoji('Excellent')).toBe('🌟');
    });

    it('should return smile for Good', () => {
      expect(component.historyQualityEmoji('Good')).toBe('😊');
    });

    it('should return neutral for Fair', () => {
      expect(component.historyQualityEmoji('Fair')).toBe('😐');
    });

    it('should return frown for Poor', () => {
      expect(component.historyQualityEmoji('Poor')).toBe('😟');
    });
  });

  describe('historyQualityBadgeClass', () => {
    it('should return emerald for Excellent', () => {
      expect(component.historyQualityBadgeClass('Excellent')).toContain('emerald');
    });

    it('should return blue for Good', () => {
      expect(component.historyQualityBadgeClass('Good')).toContain('blue');
    });

    it('should return amber for Fair', () => {
      expect(component.historyQualityBadgeClass('Fair')).toContain('amber');
    });

    it('should return red for Poor', () => {
      expect(component.historyQualityBadgeClass('Poor')).toContain('red');
    });
  });

  // ─── Live Tracking ────────────────────────────────────────

  describe('startLiveTracking / stopLiveTracking', () => {
    it('should set isLiveTracking to true when started', () => {
      component.startLiveTracking();
      expect(component.isLiveTracking()).toBeTrue();

      // Flush any dweet requests that may have been made
      httpMock.match(r => r.url.includes('dweet-proxy')).forEach(r => r.flush({ this: 'failed' }));

      component.stopLiveTracking();
    });

    it('should set isLiveTracking to false when stopped', () => {
      component.startLiveTracking();
      httpMock.match(r => r.url.includes('dweet-proxy')).forEach(r => r.flush({ this: 'failed' }));

      component.stopLiveTracking();
      expect(component.isLiveTracking()).toBeFalse();
    });

    it('should not start if already tracking', () => {
      component.startLiveTracking();
      expect(component.isLiveTracking()).toBeTrue();

      // Flush initial requests
      httpMock.match(r => r.url.includes('dweet-proxy')).forEach(r => r.flush({ this: 'failed' }));

      component.startLiveTracking(); // should be no-op — still true, no duplicate
      expect(component.isLiveTracking()).toBeTrue();
      component.stopLiveTracking();
    });
  });

  // ─── selectHistoryNight ────────────────────────────────────

  describe('selectHistoryNight', () => {
    it('should update selectedDate and trigger loadAnalysis', () => {
      spyOn(component, 'loadAnalysis');
      component.selectHistoryNight('2026-04-10');

      expect(component.selectedDate()).toBe('2026-04-10');
      expect(component.loadAnalysis).toHaveBeenCalled();
    });
  });

  // ─── onDateChange ──────────────────────────────────────────

  describe('onDateChange', () => {
    it('should update selectedDate from input event', () => {
      spyOn(component, 'loadAnalysis');
      const mockEvent = { target: { value: '2026-04-12' } } as unknown as Event;

      component.onDateChange(mockEvent);

      expect(component.selectedDate()).toBe('2026-04-12');
      expect(component.loadAnalysis).toHaveBeenCalled();
    });
  });
});
