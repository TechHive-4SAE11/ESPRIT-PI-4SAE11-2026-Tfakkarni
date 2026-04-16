import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';
import { environment } from '../../../environments/environment';
import { PrescriptionImpactResponse, CorrelationStatsResponse, PatientScoreResponse } from '../models/analytics.model';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalyticsService]
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch patient current score', () => {
    const mockResponse: PatientScoreResponse = {
      patientKeycloakId: 'patient-123',
      cognitiveScore: 80,
      dailyFunctioningScore: 70,
      medicalStabilityScore: 90,
      iotRiskScore: 10,
      engagementScore: 85,
      overallScore: 75,
      stage: 'EARLY',
      scoreTrend: 'STABLE',
      computedAt: new Date().toISOString(),
      cognitiveDomains: []
    };

    service.getPatientScore('patient-123').subscribe(data => {
      expect(data.overallScore).toBe(75);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/analytics/patient/patient-123/score')
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should fetch prescription impact', () => {
    const mockResponse: PrescriptionImpactResponse = {
      patientKeycloakId: 'patient-123',
      impactTimeline: [
        { date: '2024-01-01', avgScore: 75, medAdherence: 90, hasNewPrescription: false }
      ],
      markers: []
    };

    service.getPrescriptionImpact('patient-123', 60).subscribe(data => {
      expect(data.impactTimeline.length).toBe(1);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/analytics/patient/patient-123/prescription-impact') &&
      request.params.get('days') === '60'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should fetch correlation stats', () => {
    const mockResponse: CorrelationStatsResponse = {
      patientKeycloakId: 'patient-123',
      correlationTimeline: [],
      keyInsight: 'Positive',
      adherenceCorrelation: 0.85
    };

    service.getCorrelationStats('patient-123', 30).subscribe(data => {
      expect(data.adherenceCorrelation).toBe(0.85);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/analytics/patient/patient-123/correlation') &&
      request.params.get('days') === '30'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });
});
