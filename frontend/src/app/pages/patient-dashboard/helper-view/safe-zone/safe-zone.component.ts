import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  Input,
  Output,
  EventEmitter,
  signal,
  inject,
  PLATFORM_ID,
  ElementRef,
  ViewChild,
  DestroyRef,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, switchMap, catchError, of, timer } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import {
  SafeZoneService,
  type SafeZoneResponse,
  type SafeZoneRequest,
  type GeofenceAlertResponse,
} from '@/core/services/safe-zone.service';
import { DweetService, type DweetPosition } from '@/core/services/dweet.service';
import { isPointInPolygon } from '@/core/utils/geofence.util';

@Component({
  selector: 'app-safe-zone',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardBadgeComponent,
  ],
  template: `
    <div class="flex items-center gap-2 mb-6">
      <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
        <z-icon zType="arrow-left" class="mr-1" />
        Back
      </button>
      <h2 class="text-2xl font-bold">Safe Zone — GPS Geofencing</h2>
    </div>

    <!-- GEOFENCE VIOLATION ALERT BANNER -->
    @if (isOutsideZone()) {
      <div class="mb-6 p-4 rounded-lg bg-destructive/10 border-2 border-destructive animate-pulse flex items-center gap-3">
        <z-icon zType="alert-triangle" class="h-8 w-8 text-destructive shrink-0" />
        <div class="flex-1">
          <h3 class="font-bold text-destructive text-lg">⚠️ GEOFENCE ALERT</h3>
          <p class="text-destructive text-sm">
            Patient is OUTSIDE all safe zones!
            @if (lastGpsPosition()) {
              Location: {{ lastGpsPosition()!.lat.toFixed(6) }}, {{ lastGpsPosition()!.lng.toFixed(6) }}
            }
          </p>
        </div>
        <button z-button zType="destructive" zSize="sm" (click)="dismissAlert()">
          Dismiss
        </button>
      </div>
    }

    <!-- GPS STATUS BAR -->
    <z-card class="p-4 mb-6">
      <!-- Thing name config -->
      <div class="mb-3 pb-3 border-b border-border">
        <label class="text-xs font-medium text-muted-foreground block mb-1">
          dweet.cc thing name — must match <code class="bg-muted px-1 rounded">THING_NAME</code> in your Python script
        </label>
        <div class="flex gap-2 items-center">
          <input
            class="flex-1 px-2 py-1 text-sm border border-border rounded-md bg-background text-foreground font-mono focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="thingName()"
            (input)="thingName.set($any($event.target).value)"
            placeholder="tfk-gps-..."
          />
          <button z-button zType="outline" zSize="sm" (click)="resetThingName()">
            Reset
          </button>
        </div>
      </div>
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2">
            @if (isTracking()) {
              <div class="w-3 h-3 rounded-full bg-green-500 animate-pulse"></div>
              <span class="text-sm font-medium text-green-700 dark:text-green-400">GPS Live</span>
            } @else {
              <div class="w-3 h-3 rounded-full bg-muted-foreground"></div>
              <span class="text-sm text-muted-foreground">GPS Offline</span>
            }
          </div>
          @if (lastGpsPosition()) {
            <z-badge zType="secondary">
              <z-icon zType="map-pin" class="mr-1 inline" />
              {{ lastGpsPosition()!.lat.toFixed(5) }}, {{ lastGpsPosition()!.lng.toFixed(5) }}
            </z-badge>
          }
          @if (gpsError()) {
            <div class="text-xs text-destructive">
              ❌ {{ gpsError() }}
              <span class="text-muted-foreground ml-1">(polling: dweet.cc/get/latest/dweet/for/{{ thingName() }})</span>
            </div>
          }
        </div>
        <div class="flex gap-2">
          @if (!isTracking()) {
            <button z-button zSize="sm" (click)="startTracking()">
              <z-icon zType="play" class="mr-1" />
              Start Tracking
            </button>
          } @else {
            <button z-button zType="outline" zSize="sm" (click)="stopTracking()">
              <z-icon zType="x" class="mr-1" />
              Stop Tracking
            </button>
          }
        </div>
      </div>
    </z-card>

    <!-- MAP + DRAWING -->
    <z-card class="p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-semibold">
          @if (isDrawing()) {
            Drawing Zone — Click map to add points ({{ drawingPoints().length }} vertices)
          } @else if (editingZoneId()) {
            Editing Zone — Click map to add points ({{ drawingPoints().length }} vertices)
          } @else {
            Safe Zone Map
          }
        </h3>
        <div class="flex gap-2">
          @if (!isDrawing() && !editingZoneId()) {
            <button z-button zSize="sm" (click)="startDrawing()">
              <z-icon zType="plus" class="mr-1" />
              Draw Zone
            </button>
          } @else {
            @if (drawingPoints().length >= 3) {
              <button z-button zSize="sm" (click)="finishDrawing()">
                <z-icon zType="check" class="mr-1" />
                Finish ({{ drawingPoints().length }} pts)
              </button>
            }
            <button z-button zType="outline" zSize="sm" (click)="cancelDrawing()">
              <z-icon zType="x" class="mr-1" />
              Cancel
            </button>
          }
        </div>
      </div>

      <div
        #mapContainer
        class="w-full h-[450px] rounded-lg border border-border z-0"
      ></div>

      @if (isDrawing() || editingZoneId()) {
        <p class="text-sm text-muted-foreground mt-3">
          <z-icon zType="info" class="inline mr-1" />
          Click on the map to add polygon vertices. Minimum 3 points required.
          @if (drawingPoints().length > 0) {
            <button class="text-primary underline ml-2" (click)="undoLastPoint()">Undo last point</button>
          }
        </p>
      }
    </z-card>

    <!-- SAVE ZONE FORM (visible after finishing drawing) -->
    @if (showSaveForm()) {
      <z-card class="p-6 mb-6">
        <h3 class="font-semibold mb-4">{{ editingZoneId() ? 'Update Safe Zone' : 'Save Safe Zone' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium mb-1 block">Zone Name</label>
            <input
              class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              [value]="zoneName()"
              (input)="zoneName.set($any($event.target).value)"
              placeholder="e.g., Home Area, Neighborhood, Garden"
            />
          </div>
          <z-badge zType="secondary">{{ drawingPoints().length }} vertices</z-badge>
        </div>

        @if (errorMessage()) {
          <div class="mt-4 p-4 rounded-md bg-destructive/10 border border-destructive text-destructive text-sm">
            {{ errorMessage() }}
          </div>
        }
        @if (successMessage()) {
          <div class="mt-4 p-4 rounded-md bg-green-500/10 border border-green-500 text-green-700 text-sm">
            {{ successMessage() }}
          </div>
        }

        <div class="mt-4 flex gap-3">
          <button z-button [disabled]="!canSave()" (click)="saveZone()">
            @if (saving()) {
              <z-icon zType="loader-2" class="mr-2 animate-spin" />
              Saving...
            } @else {
              <z-icon zType="check" class="mr-2" />
              {{ editingZoneId() ? 'Update Zone' : 'Save Zone' }}
            }
          </button>
          <button z-button zType="outline" (click)="cancelDrawing()">Cancel</button>
        </div>
      </z-card>
    }

    <!-- SAVED ZONES LIST -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-xl font-bold">Saved Safe Zones ({{ zones().length }})</h3>
    </div>

    @if (zones().length > 0) {
      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3 mb-6">
        @for (zone of zones(); track zone.id) {
          <z-card class="p-5">
            <div class="flex items-start justify-between mb-2">
              <div>
                <h4 class="font-semibold">{{ zone.name }}</h4>
                <p class="text-sm text-muted-foreground">{{ zone.points.length }} vertices</p>
              </div>
              <z-badge [zType]="zone.active ? 'default' : 'secondary'">
                {{ zone.active ? 'Active' : 'Inactive' }}
              </z-badge>
            </div>
            <p class="text-xs text-muted-foreground mb-3">
              Created {{ zone.createdAt | date:'mediumDate' }}
            </p>
            <div class="flex gap-2 flex-wrap">
              <button z-button zSize="sm" (click)="focusZone(zone)">
                <z-icon zType="eye" class="mr-1" />
                Focus
              </button>
              <button z-button zSize="sm" zType="outline" (click)="startEdit(zone)">
                <z-icon zType="edit" class="mr-1" />
                Edit
              </button>
              <button z-button zType="destructive" zSize="sm" (click)="deleteZone(zone)">
                <z-icon zType="trash-2" class="mr-1" />
                Delete
              </button>
            </div>
          </z-card>
        }
      </div>
    } @else {
      <z-card class="p-12 text-center mb-6">
        <z-icon zType="shield" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 class="font-semibold mb-2">No safe zones defined</h3>
        <p class="text-muted-foreground">Draw a zone on the map to set up geofencing for the patient.</p>
      </z-card>
    }

    <!-- VIOLATION HISTORY -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-xl font-bold">Violation History ({{ violations().length }})</h3>
      @if (violations().length > 0) {
        <button z-button zType="outline" zSize="sm" (click)="loadViolations()">
          <z-icon zType="rotate-ccw" class="mr-1" />
          Refresh
        </button>
      }
    </div>

    @if (violations().length > 0) {
      <div class="space-y-3">
        @for (v of violations(); track v.id) {
          <z-card class="p-4">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <z-icon zType="alert-triangle" class="h-5 w-5 text-destructive" />
                <div>
                  <p class="font-medium text-sm">
                    Left zone "{{ v.safeZoneName }}"
                  </p>
                  <p class="text-xs text-muted-foreground">
                    {{ v.createdAt | date:'medium' }} — GPS: {{ v.latitude.toFixed(5) }}, {{ v.longitude.toFixed(5) }}
                  </p>
                </div>
              </div>
              @if (!v.acknowledged) {
                <button z-button zSize="sm" zType="outline" (click)="acknowledgeViolation(v)">
                  <z-icon zType="check" class="mr-1" />
                  Acknowledge
                </button>
              } @else {
                <z-badge zType="secondary">Acknowledged</z-badge>
              }
            </div>
          </z-card>
        }
      </div>
    } @else {
      <z-card class="p-8 text-center">
        <z-icon zType="check" class="mx-auto h-10 w-10 text-green-500 mb-3" />
        <p class="text-muted-foreground">No geofence violations recorded.</p>
      </z-card>
    }
  `,
})
export class SafeZoneComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;

  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly safeZoneService = inject(SafeZoneService);
  private readonly dweetService = inject(DweetService);
  private readonly keycloakService = inject(KeycloakService);

  private map: any = null;
  private L: any = null;
  private gpsMarker: any = null;
  private gpsSubscription: Subscription | null = null;
  private readonly drawnPolygons: Map<number, any> = new Map();
  private drawingMarkers: any[] = [];
  private drawingPolyline: any = null;
  private previewPolygon: any = null;
  private alertAudio: HTMLAudioElement | null = null;

  // State signals
  zones = signal<SafeZoneResponse[]>([]);
  violations = signal<GeofenceAlertResponse[]>([]);
  isDrawing = signal(false);
  drawingPoints = signal<{ lat: number; lng: number }[]>([]);
  showSaveForm = signal(false);
  zoneName = signal('');
  editingZoneId = signal<number | null>(null);
  saving = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  // GPS tracking
  isTracking = signal(false);
  lastGpsPosition = signal<DweetPosition | null>(null);
  gpsError = signal('');
  isOutsideZone = signal(false);
  thingName = signal('');
  private lastViolationReportedAt = 0;
  private readonly VIOLATION_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

  ngOnInit(): void {
    if (this.keycloakId) {
      this.thingName.set(`tfk-gps-${this.keycloakId}`);
      this.loadZones();
      this.loadViolations();
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.initMap();
      this.initAlertAudio();
    }
  }

  ngOnDestroy(): void {
    this.stopTracking();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  // ─── Map ─────────────────────────────────────────────

  private async initMap(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;

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

    this.map = this.L.map(this.mapContainer.nativeElement).setView([36.8, 10.18], 13);

    this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(this.map);

    this.map.on('click', (e: any) => this.onMapClick(e));
  }

  private onMapClick(e: any): void {
    if (!this.isDrawing() && !this.editingZoneId()) return;

    const point = { lat: e.latlng.lat, lng: e.latlng.lng };
    this.drawingPoints.update(pts => [...pts, point]);

    // Add small circle marker for the vertex
    const marker = this.L.circleMarker(e.latlng, {
      radius: 6,
      color: '#6366f1',
      fillColor: '#6366f1',
      fillOpacity: 0.8,
    }).addTo(this.map);
    this.drawingMarkers.push(marker);

    this.updateDrawingPreview();
  }

  private updateDrawingPreview(): void {
    const pts = this.drawingPoints();
    const latlngs = pts.map(p => [p.lat, p.lng]);

    // Remove old preview
    if (this.drawingPolyline) {
      this.map.removeLayer(this.drawingPolyline);
      this.drawingPolyline = null;
    }
    if (this.previewPolygon) {
      this.map.removeLayer(this.previewPolygon);
      this.previewPolygon = null;
    }

    if (latlngs.length >= 3) {
      this.previewPolygon = this.L.polygon(latlngs, {
        color: '#6366f1',
        fillColor: '#6366f1',
        fillOpacity: 0.15,
        dashArray: '5, 10',
      }).addTo(this.map);
    } else if (latlngs.length >= 2) {
      this.drawingPolyline = this.L.polyline(latlngs, {
        color: '#6366f1',
        dashArray: '5, 10',
      }).addTo(this.map);
    }
  }

  private clearDrawingLayers(): void {
    for (const m of this.drawingMarkers) {
      this.map?.removeLayer(m);
    }
    this.drawingMarkers = [];
    if (this.drawingPolyline) {
      this.map?.removeLayer(this.drawingPolyline);
      this.drawingPolyline = null;
    }
    if (this.previewPolygon) {
      this.map?.removeLayer(this.previewPolygon);
      this.previewPolygon = null;
    }
  }

  private renderAllZones(): void {
    // Clear existing polygons from map
    for (const [, polygon] of this.drawnPolygons) {
      this.map?.removeLayer(polygon);
    }
    this.drawnPolygons.clear();

    if (!this.L || !this.map) return;

    for (const zone of this.zones()) {
      const latlngs = zone.points.map(p => [p.lat, p.lng]);
      const color = zone.active ? '#22c55e' : '#9ca3af';
      const polygon = this.L.polygon(latlngs, {
        color,
        fillColor: color,
        fillOpacity: zone.active ? 0.15 : 0.05,
        weight: 2,
      }).addTo(this.map);
      polygon.bindTooltip(zone.name, { permanent: false, direction: 'center' });
      this.drawnPolygons.set(zone.id, polygon);
    }
  }

  // ─── Drawing controls ────────────────────────────────

  startDrawing(): void {
    this.isDrawing.set(true);
    this.drawingPoints.set([]);
    this.showSaveForm.set(false);
    this.zoneName.set('');
    this.editingZoneId.set(null);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.clearDrawingLayers();
  }

  finishDrawing(): void {
    if (this.drawingPoints().length < 3) return;
    this.isDrawing.set(false);
    this.showSaveForm.set(true);
  }

  cancelDrawing(): void {
    this.isDrawing.set(false);
    this.showSaveForm.set(false);
    this.editingZoneId.set(null);
    this.drawingPoints.set([]);
    this.zoneName.set('');
    this.errorMessage.set('');
    this.successMessage.set('');
    this.clearDrawingLayers();
  }

  undoLastPoint(): void {
    this.drawingPoints.update(pts => pts.slice(0, -1));
    const lastMarker = this.drawingMarkers.pop();
    if (lastMarker) this.map?.removeLayer(lastMarker);
    this.updateDrawingPreview();
  }

  canSave(): boolean {
    return (
      this.zoneName().trim().length > 0 &&
      this.drawingPoints().length >= 3 &&
      !this.saving()
    );
  }

  // ─── CRUD ────────────────────────────────────────────

  async saveZone(): Promise<void> {
    if (!this.canSave()) return;
    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    try {
      const kc = this.keycloakService.getKeycloakInstance();
      if (kc?.refreshToken) {
        await this.keycloakService.updateToken(30);
      }
    } catch (e) {
      console.warn('[SafeZone] Token refresh failed, proceeding', e);
    }

    const request: SafeZoneRequest = {
      name: this.zoneName(),
      points: this.drawingPoints(),
      active: true,
    };

    const editId = this.editingZoneId();
    const request$ = editId
      ? this.safeZoneService.updateSafeZone(this.keycloakId, editId, request)
      : this.safeZoneService.createSafeZone(this.keycloakId, request);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set(
          this.editingZoneId() ? 'Safe zone updated!' : 'Safe zone saved!'
        );
        this.cancelDrawing();
        this.loadZones();
      },
      error: (err) => {
        console.error('[SafeZone] Failed to save zone', err);
        this.saving.set(false);
        const status = err?.status;
        let msg = 'Failed to save zone: ';
        if (status === 401 || status === 403) {
          msg += 'Authentication error. Please log out and log back in.';
        } else if (status === 0) {
          msg += 'Could not reach the server. Check if the API gateway is running.';
        } else {
          msg += err?.error?.message || err?.message || 'Unknown error';
        }
        this.errorMessage.set(msg);
      },
    });
  }

  startEdit(zone: SafeZoneResponse): void {
    this.clearDrawingLayers();
    this.editingZoneId.set(zone.id);
    this.zoneName.set(zone.name);
    this.drawingPoints.set([...zone.points]);
    this.showSaveForm.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    // Re-render drawing markers for the existing points
    if (this.L && this.map) {
      for (const pt of zone.points) {
        const marker = this.L.circleMarker([pt.lat, pt.lng], {
          radius: 6,
          color: '#6366f1',
          fillColor: '#6366f1',
          fillOpacity: 0.8,
        }).addTo(this.map);
        this.drawingMarkers.push(marker);
      }
      this.updateDrawingPreview();

      // Fit map to zone bounds
      const latlngs = zone.points.map(p => this.L.latLng(p.lat, p.lng));
      const bounds = this.L.latLngBounds(latlngs);
      this.map.fitBounds(bounds, { padding: [50, 50] });
    }
  }

  deleteZone(zone: SafeZoneResponse): void {
    const ref = this.alertDialog.confirm({
      zTitle: 'Delete Safe Zone',
      zDescription: `Are you sure you want to delete "${zone.name}"? This cannot be undone.`,
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.safeZoneService.deleteSafeZone(this.keycloakId, zone.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.loadZones(),
            error: (err) => console.error('Failed to delete zone', err),
          });
        ref.close();
      },
    });
  }

  focusZone(zone: SafeZoneResponse): void {
    if (!this.L || !this.map) return;
    const latlngs = zone.points.map(p => this.L.latLng(p.lat, p.lng));
    const bounds = this.L.latLngBounds(latlngs);
    this.map.fitBounds(bounds, { padding: [50, 50] });
  }

  loadZones(): void {
    if (!this.keycloakId) return;
    this.safeZoneService.getSafeZones(this.keycloakId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (zones) => {
          this.zones.set(zones);
          this.renderAllZones();
        },
        error: (err) => console.error('Failed to load safe zones', err),
      });
  }

  loadViolations(): void {
    if (!this.keycloakId) return;
    this.safeZoneService.getViolationHistory(this.keycloakId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (v) => this.violations.set(v),
        error: (err) => console.error('Failed to load violations', err),
      });
  }

  acknowledgeViolation(v: GeofenceAlertResponse): void {
    this.safeZoneService.acknowledgeViolation(v.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.violations.update(list =>
            list.map(item => item.id === v.id ? { ...item, acknowledged: true } : item)
          );
        },
        error: (err) => console.error('Failed to acknowledge violation', err),
      });
  }

  // ─── GPS Tracking ────────────────────────────────────

  startTracking(): void {
    if (this.isTracking()) return;
    this.isTracking.set(true);
    this.gpsError.set('');

    const thingName = this.thingName() || `tfk-gps-${this.keycloakId}`;
    console.log(`[SafeZone] Polling dweet.cc thing: ${thingName}`);

    // timer(0, 3000): emit immediately (0ms delay), then every 3s
    this.gpsSubscription = timer(0, 3000)
      .pipe(
        switchMap(() =>
          this.dweetService.getLatestPosition(thingName).pipe(
            catchError((err) => {
              const msg = err?.message || String(err);
              console.warn(`[SafeZone] GPS poll failed for "${thingName}":`, msg);
              this.gpsError.set(msg);
              return of(null);
            })
          )
        )
      )
      .subscribe((position) => {
        if (!position) return;

        this.gpsError.set('');
        this.lastGpsPosition.set(position);
        this.updateGpsMarker(position);
        this.checkGeofence(position);
      });
  }

  stopTracking(): void {
    this.isTracking.set(false);
    this.gpsSubscription?.unsubscribe();
    this.gpsSubscription = null;
  }

  resetThingName(): void {
    this.thingName.set(`tfk-gps-${this.keycloakId}`);
  }

  private updateGpsMarker(position: DweetPosition): void {
    if (!this.L || !this.map) return;

    const latlng = this.L.latLng(position.lat, position.lng);

    if (this.gpsMarker) {
      this.gpsMarker.setLatLng(latlng);
    } else {
      const patientIcon = this.L.divIcon({
        html: '<div style="background:#ef4444;width:16px;height:16px;border-radius:50%;border:3px solid white;box-shadow:0 0 8px rgba(239,68,68,0.6);"></div>',
        iconSize: [16, 16],
        iconAnchor: [8, 8],
        className: '',
      });

      this.gpsMarker = this.L.marker(latlng, { icon: patientIcon }).addTo(this.map);
      this.gpsMarker.bindTooltip('Patient GPS', { permanent: false });
    }
  }

  private checkGeofence(position: DweetPosition): void {
    const activeZones = this.zones().filter(z => z.active);
    if (activeZones.length === 0) {
      this.isOutsideZone.set(false);
      return;
    }

    const isInsideAny = activeZones.some(zone =>
      isPointInPolygon(position, zone.points)
    );

    if (isInsideAny) {
      this.isOutsideZone.set(false);
    } else {
      this.isOutsideZone.set(true);
      this.playAlertSound();

      // Report violation with cooldown
      const now = Date.now();
      if (now - this.lastViolationReportedAt > this.VIOLATION_COOLDOWN_MS) {
        this.lastViolationReportedAt = now;
        const nearestZone = activeZones[0]?.name || 'Unknown';
        this.safeZoneService.reportViolation({
          patientId: this.keycloakId,
          latitude: position.lat,
          longitude: position.lng,
          safeZoneName: nearestZone,
        })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => this.loadViolations(),
          error: (err) => console.error('Failed to report violation', err),
        });
      }
    }
  }

  dismissAlert(): void {
    this.isOutsideZone.set(false);
  }

  // ─── Audio ───────────────────────────────────────────

  private initAlertAudio(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    // Create a simple beep using AudioContext
    this.alertAudio = null; // Will use AudioContext on demand
  }

  private playAlertSound(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      const ctx = new AudioContext();
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.frequency.value = 800;
      oscillator.type = 'square';
      gain.gain.value = 0.3;
      oscillator.start();
      oscillator.stop(ctx.currentTime + 0.3);
    } catch {
      // Audio not supported or blocked
    }
  }
}
