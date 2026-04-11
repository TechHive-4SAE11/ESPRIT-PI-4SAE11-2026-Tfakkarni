import { Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { QuestionnaireService } from '@/core/services/questionnaire.service';
import { CarePlanService } from '@/core/services/care-plan.service';
import { Questionnaire, QuestionnaireSubmissionDTO, PatientAnswerDTO } from '@/core/models/questionnaire.model';
import { CarePlanResponseDTO } from '@/core/models/care-plan.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { AuthService } from '@/core/auth/auth.service';

@Component({
  selector: 'app-patient-questionnaire',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardBadgeComponent
  ],
  template: `
    <div class="container mx-auto p-4 max-w-4xl">
      <z-card 
        zTitle="Personal Health Questionnaire" 
        zDescription="Answer a few questions to receive personal care recommendations.">
        
        <div *ngIf="isLoading()" class="flex justify-center p-8">
          <z-icon zType="loader-2" class="animate-spin h-8 w-8 text-primary"></z-icon>
        </div>

        <div *ngIf="!isLoading() && questionnaire()" class="space-y-6">
          <div *ngFor="let question of questionnaire()?.questions; let i = index" class="space-y-2">
            <label class="font-medium">{{ i + 1 }}. {{ question.text }}</label>
            <textarea 
              [(ngModel)]="answers[i]" 
              class="w-full p-2 border rounded-md bg-background focus:ring-2 focus:ring-primary outline-none"
              rows="2"
              placeholder="Type your answer here...">
            </textarea>
          </div>
          
          <div class="flex justify-end pt-4">
            <z-button (click)="submit()" [zLoading]="isSubmitting()">
              Get Recommendations
            </z-button>
          </div>
        </div>

        <div *ngIf="recommendation()" class="mt-8 pt-8 border-t space-y-4">
          <h3 class="text-xl font-bold flex items-center gap-2">
            <z-icon zType="brain" class="text-primary"></z-icon>
            Recommended Care Plan
          </h3>
          
          <div class="grid gap-4 sm:grid-cols-2">
            <z-card *ngFor="let activity of recommendation()?.activities" 
                   [zTitle]="activity.activityName"
                   class="bg-muted/30">
              <div class="flex justify-end -mt-10 mb-2">
                <z-badge>{{ activity.activityType }}</z-badge>
              </div>
              <div class="text-sm">
                <p class="text-muted-foreground mb-2">{{ activity.description }}</p>
                <div class="flex items-center gap-4 text-xs">
                  <span class="flex items-center gap-1">
                    <z-icon zType="clock" class="w-3 h-3"></z-icon>
                    {{ activity.duration }}
                  </span>
                  <span class="flex items-center gap-1">
                    <z-icon zType="rotate-ccw" class="w-3 h-3"></z-icon>
                    {{ activity.frequency }}
                  </span>
                </div>
              </div>
            </z-card>
          </div>

          <div class="flex justify-end gap-3 pt-4" card-footer>
            <z-button variant="outline" (click)="reset()">Start Over</z-button>
            <z-button (click)="saveCarePlan()" [zLoading]="isSaving()">Save Care Plan</z-button>
          </div>
        </div>
      </z-card>
    </div>
  `
})
export class PatientQuestionnaireComponent implements OnInit {
  private readonly questionnaireService = inject(QuestionnaireService);
  private readonly carePlanService = inject(CarePlanService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  questionnaire = signal<Questionnaire | null>(null);
  isLoading = signal(true);
  isSubmitting = signal(false);
  isSaving = signal(false);
  recommendation = signal<CarePlanResponseDTO | null>(null);
  
  answers: string[] = [];

  ngOnInit(): void {
    this.loadQuestionnaire();
  }

  loadQuestionnaire(): void {
    this.questionnaireService.getAllQuestionnaires()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (data) => {
          if (data && data.length > 0) {
            this.questionnaire.set(data[0]);
            this.answers = new Array(data[0].questions.length).fill('');
          }
        }
      });
  }

  submit(): void {
    const q = this.questionnaire();
    if (!q) return;

    this.isSubmitting.set(true);
    const submission: QuestionnaireSubmissionDTO = {
      patientId: 1, // Hardcoded for now, should get from auth
      answers: q.questions.map((question, i) => ({
        questionId: question.id,
        answer: this.answers[i]
      }))
    };

    this.questionnaireService.submitAndRecommend(submission)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: (recommendation) => {
          this.recommendation.set(recommendation);
        }
      });
  }

  saveCarePlan(): void {
    const rec = this.recommendation();
    if (!rec) return;

    this.isSaving.set(true);
    // Reuse existing CarePlanRequestDTO structure
    const carePlanRequest = {
      sessionId: 1, // Needs a valid session ID from context
      activities: rec.activities?.map(a => ({
        activityType: a.activityType,
        activityName: a.activityName,
        description: a.description,
        frequency: a.frequency,
        duration: a.duration
      }))
    };

    this.carePlanService.createCarePlan(carePlanRequest as any)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: () => {
          alert('Care Plan saved successfully!');
          this.reset();
        }
      });
  }

  reset(): void {
    this.recommendation.set(null);
    this.answers = new Array(this.questionnaire()?.questions.length || 0).fill('');
  }
}
