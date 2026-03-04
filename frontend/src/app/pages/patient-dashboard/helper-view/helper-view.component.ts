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
import { NotificationService, type MedicationNotification, type NotificationResponse } from '@/core/services/notification.service';
import { Router } from '@angular/router';

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
  private readonly router = inject(Router);

  @Input() keycloakId = '';
  @Output() pageChange = new EventEmitter<string>();

  // State Signals
  currentPage = signal<string>('Home');
  userNeonDbId = signal<number | null>(null);
  currentUser = signal<UserInfo | null>(null);

  // ── Notifications ──────────────────────────────────────────────────────────
  private readonly notificationService = inject(NotificationService);
  notifications = signal<MedicationNotification[]>([]);
  unreadNotifCount = signal(0);
  isNotifPanelOpen = signal(false);
  isLoadingNotifs = signal(false);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserNeonDbId();
    }
  }

  setPage(page: string): void {
    this.currentPage.set(page);
    this.pageChange.emit(page);
  }


  goToAppointments(): void {
    this.router.navigate(['/appointments']);
  }

  private loadUserNeonDbId(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          console.log('[HelperView] User info retrieved. DB ID:', userInfo.id);
          this.userNeonDbId.set(userInfo.id);
          this.currentUser.set(userInfo);
          this.loadNotifications();
        }),
        catchError(err => {
          console.error('[HelperView] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ── Notification Methods ───────────────────────────────────────────────────

  loadNotifications(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.isLoadingNotifs.set(true);
    this.notificationService.getNotifications(neonId.toString())
      .pipe(
        tap((res: NotificationResponse) => {
          this.notifications.set(res.notifications || []);
          this.unreadNotifCount.set(res.unreadCount);
        }),
        catchError(() => {
          console.warn('[HelperView] Failed to load notifications');
          return of(null);
        }),
        finalize(() => this.isLoadingNotifs.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  toggleNotifPanel(): void {
    const isOpen = !this.isNotifPanelOpen();
    this.isNotifPanelOpen.set(isOpen);
    if (isOpen && this.notifications().length === 0) {
      this.loadNotifications();
    }
  }

  closeNotifPanel(): void {
    this.isNotifPanelOpen.set(false);
  }

  markNotifAsRead(notif: MedicationNotification): void {
    const neonId = this.userNeonDbId();
    if (notif.read || !neonId) return;
    this.notificationService.markAsRead(neonId.toString(), notif.id)
      .pipe(
        tap(() => {
          this.notifications.update(list =>
            list.map(n => n.id === notif.id ? { ...n, read: true } : n)
          );
          this.unreadNotifCount.update(c => Math.max(0, c - 1));
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  markAllNotifsAsRead(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.notificationService.markAllAsRead(neonId.toString())
      .pipe(
        tap(() => {
          this.notifications.update(list =>
            list.map(n => ({ ...n, read: true }))
          );
          this.unreadNotifCount.set(0);
        }),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  refreshNotifications(): void {
    const neonId = this.userNeonDbId();
    if (!neonId) return;
    this.isLoadingNotifs.set(true);
    this.notificationService.refreshNotifications(neonId.toString())
      .pipe(
        tap((res: NotificationResponse) => {
          this.notifications.set(res.notifications || []);
          this.unreadNotifCount.set(res.unreadCount);
        }),
        catchError(() => of(null)),
        finalize(() => this.isLoadingNotifs.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }
}
