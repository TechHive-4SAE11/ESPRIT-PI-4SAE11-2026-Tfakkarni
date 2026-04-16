import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MedicationService, UpdateMedicationStatusRequest, UpdateMedicationStatusResponse } from './medication.service';
import { environment } from '../../../environments/environment';
import { MedicationStatus, MedicationResponseDTO } from '../models/prescription.model';
import { PagedResponse } from '../models/paged-response.model';

describe('MedicationService', () => {
  let service: MedicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MedicationService]
    });
    service = TestBed.inject(MedicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch medications by patient with pagination', () => {
    const mockResponse: PagedResponse<MedicationResponseDTO> = {
      content: [
        { 
          id: 1, 
          medicationName: 'Med A', 
          dosage: '10mg', 
          frequency: 'Daily', 
          duration: '1 week', 
          instructions: 'Take with water', 
          status: MedicationStatus.ACTIVE, 
          startDate: null, 
          endDate: null, 
          createdAt: new Date().toISOString() 
        }
      ],
      page: 0,
      size: 5,
      totalElements: 1,      totalPages: 1,
      first: true,
      last: true
    };

    service.getMedicationsByPatientPaginated('patient-123', 0, 5).subscribe(data => {
      expect(data.content.length).toBe(1);
      expect(data.page).toBe(0);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/medications/patient/patient-123') &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should update medication status', () => {
    const mockRequest: UpdateMedicationStatusRequest = {
      status: MedicationStatus.DISCONTINUED,
      reason: 'Side effects'
    };

    const mockResponse: UpdateMedicationStatusResponse = {
      success: true,
      medicationId: 1,
      oldStatus: MedicationStatus.ACTIVE,
      newStatus: MedicationStatus.DISCONTINUED,
      endDate: new Date().toISOString(),
      message: 'Status updated'
    };

    service.updateMedicationStatus(1, mockRequest).subscribe(data => {
      expect(data.newStatus).toBe(MedicationStatus.DISCONTINUED);
      expect(data.success).toBeTrue();
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/medications/1/status`);
    expect(req.request.method).toBe('PATCH');
    req.flush(mockResponse);
  });
});
