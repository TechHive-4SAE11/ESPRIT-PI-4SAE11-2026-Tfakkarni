import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PrescriptionService } from './prescription.service';
import { environment } from '../../../environments/environment';
import { PrescriptionResponseDTO, PrescriptionRequestDTO, MedicationStatus } from '../models/prescription.model';
import { PagedResponse } from '../models/paged-response.model';

describe('PrescriptionService', () => {
  let service: PrescriptionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PrescriptionService]
    });
    service = TestBed.inject(PrescriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch prescriptions by patient with pagination', () => {
    const mockResponse: PagedResponse<PrescriptionResponseDTO> = {
      content: [
        {
          id: 1,
          sessionId: 101,
          doctorId: 'doc-1',
          medications: [{ 
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
          }],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    };

    service.getPrescriptionsByPatientPaginated('patient-123', 0, 5).subscribe(data => {
      expect(data.content.length).toBe(1);
      expect(data.page).toBe(0);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/prescriptions/patient/patient-123') &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should create a prescription', () => {
    const mockRequest: PrescriptionRequestDTO = {
      sessionId: 101,
      medications: [{ 
        medicationName: 'New Med', 
        dosage: '5mg', 
        frequency: 'Twice daily', 
        duration: '1 month', 
        instructions: 'After meal' 
      }]
    };

    const mockResponse: PrescriptionResponseDTO = {
      id: 2,
      sessionId: 101,
      doctorId: 'doc-1',
      medications: [{ 
        id: 2, 
        medicationName: 'New Med', 
        dosage: '5mg', 
        frequency: 'Twice daily', 
        duration: '1 month', 
        instructions: 'After meal', 
        status: MedicationStatus.ACTIVE, 
        startDate: null, 
        endDate: null, 
        createdAt: new Date().toISOString() 
      }],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    service.createPrescription(mockRequest).subscribe(data => {
      expect(data.id).toBe(2);
      expect(data.medications[0].medicationName).toBe('New Med');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/prescriptions`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
