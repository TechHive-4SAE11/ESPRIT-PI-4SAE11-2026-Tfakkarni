import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PrescriptionManagementComponent } from './prescription-management.component';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { of } from 'rxjs';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { PrescriptionResponseDTO } from '../../../core/models/prescription.model';

describe('PrescriptionManagementComponent', () => {
  let component: PrescriptionManagementComponent;
  let fixture: ComponentFixture<PrescriptionManagementComponent>;
  let mockPrescriptionService: jasmine.SpyObj<PrescriptionService>;

  beforeEach(async () => {
    mockPrescriptionService = jasmine.createSpyObj('PrescriptionService', ['getPrescriptionsByPatientPaginated', 'createPrescription']);
    
    mockPrescriptionService.getPrescriptionsByPatientPaginated.and.returnValue(of({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
      last: true,
      first: true
    } as PagedResponse<PrescriptionResponseDTO>));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, PrescriptionManagementComponent],
      providers: [
        { provide: PrescriptionService, useValue: mockPrescriptionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PrescriptionManagementComponent);
    component = fixture.componentInstance;
    
    // Set required input
    component.patient = { id: 1, keycloakId: 'user-123', firstName: 'Test', lastName: 'Patient' } as any;
    
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load prescriptions on init', () => {
    expect(mockPrescriptionService.getPrescriptionsByPatientPaginated).toHaveBeenCalled();
  });
});
