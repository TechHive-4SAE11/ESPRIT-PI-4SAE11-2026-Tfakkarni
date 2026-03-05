import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZardIconComponent } from '../icon';
import { ZardProgressBarComponent } from '../progress-bar';
import { SymptomPilotResponse, SymptomPrediction } from '@/core/services/symptom-pilot.service';

@Component({
    selector: 'z-symptom-co-pilot',
    standalone: true,
    imports: [CommonModule, ZardIconComponent, ZardProgressBarComponent],
    template: `
    <div class="flex flex-col gap-4 animate-in fade-in slide-in-from-right-4 duration-500">
      <!-- Critical Alert Banner -->
      @if (data()?.isCriticalAlert) {
        <div class="bg-destructive/10 border border-destructive/20 rounded-xl p-4 flex gap-3 animate-pulse shadow-sm shadow-destructive/20">
          <div class="shrink-0 w-10 h-10 rounded-full bg-destructive/20 flex items-center justify-center text-destructive">
            <z-icon zType="circle-alert" size="20"></z-icon>
          </div>
          <div>
            <p class="text-sm font-bold text-destructive uppercase tracking-wide">Urgent Alert</p>
            <p class="text-xs font-semibold text-destructive/90 leading-relaxed">{{ data()?.alertMessage }}</p>
          </div>
        </div>
      }

      <!-- Predictions List -->
      <div class="bg-card/50 border border-border/50 rounded-xl p-4 backdrop-blur-sm">
        <div class="flex items-center gap-2 mb-4 border-b border-border/30 pb-2">
          <z-icon zType="brain" size="16" class="text-primary"></z-icon>
          <span class="text-xs font-bold uppercase tracking-widest text-muted-foreground">AI Predictions</span>
        </div>

        @if (data()?.predictions && data()!.predictions.length > 0) {
          <div class="space-y-4">
            @for (pred of data()?.predictions; track pred.condition) {
              <div class="space-y-1.5 group">
                <div class="flex justify-between items-center px-0.5">
                  <span class="text-xs font-bold group-hover:text-primary transition-colors">{{ pred.condition }}</span>
                  <span class="text-[10px] font-mono font-bold" [ngClass]="getRiskColorClass(pred.riskLevel)">
                    {{ (pred.probability * 100).toFixed(0) }}%
                  </span>
                </div>
                <z-progress-bar 
                  [progress]="pred.probability * 100" 
                  zSize="sm"
                  [barClass]="getProgressBarClass(pred.riskLevel)"
                  class="opacity-90 group-hover:opacity-100 transition-opacity"
                ></z-progress-bar>
              </div>
            }
          </div>
        } @else if (loading()) {
          <div class="py-12 flex flex-col items-center justify-center gap-3 opacity-50">
             <z-icon zType="loader-2" size="20" class="animate-spin text-primary"></z-icon>
             <p class="text-[10px] uppercase font-bold tracking-tighter">Analyzing Symptoms...</p>
          </div>
        } @else {
          <div class="text-center py-8 opacity-40">
            <div class="mx-auto w-8 h-8 rounded-full bg-muted flex items-center justify-center mb-2">
               <z-icon zType="italic" size="14"></z-icon>
            </div>
            <p class="text-[10px] font-medium leading-tight px-4 italic">Start typing symptoms to see real-time AI predictions.</p>
          </div>
        }
      </div>

      <!-- Logic Explanation -->
      <div class="px-2">
         <p class="text-[9px] text-muted-foreground/60 leading-tight">
            <z-icon zType="info" size="8" class="inline mr-1"></z-icon>
            Based on Zero-Shot (BART) analysis. This is a co-pilot tool, not a final diagnosis.
         </p>
      </div>
    </div>
  `,
    styles: [`
    :host {
      display: block;
      width: 100%;
    }
  `]
})
export class SymptomCoPilotComponent {
    data = input<SymptomPilotResponse | null>(null);
    loading = input<boolean>(false);

    getRiskColorClass(risk: string) {
        switch (risk) {
            case 'HIGH': return 'text-destructive';
            case 'MODERATE': return 'text-amber-500';
            default: return 'text-emerald-500';
        }
    }

    getProgressBarClass(risk: string) {
        switch (risk) {
            case 'HIGH': return 'bg-destructive';
            case 'MODERATE': return 'bg-amber-500';
            default: return 'bg-emerald-500';
        }
    }
}
