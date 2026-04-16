import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { PredictionService } from '../../services/prediction.service';
import { of } from 'rxjs';
import { DashboardStats } from '../../models/prediction.model';
import { NgApexchartsModule } from 'ng-apexcharts';

describe('AnalyticsDashboardComponent', () => {
  let component: AnalyticsDashboardComponent;
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;
  let mockPredictionService: jasmine.SpyObj<PredictionService>;

  beforeEach(async () => {
    mockPredictionService = jasmine.createSpyObj('PredictionService', ['getDashboardStats']);
    
    const mockStats: DashboardStats = {
      totalAppointments: 10,
      globalNoShowRate: 20,
      monthlyNoShowRate: 15,
      highRiskPatients: [],
      upcomingAppointments: [],
      cancellationsByDay: { 'Monday': 2 },
      noShowRateByDoctor: { 'Dr. Smith': 10 }
    };
    
    mockPredictionService.getDashboardStats.and.returnValue(of(mockStats));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, AnalyticsDashboardComponent, NgApexchartsModule],
      providers: [
        { provide: PredictionService, useValue: mockPredictionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard stats on init', () => {
    expect(mockPredictionService.getDashboardStats).toHaveBeenCalled();
    expect(component.stats?.totalAppointments).toBe(10);
  });
});
