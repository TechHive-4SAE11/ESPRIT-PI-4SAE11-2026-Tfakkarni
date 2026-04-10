import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssistantAIService } from '@/core/services/assistant-ai.service';
import { QuizGenerateRequest, GeneratedQuiz } from '@/core/models/assistant-ai.model';

@Component({
  selector: 'app-ai-quiz-generator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center gap-3">
        <div class="p-3 rounded-2xl bg-gradient-to-br from-violet-500 to-fuchsia-500 text-white">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
        </div>
        <div>
          <h2 class="text-2xl font-bold bg-gradient-to-r from-violet-600 to-fuchsia-600 bg-clip-text text-transparent">
            AI Quiz Generator
          </h2>
          <p class="text-sm text-muted-foreground">Generate cognitive assessment quizzes powered by GPT-4</p>
        </div>
      </div>

      <!-- Generation Form -->
      <div class="grid gap-6 lg:grid-cols-3">
        <div class="lg:col-span-1">
          <div class="rounded-2xl border border-border bg-card p-6 space-y-5 shadow-sm">
            <h3 class="font-semibold text-lg flex items-center gap-2">
              <span class="text-xl">⚙️</span> Configuration
            </h3>

            <!-- Topic -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">Topic</label>
              <input type="text"
                [(ngModel)]="topic"
                placeholder="e.g. Fruits et légumes, Animaux, Géographie..."
                class="w-full px-4 py-2.5 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 transition-shadow" />
            </div>

            <!-- Questions Count -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">Number of Questions</label>
              <div class="flex gap-2">
                @for (n of [3, 5, 10]; track n) {
                  <button
                    (click)="numberOfQuestions = n"
                    [class]="numberOfQuestions === n
                      ? 'flex-1 py-2.5 rounded-xl text-sm font-bold bg-primary text-primary-foreground shadow-md shadow-primary/25 transition-all'
                      : 'flex-1 py-2.5 rounded-xl text-sm font-medium border border-input bg-background hover:bg-accent transition-colors'">
                    {{ n }}
                  </button>
                }
              </div>
            </div>

            <!-- Difficulty -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">Difficulty Level</label>
              <div class="flex gap-2">
                @for (d of difficulties; track d.value) {
                  <button
                    (click)="difficultyLevel = d.value"
                    [class]="difficultyLevel === d.value
                      ? 'flex-1 py-2.5 rounded-xl text-sm font-bold text-white shadow-md transition-all ' + d.activeClass
                      : 'flex-1 py-2.5 rounded-xl text-sm font-medium border border-input bg-background hover:bg-accent transition-colors'">
                    {{ d.emoji }} {{ d.label }}
                  </button>
                }
              </div>
            </div>

            <!-- Caregiver ID -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">Caregiver ID</label>
              <input type="number"
                [(ngModel)]="caregiverId"
                placeholder="Patient/Caregiver ID"
                class="w-full px-4 py-2.5 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 transition-shadow" />
            </div>

            <!-- Generate Button -->
            <button
              (click)="generateQuiz()"
              [disabled]="isGenerating() || !topic.trim()"
              class="w-full py-3 rounded-xl text-sm font-bold text-white bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-violet-500/25 transition-all flex items-center justify-center gap-2">
              @if (isGenerating()) {
                <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Generating with AI...
              } @else {
                🧠 Generate Quiz
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

          @if (generatedQuiz(); as quiz) {
            <!-- Success banner -->
            <div class="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800">
              <div class="flex items-center gap-3">
                <span class="text-2xl">✅</span>
                <div>
                  <p class="font-semibold text-emerald-800 dark:text-emerald-200">Quiz Generated Successfully!</p>
                  <p class="text-sm text-emerald-600 dark:text-emerald-400">
                    "{{ quiz.topic }}" — {{ quiz.questions?.length || 0 }} questions • Level {{ quiz.levelReached }} • Saved to game-service (ID: {{ quiz.id }})
                  </p>
                </div>
              </div>
            </div>

            <!-- Questions -->
            @for (q of quiz.questions; track q.id; let i = $index) {
              <div class="rounded-2xl border border-border bg-card p-5 shadow-sm hover:shadow-md transition-shadow">
                <div class="flex items-start justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 text-primary font-bold text-sm">{{ i + 1 }}</span>
                    <span class="text-xs px-2 py-0.5 rounded-full font-semibold"
                      [class]="q.difficultyLevel === 3 ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                        : q.difficultyLevel === 2 ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
                        : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'">
                      Level {{ q.difficultyLevel }}
                    </span>
                  </div>
                </div>
                <p class="font-medium mb-4 text-foreground">{{ q.text }}</p>

                <div class="grid gap-2 sm:grid-cols-2">
                  @for (a of q.answers; track a.text) {
                    <div class="flex items-start gap-2.5 p-3 rounded-xl text-sm transition-colors"
                      [class]="a.isCorrect
                        ? 'bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800'
                        : 'bg-muted/30 border border-transparent'">
                      <span class="shrink-0 mt-0.5">{{ a.isCorrect ? '✅' : '○' }}</span>
                      <div>
                        <p [class]="a.isCorrect ? 'font-semibold text-emerald-700 dark:text-emerald-300' : 'text-muted-foreground'">{{ a.text }}</p>
                        @if (a.explanation && a.isCorrect) {
                          <p class="text-xs text-emerald-600 dark:text-emerald-400 mt-1 italic">{{ a.explanation }}</p>
                        }
                      </div>
                    </div>
                  }
                </div>
              </div>
            }
          } @else if (!isGenerating()) {
            <!-- Empty state -->
            <div class="rounded-2xl border-2 border-dashed border-border bg-muted/10 p-16 text-center">
              <div class="text-5xl mb-4">🧠</div>
              <h3 class="text-lg font-semibold mb-2 text-foreground">No quiz generated yet</h3>
              <p class="text-sm text-muted-foreground max-w-sm mx-auto">
                Configure the quiz parameters on the left and click "Generate Quiz" to create an AI-powered cognitive assessment.
              </p>
            </div>
          }

          @if (isGenerating()) {
            <div class="space-y-4">
              @for (_ of [1, 2, 3]; track _) {
                <div class="rounded-2xl border border-border bg-card p-5 space-y-3 animate-pulse">
                  <div class="h-4 bg-muted rounded-full w-3/4"></div>
                  <div class="grid gap-2 sm:grid-cols-2">
                    <div class="h-12 bg-muted/60 rounded-xl"></div>
                    <div class="h-12 bg-muted/60 rounded-xl"></div>
                    <div class="h-12 bg-muted/60 rounded-xl"></div>
                    <div class="h-12 bg-muted/60 rounded-xl"></div>
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
export class AiQuizGeneratorComponent {
  private readonly aiService: AssistantAIService;

  topic = '';
  numberOfQuestions = 5;
  difficultyLevel = 1;
  caregiverId = 1;

  isGenerating = signal(false);
  generatedQuiz = signal<GeneratedQuiz | null>(null);
  errorMessage = signal<string | null>(null);

  difficulties = [
    { value: 1, label: 'Easy', emoji: '🟢', activeClass: 'bg-emerald-500 shadow-emerald-500/25' },
    { value: 2, label: 'Med', emoji: '🟡', activeClass: 'bg-amber-500 shadow-amber-500/25' },
    { value: 3, label: 'Hard', emoji: '🔴', activeClass: 'bg-red-500 shadow-red-500/25' },
  ];

  constructor(aiService: AssistantAIService) {
    this.aiService = aiService;
  }

  generateQuiz(): void {
    if (!this.topic.trim() || this.isGenerating()) return;

    this.isGenerating.set(true);
    this.errorMessage.set(null);
    this.generatedQuiz.set(null);

    const request: QuizGenerateRequest = {
      topic: this.topic.trim(),
      numberOfQuestions: this.numberOfQuestions,
      difficultyLevel: this.difficultyLevel,
      caregiverId: this.caregiverId,
    };

    this.aiService.generateQuiz(request).subscribe({
      next: (quiz) => {
        this.generatedQuiz.set(quiz);
        this.isGenerating.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || err.message || 'Failed to generate quiz');
        this.isGenerating.set(false);
      },
    });
  }
}
