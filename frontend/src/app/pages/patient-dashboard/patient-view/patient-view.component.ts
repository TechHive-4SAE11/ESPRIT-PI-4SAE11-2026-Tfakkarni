import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { PrescriptionService } from '@/core/services/prescription.service';
import { CarePlanService } from '@/core/services/care-plan.service';
import { UserApiService } from '@/core/services/user-api.service';
import { CustomGameService, type CustomGameResponse } from '@/core/services/custom-game.service';
import { type PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { type CarePlanResponseDTO } from '@/core/models/care-plan.model';
import { AuthService } from '@/core/auth';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';

@Component({
  selector: 'app-patient-view',
  standalone: true,
  imports: [CommonModule, PrescriptionListComponent, CarePlanListComponent],
  templateUrl: './patient-view.component.html',
})
export class PatientViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly carePlanService = inject(CarePlanService);
  private readonly userApiService = inject(UserApiService);
  private readonly customGameService = inject(CustomGameService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  @Input() keycloakId = '';
  @Output() switchToHelper = new EventEmitter<void>();

  // State Signals
  currentPage = signal<string>('Home');
  customGames = signal<CustomGameResponse[]>([]);
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  carePlans = signal<CarePlanResponseDTO[]>([]);

  // Loading State Signals
  isLoadingCustomGames = signal<boolean>(false);
  isLoadingPrescriptions = signal<boolean>(false);
  isLoadingCarePlans = signal<boolean>(false);

  // User Info
  userNeonDbId = signal<number | null>(null);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadData();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  logout(): void {
    this.authService.logout();
  }

  loadData(): void {
    if (!this.keycloakId) return;

    this.loadCustomGames();
    this.loadPrescriptions();
    this.loadCarePlans();
  }

  private loadPrescriptions(): void {
    if (!this.keycloakId) return;

    this.isLoadingPrescriptions.set(true);

    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
        }),
        switchMap(userInfo => {
          const neonDbId = userInfo.id.toString();
          return this.prescriptionService.getPrescriptionsByPatient(neonDbId);
        }),
        tap(prescriptions => {
          this.prescriptions.set(prescriptions);
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load prescriptions', err);
          return of([]);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadCarePlans(): void {
    if (!this.keycloakId) {
      return;
    }

    this.isLoadingCarePlans.set(true);

    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        switchMap(userInfo => {
          const neonDbId = userInfo.id.toString();
          return this.carePlanService.getCarePlansByPatient(neonDbId);
        }),
        tap(plans => {
          this.carePlans.set(plans);
        }),
        catchError(err => {
          console.error('[PatientView] Failed to load care plans', err);
          return of([]);
        }),
        finalize(() => this.isLoadingCarePlans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadCustomGames(): void {
    this.isLoadingCustomGames.set(true);
    this.customGameService.getGames(this.keycloakId)
      .pipe(
        tap(games => this.customGames.set(games)),
        catchError(err => {
          console.error('[PatientView] Failed to load custom games', err);
          return of([]);
        }),
        finalize(() => this.isLoadingCustomGames.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  playCustomGame(gameId: number): void {
    this.router.navigate(['/patient/play-memory', gameId]);
  }

  playRandomMix(): void {
    this.router.navigate(['/patient/play-memory', 'random']);
  }
}
