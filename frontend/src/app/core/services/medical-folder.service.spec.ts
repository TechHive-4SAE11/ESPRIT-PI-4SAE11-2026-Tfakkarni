import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MedicalFolderService, MedicalFolder, MedicalFolderPage, CreateMedicalFolderRequest } from './medical-folder.service';
import { environment } from '@/environments/environment';

describe('MedicalFolderService', () => {
  let service: MedicalFolderService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiBaseUrl}/api/medical-folders`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MedicalFolderService]
    });
    service = TestBed.inject(MedicalFolderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that there are no outstanding requests after each test
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should retrieve a single medical folder by ID (GET)', () => {
    const dummyFolder: MedicalFolder = {
      id: 1,
      patientId: 'patient-123',
      doctorId: 'doctor-456',
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    };

    service.getById(1).subscribe(folder => {
      expect(folder).toEqual(dummyFolder);
      expect(folder.patientId).toBe('patient-123');
    });

    // Expect a GET request to the correct URL
    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');

    // Flush the mocked data back to the service
    req.flush(dummyFolder);
  });

  it('should retrieve a paginated list of medical folders (GET)', () => {
    const dummyPage: MedicalFolderPage = {
      content: [
        {
          id: 1,
          patientId: 'patient-123',
          doctorId: 'doctor-456',
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 1
    };

    service.getPage({ page: 0, size: 5, sort: 'id,asc' }).subscribe(page => {
      expect(page.content.length).toBe(1);
      expect(page.totalElements).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}?page=0&size=5&sort=id,asc`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyPage);
  });

  it('should create a new medical folder (POST)', () => {
    const newFolderRequest: CreateMedicalFolderRequest = {
      patientId: 'new-patient',
      doctorId: 'new-doctor'
    };

    const expectedResponse: MedicalFolder = {
      id: 2,
      patientId: 'new-patient',
      doctorId: 'new-doctor',
      createdAt: '2025-02-01T00:00:00Z',
      updatedAt: '2025-02-01T00:00:00Z',
    };

    service.create(newFolderRequest).subscribe(folder => {
      expect(folder.id).toBe(2);
      expect(folder.patientId).toBe('new-patient');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    // Expect the payload attached to the request to match what we sent
    expect(req.request.body).toEqual(newFolderRequest);

    req.flush(expectedResponse);
  });

  it('should restrict a patient booking (POST with reason)', () => {
    const expectedResponse = { id: 1, patientId: 'patient', doctorId: 'doc', createdAt: '', updatedAt: '', bookingRestricted: true };

    service.restrictBooking(1, 'Patient was rude').subscribe(folder => {
      expect(folder.bookingRestricted).toBeTrue();
    });

    const req = httpMock.expectOne(`${baseUrl}/1/restrict-booking?reason=Patient%20was%20rude`);
    expect(req.request.method).toBe('POST');
    req.flush(expectedResponse);
  });
});
