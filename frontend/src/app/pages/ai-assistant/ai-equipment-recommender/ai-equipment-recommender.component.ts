import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssistantAIService } from '@/core/services/assistant-ai.service';
import {
  EquipmentRecommendRequest,
  EquipmentRecommendResponse,
  EquipmentRecommendation,
} from '@/core/models/assistant-ai.model';

@Component({
  selector: 'app-ai-equipment-recommender',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center gap-3">
        <div class="p-3 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/>
          </svg>
        </div>
        <div>
          <h2 class="text-2xl font-bold bg-gradient-to-r from-cyan-600 to-blue-600 bg-clip-text text-transparent">
            AI Equipment Recommender
          </h2>
          <p class="text-sm text-muted-foreground">Get intelligent equipment recommendations for patient care</p>
        </div>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- Input Panel -->
        <div class="lg:col-span-1">
          <div class="rounded-2xl border border-border bg-card p-6 space-y-5 shadow-sm">
            <h3 class="font-semibold text-lg flex items-center gap-2">
              <span class="text-xl">🏥</span> Patient Info
            </h3>

            <!-- Patient ID -->
            <div class="space-y-2">
              <label class="text-sm font-medium">Patient ID</label>
              <input type="number" [(ngModel)]="patientId"
                class="w-full px-4 py-2.5 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-cyan-500/40 transition-shadow" />
            </div>

            <!-- Condition -->
            <div class="space-y-2">
              <label class="text-sm font-medium">Medical Condition</label>
              <div class="grid grid-cols-2 gap-2">
                @for (c of conditions; track c.value) {
                  <button
                    (click)="condition = c.value"
                    [class]="condition === c.value
                      ? 'py-2.5 px-3 rounded-xl text-xs font-bold bg-cyan-500 text-white shadow-md shadow-cyan-500/25'
                      : 'py-2.5 px-3 rounded-xl text-xs font-medium border border-input bg-background hover:bg-accent transition-colors'">
                    {{ c.emoji }} {{ c.label }}
                  </button>
                }
              </div>
            </div>

            <!-- Severity -->
            <div class="space-y-2">
              <label class="text-sm font-medium">Severity</label>
              <div class="flex gap-2">
                @for (s of severities; track s.value) {
                  <button
                    (click)="severity = s.value"
                    [class]="severity === s.value
                      ? 'flex-1 py-2.5 rounded-xl text-sm font-bold text-white shadow-md transition-all ' + s.activeClass
                      : 'flex-1 py-2.5 rounded-xl text-sm font-medium border border-input bg-background hover:bg-accent transition-colors'">
                    {{ s.label }}
                  </button>
                }
              </div>
            </div>

            <button
              (click)="recommend()"
              [disabled]="isLoading()"
              class="w-full py-3 rounded-xl text-sm font-bold text-white bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-700 hover:to-blue-700 disabled:opacity-50 shadow-lg shadow-cyan-500/25 transition-all flex items-center justify-center gap-2">
              @if (isLoading()) {
                <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Analyzing...
              } @else {
                🔍 Get Recommendations
              }
            </button>
          </div>
        </div>

        <!-- Results -->
        <div class="lg:col-span-2 space-y-4">
          @if (errorMessage()) {
            <div class="p-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
              <strong>Error:</strong> {{ errorMessage() }}
            </div>
          }

          @if (response(); as res) {
            <!-- General advice -->
            <div class="rounded-2xl border border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-900/20 p-5">
              <div class="flex items-start gap-3">
                <span class="text-2xl shrink-0">💡</span>
                <div>
                  <p class="font-semibold text-blue-800 dark:text-blue-200 mb-1">General Advice</p>
                  <p class="text-sm text-blue-700 dark:text-blue-300 leading-relaxed">{{ res.generalAdvice }}</p>
                </div>
              </div>
            </div>

            <!-- Recommendations -->
            <div class="space-y-3">
              @for (rec of res.recommendations; track rec.equipmentId; let i = $index) {
                <div class="rounded-2xl border border-border bg-card p-5 shadow-sm hover:shadow-md transition-shadow">
                  <div class="flex items-start gap-4">
                    <!-- Rank badge -->
                    <div class="shrink-0 w-12 h-12 rounded-2xl flex items-center justify-center text-lg font-black"
                      [class]="i === 0 ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
                        : i === 1 ? 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                        : 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300'">
                      {{ i === 0 ? '🥇' : i === 1 ? '🥈' : '🥉' }}
                    </div>

                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <h4 class="font-bold text-foreground">{{ rec.equipmentName }}</h4>
                        <span class="text-xs px-2 py-0.5 rounded-full bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300 font-semibold">
                          {{ rec.category }}
                        </span>
                      </div>

                      <!-- Relevance bar -->
                      <div class="flex items-center gap-2 mb-3">
                        <div class="flex-1 max-w-[120px] h-2 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                          <div class="h-full rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 transition-all"
                            [style.width.%]="rec.relevanceScore * 100">
                          </div>
                        </div>
                        <span class="text-xs font-semibold text-muted-foreground">{{ (rec.relevanceScore * 100) | number:'1.0-0' }}% match</span>
                      </div>

                      <p class="text-sm text-muted-foreground mb-3 leading-relaxed">{{ rec.justification }}</p>

                      @if (rec.usageInstructions) {
                        <div class="p-3 rounded-xl bg-muted/30 border border-border/50">
                          <p class="text-xs font-semibold text-muted-foreground mb-1">📋 Usage Instructions</p>
                          <p class="text-sm text-foreground leading-relaxed">{{ rec.usageInstructions }}</p>
                        </div>
                      }
                    </div>
                  </div>
                </div>
              }
            </div>
          } @else if (!isLoading()) {
            <div class="rounded-2xl border-2 border-dashed border-border bg-muted/10 p-16 text-center">
              <div class="text-5xl mb-4">🏥</div>
              <h3 class="text-lg font-semibold mb-2 text-foreground">No recommendations yet</h3>
              <p class="text-sm text-muted-foreground max-w-sm mx-auto">
                Select a patient condition and severity, then click "Get Recommendations" for AI-powered suggestions.
              </p>
            </div>
          }

          @if (isLoading()) {
            <div class="space-y-4">
              @for (_ of [1, 2, 3]; track _) {
                <div class="rounded-2xl border border-border bg-card p-5 animate-pulse flex gap-4">
                  <div class="w-12 h-12 bg-muted rounded-2xl shrink-0"></div>
                  <div class="flex-1 space-y-3">
                    <div class="h-4 bg-muted rounded-full w-1/2"></div>
                    <div class="h-2 bg-muted/60 rounded-full w-full"></div>
                    <div class="h-3 bg-muted/40 rounded-full w-3/4"></div>
                  </div>
                </div>
              }
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class AiEquipmentRecommenderComponent {
  private readonly aiService: AssistantAIService;

  patientId = 1;
  condition = 'MOBILITY';
  severity = 'MODERATE';

  isLoading = signal(false);
  response = signal<EquipmentRecommendResponse | null>(null);
  errorMessage = signal<string | null>(null);

  conditions = [
    { value: 'MOBILITY', label: 'Mobility', emoji: '🦽' },
    { value: 'RESPIRATORY', label: 'Respiratory', emoji: '🫁' },
    { value: 'CARDIAC', label: 'Cardiac', emoji: '❤️' },
    { value: 'NEUROLOGICAL', label: 'Neuro', emoji: '🧠' },
    { value: 'ORTHOPEDIC', label: 'Orthopedic', emoji: '🦴' },
    { value: 'DAILY_LIVING', label: 'Daily Living', emoji: '🏠' },
  ];

  severities = [
    { value: 'MILD', label: 'Mild', activeClass: 'bg-emerald-500 shadow-emerald-500/25' },
    { value: 'MODERATE', label: 'Moderate', activeClass: 'bg-amber-500 shadow-amber-500/25' },
    { value: 'SEVERE', label: 'Severe', activeClass: 'bg-red-500 shadow-red-500/25' },
  ];

  constructor(aiService: AssistantAIService) {
    this.aiService = aiService;
  }

  recommend(): void {
    if (this.isLoading()) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.response.set(null);

    const request: EquipmentRecommendRequest = {
      patientId: this.patientId,
      condition: this.condition,
      severity: this.severity,
    };

    this.aiService.recommendEquipment(request).subscribe({
      next: (res) => {
        this.response.set(res);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to get recommendations');
        this.isLoading.set(false);
      },
    });
  }
}
