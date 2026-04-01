import {
  Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed,
  DestroyRef, inject, output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog/alert-dialog.service';
import { ZardInputDirective } from '@/shared/components/input';

import { UserInfo } from '@/core/services/user-api.service';
import { SessionService, SessionResponseDTO, SessionRequestDTO } from '@/core/services/session.service';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';

@Component({
  selector: 'app-session-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    ZardInputDirective,
  ],
  template: `
    <!-- ═══ SESSION MANAGEMENT ═══ -->

    <!-- Loading state -->
    @if (isLoadingFolder()) {
      <z-card class="p-8">
        <div class="flex flex-col items-center gap-3">
          <z-icon zType="loader-2" class="w-8 h-8 animate-spin text-primary" />
          <p class="text-muted-foreground">Loading medical folder…</p>
        </div>
      </z-card>
    }

    <!-- No medical folder found -->
    @else if (!medicalFolder() && !isLoadingFolder()) {
      <z-card class="p-8">
        <div class="text-center space-y-3">
          <z-icon zType="folder" class="w-12 h-12 text-muted-foreground mx-auto opacity-50" />
          <h3 class="text-lg font-semibold">No Medical Folder</h3>
          <p class="text-muted-foreground text-sm max-w-md mx-auto">
            This patient does not have a medical folder assigned to you yet.
            Create one from the Medical Folders page first.
          </p>
          <button z-button (click)="goToMedicalFolders.emit()">
            <z-icon zType="folder" class="mr-2" />
            Go to Medical Folders
          </button>
        </div>
      </z-card>
    }

    <!-- Main content -->
    @else {
      <div class="space-y-6">

        <!-- Header + Create button -->
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div>
            <h2 class="text-2xl font-bold">Consultation Sessions</h2>
            <p class="text-sm text-muted-foreground">
              {{ patient.firstName }} {{ patient.lastName }} — Folder #{{ medicalFolder()?.id }}
            </p>
          </div>
          <button z-button (click)="openCreateForm()">
            <z-icon zType="plus" class="mr-2" />
            New Session
          </button>
        </div>

        <!-- ─── CREATE / EDIT SESSION FORM ─── -->
        @if (showForm()) {
          <z-card class="overflow-hidden">
            <div class="bg-gradient-to-r from-primary/10 to-primary/5 px-6 py-4 border-b border-border/50">
              <h3 class="font-semibold text-lg flex items-center gap-2">
                <z-icon zType="plus-circle" class="h-5 w-5 text-primary" />
                {{ editingSession() ? 'Edit' : 'New' }} Consultation Session
              </h3>
            </div>
            <div class="p-6 space-y-5">
              <!-- Date -->
              <div>
                <label class="block text-sm font-medium mb-1.5">
                  Session Date <span class="text-destructive">*</span>
                </label>
                <input
                  z-input
                  type="date"
                  class="w-full"
                  [(ngModel)]="formDate"
                  [max]="todayString"
                />
                @if (formSubmitted() && !formDate) {
                  <p class="text-destructive text-xs mt-1">Session date is required</p>
                }
              </div>

              <!-- Notes -->
              <div>
                <label class="block text-sm font-medium mb-1.5">
                  Session Notes <span class="text-destructive">*</span>
                </label>
                <textarea
                  class="w-full min-h-[120px] rounded-lg border border-border bg-background px-3 py-2 text-sm
                         focus:outline-none focus:ring-2 focus:ring-ring transition-colors resize-y"
                  placeholder="Describe the consultation — symptoms observed, patient state, key observations…"
                  [(ngModel)]="formNotes"
                ></textarea>
                @if (formSubmitted() && !formNotes.trim()) {
                  <p class="text-destructive text-xs mt-1">Session notes are required</p>
                }
              </div>

              <!-- Error -->
              @if (formError()) {
                <div class="rounded-lg bg-destructive/10 border border-destructive/30 p-3 text-sm text-destructive">
                  {{ formError() }}
                </div>
              }

              <!-- Actions -->
              <div class="flex items-center justify-end gap-3 pt-2">
                <button z-button zType="outline" (click)="cancelForm()">Cancel</button>
                <button z-button [disabled]="isSubmitting()" (click)="submitForm()">
                  @if (isSubmitting()) {
                    <z-icon zType="loader-2" class="mr-2 h-4 w-4 animate-spin" />
                    Saving…
                  } @else {
                    <z-icon zType="check" class="mr-2" />
                    {{ editingSession() ? 'Update Session' : 'Create Session' }}
                  }
                </button>
              </div>
            </div>
          </z-card>
        }

        <!-- ─── SESSIONS LIST ─── -->
        @if (isLoadingSessions()) {
          <div class="space-y-3">
            <z-skeleton class="h-24 w-full rounded-xl" />
            <z-skeleton class="h-24 w-full rounded-xl" />
          </div>
        } @else if (sessions().length === 0) {
          <z-card class="p-12 text-center">
            <z-icon zType="calendar" class="w-12 h-12 mx-auto mb-4 text-muted-foreground opacity-40" />
            <h3 class="text-lg font-semibold mb-2">No Sessions Yet</h3>
            <p class="text-muted-foreground text-sm mb-4">
              Create a consultation session to start documenting visits, then create prescriptions and care plans.
            </p>
            @if (!showForm()) {
              <button z-button (click)="openCreateForm()">
                <z-icon zType="plus" class="mr-2" />
                Create First Session
              </button>
            }
          </z-card>
        } @else {
          <div class="space-y-3">
            @for (session of sessions(); track session.id) {
              <z-card class="overflow-hidden hover:shadow-md transition-shadow">
                <div class="flex items-start gap-4 p-5">
                  <!-- Date badge -->
                  <div class="shrink-0 w-16 h-16 rounded-xl bg-primary/10 flex flex-col items-center justify-center text-primary">
                    <span class="text-lg font-bold leading-none">{{ getDay(session.sessionDate) }}</span>
                    <span class="text-[10px] uppercase font-semibold mt-0.5">{{ getMonth(session.sessionDate) }}</span>
                    <span class="text-[10px] text-muted-foreground">{{ getYear(session.sessionDate) }}</span>
                  </div>

                  <!-- Content -->
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <h4 class="font-semibold text-sm">Consultation Session</h4>
                      <span class="text-[10px] px-2 py-0.5 rounded-full bg-muted font-medium text-muted-foreground">
                        #{{ session.id }}
                      </span>
                    </div>
                    <p class="text-sm text-muted-foreground line-clamp-2">{{ session.notes || 'No notes recorded' }}</p>
                    <p class="text-xs text-muted-foreground mt-2">
                      Created {{ formatDateTime(session.createdAt) }}
                    </p>
                  </div>

                  <!-- Actions -->
                  <div class="shrink-0 flex items-center gap-1">
                    <button z-button zType="ghost" zSize="sm"
                      class="text-primary"
                      title="Create Prescription from this session"
                      (click)="createPrescriptionFrom.emit(session)">
                      <z-icon zType="file-text" class="h-4 w-4" />
                    </button>
                    <button z-button zType="ghost" zSize="sm"
                      class="text-green-600"
                      title="Create Care Plan from this session"
                      (click)="createCarePlanFrom.emit(session)">
                      <z-icon zType="activity" class="h-4 w-4" />
                    </button>
                    <button z-button zType="ghost" zSize="sm"
                      title="Edit session"
                      (click)="openEditForm(session)">
                      <z-icon zType="edit" class="h-4 w-4" />
                    </button>
                    <button z-button zType="ghost" zSize="sm"
                      class="text-destructive"
                      title="Delete session"
                      (click)="deleteSession(session)">
                      <z-icon zType="trash-2" class="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </z-card>
            }
          </div>
        }

      </div>
    }
  `,
})
export class SessionManagementComponent implements OnInit, OnChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly sessionService = inject(SessionService);
  private readonly medicalFolderService = inject(MedicalFolderService);

  @Input({ required: true }) patient!: UserInfo;
  @Input() doctor: UserInfo | null = null;

  /** Emitted when user wants to go to Medical Folders page */
  goToMedicalFolders = output<void>();
  /** Emitted when user wants to create a prescription from a session */
  createPrescriptionFrom = output<SessionResponseDTO>();
  /** Emitted when user wants to create a care plan from a session */
  createCarePlanFrom = output<SessionResponseDTO>();
  /** Emitted when session list changes (created/deleted) so parent can refresh */
  sessionsChanged = output<void>();

  // State
  medicalFolder = signal<MedicalFolder | null>(null);
  sessions = signal<SessionResponseDTO[]>([]);
  isLoadingFolder = signal(false);
  isLoadingSessions = signal(false);
  isSubmitting = signal(false);
  showForm = signal(false);
  formSubmitted = signal(false);
  formError = signal<string | null>(null);
  editingSession = signal<SessionResponseDTO | null>(null);

  // Form fields (two-way bound with ngModel)
  formDate = '';
  formNotes = '';
  todayString = new Date().toISOString().split('T')[0];

  ngOnInit(): void {
    this.loadFolder();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patient'] && !changes['patient'].firstChange) {
      this.loadFolder();
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // DATA LOADING
  // ══════════════════════════════════════════════════════════════════════════

  loadFolder(): void {
    if (!this.patient?.keycloakId || !this.doctor?.keycloakId) return;

    this.isLoadingFolder.set(true);
    const patientDbId = this.patient.keycloakId;
    const doctorDbId = this.doctor.keycloakId;

    this.medicalFolderService.getMedicalFoldersByPatient(patientDbId)
      .pipe(
        tap(folders => {
          const match = folders.find(f => f.doctorId === doctorDbId);
          this.medicalFolder.set(match ?? null);
          if (match) {
            this.loadSessions(match.id);
          }
        }),
        catchError(() => {
          this.medicalFolder.set(null);
          return of([]);
        }),
        finalize(() => this.isLoadingFolder.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  loadSessions(folderId: number): void {
    this.isLoadingSessions.set(true);
    this.sessionService.getSessionsByMedicalFolder(folderId)
      .pipe(
        tap(sessions => {
          // Sort newest first
          sessions.sort((a, b) =>
            new Date(b.sessionDate).getTime() - new Date(a.sessionDate).getTime()
          );
          this.sessions.set(sessions);
        }),
        catchError(() => {
          this.sessions.set([]);
          return of([]);
        }),
        finalize(() => this.isLoadingSessions.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // FORM
  // ══════════════════════════════════════════════════════════════════════════

  openCreateForm(): void {
    this.editingSession.set(null);
    this.formDate = this.todayString;
    this.formNotes = '';
    this.formSubmitted.set(false);
    this.formError.set(null);
    this.showForm.set(true);
  }

  openEditForm(session: SessionResponseDTO): void {
    this.editingSession.set(session);
    this.formDate = session.sessionDate;
    this.formNotes = session.notes || '';
    this.formSubmitted.set(false);
    this.formError.set(null);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingSession.set(null);
    this.formError.set(null);
  }

  submitForm(): void {
    this.formSubmitted.set(true);

    if (!this.formDate || !this.formNotes.trim()) {
      this.formError.set('Please fill in all required fields.');
      return;
    }

    const folder = this.medicalFolder();
    if (!folder) {
      this.formError.set('No medical folder found for this patient.');
      return;
    }

    this.isSubmitting.set(true);
    this.formError.set(null);

    // Backend requires LocalDateTime format (YYYY-MM-DDTHH:mm:ss)
    const dateTime = this.formDate.includes('T') ? this.formDate : `${this.formDate}T00:00:00`;

    const request: SessionRequestDTO = {
      medicalFolderId: folder.id,
      sessionDate: dateTime,
      notes: this.formNotes.trim(),
    };

    const editing = this.editingSession();

    // The backend only has createSession; for edits we'd need an update endpoint.
    // For now, create only. If the backend adds update, we can wire it here.
    this.sessionService.createSession(request)
      .pipe(
        tap(created => {
          this.showForm.set(false);
          this.editingSession.set(null);
          this.loadSessions(folder.id);
          this.sessionsChanged.emit();
          this.alertDialog.info({
            zTitle: 'Session Created',
            zDescription: `Consultation session for ${this.formDate} has been created. You can now create prescriptions or care plans from this session.`,
            zOkText: 'OK',
          });
        }),
        catchError(error => {
          const msg = error?.error?.message || error?.error?.error || 'Failed to create session. Please try again.';
          this.formError.set(msg);
          return of(null);
        }),
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // DELETE
  // ══════════════════════════════════════════════════════════════════════════

  deleteSession(session: SessionResponseDTO): void {
    this.alertDialog.confirm({
      zTitle: 'Delete Session',
      zDescription: `Delete the consultation session from ${session.sessionDate}? Any prescriptions or care plans linked to this session may be affected.`,
      zOkText: 'Delete',
      zCancelText: 'Cancel',
      zOkDestructive: true,
      zOnOk: () => {
        // The backend SessionService doesn't have a delete endpoint yet.
        // For now, inform the user. If the backend adds it, we wire it here.
        this.alertDialog.info({
          zTitle: 'Not Supported',
          zDescription: 'Session deletion is not supported by the backend yet. Contact your administrator.',
          zOkText: 'OK',
        });
      },
    });
  }

  // ══════════════════════════════════════════════════════════════════════════
  // DATE HELPERS
  // ══════════════════════════════════════════════════════════════════════════

  getDay(dateStr: string): string {
    try { return new Date(dateStr).getDate().toString().padStart(2, '0'); } catch { return '--'; }
  }

  getMonth(dateStr: string): string {
    try { return new Date(dateStr).toLocaleString('en', { month: 'short' }); } catch { return '---'; }
  }

  getYear(dateStr: string): string {
    try { return new Date(dateStr).getFullYear().toString(); } catch { return '----'; }
  }

  formatDateTime(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
      });
    } catch { return dateStr; }
  }
}
