import { Component, ElementRef, inject, input, OnDestroy, PLATFORM_ID, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { distinctUntilChanged, filter } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { DiagnosticsService, type Diagnostics } from '@/core/services/diagnostics.service';
import {
  CoachingService,
  type CoachingGoal,
  type CoachingGoalRequestBody,
  type CoachingGoalStatus,
  type CoachingGoalType,
  type CoachingMood,
  type CoachingNotification,
  type CoachingProgress,
  type ProgressRecordedByRole,
} from '@/core/services/coaching.service';

export type CoachingPanelMode = 'doctor' | 'helper' | 'patient';

@Component({
  selector: 'app-coaching-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  templateUrl: './coaching-panel.component.html',
})
export class CoachingPanelComponent implements OnDestroy {
  private readonly coachingService = inject(CoachingService);
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly platformId = inject(PLATFORM_ID);

  @ViewChild('goalMapContainer') goalMapContainer?: ElementRef<HTMLDivElement>;

  folderId = input.required<number>();
  /** doctor: full CRUD; helper/patient: log progress only */
  mode = input<CoachingPanelMode>('doctor');
  notificationUserIdOverride = input<string>('');

  goals = signal<CoachingGoal[]>([]);
  diagnostics = signal<Diagnostics[]>([]);
  loading = signal(false);
  expandedGoalId = signal<number | null>(null);
  progressByGoal = signal<Record<number, CoachingProgress[]>>({});
  loadingProgressFor = signal<number | null>(null);
  notifications = signal<CoachingNotification[]>([]);
  notifOpen = signal(false);
  notifLoading = signal(false);
  notifUnreadCount = signal(0);
  demoMode = signal(false);
  switchingMode = signal(false);

  showGoalForm = signal(false);
  editingGoal = signal<CoachingGoal | null>(null);

  goalTypes: CoachingGoalType[] = [
    'COGNITIVE_IMPROVEMENT',
    'ACTIVITY_INCREASE',
    'MEDICATION_ADHERENCE',
    'SOCIAL_ENGAGEMENT',
    'NUTRITION',
    'OTHER',
  ];

  goalForm: CoachingGoalRequestBody = this.emptyGoalForm();

  progressDate = '';
  progressPct: number | null = null;
  progressMood = '' as CoachingMood | '';
  progressEnergy: number | null = null;
  progressNotes = '';
  progressFeedback = '';
  private map: any = null;
  private marker: any = null;
  private L: any = null;
  private expandedMaps = new Map<number, any>();
  private expandedTargetMarkers = new Map<number, any>();
  private expandedCurrentMarkers = new Map<number, any>();
  private expandedRouteLines = new Map<number, any>();
  private notifPollTimer: ReturnType<typeof setInterval> | null = null;
  private reminderBellLoopTimer: ReturnType<typeof setInterval> | null = null;
  private reminderAudioCtx: AudioContext | null = null;
  private seenNotifIds = new Set<number>();
  private geoLoadingByGoal = signal<Record<number, boolean>>({});
  private currentPositionByGoal = signal<Record<number, { lat: number; lng: number }>>({});
  private weatherByGoal = signal<Record<number, {
    temperature: number;
    weatherCode: number;
    humidity?: number;
    wind?: number;
    feelsLike?: number;
    fetchedAtIso: string;
  }>>({});

  constructor() {
    // toObservable fires when folderId is bound (more reliable than effect() for input signals on first paint)
    toObservable(this.folderId)
      .pipe(
        filter((id): id is number => id != null && id > 0),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.loadGoals();
        this.loadDiagnostics();
        this.loadNotifications(true);
        this.loadSchedulerMode();
      });
    this.startNotifPolling();
  }

  ngOnDestroy(): void {
    this.destroyGoalMap();
    this.destroyExpandedMaps();
    this.stopNotifPolling();
    this.stopReminderBellLoop();
  }

  isDoctor(): boolean {
    return this.mode() === 'doctor';
  }

  defaultProgressRole(): ProgressRecordedByRole {
    return this.mode() === 'patient' ? 'PATIENT' : 'HELPER';
  }

  private emptyGoalForm(): CoachingGoalRequestBody {
    return {
      goalType: 'OTHER',
      goalTitle: '',
      actionSteps: '',
      tips: '',
      targetDays: undefined,
      priority: 'MEDIUM',
      outdoorActivity: false,
      latitude: undefined,
      longitude: undefined,
      diagnosticId: undefined,
    };
  }

  loadGoals(): void {
    const fid = this.folderId();
    this.loading.set(true);
    this.coachingService.listGoals(fid).subscribe({
      next: (list) => {
        this.goals.set(list);
        this.loadOutdoorWeatherForGoals(list);
        this.loading.set(false);
      },
      error: () => {
        this.goals.set([]);
        this.loading.set(false);
      },
    });
  }

  loadDiagnostics(): void {
    this.diagnosticsService.getByFolder(this.folderId()).subscribe({
      next: (list) => this.diagnostics.set(list),
      error: () => this.diagnostics.set([]),
    });
  }

  openCreateGoal(): void {
    this.editingGoal.set(null);
    this.goalForm = this.emptyGoalForm();
    this.showGoalForm.set(true);
    this.scheduleEnsureMapReady();
  }

  openEditGoal(g: CoachingGoal): void {
    this.editingGoal.set(g);
    this.goalForm = {
      diagnosticId: g.diagnosticId ?? undefined,
      goalType: g.goalType,
      goalTitle: g.goalTitle,
      actionSteps: g.actionSteps ?? '',
      tips: g.tips ?? '',
      targetDays: g.targetDays ?? undefined,
      priority: g.priority,
      outdoorActivity: g.outdoorActivity,
      latitude: g.latitude ?? undefined,
      longitude: g.longitude ?? undefined,
    };
    this.showGoalForm.set(true);
    this.scheduleEnsureMapReady();
  }

  cancelGoalForm(): void {
    this.showGoalForm.set(false);
    this.editingGoal.set(null);
    this.destroyGoalMap();
  }

  onOutdoorToggle(enabled: boolean): void {
    this.goalForm.outdoorActivity = enabled;
    if (enabled) {
      this.scheduleEnsureMapReady();
      return;
    }
    this.goalForm.latitude = undefined;
    this.goalForm.longitude = undefined;
    this.destroyGoalMap();
  }

  clearChosenLocation(): void {
    this.goalForm.latitude = undefined;
    this.goalForm.longitude = undefined;
    if (this.marker && this.map) {
      this.map.removeLayer(this.marker);
      this.marker = null;
    }
  }

  private scheduleEnsureMapReady(): void {
    if (!this.showGoalForm() || !this.goalForm.outdoorActivity) return;
    if (!isPlatformBrowser(this.platformId)) return;
    // Wait for template to render map container.
    setTimeout(() => {
      this.ensureMapReady();
    }, 0);
  }

  private async ensureMapReady(): Promise<void> {
    if (!this.showGoalForm() || !this.goalForm.outdoorActivity) return;
    if (!isPlatformBrowser(this.platformId)) return;
    if (!this.goalMapContainer?.nativeElement) return;

    if (!this.map) {
      await this.initGoalMap();
    }
    // marker restore while editing existing goal
    if (this.goalForm.latitude != null && this.goalForm.longitude != null) {
      this.setMapMarker(this.goalForm.latitude, this.goalForm.longitude, true);
    }
    // Fix rendering if container was hidden then shown
    this.map?.invalidateSize?.();
  }

  private async initGoalMap(): Promise<void> {
    this.L = await import('leaflet');
    const iconDefault = this.L.icon({
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
    });
    this.L.Marker.prototype.options.icon = iconDefault;

    const initialLat = this.goalForm.latitude ?? 36.8065;
    const initialLng = this.goalForm.longitude ?? 10.1815;
    this.map = this.L.map(this.goalMapContainer!.nativeElement).setView([initialLat, initialLng], 12);
    this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      const { lat, lng } = e.latlng;
      this.goalForm.latitude = lat;
      this.goalForm.longitude = lng;
      this.setMapMarker(lat, lng, false);
    });
  }

  private setMapMarker(lat: number, lng: number, recenter: boolean): void {
    if (!this.map || !this.L) return;
    const latlng = this.L.latLng(lat, lng);
    if (this.marker) {
      this.marker.setLatLng(latlng);
    } else {
      this.marker = this.L.marker(latlng).addTo(this.map);
    }
    if (recenter) {
      this.map.setView(latlng, 13);
    }
  }

  private destroyGoalMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.marker = null;
    this.L = null;
  }

  submitGoalForm(): void {
    const title = (this.goalForm.goalTitle || '').trim();
    if (!title) {
      this.alertDialog.warning({ zTitle: 'Validation', zContent: 'Title is required.' });
      return;
    }
    const fid = this.folderId();
    const body: CoachingGoalRequestBody = {
      ...this.goalForm,
      goalTitle: title,
      diagnosticId: this.goalForm.diagnosticId ?? undefined,
    };
    const edit = this.editingGoal();
    if (edit) {
      this.coachingService.updateGoal(fid, edit.id, body).subscribe({
        next: () => {
          this.cancelGoalForm();
          this.loadGoals();
          this.loadNotifications(true);
          this.alertDialog.info({ zTitle: 'OK', zContent: 'Goal updated.' });
        },
        error: (err) =>
          this.alertDialog.warning({
            zTitle: 'Error',
            zContent: err?.error?.message || 'Update failed.',
          }),
      });
    } else {
      this.coachingService.createGoal(fid, body).subscribe({
        next: () => {
          this.cancelGoalForm();
          this.loadGoals();
          this.loadNotifications(true);
          this.alertDialog.info({ zTitle: 'OK', zContent: 'Goal created.' });
        },
        error: (err) =>
          this.alertDialog.warning({
            zTitle: 'Error',
            zContent: err?.error?.message || 'Create failed.',
          }),
      });
    }
  }

  setGoalStatus(g: CoachingGoal, status: CoachingGoalStatus): void {
    this.coachingService.patchGoalStatus(this.folderId(), g.id, status).subscribe({
      next: () => {
        this.loadGoals();
        this.loadNotifications(true);
      },
      error: (err) =>
        this.alertDialog.warning({
          zTitle: 'Error',
          zContent: err?.error?.message || 'Status update failed.',
        }),
    });
  }

  deleteGoal(g: CoachingGoal): void {
    this.alertDialog
      .confirm({
        zTitle: 'Delete goal',
        zContent: `Delete « ${g.goalTitle} » ?`,
        zOkDestructive: true,
        zOkText: 'Delete',
      })
      .afterClosed$.subscribe((ok: boolean) => {
        if (!ok) return;
        this.coachingService.deleteGoal(this.folderId(), g.id).subscribe({
          next: () => {
            this.loadGoals();
            this.loadNotifications(true);
            this.alertDialog.info({ zTitle: 'Deleted', zContent: 'Goal removed.' });
          },
          error: (err) =>
            this.alertDialog.warning({
              zTitle: 'Error',
              zContent: err?.error?.message || 'Delete failed.',
            }),
        });
      });
  }

  toggleProgress(goalId: number): void {
    const cur = this.expandedGoalId();
    if (cur === goalId) {
      this.expandedGoalId.set(null);
      this.destroyExpandedMap(goalId);
      return;
    }
    this.expandedGoalId.set(goalId);
    this.loadProgress(goalId);
    // wait for template render
    setTimeout(() => this.ensureExpandedGoalMap(goalId), 0);
  }

  loadProgress(goalId: number): void {
    this.loadingProgressFor.set(goalId);
    this.coachingService.listProgress(this.folderId(), goalId).subscribe({
      next: (list) => {
        this.progressByGoal.update((m) => ({ ...m, [goalId]: list }));
        this.loadingProgressFor.set(null);
      },
      error: () => {
        this.progressByGoal.update((m) => ({ ...m, [goalId]: [] }));
        this.loadingProgressFor.set(null);
      },
    });
  }

  submitProgress(goalId: number): void {
    const role = this.defaultProgressRole();
    const body = {
      dateRecorded: this.progressDate || null,
      completionPercentage: this.progressPct ?? undefined,
      mood: this.progressMood || undefined,
      energyLevel: this.progressEnergy ?? undefined,
      helperNotes: this.progressNotes || undefined,
      patientFeedback: this.progressFeedback || undefined,
      recordedByRole: role,
    };
    this.coachingService.addProgress(this.folderId(), goalId, body).subscribe({
      next: () => {
        this.progressDate = '';
        this.progressPct = null;
        this.progressMood = '';
        this.progressEnergy = null;
        this.progressNotes = '';
        this.progressFeedback = '';
        this.loadProgress(goalId);
        this.loadNotifications(true);
        this.alertDialog.info({ zTitle: 'OK', zContent: 'Progress saved.' });
      },
      error: (err) =>
        this.alertDialog.warning({
          zTitle: 'Error',
          zContent: err?.error?.message || 'Could not save progress.',
        }),
    });
  }

  isGeoLoading(goalId: number): boolean {
    return !!this.geoLoadingByGoal()[goalId];
  }

  hasGoalCoordinates(g: CoachingGoal): boolean {
    return g.outdoorActivity && g.latitude != null && g.longitude != null;
  }

  hasCurrentPosition(goalId: number): boolean {
    return !!this.currentPositionByGoal()[goalId];
  }

  latestWeatherForGoal(goalId: number): string | null {
    const list = this.progressByGoal()[goalId] || [];
    const row = list.find((p) => !!p.weatherSummary);
    return row?.weatherSummary ?? null;
  }

  weatherCardForGoal(goalId: number): {
    temperature: number;
    weatherCode: number;
    humidity?: number;
    wind?: number;
    feelsLike?: number;
    fetchedAtIso: string;
  } | null {
    return this.weatherByGoal()[goalId] ?? null;
  }

  weatherLabel(code: number): string {
    if (code === 0) return 'Clear Sky';
    if ([1, 2].includes(code)) return 'Mainly Clear';
    if (code === 3) return 'Cloudy';
    if ([45, 48].includes(code)) return 'Foggy';
    if ([51, 53, 55, 56, 57].includes(code)) return 'Drizzle';
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return 'Rain';
    if ([71, 73, 75, 77, 85, 86].includes(code)) return 'Snow';
    if ([95, 96, 99].includes(code)) return 'Thunderstorm';
    return 'Weather';
  }

  weatherIcon(code: number): string {
    if (code === 0) return '☀️';
    if ([1, 2].includes(code)) return '🌤️';
    if (code === 3) return '☁️';
    if ([45, 48].includes(code)) return '🌫️';
    if ([51, 53, 55, 56, 57].includes(code)) return '🌦️';
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return '🌧️';
    if ([71, 73, 75, 77, 85, 86].includes(code)) return '❄️';
    if ([95, 96, 99].includes(code)) return '⛈️';
    return '🌈';
  }

  private async loadOutdoorWeatherForGoals(goals: CoachingGoal[]): Promise<void> {
    const targets = goals.filter((g) => g.outdoorActivity && g.latitude != null && g.longitude != null);
    if (!targets.length) return;
    for (const g of targets) {
      try {
        const url =
          `https://api.open-meteo.com/v1/forecast?latitude=${g.latitude}&longitude=${g.longitude}` +
          `&current=temperature_2m,weather_code,relative_humidity_2m,apparent_temperature,wind_speed_10m&timezone=auto`;
        const res = await fetch(url);
        if (!res.ok) continue;
        const json = await res.json();
        const c = json?.current;
        if (!c) continue;
        this.weatherByGoal.update((m) => ({
          ...m,
          [g.id]: {
            temperature: Number(c.temperature_2m),
            weatherCode: Number(c.weather_code),
            humidity: c.relative_humidity_2m != null ? Number(c.relative_humidity_2m) : undefined,
            wind: c.wind_speed_10m != null ? Number(c.wind_speed_10m) : undefined,
            feelsLike: c.apparent_temperature != null ? Number(c.apparent_temperature) : undefined,
            fetchedAtIso: new Date().toISOString(),
          },
        }));
      } catch {
        // non-blocking UI enhancement
      }
    }
  }

  locateMeAndDrawRoute(g: CoachingGoal): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (!this.hasGoalCoordinates(g)) return;
    if (!navigator.geolocation) {
      this.alertDialog.warning({ zTitle: 'Location', zContent: 'Geolocation is not supported by this browser.' });
      return;
    }
    this.geoLoadingByGoal.update((m) => ({ ...m, [g.id]: true }));
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        this.currentPositionByGoal.update((m) => ({ ...m, [g.id]: { lat, lng } }));
        this.geoLoadingByGoal.update((m) => ({ ...m, [g.id]: false }));
        this.ensureExpandedGoalMap(g.id);
        this.drawRouteForGoal(g.id, g.latitude!, g.longitude!, lat, lng);
      },
      () => {
        this.geoLoadingByGoal.update((m) => ({ ...m, [g.id]: false }));
        this.alertDialog.warning({ zTitle: 'Location', zContent: 'Unable to read your current location.' });
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  private async ensureExpandedGoalMap(goalId: number): Promise<void> {
    const goal = this.goals().find((x) => x.id === goalId);
    if (!goal || !this.hasGoalCoordinates(goal)) return;
    if (!isPlatformBrowser(this.platformId)) return;
    const mapEl = document.getElementById(`coaching-goal-map-${goalId}`);
    if (!mapEl) return;

    if (!this.L) {
      this.L = await import('leaflet');
      const iconDefault = this.L.icon({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      });
      this.L.Marker.prototype.options.icon = iconDefault;
    }

    let map = this.expandedMaps.get(goalId);
    if (!map) {
      map = this.L.map(mapEl).setView([goal.latitude!, goal.longitude!], 12);
      this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19,
      }).addTo(map);
      this.expandedMaps.set(goalId, map);
    } else {
      map.invalidateSize();
    }

    let targetMarker = this.expandedTargetMarkers.get(goalId);
    const target = this.L.latLng(goal.latitude!, goal.longitude!);
    if (!targetMarker) {
      targetMarker = this.L.marker(target).addTo(map).bindPopup('Goal location');
      this.expandedTargetMarkers.set(goalId, targetMarker);
    } else {
      targetMarker.setLatLng(target);
    }

    const current = this.currentPositionByGoal()[goalId];
    if (current) {
      this.drawRouteForGoal(goalId, goal.latitude!, goal.longitude!, current.lat, current.lng);
    } else {
      map.setView(target, 12);
    }
  }

  private drawRouteForGoal(goalId: number, targetLat: number, targetLng: number, currentLat: number, currentLng: number): void {
    const map = this.expandedMaps.get(goalId);
    if (!map || !this.L) return;
    const target = this.L.latLng(targetLat, targetLng);
    const current = this.L.latLng(currentLat, currentLng);

    let currentMarker = this.expandedCurrentMarkers.get(goalId);
    if (!currentMarker) {
      currentMarker = this.L.circleMarker(current, {
        radius: 7,
        color: '#2563eb',
        fillColor: '#3b82f6',
        fillOpacity: 0.9,
      }).addTo(map).bindPopup('Your current location');
      this.expandedCurrentMarkers.set(goalId, currentMarker);
    } else {
      currentMarker.setLatLng(current);
    }

    const oldLine = this.expandedRouteLines.get(goalId);
    if (oldLine) {
      map.removeLayer(oldLine);
    }
    const line = this.L.polyline([current, target], {
      color: '#0f766e',
      weight: 4,
      opacity: 0.8,
      dashArray: '8 6',
    }).addTo(map);
    this.expandedRouteLines.set(goalId, line);
    map.fitBounds(line.getBounds(), { padding: [24, 24] });
  }

  private destroyExpandedMap(goalId: number): void {
    const map = this.expandedMaps.get(goalId);
    if (map) {
      map.remove();
      this.expandedMaps.delete(goalId);
    }
    this.expandedTargetMarkers.delete(goalId);
    this.expandedCurrentMarkers.delete(goalId);
    this.expandedRouteLines.delete(goalId);
  }

  private destroyExpandedMaps(): void {
    for (const id of this.expandedMaps.keys()) {
      this.destroyExpandedMap(id);
    }
  }

  toggleNotifFeed(): void {
    const next = !this.notifOpen();
    this.notifOpen.set(next);
    if (next) {
      this.loadNotifications(false);
    }
  }

  toggleDemoMode(): void {
    const next = !this.demoMode();
    this.switchingMode.set(true);
    this.coachingService.setSchedulerMode(next).subscribe({
      next: (res) => {
        this.demoMode.set(!!res.demoMode);
        this.switchingMode.set(false);
        this.alertDialog.info({
          zTitle: 'Reminder mode',
          zContent: this.demoMode()
            ? 'Demo mode ON: stale reminders are sent every 5 minutes.'
            : 'Normal mode ON: reminder timing follows each goal target days.',
        });
      },
      error: () => {
        this.switchingMode.set(false);
        this.alertDialog.warning({
          zTitle: 'Reminder mode',
          zContent: 'Could not switch mode. Please try again.',
        });
      },
    });
  }

  markNotificationRead(notifId: number): void {
    this.coachingService.markMyNotificationAsRead(notifId, this.notificationRecipient()).subscribe({
      next: (updated) => {
        this.notifications.update((list) => list.map((n) => (n.id === updated.id ? updated : n)));
        this.notifUnreadCount.set(this.notifications().filter((n) => !n.read).length);
      },
    });
  }

  markAllNotificationsRead(): void {
    this.coachingService.markAllMyNotificationsAsRead(this.notificationRecipient()).subscribe({
      next: () => {
        this.notifications.update((list) => list.map((n) => ({ ...n, read: true, readAt: new Date().toISOString() })));
        this.notifUnreadCount.set(0);
      },
    });
  }

  private startNotifPolling(): void {
    if (!isPlatformBrowser(this.platformId) || this.notifPollTimer) return;
    this.notifPollTimer = setInterval(() => this.loadNotifications(true), 10000);
  }

  private stopNotifPolling(): void {
    if (this.notifPollTimer) {
      clearInterval(this.notifPollTimer);
      this.notifPollTimer = null;
    }
  }

  private loadSchedulerMode(): void {
    this.coachingService.getSchedulerMode().subscribe({
      next: (res) => this.demoMode.set(!!res.demoMode),
      error: () => {
        // keep default false silently
      },
    });
  }

  private loadNotifications(showPopupForNew: boolean): void {
    this.notifLoading.set(true);
    this.coachingService.listMyNotifications(this.notificationRecipient()).subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.notifUnreadCount.set(list.filter((n) => !n.read).length);
        if (showPopupForNew) {
          const fresh = list.find((n) => !n.read && !this.seenNotifIds.has(n.id));
          if (fresh) {
            this.startReminderBellLoop();
            this.alertDialog.info({
              zTitle: fresh.title,
              zContent: fresh.message,
              zMaskClosable: false,
              zOnOk: () => {
                this.stopReminderBellLoop();
              },
            });
          }
        }
        this.seenNotifIds = new Set(list.map((n) => n.id));
        this.notifLoading.set(false);
      },
      error: () => {
        this.notifLoading.set(false);
        this.alertDialog.warning({
          zTitle: 'Notifications',
          zContent: 'Unable to load reminders. Please refresh or reconnect.',
        });
      },
    });
  }

  private playReminderBell(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      const AudioCtx = (window as any).AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx: AudioContext = this.reminderAudioCtx ?? new AudioCtx();
      this.reminderAudioCtx = ctx;
      const now = ctx.currentTime;
      const durations = [0, 0.16, 0.34];
      durations.forEach((start, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(i === 1 ? 1174.66 : 987.77, now + start);
        gain.gain.setValueAtTime(0.0001, now + start);
        gain.gain.exponentialRampToValueAtTime(0.12, now + start + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + start + 0.14);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + start);
        osc.stop(now + start + 0.16);
      });
    } catch {
      // Ignore audio limitations (autoplay policies vary by browser)
    }
  }

  private startReminderBellLoop(): void {
    if (this.reminderBellLoopTimer) return;
    this.playReminderBell();
    this.reminderBellLoopTimer = setInterval(() => this.playReminderBell(), 1500);
  }

  private stopReminderBellLoop(): void {
    if (this.reminderBellLoopTimer) {
      clearInterval(this.reminderBellLoopTimer);
      this.reminderBellLoopTimer = null;
    }
    if (this.reminderAudioCtx) {
      this.reminderAudioCtx.close().catch(() => {});
      this.reminderAudioCtx = null;
    }
  }

  private notificationRecipient(): string | undefined {
    const v = (this.notificationUserIdOverride() || '').trim();
    return v || undefined;
  }

  labelGoalType(t: CoachingGoalType): string {
    const map: Record<CoachingGoalType, string> = {
      COGNITIVE_IMPROVEMENT: 'Cognitive',
      ACTIVITY_INCREASE: 'Activity',
      MEDICATION_ADHERENCE: 'Medication',
      SOCIAL_ENGAGEMENT: 'Social',
      NUTRITION: 'Nutrition',
      OTHER: 'Other',
    };
    return map[t] ?? t;
  }
}
