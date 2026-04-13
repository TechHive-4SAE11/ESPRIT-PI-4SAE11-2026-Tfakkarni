import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { ZardDialogService } from '@/shared/components/dialog';
import { DiagnosticsService, type Diagnostics } from '@/core/services/diagnostics.service';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';
import { DiagnosticsFormComponent } from '../diagnostics-form/diagnostics-form.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-diagnostics-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    // @ts-ignore - used for dynamic component instantiation in dialog.create()
    DiagnosticsFormComponent,
  ],
  templateUrl: './diagnostics-list.component.html',
})
export class DiagnosticsListComponent implements OnInit {
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);

  folders = signal<MedicalFolder[]>([]);
  diagnostics = signal<Diagnostics[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  folderFilterId = signal<number | null>(null);
  searchDisease = signal('');
  currentPage = signal(1);

  filteredDiagnostics = computed(() => {
    let list = this.diagnostics();
    const folderId = this.folderFilterId();
    if (folderId != null) {
      list = list.filter((d) => d.medicalFolderId === folderId);
    }
    const term = this.searchDisease().toLowerCase().trim();
    if (term) {
      list = list.filter((d) => d.diseaseName?.toLowerCase().includes(term));
    }
    return list;
  });

  paginated = computed(() => {
    const list = this.filteredDiagnostics();
    const page = this.currentPage();
    const start = (page - 1) * PAGE_SIZE;
    return list.slice(start, start + PAGE_SIZE);
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredDiagnostics().length / PAGE_SIZE)));

  getFolderById(id: number): MedicalFolder | undefined {
    return this.folders().find((f) => f.id === id);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.medicalFolderService
      .getAll()
      .pipe(
        catchError(() => of([])),
        switchMap((folders) => {
          this.folders.set(folders);
          if (folders.length === 0) return of([]);
          return forkJoin(
            folders.map((f) => this.diagnosticsService.getByFolder(f.id).pipe(catchError(() => of([])))),
          ).pipe(
            switchMap((arrays) => of(arrays.flat())),
          );
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (diagnostics) => this.diagnostics.set(diagnostics),
        error: (err) => this.error.set(err?.error?.message || 'Failed to load'),
      });
  }

  setPage(p: number): void {
    this.currentPage.set(Math.max(1, Math.min(p, this.totalPages())));
  }

  openCreate(): void {
    const formRef = this.dialog.create<DiagnosticsFormComponent, unknown>({
      zTitle: 'Create Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '1000px',
      zHideFooter: true,
      zDraggable: true,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.onSubmitCallback = (payload) => {
        if ('id' in payload) return;
        this.diagnosticsService.create(payload).subscribe({
          next: () => {
            formRef.close();
            this.load();
            this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics created.' });
          },
          error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
        });
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  openEdit(d: Diagnostics): void {
    const formRef = this.dialog.create<DiagnosticsFormComponent, unknown>({
      zTitle: 'Edit Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '1000px',
      zHideFooter: true,
      zDraggable: true,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.editModel = d;
      form.onSubmitCallback = (payload) => {
        if ('id' in payload) {
          this.diagnosticsService.update(payload.id, payload.data).subscribe({
            next: () => {
              formRef.close();
              this.load();
              this.alertDialog.info({ zTitle: 'Success', zContent: 'Diagnostics updated.' });
            },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
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
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Delete failed.' }),
          });
        }
      });
  }
}
