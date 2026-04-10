import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@/environments/environment';
import type { FeatureGateResponse } from '@/core/models/analytics.model';

@Injectable({ providedIn: 'root' })
export class FeatureGateService {
  private readonly base = `${environment.apiBaseUrl}/api/analytics`;
  private readonly CACHE_KEY = 'tfk_feature_gates';
  private readonly CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

  readonly gates = signal<FeatureGateResponse | null>(null);
  readonly loading = signal(false);

  readonly stage = computed(() => this.gates()?.stage ?? 'UNKNOWN');
  readonly iotEnabled = computed(() => this.gates()?.iotEnabled ?? false);
  readonly iotLevel = computed(() => this.gates()?.iotLevel ?? 'DISABLED');
  readonly gameComplexity = computed(() => this.gates()?.gameComplexity ?? 'STANDARD');
  readonly monitoringLevel = computed(() => this.gates()?.monitoringLevel ?? 'OPTIONAL');
  readonly uiMode = computed(() => this.gates()?.uiMode ?? 'STANDARD');
  readonly safeZoneRequired = computed(() => this.gates()?.safeZoneRequired ?? false);
  readonly notificationEscalation = computed(() => this.gates()?.notificationEscalation ?? 'LOW');

  constructor(private readonly http: HttpClient) {
    this.loadFromCache();
  }

  loadGates(patientKeycloakId: string): void {
    // Check cache first
    const cached = this.getCached();
    if (cached && cached.patientKeycloakId === patientKeycloakId) {
      this.gates.set(cached);
      return;
    }

    this.loading.set(true);
    this.http
      .get<FeatureGateResponse>(
        `${this.base}/patient/${encodeURIComponent(patientKeycloakId)}/feature-gates`
      )
      .subscribe({
        next: (response) => {
          this.gates.set(response);
          this.saveToCache(response);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  private loadFromCache(): void {
    const cached = this.getCached();
    if (cached) {
      this.gates.set(cached);
    }
  }

  private getCached(): FeatureGateResponse | null {
    try {
      const raw = localStorage.getItem(this.CACHE_KEY);
      if (!raw) return null;
      const { data, timestamp } = JSON.parse(raw);
      if (Date.now() - timestamp > this.CACHE_TTL_MS) {
        localStorage.removeItem(this.CACHE_KEY);
        return null;
      }
      return data as FeatureGateResponse;
    } catch {
      return null;
    }
  }

  private saveToCache(data: FeatureGateResponse): void {
    try {
      localStorage.setItem(
        this.CACHE_KEY,
        JSON.stringify({ data, timestamp: Date.now() })
      );
    } catch {
      // localStorage full or unavailable
    }
  }

  clearCache(): void {
    localStorage.removeItem(this.CACHE_KEY);
    this.gates.set(null);
  }
}
