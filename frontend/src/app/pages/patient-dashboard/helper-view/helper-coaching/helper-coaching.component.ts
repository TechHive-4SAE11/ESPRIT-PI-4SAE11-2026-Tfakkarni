import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PLATFORM_ID } from '@angular/core';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';
import { UserApiService } from '@/core/services/user-api.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { CoachingPanelComponent } from '@/pages/medical-folders/coaching-panel/coaching-panel.component';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

/** If set, folder list uses this patient Keycloak subject instead of the logged-in user (helper-only logins). */
const PATIENT_OVERRIDE_STORAGE_KEY = 'tfk_helper_coaching_patient_keycloak_id';

@Component({
  selector: 'app-helper-coaching',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    CoachingPanelComponent,
  ],
  template: `
    <div class="space-y-4">
      <div class="flex items-center gap-2 mb-2">
        <button z-button zType="ghost" zSize="sm" type="button" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-1" />
          Back
        </button>
        <h2 class="text-2xl font-bold">Coaching goals</h2>
      </div>

      <z-card class="p-4 bg-muted/40 border-dashed">
        <p class="text-sm text-foreground leading-relaxed">
          <strong class="font-semibold">How this screen finds folders:</strong> medical folders (and coaching goals) belong to the
          <strong>patient’s</strong> Keycloak user id. In this app, the helper usually uses the
          <strong>same login as the patient</strong> and switches &quot;Patient&quot; / &quot;Helper&quot; in the header.
        </p>
        <p class="text-sm text-muted-foreground mt-2">
          If you use a <strong>separate helper account</strong>, your own id is not the patient id — so no folders appear until you set the patient id below.
        </p>
      </z-card>

      <div class="rounded-lg border border-border bg-card p-4 space-y-3">
        <label class="block text-sm font-medium">Patient Keycloak ID (optional override)</label>
        <p class="text-xs text-muted-foreground">
          Paste the patient’s Keycloak <code class="rounded bg-muted px-1">sub</code> (same value stored on the medical folder as
          patient). Current lookup:
          <code class="rounded bg-muted px-1 text-[11px] break-all">{{ effectivePatientId() || '—' }}</code>
        </p>
        <div class="flex flex-col sm:flex-row gap-2">
          <input
            type="text"
            class="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm font-mono"
            placeholder="Patient Keycloak subject (sub)"
            [(ngModel)]="patientIdOverrideDraft"
            (keyup.enter)="applyPatientOverride()"
          />
          <div class="flex gap-2 shrink-0">
            <button z-button zSize="sm" type="button" (click)="applyPatientOverride()">Apply</button>
            <button z-button zType="outline" zSize="sm" type="button" (click)="clearPatientOverride()">Clear</button>
          </div>
        </div>
      </div>

      <p class="text-muted-foreground text-sm">Select a medical folder to view goals and log progress as helper.</p>

      @if (selectedFolderId() == null) {
        @if (loading()) {
          <z-skeleton class="h-24 w-full" />
        } @else if (folders().length === 0) {
          <z-card class="p-8 text-center space-y-2">
            <p class="text-muted-foreground">No medical folder found for patient id above.</p>
            <p class="text-sm text-muted-foreground">
              Confirm the doctor created a folder for that patient, and the patient id matches
              <code class="rounded bg-muted px-1 text-xs">id_patient</code> in the database.
            </p>
          </z-card>
        } @else {
          <div class="grid gap-3">
            @for (f of folders(); track f.id) {
              <button
                type="button"
                (click)="selectedFolderId.set(f.id)"
                class="w-full text-left rounded-xl border border-border bg-card p-4 hover:bg-muted/50 transition-colors"
              >
                <div class="flex items-center justify-between gap-2">
                  <div class="flex items-center gap-3">
                    <z-icon zType="folder" class="h-8 w-8 text-emerald-600" />
                    <div>
                      <p class="font-semibold">Folder #{{ f.id }}</p>
                      <p class="text-sm text-muted-foreground">Doctor: {{ doctorLabel(f.doctorId) }}</p>
                    </div>
                  </div>
                  <z-icon zType="chevron-right" class="h-5 w-5 text-muted-foreground" />
                </div>
              </button>
            }
          </div>
        }
      } @else {
        <button z-button zType="outline" zSize="sm" type="button" (click)="selectedFolderId.set(null)" class="mb-2">
          ← Choose another folder
        </button>
        <app-coaching-panel
          [folderId]="selectedFolderId()!"
          mode="helper"
          [notificationUserIdOverride]="effectivePatientId()" />
      }
    </div>
  `,
})
export class HelperCoachingComponent implements OnInit, OnChanges {
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly userApiService = inject(UserApiService);
  private readonly platformId = inject(PLATFORM_ID);

  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  folders = signal<MedicalFolder[]>([]);
  loading = signal(true);
  selectedFolderId = signal<number | null>(null);
  private doctorNames = signal<Record<string, string>>({});

  /** Bound to input; persisted on Apply */
  patientIdOverrideDraft = '';

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const stored = localStorage.getItem(PATIENT_OVERRIDE_STORAGE_KEY);
        if (stored) {
          this.patientIdOverrideDraft = stored;
        }
      } catch {
        /* ignore */
      }
    }
    this.loadFolders();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['keycloakId'] && !changes['keycloakId'].firstChange && this.keycloakId) {
      this.selectedFolderId.set(null);
      this.loadFolders();
    }
  }

  /** Patient Keycloak subject used for GET /medical-folders/patient/{id} */
  effectivePatientId(): string {
    const trimmed = this.patientIdOverrideDraft.trim();
    return trimmed || this.keycloakId;
  }

  applyPatientOverride(): void {
    const v = this.patientIdOverrideDraft.trim();
    if (isPlatformBrowser(this.platformId)) {
      try {
        if (v) {
          localStorage.setItem(PATIENT_OVERRIDE_STORAGE_KEY, v);
        } else {
          localStorage.removeItem(PATIENT_OVERRIDE_STORAGE_KEY);
        }
      } catch {
        /* ignore */
      }
    }
    this.selectedFolderId.set(null);
    this.loadFolders();
  }

  clearPatientOverride(): void {
    this.patientIdOverrideDraft = '';
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.removeItem(PATIENT_OVERRIDE_STORAGE_KEY);
      } catch {
        /* ignore */
      }
    }
    this.selectedFolderId.set(null);
    this.loadFolders();
  }

  doctorLabel(doctorId: string): string {
    return this.doctorNames()[doctorId] || doctorId;
  }

  private loadFolders(): void {
    const patientId = this.effectivePatientId();
    if (!patientId) {
      this.folders.set([]);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.medicalFolderService.getMedicalFoldersByPatient(patientId).subscribe({
      next: (list) => {
        this.folders.set(list);
        this.loadDoctorNames(list);
        this.loading.set(false);
      },
      error: () => {
        this.folders.set([]);
        this.loading.set(false);
      },
    });
  }

  private loadDoctorNames(folders: MedicalFolder[]): void {
    const ids = [...new Set(folders.map((f) => f.doctorId).filter(Boolean))];
    if (ids.length === 0) {
      this.doctorNames.set({});
      return;
    }
    forkJoin(ids.map((id) => this.userApiService.getUserByKeycloakId(id).pipe(catchError(() => of(null))))).subscribe(
      (users) => {
        const map: Record<string, string> = {};
        ids.forEach((id, i) => {
          const u = users[i];
          map[id] = u ? `${u.firstName} ${u.lastName}`.trim() || id : id;
        });
        this.doctorNames.set(map);
      }
    );
  }
}
