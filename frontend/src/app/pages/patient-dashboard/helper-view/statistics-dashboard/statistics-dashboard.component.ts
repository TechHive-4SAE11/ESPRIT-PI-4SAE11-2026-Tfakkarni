import {
  Component,
  Input,
  OnInit,
  signal,
  computed,
  DestroyRef,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, catchError, of } from 'rxjs';

import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexOptions } from 'apexcharts';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardTabComponent, ZardTabGroupComponent } from '@/shared/components/tabs';
import { StatisticsService, type PeriodMode } from '@/core/services/statistics.service';
import type {
  ScoreTrendResponse,
  IncidentStatsResponse,
  MedicationComplianceResponse,
  HydrationTrendResponse,
  ActivityTrendResponse,
  StreakResponse,
} from '@/core/models/statistics.model';

// ─── Palette ────────────────────────────────────────────────────────────────
const C = {
  primary: '#7c3aed',
  success: '#22c55e',
  warning: '#f97316',
  danger:  '#ef4444',
  muted:   '#94a3b8',
  info:    '#38bdf8',
};

// ─── Period options ──────────────────────────────────────────────────────────
interface PeriodOption {
  label: string;
  mode:  PeriodMode;
}

function buildPeriodOptions(): PeriodOption[] {
  const now   = new Date();
  const year  = now.getFullYear();
  const month = now.getMonth();

  const MONTHS_FR = ['Janvier','Février','Mars','Avril','Mai','Juin',
                     'Juillet','Août','Septembre','Octobre','Novembre','Décembre'];

  const opts: PeriodOption[] = [
    { label: '7 derniers jours',  mode: { type: 'days', value: 7  } },
    { label: '30 derniers jours', mode: { type: 'days', value: 30 } },
    { label: `${MONTHS_FR[month]} ${year}`,
      mode: { type: 'current_month' } },
  ];

  for (let i = 1; i <= 6; i++) {
    const m = (month - i + 12) % 12;
    const y = month - i < 0 ? year - 1 : year;
    const firstDay = `${y}-${String(m + 1).padStart(2, '0')}-01`;
    const lastDay  = new Date(y, m + 1, 0);
    const end      = `${y}-${String(m + 1).padStart(2, '0')}-${String(lastDay.getDate()).padStart(2, '0')}`;
    opts.push({
      label: `${MONTHS_FR[m]} ${y}`,
      mode:  { type: 'range', start: firstDay, end },
    });
  }

  return opts;
}

// ─── Component ───────────────────────────────────────────────────────────────

@Component({
  selector: 'app-statistics-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    NgApexchartsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardTabComponent,
    ZardTabGroupComponent,
  ],
  template: `
    <div class="space-y-6">

      <!-- ── Header ── -->
      <div class="flex flex-col gap-4">
        <div>
          <h2 class="text-2xl font-bold tracking-tight">Statistiques & Analytics</h2>
          <p class="text-sm text-muted-foreground mt-1">Vue complète de la santé du patient</p>
        </div>

        <!-- Period selector + refresh -->
        <div class="flex flex-wrap items-center gap-3">
          <select
            class="px-3 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary/50 transition-shadow"
            [value]="selectedPeriodIndex()"
            (change)="onPeriodChange($event)"
          >
            @for (opt of periodOptions; track $index) {
              <option [value]="$index">{{ opt.label }}</option>
            }
          </select>

          <button z-button zType="outline" zSize="sm" (click)="loadAll()" [disabled]="loading()">
            <z-icon
              [zType]="loading() ? 'loader-2' : 'refresh-cw'"
              class="h-4 w-4 mr-1.5"
              [class.animate-spin]="loading()"
            />
            Actualiser
          </button>

          <!-- Streak badge -->
          @if (streakData(); as streak) {
            <div class="ml-auto flex items-center gap-2 px-3 py-1.5 rounded-full bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
              <span class="text-lg">🔥</span>
              <span class="text-sm font-semibold text-amber-700 dark:text-amber-300">{{ streak.currentStreak }} jour{{ streak.currentStreak > 1 ? 's' : '' }}</span>
              <span class="text-xs text-amber-600/70 dark:text-amber-400/70">série</span>
            </div>
          }
        </div>
      </div>

      <!-- ── Loading ── -->
      @if (loading()) {
        <div class="flex flex-col items-center justify-center py-20 gap-4">
          <div class="relative">
            <div class="w-16 h-16 rounded-full border-4 border-primary/20"></div>
            <div class="absolute inset-0 w-16 h-16 rounded-full border-4 border-transparent border-t-primary animate-spin"></div>
          </div>
          <p class="text-sm text-muted-foreground">Chargement des statistiques…</p>
        </div>

      } @else if (error()) {
        <z-card class="p-8 text-center border-destructive/50 bg-destructive/5">
          <z-icon zType="alert-triangle" class="h-12 w-12 text-destructive mx-auto mb-3" />
          <p class="font-semibold text-destructive mb-2">Erreur de chargement</p>
          <p class="text-sm text-muted-foreground mb-4">{{ error() }}</p>
          <button z-button (click)="loadAll()">Réessayer</button>
        </z-card>

      } @else if (noData()) {
        <z-card class="p-12 text-center">
          <z-icon zType="bar-chart-3" class="h-16 w-16 text-muted-foreground mx-auto mb-4 opacity-40" />
          <h3 class="font-semibold text-lg mb-2">Aucune donnée sur cette période</h3>
          <p class="text-muted-foreground text-sm">
            Les graphiques s'afficheront dès que des données du suivi quotidien seront enregistrées.
          </p>
        </z-card>

      } @else {

        <!-- ── Summary cards ── -->
        <div class="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <!-- Score -->
          <div class="rounded-xl border bg-gradient-to-br from-violet-50 to-violet-100/50 dark:from-violet-950/30 dark:to-violet-900/20 p-4 text-center">
            <p class="text-xs font-medium text-violet-600 dark:text-violet-400 uppercase tracking-wider mb-1">Score moyen</p>
            <p class="text-3xl font-bold text-violet-700 dark:text-violet-300">{{ avgScore() }}</p>
            <p class="text-xs text-violet-500/70 mt-0.5">/ 100</p>
          </div>
          <!-- Medications -->
          <div class="rounded-xl border bg-gradient-to-br from-emerald-50 to-emerald-100/50 dark:from-emerald-950/30 dark:to-emerald-900/20 p-4 text-center">
            <p class="text-xs font-medium text-emerald-600 dark:text-emerald-400 uppercase tracking-wider mb-1">Observance</p>
            <p class="text-3xl font-bold text-emerald-700 dark:text-emerald-300">{{ compliancePct() }}%</p>
            <p class="text-xs text-emerald-500/70 mt-0.5">médicaments</p>
          </div>
          <!-- Hydration -->
          <div class="rounded-xl border bg-gradient-to-br from-sky-50 to-sky-100/50 dark:from-sky-950/30 dark:to-sky-900/20 p-4 text-center">
            <p class="text-xs font-medium text-sky-600 dark:text-sky-400 uppercase tracking-wider mb-1">Hydratation</p>
            <p class="text-3xl font-bold text-sky-700 dark:text-sky-300">{{ avgHydration() }}</p>
            <p class="text-xs text-sky-500/70 mt-0.5">ml / jour</p>
          </div>
          <!-- Activity -->
          <div class="rounded-xl border bg-gradient-to-br from-green-50 to-green-100/50 dark:from-green-950/30 dark:to-green-900/20 p-4 text-center">
            <p class="text-xs font-medium text-green-600 dark:text-green-400 uppercase tracking-wider mb-1">Activité</p>
            <p class="text-3xl font-bold text-green-700 dark:text-green-300">{{ avgActivity() }}</p>
            <p class="text-xs text-green-500/70 mt-0.5">min / jour</p>
          </div>
          <!-- Incidents -->
          <div class="rounded-xl border bg-gradient-to-br from-red-50 to-red-100/50 dark:from-red-950/30 dark:to-red-900/20 p-4 text-center col-span-2 lg:col-span-1">
            <p class="text-xs font-medium text-red-600 dark:text-red-400 uppercase tracking-wider mb-1">Incidents</p>
            <p class="text-3xl font-bold text-red-700 dark:text-red-300">{{ totalIncidents() }}</p>
            <p class="text-xs text-red-500/70 mt-0.5">sur la période</p>
          </div>
        </div>

        <!-- ── Tabbed charts ── -->
        <z-tab-group class="w-full">

          <!-- Tab 1: Score Santé -->
          <z-tab label="📊 Score Santé">
            <div class="pt-5">
              <z-card class="p-5 shadow-sm rounded-xl overflow-hidden">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h3 class="font-semibold text-base">Évolution du Score Santé</h3>
                    <p class="text-xs text-muted-foreground mt-0.5">Score global quotidien sur 100 points</p>
                  </div>
                  <span class="text-xs text-muted-foreground bg-muted/40 px-2.5 py-1 rounded-full">{{ periodLabel() }}</span>
                </div>
                @if (scoreChartOptions(); as opt) {
                  <apx-chart
                    [series]="$any(opt).series" [chart]="$any(opt).chart"
                    [xaxis]="$any(opt).xaxis"   [yaxis]="$any(opt).yaxis"
                    [stroke]="$any(opt).stroke" [tooltip]="$any(opt).tooltip"
                    [colors]="$any(opt).colors" [grid]="$any(opt).grid"
                    [markers]="$any(opt).markers" [annotations]="$any(opt).annotations"
                  />
                } @else {
                  <p class="text-sm text-muted-foreground text-center py-8">Aucune donnée disponible</p>
                }
              </z-card>
            </div>
          </z-tab>

          <!-- Tab 2: Médicaments -->
          <z-tab label="💊 Médicaments">
            <div class="pt-5">
              <z-card class="p-5 shadow-sm rounded-xl overflow-hidden">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h3 class="font-semibold text-base">Observance médicamenteuse</h3>
                    <p class="text-xs text-muted-foreground mt-0.5">Ratio médicaments pris vs oubliés</p>
                  </div>
                </div>
                @if (medicationChartOptions(); as opt) {
                  <div class="max-w-sm mx-auto">
                    <apx-chart
                      [series]="$any(opt).series" [chart]="$any(opt).chart"
                      [labels]="$any(opt).labels" [colors]="$any(opt).colors"
                      [legend]="$any(opt).legend" [plotOptions]="$any(opt).plotOptions"
                    />
                  </div>
                } @else {
                  <p class="text-sm text-muted-foreground text-center py-8">Aucune prise enregistrée</p>
                }
              </z-card>
            </div>
          </z-tab>

          <!-- Tab 3: Hydratation -->
          <z-tab label="💧 Hydratation">
            <div class="pt-5">
              <z-card class="p-5 shadow-sm rounded-xl overflow-hidden">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h3 class="font-semibold text-base">Hydratation quotidienne</h3>
                    <p class="text-xs text-muted-foreground mt-0.5">Objectif : 1 500 ml par jour</p>
                  </div>
                </div>
                @if (hydrationChartOptions(); as opt) {
                  <apx-chart
                    [series]="$any(opt).series" [chart]="$any(opt).chart"
                    [xaxis]="$any(opt).xaxis"   [stroke]="$any(opt).stroke"
                    [fill]="$any(opt).fill"     [tooltip]="$any(opt).tooltip"
                    [colors]="$any(opt).colors" [yaxis]="$any(opt).yaxis"
                    [annotations]="$any(opt).annotations"
                  />
                } @else {
                  <p class="text-sm text-muted-foreground text-center py-8">Aucune donnée</p>
                }
              </z-card>
            </div>
          </z-tab>

          <!-- Tab 4: Activité -->
          <z-tab label="🏃 Activité">
            <div class="pt-5">
              <z-card class="p-5 shadow-sm rounded-xl overflow-hidden">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h3 class="font-semibold text-base">Activité physique quotidienne</h3>
                    <p class="text-xs text-muted-foreground mt-0.5">Objectif : 30 minutes par jour</p>
                  </div>
                </div>
                @if (activityChartOptions(); as opt) {
                  <apx-chart
                    [series]="$any(opt).series"      [chart]="$any(opt).chart"
                    [plotOptions]="$any(opt).plotOptions"
                    [xaxis]="$any(opt).xaxis"         [colors]="$any(opt).colors"
                    [tooltip]="$any(opt).tooltip" [annotations]="$any(opt).annotations"
                  />
                } @else {
                  <p class="text-sm text-muted-foreground text-center py-8">Aucune donnée</p>
                }
              </z-card>
            </div>
          </z-tab>

          <!-- Tab 5: Incidents -->
          <z-tab label="⚠️ Incidents">
            <div class="pt-5">
              <z-card class="p-5 shadow-sm rounded-xl overflow-hidden">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h3 class="font-semibold text-base">Incidents par type</h3>
                    <p class="text-xs text-muted-foreground mt-0.5">Chutes, confusion, agitation…</p>
                  </div>
                </div>
                @if (incidentChartOptions(); as opt) {
                  <apx-chart
                    [series]="$any(opt).series"      [chart]="$any(opt).chart"
                    [plotOptions]="$any(opt).plotOptions"
                    [xaxis]="$any(opt).xaxis"         [colors]="$any(opt).colors"
                    [tooltip]="$any(opt).tooltip"
                  />
                } @else {
                  <p class="text-sm text-muted-foreground text-center py-8">Aucun incident sur la période ✓</p>
                }
              </z-card>
            </div>
          </z-tab>

        </z-tab-group>
      }
    </div>
  `,
})
export class StatisticsDashboardComponent implements OnInit {

  @Input() keycloakId = '';

  private readonly statsSvc   = inject(StatisticsService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Période ─────────────────────────────────────────────────────────────────
  readonly periodOptions      = buildPeriodOptions();
  selectedPeriodIndex         = signal(0);

  periodLabel = computed(() => {
    const opt = this.periodOptions[this.selectedPeriodIndex()];
    return opt ? opt.label : '';
  });

  // ── Data ─────────────────────────────────────────────────────────────────────
  loading         = signal(false);
  error           = signal<string | null>(null);
  scoreData       = signal<ScoreTrendResponse       | null>(null);
  incidentData    = signal<IncidentStatsResponse    | null>(null);
  medicationData  = signal<MedicationComplianceResponse | null>(null);
  hydrationData   = signal<HydrationTrendResponse   | null>(null);
  activityData    = signal<ActivityTrendResponse    | null>(null);
  streakData      = signal<StreakResponse | null>(null);

  // ── Summary computeds ───────────────────────────────────────────────────────
  avgScore = computed(() => {
    const s = this.scoreData();
    if (!s?.scores?.length) return 0;
    return Math.round(s.scores.reduce((a, b) => a + b, 0) / s.scores.length);
  });

  compliancePct = computed(() => {
    const m = this.medicationData();
    if (!m || (m.taken + m.missed) === 0) return 0;
    return Math.round((m.taken / (m.taken + m.missed)) * 100);
  });

  avgHydration = computed(() => {
    const h = this.hydrationData();
    if (!h?.values?.length) return 0;
    return Math.round(h.values.reduce((a, b) => a + b, 0) / h.values.length);
  });

  avgActivity = computed(() => {
    const a = this.activityData();
    if (!a?.values?.length) return 0;
    return Math.round(a.values.reduce((a2, b) => a2 + b, 0) / a.values.length);
  });

  totalIncidents = computed(() => {
    const i = this.incidentData();
    if (!i?.values?.length) return 0;
    return i.values.reduce((a, b) => a + b, 0);
  });

  noData = computed(() => {
    const s = this.scoreData();
    const m = this.medicationData();
    const h = this.hydrationData();
    const a = this.activityData();
    const hasScore     = s && s.scores?.some(v => v > 0);
    const hasMed       = m && (m.taken + m.missed) > 0;
    const hasHydration = h && h.values?.some(v => v > 0);
    const hasActivity  = a && a.values?.some(v => v > 0);
    return !hasScore && !hasMed && !hasHydration && !hasActivity;
  });

  // ── Computed chart options ───────────────────────────────────────────────────

  scoreChartOptions = computed((): Partial<ApexOptions> | null => {
    const d = this.scoreData();
    if (!d?.dates?.length) return null;

    const pointColors = d.scores.map(s =>
      s >= 85 ? C.success :
      s >= 65 ? '#84cc16' :
      s >= 45 ? C.warning : C.danger
    );

    return {
      series: [{ name: 'Score', data: d.scores }],
      chart:  { type: 'line', height: 340, animations: { enabled: false }, toolbar: { show: false }, fontFamily: 'inherit' },
      stroke: { curve: 'smooth', width: 3 },
      markers: { size: d.dates.length <= 14 ? 5 : 0, colors: pointColors, strokeWidth: 0 },
      xaxis:  { categories: d.dates, labels: { rotate: -45, style: { fontSize: '11px' } }, tickAmount: Math.min(d.dates.length, 15) },
      yaxis:  { min: 0, max: 100, tickAmount: 5, labels: { formatter: (v: number) => v + '' } },
      colors: [C.primary],
      tooltip: { y: { formatter: (v: number) => v + ' / 100 pts' } },
      grid:   { borderColor: '#e2e8f0', strokeDashArray: 3 },
      annotations: {
        yaxis: [
          { y: 85, borderColor: C.success,  label: { text: 'Excellent', style: { color: C.success  } } },
          { y: 65, borderColor: '#84cc16',  label: { text: 'Stable',    style: { color: '#84cc16'  } } },
          { y: 45, borderColor: C.warning,  label: { text: 'Risque',    style: { color: C.warning  } } },
        ],
      },
    };
  });

  incidentChartOptions = computed((): Partial<ApexOptions> | null => {
    const d = this.incidentData();
    if (!d?.labels?.length) return null;
    return {
      series: [{ name: 'Incidents', data: d.values }],
      chart:  { type: 'bar', height: 300, animations: { enabled: false }, toolbar: { show: false }, fontFamily: 'inherit' },
      plotOptions: { bar: { horizontal: false, columnWidth: '55%', borderRadius: 5 } },
      xaxis:  { categories: d.labels, labels: { style: { fontSize: '11px' } } },
      colors: [C.danger],
      tooltip: { y: { formatter: (v: number) => v + ' incident(s)' } },
    };
  });

  medicationChartOptions = computed((): Partial<ApexOptions> | null => {
    const d = this.medicationData();
    if (!d || (d.taken === 0 && d.missed === 0)) return null;
    const pct = Math.round((d.taken / (d.taken + d.missed)) * 100);
    return {
      series: [d.taken, d.missed],
      chart:  { type: 'donut', height: 320, animations: { enabled: false }, fontFamily: 'inherit' },
      labels: [`Pris (${pct}%)`, `Non pris (${100 - pct}%)`],
      colors: [C.success, C.warning],
      legend: { position: 'bottom', fontSize: '13px' },
      plotOptions: { pie: { donut: { size: '68%', labels: {
        show: true,
        total: { show: true, label: 'Observance', formatter: () => pct + '%', color: pct >= 80 ? C.success : C.warning },
      } } } },
    };
  });

  hydrationChartOptions = computed((): Partial<ApexOptions> | null => {
    const d = this.hydrationData();
    if (!d?.dates?.length) return null;
    return {
      series: [{ name: 'Hydratation (ml)', data: d.values }],
      chart:  { type: 'area', height: 340, animations: { enabled: false }, toolbar: { show: false }, fontFamily: 'inherit' },
      stroke: { curve: 'smooth', width: 2 },
      fill:   { type: 'gradient', gradient: { opacityFrom: 0.4, opacityTo: 0.05 } },
      xaxis:  { categories: d.dates, labels: { rotate: -45, style: { fontSize: '11px' } }, tickAmount: Math.min(d.dates.length, 15) },
      yaxis:  { min: 0, labels: { formatter: (v: number) => v + ' ml' } },
      colors: [C.info],
      tooltip: { y: { formatter: (v: number) => v + ' ml' } },
      annotations: {
        yaxis: [{ y: 1500, borderColor: C.success, strokeDashArray: 5,
                  label: { text: 'Objectif 1500 ml', style: { color: C.success, fontSize: '11px' } } }],
      },
    };
  });

  activityChartOptions = computed((): Partial<ApexOptions> | null => {
    const d = this.activityData();
    if (!d?.dates?.length) return null;
    return {
      series: [{ name: 'Minutes', data: d.values }],
      chart:  { type: 'bar', height: 340, animations: { enabled: false }, toolbar: { show: false }, fontFamily: 'inherit' },
      plotOptions: { bar: { horizontal: false, columnWidth: '55%', borderRadius: 5 } },
      xaxis:  { categories: d.dates, labels: { rotate: -45, style: { fontSize: '11px' } }, tickAmount: Math.min(d.dates.length, 15) },
      colors: [C.success],
      tooltip: { y: { formatter: (v: number) => v + ' min' } },
      annotations: {
        yaxis: [{ y: 30, borderColor: C.primary, strokeDashArray: 5,
                  label: { text: 'Objectif 30 min', style: { color: C.primary, fontSize: '11px' } } }],
      },
    };
  });

  // ── Lifecycle ────────────────────────────────────────────────────────────────

  ngOnInit(): void {
    if (this.keycloakId) this.loadAll();
  }

  onPeriodChange(e: Event): void {
    const idx = parseInt((e.target as HTMLSelectElement).value, 10);
    this.selectedPeriodIndex.set(idx);
    this.loadAll();
  }

  loadAll(): void {
    if (!this.keycloakId) return;

    this.loading.set(true);
    this.error.set(null);

    const mode = this.periodOptions[this.selectedPeriodIndex()].mode;

    forkJoin({
      score:      this.statsSvc.getScoreTrend(this.keycloakId, mode),
      incident:   this.statsSvc.getIncidentTypes(this.keycloakId, mode),
      medication: this.statsSvc.getMedicationCompliance(this.keycloakId, mode),
      hydration:  this.statsSvc.getHydrationTrend(this.keycloakId, mode),
      activity:   this.statsSvc.getActivityTrend(this.keycloakId, mode),
      streak:     this.statsSvc.getStreak(this.keycloakId).pipe(catchError(() => of(null))),
    }).pipe(
      catchError(err => {
        this.error.set(err?.message ?? 'Erreur lors du chargement des statistiques.');
        this.loading.set(false);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(result => {
      this.loading.set(false);
      if (!result) return;
      this.scoreData.set(result.score);
      this.incidentData.set(result.incident);
      this.medicationData.set(result.medication);
      this.hydrationData.set(result.hydration);
      this.activityData.set(result.activity);
      this.streakData.set(result.streak);
    });
  }
}
