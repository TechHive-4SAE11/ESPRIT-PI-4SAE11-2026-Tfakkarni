import {
  Component,
  OnInit,
  signal,
  Input,
  Output,
  EventEmitter,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { PrescriptionListComponent } from '@/shared/components/prescription-list/prescription-list.component';
import { DataLibraryComponent } from './data-library/data-library.component';
import { TagManagerComponent } from './tag-manager/tag-manager.component';
import { GameBuilderComponent } from './game-builder/game-builder.component';

import { PrescriptionService } from '@/core/services/prescription.service';
import { PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-helper-view',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    PrescriptionListComponent,
    DataLibraryComponent,
    TagManagerComponent,
    GameBuilderComponent,
  ],
  templateUrl: './helper-view.component.html',
})
export class HelperViewComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly keycloakService = inject(KeycloakService);
  private readonly prescriptionService = inject(PrescriptionService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // State Signals
  currentPage = signal<string>('Home');
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  isLoadingPrescriptions = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadPrescriptions();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    this.pageChange.emit(page);
  }

  private loadPrescriptions(): void {
    if (!this.keycloakId) {
      console.warn('[HelperView] No keycloakId provided, skipping prescription load');
      return;
    }

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
          console.error('[HelperView] Failed to load prescriptions', err);
          return of([]);
        }),
        finalize(() => this.isLoadingPrescriptions.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
