import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { of, forkJoin } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { ZardDialogService } from '@/shared/components/dialog';
import { MedicalHistoryService, type MedicalHistory } from '@/core/services/medical-history.service';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';
// @ts-expect-error - used for dynamic component instantiation in dialog.create()
import { MedicalHistoryFormComponent } from '../medical-history-form/medical-history-form.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-medical-history-list',
  standalone: true,
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    // @ts-ignore - used for dynamic component instantiation in dialog.create()
    MedicalHistoryFormComponent,
  ],
  templateUrl: './medical-history-list.component.html',
})
export class MedicalHistoryListComponent implements OnInit {
  private readonly medicalHistoryService = inject(MedicalHistoryService);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);

  folders = signal<MedicalFolder[]>([]);
  historyList = signal<MedicalHistory[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  folderFilterId = signal<number | null>(null);
  searchTerm = signal('');
  currentPage = signal(1);

  filteredHistory = computed(() => {
    let list = this.historyList();
    const folderId = this.folderFilterId();
    if (folderId != null) {
      list = list.filter((h) => h.medicalFolderId === folderId);
    }
    const term = this.searchTerm().toLowerCase().trim();
    if (term) {
      list = list.filter(
        (h) =>
          (h.allergies?.toLowerCase().includes(term)) ||
          (h.conditions?.toLowerCase().includes(term)) ||
          (h.surgeries?.toLowerCase().includes(term)),
      );
    }
    return list;
  });

  paginated = computed(() => {
    const list = this.filteredHistory();
    const page = this.currentPage();
    const start = (page - 1) * PAGE_SIZE;
    return list.slice(start, start + PAGE_SIZE);
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredHistory().length / PAGE_SIZE)));

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
            folders.map((f) => this.medicalHistoryService.getByFolder(f.id).pipe(catchError(() => of([])))),
          ).pipe(switchMap((arrays: unknown) => of((arrays as any[]).flat())));
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (list) => this.historyList.set(list),
        error: (err) => this.error.set(err?.error?.message || 'Failed to load'),
      });
  }

  setPage(p: number): void {
    this.currentPage.set(Math.max(1, Math.min(p, this.totalPages())));
  }

  openCreate(): void {
    const formRef = this.dialog.create<MedicalHistoryFormComponent, unknown>({
      zTitle: 'Create Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '1000px',
      zHideFooter: true,
      zDraggable: true,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.onSubmitCallback = (payload) => {
        if ('id' in payload) return;
        this.medicalHistoryService.create(payload).subscribe({
          next: () => {
            formRef.close();
            this.load();
            this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history created.' });
          },
          error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Create failed.' }),
        });
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  openEdit(h: MedicalHistory): void {
    const formRef = this.dialog.create<MedicalHistoryFormComponent, unknown>({
      zTitle: 'Edit Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '1000px',
      zHideFooter: true,
      zDraggable: true,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.editModel = h;
      form.onSubmitCallback = (payload) => {
        if ('id' in payload) {
          this.medicalHistoryService.update(payload.id, payload.data).subscribe({
            next: () => {
              formRef.close();
              this.load();
              this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical history updated.' });
            },
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Update failed.' }),
          });
        }
      };
      form.onCancelCallback = () => formRef.close();
    }
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
            error: (err) => this.alertDialog.warning({ zTitle: 'Error', zContent: err?.error?.message || 'Delete failed.' }),
          });
        }
      });
  }
}
