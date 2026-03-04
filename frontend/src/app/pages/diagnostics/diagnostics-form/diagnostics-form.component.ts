import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { z } from 'zod';
import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { Z_MODAL_DATA } from '@/shared/components/dialog/dialog.service';
import type { Diagnostics, CreateDiagnosticsRequest, UpdateDiagnosticsRequest } from '@/core/services/diagnostics.service';
import type { MedicalFolder } from '@/core/services/medical-folder.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';

export interface DiagnosticsDialogData {
  medicalFolderId?: number;
}

// ─── Zod Validation Schema ──────────────────────────────────────────────────────
const diagnosticsSchema = z.object({
  medicalFolderId: z.number().min(1, { message: 'Medical folder is required' }),
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
  imports: [CommonModule, ReactiveFormsModule, ZardButtonComponent, ZardIconComponent, ZardInputDirective],
  template: `
    <form (ngSubmit)="onSubmit($event)" class="flex flex-col gap-4">
      @if (!prefilledFolderId()) {
      <div>
        <label for="medicalFolderId" class="block text-sm font-medium mb-1">Medical Folder</label>
        <select
          id="medicalFolderId"
          class="w-full flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          [value]="form.controls.medicalFolderId.value"
          (change)="form.controls.medicalFolderId.setValue(+$any($event.target).value)"
        >
          <option [value]="null">Select folder</option>
          @for (f of folders(); track f.id) {
            <option [value]="f.id">{{ f.patientId }} ({{ f.id }})</option>
          }
        </select>
        @if (form.controls.medicalFolderId.touched && form.controls.medicalFolderId.errors) {
          <p class="text-destructive text-sm mt-1">Required</p>
        }
      </div>
      }
      <div>
        <label for="diseaseName" class="block text-sm font-medium mb-1">Disease Name</label>
        <input id="diseaseName" type="text" z-input class="w-full" [formControl]="form.controls.diseaseName" placeholder="Disease name" />
        @if (form.controls.diseaseName.touched && form.controls.diseaseName.errors) {
          <p class="text-destructive text-sm mt-1">
            {{ form.controls.diseaseName.errors['zodError'] || 'Disease name is required' }}
          </p>
        }
      </div>
      <div>
        <label for="stage" class="block text-sm font-medium mb-1">Stage (optional)</label>
        <input id="stage" type="text" z-input class="w-full" [formControl]="form.controls.stage" placeholder="Stage" />
      </div>
      <div>
        <label for="comorbidities" class="block text-sm font-medium mb-1">Comorbidities (optional)</label>
        <textarea id="comorbidities" z-input class="w-full min-h-[80px]" [formControl]="form.controls.comorbidities" placeholder="Comorbidities"></textarea>
      </div>
      <div>
        <label for="diagnosisDate" class="block text-sm font-medium mb-1">Diagnosis Date</label>
        <input
          id="diagnosisDate"
          type="datetime-local"
          class="w-full flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm"
          [value]="diagnosisDateValue()"
          (input)="onDiagnosisDateInput($any($event.target).value)"
        />
        @if (form.controls.diagnosisDate.touched && form.controls.diagnosisDate.errors) {
          <p class="text-destructive text-sm mt-1">Required</p>
        }
      </div>
      <div class="flex gap-2 justify-end pt-2">
        <button type="button" z-button zType="outline" (click)="onCancelClick()">Cancel</button>
        <button type="button" z-button [disabled]="form.invalid || isSubmitting()" (click)="onSubmit($event)">{{ editModelSignal() ? 'Update' : 'Create' }}</button>
      </div>
    </form>
  `,
})
export class DiagnosticsFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly modalData = inject<DiagnosticsDialogData | null>(Z_MODAL_DATA, { optional: true });

  /** When set (e.g. from folder detail), medical folder is fixed and the folder selector is hidden. */
  prefilledFolderId = signal<number | null>(null);

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
}
