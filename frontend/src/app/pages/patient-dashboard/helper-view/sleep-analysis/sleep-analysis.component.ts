import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  signal,
  inject,
  DestroyRef,
  ChangeDetectionStrategy,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap, Subscription } from 'rxjs';
import { timer } from 'rxjs';
import {
  NgApexchartsModule,
  ChartComponent,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexStroke,
  ApexFill,
  ApexDataLabels,
  ApexTooltip,
  ApexPlotOptions,
  ApexLegend,
  ApexResponsive,
} from 'ng-apexcharts';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { IotService, SleepAnalysisResponse, SleepStageEntry, SleepHistoryResponse, DailySleepEntry } from '@/core/services/iot.service';

@Component({
  selector: 'app-sleep-analysis',
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
    <div class="flex items-center gap-2 mb-6">
      <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
        <z-icon zType="arrow-left" class="mr-1" /> Back
      </button>
      <h2 class="text-2xl font-bold">Sleep Analysis & Live Heartbeat</h2>
    </div>

    <!-- ══ LIVE HEARTBEAT MONITORING ══ -->
    <z-card class="p-5 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold flex items-center gap-2">
          <span class="text-2xl">❤️</span> Live Heartbeat Monitor
        </h3>
        <div class="flex gap-2 items-center">
          @if (isLiveTracking()) {
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 rounded-full bg-green-500 animate-pulse"></div>
              <span class="text-sm font-medium text-green-600 dark:text-green-400">Live</span>
            </div>
            <button z-button zType="outline" zSize="sm" (click)="stopLiveTracking()">
              <z-icon zType="x" class="mr-1" /> Stop
            </button>
          } @else {
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 rounded-full bg-muted-foreground"></div>
              <span class="text-sm text-muted-foreground">Offline</span>
            </div>
            <button z-button zSize="sm" (click)="startLiveTracking()">
              <z-icon zType="activity" class="mr-1" /> Start Tracking
            </button>
          }
        </div>
      </div>

      <!-- Live BPM Display -->
      <div class="flex items-center gap-6 flex-wrap">
        <div class="flex items-center gap-4">
          <div class="relative">
            <div class="w-24 h-24 rounded-full flex items-center justify-center border-4 transition-colors"
              [class]="liveBpmClass()">
              <span class="text-3xl font-bold">{{ liveBpm() ?? '--' }}</span>
            </div>
            @if (isLiveTracking() && liveBpm()) {
              <div class="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-red-500 animate-ping"></div>
              <div class="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-red-500"></div>
            }
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Current BPM</p>
            <p class="text-lg font-semibold" [class]="liveBpmStatusTextClass()">{{ liveBpmStatus() }}</p>
          </div>
        </div>

        <!-- Recent BPM history (mini sparkline) -->
        @if (recentBpmHistory().length > 0) {
          <div class="flex-1 min-w-[200px]">
            <p class="text-xs text-muted-foreground mb-2">Recent readings (last {{ recentBpmHistory().length }})</p>
            <div class="flex items-end gap-1 h-12">
              @for (bpm of recentBpmHistory(); track $index) {
                <div
                  class="flex-1 rounded-t transition-all"
                  [style.height.%]="bpmToBarHeight(bpm)"
                  [class]="bpmToBarColor(bpm)"
                  [title]="bpm + ' BPM'"
                ></div>
              }
            </div>
          </div>
        }

        <!-- Alert Status -->
        @if (liveAlert()) {
          <div class="p-3 rounded-lg bg-red-100 dark:bg-red-900/30 border-2 border-red-400 animate-pulse">
            <div class="flex items-center gap-2">
              <span class="text-2xl">🚨</span>
              <div>
                <p class="font-bold text-red-700 dark:text-red-300 text-sm">{{ liveAlert() }}</p>
                <p class="text-xs text-red-600 dark:text-red-400">Telegram alert sent</p>
              </div>
            </div>
          </div>
        }
      </div>

      @if (liveError()) {
        <p class="text-xs text-destructive mt-3">❌ {{ liveError() }}</p>
      }

      <p class="text-xs text-muted-foreground mt-3">
        Polling dweet.cc thing: <code class="bg-muted px-1.5 py-0.5 rounded text-xs">tfakkarni-high-1</code> every 3s
      </p>
    </z-card>

    <!-- Date picker -->
    <div class="flex items-center gap-3 mb-6">
      <label class="text-sm font-medium text-muted-foreground">Night of:</label>
      <input
        type="date"
        [value]="selectedDate()"
        (change)="onDateChange($event)"
        class="px-3 py-2 rounded-lg border border-input bg-background text-sm"
      />
      <button z-button zType="outline" zSize="sm" (click)="loadAnalysis()">
        <z-icon zType="refresh-cw" class="mr-1 h-4 w-4" /> Refresh
      </button>
    </div>

    @if (isLoading()) {
      <div class="flex flex-col items-center justify-center py-20 gap-4">
        <div class="w-12 h-12 rounded-full border-4 border-indigo-200 border-t-indigo-500 animate-spin"></div>
        <p class="text-muted-foreground">Analyzing sleep data...</p>
      </div>
    } @else if (error()) {
      <z-card class="p-8 text-center">
        <p class="text-4xl mb-3">😴</p>
        <h3 class="text-lg font-semibold mb-2">No Sleep Data</h3>
        <p class="text-muted-foreground">{{ error() }}</p>
      </z-card>
    } @else if (analysis()) {
      <!-- Summary Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <z-card class="p-5">
          <div class="flex items-center gap-3">
            <div class="p-2.5 rounded-xl bg-indigo-100 dark:bg-indigo-900/30">
              <span class="text-2xl">🛏️</span>
            </div>
            <div>
              <p class="text-xs text-muted-foreground font-medium">Total Sleep</p>
              <p class="text-xl font-bold">{{ formatMinutes(analysis()!.summary.totalSleepMinutes) }}</p>
            </div>
          </div>
        </z-card>

        <z-card class="p-5">
          <div class="flex items-center gap-3">
            <div class="p-2.5 rounded-xl bg-emerald-100 dark:bg-emerald-900/30">
              <span class="text-2xl">⚡</span>
            </div>
            <div>
              <p class="text-xs text-muted-foreground font-medium">Efficiency</p>
              <p class="text-xl font-bold">{{ analysis()!.summary.sleepEfficiency }}%</p>
            </div>
          </div>
        </z-card>

        <z-card class="p-5">
          <div class="flex items-center gap-3">
            <div class="p-2.5 rounded-xl bg-amber-100 dark:bg-amber-900/30">
              <span class="text-2xl">⭐</span>
            </div>
            <div>
              <p class="text-xs text-muted-foreground font-medium">Quality Score</p>
              <p class="text-xl font-bold">{{ analysis()!.summary.qualityScore }}<span class="text-sm font-normal text-muted-foreground">/100</span></p>
            </div>
          </div>
        </z-card>

        <z-card class="p-5">
          <div class="flex items-center gap-3">
            <div class="p-2.5 rounded-xl bg-red-100 dark:bg-red-900/30">
              <span class="text-2xl">👁️</span>
            </div>
            <div>
              <p class="text-xs text-muted-foreground font-medium">Awakenings</p>
              <p class="text-xl font-bold">{{ analysis()!.summary.awakenings }}</p>
            </div>
          </div>
        </z-card>
      </div>

      <!-- Quality Badge -->
      <div class="mb-6">
        <z-card class="p-4">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-2xl" [class]="qualityBadgeClass()">
              <span class="text-3xl">{{ qualityEmoji() }}</span>
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Sleep Quality</p>
              <p class="text-2xl font-bold" [class]="qualityTextClass()">{{ analysis()!.summary.qualityLabel }}</p>
            </div>
          </div>
        </z-card>
      </div>

      <!-- Heart Rate Timeline Chart -->
      <z-card class="p-5 mb-6">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <span>❤️</span> Heart Rate Timeline
        </h3>
        <div id="heart-rate-chart">
          <apx-chart
            [series]="heartRateChartSeries()"
            [chart]="heartRateChartOptions"
            [xaxis]="heartRateXAxis()"
            [yaxis]="heartRateYAxis"
            [stroke]="heartRateStroke"
            [fill]="heartRateFill"
            [dataLabels]="noDataLabels"
            [tooltip]="heartRateTooltip"
          />
        </div>
      </z-card>

      <!-- Sleep Stages Timeline Chart -->
      <z-card class="p-5 mb-6">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <span>🌙</span> Sleep Stages Timeline
        </h3>
        <div class="flex gap-4 mb-3 flex-wrap">
          <span class="flex items-center gap-1.5 text-xs font-medium"><span class="w-3 h-3 rounded-sm bg-red-400 inline-block"></span> Awake</span>
          <span class="flex items-center gap-1.5 text-xs font-medium"><span class="w-3 h-3 rounded-sm bg-yellow-400 inline-block"></span> Light</span>
          <span class="flex items-center gap-1.5 text-xs font-medium"><span class="w-3 h-3 rounded-sm bg-blue-500 inline-block"></span> Deep</span>
          <span class="flex items-center gap-1.5 text-xs font-medium"><span class="w-3 h-3 rounded-sm bg-green-500 inline-block"></span> REM</span>
        </div>
        <div id="stages-chart">
          <apx-chart
            [series]="stagesChartSeries()"
            [chart]="stagesChartOptions"
            [xaxis]="stagesXAxis()"
            [yaxis]="stagesYAxis"
            [plotOptions]="stagesPlotOptions"
            [dataLabels]="noDataLabels"
            [tooltip]="stagesTooltip"
          />
        </div>
      </z-card>

      <div class="grid md:grid-cols-2 gap-6 mb-6">
        <!-- Sleep Stage Distribution (Donut) -->
        <z-card class="p-5">
          <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
            <span>🍩</span> Stage Distribution
          </h3>
          <div id="donut-chart">
            <apx-chart
              [series]="donutSeries()"
              [chart]="donutChartOptions"
              [labels]="donutLabels"
              [fill]="donutFill"
              [legend]="donutLegend"
              [dataLabels]="donutDataLabels"
              [responsive]="donutResponsive"
            />
          </div>
        </z-card>

        <!-- Stage Duration Bar Chart -->
        <z-card class="p-5">
          <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
            <span>📊</span> Stage Durations
          </h3>
          <div id="bar-chart">
            <apx-chart
              [series]="barSeries()"
              [chart]="barChartOptions"
              [xaxis]="barXAxis"
              [plotOptions]="barPlotOptions"
              [fill]="barFill"
              [dataLabels]="barDataLabels"
              [tooltip]="barTooltip"
            />
          </div>
        </z-card>
      </div>

      <!-- Insights -->
      <z-card class="p-5 mb-6">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <span>💡</span> Sleep Insights
        </h3>
        <div class="space-y-3">
          @for (insight of analysis()!.insights; track insight) {
            <div class="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
              <span class="text-lg mt-0.5">📌</span>
              <p class="text-sm">{{ insight }}</p>
            </div>
          }
        </div>
      </z-card>
    }

    <!-- ══ SLEEP HISTORY (7-DAY) ══ -->
    <div class="mt-8">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-xl font-bold flex items-center gap-2">
          <z-icon zType="calendar" class="h-5 w-5" /> Sleep History
        </h3>
        <button z-button zType="outline" zSize="sm" (click)="loadHistory()">
          <z-icon zType="refresh-cw" class="mr-1 h-4 w-4" /> Refresh
        </button>
      </div>

      @if (isLoadingHistory()) {
        <div class="flex flex-col items-center justify-center py-12 gap-3">
          <div class="w-10 h-10 rounded-full border-4 border-indigo-200 border-t-indigo-500 animate-spin"></div>
          <p class="text-sm text-muted-foreground">Loading sleep history...</p>
        </div>
      } @else if (sleepHistory()) {
        <!-- Weekly Summary Card -->
        @if (sleepHistory()!.weeklySummary; as ws) {
          <z-card class="p-5 mb-6">
            <div class="flex items-center gap-4 mb-4">
              <div class="p-3 rounded-2xl" [class]="historyQualityBadgeClass(ws.avgQualityLabel)">
                <span class="text-3xl">{{ historyQualityEmoji(ws.avgQualityLabel) }}</span>
              </div>
              <div>
                <p class="text-sm text-muted-foreground">Weekly Average</p>
                <p class="text-2xl font-bold" [class]="historyQualityTextClass(ws.avgQualityLabel)">
                  {{ ws.avgQualityLabel }} — {{ ws.avgQualityScore | number:'1.0-0' }}/100
                </p>
              </div>
              <div class="ml-auto text-right">
                <p class="text-xs text-muted-foreground">{{ ws.nightsWithData }} nights analyzed</p>
                <p class="text-sm font-medium flex items-center gap-1 justify-end">
                  @if (ws.trend === 'IMPROVING') {
                    <span class="text-green-600">↗ Improving</span>
                  } @else if (ws.trend === 'DECLINING') {
                    <span class="text-red-600">↘ Declining</span>
                  } @else {
                    <span class="text-blue-600">→ Stable</span>
                  }
                </p>
              </div>
            </div>

            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="p-3 rounded-lg bg-muted/50 text-center">
                <p class="text-xs text-muted-foreground mb-1">Avg Sleep</p>
                <p class="text-lg font-bold">{{ formatMinutes(ws.avgTotalSleepMinutes) }}</p>
              </div>
              <div class="p-3 rounded-lg bg-muted/50 text-center">
                <p class="text-xs text-muted-foreground mb-1">Avg Deep %</p>
                <p class="text-lg font-bold">{{ ws.avgDeepSleepPercent | number:'1.1-1' }}%</p>
              </div>
              <div class="p-3 rounded-lg bg-muted/50 text-center">
                <p class="text-xs text-muted-foreground mb-1">Avg Efficiency</p>
                <p class="text-lg font-bold">{{ ws.avgEfficiency | number:'1.1-1' }}%</p>
              </div>
              <div class="p-3 rounded-lg bg-muted/50 text-center">
                <p class="text-xs text-muted-foreground mb-1">Total Awakenings</p>
                <p class="text-lg font-bold">{{ ws.totalAwakenings }}</p>
              </div>
            </div>
          </z-card>
        }

        <!-- Quality Trend Chart -->
        @if (historyTrendSeries().length > 0) {
          <z-card class="p-5 mb-6">
            <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
              <span>📈</span> Quality Score Trend
            </h3>
            <div id="history-trend-chart">
              <apx-chart
                [series]="historyTrendSeries()"
                [chart]="historyTrendChartOptions"
                [xaxis]="historyTrendXAxis()"
                [yaxis]="historyTrendYAxis"
                [stroke]="historyTrendStroke"
                [fill]="historyTrendFill"
                [dataLabels]="historyTrendDataLabels"
                [tooltip]="historyTrendTooltip"
              />
            </div>
          </z-card>
        }

        <!-- Nightly Breakdown Table -->
        @if (sleepHistory()!.entries.length > 0) {
          <z-card class="p-5 mb-6">
            <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
              <span>🗓️</span> Nightly Breakdown
            </h3>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-border text-left">
                    <th class="py-3 px-2 font-medium text-muted-foreground">Night</th>
                    <th class="py-3 px-2 font-medium text-muted-foreground">Quality</th>
                    <th class="py-3 px-2 font-medium text-muted-foreground">Total Sleep</th>
                    <th class="py-3 px-2 font-medium text-muted-foreground">Deep %</th>
                    <th class="py-3 px-2 font-medium text-muted-foreground">Efficiency</th>
                    <th class="py-3 px-2 font-medium text-muted-foreground">Awakenings</th>
                  </tr>
                </thead>
                <tbody>
                  @for (entry of sleepHistory()!.entries; track entry.date) {
                    <tr class="border-b border-border/50 hover:bg-muted/30 transition-colors cursor-pointer"
                        (click)="selectHistoryNight(entry.date)">
                      <td class="py-3 px-2 font-medium">{{ entry.date }}</td>
                      <td class="py-3 px-2">
                        <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium"
                          [class]="historyQualityBadgeSmallClass(entry.summary.qualityLabel)">
                          {{ historyQualityEmoji(entry.summary.qualityLabel) }} {{ entry.summary.qualityLabel }}
                          <span class="opacity-70">({{ entry.summary.qualityScore }})</span>
                        </span>
                      </td>
                      <td class="py-3 px-2">{{ formatMinutes(entry.summary.totalSleepMinutes) }}</td>
                      <td class="py-3 px-2">{{ entry.summary.deepSleepPercent | number:'1.1-1' }}%</td>
                      <td class="py-3 px-2">{{ entry.summary.sleepEfficiency | number:'1.1-1' }}%</td>
                      <td class="py-3 px-2">{{ entry.summary.awakenings }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </z-card>
        }

        <!-- Weekly Insights -->
        @if (sleepHistory()!.weeklySummary?.weeklyInsights?.length) {
          <z-card class="p-5">
            <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
              <span>🧠</span> Weekly Insights
            </h3>
            <div class="space-y-3">
              @for (insight of sleepHistory()!.weeklySummary.weeklyInsights; track insight) {
                <div class="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                  <span class="text-lg mt-0.5">📌</span>
                  <p class="text-sm">{{ insight }}</p>
                </div>
              }
            </div>
          </z-card>
        }
      }
    </div>
  `,
})
export class SleepAnalysisComponent implements OnInit, OnDestroy {
  private readonly destroyRef = inject(DestroyRef);
  private readonly iotService = inject(IotService);

  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  // State
  analysis = signal<SleepAnalysisResponse | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  selectedDate = signal('2026-04-06'); // Default to mock data date

  // ── Live tracking state ───────────────────────────────────────────
  isLiveTracking = signal(false);
  liveBpm = signal<number | null>(null);
  liveError = signal('');
  liveAlert = signal<string | null>(null);
  recentBpmHistory = signal<number[]>([]);
  private liveSubscription: Subscription | null = null;
  private readonly HIGH_BPM_THRESHOLD = 120;
  private readonly LOW_BPM_THRESHOLD = 40;
  private alertCooldown = false;

  // ── Chart configs (static) ───────────────────────────────────────

  heartRateChartOptions: ApexChart = { type: 'area', height: 280, toolbar: { show: false }, zoom: { enabled: false } };
  heartRateYAxis: ApexYAxis = { min: 40, max: 95, title: { text: 'BPM' } };
  heartRateStroke: ApexStroke = { curve: 'smooth', width: 2 };
  heartRateFill: ApexFill = { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.5, opacityTo: 0.1 } };
  heartRateTooltip: ApexTooltip = { x: { format: 'HH:mm' }, y: { formatter: (val: number) => `${val} BPM` } };
  noDataLabels: ApexDataLabels = { enabled: false };

  stagesChartOptions: ApexChart = { type: 'bar', height: 200, stacked: true, toolbar: { show: false } };
  stagesYAxis: ApexYAxis = { labels: { show: false } };
  stagesPlotOptions: ApexPlotOptions = { bar: { horizontal: false, columnWidth: '100%' } };
  stagesTooltip: ApexTooltip = { enabled: true };

  donutChartOptions: ApexChart = { type: 'donut', height: 280 };
  donutLabels = ['Deep Sleep', 'Light Sleep', 'REM Sleep', 'Awake'];
  donutFill: ApexFill = { colors: ['#3b82f6', '#facc15', '#22c55e', '#ef4444'] };
  donutLegend: ApexLegend = { position: 'bottom', labels: { useSeriesColors: true } };
  donutDataLabels: ApexDataLabels = { enabled: true, formatter: (val: number) => `${Math.round(val)}%` };
  donutResponsive: ApexResponsive[] = [{ breakpoint: 480, options: { chart: { width: 280 }, legend: { position: 'bottom' } } }];

  barChartOptions: ApexChart = { type: 'bar', height: 280, toolbar: { show: false } };
  barXAxis: ApexXAxis = { categories: ['Deep', 'Light', 'REM', 'Awake'] };
  barPlotOptions: ApexPlotOptions = { bar: { horizontal: true, distributed: true, barHeight: '60%' } };
  barFill: ApexFill = { colors: ['#3b82f6', '#facc15', '#22c55e', '#ef4444'] };
  barDataLabels: ApexDataLabels = { enabled: true, formatter: (val: number) => `${val} min` };
  barTooltip: ApexTooltip = { y: { formatter: (val: number) => `${val} minutes` } };

  // ── Computed chart data (signals) ─────────────────────────────────

  heartRateChartSeries = signal<any[]>([]);
  heartRateXAxis = signal<ApexXAxis>({ type: 'datetime', labels: { datetimeUTC: false, format: 'HH:mm' } });

  stagesChartSeries = signal<any[]>([]);
  stagesXAxis = signal<ApexXAxis>({ type: 'datetime', labels: { datetimeUTC: false, format: 'HH:mm' } });

  donutSeries = signal<number[]>([]);
  barSeries = signal<any[]>([]);

  // ── Sleep History state ──────────────────────────────────────────
  sleepHistory = signal<SleepHistoryResponse | null>(null);
  isLoadingHistory = signal(false);
  historyTrendSeries = signal<any[]>([]);
  historyTrendXAxis = signal<ApexXAxis>({ type: 'category', labels: { rotate: -45 } });

  historyTrendChartOptions: ApexChart = { type: 'line', height: 250, toolbar: { show: false }, zoom: { enabled: false } };
  historyTrendYAxis: ApexYAxis = { min: 0, max: 100, title: { text: 'Quality Score' } };
  historyTrendStroke: ApexStroke = { curve: 'smooth', width: 3 };
  historyTrendFill: ApexFill = { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.1 } };
  historyTrendDataLabels: ApexDataLabels = { enabled: true, formatter: (val: number) => `${val}` };
  historyTrendTooltip: ApexTooltip = { y: { formatter: (val: number) => `${val}/100` } };

  ngOnInit(): void {
    this.loadAnalysis();
    this.loadHistory();
  }

  ngOnDestroy(): void {
    this.stopLiveTracking();
  }

  // ── Live heartbeat tracking ────────────────────────────────────────

  startLiveTracking(): void {
    if (this.isLiveTracking()) return;
    this.isLiveTracking.set(true);
    this.liveError.set('');
    this.liveAlert.set(null);

    const thingName = `tfakkarni-high-1`;

    this.liveSubscription = timer(0, 3000)
      .pipe(
        switchMap(() =>
          this.iotService.getLiveBpmFromDweet(thingName).pipe(
            catchError(err => {
              this.liveError.set(err?.message || 'Failed to read from dweet.cc');
              return of(null);
            }),
          ),
        ),
      )
      .subscribe(bpm => {
        if (bpm !== null && bpm !== undefined) {
          this.liveError.set('');
          this.liveBpm.set(bpm);
          this.recentBpmHistory.update(h => {
            const updated = [...h, bpm];
            return updated.length > 30 ? updated.slice(-30) : updated;
          });

          // Check for alerts — report to backend (triggers Telegram alert)
          if (bpm > this.HIGH_BPM_THRESHOLD && !this.alertCooldown) {
            this.liveAlert.set(`ELEVATED heart rate: ${bpm} BPM (>${this.HIGH_BPM_THRESHOLD})`);
            this.triggerAlertCooldown();
            this.reportBpmToBackend(bpm);
          } else if (bpm < this.LOW_BPM_THRESHOLD && !this.alertCooldown) {
            this.liveAlert.set(`LOW heart rate: ${bpm} BPM (<${this.LOW_BPM_THRESHOLD})`);
            this.triggerAlertCooldown();
            this.reportBpmToBackend(bpm);
          } else if (bpm >= this.LOW_BPM_THRESHOLD && bpm <= this.HIGH_BPM_THRESHOLD) {
            this.liveAlert.set(null);
          }
        }
      });
  }

  stopLiveTracking(): void {
    this.isLiveTracking.set(false);
    this.liveSubscription?.unsubscribe();
    this.liveSubscription = null;
  }

  private triggerAlertCooldown(): void {
    this.alertCooldown = true;
    setTimeout(() => { this.alertCooldown = false; }, 60_000); // 1 min cooldown on UI side
  }

  /** Post abnormal BPM to backend so HeartbeatAlertService sends a Telegram alert. */
  private reportBpmToBackend(bpm: number): void {
    this.iotService
      .recordHeartbeat({ patientId: this.keycloakId, bpm })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err) => console.error('Failed to report BPM to backend', err),
      });
  }

  liveBpmClass(): string {
    const bpm = this.liveBpm();
    if (!bpm) return 'border-muted-foreground text-muted-foreground';
    if (bpm > this.HIGH_BPM_THRESHOLD) return 'border-red-500 text-red-600 bg-red-50 dark:bg-red-900/20';
    if (bpm < this.LOW_BPM_THRESHOLD) return 'border-orange-500 text-orange-600 bg-orange-50 dark:bg-orange-900/20';
    return 'border-green-500 text-green-600 bg-green-50 dark:bg-green-900/20';
  }

  liveBpmStatus(): string {
    const bpm = this.liveBpm();
    if (!bpm) return 'No data';
    if (bpm > this.HIGH_BPM_THRESHOLD) return 'Elevated!';
    if (bpm < this.LOW_BPM_THRESHOLD) return 'Too Low!';
    if (bpm > 100) return 'High Normal';
    if (bpm < 60) return 'Low Normal';
    return 'Normal';
  }

  liveBpmStatusTextClass(): string {
    const bpm = this.liveBpm();
    if (!bpm) return 'text-muted-foreground';
    if (bpm > this.HIGH_BPM_THRESHOLD) return 'text-red-600 dark:text-red-400';
    if (bpm < this.LOW_BPM_THRESHOLD) return 'text-orange-600 dark:text-orange-400';
    return 'text-green-600 dark:text-green-400';
  }

  bpmToBarHeight(bpm: number): number {
    // Scale BPM 40-140 to 10-100%
    return Math.max(10, Math.min(100, ((bpm - 40) / 100) * 100));
  }

  bpmToBarColor(bpm: number): string {
    if (bpm > this.HIGH_BPM_THRESHOLD) return 'bg-red-400';
    if (bpm < this.LOW_BPM_THRESHOLD) return 'bg-orange-400';
    return 'bg-green-400';
  }

  onDateChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedDate.set(input.value);
    this.loadAnalysis();
  }

  loadAnalysis(): void {
    if (!this.keycloakId) return;
    this.isLoading.set(true);
    this.error.set(null);

    this.iotService.getSleepAnalysis(this.keycloakId, this.selectedDate())
      .pipe(
        tap((res) => {
          if (!res.timeline || res.timeline.length === 0) {
            this.error.set('No heartbeat data found for this night. Try selecting a different date.');
            this.analysis.set(null);
            return;
          }
          this.analysis.set(res);
          this.buildCharts(res);
        }),
        catchError((err) => {
          console.error('[SleepAnalysis] Error:', err);
          this.error.set('Failed to load sleep analysis. Make sure the IoT service is running.');
          this.analysis.set(null);
          return of(null);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  formatMinutes(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}h ${m}m`;
  }

  qualityBadgeClass(): string {
    const label = this.analysis()?.summary.qualityLabel;
    switch (label) {
      case 'Excellent': return 'bg-emerald-100 dark:bg-emerald-900/30';
      case 'Good': return 'bg-blue-100 dark:bg-blue-900/30';
      case 'Fair': return 'bg-amber-100 dark:bg-amber-900/30';
      default: return 'bg-red-100 dark:bg-red-900/30';
    }
  }

  qualityTextClass(): string {
    const label = this.analysis()?.summary.qualityLabel;
    switch (label) {
      case 'Excellent': return 'text-emerald-600 dark:text-emerald-400';
      case 'Good': return 'text-blue-600 dark:text-blue-400';
      case 'Fair': return 'text-amber-600 dark:text-amber-400';
      default: return 'text-red-600 dark:text-red-400';
    }
  }

  qualityEmoji(): string {
    const label = this.analysis()?.summary.qualityLabel;
    switch (label) {
      case 'Excellent': return '🌟';
      case 'Good': return '😊';
      case 'Fair': return '😐';
      default: return '😟';
    }
  }

  // ── Sleep History ───────────────────────────────────────────────

  loadHistory(): void {
    if (!this.keycloakId) return;
    this.isLoadingHistory.set(true);

    this.iotService.getSleepHistory(this.keycloakId, 7)
      .pipe(
        tap(res => {
          this.sleepHistory.set(res);
          this.buildHistoryTrendChart(res.entries);
        }),
        catchError(err => {
          console.error('[SleepHistory] Error:', err);
          return of(null);
        }),
        finalize(() => this.isLoadingHistory.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  selectHistoryNight(date: string): void {
    this.selectedDate.set(date);
    this.loadAnalysis();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  historyQualityBadgeClass(label: string): string {
    switch (label) {
      case 'Excellent': return 'bg-emerald-100 dark:bg-emerald-900/30';
      case 'Good': return 'bg-blue-100 dark:bg-blue-900/30';
      case 'Fair': return 'bg-amber-100 dark:bg-amber-900/30';
      default: return 'bg-red-100 dark:bg-red-900/30';
    }
  }

  historyQualityTextClass(label: string): string {
    switch (label) {
      case 'Excellent': return 'text-emerald-600 dark:text-emerald-400';
      case 'Good': return 'text-blue-600 dark:text-blue-400';
      case 'Fair': return 'text-amber-600 dark:text-amber-400';
      default: return 'text-red-600 dark:text-red-400';
    }
  }

  historyQualityEmoji(label: string): string {
    switch (label) {
      case 'Excellent': return '🌟';
      case 'Good': return '😊';
      case 'Fair': return '😐';
      default: return '😟';
    }
  }

  historyQualityBadgeSmallClass(label: string): string {
    switch (label) {
      case 'Excellent': return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400';
      case 'Good': return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'Fair': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
      default: return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
    }
  }

  private buildHistoryTrendChart(entries: DailySleepEntry[]): void {
    if (!entries || entries.length === 0) {
      this.historyTrendSeries.set([]);
      return;
    }

    const data = entries.map(e => e.summary.qualityScore);
    const categories = entries.map(e => e.date);

    this.historyTrendSeries.set([{
      name: 'Quality Score',
      data,
      color: '#6366f1',
    }]);
    this.historyTrendXAxis.set({
      type: 'category',
      categories,
      labels: { rotate: -45 },
    });
  }

  // ── Chart builders ─────────────────────────────────────────────────

  private buildCharts(res: SleepAnalysisResponse): void {
    this.buildHeartRateChart(res.timeline);
    this.buildStagesChart(res.timeline);
    this.buildDonutChart(res.summary);
    this.buildBarChart(res.summary);
  }

  private buildHeartRateChart(timeline: SleepStageEntry[]): void {
    const data = timeline.map(e => ({
      x: new Date(e.timestamp).getTime(),
      y: e.bpm,
    }));

    this.heartRateChartSeries.set([{ name: 'Heart Rate', data, color: '#ef4444' }]);
    this.heartRateXAxis.set({
      type: 'datetime',
      labels: { datetimeUTC: false, format: 'HH:mm' },
    });
  }

  private buildStagesChart(timeline: SleepStageEntry[]): void {
    const stageMap: Record<string, { color: string; y: number }> = {
      AWAKE: { color: '#ef4444', y: 4 },
      REM: { color: '#22c55e', y: 3 },
      LIGHT: { color: '#facc15', y: 2 },
      DEEP: { color: '#3b82f6', y: 1 },
    };

    // Build one bar per stage with y-value representing stage level
    const stages = ['DEEP', 'LIGHT', 'REM', 'AWAKE'];
    const series = stages.map(stage => ({
      name: stage.charAt(0) + stage.slice(1).toLowerCase(),
      data: timeline.map(e => ({
        x: new Date(e.timestamp).getTime(),
        y: e.stage === stage ? stageMap[stage].y : 0,
      })),
      color: stageMap[stage].color,
    }));

    this.stagesChartSeries.set(series);
    this.stagesXAxis.set({
      type: 'datetime',
      labels: { datetimeUTC: false, format: 'HH:mm' },
    });
  }

  private buildDonutChart(summary: any): void {
    this.donutSeries.set([
      summary.deepSleepPercent,
      summary.lightSleepPercent,
      summary.remSleepPercent,
      summary.awakePercent,
    ]);
  }

  private buildBarChart(summary: any): void {
    this.barSeries.set([{
      name: 'Minutes',
      data: [
        summary.deepSleepMinutes,
        summary.lightSleepMinutes,
        summary.remSleepMinutes,
        summary.awakeMinutes,
      ],
    }]);
  }
}
