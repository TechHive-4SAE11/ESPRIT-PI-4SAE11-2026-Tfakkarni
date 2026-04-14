import { Component, Input, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PrescriptionService } from '@/core/services/prescription.service';
import { PrescriptionResponseDTO } from '@/core/models/prescription.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';

@Component({
  selector: 'app-prescription-history-analytics',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardIconComponent, ZardBadgeComponent],
  templateUrl: './prescription-history-analytics.component.html',
})
export class PrescriptionHistoryAnalyticsComponent implements OnInit {
  @Input() patientId!: string | number;
  prescriptions = signal<PrescriptionResponseDTO[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  constructor(private prescriptionService: PrescriptionService) {}

  ngOnInit(): void {
    if (this.patientId === undefined || this.patientId === null) return;
    this.isLoading.set(true);
    this.prescriptionService.getPrescriptionsByPatient(this.patientId.toString())
      .subscribe({
        next: (data) => {
          this.prescriptions.set(data);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.error.set('Failed to load prescription history.');
          this.isLoading.set(false);
        }
      });
  }

  get totalPrescriptions() {
    return this.prescriptions().length;
  }

  get uniqueMedications() {
    const meds = new Set<string>();
    this.prescriptions().forEach(p => p.medications.forEach(m => meds.add(m.medicationName)));
    return meds.size;
  }

  get adherenceRate() {
    // Example: % of medications with status ACTIVE or ONGOING
    const allMeds = this.prescriptions().flatMap(p => p.medications);
    if (allMeds.length === 0) return 0;
    const adherent = allMeds.filter(m => m.status === 'ACTIVE' || m.status === 'ONGOING').length;
    return Math.round((adherent / allMeds.length) * 100);
  }
}
