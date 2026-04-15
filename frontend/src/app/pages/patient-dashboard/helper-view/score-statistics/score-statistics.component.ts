import {
  Component,
  Input,
  OnInit,
  Output,
  EventEmitter,
  signal,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { NgApexchartsModule } from 'ng-apexcharts';
import type { ApexOptions } from 'apexcharts';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { AnalyticsService } from '@/core/services/analytics.service';
import type { ScoreHistoryEntry, PatientScoreResponse } from '@/core/models/analytics.model';

const COLORS = {
  overall:  '#7c3aed',
  cognitive: '#3b82f6',
  daily:    '#22c55e',
  medical:  '#f97316',
  iot:      '#ef4444',
  engagement: '#06b6d4',
};

@Component({
  selector: 'app-score-statistics',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    NgApexchartsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
  ],
  template: `
    <div class="space-y-6">

      <!-- Back button -->
      <div class="flex items-center gap-2">
        <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
          <z-icon zType="arrow-left" class="mr-1" />
          Retour
        </button>
        <h2 class="text-2xl font-bold tracking-tight">Statistiques complètes du score</h2>
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="flex flex-col items-center justify-center py-20 gap-4">
          <div class="relative">
            <div class="w-16 h-16 rounded-full border-4 border-primary/20"></div>
            <div class="absolute inset-0 w-16 h-16 rounded-full border-4 border-transparent border-t-primary animate-spin"></div>
          </div>
          <p class="text-sm text-muted-foreground">Chargement de l'historique…</p>
        </div>
      } @else if (error()) {
        <z-card class="p-8 text-center border-destructive/50 bg-destructive/5">
          <z-icon zType="alert-triangle" class="h-12 w-12 text-destructive mx-auto mb-3" />
          <p class="font-semibold text-destructive mb-2">Erreur de chargement</p>
          <p class="text-sm text-muted-foreground mb-4">{{ error() }}</p>
          <button z-button (click)="loadHistory()">Réessayer</button>
        </z-card>
      } @else {

        <!-- Current score summary cards -->
        @if (currentScore) {
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
            <div class="rounded-xl border bg-gradient-to-br from-violet-50 to-violet-100/50 dark:from-violet-950/30 dark:to-violet-900/20 p-4 text-center">
              <p class="text-xs font-medium text-violet-600 dark:text-violet-400 uppercase tracking-wider mb-1">Score Global</p>
              <p class="text-3xl font-bold text-violet-700 dark:text-violet-300">{{ currentScore.overallScore | number:'1.0-0' }}</p>
            </div>
            <div class="rounded-xl border bg-gradient-to-br from-blue-50 to-blue-100/50 dark:from-blue-950/30 dark:to-blue-900/20 p-4 text-center">
              <p class="text-xs font-medium text-blue-600 dark:text-blue-400 uppercase tracking-wider mb-1">Cognitif</p>
              <p class="text-3xl font-bold text-blue-700 dark:text-blue-300">{{ currentScore.cognitiveScore | number:'1.0-0' }}</p>
            </div>
            <div class="rounded-xl border bg-gradient-to-br from-green-50 to-green-100/50 dark:from-green-950/30 dark:to-green-900/20 p-4 text-center">
              <p class="text-xs font-medium text-green-600 dark:text-green-400 uppercase tracking-wider mb-1">Quotidien</p>
              <p class="text-3xl font-bold text-green-700 dark:text-green-300">{{ currentScore.dailyFunctioningScore | number:'1.0-0' }}</p>
            </div>
            <div class="rounded-xl border bg-gradient-to-br from-orange-50 to-orange-100/50 dark:from-orange-950/30 dark:to-orange-900/20 p-4 text-center">
              <p class="text-xs font-medium text-orange-600 dark:text-orange-400 uppercase tracking-wider mb-1">Médical</p>
              <p class="text-3xl font-bold text-orange-700 dark:text-orange-300">{{ currentScore.medicalStabilityScore | number:'1.0-0' }}</p>
            </div>
            <div class="rounded-xl border bg-gradient-to-br from-red-50 to-red-100/50 dark:from-red-950/30 dark:to-red-900/20 p-4 text-center">
              <p class="text-xs font-medium text-red-600 dark:text-red-400 uppercase tracking-wider mb-1">IoT</p>
              <p class="text-3xl font-bold text-red-700 dark:text-red-300">{{ currentScore.iotRiskScore | number:'1.0-0' }}</p>
            </div>
            <div class="rounded-xl border bg-gradient-to-br from-cyan-50 to-cyan-100/50 dark:from-cyan-950/30 dark:to-cyan-900/20 p-4 text-center">
              <p class="text-xs font-medium text-cyan-600 dark:text-cyan-400 uppercase tracking-wider mb-1">Engagement</p>
              <p class="text-3xl font-bold text-cyan-700 dark:text-cyan-300">{{ currentScore.engagementScore | number:'1.0-0' }}</p>
            </div>
          </div>
        }

        <!-- Overall score evolution chart -->
        @if (overallChartOptions()) {
          <z-card>
            <div class="p-5">
              <h3 class="text-lg font-semibold mb-1">📈 Évolution du score global</h3>
              <p class="text-sm text-muted-foreground mb-4">Historique sur les {{ dayRange }} derniers jours</p>
              <apx-chart
                [series]="overallChartOptions()!.series!"
                [chart]="overallChartOptions()!.chart!"
                [xaxis]="overallChartOptions()!.xaxis!"
                [yaxis]="overallChartOptions()!.yaxis!"
                [stroke]="overallChartOptions()!.stroke!"
                [colors]="overallChartOptions()!.colors!"
                [tooltip]="overallChartOptions()!.tooltip!"
                [grid]="overallChartOptions()!.grid!"
                [markers]="overallChartOptions()!.markers!"
                [legend]="overallChartOptions()!.legend!"
              />
            </div>
          </z-card>
        }

        <!-- Per-category charts -->
        <div class="grid gap-4 md:grid-cols-2">
          @for (chart of categoryCharts(); track chart.title) {
            <z-card>
              <div class="p-5">
                <h3 class="text-base font-semibold mb-1">{{ chart.emoji }} {{ chart.title }}</h3>
                <p class="text-xs text-muted-foreground mb-3">{{ chart.subtitle }}</p>
                <apx-chart
                  [series]="chart.options.series!"
                  [chart]="chart.options.chart!"
                  [xaxis]="chart.options.xaxis!"
                  [yaxis]="chart.options.yaxis!"
                  [stroke]="chart.options.stroke!"
                  [colors]="chart.options.colors!"
                  [tooltip]="chart.options.tooltip!"
                  [grid]="chart.options.grid!"
                  [fill]="chart.options.fill!"
                  [dataLabels]="chart.options.dataLabels!"
                />
              </div>
            </z-card>
          }
        </div>

        <!-- Radar chart -->
        @if (radarOptions()) {
          <z-card>
            <div class="p-5">
              <h3 class="text-lg font-semibold mb-1">🎯 Répartition actuelle des scores</h3>
              <p class="text-sm text-muted-foreground mb-4">Vue radar des différentes composantes</p>
              <div class="max-w-lg mx-auto">
                <apx-chart
                  [series]="radarOptions()!.series!"
                  [chart]="radarOptions()!.chart!"
                  [xaxis]="radarOptions()!.xaxis!"
                  [yaxis]="radarOptions()!.yaxis!"
                  [colors]="radarOptions()!.colors!"
                  [fill]="radarOptions()!.fill!"
                  [stroke]="radarOptions()!.stroke!"
                  [markers]="radarOptions()!.markers!"
                />
              </div>
            </div>
          </z-card>
        }
      }
    </div>
  `,
})
export class ScoreStatisticsComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);

  @Input({ required: true }) keycloakId = '';
  @Input() currentScore: PatientScoreResponse | null = null;
  @Output() goBack = new EventEmitter<void>();

  loading = signal(false);
  error = signal<string | null>(null);
  history = signal<ScoreHistoryEntry[]>([]);
  dayRange = 90;

  overallChartOptions = computed<ApexOptions | null>(() => {
    const data = this.history();
    if (!data.length) return null;
    return this.buildOverallChart(data);
  });

  categoryCharts = computed(() => {
    const data = this.history();
    if (!data.length) return [];
    return this.buildCategoryCharts(data);
  });

  radarOptions = computed<ApexOptions | null>(() => {
    const score = this.currentScore;
    if (!score) return null;
    return this.buildRadar(score);
  });

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.loading.set(true);
    this.error.set(null);
    this.analyticsService.getScoreHistory(this.keycloakId, this.dayRange).subscribe({
      next: entries => {
        this.history.set(entries.sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime()));
        this.loading.set(false);
      },
      error: err => {
        this.error.set('Impossible de charger l\'historique des scores');
        this.loading.set(false);
      },
    });
  }

  private formatDate(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }

  private buildOverallChart(data: ScoreHistoryEntry[]): ApexOptions {
    const dates = data.map(d => this.formatDate(d.recordedAt));
    return {
      series: [
        { name: 'Global',     data: data.map(d => Math.round(d.overallScore)) },
        { name: 'Cognitif',   data: data.map(d => Math.round(d.cognitiveScore)) },
        { name: 'Quotidien',  data: data.map(d => Math.round(d.dailyFunctioningScore)) },
        { name: 'Médical',    data: data.map(d => Math.round(d.medicalStabilityScore)) },
        { name: 'IoT',        data: data.map(d => Math.round(d.iotRiskScore)) },
      ],
      chart: { type: 'line', height: 350, toolbar: { show: false }, fontFamily: 'inherit' },
      stroke: { width: [3, 2, 2, 2, 2], curve: 'smooth' },
      colors: [COLORS.overall, COLORS.cognitive, COLORS.daily, COLORS.medical, COLORS.iot],
      xaxis: { categories: dates, labels: { style: { fontSize: '11px' } } },
      yaxis: { min: 0, max: 100, labels: { style: { fontSize: '11px' } } },
      tooltip: { shared: true, intersect: false },
      grid: { borderColor: '#e2e8f0', strokeDashArray: 3 },
      markers: { size: 0, hover: { size: 5 } },
      legend: { position: 'top', fontSize: '12px' },
    };
  }

  private buildCategoryCharts(data: ScoreHistoryEntry[]): { title: string; subtitle: string; emoji: string; options: ApexOptions }[] {
    const dates = data.map(d => this.formatDate(d.recordedAt));

    const makeAreaChart = (values: number[], color: string): ApexOptions => ({
      series: [{ name: 'Score', data: values }],
      chart: { type: 'area', height: 220, sparkline: { enabled: false }, toolbar: { show: false }, fontFamily: 'inherit' },
      stroke: { width: 2, curve: 'smooth' },
      colors: [color],
      xaxis: { categories: dates, labels: { show: true, rotate: -45, style: { fontSize: '10px' } } },
      yaxis: { min: 0, max: 100, labels: { style: { fontSize: '10px' } } },
      tooltip: { y: { formatter: (v: number) => `${v}/100` } },
      grid: { borderColor: '#e2e8f0', strokeDashArray: 3 },
      fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 100] } },
      dataLabels: { enabled: false },
    });

    return [
      {
        title: 'Score Cognitif',
        subtitle: 'Performances aux jeux de mémoire',
        emoji: '🧠',
        options: makeAreaChart(data.map(d => Math.round(d.cognitiveScore)), COLORS.cognitive),
      },
      {
        title: 'Fonctionnement Quotidien',
        subtitle: 'Alimentation, hydratation, activité',
        emoji: '🏠',
        options: makeAreaChart(data.map(d => Math.round(d.dailyFunctioningScore)), COLORS.daily),
      },
      {
        title: 'Stabilité Médicale',
        subtitle: 'Observance médicamenteuse & rendez-vous',
        emoji: '💊',
        options: makeAreaChart(data.map(d => Math.round(d.medicalStabilityScore)), COLORS.medical),
      },
      {
        title: 'Risque IoT',
        subtitle: 'Alertes bracelet, chutes, fréquence cardiaque',
        emoji: '📡',
        options: makeAreaChart(data.map(d => Math.round(d.iotRiskScore)), COLORS.iot),
      },
    ];
  }

  private buildRadar(score: PatientScoreResponse): ApexOptions {
    return {
      series: [{
        name: 'Score actuel',
        data: [
          Math.round(score.cognitiveScore),
          Math.round(score.dailyFunctioningScore),
          Math.round(score.medicalStabilityScore),
          Math.round(score.iotRiskScore),
          Math.round(score.engagementScore),
        ],
      }],
      chart: { type: 'radar', height: 350, toolbar: { show: false }, fontFamily: 'inherit' },
      xaxis: { categories: ['Cognitif', 'Quotidien', 'Médical', 'IoT', 'Engagement'] },
      yaxis: { show: false, min: 0, max: 100 },
      colors: [COLORS.overall],
      fill: { opacity: 0.25 },
      stroke: { width: 2 },
      markers: { size: 4 },
    };
  }
}
