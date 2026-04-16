import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CarePlanService } from './care-plan.service';
import { environment } from '../../../environments/environment';
import { CarePlanResponseDTO, CarePlanRequestDTO, CareActivityResponseDTO, CareActivityType } from '../models/care-plan.model';
import { PagedResponse } from '../models/paged-response.model';

describe('CarePlanService', () => {
  let service: CarePlanService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CarePlanService]
    });
    service = TestBed.inject(CarePlanService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch care plans by patient with pagination', () => {
    const mockResponse: PagedResponse<CarePlanResponseDTO> = {
      content: [
        {
          id: 1,
          sessionId: 101,
          doctorId: 'doc-1',
          activities: [{ 
            id: 1, 
            activityName: 'Walking', 
            activityType: CareActivityType.PHYSICAL_ACTIVITY, 
            description: '30 mins walking', 
            frequency: 'Daily', 
            duration: '1 week', 
            completionStatus: 'PENDING', 
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

    service.getCarePlansByPatientPaginated('patient-123', 0, 5).subscribe(data => {
      expect(data.content.length).toBe(1);
      expect(data.page).toBe(0);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/care-plans/patient/patient-123') &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should update activity status', () => {
    const status = 'COMPLETED';
    const mockResponse: CareActivityResponseDTO = {
      id: 1,
      activityName: 'Walking',
      activityType: CareActivityType.PHYSICAL_ACTIVITY,
      description: '30 mins walking',
      frequency: 'Daily',
      duration: '1 week',
      completionStatus: status,
      createdAt: new Date().toISOString()
    };

    service.updateActivityStatus(1, status).subscribe(data => {
      expect(data.completionStatus).toBe(status);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/care-plans/activities/1/status`);
    expect(req.request.method).toBe('PATCH');
    req.flush(mockResponse);
  });
});
