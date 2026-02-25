import { Component, Input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import type { Diagnostics } from '@/core/services/diagnostics.service';
import type { MedicalHistory } from '@/core/services/medical-history.service';

export type CompareMode = 'diagnostics' | 'history';

interface DiffRow {
  field: string;
  label: string;
  left: string;
  right: string;
  status: 'same' | 'changed' | 'added' | 'removed';
}

@Component({
  selector: 'app-dossier-compare',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dossier-compare.component.html',
})
export class DossierCompareComponent {
  private modeSignal = signal<CompareMode>('diagnostics');
  private itemsSignal = signal<Diagnostics[] | MedicalHistory[]>([]);

  @Input() set mode(v: CompareMode) {
    if (v) this.modeSignal.set(v);
  }
  @Input() set items(v: Diagnostics[] | MedicalHistory[] | null | undefined) {
    this.itemsSignal.set(v ?? []);
  }

  itemsList = this.itemsSignal.asReadonly();

  selectedLeftId = signal<string | number | null>(null);
  selectedRightId = signal<string | number | null>(null);

  private leftItem = computed(() => {
    const id = this.selectedLeftId();
    const list = this.itemsSignal();
    if (id == null || !list?.length) return null;
    return list.find((x) => x.id === Number(id)) ?? null;
  });
  private rightItem = computed(() => {
    const id = this.selectedRightId();
    const list = this.itemsSignal();
    if (id == null || !list?.length) return null;
    return list.find((x) => x.id === Number(id)) ?? null;
  });

  diffRows = computed((): DiffRow[] => {
    const left = this.leftItem();
    const right = this.rightItem();
    const m = this.modeSignal();
    if (!left || !right || left.id === right.id) return [];

    const str = (v: unknown) => (v == null || v === undefined ? '' : String(v).trim());
    const eq = (a: string, b: string) => a === b;

    if (m === 'diagnostics') {
      const l = left as Diagnostics;
      const r = right as Diagnostics;
      const rows: DiffRow[] = [
        { field: 'diseaseName', label: 'Disease', left: str(l.diseaseName), right: str(r.diseaseName), status: eq(str(l.diseaseName), str(r.diseaseName)) ? 'same' : 'changed' },
        { field: 'stage', label: 'Stage', left: str(l.stage), right: str(r.stage), status: eq(str(l.stage), str(r.stage)) ? 'same' : 'changed' },
        { field: 'diagnosisDate', label: 'Diagnosis date', left: str(l.diagnosisDate), right: str(r.diagnosisDate), status: eq(str(l.diagnosisDate), str(r.diagnosisDate)) ? 'same' : 'changed' },
        { field: 'comorbidities', label: 'Comorbidities', left: str(l.comorbidities), right: str(r.comorbidities), status: eq(str(l.comorbidities), str(r.comorbidities)) ? 'same' : 'changed' },
      ];
      return rows;
    }
    const l = left as MedicalHistory;
    const r = right as MedicalHistory;
    return [
      { field: 'allergies', label: 'Allergies', left: str(l.allergies), right: str(r.allergies), status: eq(str(l.allergies), str(r.allergies)) ? 'same' : 'changed' },
      { field: 'conditions', label: 'Conditions', left: str(l.conditions), right: str(r.conditions), status: eq(str(l.conditions), str(r.conditions)) ? 'same' : 'changed' },
      { field: 'surgeries', label: 'Surgeries', left: str(l.surgeries), right: str(r.surgeries), status: eq(str(l.surgeries), str(r.surgeries)) ? 'same' : 'changed' },
    ];
  });

  trackItem(item: Diagnostics | MedicalHistory): number {
    return item.id;
  }

  optionLabel(item: Diagnostics | MedicalHistory): string {
    const m = this.modeSignal();
    if (m === 'diagnostics') {
      const d = item as Diagnostics;
      return `${d.diseaseName}${d.stage ? ` (${d.stage})` : ''} — ${d.diagnosisDate ? new Date(d.diagnosisDate).toLocaleDateString() : ''}`;
    }
    const h = item as MedicalHistory;
    const parts = [h.allergies, h.conditions, h.surgeries].filter(Boolean);
    return parts.length ? parts.slice(0, 1).join('').slice(0, 40) + (parts[0].length > 40 ? '…' : '') : `Entry #${h.id}`;
  }

  onLeftChange(value: string): void {
    this.selectedLeftId.set(value === '' ? null : Number(value));
  }

  onRightChange(value: string): void {
    this.selectedRightId.set(value === '' ? null : Number(value));
  }
}
