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
import { SymptomPilotService, SymptomPilotResponse } from '@/core/services/symptom-pilot.service';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';
import { SymptomCoPilotComponent } from '@/shared/components/symptom-co-pilot/symptom-co-pilot.component';

export interface MedicalHistoryDialogData {
  medicalFolderId?: number;
}

// ─── Zod Validation Schema ──────────────────────────────────────────────────────
const medicalHistorySchema = z.object({
  medicalFolderId: z.coerce.number().min(1, { message: 'Medical folder is required' }),
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
  symptoms: z.string()
    .max(2000, { message: 'Symptoms must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  recommendedTreatment: z.string()
    .max(2000, { message: 'Recommended treatment must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
  familyHistory: z.string()
    .max(2000, { message: 'Family history must not exceed 2000 characters' })
    .optional()
    .transform(v => (v?.trim() ? v.trim() : undefined)),
});

@Component({
  selector: 'app-medical-history-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZardButtonComponent, ZardIconComponent, ZardInputDirective, SymptomCoPilotComponent],
  template: `
    <div class="p-1">
      <form (ngSubmit)="onSubmit($event)" class="flex flex-col gap-5">
        <!-- AI Critical Alert Banner -->
        @if (pilotData()?.isCriticalAlert) {
          <div class="bg-destructive/15 border-2 border-destructive/30 rounded-2xl p-4 flex items-center gap-4 animate-in zoom-in-95 shadow-lg shadow-destructive/20 mb-2">
            <div class="w-12 h-12 rounded-full bg-destructive/20 flex items-center justify-center text-destructive animate-pulse">
               <z-icon zType="circle-alert" size="24"></z-icon>
            </div>
            <div class="flex-1">
               <h3 class="text-sm font-black text-destructive uppercase tracking-wider">Critical AI Detection</h3>
               <p class="text-xs font-bold text-destructive/80">{{ pilotData()?.alertMessage }}</p>
            </div>
            <div class="px-3 py-1 bg-destructive text-destructive-foreground text-[10px] font-black rounded-full animate-bounce">
               URGENT
            </div>
          </div>
        }

        @if (!prefilledFolderId()) {
        <div class="space-y-1.5">
          <label for="medicalFolderId" class="block text-sm font-semibold text-muted-foreground">Medical Folder</label>
          <select
            id="medicalFolderId"
            class="w-full h-9 rounded-md border border-input bg-background px-3 py-1 text-sm focus:ring-2 focus:ring-primary/20 outline-none transition-all"
            [value]="form.controls.medicalFolderId.value"
            (change)="form.controls.medicalFolderId.setValue(+$any($event.target).value)"
          >
            <option [value]="null">Select folder</option>
            @for (f of folders(); track f.id) {
              <option [value]="f.id">{{ f.patientId }} (ID: {{ f.id }})</option>
            }
          </select>
          @if ((form.controls.medicalFolderId.touched || formSubmitted()) && form.controls.medicalFolderId.errors) {
            <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.medicalFolderId.errors['message'] || 'Medical folder is required' }}</p>
          }
        </div>
        }

        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div class="space-y-1.5">
            <label for="allergies" class="block text-sm font-semibold text-muted-foreground">Allergies (optional)</label>
            <textarea id="allergies" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.allergies" placeholder="e.g., Peanuts, Penicillin"></textarea>
            @if ((form.controls.allergies.touched || formSubmitted()) && form.controls.allergies.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.allergies.errors['message'] }}</p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="conditions" class="block text-sm font-semibold text-muted-foreground">Conditions (optional)</label>
            <textarea id="conditions" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.conditions" placeholder="e.g., Hypertension, Diabetes"></textarea>
            @if ((form.controls.conditions.touched || formSubmitted()) && form.controls.conditions.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.conditions.errors['message'] }}</p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="surgeries" class="block text-sm font-semibold text-muted-foreground">Surgeries (optional)</label>
            <textarea id="surgeries" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.surgeries" placeholder="Recent surgeries..."></textarea>
            @if ((form.controls.surgeries.touched || formSubmitted()) && form.controls.surgeries.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.surgeries.errors['message'] }}</p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="symptoms" class="block text-sm font-semibold text-muted-foreground flex justify-between">
              <span>Symptoms (optional)</span>
              <span class="text-[10px] text-primary flex items-center gap-1 opacity-70">
                <z-icon zType="brain" size="10"></z-icon> Co-Pilot Active
              </span>
            </label>
            <textarea id="symptoms" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.symptoms" placeholder="Current symptoms..." (input)="onSymptomsInput()"></textarea>
            @if ((form.controls.symptoms.touched || formSubmitted()) && form.controls.symptoms.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.symptoms.errors['message'] }}</p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="recommendedTreatment" class="block text-sm font-semibold text-muted-foreground">Recommended Treatment (optional)</label>
            <textarea id="recommendedTreatment" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.recommendedTreatment" placeholder="Treatment plan..."></textarea>
            @if ((form.controls.recommendedTreatment.touched || formSubmitted()) && form.controls.recommendedTreatment.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.recommendedTreatment.errors['message'] }}</p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="familyHistory" class="block text-sm font-semibold text-muted-foreground">Family History (optional)</label>
            <textarea id="familyHistory" z-input class="w-full min-h-[90px] text-sm" [formControl]="form.controls.familyHistory" placeholder="Family medical history..."></textarea>
            @if ((form.controls.familyHistory.touched || formSubmitted()) && form.controls.familyHistory.errors) {
              <p class="text-destructive text-xs mt-1 font-medium animate-in fade-in slide-in-from-top-1">{{ form.controls.familyHistory.errors['message'] }}</p>
            }
          </div>
        </div>

        <div class="flex gap-3 justify-end pt-5 border-t border-border mt-2">
          <button type="button" z-button zType="outline" class="min-w-[100px]" (click)="onCancelClick()">Annuler</button>
          <button type="button" z-button [disabled]="(form.invalid && formSubmitted()) || isSubmitting()" class="min-w-[120px]" (click)="onSubmit($event)">
            {{ isSubmitting() ? 'Envoi...' : (editModelSignal() ? 'Enregistrer' : 'Créer') }}
          </button>
        </div>

        @if (form.invalid && formSubmitted()) {
          <div class="mt-2 p-3 bg-destructive/5 border border-destructive/10 rounded-xl text-xs text-destructive animate-in bounce-in-95">
              <p class="font-bold flex items-center mb-2 uppercase tracking-tight">
                  <z-icon zType="circle-alert" size="14" class="mr-1.5"></z-icon>
                  Veuillez corriger les champs suivants :
              </p>
              <ul class="list-disc list-inside space-y-1 ml-1 opacity-90">
                  @if (form.controls.medicalFolderId.errors) { <li>Sélectionnez un dossier médical</li> }
                  @if (form.controls.allergies.errors) { <li>{{ form.controls.allergies.errors['message'] }}</li> }
                  @if (form.controls.conditions.errors) { <li>{{ form.controls.conditions.errors['message'] }}</li> }
                  @if (form.controls.surgeries.errors) { <li>{{ form.controls.surgeries.errors['message'] }}</li> }
                  @if (form.controls.symptoms.errors) { <li>{{ form.controls.symptoms.errors['message'] }}</li> }
                  @if (form.controls.recommendedTreatment.errors) { <li>{{ form.controls.recommendedTreatment.errors['message'] }}</li> }
                  @if (form.controls.familyHistory.errors) { <li>{{ form.controls.familyHistory.errors['message'] }}</li> }
              </ul>
          </div>
        }
        <!-- AI Co-Pilot Inline Panel -->
        @if (pilotData()?.isCriticalAlert || pilotData()?.predictions?.length || isAnalyzing()) {
          <div class="border border-primary/20 bg-card/50 rounded-xl p-4 animate-in fade-in slide-in-from-bottom-2">
            <z-symptom-co-pilot [data]="pilotData()" [loading]="isAnalyzing()"></z-symptom-co-pilot>
          </div>
        }
      </form>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      max-width: 100%;
      overflow-x: hidden;
    }
    form {
      width: 100%;
    }
  `]
})
export class MedicalHistoryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly symptomPilotService = inject(SymptomPilotService);
  private readonly modalData = inject<MedicalHistoryDialogData | null>(Z_MODAL_DATA, { optional: true });

  private readonly destroy$ = new Subject<void>();
  private readonly symptomChange$ = new Subject<string>();

  pilotData = signal<SymptomPilotResponse | null>(null);
  isAnalyzing = signal(false);

  /** When set (e.g. from folder detail), medical folder is fixed and the folder selector is hidden. */
  prefilledFolderId = signal<number | null>(null);
  formSubmitted = signal(false);

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
    this.symptomChange$.pipe(
      debounceTime(800),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (!value.trim()) {
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

  onSymptomsInput(): void {
    this.symptomChange$.next(this.form.controls.symptoms.value);
  }

  @Input() set editModel(m: MedicalHistory | null) {
    this._editModel.set(m);
    if (m) {
      this.form.patchValue({
        medicalFolderId: m.medicalFolderId,
        allergies: m.allergies ?? '',
        conditions: m.conditions ?? '',
        surgeries: m.surgeries ?? '',
        symptoms: m.symptoms ?? '',
        recommendedTreatment: m.recommendedTreatment ?? '',
        familyHistory: m.familyHistory ?? '',
      });
    } else {
      this.form.reset();
      this.formSubmitted.set(false);
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
    symptoms: ['', createZodValidator(medicalHistorySchema.shape.symptoms)],
    recommendedTreatment: ['', createZodValidator(medicalHistorySchema.shape.recommendedTreatment)],
    familyHistory: ['', createZodValidator(medicalHistorySchema.shape.familyHistory)],
  });

  constructor() {
    this.medicalFolderService.getAll().subscribe((list) => this.folders.set(list));
  }

  onSubmit(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.formSubmitted.set(true);
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
          symptoms: raw.symptoms || undefined,
          recommendedTreatment: raw.recommendedTreatment || undefined,
          familyHistory: raw.familyHistory || undefined,
        },
      });
    } else {
      this.onSubmitCallback({
        medicalFolderId: raw.medicalFolderId,
        allergies: raw.allergies || undefined,
        conditions: raw.conditions || undefined,
        surgeries: raw.surgeries || undefined,
        symptoms: raw.symptoms || undefined,
        recommendedTreatment: raw.recommendedTreatment || undefined,
        familyHistory: raw.familyHistory || undefined,
      });
    }
    this.isSubmitting.set(false);
  }

  onCancelClick(): void {
    if (this.onCancelCallback) this.onCancelCallback();
  }
}
