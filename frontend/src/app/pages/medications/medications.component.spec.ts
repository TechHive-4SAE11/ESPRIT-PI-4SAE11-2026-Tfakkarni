import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MedicationManagementComponent } from './medications.component';
import { MedicationService } from '../../core/services/medication.service';
import { UserApiService } from '../../core/services/user-api.service';
import { of } from 'rxjs';
import { PagedResponse } from '../../core/models/paged-response.model';
import { MedicationResponseDTO } from '../../core/models/prescription.model';

describe('MedicationManagementComponent', () => {
  let component: MedicationManagementComponent;
  let fixture: ComponentFixture<MedicationManagementComponent>;
  let mockMedicationService: jasmine.SpyObj<MedicationService>;
  let mockUserApiService: jasmine.SpyObj<UserApiService>;

  beforeEach(async () => {
    mockMedicationService = jasmine.createSpyObj('MedicationService', ['getMedicationsByPatientPaginated', 'updateMedicationStatus']);
    mockUserApiService = jasmine.createSpyObj('UserApiService', ['getUserByKeycloakId']);

    mockMedicationService.getMedicationsByPatientPaginated.and.returnValue(of({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
      last: true,
      first: true
    } as PagedResponse<MedicationResponseDTO>));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MedicationManagementComponent],
      providers: [
        { provide: MedicationService, useValue: mockMedicationService },
        { provide: UserApiService, useValue: mockUserApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MedicationManagementComponent);
    component = fixture.componentInstance;
    
    // Set required input
    component.patient = { id: 1, keycloakId: 'patient-123', firstName: 'Test', lastName: 'Patient' } as any;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load medications on init', () => {
    expect(mockMedicationService.getMedicationsByPatientPaginated).toHaveBeenCalled();
  });

  it('should filter by status and reset to first page', () => {
    component.currentPage.set(2);
    component.filterByStatus('ACTIVE' as any);
    expect(component.selectedStatus()).toBe('ACTIVE' as any);
    expect(component.currentPage()).toBe(0);
    expect(mockMedicationService.getMedicationsByPatientPaginated).toHaveBeenCalled();
  });
});
