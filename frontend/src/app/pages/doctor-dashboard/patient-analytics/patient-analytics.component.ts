import { Component, Input, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';
import { ZardButtonComponent } from '@/shared/components/button';
import { GameService, type ScoreAnalyticsResponse, type AttemptPoint } from '@/core/services/game.service';

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
  ],
  template: `
    @if (loading()) {
      <div class="flex flex-col items-center justify-center min-h-[300px]">
        <z-icon zType="loader-2" class="h-10 w-10 animate-spin text-primary mb-3" />
        <p class="text-muted-foreground">Loading analytics...</p>
      </div>
    } @else if (analytics()) {
      <!-- Summary Cards -->
      <div class="grid gap-4 md:grid-cols-4 mb-8">
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Total Games</p>
              <p class="text-3xl font-bold">{{ analytics()!.totalGamesPlayed }}</p>
            </div>
            <z-icon zType="gamepad-2" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Last 7 Days</p>
              <p class="text-3xl font-bold">{{ analytics()!.gamesLast7Days }}</p>
            </div>
            <z-icon zType="calendar" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Avg Score</p>
              <p class="text-3xl font-bold">{{ analytics()!.averageScore | number:'1.0-1' }}%</p>
            </div>
            <z-icon zType="target" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Avg (7 days)</p>
              <p class="text-3xl font-bold">{{ analytics()!.averageScoreLast7Days | number:'1.0-1' }}%</p>
            </div>
            <z-icon zType="activity" class="text-primary h-8 w-8" />
          </div>
        </z-card>
      </div>

      <!-- Score Progression Chart -->
      @if (analytics()!.scoreHistory.length > 0) {
        <z-card class="p-6 mb-8">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold">Score Progression</h3>
            <div class="flex gap-2">
              <button z-button [zType]="chartFilter() === 'all' ? 'default' : 'outline'" zSize="sm"
                      (click)="chartFilter.set('all')">All Time</button>
              <button z-button [zType]="chartFilter() === '7d' ? 'default' : 'outline'" zSize="sm"
                      (click)="chartFilter.set('7d')">Last 7 Days</button>
              <button z-button [zType]="chartFilter() === '30d' ? 'default' : 'outline'" zSize="sm"
                      (click)="chartFilter.set('30d')">Last 30 Days</button>
            </div>
          </div>

          @if (filteredHistory().length > 0) {
            <!-- SVG Line Chart -->
            <div class="w-full overflow-x-auto">
              <svg [attr.viewBox]="'0 0 ' + chartWidth() + ' 280'" class="w-full min-w-[600px]"
                   preserveAspectRatio="xMidYMid meet">
                <!-- Background grid lines -->
                @for (line of gridLines; track line) {
                  <line [attr.x1]="chartPadding.left" [attr.y1]="getY(line)"
                        [attr.x2]="chartWidth() - chartPadding.right" [attr.y2]="getY(line)"
                        stroke="currentColor" class="text-border" stroke-width="1" stroke-dasharray="4,4" />
                  <text [attr.x]="chartPadding.left - 8" [attr.y]="getY(line) + 4"
                        text-anchor="end" class="text-muted-foreground" fill="currentColor" font-size="11">
                    {{ line }}%
                  </text>
                }

                <!-- Line path -->
                <polyline [attr.points]="chartPoints()" fill="none"
                          stroke="hsl(var(--primary))" stroke-width="2.5" stroke-linecap="round"
                          stroke-linejoin="round" />

                <!-- Area fill -->
                <polygon [attr.points]="areaPoints()" fill="hsl(var(--primary))" opacity="0.08" />

                <!-- Data points -->
                @for (point of chartData(); track point.index) {
                  <circle [attr.cx]="point.x" [attr.cy]="point.y" r="4"
                          fill="hsl(var(--primary))" stroke="hsl(var(--background))" stroke-width="2" />

                  <!-- Tooltip on hover (using title) -->
                  <circle [attr.cx]="point.x" [attr.cy]="point.y" r="12"
                          fill="transparent" class="cursor-pointer">
                    <title>{{ point.tooltip }}</title>
                  </circle>
                }

                <!-- X-axis labels (show a subset to avoid crowding) -->
                @for (label of xLabels(); track label.index) {
                  <text [attr.x]="label.x" [attr.y]="260"
                        text-anchor="middle" class="text-muted-foreground" fill="currentColor" font-size="10">
                    {{ label.text }}
                  </text>
                }
              </svg>
            </div>
          } @else {
            <div class="text-center py-12 text-muted-foreground">
              <p>No games played in this period</p>
            </div>
          }
        </z-card>

        <!-- Game Type Breakdown -->
        <z-card class="p-6 mb-8">
          <h3 class="text-lg font-semibold mb-4">Game Type Breakdown</h3>
          <div class="grid gap-3 md:grid-cols-2">
            @for (gt of gameTypeStats(); track gt.type) {
              <div class="flex items-center gap-4 p-4 bg-muted/50 rounded-xl">
                <span class="text-2xl">{{ gt.emoji }}</span>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium">{{ gt.label }}</p>
                  <p class="text-xs text-muted-foreground">{{ gt.count }} games — avg {{ gt.avgScore | number:'1.0-0' }}%</p>
                </div>
                <z-progress-bar [progress]="gt.avgScore" class="w-20 h-2" />
              </div>
            }
          </div>
        </z-card>

        <!-- Recent Attempts Table -->
        <z-card class="p-6">
          <h3 class="text-lg font-semibold mb-4">Recent Activity</h3>
          <div class="space-y-2 max-h-[400px] overflow-y-auto">
            @for (attempt of recentAttempts(); track attempt.attemptId; let i = $index) {
              <div class="flex items-center justify-between p-3 rounded-lg"
                   [class]="i % 2 === 0 ? 'bg-muted/30' : ''">
                <div class="flex items-center gap-3">
                  <span class="text-lg">{{ getTypeEmoji(attempt.gameType) }}</span>
                  <div>
                    <p class="text-sm font-medium">{{ attempt.gameTitle }}</p>
                    <p class="text-xs text-muted-foreground">{{ formatDate(attempt.completedAt) }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <z-badge [zType]="attempt.percentage >= 70 ? 'default' : 'secondary'">
                    {{ attempt.score }}/{{ attempt.totalQuestions }}
                  </z-badge>
                  <span class="text-sm font-bold"
                        [class]="attempt.percentage >= 70 ? 'text-green-600' : attempt.percentage >= 40 ? 'text-yellow-600' : 'text-red-500'">
                    {{ attempt.percentage | number:'1.0-0' }}%
                  </span>
                </div>
              </div>
            }
          </div>
        </z-card>
      } @else {
        <z-card class="p-12 text-center">
          <z-icon zType="gamepad-2" class="w-12 h-12 mx-auto text-muted-foreground mb-4" />
          <h3 class="text-lg font-semibold mb-2">No Game Data Yet</h3>
          <p class="text-muted-foreground">This patient hasn't played any games yet.</p>
        </z-card>
      }
    } @else {
      <z-card class="p-8 text-center text-red-500">
        <p>Failed to load analytics data.</p>
        <button z-button class="mt-4" (click)="loadAnalytics()">Retry</button>
      </z-card>
    }
  `,
})
export class PatientAnalyticsComponent implements OnChanges {
  @Input({ required: true }) patientKeycloakId!: string;

  loading = signal(false);
  analytics = signal<ScoreAnalyticsResponse | null>(null);
  chartFilter = signal<'all' | '7d' | '30d'>('all');

  readonly gridLines = [0, 25, 50, 75, 100];
  readonly chartPadding = { top: 20, right: 20, bottom: 40, left: 50 };
  readonly chartHeight = 220;

  constructor(private readonly gameService: GameService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patientKeycloakId'] && this.patientKeycloakId) {
      this.loadAnalytics();
    }
  }

  loadAnalytics(): void {
    this.loading.set(true);
    this.gameService.getScoreAnalytics(this.patientKeycloakId).subscribe({
      next: data => {
        this.analytics.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.analytics.set(null);
        this.loading.set(false);
      },
    });
  }

  // ── Chart computations ──

  filteredHistory = computed((): AttemptPoint[] => {
    const data = this.analytics();
    if (!data) return [];
    const filter = this.chartFilter();
    if (filter === 'all') return data.scoreHistory;

    const now = new Date();
    const days = filter === '7d' ? 7 : 30;
    const cutoff = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
    return data.scoreHistory.filter(p => new Date(p.completedAt) >= cutoff);
  });

  chartWidth = computed(() => {
    const points = this.filteredHistory().length;
    return Math.max(600, this.chartPadding.left + this.chartPadding.right + points * 50);
  });

  chartData = computed(() => {
    const history = this.filteredHistory();
    if (history.length === 0) return [];
    const usableWidth = this.chartWidth() - this.chartPadding.left - this.chartPadding.right;
    const step = history.length > 1 ? usableWidth / (history.length - 1) : 0;

    return history.map((point, i) => {
      const x = this.chartPadding.left + (history.length > 1 ? i * step : usableWidth / 2);
      const y = this.getY(point.percentage);
      return {
        index: i,
        x,
        y,
        tooltip: `${point.gameTitle}\n${point.score}/${point.totalQuestions} (${Math.round(point.percentage)}%)\n${this.formatDate(point.completedAt)}`,
      };
    });
  });

  chartPoints = computed(() => {
    return this.chartData().map(p => `${p.x},${p.y}`).join(' ');
  });

  areaPoints = computed(() => {
    const data = this.chartData();
    if (data.length === 0) return '';
    const baseY = this.getY(0);
    const top = data.map(p => `${p.x},${p.y}`).join(' ');
    return `${data[0].x},${baseY} ${top} ${data[data.length - 1].x},${baseY}`;
  });

  xLabels = computed(() => {
    const data = this.chartData();
    const history = this.filteredHistory();
    if (data.length === 0) return [];
    // Show at most 10 labels spread evenly
    const maxLabels = 10;
    const step = Math.max(1, Math.ceil(data.length / maxLabels));
    return data
      .filter((_, i) => i % step === 0 || i === data.length - 1)
      .map((point, _, arr) => {
        const h = history[point.index];
        return {
          index: point.index,
          x: point.x,
          text: this.formatShortDate(h.completedAt),
        };
      });
  });

  gameTypeStats = computed(() => {
    const data = this.analytics();
    if (!data) return [];
    const map = new Map<string, { count: number; totalPct: number }>();
    for (const p of data.scoreHistory) {
      const existing = map.get(p.gameType) || { count: 0, totalPct: 0 };
      existing.count++;
      existing.totalPct += p.percentage;
      map.set(p.gameType, existing);
    }
    const emojiMap: Record<string, string> = { MINI: '📷', CUSTOM: '🧩', MOVIE: '🎬', PERSONAL: '🧠' };
    const labelMap: Record<string, string> = { MINI: 'Image Games', CUSTOM: 'Memory Mix', MOVIE: 'Movie Games', PERSONAL: 'Personal Questions' };

    return Array.from(map.entries()).map(([type, stats]) => ({
      type,
      emoji: emojiMap[type] || '📄',
      label: labelMap[type] || type,
      count: stats.count,
      avgScore: stats.count > 0 ? stats.totalPct / stats.count : 0,
    }));
  });

  recentAttempts = computed(() => {
    const data = this.analytics();
    if (!data) return [];
    // Show most recent first, limit to 20
    return [...data.scoreHistory].reverse().slice(0, 20);
  });

  // ── Helpers ──

  getY(percentage: number): number {
    return this.chartPadding.top + this.chartHeight - (percentage / 100) * this.chartHeight;
  }

  getTypeEmoji(type: string): string {
    const map: Record<string, string> = { MINI: '📷', CUSTOM: '🧩', MOVIE: '🎬', PERSONAL: '🧠' };
    return map[type] || '📄';
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  formatShortDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
