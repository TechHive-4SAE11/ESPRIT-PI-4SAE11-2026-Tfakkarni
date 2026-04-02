import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
  ViewChild,
  ElementRef,
  AfterViewChecked,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { DossierAnalyticsService } from '@/core/services/dossier-analytics.service';
import type {
  DiseaseCount,
  MonthComparison,
  DiagnosticsByMonth,
  CrossPatientDisease,
  ClinicalSafetyStats,
} from '@/core/services/dossier-analytics.service';
import { MedicalFolderPdfService } from '@/core/services/medical-folder-pdf.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const CHART_PALETTE = [
  '#2563eb', // blue
  '#dc2626', // red
  '#ea580c', // orange
  '#16a34a', // green
  '#0d9488', // teal
  '#7c3aed', // purple
  '#94a3b8', // grey
  '#eab308', // yellow
];

@Component({
  selector: 'app-dossier-analytics',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    ZardButtonComponent,
    ZardTableImports,
  ],
  template: `
    <div class="space-y-6">
      <h2 class="text-2xl font-bold flex items-center gap-2">
        <z-icon zType="bar-chart-3" class="h-8 w-8 text-primary" />
        Dossier Analytics
      </h2>
      <p class="text-muted-foreground">Statistics from medical history and diagnostics.</p>

      @if (loading()) {
        <z-card class="p-6 rounded-xl shadow-sm"><z-skeleton class="h-64 w-full" /></z-card>
      } @else {
        <div class="grid gap-6 md:grid-cols-2">
          <z-card class="p-6 rounded-xl shadow-sm border border-border/50">
            <h3 class="text-lg font-semibold mb-1 flex items-center gap-2">
              <z-icon zType="activity" class="h-5 w-5 text-primary" />
              Diseases chart
            </h3>
            <p class="text-muted-foreground text-sm mb-4">Number of diagnoses by disease</p>
            <div class="flex flex-col md:flex-row md:items-center gap-4">
              <div class="h-56 w-full md:w-56 shrink-0" style="position: relative;">
                <canvas #topDiseasesCanvas></canvas>
              </div>
              @if (diseaseLegend().length) {
                <ul class="flex-1 space-y-2 text-sm">
                  @for (item of diseaseLegend(); track item.diseaseName) {
                    <li class="flex items-center gap-2">
                      <span class="w-3 h-3 rounded-full shrink-0" [style.background]="item.color"></span>
                      <span class="font-medium truncate">{{ item.diseaseName }}</span>
                      <span class="text-muted-foreground ml-auto shrink-0">{{ item.count }}</span>
                    </li>
                  }
                </ul>
              }
            </div>
          </z-card>
          <div class="space-y-4">
            <z-card class="p-6 rounded-xl shadow-sm border border-border/50">
              <h3 class="text-lg font-semibold mb-1 flex items-center gap-2">
                <z-icon zType="calendar" class="h-5 w-5 text-primary" />
                This month vs last month
              </h3>
              <p class="text-muted-foreground text-sm mb-4">Diagnostics and new folders</p>
              <div class="h-52" style="position: relative;">
                <canvas #comparisonCanvas></canvas>
              </div>
            </z-card>
            <div class="grid grid-cols-2 gap-4">
              <z-card class="p-4 rounded-xl shadow-sm border border-border/50">
                <p class="text-sm text-muted-foreground">Diagnostics this month</p>
                <p class="text-2xl font-bold mt-1">{{ comparisonSummary().thisMonthDiagnostics }}</p>
                <div class="mt-2 h-2 rounded-full bg-muted overflow-hidden">
                  <div class="h-full rounded-full bg-primary" [style.width.%]="comparisonSummary().diagnosticsPercent"></div>
                </div>
                <p class="text-xs text-muted-foreground mt-1">vs last month: {{ comparisonSummary().lastMonthDiagnostics }}</p>
              </z-card>
              <z-card class="p-4 rounded-xl shadow-sm border border-border/50">
                <p class="text-sm text-muted-foreground">New folders this month</p>
                <p class="text-2xl font-bold mt-1">{{ comparisonSummary().thisMonthFolders }}</p>
                <div class="mt-2 h-2 rounded-full bg-muted overflow-hidden">
                  <div class="h-full rounded-full bg-emerald-500" [style.width.%]="comparisonSummary().foldersPercent"></div>
                </div>
                <p class="text-xs text-muted-foreground mt-1">vs last month: {{ comparisonSummary().lastMonthFolders }}</p>
              </z-card>
            </div>
          </div>
        </div>
        <z-card class="p-6 rounded-xl shadow-sm border border-border/50">
          <h3 class="text-lg font-semibold mb-1 flex items-center gap-2">
            <z-icon zType="trending-up" class="h-5 w-5 text-primary" />
            Diagnostics over time
          </h3>
          <p class="text-muted-foreground text-sm mb-4">Number of diagnostics per month ({{ selectedYear() }})</p>
          <div class="h-72" style="position: relative;">
            <canvas #byMonthCanvas></canvas>
          </div>
        </z-card>

        <z-card class="p-6 rounded-xl shadow-sm border border-border/50 bg-gradient-to-br from-background to-destructive/5">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-xl font-bold flex items-center gap-2">
                <z-icon zType="shield" class="h-6 w-6 text-destructive" />
                Clinical Compliance & Safety Audit
              </h3>
              <p class="text-muted-foreground text-sm">Cross-service risk analysis (Diagnostics vs Prescriptions)</p>
            </div>
            <div class="px-3 py-1 bg-destructive/10 text-destructive text-xs font-bold rounded-full animate-pulse">
              LIVE RISK MONITOR
            </div>
          </div>

          <div class="grid gap-4 md:grid-cols-3 mb-8">
            <div class="p-4 rounded-xl border border-border/50 bg-background/50 backdrop-blur-sm">
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-lg bg-emerald-500/10 text-emerald-500">
                  <z-icon zType="check-circle" class="h-5 w-5" />
                </div>
                <span class="text-sm font-medium text-muted-foreground">Treatment Coverage</span>
              </div>
              <div class="flex items-end gap-2">
                <span class="text-3xl font-bold">{{ data()?.safety?.treatmentCoverageRate | number:'1.1-1' }}%</span>
                <span class="text-xs text-muted-foreground mb-1">of diagnostics prescribed</span>
              </div>
              <div class="mt-3 h-1.5 w-full bg-muted rounded-full overflow-hidden">
                <div class="h-full bg-emerald-500 rounded-full" [style.width.%]="data()?.safety?.treatmentCoverageRate"></div>
              </div>
            </div>

            <div class="p-4 rounded-xl border border-border/50 bg-background/50 backdrop-blur-sm">
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-lg bg-orange-500/10 text-orange-500">
                  <z-icon zType="pill" class="h-5 w-5" />
                </div>
                <span class="text-sm font-medium text-muted-foreground">Polypharmacy Risk</span>
              </div>
              <div class="flex items-end gap-2">
                <span class="text-3xl font-bold">{{ data()?.safety?.polypharmacyRiskCount }}</span>
                <span class="text-xs text-muted-foreground mb-1">patients with >5 meds</span>
              </div>
              <p class="text-[10px] text-muted-foreground mt-2 italic">Increased drug interaction probability</p>
            </div>

            <div class="p-4 rounded-xl border border-border/50 bg-background/50 backdrop-blur-sm">
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-lg bg-destructive/10 text-destructive">
                  <z-icon zType="alert-triangle" class="h-5 w-5" />
                </div>
                <span class="text-sm font-medium text-muted-foreground">Chronic Alerts</span>
              </div>
              <div class="flex items-end gap-2">
                <span class="text-3xl font-bold">{{ data()?.safety?.chronicMonitoringAlerts }}</span>
                <span class="text-xs text-muted-foreground mb-1">untreated chronic cases</span>
              </div>
              <p class="text-[10px] text-destructive mt-2 font-medium">Immediate review required</p>
            </div>
          </div>

          @if (data()?.safety?.potentialConflicts?.length) {
            <h4 class="text-sm font-bold uppercase tracking-wider text-muted-foreground mb-4">Medication-Condition Conflict Alerts</h4>
            <div class="overflow-x-auto border border-border/50 rounded-lg">
              <table z-table class="w-full">
                <thead z-table-header class="bg-muted/30">
                  <tr z-table-row>
                    <th z-table-head class="text-xs">Patient</th>
                    <th z-table-head class="text-xs">Prescribed Drug</th>
                    <th z-table-head class="text-xs">Conflicting Condition</th>
                    <th z-table-head class="text-xs">Severity</th>
                  </tr>
                </thead>
                <tbody z-table-body>
                  @for (c of data()?.safety?.potentialConflicts; track $index) {
                    <tr z-table-row class="hover:bg-destructive/5 transition-colors">
                      <td z-table-cell class="font-bold py-2">{{ c.patientId }}</td>
                      <td z-table-cell class="py-2"><span class="px-2 py-0.5 rounded bg-muted text-xs font-mono">{{ c.medicationName }}</span></td>
                      <td z-table-cell class="py-2 text-destructive font-medium">{{ c.conflictingCondition }}</td>
                      <td z-table-cell class="py-2">
                        <span class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                          [style.backgroundColor]="c.severity === 'HIGH' ? '#dc2626' : 'rgba(249, 115, 22, 0.2)'"
                          [style.color]="c.severity === 'HIGH' ? '#fff' : '#ea580c'">
                          {{ c.severity }}
                        </span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="py-12 text-center border-2 border-dashed border-border/50 rounded-xl bg-background/50">
              <z-icon zType="check-circle" class="h-12 w-12 text-emerald-500/30 mx-auto mb-3" />
              <p class="text-muted-foreground text-sm font-medium">No active medication conflicts detected across the patient pool.</p>
              <p class="text-[10px] text-muted-foreground/60 uppercase tracking-widest mt-1">System status: Secure</p>
            </div>
          }
        </z-card>

        <z-card class="p-6 rounded-xl shadow-sm border border-border/50">
          <h3 class="text-lg font-semibold mb-1 flex items-center gap-2">
            <z-icon zType="users" class="h-5 w-5 text-primary" />
            Cross-Patient Disease Analysis
          </h3>
          <p class="text-muted-foreground text-sm mb-4">Search across all dossiers by disease (and optional stage). Export for research.</p>
          <div class="flex flex-wrap items-end gap-3 mb-4">
            <div class="flex flex-col gap-1">
              <label class="text-sm font-medium">Disease name</label>
              <input type="text" class="rounded-lg border border-input bg-background px-3 py-2 text-sm w-48 max-w-full" placeholder="e.g. Diabetes"
                [value]="crossDiseaseName()" (input)="crossDiseaseName.set($any($event.target).value)" />
            </div>
            <div class="flex flex-col gap-1">
              <label class="text-sm font-medium">Stage (optional)</label>
              <input type="text" class="rounded-lg border border-input bg-background px-3 py-2 text-sm w-32 max-w-full" placeholder="e.g. 2"
                [value]="crossStage()" (input)="crossStage.set($any($event.target).value)" />
            </div>
            <button z-button zType="default" zSize="sm" (click)="searchByDisease()" [disabled]="crossPatientLoading() || !crossDiseaseName().trim()">
              Search
            </button>
            @if (crossPatientResults().length > 0) {
              <button z-button zType="outline" zSize="sm" (click)="exportCrossPatientCsv()">Export CSV</button>
              <button z-button zType="outline" zSize="sm" (click)="exportCrossPatientPdf()">Export PDF</button>
            }
          </div>
          @if (crossPatientLoading()) {
            <z-skeleton class="h-24 w-full" />
          } @else if (crossPatientResults().length > 0) {
            <div class="overflow-x-auto">
              <table z-table>
                <thead z-table-header>
                  <tr z-table-row>
                    <th z-table-head>Patient ID</th>
                    <th z-table-head>Doctor ID</th>
                    <th z-table-head>Folder</th>
                    <th z-table-head>Disease</th>
                    <th z-table-head>Stage</th>
                    <th z-table-head>Diagnosis date</th>
                  </tr>
                </thead>
                <tbody z-table-body>
                  @for (r of crossPatientResults(); track r.diagnosticsId) {
                    <tr z-table-row>
                      <td z-table-cell class="font-medium">{{ r.patientId }}</td>
                      <td z-table-cell>{{ r.doctorId }}</td>
                      <td z-table-cell>#{{ r.medicalFolderId }}</td>
                      <td z-table-cell>{{ r.diseaseName }}</td>
                      <td z-table-cell>{{ r.stage ?? '-' }}</td>
                      <td z-table-cell class="text-muted-foreground">{{ r.diagnosisDate | date:'short' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <p class="text-sm text-muted-foreground mt-2">{{ crossPatientResults().length }} result(s)</p>
          } @else if (crossSearched()) {
            <p class="text-muted-foreground text-sm py-4">No results. Try a different disease name or stage.</p>
          }
        </z-card>
      }
    </div>
  `,
})
export class DossierAnalyticsComponent implements OnInit, AfterViewChecked, OnDestroy {
  private readonly analytics = inject(DossierAnalyticsService);
  private readonly pdfService = inject(MedicalFolderPdfService);

  @ViewChild('topDiseasesCanvas') topDiseasesRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('comparisonCanvas') comparisonRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('byMonthCanvas') byMonthRef!: ElementRef<HTMLCanvasElement>;

  loading = signal(true);
  selectedYear = signal(new Date().getFullYear());
  crossDiseaseName = signal('');
  crossStage = signal('');
  crossPatientResults = signal<CrossPatientDisease[]>([]);
  crossPatientLoading = signal(false);
  crossSearched = signal(false);
  private chartTop: Chart | null = null;
  private chartComparison: Chart | null = null;
  private chartByMonth: Chart | null = null;
  private chartsDrawn = false;
  data = signal<{
    diseases: DiseaseCount[];
    comparison: MonthComparison;
    byMonth: DiagnosticsByMonth[];
    safety: ClinicalSafetyStats;
  } | null>(null);

  diseaseLegend = computed(() => {
    const d = this.data();
    if (!d?.diseases?.length) return [];
    const total = d.diseases.reduce((s, x) => s + x.count, 0);
    return d.diseases.map((x, i) => ({
      diseaseName: x.diseaseName,
      count: x.count,
      color: CHART_PALETTE[i % CHART_PALETTE.length],
    }));
  });

  comparisonSummary = computed(() => {
    const d = this.data();
    if (!d?.comparison) {
      return {
        thisMonthDiagnostics: 0,
        lastMonthDiagnostics: 0,
        thisMonthFolders: 0,
        lastMonthFolders: 0,
        diagnosticsPercent: 0,
        foldersPercent: 0,
      };
    }
    const c = d.comparison;
    const diagTotal = c.thisMonthDiagnostics + c.lastMonthDiagnostics;
    const folderTotal = c.thisMonthFolders + c.lastMonthFolders;
    return {
      thisMonthDiagnostics: c.thisMonthDiagnostics,
      lastMonthDiagnostics: c.lastMonthDiagnostics,
      thisMonthFolders: c.thisMonthFolders,
      lastMonthFolders: c.lastMonthFolders,
      diagnosticsPercent: diagTotal ? Math.round((c.thisMonthDiagnostics / diagTotal) * 100) : 0,
      foldersPercent: folderTotal ? Math.round((c.thisMonthFolders / folderTotal) * 100) : 0,
    };
  });

  ngOnInit(): void {
    this.loading.set(true);
    const year = this.selectedYear();
    forkJoin({
      diseases: this.analytics.getTopDiseases(10),
      comparison: this.analytics.getMonthComparison(),
      byMonth: this.analytics.getDiagnosticsByMonth(year),
      safety: this.analytics.getSafetyAudit(),
    }).subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  ngAfterViewChecked(): void {
    const pending = this.data();
    if (this.loading() || this.chartsDrawn || !pending) return;
    if (!this.topDiseasesRef?.nativeElement) return;
    this.drawTopDiseases(pending.diseases);
    this.drawComparison(pending.comparison);
    this.drawByMonth(pending.byMonth);
    this.chartsDrawn = true;
  }

  ngOnDestroy(): void {
    this.chartTop?.destroy();
    this.chartComparison?.destroy();
    this.chartByMonth?.destroy();
  }

  searchByDisease(): void {
    const name = this.crossDiseaseName().trim();
    if (!name) return;
    this.crossPatientLoading.set(true);
    this.crossSearched.set(true);
    this.analytics.getByDisease(name, this.crossStage() || undefined).subscribe({
      next: (list) => {
        this.crossPatientResults.set(list);
        this.crossPatientLoading.set(false);
      },
      error: () => {
        this.crossPatientResults.set([]);
        this.crossPatientLoading.set(false);
      },
    });
  }

  exportCrossPatientCsv(): void {
    const rows = this.crossPatientResults();
    if (!rows.length) return;
    const headers = ['Patient ID', 'Doctor ID', 'Folder ID', 'Disease', 'Stage', 'Diagnosis date'];
    const escape = (v: string | number | null | undefined) => {
      const s = v == null ? '' : String(v);
      return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };
    const lines = [headers.join(','), ...rows.map((r) => [r.patientId, r.doctorId, r.medicalFolderId, r.diseaseName, r.stage ?? '', r.diagnosisDate].map(escape).join(','))];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `cross-patient-disease-${this.crossDiseaseName().trim().replace(/\s+/g, '-')}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  exportCrossPatientPdf(): void {
    const rows = this.crossPatientResults();
    if (!rows.length) return;
    const title = `Cross-Patient: ${this.crossDiseaseName().trim()}${this.crossStage().trim() ? ` (stage ${this.crossStage().trim()})` : ''}`;
    const blob = this.pdfService.exportCrossPatientReport(
      rows.map((r) => ({ patientId: r.patientId, doctorId: r.doctorId, medicalFolderId: r.medicalFolderId, diseaseName: r.diseaseName, stage: r.stage, diagnosisDate: r.diagnosisDate })),
      title
    );
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `cross-patient-disease-report.pdf`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  private drawTopDiseases(data: DiseaseCount[]): void {
    if (!this.topDiseasesRef?.nativeElement || !data.length) return;
    this.chartTop?.destroy();
    const colors = data.map((_, i) => CHART_PALETTE[i % CHART_PALETTE.length]);
    this.chartTop = new Chart(this.topDiseasesRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: data.map((d) => d.diseaseName),
        datasets: [
          {
            data: data.map((d) => d.count),
            backgroundColor: colors,
            borderColor: '#fff',
            borderWidth: 2,
            hoverOffset: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '60%',
        plugins: {
          legend: { display: false },
        },
      },
    });
  }

  private drawComparison(comp: MonthComparison): void {
    if (!this.comparisonRef?.nativeElement) return;
    this.chartComparison?.destroy();
    this.chartComparison = new Chart(this.comparisonRef.nativeElement, {
      type: 'bar',
      data: {
        labels: ['Diagnostics', 'New folders'],
        datasets: [
          {
            label: 'This month',
            data: [comp.thisMonthDiagnostics, comp.thisMonthFolders],
            backgroundColor: 'rgba(234, 88, 12, 0.8)',
            borderColor: 'rgb(234, 88, 12)',
            borderWidth: 1,
          },
          {
            label: 'Last month',
            data: [comp.lastMonthDiagnostics, comp.lastMonthFolders],
            backgroundColor: 'rgba(101, 116, 95, 0.7)',
            borderColor: 'rgb(101, 116, 95)',
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top' } },
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(0,0,0,0.06)' },
          },
          x: {
            grid: { display: false },
          },
        },
      },
    });
  }

  private drawByMonth(byMonth: DiagnosticsByMonth[]): void {
    if (!this.byMonthRef?.nativeElement) return;
    const byMonthKey: Record<number, number> = {};
    for (let m = 1; m <= 12; m++) byMonthKey[m] = 0;
    for (const row of byMonth) {
      byMonthKey[row.month] = (byMonthKey[row.month] || 0) + row.count;
    }
    const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const data = labels.map((_, i) => byMonthKey[i + 1] ?? 0);
    this.chartByMonth?.destroy();
    this.chartByMonth = new Chart(this.byMonthRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Diagnostics',
            data,
            borderColor: '#0d9488',
            backgroundColor: 'rgba(13, 148, 136, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.35,
            pointRadius: 4,
            pointHoverRadius: 6,
            pointBackgroundColor: '#0d9488',
            pointBorderColor: '#fff',
            pointBorderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(0,0,0,0.06)' },
          },
          x: {
            grid: { display: false },
          },
        },
      },
    });
  }
}
