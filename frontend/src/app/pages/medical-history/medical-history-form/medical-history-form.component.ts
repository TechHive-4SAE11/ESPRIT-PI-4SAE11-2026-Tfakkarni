import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { z } from 'zod';
import { createZodValidator } from '@/core/utils/zod-validator';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { Z_MODAL_DATA } from '@/shared/components/dialog/dialog.service';
import type { MedicalHistory, CreateMedicalHistoryRequest, UpdateMedicalHistoryRequest } from '@/core/services/medical-history.service';
import type { MedicalFolder } from '@/core/services/medical-folder.service';
import { MedicalFolderService } from '@/core/services/medical-folder.service';

export interface MedicalHistoryDialogData {
  medicalFolderId?: number;
}

// ─── Zod Validation Schema ──────────────────────────────────────────────────────
const medicalHistorySchema = z.object({
  medicalFolderId: z.number().min(1, { message: 'Medical folder is required' }),
  allergies: z.string()
    .max(2000, { message: 'Allergies must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  conditions: z.string()
    .max(2000, { message: 'Conditions must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  surgeries: z.string()
    .max(2000, { message: 'Surgeries must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
});

@Component({
  selector: 'app-medical-history-form',
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
        <label for="allergies" class="block text-sm font-medium mb-1">Allergies (optional)</label>
        <textarea id="allergies" z-input class="w-full min-h-[80px]" [formControl]="form.controls.allergies" placeholder="Allergies"></textarea>
      </div>
      <div>
        <label for="conditions" class="block text-sm font-medium mb-1">Conditions (optional)</label>
        <textarea id="conditions" z-input class="w-full min-h-[80px]" [formControl]="form.controls.conditions" placeholder="Conditions"></textarea>
      </div>
      <div>
        <label for="surgeries" class="block text-sm font-medium mb-1">Surgeries (optional)</label>
        <textarea id="surgeries" z-input class="w-full min-h-[80px]" [formControl]="form.controls.surgeries" placeholder="Surgeries"></textarea>
      </div>
      <div class="flex gap-2 justify-end pt-2">
        <button type="button" z-button zType="outline" (click)="onCancelClick()">Cancel</button>
        <button type="button" z-button [disabled]="form.invalid || isSubmitting()" (click)="onSubmit($event)">{{ editModelSignal() ? 'Update' : 'Create' }}</button>
      </div>
    </form>
  `,
})
export class MedicalHistoryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly modalData = inject<MedicalHistoryDialogData | null>(Z_MODAL_DATA, { optional: true });

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

  @Input() set editModel(m: MedicalHistory | null) {
    this._editModel.set(m);
    if (m) {
      this.form.patchValue({
        medicalFolderId: m.medicalFolderId,
        allergies: m.allergies ?? '',
        conditions: m.conditions ?? '',
        surgeries: m.surgeries ?? '',
      });
    }
  }

  private readonly _editModel = signal<MedicalHistory | null>(null);
  readonly editModelSignal = this._editModel.asReadonly();

  folders = signal<MedicalFolder[]>([]);
  isSubmitting = signal(false);
  onSubmitCallback: ((payload: CreateMedicalHistoryRequest | { id: number; data: UpdateMedicalHistoryRequest }) => void) | null = null;
  onCancelCallback: (() => void) | null = null;

  form = this.fb.nonNullable.group({
    medicalFolderId: [0 as number, createZodValidator(medicalHistorySchema.shape.medicalFolderId)],
    allergies: ['', createZodValidator(medicalHistorySchema.shape.allergies)],
    conditions: ['', createZodValidator(medicalHistorySchema.shape.conditions)],
    surgeries: ['', createZodValidator(medicalHistorySchema.shape.surgeries)],
  });

  constructor() {
    this.medicalFolderService.getAll().subscribe((list) => this.folders.set(list));
  }

  onSubmit(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.onSubmitCallback) return;
    const raw = this.form.getRawValue();
    const edit = this._editModel();
    if (edit) {
      this.onSubmitCallback({
        id: edit.id,
        data: {
          allergies: raw.allergies || undefined,
          conditions: raw.conditions || undefined,
          surgeries: raw.surgeries || undefined,
        },
      });
    } else {
      this.onSubmitCallback({
        medicalFolderId: raw.medicalFolderId,
        allergies: raw.allergies || undefined,
        conditions: raw.conditions || undefined,
        surgeries: raw.surgeries || undefined,
      });
    }
    this.isSubmitting.set(false);
  }

  onCancelClick(): void {
    if (this.onCancelCallback) this.onCancelCallback();
  }
}
