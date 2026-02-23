import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, finalize, of } from 'rxjs';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardAlertDialogService } from '@/shared/components/alert-dialog';
import { ZardDialogService } from '@/shared/components/dialog';
import { MedicalFolderService, type MedicalFolder } from '@/core/services/medical-folder.service';
import { UserApiService } from '@/core/services/user-api.service';
import type { UserInfo } from '@/core/services/user-api.service';
import { MedicalFolderFormComponent } from '../medical-folder-form/medical-folder-form.component';
import { MedicalFolderDetailComponent } from '../medical-folder-detail/medical-folder-detail.component';

const PAGE_SIZE = 10;

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
})
export class MedicalFolderListComponent implements OnInit {
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly userApiService = inject(UserApiService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);

  folders = signal<MedicalFolder[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  searchTerm = signal('');
  currentPage = signal(1);
  folderToView = signal<MedicalFolder | null>(null);
  showDetail = signal(false);
  patientNameMap = signal<Record<string, string>>({});

  /** When set, open this folder's detail view on load (e.g. from global search). */
  initialFolderId = input<number | null>(null);

  /** Filter folders to only show those for this doctor. */
  doctorId = input<string | null>(null);

  /** Doctor information (used to auto-fill form) */
  doctor = input<UserInfo | null>(null);

  /** Emitted when detail view is closed (e.g. back to list) so parent can clear initialFolderId. */
  detailClosed = output<void>();

  filteredFolders = computed(() => {
    let list = this.folders();
    
    // Backend handles doctorId filtering via getByDoctorId() endpoint
    // Only apply search term filtering here
    const term = this.searchTerm().toLowerCase().trim();
    if (!term) return list;
    return list.filter(
      (f) =>
        f.patientId?.toLowerCase().includes(term) ||
        this.getPatientName(f.patientId).toLowerCase().includes(term)
    );
  });

  paginatedFolders = computed(() => {
    const list = this.filteredFolders();
    const page = this.currentPage();
    const start = (page - 1) * PAGE_SIZE;
    return list.slice(start, start + PAGE_SIZE);
  });

  totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredFolders().length / PAGE_SIZE))
  );

  ngOnInit(): void {
    this.loadPatientNames();
    this.loadFolders();
    const id = this.initialFolderId();
    if (id != null) {
      this.medicalFolderService.getById(id).subscribe({
        next: (folder) => {
          this.folderToView.set(folder);
          this.showDetail.set(true);
        },
      });
    }
  }

  loadFolders(): void {
    this.loading.set(true);
    this.error.set(null);
    
    const doctorId = this.doctorId();
    const request$ = doctorId 
      ? this.medicalFolderService.getByDoctorId(doctorId)
      : this.medicalFolderService.getAll();
    
    request$
      .pipe(
        catchError((err) => {
          this.error.set(err?.error?.message || 'Failed to load medical folders');
          return of([]);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe((list) => this.folders.set(list));
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
  }

  setPage(p: number): void {
    this.currentPage.set(Math.max(1, Math.min(p, this.totalPages())));
  }

  openCreate(): void {
    const formRef = this.dialog.create<MedicalFolderFormComponent, unknown>({
      zTitle: 'Create Medical Folder',
      zContent: MedicalFolderFormComponent,
      zWidth: '420px',
      zHideFooter: true,
      zData: undefined,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.doctor = this.doctor();
      form.onSubmitCallback = (data) => {
        console.log('Create callback invoked with data:', data);
        this.medicalFolderService.create(data as { patientId: string }).subscribe({
          next: () => {
            console.log('Medical folder created successfully');
            form.isSubmitting.set(false);
            formRef.close();
            this.loadFolders();
            this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical folder created.' });
          },
          error: (err) => {
            console.error('Failed to create medical folder:', err);
            form.isSubmitting.set(false);
            this.alertDialog.warning({
              zTitle: 'Error',
              zContent: err?.error?.message || 'Failed to create folder.',
            });
          },
        });
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  openEdit(folder: MedicalFolder): void {
    const formRef = this.dialog.create<MedicalFolderFormComponent, unknown>({
      zTitle: 'Edit Medical Folder',
      zContent: MedicalFolderFormComponent,
      zWidth: '420px',
      zHideFooter: true,
      zData: undefined,
    });
    const form = formRef.componentInstance;
    if (form) {
      form.doctor = this.doctor();
      form.folder = folder;
      form.onSubmitCallback = (data) => {
        console.log('Update callback invoked with data:', data);
        this.medicalFolderService.update(folder.id, data as { patientId?: string; doctorId?: string }).subscribe({
          next: () => {
            console.log('Medical folder updated successfully');
            form.isSubmitting.set(false);
            formRef.close();
            this.loadFolders();
            this.alertDialog.info({ zTitle: 'Success', zContent: 'Medical folder updated.' });
          },
          error: (err) => {
            console.error('Failed to update medical folder:', err);
            form.isSubmitting.set(false);
            this.alertDialog.warning({
              zTitle: 'Error',
              zContent: err?.error?.message || 'Failed to update folder.',
            });
          },
        });
      };
      form.onCancelCallback = () => formRef.close();
    }
  }

  viewDetail(folder: MedicalFolder): void {
    this.folderToView.set(folder);
    this.showDetail.set(true);
  }

  closeDetail(): void {
    this.showDetail.set(false);
    this.folderToView.set(null);
    this.detailClosed.emit();
    this.loadFolders();
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
              this.loadFolders();
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
