import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CarePlanManagementComponent } from './care-plan-management.component';
import { CarePlanService } from '../../../core/services/care-plan.service';
import { of } from 'rxjs';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { CarePlanResponseDTO } from '../../../core/models/care-plan.model';

describe('CarePlanManagementComponent', () => {
  let component: CarePlanManagementComponent;
  let fixture: ComponentFixture<CarePlanManagementComponent>;
  let mockCarePlanService: jasmine.SpyObj<CarePlanService>;

  beforeEach(async () => {
    mockCarePlanService = jasmine.createSpyObj('CarePlanService', ['getCarePlansByPatientPaginated', 'createCarePlan']);
    
    mockCarePlanService.getCarePlansByPatientPaginated.and.returnValue(of({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
      last: true,
      first: true
    } as PagedResponse<CarePlanResponseDTO>));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, CarePlanManagementComponent],
      providers: [
        { provide: CarePlanService, useValue: mockCarePlanService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CarePlanManagementComponent);
    component = fixture.componentInstance;
    
    // Set required input
    component.patient = { id: 1, keycloakId: 'user-123', firstName: 'Test', lastName: 'Patient' } as any;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load care plans on init', () => {
    expect(mockCarePlanService.getCarePlansByPatientPaginated).toHaveBeenCalled();
  });
});
