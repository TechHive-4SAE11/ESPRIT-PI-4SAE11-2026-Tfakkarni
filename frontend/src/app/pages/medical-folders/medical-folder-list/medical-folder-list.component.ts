import { Component, computed, inject, input, OnInit, output, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { catchError, finalize, of } from 'rxjs';
import { forkJoin } from 'rxjs';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { ZardDialogService } from '@/shared/components/dialog';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';
import { DiagnosticsService } from '@/core/services/diagnostics.service';
import { MedicalHistoryService } from '@/core/services/medical-history.service';
import { MedicalFolderPdfService } from '@/core/services/medical-folder-pdf.service';
import { UserApiService } from '@/core/services/user-api.service';
import type { UserInfo } from '@/core/services/user-api.service';
// @ts-ignore - used for dynamic component instantiation
import { MedicalFolderFormComponent, type MedicalFolderDialogData } from '../medical-folder-form/medical-folder-form.component';
import { MedicalFolderDetailComponent } from '../medical-folder-detail/medical-folder-detail.component';

const PAGE_SIZE = 10;

/** Survives F5: reopen same dossier detail after refresh (doctor medical folders). */
const OPEN_FOLDER_SESSION_KEY = 'tfk_medical_folder_detail_open_id';

@Component({
  selector: 'app-medical-folder-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    MedicalFolderFormComponent,
    MedicalFolderDetailComponent,
  ],
  templateUrl: './medical-folder-list.component.html',
  styles: [
    `.medical-folders-page :host { display: block; }`,
    `.medical-folders-search:focus { border-color: hsl(var(--ring)); }`,
    `.medical-folders-action-btn { color: hsl(var(--muted-foreground)); }`,
    `.medical-folders-action-btn:hover { color: hsl(var(--primary)); background: hsl(var(--primary) / 0.1); }`,
    `.medical-folders-table th { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid hsl(var(--border)); white-space: nowrap; }`,
    `.medical-folders-table td { padding: 0.75rem 1rem; border-bottom: 1px solid hsl(var(--border) / 0.6); vertical-align: middle; }`,
    `.medical-folders-table tbody tr:last-child td { border-bottom: none; }`,
    `.medical-folders-row-alt { background: hsl(var(--muted) / 0.3); }`,
    `.medical-folders-row-btn { border: none; cursor: pointer; background: transparent; }`,
    `.medical-folders-row-btn:hover { opacity: 0.9; }`,
    `.medical-folders-row-btn-pdf { color: hsl(142 76% 36%); }`,
    `.medical-folders-row-btn-pdf:hover { background: hsl(142 76% 36% / 0.1); }`,
    `.medical-folders-row-btn-view { color: hsl(var(--primary)); }`,
    `.medical-folders-row-btn-view:hover { background: hsl(var(--primary) / 0.1); }`,
    `.medical-folders-row-btn-edit { color: hsl(217 91% 60%); }`,
    `.medical-folders-row-btn-edit:hover { background: hsl(217 91% 60% / 0.1); }`,
    `.medical-folders-row-btn-delete { color: hsl(25 95% 53%); }`,
    `.medical-folders-row-btn-delete:hover { background: hsl(25 95% 53% / 0.1); }`,
    `.medical-folders-filter-btn { padding: 0.375rem 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; border: 1px solid hsl(var(--border)); background: hsl(var(--background)); color: hsl(var(--foreground)); cursor: pointer; transition: all 0.15s; }`,
    `.medical-folders-filter-btn:hover { background: hsl(var(--muted)); }`,
    `.medical-folders-filter-btn-active { background: hsl(var(--primary)); color: hsl(var(--primary-foreground)); border-color: hsl(var(--primary)); }`,
  ],
})
export class MedicalFolderListComponent implements OnInit {
  /** Exposed for template (Items per page) */
  get pageSize(): number {
    return PAGE_SIZE;
  }
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly medicalHistoryService = inject(MedicalHistoryService);
  private readonly pdfService = inject(MedicalFolderPdfService);
  private readonly userApiService = inject(UserApiService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);
  private readonly platformId = inject(PLATFORM_ID);

  folders = signal<MedicalFolder[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  dataLoaded = signal(false);
  searchTerm = signal('');
  currentPage = signal(1);
  totalPagesFromServer = signal(1);
  totalElementsFromServer = signal(0);
  folderToView = signal<MedicalFolder | null>(null);
  showDetail = signal(false);
  patientNameMap = signal<Record<string, string>>({});
  stats = signal<{ total: number; thisMonth: number; thisWeek: number; patientCount: number } | null>(null);

  initialFolderId = input<number | null>(null);
  doctorId = input<string | null>(null);
  doctor = input<UserInfo | null>(null);
  detailClosed = output<void>();

  sortOrder = signal<'name-asc' | 'name-desc' | 'date-desc' | 'date-asc'>('date-desc');

  /** Current page content (server-side pagination). */
  paginatedFolders = computed(() => this.folders());

  totalPages = computed(() => Math.max(1, this.totalPagesFromServer()));

  statTotal = computed(() => this.stats()?.total ?? 0);
  statThisMonth = computed(() => this.stats()?.thisMonth ?? 0);
  statThisWeek = computed(() => this.stats()?.thisWeek ?? 0);
  statPatients = computed(() => this.stats()?.patientCount ?? 0);

  private toSortParam(): string {
    const order = this.sortOrder();
    if (order === 'name-asc') return 'patientId,asc';
    if (order === 'name-desc') return 'patientId,desc';
    if (order === 'date-asc') return 'createdAt,asc';
    return 'createdAt,desc';
  }

  listContentMode = computed(() => {
    if (this.showDetail() && this.folderToView()) return 'detail';
    if (this.folders().length === 0 && this.dataLoaded()) return 'empty';
    return 'table';
  });

  ngOnInit(): void {
    this.loadPatientNames();
    this.loadFolders();
    const fromParent = this.initialFolderId();
    const fromSession = fromParent == null ? this.readOpenFolderIdFromSession() : null;
    const idToOpen = fromParent ?? fromSession;
    if (idToOpen != null) {
      this.medicalFolderService.getById(idToOpen).subscribe({
        next: (folder) => {
          this.folderToView.set(folder);
          this.showDetail.set(true);
          this.persistOpenFolderId(folder.id);
        },
        error: () => this.persistOpenFolderId(null),
      });
    }
  }

  private persistOpenFolderId(id: number | null): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      if (id != null) {
        sessionStorage.setItem(OPEN_FOLDER_SESSION_KEY, String(id));
      } else {
        sessionStorage.removeItem(OPEN_FOLDER_SESSION_KEY);
      }
    } catch {
      /* ignore quota / private mode */
    }
  }

  private readOpenFolderIdFromSession(): number | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const raw = sessionStorage.getItem(OPEN_FOLDER_SESSION_KEY);
      if (raw == null || raw === '') return null;
      const n = Number.parseInt(raw, 10);
      return Number.isFinite(n) && n > 0 ? n : null;
    } catch {
      return null;
    }
  }

  loadFolders(): void {
    this.loading.set(true);
    this.error.set(null);

    const page = this.currentPage() - 1;
    const search = this.searchTerm().trim() || undefined;
    const sort = this.toSortParam();

    forkJoin({
      page: this.medicalFolderService.getPage({
        page,
        size: PAGE_SIZE,
        sort,
        search,
      }).pipe(
        catchError((err) => {
          this.error.set(err?.error?.message || 'Failed to load medical folders');
          return of({
            content: [],
            totalElements: 0,
            totalPages: 1,
            size: PAGE_SIZE,
            number: 0,
            first: true,
            last: true,
            numberOfElements: 0,
          });
        })
      ),
      stats: this.medicalFolderService.getStats().pipe(
        catchError(() => of({ total: 0, thisMonth: 0, thisWeek: 0, patientCount: 0 }))
      ),
    }).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: ({ page: p, stats: s }) => {
        this.folders.set(p.content ?? []);
        this.totalPagesFromServer.set(Math.max(1, p.totalPages ?? 1));
        this.totalElementsFromServer.set(p.totalElements ?? 0);
        this.stats.set(s);
        this.dataLoaded.set(true);
        this.error.set(null);
      },
    });
  }

  private loadPatientNames(): void {
    this.userApiService.getUsersByRole('patient').subscribe({
      next: (patients) => {
        const map: Record<string, string> = {};
        for (const patient of patients) {
          map[patient.keycloakId] = `${patient.firstName} ${patient.lastName}`.trim();
        }
        this.patientNameMap.set(map);
      },
      error: () => {
        this.patientNameMap.set({});
      },
    });
  }

  getPatientName(patientId: string): string {
    return this.patientNameMap()[patientId] || 'Unknown Patient';
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(1);
    this.loadFolders();
  }

  setSortOrder(order: 'name-asc' | 'name-desc' | 'date-desc' | 'date-asc'): void {
    this.sortOrder.set(order);
    this.currentPage.set(1);
    this.loadFolders();
  }

  setPage(p: number): void {
    this.currentPage.set(Math.max(1, Math.min(p, this.totalPages())));
    this.loadFolders();
  }

  openCreate(): void {
    const zData: MedicalFolderDialogData = { callbacks: {} };
    const formRef = this.dialog.create<MedicalFolderFormComponent, unknown>({
      zTitle: 'Create Medical Folder',
      zContent: MedicalFolderFormComponent,
      zWidth: '420px',
      zHideFooter: true,
      zDraggable: true,
      zData,
    });

    zData.callbacks!.onSubmit = (data) => {
      this.medicalFolderService.create(data as { patientId: string }).subscribe({
        next: () => {
          formRef.componentInstance?.isSubmitting.set(false);
          formRef.close();
          setTimeout(() => this.loadFolders(), 0);
          this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical folder created.' });
        },
        error: (err) => {
          formRef.componentInstance?.isSubmitting.set(false);
          this.alertDialog.warning({
            zTitle: 'Error',
            zContent: err?.error?.message || 'Failed to create folder.',
          });
        },
      });
    };
    zData.callbacks!.onCancel = () => formRef.close();
    const doctor = this.doctor();
    const form = formRef.componentInstance;
    if (form) form.doctor = doctor;
    setTimeout(() => { const f = formRef.componentInstance; if (f) f.doctor = doctor; }, 0);
  }

  openEdit(folder: MedicalFolder): void {
    const zData: MedicalFolderDialogData = { callbacks: {} };
    const formRef = this.dialog.create<MedicalFolderFormComponent, unknown>({
      zTitle: 'Edit Medical Folder',
      zContent: MedicalFolderFormComponent,
      zWidth: '420px',
      zHideFooter: true,
      zDraggable: true,
      zData,
    });
    zData.callbacks!.onSubmit = (data) => {
      this.medicalFolderService.update(folder.id, data as { patientId?: string; doctorId?: string }).subscribe({
        next: () => {
          formRef.componentInstance?.isSubmitting.set(false);
          formRef.close();
          setTimeout(() => this.loadFolders(), 0);
          this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical folder updated.' });
        },
        error: (err) => {
          formRef.componentInstance?.isSubmitting.set(false);
          this.alertDialog.warning({
            zTitle: 'Error',
            zContent: err?.error?.message || 'Failed to update folder.',
          });
        },
      });
    };
    zData.callbacks!.onCancel = () => formRef.close();
    const doctor = this.doctor();
    const form = formRef.componentInstance;
    if (form) {
      form.doctor = doctor;
      form.folder = folder;
    }
    setTimeout(() => {
      const f = formRef.componentInstance;
      if (f) { f.doctor = doctor; f.folder = folder; }
    }, 0);
  }

  viewDetail(folder: MedicalFolder): void {
    this.folderToView.set(folder);
    this.showDetail.set(true);
    this.persistOpenFolderId(folder.id);
  }

  closeDetail(): void {
    this.showDetail.set(false);
    this.folderToView.set(null);
    this.persistOpenFolderId(null);
    this.detailClosed.emit();
    this.loadFolders();
  }

  exportFolderPdf(folder: MedicalFolder): void {
    forkJoin({
      diagnostics: this.diagnosticsService.getByFolder(folder.id).pipe(catchError(() => of([]))),
      history: this.medicalHistoryService.getByFolder(folder.id).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ diagnostics, history }) => {
        const blob = this.pdfService.exportDossier(
          folder,
          diagnostics,
          history,
          this.getPatientName(folder.patientId)
        );
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dossier-medical-${folder.patientId}-${folder.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.alertDialog.info({
          zTitle: 'Export PDF',
          zContent: 'Dossier médical téléchargé.',
        });
      },
      error: (err) => {
        this.alertDialog.warning({
          zTitle: 'Erreur',
          zContent: err?.error?.message || 'Impossible de générer le PDF.',
        });
      },
    });
  }

  deleteFolder(folder: MedicalFolder): void {
    this.alertDialog
      .confirm({
        zTitle: 'Delete Medical Folder',
        zContent: `Delete folder for patient ${folder.patientId}? This cannot be undone.`,
        zOkDestructive: true,
        zOkText: 'Delete',
      })
      .afterClosed$.subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.medicalFolderService.delete(folder.id).subscribe({
            next: () => {
              setTimeout(() => this.loadFolders(), 0);
              this.alertDialog.info({ zTitle: 'Deleted', zContent: 'Medical folder deleted.' });
            },
            error: (err) => {
              this.alertDialog.warning({
                zTitle: 'Error',
                zContent: err?.error?.message || 'Failed to delete folder.',
              });
            },
          });
        }
      });
  }
}
