import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  Input,
  Output,
  EventEmitter,
  AfterViewInit,
  ElementRef,
  ViewChild,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { PlaceService, type PlaceResponse, type CreatePlaceRequest } from '@/core/services/place.service';

@Component({
  selector: 'app-add-place',
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
      <h2 class="text-2xl font-bold">Guess the Place — Manage Places</h2>
    </div>

    <!-- Add / Edit place form -->
    <z-card class="p-6 mb-6">
      <h3 class="font-semibold mb-4">{{ editingPlaceId() ? 'Edit Place' : 'Add a New Place' }}</h3>
      <p class="text-sm text-muted-foreground mb-4">
        Click on the map to select a location, then enter a name and save.
      </p>

      <!-- Map -->
      <div
        #mapContainer
        class="w-full h-[350px] rounded-lg border border-border mb-4 z-0"
      ></div>

      @if (selectedLat() !== null) {
        <div class="flex gap-4 mb-4 text-sm">
          <z-badge zType="secondary">Lat: {{ selectedLat()!.toFixed(5) }}</z-badge>
          <z-badge zType="secondary">Lng: {{ selectedLng()!.toFixed(5) }}</z-badge>
        </div>
      } @else {
        <p class="text-sm text-muted-foreground mb-4">
          <z-icon zType="map-pin" class="inline mr-1" />
          Click the map to pick a location
        </p>
      }

      <div class="space-y-4">
        <div>
          <label class="text-sm font-medium mb-1 block">Place Name</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="placeName()"
            (input)="placeName.set($any($event.target).value)"
            placeholder="e.g., Home, Central Park, Grandma's House"
          />
        </div>
        <div>
          <label class="text-sm font-medium mb-1 block">Hint (optional)</label>
          <input
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            [value]="placeHint()"
            (input)="placeHint.set($any($event.target).value)"
            placeholder="e.g., Where we have breakfast every morning"
          />
        </div>
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
        <button
          z-button
          [disabled]="!canSave()"
          (click)="savePlace()"
        >
          @if (saving()) {
            <z-icon zType="loader-2" class="mr-2 animate-spin" />
            Saving...
          } @else {
            <z-icon zType="check" class="mr-2" />
            {{ editingPlaceId() ? 'Save Changes' : 'Save Place' }}
          }
        </button>
        <button z-button zType="outline" (click)="resetForm()">{{ editingPlaceId() ? 'Cancel Edit' : 'Clear' }}</button>
      </div>
    </z-card>

    <!-- Saved places list -->
    <h3 class="text-xl font-bold mb-4">Saved Places ({{ places().length }})</h3>

    @if (places().length > 0) {
      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        @for (place of places(); track place.id) {
          <z-card class="p-5">
            <div class="flex items-start justify-between mb-2">
              <div>
                <h4 class="font-semibold">{{ place.name }}</h4>
                @if (place.hint) {
                  <p class="text-sm text-muted-foreground">{{ place.hint }}</p>
                }
              </div>
              <z-badge zType="secondary">
                <z-icon zType="map-pin" class="mr-1 inline" />
                {{ place.latitude.toFixed(2) }}, {{ place.longitude.toFixed(2) }}
              </z-badge>
            </div>
            <p class="text-xs text-muted-foreground mb-3">
              Added {{ place.createdAt | date:'mediumDate' }}
            </p>
            <div class="flex gap-2">
              <button z-button zSize="sm" (click)="startEdit(place)">
                <z-icon zType="settings" class="mr-1" />
                Edit
              </button>
              <button z-button zType="destructive" zSize="sm" (click)="deletePlace(place.id)">
                <z-icon zType="trash-2" class="mr-1" />
                Remove
              </button>
            </div>
          </z-card>
        }
      </div>
    } @else {
      <z-card class="p-12 text-center">
        <z-icon zType="map-pin" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 class="font-semibold mb-2">No places saved yet</h3>
        <p class="text-muted-foreground">Add at least 3 places so the patient can play the guessing game!</p>
      </z-card>
    }
  `,
})
export class AddPlaceComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;

  private map: any = null;
  private marker: any = null;
  private L: any = null;
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly platformId = inject(PLATFORM_ID);

  editingPlaceId = signal<number | null>(null);
  placeName = signal('');
  placeHint = signal('');
  selectedLat = signal<number | null>(null);
  selectedLng = signal<number | null>(null);
  saving = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  places = signal<PlaceResponse[]>([]);

  constructor(
    private readonly placeService: PlaceService,
    private readonly keycloakService: KeycloakService,
  ) {}

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadPlaces();
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.initMap();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  canSave(): boolean {
    return (
      this.placeName().trim().length > 0 &&
      this.selectedLat() !== null &&
      this.selectedLng() !== null &&
      !this.saving()
    );
  }

  private async initMap(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;
    
    // Dynamically import Leaflet only in browser
    this.L = await import('leaflet');
    
    // Fix Leaflet default icon paths (common issue with bundlers)
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

    this.map = this.L.map(this.mapContainer.nativeElement).setView([36.8, 10.18], 12);

    this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      const { lat, lng } = e.latlng;
      this.selectedLat.set(lat);
      this.selectedLng.set(lng);

      if (this.marker) {
        this.marker.setLatLng(e.latlng);
      } else {
        this.marker = this.L.marker(e.latlng).addTo(this.map!);
      }
    });
  }

  async savePlace(): Promise<void> {
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
      console.warn('[AddPlace] Token refresh failed, proceeding', e);
    }

    const request: CreatePlaceRequest = {
      name: this.placeName(),
      latitude: this.selectedLat()!,
      longitude: this.selectedLng()!,
      hint: this.placeHint(),
    };

    const request$ = this.editingPlaceId()
      ? this.placeService.editPlace(this.editingPlaceId()!, request)
      : this.placeService.createPlace(this.keycloakId, request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set(this.editingPlaceId() ? 'Place updated successfully!' : 'Place saved successfully!');
        this.resetForm();
        this.loadPlaces();
      },
      error: err => {
        console.error('[AddPlace] Failed to save place', err);
        this.saving.set(false);
        const status = err?.status;
        let msg = 'Failed to save place: ';
        if (status === 401 || status === 403) {
          msg += 'Authentication error. Please log out and log back in.';
        } else if (status === 0) {
          msg += 'Could not reach the server. Check if the API gateway is running on port 9090.';
        } else {
          msg += (err?.error?.error || err?.message || 'Unknown error');
        }
        this.errorMessage.set(msg);
      },
    });
  }

  startEdit(place: PlaceResponse): void {
    this.editingPlaceId.set(place.id);
    this.placeName.set(place.name);
    this.placeHint.set(place.hint || '');
    this.selectedLat.set(place.latitude);
    this.selectedLng.set(place.longitude);
    this.errorMessage.set('');
    this.successMessage.set('');

    // Update the map marker
    if (this.map && this.L) {
      const latlng = this.L.latLng(place.latitude, place.longitude);
      if (this.marker) {
        this.marker.setLatLng(latlng);
      } else {
        this.marker = this.L.marker(latlng).addTo(this.map);
      }
      this.map.setView(latlng, 14);
    }

    // Scroll to top of form
    if (isPlatformBrowser(this.platformId)) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  deletePlace(id: number): void {
    const ref = this.alertDialog.confirm({
      zTitle: 'Delete Place',
      zDescription: 'Are you sure you want to remove this place? This action cannot be undone.',
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        this.placeService.deletePlace(id).subscribe({
          next: () => this.loadPlaces(),
          error: err => console.error('Failed to delete place', err),
        });
        ref.close();
      },
    });
  }

  resetForm(): void {
    this.editingPlaceId.set(null);
    this.placeName.set('');
    this.placeHint.set('');
    this.selectedLat.set(null);
    this.selectedLng.set(null);
    this.errorMessage.set('');
    if (this.marker && this.map) {
      this.map.removeLayer(this.marker);
      this.marker = null;
    }
  }

  private loadPlaces(): void {
    if (!this.keycloakId) return;
    this.placeService.getPatientPlaces(this.keycloakId).subscribe({
      next: places => this.places.set(places),
      error: err => console.error('Failed to load places', err),
    });
  }
}
