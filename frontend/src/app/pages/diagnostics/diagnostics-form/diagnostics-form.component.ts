import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { z } from 'zod';
import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { Z_MODAL_DATA } from '@/shared/components/dialog/dialog.service';
import type { Diagnostics, CreateDiagnosticsRequest, UpdateDiagnosticsRequest, DiagnosticAttachment } from '@/core/services/diagnostics.service';
import type { MedicalFolder } from '@/core/services/medical-folder.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import { DiagnosticsService } from '@/core/services/diagnostics.service';
import { SymptomPilotService, SymptomPilotResponse } from '@/core/services/symptom-pilot.service';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { SymptomCoPilotComponent } from '@/shared/components/symptom-co-pilot/symptom-co-pilot.component';

export interface DiagnosticsDialogData {
  medicalFolderId?: number;
}

// ─── Zod Validation Schema ──────────────────────────────────────────────────────
const diagnosticsSchema = z.object({
  medicalFolderId: z.coerce.number().min(1, { message: 'Medical folder is required' }),
  diseaseName: z.string()
    .min(1, { message: 'Disease name is required' })
    .min(2, { message: 'Disease name must be at least 2 characters' })
    .max(255, { message: 'Disease name must not exceed 255 characters' })
    .trim(),
  stage: z.string()
    .max(100, { message: 'Stage must not exceed 100 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  comorbidities: z.string()
    .max(1000, { message: 'Comorbidities must not exceed 1000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  diagnosisDate: z.string()
    .min(1, { message: 'Diagnosis date is required' })
    .refine(val => !isNaN(new Date(val).getTime()), { message: 'Invalid date format' })
    .refine(val => new Date(val) <= new Date(), { message: 'Diagnosis date cannot be in the future' }),
});

@Component({
  selector: 'app-diagnostics-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZardButtonComponent, ZardIconComponent, ZardInputDirective, SymptomCoPilotComponent],
  templateUrl: './diagnostics-form.component.html',
  styles: [`
    :host {
      display: block;
      max-width: 100%;
      overflow-x: hidden;
    }
    form {
      width: 100%;
    }
    .file-row {
      display: grid;
      grid-template-columns: 140px 1fr;
      gap: 0.5rem;
      align-items: center;
    }
    @media (max-width: 480px) {
      .file-row {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DiagnosticsFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly diagnosticsService = inject(DiagnosticsService);
  private readonly symptomPilotService = inject(SymptomPilotService);
  private readonly modalData = inject<DiagnosticsDialogData | null>(Z_MODAL_DATA, { optional: true });

  private readonly destroy$ = new Subject<void>();
  private readonly aiTrigger$ = new Subject<string>();

  pilotData = signal<SymptomPilotResponse | null>(null);
  isAnalyzing = signal(false);

  /** When set (e.g. from folder detail), medical folder is fixed and the folder selector is hidden. */
  prefilledFolderId = signal<number | null>(null);
  formSubmitted = signal(false);

  /** File upload handling */
  selectedFiles = signal<File[]>([]);
  fileDescriptions = signal<string[]>([]);
  isDragging = signal(false);

  @Input() set prefillFolderId(id: number | null) {
    this.prefilledFolderId.set(id ?? null);
    if (id != null) {
      this.form.controls.medicalFolderId.setValue(id);
    }
  }

  ngOnInit(): void {
    const folderId = this.modalData?.medicalFolderId ?? null;
    if (folderId != null) {
      this.prefilledFolderId.set(folderId);
      this.form.controls.medicalFolderId.setValue(folderId);
    }

    // AI Pilot Listener
    this.aiTrigger$.pipe(
      debounceTime(1000),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (!value.trim() || value.length < 5) {
        this.pilotData.set(null);
        return;
      }
      this.isAnalyzing.set(true);
      this.symptomPilotService.analyze(value).subscribe({
        next: (res) => {
          this.pilotData.set(res);
          this.isAnalyzing.set(false);
        },
        error: () => this.isAnalyzing.set(false)
      });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFieldInput(): void {
    const combined = `${this.form.controls.diseaseName.value} ${this.form.controls.comorbidities.value}`;
    this.aiTrigger$.next(combined);
  }

  @Input() set editModel(m: Diagnostics | null) {
    this._editModel.set(m);
    if (m) {
      const dateVal = m.diagnosisDate ? m.diagnosisDate.slice(0, 16) : '';
      this.diagnosisDateValue.set(dateVal);
      this.form.patchValue({
        medicalFolderId: m.medicalFolderId,
        diseaseName: m.diseaseName,
        stage: m.stage ?? '',
        comorbidities: m.comorbidities ?? '',
        diagnosisDate: m.diagnosisDate ? new Date(m.diagnosisDate).toISOString() : '',
      });
    } else {
      this.form.reset();
      this.formSubmitted.set(false);
    }
  }

  private readonly _editModel = signal<Diagnostics | null>(null);
  readonly editModelSignal = this._editModel.asReadonly();

  folders = signal<MedicalFolder[]>([]);
  isSubmitting = signal(false);
  onSubmitCallback: ((payload: CreateDiagnosticsRequest | { id: number; data: UpdateDiagnosticsRequest }) => void) | null = null;
  onCancelCallback: (() => void) | null = null;

  form = this.fb.nonNullable.group({
    medicalFolderId: [0 as number, createZodValidator(diagnosticsSchema.shape.medicalFolderId)],
    diseaseName: ['', createZodValidator(diagnosticsSchema.shape.diseaseName)],
    stage: ['', createZodValidator(diagnosticsSchema.shape.stage)],
    comorbidities: ['', createZodValidator(diagnosticsSchema.shape.comorbidities)],
    diagnosisDate: ['', createZodValidator(diagnosticsSchema.shape.diagnosisDate)],
  });

  diagnosisDateValue = signal('');

  constructor() {
    this.medicalFolderService.getAll().subscribe((list) => this.folders.set(list));
  }

  onDiagnosisDateInput(value: string): void {
    this.diagnosisDateValue.set(value);
    this.form.controls.diagnosisDate.setValue(value ? new Date(value).toISOString() : '');
  }

  onSubmit(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.formSubmitted.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.onSubmitCallback) return;
    const raw = this.form.getRawValue();
    const diagnosisDate = raw.diagnosisDate ? new Date(raw.diagnosisDate).toISOString() : new Date().toISOString();
    this.isSubmitting.set(true);
    const edit = this._editModel();
    if (edit) {
      this.onSubmitCallback({
        id: edit.id,
        data: {
          diseaseName: raw.diseaseName,
          stage: raw.stage || undefined,
          comorbidities: raw.comorbidities || undefined,
          diagnosisDate,
        },
      });
    } else {
      this.onSubmitCallback({
        medicalFolderId: raw.medicalFolderId,
        diseaseName: raw.diseaseName,
        stage: raw.stage || undefined,
        comorbidities: raw.comorbidities || undefined,
        diagnosisDate,
      });
    }
    this.isSubmitting.set(false);
  }

  onCancelClick(): void {
    if (this.onCancelCallback) this.onCancelCallback();
  }

  // File handling methods
  triggerFileSelect(): void {
    const input = document.getElementById('fileInput') as HTMLInputElement;
    input?.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const newFiles = Array.from(input.files);
      const currentFiles = this.selectedFiles();
      const updatedFiles = [...currentFiles, ...newFiles];
      this.selectedFiles.set(updatedFiles);
      const currentDescriptions = this.fileDescriptions();
      const newDescriptions = new Array(newFiles.length).fill('');
      this.fileDescriptions.set([...currentDescriptions, ...newDescriptions]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      const newFiles = Array.from(files);
      const currentFiles = this.selectedFiles();
      const updatedFiles = [...currentFiles, ...newFiles];
      this.selectedFiles.set(updatedFiles);
      const currentDescriptions = this.fileDescriptions();
      const newDescriptions = new Array(newFiles.length).fill('');
      this.fileDescriptions.set([...currentDescriptions, ...newDescriptions]);
    }
  }

  removeFile(index: number): void {
    const currentFiles = this.selectedFiles();
    const currentDescriptions = this.fileDescriptions();
    currentFiles.splice(index, 1);
    currentDescriptions.splice(index, 1);
    this.selectedFiles.set([...currentFiles]);
    this.fileDescriptions.set([...currentDescriptions]);
  }

  clearFiles(): void {
    this.selectedFiles.set([]);
    this.fileDescriptions.set([]);
    this.isDragging.set(false);
  }

  updateFileDescription(index: number, description: string): void {
    const currentDescriptions = this.fileDescriptions();
    currentDescriptions[index] = description;
    this.fileDescriptions.set([...currentDescriptions]);
  }
}
