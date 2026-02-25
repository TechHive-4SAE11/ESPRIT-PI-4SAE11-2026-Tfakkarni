import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { CarePlanListComponent } from '@/shared/components/care-plan-list/care-plan-list.component';
import { MedicationManagementComponent } from '@/pages/medications/medications.component';
import { DataLibraryComponent } from './data-library/data-library.component';
import { TagManagerComponent } from './tag-manager/tag-manager.component';
import { GameBuilderComponent } from './game-builder/game-builder.component';
import { ProfileComponent } from './profile/profile.component';

import { PrescriptionService } from '@/core/services/prescription.service';
import { PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { CarePlanService } from '@/core/services/care-plan.service';
import { CarePlanResponseDTO } from '@/core/models/care-plan.model';
import { SuiviQuotidienComponent } from './suivi-quotidien/suivi-quotidien.component';
import { StatisticsDashboardComponent } from './statistics-dashboard/statistics-dashboard.component';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';

@Component({
  selector: 'app-helper-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './helper-view.component.html',
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    MedicationManagementComponent,
    SuiviQuotidienComponent,
    PrescriptionListComponent,
    StatisticsDashboardComponent,
    CarePlanListComponent,
    DataLibraryComponent,
    TagManagerComponent,
    GameBuilderComponent,
    ProfileComponent,
  ],
})
export class HelperViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly keycloakService = inject(KeycloakService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // State Signals
  currentPage = signal<string>('Home');
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserNeonDbId();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    this.pageChange.emit(page);
  }

  private loadUserNeonDbId(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[HelperView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
          this.currentUser.set(userInfo);
        }),
        catchError(err => {
          console.error('[HelperView] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }}
