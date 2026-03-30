import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Meeting,
  MeetingService,
  CreateMeetingRequest,
} from '@/core/services/meeting.service';
import { UserApiService, UserInfo } from '@/core/services/user-api.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-meeting-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="space-y-6">
      <!-- HEADER -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold">Reunions video</h2>
          <p class="text-muted-foreground text-sm mt-1">
            Visioconferences avec les aidants de vos patients
          </p>
        </div>
        <button z-button (click)="showCreateForm = !showCreateForm">
          <z-icon zType="plus" class="mr-1 h-4 w-4" />
          Nouvelle reunion
        </button>
      </div>

      <!-- CREATE FORM -->
      @if (showCreateForm) {
        <div class="bg-card border border-border rounded-xl p-5 space-y-4">
          <h3 class="font-semibold text-sm">Planifier une reunion</h3>

          <div>
            <label class="block text-muted-foreground text-xs mb-1.5">Patient concerne</label>
            <select
              [(ngModel)]="selectedPatientId"
              class="w-full bg-muted text-foreground text-sm rounded-lg px-3 py-2.5 border border-border focus:border-primary focus:outline-none"
            >
              <option value="">-- Selectionner un patient --</option>
              @for (p of patients; track p.keycloakId) {
                <option [value]="p.keycloakId">{{ p.firstName }} {{ p.lastName }}</option>
              }
            </select>
          </div>

          <div>
            <label class="block text-muted-foreground text-xs mb-1.5">
              Date et heure (optionnel)
            </label>
            <input
              type="datetime-local"
              [(ngModel)]="scheduledAt"
              class="w-full bg-muted text-foreground text-sm rounded-lg px-3 py-2.5 border border-border focus:border-primary focus:outline-none"
            />
          </div>

          <div class="flex items-center gap-3 pt-1">
            <button
              z-button
              [disabled]="!selectedPatientId || creating"
              (click)="createMeeting()"
            >
              @if (creating) {
                Creation...
              } @else {
                Creer la reunion
              }
            </button>
            <button z-button zType="outline" (click)="showCreateForm = false">
              Annuler
            </button>
            @if (createError) {
              <span class="text-destructive text-xs">{{ createError }}</span>
            }
          </div>
        </div>
      }

      <!-- LIST -->
      @if (loading) {
        <div class="flex justify-center py-12">
          <div class="w-8 h-8 border-2 border-muted border-t-primary rounded-full animate-spin"></div>
        </div>
      } @else if (meetings.length === 0) {
        <div class="bg-card border border-border rounded-xl p-10 text-center">
          <z-icon zType="video" class="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
          <p class="text-muted-foreground text-sm font-medium">
            Aucune reunion pour le moment.
          </p>
          <p class="text-muted-foreground/60 text-xs mt-1.5">
            Creez une reunion video pour commencer.
          </p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (m of meetings; track m.id) {
            <div class="bg-card border border-border rounded-xl p-4 hover:border-primary/30 transition">
              <div class="flex items-center justify-between">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="font-medium text-sm truncate">
                      {{ m.patientName }}
                    </span>
                    <span
                      class="text-xs font-medium px-2 py-0.5 rounded-full"
                      [ngClass]="getStatusBadgeClass(m.status)"
                    >
                      {{ getStatusLabel(m.status) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-4 text-xs text-muted-foreground">
                    <span>{{ m.createdAt | date : 'dd/MM/yyyy HH:mm' }}</span>
                    @if (m.durationMinutes) {
                      <span>{{ m.durationMinutes }} min</span>
                    }
                    <span class="opacity-40 font-mono">{{ m.roomName }}</span>
                  </div>
                </div>

                <div class="flex items-center gap-2 ml-3">
                  @if (m.status === 'SCHEDULED' || m.status === 'ACTIVE') {
                    <button z-button zSize="sm" (click)="openMeeting.emit(m)">
                      Rejoindre
                    </button>
                  }
                  @if (m.status === 'ENDED') {
                    <button z-button zType="outline" zSize="sm" (click)="toggleSummary(m.id)">
                      @if (expandedSummaryId === m.id) {
                        Masquer resume
                      } @else {
                        Voir resume AI
                      }
                    </button>
                  }
                </div>
              </div>

              @if (expandedSummaryId === m.id && m.aiSummary) {
                <div
                  class="mt-3 pt-3 border-t border-border text-sm leading-relaxed bg-muted/30 rounded-lg p-4"
                  style="white-space: pre-wrap"
                >
                  {{ m.aiSummary }}
                </div>
              }
              @if (expandedSummaryId === m.id && !m.aiSummary) {
                <div class="mt-3 pt-3 border-t border-border text-xs text-muted-foreground italic">
                  Aucun resume AI disponible pour cette reunion.
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class MeetingListComponent implements OnInit {
  @Input() doctorKeycloakId!: string;
  @Output() openMeeting = new EventEmitter<Meeting>();

  private meetingService = inject(MeetingService);
  private userApiService = inject(UserApiService);

  meetings: Meeting[] = [];
  patients: UserInfo[] = [];
  loading = true;

  showCreateForm = false;
  selectedPatientId = '';
  scheduledAt = '';
  creating = false;
  createError = '';

  expandedSummaryId: number | null = null;

  ngOnInit(): void {
    this.loadMeetings();
    this.loadPatients();
  }

  loadMeetings(): void {
    this.loading = true;
    this.meetingService.getMeetingsForDoctor(this.doctorKeycloakId).subscribe({
      next: (meetings) => {
        this.meetings = meetings;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  loadPatients(): void {
    this.userApiService.getUsersByRole('patient').subscribe({
      next: (patients) => {
        this.patients = patients;
      },
      error: (err) => {
        console.warn('Could not load patients:', err);
        this.patients = [];
      },
    });
  }

  createMeeting(): void {
    if (!this.selectedPatientId || this.creating) return;

    this.creating = true;
    this.createError = '';

    const request: CreateMeetingRequest = {
      patientKeycloakId: this.selectedPatientId,
      doctorKeycloakId: this.doctorKeycloakId,
      scheduledAt: this.scheduledAt || undefined,
    };

    this.meetingService.createMeeting(request).subscribe({
      next: (meeting) => {
        this.creating = false;
        this.showCreateForm = false;
        this.selectedPatientId = '';
        this.scheduledAt = '';
        this.meetings.unshift(meeting);
        this.openMeeting.emit(meeting);
      },
      error: (err) => {
        this.creating = false;
        this.createError = 'Erreur lors de la creation de la reunion.';
        console.error('Create meeting error:', err);
      },
    });
  }

  toggleSummary(id: number): void {
    this.expandedSummaryId = this.expandedSummaryId === id ? null : id;
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Planifiee';
      case 'ACTIVE': return 'En cours';
      case 'ENDED': return 'Terminee';
      default: return status;
    }
  }

  getStatusBadgeClass(status: string): Record<string, boolean> {
    return {
      'bg-muted text-muted-foreground': status === 'SCHEDULED',
      'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-300': status === 'ACTIVE',
      'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300': status === 'ENDED',
    };
  }
}
