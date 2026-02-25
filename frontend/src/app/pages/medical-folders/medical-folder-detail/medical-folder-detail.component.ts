import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { ZardDialogService } from '@/shared/components/dialog';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { DiagnosticsService, type Diagnostics } from '@/core/services/diagnostics.service';
import { MedicalHistoryService, type MedicalHistory } from '@/core/services/medical-history.service';
import { UserApiService } from '@/core/services/user-api.service';
import { AIReportService, type AIReport, type AIReportPayload } from '@/core/services/ai-report.service';
import { MedicalFolderFormComponent } from '../medical-folder-form/medical-folder-form.component';
import { DiagnosticsFormComponent } from '@/pages/diagnostics/diagnostics-form/diagnostics-form.component';
import { MedicalHistoryFormComponent } from '@/pages/medical-history/medical-history-form/medical-history-form.component';
import { DossierCompareComponent } from '../dossier-compare/dossier-compare.component';

const TABLE_PAGE_SIZE = 5;

@Component({
  selector: 'app-medical-folder-detail',
  standalone: true,
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    MedicalFolderFormComponent,
    DiagnosticsFormComponent,
    MedicalHistoryFormComponent,
    DossierCompareComponent,
  ],
  templateUrl: './medical-folder-detail.component.html',
})
export class MedicalFolderDetailComponent implements OnInit {
  folderId = input.required<number>();
  readOnly = input<boolean>(false);
  back = output<void>();
  updated = output<void>();

  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly medicalHistoryService = inject(MedicalHistoryService);
  private readonly userApiService = inject(UserApiService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);
  private readonly aiReportService = inject(AIReportService);

  folder = signal<{ id: number; patientId: string; doctorId: string; createdAt: string; updatedAt: string } | null>(null);
  patientDisplayName = signal<string>('');
  doctorDisplayName = signal<string>('');
  latestAiReport = signal<AIReport | null>(null);
  loadingAiReport = signal(false);
  generatingReport = signal(false);
  diagnostics = signal<Diagnostics[]>([]);
  history = signal<MedicalHistory[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  readonly tablePageSize = TABLE_PAGE_SIZE;
  currentPageDiagnostics = signal(1);
  currentPageHistory = signal(1);

  totalPagesDiagnostics = computed(() => Math.max(1, Math.ceil(this.diagnostics().length / TABLE_PAGE_SIZE)));
  totalPagesHistory = computed(() => Math.max(1, Math.ceil(this.history().length / TABLE_PAGE_SIZE)));

  paginatedDiagnostics = computed(() => {
    const list = this.diagnostics();
    const page = this.currentPageDiagnostics();
    const start = (page - 1) * TABLE_PAGE_SIZE;
    return list.slice(start, start + TABLE_PAGE_SIZE);
  });

  paginatedHistory = computed(() => {
    const list = this.history();
    const page = this.currentPageHistory();
    const start = (page - 1) * TABLE_PAGE_SIZE;
    return list.slice(start, start + TABLE_PAGE_SIZE);
  });

  /** Timeline events: diagnostics + history entries, sorted by date. */
  timelineEvents = computed(() => {
    const events: { date: string; label: string; category: 'diagnostic' | 'history' }[] = [];
    for (const d of this.diagnostics()) {
      events.push({
        date: d.diagnosisDate,
        label: `${d.diseaseName}${d.stage ? ` (${d.stage})` : ''}`,
        category: 'diagnostic',
      });
    }
    for (const h of this.history()) {
      const parts = [h.allergies, h.conditions, h.surgeries].filter(Boolean);
      events.push({
        date: h.createdAt,
        label: parts.length ? parts.join(' · ') : 'Antécédent enregistré',
        category: 'history',
      });
    }
    events.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    return events;
  });

  /** Group diagnostics by disease name; for each with >1 entry, detect if stage worsened. */
  diseaseProgression = computed(() => {
    const byDisease = new Map<string, Diagnostics[]>();
    for (const d of this.diagnostics()) {
      const key = (d.diseaseName || '').trim() || 'Sans nom';
      if (!byDisease.has(key)) byDisease.set(key, []);
      byDisease.get(key)!.push(d);
    }
    const result: { diseaseName: string; entries: Diagnostics[]; stageWorsened: boolean }[] = [];
    for (const [name, entries] of byDisease) {
      const sorted = [...entries].sort(
        (a, b) => new Date(a.diagnosisDate).getTime() - new Date(b.diagnosisDate).getTime()
      );
      let stageWorsened = false;
      if (sorted.length >= 2) {
        const stages = sorted.map((e) => this.parseStageOrder(e.stage));
        for (let i = 1; i < stages.length; i++) {
          const curr = stages[i];
          const prev = stages[i - 1];
          if (curr != null && prev != null && curr > prev) {
            stageWorsened = true;
            break;
          }
        }
      }
      result.push({ diseaseName: name, entries: sorted, stageWorsened });
    }
    return result.filter((r) => r.entries.length >= 2);
  });

  /** Extract numeric order from stage string (e.g. "Stage 2" -> 2). */
  parseStageOrder(stage: string | undefined): number | null {
    if (!stage?.trim()) return null;
    const m = stage.match(/\d+/);
    return m ? parseInt(m[0], 10) : null;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = this.folderId();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.medicalFolderService.getById(id).subscribe({
      next: (f) => {
        this.folder.set(f);
        this.loadDisplayNames(f);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Folder not found');
      },
      complete: () => this.loading.set(false),
    });
    this.loadLatestAiReport();
    this.diagnosticsService.getByFolder(id).subscribe({
      next: (list) => {
        this.diagnostics.set(list);
        this.currentPageDiagnostics.set(1);
      },
      error: () => this.diagnostics.set([]),
    });
    this.medicalHistoryService.getByFolder(id).subscribe({
      next: (list) => {
        this.history.set(list);
        this.currentPageHistory.set(1);
      },
      error: () => this.history.set([]),
    });
  }

  setPageDiagnostics(p: number): void {
    this.currentPageDiagnostics.set(Math.max(1, Math.min(p, this.totalPagesDiagnostics())));
  }

  setPageHistory(p: number): void {
    this.currentPageHistory.set(Math.max(1, Math.min(p, this.totalPagesHistory())));
  }

  private loadDisplayNames(f: { patientId: string; doctorId: string }): void {
    this.patientDisplayName.set('');
    this.doctorDisplayName.set('');
    forkJoin({
      patient: this.userApiService.getUserByKeycloakId(f.patientId).pipe(
        catchError(() => of(null)),
      ),
      doctor: this.userApiService.getUserByKeycloakId(f.doctorId).pipe(
        catchError(() => of(null)),
      ),
    }).subscribe(({ patient, doctor }) => {
      this.patientDisplayName.set(patient ? `${patient.firstName} ${patient.lastName}`.trim() || f.patientId : f.patientId);
      this.doctorDisplayName.set(doctor ? `${doctor.firstName} ${doctor.lastName}`.trim() || f.doctorId : f.doctorId);
    });
  }

  loadLatestAiReport(): void {
    const id = this.folderId();
    if (!id) return;
    this.loadingAiReport.set(true);
    this.aiReportService.getLatest(id).pipe(
      catchError(() => of(null)),
    ).subscribe({
      next: (report) => {
        this.latestAiReport.set(report);
        this.loadingAiReport.set(false);
        if (report?.status === 'PENDING') this.startPollingAiReport();
      },
      error: () => {
        this.loadingAiReport.set(false);
      },
    });
  }

  private pollInterval: ReturnType<typeof setInterval> | null = null;

  private startPollingAiReport(): void {
    if (this.pollInterval) return;
    const id = this.folderId();
    this.pollInterval = setInterval(() => {
      this.aiReportService.getLatest(id).pipe(catchError(() => of(null))).subscribe((report) => {
        this.latestAiReport.set(report);
        if (report?.status !== 'PENDING') {
          if (this.pollInterval) clearInterval(this.pollInterval);
          this.pollInterval = null;
          this.generatingReport.set(false);
        }
      });
    }, 3000);
  }

  generateAiReport(): void {
    const id = this.folderId();
    if (!id) return;
    this.generatingReport.set(true);
    this.aiReportService.generate(id).subscribe({
      next: (report) => {
        this.latestAiReport.set(report);
        if (report.status === 'PENDING') this.startPollingAiReport();
        else this.generatingReport.set(false);
        this.alertDialog.info({ zTitle: 'AI Report', zContent: 'Report generation started. It will appear when ready.' });
      },
      error: (err) => {
        this.generatingReport.set(false);
        this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Failed to start report generation.' });
      },
    });
  }

  openAiReportModal(): void {
    const report = this.latestAiReport();
    if (!report?.reportJson || report.status !== 'READY') return;
    const payload = report.reportJson as AIReportPayload;
    const risk = payload.riskLevel ?? 'LOW';
    const riskColor = risk === 'HIGH' ? 'text-destructive' : risk === 'MEDIUM' ? 'text-amber-600' : 'text-green-600';
    let content = `<div class="space-y-4 text-left">`;
    content += `<p><strong>Risk level:</strong> <span class="${riskColor} font-semibold">${risk}</span></p>`;
    if (payload.advice) content += `<p><strong>Advice:</strong> ${payload.advice}</p>`;
    if (payload.differentials?.length) {
      content += `<p><strong>Differentials to consider:</strong></p><ul class="list-disc pl-5 space-y-1">${payload.differentials.map((d) => `<li>${d}</li>`).join('')}</ul>`;
    }
    if (payload.anomalies?.length) {
      content += `<p><strong>Anomalies:</strong></p><ul class="list-disc pl-5 space-y-1 text-amber-700">${payload.anomalies.map((a) => `<li>${a}</li>`).join('')}</ul>`;
    }
    if (payload.contradictions?.length) {
      content += `<p><strong>Contradictions:</strong></p><ul class="list-disc pl-5 space-y-1 text-destructive">${payload.contradictions.map((c) => `<li>${c}</li>`).join('')}</ul>`;
    }
    content += `</div>`;
    this.alertDialog.info({ zTitle: '🤖 AI Clinical Report', zContent: content });
  }

  openCompareDiagnostics(): void {
    const list = this.diagnostics();
    if (list.length < 2) {
      this.alertDialog.info({ zTitle: 'Compare', zContent: 'Add at least two diagnostics to compare.' });
      return;
    }
    const ref = this.dialog.create<DossierCompareComponent, unknown>({
      zTitle: 'Compare Diagnostics',
      zContent: DossierCompareComponent,
      zWidth: '640px',
      zHideFooter: true,
    });
    const comp = ref.componentInstance;
    if (comp) {
      comp.mode = 'diagnostics';
      comp.items = list;
    }
  }

  openCompareHistory(): void {
    const list = this.history();
    if (list.length < 2) {
      this.alertDialog.info({ zTitle: 'Compare', zContent: 'Add at least two medical history entries to compare.' });
      return;
    }
    const ref = this.dialog.create<DossierCompareComponent, unknown>({
      zTitle: 'Compare Medical History',
      zContent: DossierCompareComponent,
      zWidth: '640px',
      zHideFooter: true,
    });
    const comp = ref.componentInstance;
    if (comp) {
      comp.mode = 'history';
      comp.items = list;
    }
  }

  goBack(): void {
    this.back.emit();
  }

  openEditFolder(): void {
    const f = this.folder();
    if (!f) return;
    const formRef = this.dialog.create<MedicalFolderFormComponent, unknown>({
      zTitle: 'Edit Medical Folder',
      zContent: MedicalFolderFormComponent,
      zWidth: '420px',
      zHideFooter: true,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.folder = f;
      form.onSubmitCallback = (data) => {
        this.medicalFolderService.update(f.id, data as { patientId?: string; doctorId?: string }).subscribe({
          next: (updated) => {
            this.folder.set(updated);
            formRef.close();
            this.alertDialog.info({ zTitle: 'Success', zContent: 'Folder updated.' });
            this.updated.emit();
          },
          error: (err) => {
            this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' });
          },
        });
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  openAddDiagnostics(): void {
    const id = this.folderId();
    const formRef = this.dialog.create<DiagnosticsFormComponent, { medicalFolderId: number }>({
      zTitle: 'Add Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zDraggable: true,
      zData: { medicalFolderId: id },
    });
    const setCallbacks = () => {
      const form = formRef.componentInstance;
      if (form) {
        form.prefillFolderId = id;
        form.onSubmitCallback = (payload) => {
          if ('id' in payload) {
            this.diagnosticsService.update(payload.id, payload.data).subscribe({
              next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics updated.' }); },
              error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
            });
          } else {
            this.diagnosticsService.create(payload).subscribe({
              next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics added.' }); },
              error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
            });
          }
        };
        form.onCancelCallback = () => formRef.close();
      }
    };
    setCallbacks();
    setTimeout(setCallbacks, 0);
  }

  openAddHistory(): void {
    const id = this.folderId();
    const formRef = this.dialog.create<MedicalHistoryFormComponent, { medicalFolderId: number }>({
      zTitle: 'Add Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zDraggable: true,
      zData: { medicalFolderId: id },
    });
    const setCallbacks = () => {
      const form = formRef.componentInstance;
      if (form) {
        form.prefillFolderId = id;
        form.onSubmitCallback = (payload) => {
          if ('id' in payload) {
            this.medicalHistoryService.update(payload.id, payload.data).subscribe({
              next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history updated.' }); },
              error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
            });
          } else {
            this.medicalHistoryService.create(payload).subscribe({
              next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history added.' }); },
              error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
            });
          }
        };
        form.onCancelCallback = () => formRef.close();
      }
    };
    setCallbacks();
    setTimeout(setCallbacks, 0);
  }

  editDiagnostics(d: Diagnostics): void {
    const formRef = this.dialog.create<DiagnosticsFormComponent, unknown>({
      zTitle: 'Edit Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zDraggable: true,
      zData: { diagnostics: d },
    });
    const form = formRef.componentInstance;
    if (form) {
      form.prefillFolderId = this.folderId();
      form.editModel = d;
      form.onSubmitCallback = (payload) => {
        if ('id' in payload) {
          this.diagnosticsService.update(payload.id, payload.data).subscribe({
            next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics updated.' }); },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
          });
        } else {
          this.diagnosticsService.create(payload).subscribe({
            next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics added.' }); },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
          });
        }
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  deleteDiagnostics(d: Diagnostics): void {
    this.alertDialog
      .confirm({
        zTitle: 'Delete Diagnostics',
        zContent: `Delete "${d.diseaseName}"?`,
        zOkDestructive: true,
        zOkText: 'Delete',
      })
      .afterClosed$.subscribe((ok: boolean) => {
        if (ok) {
          this.diagnosticsService.delete(d.id).subscribe({
            next: () => {
              this.load();
              this.alertDialog.info({ zTitle: 'Deleted', zContent: 'Diagnostics removed.' });
            },
            error: (err) => {
              this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Delete failed.' });
            },
          });
        }
      });
  }

  editHistory(h: MedicalHistory): void {
    const id = this.folderId();
    const formRef = this.dialog.create<MedicalHistoryFormComponent, { medicalFolderId: number }>({
      zTitle: 'Edit Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zDraggable: true,
      zData: { medicalFolderId: id },
    });
    const setCallbacks = () => {
      const form = formRef.componentInstance;
      if (form) {
        form.prefillFolderId = id;
        form.editModel = h;
        form.onSubmitCallback = (payload) => {
          if ('id' in payload) {
            this.medicalHistoryService.update(payload.id, payload.data).subscribe({
              next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history updated.' }); },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
          });
        } else {
          this.medicalHistoryService.create(payload).subscribe({
            next: () => { formRef.close(); this.load(); this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history added.' }); },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
          });
        }
      };
      form.onCancelCallback = () => formRef.close();
      }
    };
    setCallbacks();
    setTimeout(setCallbacks, 0);
  }

  deleteHistory(h: MedicalHistory): void {
    this.alertDialog
      .confirm({
        zTitle: 'Delete Medical History',
        zContent: 'Delete this medical history entry?',
        zOkDestructive: true,
        zOkText: 'Delete',
      })
      .afterClosed$.subscribe((ok: boolean) => {
        if (ok) {
          this.medicalHistoryService.delete(h.id).subscribe({
            next: () => {
              this.load();
              this.alertDialog.info({ zTitle: 'Deleted', zContent: 'Medical history removed.' });
            },
            error: (err) => {
              this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Delete failed.' });
            },
          });
        }
      });
  }
}
