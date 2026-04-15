import { Component, Input, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardTabComponent, ZardTabGroupComponent } from '@/shared/components/tabs/tabs.component';
import { GameService, type ScoreAnalyticsResponse, type AttemptPoint } from '@/core/services/game.service';
import { AnalyticsService } from '@/core/services/analytics.service';
import { CorrelationStatsResponse, CorrelationPoint, PrescriptionImpactResponse } from '@/core/models/analytics.model';

@Component({
  selector: 'app-patient-analytics',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardProgressBarComponent,
    ZardButtonComponent,
    ZardTabComponent,
    ZardTabGroupComponent,
  ],
  template: `
    <div class="mb-6 flex items-center justify-between">
      <h2 class="text-2xl font-bold tracking-tight">Treatment Efficacy Analytics</h2>
      <button z-button zType="outline" zSize="sm" (click)="loadAll()">
        <z-icon zType="refresh-cw" class="mr-2 h-4 w-4" /> Refresh Data
      </button>
    </div>

    <z-tab-group class="w-full">
      <z-tab label="Prescription Impact">
        <div class="pt-4">
          @if (loadingImpact()) {
             <div class="flex flex-col items-center justify-center min-h-[300px]">
                <z-icon zType="loader-2" class="h-10 w-10 animate-spin text-primary mb-3" />
                <p class="text-muted-foreground">Analyzing prescription changes...</p>
             </div>
          } @else if (impactData()) {
            <z-card class="p-6 overflow-hidden border-none shadow-xl bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-950">
              <div class="flex items-center justify-between mb-8">
                <div>
                  <h3 class="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <span class="p-2 bg-primary/10 rounded-lg">
                      <z-icon zType="activity" class="w-5 h-5 text-primary" />
                    </span>
                    Cognitive Response vs. Medication Adherence
                  </h3>
                  <p class="text-sm text-muted-foreground mt-1">Comparing game performance with medication compliance rates</p>
                </div>
                <div class="flex flex-wrap items-center gap-4">
                  <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-primary/5 border border-primary/10">
                    <span class="w-3 h-3 rounded-full bg-primary"></span>
                    <span class="text-xs font-bold text-slate-600 dark:text-slate-300">GAME SCORE</span>
                  </div>
                  <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-500/5 border border-emerald-500/10">
                    <span class="w-3 h-3 rounded-full bg-emerald-500"></span>
                    <span class="text-xs font-bold text-slate-600 dark:text-slate-300">ADHERENCE</span>
                  </div>
                  <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-red-500/5 border border-red-500/10">
                    <span class="w-4 h-0.5 border-t-2 border-dashed border-red-500"></span>
                    <span class="text-xs font-bold text-slate-600 dark:text-slate-300">NEW TREATMENT</span>
                  </div>
                </div>
              </div>

              <div class="w-full overflow-x-auto custom-scrollbar pb-4">
                 <svg [attr.viewBox]="'-10 -40 1020 380'" class="w-full min-w-[800px] drop-shadow-sm">
                    <!-- Subtle Background Grid -->
                    @for (v of [0, 25, 50, 75, 100]; track v) {
                      <line x1="60" [attr.y1]="impactY(v)" x2="960" [attr.y1]="impactY(v)" 
                            stroke="currentColor" class="text-slate-200 dark:text-slate-800" 
                            stroke-width="1" [attr.stroke-dasharray]="v === 0 ? '0' : '4,4'" />
                      <text x="50" [attr.y]="impactY(v) + 4" text-anchor="end" font-size="11" 
                            class="fill-slate-400 font-medium font-mono">{{v}}%</text>
                    }

                    <!-- Gradient Definition -->
                    <defs>
                      <linearGradient id="impactGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stop-color="hsl(var(--primary))" stop-opacity="0.2" />
                        <stop offset="100%" stop-color="hsl(var(--primary))" stop-opacity="0" />
                      </linearGradient>
                      <linearGradient id="adherenceGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stop-color="#10b981" stop-opacity="0.1" />
                        <stop offset="100%" stop-color="#10b981" stop-opacity="0" />
                      </linearGradient>
                      <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                        <feGaussianBlur stdDeviation="3" result="blur" />
                        <feComposite in="SourceGraphic" in2="blur" operator="over" />
                      </filter>
                    </defs>

                    <!-- Treatment Event Markers (Red Dashed Lines) -->
                    @for (marker of impactData()!.markers; track marker.date) {
                      @let x = impactX(marker.date);
                      <g class="group cursor-pointer">
                         <line [attr.x1]="x" y1="30" [attr.x2]="x" y2="300" 
                               stroke="#f43f5e" stroke-width="2" stroke-dasharray="8,4" 
                               class="opacity-30 group-hover:opacity-100 group-hover:stroke-width-3 transition-all duration-300" />
                         
                         <!-- Tooltip Card that appears on the TOP -->
                         <g class="opacity-0 group-hover:opacity-100 transition-all duration-200 pointer-events-none">
                            <rect [attr.x]="x - 85" y="0" width="170" height="26" rx="6" fill="#f43f5e" filter="url(#glow)" />
                            <text [attr.x]="x" y="17" text-anchor="middle" font-size="9" font-weight="bold" fill="white">
                               {{ marker.description | slice:0:30 }}{{ marker.description.length > 30 ? '...' : '' }}
                            </text>
                            <path [attr.d]="'M ' + (x-5) + ' 26 L ' + x + ' 32 L ' + (x+5) + ' 26 Z'" fill="#f43f5e" />
                         </g>
                      </g>
                    }

                    <!-- Adherence Path (Green) -->
                    <path [attr.d]="adherencePath(true)" fill="url(#adherenceGradient)" />
                    <path [attr.d]="adherencePath(false)" fill="none" stroke="#10b981" 
                          stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="opacity-40" />

                    <!-- The Cognitive Score Path (Purple) -->
                    <path [attr.d]="impactPath(true)" fill="url(#impactGradient)" />
                    <path [attr.d]="impactPath(false)" fill="none" class="stroke-primary" 
                          stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#glow)" />

                    <!-- Interaction Interaction Regions (Rectangles for easier hovering) -->
                    @for (point of impactData()!.impactTimeline; track point.date; let i = $index) {
                      @let sx = impactX(point.date);
                      @let sy = impactY(point.avgScore || 0);
                      @let ay = impactY((point.medAdherence || 0) * 100);

                      <g class="group/point">
                         <!-- Large invisible hover area -->
                         <rect [attr.x]="sx - 15" y="30" width="30" height="270" fill="transparent" class="cursor-pointer" />

                         <!-- Score Circle (if exists) -->
                         @if (point.avgScore !== null && point.avgScore !== undefined) {
                            <circle [attr.cx]="sx" [attr.cy]="sy" r="4" 
                                    class="fill-primary stroke-white dark:stroke-slate-900 group-hover/point:r-6 transition-all duration-200" stroke-width="2" />
                         }

                         <!-- Enhanced Multi-Data Tooltip -->
                         <foreignObject [attr.x]="sx - 65" [attr.y]="Math.min(sy, ay) - 80" width="130" height="75" 
                                        class="opacity-0 group-hover/point:opacity-100 transition-all duration-300 pointer-events-none group-hover/point:-translate-y-2">
                           <div class="bg-slate-900/95 backdrop-blur-md text-white p-2 rounded-xl shadow-2xl border border-white/20">
                             <div class="text-[9px] text-slate-400 font-bold uppercase tracking-wider text-center border-b border-white/10 pb-1 mb-1.5">
                               {{ formatShortDate(point.date) }}
                             </div>
                             <div class="space-y-1 px-1">
                               <div class="flex justify-between items-center">
                                 <span class="text-[9px] font-medium text-primary">Score:</span>
                                 <span class="text-xs font-bold">{{ point.avgScore !== null ? (point.avgScore | number:'1.0-0') + '%' : 'N/A' }}</span>
                               </div>
                               <div class="flex justify-between items-center">
                                 <span class="text-[9px] font-medium text-emerald-400">Adher:</span>
                                 <span class="text-xs font-bold">{{ (point.medAdherence || 0) * 100 | number:'1.0-0' }}%</span>
                               </div>
                             </div>
                           </div>
                         </foreignObject>
                      </g>
                    }

                    <!-- X-Axis Date Labels -->
                    @for (point of impactData()!.impactTimeline; track point.date; let i = $index) {
                      @if (i % (impactData()!.impactTimeline.length > 10 ? 4 : 2) === 0) {
                        <text [attr.x]="impactX(point.date)" y="335" text-anchor="middle" font-size="11" 
                              class="fill-slate-400 dark:fill-slate-500 font-medium">
                          {{ formatShortDate(point.date) }}
                        </text>
                      }
                    }
                 </svg>
              </div>

              <div class="mt-10 grid grid-cols-1 md:grid-cols-2 gap-8">
                 <div class="p-5 rounded-2xl border border-primary/10 bg-primary/5 flex gap-4">
                    <div class="shrink-0 w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                       <z-icon zType="brain" class="text-primary w-5 h-5" />
                    </div>
                    <div>
                      <h4 class="font-bold text-slate-900 dark:text-white mb-1">Clinical Insight</h4>
                      <p class="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                        Compare the <span class="text-primary font-bold">Cognitive Response</span> trend with <span class="text-emerald-500 font-bold">Medication Adherence</span>. 
                        Drops in score often correlate with missed medication (low adherence) or new treatment stabilization periods.
                      </p>
                    </div>
                 </div>
                 
                 <div class="space-y-4">
                    <div class="flex items-center justify-between">
                      <h4 class="font-bold text-slate-900 dark:text-white text-xs uppercase tracking-[0.2em]">Treatment History</h4>
                      <span class="text-[10px] py-0.5 px-2 bg-slate-100 dark:bg-slate-800 rounded font-mono text-slate-500">60 DAY VIEW</span>
                    </div>
                    <div class="grid gap-3 max-h-[160px] overflow-y-auto pr-2 custom-scrollbar">
                      @for (marker of impactData()!.markers.slice().reverse(); track marker.date) {
                        <div class="group flex items-center gap-4 p-3 rounded-xl bg-white dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800 hover:border-primary/30 transition-all shadow-sm hover:shadow-md hover:-translate-y-0.5">
                          <div class="shrink-0 w-10 h-10 bg-red-50 dark:bg-red-900/20 rounded-lg flex items-center justify-center text-red-500 group-hover:scale-110 transition-transform">
                            <z-icon zType="pill" zSize="sm" />
                          </div>
                          <div class="min-w-0 flex-1">
                            <p class="text-sm font-bold text-slate-900 dark:text-white truncate">{{ marker.description }}</p>
                            <div class="flex items-center gap-2 mt-0.5">
                              <z-icon zType="calendar" class="w-3 h-3 text-slate-400" />
                              <span class="text-[11px] font-medium text-slate-500">{{ formatDateShort(marker.date) }}</span>
                            </div>
                          </div>
                        </div>
                      }
                    </div>
                 </div>
              </div>
            </z-card>
          }
        </div>
      </z-tab>
    </z-tab-group>
  `,
})
export class PatientAnalyticsComponent implements OnChanges {
  @Input({ required: true }) patientKeycloakId!: string;

  loading = signal(false);
  loadingImpact = signal(false);
  correlation = signal<CorrelationStatsResponse | null>(null);
  impactData = signal<PrescriptionImpactResponse | null>(null);
  protected readonly Math = Math;

  avgAdherence = computed(() => {
    const timeline = this.correlation()?.correlationTimeline || [];
    if (timeline.length === 0) return 0;
    const sum = timeline.reduce((acc, curr) => acc + (curr.medicationAdherence || 0), 0);
    return sum / timeline.length;
  });

  avgScore = computed(() => {
    const timeline = this.correlation()?.correlationTimeline || [];
    if (timeline.length === 0) return 0;
    const sum = timeline.reduce((acc, curr) => acc + (curr.avgGameScore || 0), 0);
    return sum / timeline.length;
  });

  totalIncidents = computed(() => {
    return this.correlation()?.correlationTimeline.reduce((acc, curr) => acc + (curr.incidentCount || 0), 0) || 0;
  });

  constructor(
    private readonly analyticsService: AnalyticsService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["patientKeycloakId"] && this.patientKeycloakId) {
      this.loadAll();
    }
  }

  loadAll(): void {
    this.loading.set(true);
    this.loadingImpact.set(true);
    this.loadCorrelation();
    this.loadImpact();
  }

  loadCorrelation(): void {
    this.analyticsService.getCorrelationStats(this.patientKeycloakId, 30).subscribe({
      next: data => {
        this.correlation.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.correlation.set(null);
        this.loading.set(false);
      },
    });
  }

  loadImpact(): void {
    this.analyticsService.getPrescriptionImpact(this.patientKeycloakId, 60).subscribe({
      next: data => {
        this.impactData.set(data);
        this.loadingImpact.set(false);
      },
      error: () => {
        this.impactData.set(null);
        this.loadingImpact.set(false);
      }
    });
  }

  corrChartWidth = computed(() => {
    const points = this.correlation()?.correlationTimeline.length || 0;
    return Math.max(600, 60 + 20 + points * 40);
  });

  getCorrX(index: number): number { return 60 + index * 40; }
  getCorrY(percent: number): number { return 260 - (percent / 100) * 230; }

  corrLinePoints = computed(() => {
    const timeline = this.correlation()?.correlationTimeline || [];
    return timeline
      .map((p, i) => `${this.getCorrX(i)},${this.getCorrY(p.avgGameScore || 0)}`)
      .join(" ");
  });

  corrPathArea = computed(() => {
    const timeline = this.correlation()?.correlationTimeline || [];
    if (timeline.length === 0) return "";
    const points = timeline.map((p, i) => `${this.getCorrX(i)},${this.getCorrY(p.avgGameScore || 0)}`).join(" L ");
    const firstX = this.getCorrX(0);
    const lastX = this.getCorrX(timeline.length - 1);
    return `M ${firstX},${this.getCorrY(0)} L ${points} L ${lastX},${this.getCorrY(0)} Z`;
  });

  impactX(dateStr: string): number {
    const data = this.impactData()?.impactTimeline || [];
    const index = data.findIndex(p => p.date === dateStr);
    const width = data.length > 1 ? data.length - 1 : 1;
    return 50 + (index * (900 / width));
  }

  impactY(val: number): number { return 300 - (val / 100) * 250; }

  impactPath(area: boolean): string {
    const timeline = this.impactData()?.impactTimeline || [];
    const points = timeline
      .filter(p => p.avgScore !== null && p.avgScore !== undefined)
      .map(p => `${this.impactX(p.date)},${this.impactY(p.avgScore!)}`);

    if (points.length === 0) return "";
    
    let path = `M ${points.join(" L ")}`;
    if (area) {
      const lastX = this.impactX(timeline[timeline.length-1].date);
      const firstX = this.impactX(timeline[0].date);
      path += ` L ${lastX},300 L ${firstX},300 Z`;
    }
    return path;
  }

  adherencePath(area: boolean): string {
    const timeline = this.impactData()?.impactTimeline || [];
    const points = timeline
      .map(p => `${this.impactX(p.date)},${this.impactY((p.medAdherence || 0) * 100)}`);

    if (points.length === 0) return "";
    
    let path = `M ${points.join(" L ")}`;
    if (area) {
      const lastX = this.impactX(timeline[timeline.length-1].date);
      const firstX = this.impactX(timeline[0].date);
      path += ` L ${lastX},300 L ${firstX},300 Z`;
    }
    return path;
  }

  formatShortDate(date: string): string {
    const d = new Date(date);
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return `${d.getDate()} ${months[d.getMonth()]}`;
  }

  formatDateShort(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
  }

  getScoreAtDate(date: string): number {
    const point = this.impactData()?.impactTimeline.find(p => p.date === date);
    return point?.avgScore ?? 50; // Fallback to middle of chart if no exact match
  }
}
