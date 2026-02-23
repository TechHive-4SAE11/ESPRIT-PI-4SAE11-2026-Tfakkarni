import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '@/environments/environment';
import {
  DailyLogResponse,
  MedicationIntakeLogRequest,
  MedicationIntakeLogResponse,
  IntakeStatus,
} from '@/core/models/daily-monitoring.model';

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * Service partagé — Source unique de vérité pour le log quotidien du jour.
 *
 * Utilisé par :
 *  - PatientViewComponent  (vue patient — section Médicaments)
 *  - SuiviQuotidienComponent (vue aidant — onglet Médicaments)
 *
 * Les deux composants lisent et écrivent via ce service.
 * Toute modification dans l'un se répercute instantanément dans l'autre.
 */
@Injectable({ providedIn: 'root' })
export class DailyLogStateService {

  private readonly base = `${environment.apiBaseUrl}/api/daily-monitoring`;

  // ── État interne ────────────────────────────────────────────────────────────
  private readonly _todayLog   = signal<DailyLogResponse | null>(null);
  private readonly _loading    = signal(false);
  private readonly _patientId  = signal<string | null>(null);

  // ── Lecture publique ────────────────────────────────────────────────────────
  readonly todayLog        = this._todayLog.asReadonly();
  readonly loading         = this._loading.asReadonly();
  readonly currentPatientId = this._patientId.asReadonly();

  // ── Computed helpers ────────────────────────────────────────────────────────
  readonly todayMedications = computed<MedicationIntakeLogResponse[]>(() =>
    this._todayLog()?.medicationIntakes ?? []
  );

  readonly medsTakenCount = computed(() =>
    this.todayMedications().filter(m => m.status === 'PRIS').length
  );

  readonly medsTotal = computed(() => this.todayMedications().length);

  readonly medsProgressPercent = computed(() => {
    const t = this.medsTotal();
    return t === 0 ? 0 : Math.round((this.medsTakenCount() / t) * 100);
  });

  constructor(private readonly http: HttpClient) {}

  // ── API ─────────────────────────────────────────────────────────────────────

  /**
   * Charge (ou crée) le log du jour pour un patient.
   * À appeler une fois au démarrage de chaque vue qui a besoin des médicaments.
   * Si le patientId est identique à celui déjà chargé, évite un appel doublon.
   */
  loadTodayLog(patientKeycloakId: string, forceRefresh = false): Observable<DailyLogResponse | null> {
    // Si même patient et déjà chargé et pas forceRefresh → retourner l'état actuel
    if (!forceRefresh && this._patientId() === patientKeycloakId && this._todayLog() !== null) {
      return of(this._todayLog());
    }

    this._loading.set(true);
    this._patientId.set(patientKeycloakId);

    return this.http
      .post<DailyLogResponse>(
        `${this.base}/patient/${encodeURIComponent(patientKeycloakId)}/date/${todayIso()}`,
        {}
      )
      .pipe(
        tap(log => {
          this._todayLog.set(log);
          this._loading.set(false);
        }),
        catchError(err => {
          console.error('[DailyLogState] loadTodayLog error', err);
          this._loading.set(false);
          return of(null);
        })
      );
  }

  /**
   * Force un rechargement depuis le serveur (après toute modification).
   */
  refresh(patientKeycloakId?: string): Observable<DailyLogResponse | null> {
    const id = patientKeycloakId ?? this._patientId();
    if (!id) return of(null);
    return this.loadTodayLog(id, true);
  }

  /**
   * Met à jour le statut d'un médicament et synchronise l'état partagé
   * sans attendre un refresh complet (optimistic update).
   */
  updateMedicationStatus(
    medId: number,
    newStatus: IntakeStatus,
    takenAt?: string
  ): void {
    const current = this._todayLog();
    if (!current) return;
    this._todayLog.set({
      ...current,
      medicationIntakes: current.medicationIntakes.map(m =>
        m.id === medId
          ? { ...m, status: newStatus, takenAt: newStatus === 'PRIS' ? (takenAt ?? m.takenAt) : m.takenAt }
          : m
      ),
    });
  }

  /**
   * API call — met à jour une prise médicamenteuse sur le serveur
   * puis applique l'optimistic update dans le signal partagé.
   */
  toggleMedication(
    logId: number,
    med: MedicationIntakeLogResponse,
    newStatus: IntakeStatus,
    takenAt?: string
  ): Observable<MedicationIntakeLogResponse | null> {
    const dto: MedicationIntakeLogRequest = {
      medicationId: med.medicationId,
      takenAt: newStatus === 'PRIS' ? (takenAt ?? med.takenAt) : med.takenAt,
      status: newStatus,
      notes: med.notes,
    };

    return this.http
      .put<MedicationIntakeLogResponse>(
        `${this.base}/${logId}/medication-intakes/${med.id}`,
        dto
      )
      .pipe(
        tap(updated => {
          // Mettre à jour le signal partagé immédiatement
          this.updateMedicationStatus(updated.id, updated.status as IntakeStatus, updated.takenAt);
        }),
        catchError(err => {
          console.error('[DailyLogState] toggleMedication error', err);
          return of(null);
        })
      );
  }

  /**
   * Ecrit un log externe dans le state (ex. après getOrCreate depuis SuiviQuotidien)
   * pour que PatientView voie les changements sans re-fetcher.
   */
  setLog(log: DailyLogResponse): void {
    this._todayLog.set(log);
  }

  /**
   * Retourne l'id du log courant (null si pas encore chargé).
   */
  get currentLogId(): number | null {
    return this._todayLog()?.id ?? null;
  }
}
