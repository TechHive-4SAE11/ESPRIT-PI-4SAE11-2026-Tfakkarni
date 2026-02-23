import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
import { MedicalFolderFormComponent } from '../medical-folder-form/medical-folder-form.component';
import { DiagnosticsFormComponent } from '@/pages/diagnostics/diagnostics-form/diagnostics-form.component';
import { MedicalHistoryFormComponent } from '@/pages/medical-history/medical-history-form/medical-history-form.component';

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
  ],
  templateUrl: './medical-folder-detail.component.html',
})
export class MedicalFolderDetailComponent implements OnInit {
  folderId = input.required<number>();
  back = output<void>();
  updated = output<void>();

  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly medicalHistoryService = inject(MedicalHistoryService);
  private readonly alertDialog = inject(ZardAlertDialogService);
  private readonly dialog = inject(ZardDialogService);

  folder = signal<{ id: number; patientId: string; doctorId: string; createdAt: string; updatedAt: string } | null>(null);
  diagnostics = signal<Diagnostics[]>([]);
  history = signal<MedicalHistory[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = this.folderId();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.medicalFolderService.getById(id).subscribe({
      next: (f) => this.folder.set(f),
      error: (err) => {
        this.error.set(err?.error?.message || 'Folder not found');
      },
      complete: () => this.loading.set(false),
    });
    this.diagnosticsService.getByFolder(id).subscribe({
      next: (list) => this.diagnostics.set(list),
      error: () => this.diagnostics.set([]),
    });
    this.medicalHistoryService.getByFolder(id).subscribe({
      next: (list) => this.history.set(list),
      error: () => this.history.set([]),
    });
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
    const formRef = this.dialog.create<DiagnosticsFormComponent, unknown>({
      zTitle: 'Add Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zData: { medicalFolderId: id },
    });
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
  }

  openAddHistory(): void {
    const id = this.folderId();
    const formRef = this.dialog.create<MedicalHistoryFormComponent, unknown>({
      zTitle: 'Add Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zData: { medicalFolderId: id },
    });
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
  }

  editDiagnostics(d: Diagnostics): void {
    const formRef = this.dialog.create<DiagnosticsFormComponent, unknown>({
      zTitle: 'Edit Diagnostics',
      zContent: DiagnosticsFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zData: { diagnostics: d },
    });
    const form = formRef.componentInstance;
    if (form) {
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
    const formRef = this.dialog.create<MedicalHistoryFormComponent, unknown>({
      zTitle: 'Edit Medical History',
      zContent: MedicalHistoryFormComponent,
      zWidth: '480px',
      zHideFooter: true,
      zData: { history: h },
    });
    const form = formRef.componentInstance;
    if (form) {
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
