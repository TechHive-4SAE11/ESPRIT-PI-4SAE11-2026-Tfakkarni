import { Component, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon';

interface QuizQuestion {
  id: number;
  text: string;
  category: string;
  weight: number;
}

const QUESTIONS: QuizQuestion[] = [
  { id: 1, text: 'Do you frequently forget recent conversations or events?', category: 'Memory', weight: 1 },
  { id: 2, text: 'Do you misplace everyday items (keys, glasses, phone) more than usual?', category: 'Memory', weight: 1 },
  { id: 3, text: 'Do you have difficulty remembering appointments or important dates?', category: 'Memory', weight: 1 },
  { id: 4, text: 'Do you struggle to find the right words during conversations?', category: 'Language', weight: 1 },
  { id: 5, text: 'Do you have trouble following or joining a conversation?', category: 'Language', weight: 1 },
  { id: 6, text: 'Do you find it difficult to plan or solve simple problems (e.g., following a recipe)?', category: 'Executive Function', weight: 1.2 },
  { id: 7, text: 'Do you have trouble managing finances or paying bills on time?', category: 'Executive Function', weight: 1.2 },
  { id: 8, text: 'Do you get lost in familiar places or lose track of directions?', category: 'Orientation', weight: 1.3 },
  { id: 9, text: 'Do you lose track of the date, season, or time of day?', category: 'Orientation', weight: 1.3 },
  { id: 10, text: 'Do you have difficulty completing familiar tasks at home or work?', category: 'Daily Activities', weight: 1.2 },
  { id: 11, text: 'Have you noticed changes in your mood or personality recently (e.g., increased anxiety, confusion, or withdrawal)?', category: 'Behaviour', weight: 1 },
  { id: 12, text: 'Do you find it harder to make decisions than before?', category: 'Executive Function', weight: 1.1 },
  { id: 13, text: 'Do you repeat the same questions or stories without realising it?', category: 'Memory', weight: 1.2 },
  { id: 14, text: 'Do you have trouble recognising faces of people you know well?', category: 'Recognition', weight: 1.4 },
  { id: 15, text: 'Do you feel disoriented when you wake up, unsure of where you are?', category: 'Orientation', weight: 1.3 },
];

type AnswerValue = 'never' | 'sometimes' | 'often' | 'always';

const ANSWER_SCORES: Record<AnswerValue, number> = {
  never: 0,
  sometimes: 1,
  often: 2,
  always: 3,
};

const ANSWER_LABELS: { value: AnswerValue; label: string; emoji: string }[] = [
  { value: 'never', label: 'Never', emoji: '😊' },
  { value: 'sometimes', label: 'Sometimes', emoji: '🤔' },
  { value: 'often', label: 'Often', emoji: '😟' },
  { value: 'always', label: 'Always', emoji: '😰' },
];

@Component({
  selector: 'app-quiz',
  standalone: true,
  imports: [CommonModule, RouterLink, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">

      <!-- Sticky Header -->
      <header class="border-b border-border/40 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm sticky top-0 z-50">
        <div class="container mx-auto flex items-center justify-between px-6 py-4">
          <a routerLink="/landing" class="flex items-center gap-2">
            <span class="text-2xl">🧠</span>
            <span class="text-xl font-bold text-primary">Tfakkarni</span>
          </a>
          <div class="flex items-center gap-3">
            <a routerLink="/landing">
              <button z-button zType="outline" zSize="sm">
                <z-icon zType="arrow-left" class="mr-1" />
                Back to Home
              </button>
            </a>
          </div>
        </div>
      </header>

      <main class="container mx-auto px-6 py-10 max-w-3xl">

        <!-- ─── INTRO SCREEN ─── -->
        @if (phase() === 'intro') {
          <div class="text-center py-12 space-y-6 animate-fade-in">
            <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 text-sm font-medium">
              <span>📋</span>
              <span>Cognitive Self-Assessment</span>
            </div>
            <h1 class="text-4xl md:text-5xl font-bold text-foreground">
              Alzheimer's Self-Assessment Quiz
            </h1>
            <p class="text-lg text-muted-foreground max-w-xl mx-auto">
              This short questionnaire evaluates common early signs of cognitive decline.
              Answer honestly — there are no right or wrong answers.
            </p>

            <div class="flex flex-col items-center gap-4 mt-8">
              <div class="grid grid-cols-3 gap-4 text-center">
                <div class="p-4 rounded-xl bg-white dark:bg-gray-800 border border-border/50 shadow-sm">
                  <p class="text-2xl font-bold text-primary">{{ questions.length }}</p>
                  <p class="text-xs text-muted-foreground">Questions</p>
                </div>
                <div class="p-4 rounded-xl bg-white dark:bg-gray-800 border border-border/50 shadow-sm">
                  <p class="text-2xl font-bold text-primary">5 min</p>
                  <p class="text-xs text-muted-foreground">Duration</p>
                </div>
                <div class="p-4 rounded-xl bg-white dark:bg-gray-800 border border-border/50 shadow-sm">
                  <p class="text-2xl font-bold text-primary">Free</p>
                  <p class="text-xs text-muted-foreground">No Account</p>
                </div>
              </div>
            </div>

            <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl p-5 max-w-lg mx-auto mt-6">
              <p class="text-sm text-muted-foreground">
                <strong class="text-foreground">⚠️ Disclaimer:</strong>
                This quiz is not a medical diagnosis. It is a screening tool to help identify
                potential signs of cognitive decline. If your score is above 60%, we strongly encourage
                you to consult a healthcare professional.
              </p>
            </div>

            <button z-button zSize="lg" class="text-lg px-10 py-6 mt-6" (click)="startQuiz()">
              Start the Quiz
              <z-icon zType="arrow-right" class="ml-2" />
            </button>
          </div>
        }

        <!-- ─── QUIZ QUESTIONS ─── -->
        @if (phase() === 'quiz') {
          <div class="space-y-8">
            <!-- Progress bar -->
            <div class="space-y-2">
              <div class="flex items-center justify-between text-sm text-muted-foreground">
                <span>Question {{ currentIndex() + 1 }} of {{ questions.length }}</span>
                <span>{{ progressPercent() | number:'1.0-0' }}% complete</span>
              </div>
              <div class="h-2 rounded-full bg-muted overflow-hidden">
                <div
                  class="h-full rounded-full bg-primary transition-all duration-500 ease-out"
                  [style.width.%]="progressPercent()"
                ></div>
              </div>
            </div>

            <!-- Category tag -->
            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-medium">
              {{ currentQuestion().category }}
            </div>

            <!-- Question -->
            <div class="bg-white dark:bg-gray-800 rounded-2xl p-8 border border-border/50 shadow-sm">
              <h2 class="text-xl md:text-2xl font-semibold mb-8">
                {{ currentQuestion().text }}
              </h2>

              <!-- Answer options -->
              <div class="grid gap-3">
                @for (answer of answerOptions; track answer.value) {
                  <button
                    class="w-full text-left p-4 rounded-xl border-2 transition-all duration-200"
                    [class]="
                      currentAnswer() === answer.value
                        ? 'border-primary bg-primary/5 dark:bg-primary/10 shadow-sm'
                        : 'border-border/50 bg-white dark:bg-gray-800 hover:border-primary/40 hover:bg-primary/5'
                    "
                    (click)="selectAnswer(answer.value)"
                  >
                    <div class="flex items-center gap-3">
                      <span class="text-2xl">{{ answer.emoji }}</span>
                      <span class="font-medium">{{ answer.label }}</span>
                      @if (currentAnswer() === answer.value) {
                        <z-icon zType="check" class="ml-auto h-5 w-5 text-primary" />
                      }
                    </div>
                  </button>
                }
              </div>
            </div>

            <!-- Navigation -->
            <div class="flex items-center justify-between">
              <button
                z-button
                zType="outline"
                [disabled]="currentIndex() === 0"
                (click)="previousQuestion()"
              >
                <z-icon zType="arrow-left" class="mr-1" />
                Previous
              </button>
              <button
                z-button
                [disabled]="!currentAnswer()"
                (click)="nextQuestion()"
              >
                {{ currentIndex() === questions.length - 1 ? 'See Results' : 'Next' }}
                <z-icon zType="arrow-right" class="ml-1" />
              </button>
            </div>
          </div>
        }

        <!-- ─── RESULTS SCREEN ─── -->
        @if (phase() === 'results') {
          <div class="space-y-8 animate-fade-in">
            <div class="text-center space-y-3">
              <h1 class="text-3xl md:text-4xl font-bold">Your Results</h1>
              <p class="text-muted-foreground">Based on your self-assessment answers</p>
            </div>

            <!-- Score circle -->
            <div class="flex justify-center">
              <div class="relative w-52 h-52">
                <svg class="w-full h-full" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="45" fill="none" stroke="hsl(var(--muted))" stroke-width="8" />
                  <circle
                    cx="50" cy="50" r="45"
                    fill="none"
                    [attr.stroke]="scoreColor()"
                    stroke-width="8"
                    stroke-linecap="round"
                    [attr.stroke-dasharray]="circumference"
                    [attr.stroke-dashoffset]="strokeOffset()"
                    transform="rotate(-90 50 50)"
                    class="transition-all duration-1000 ease-out"
                  />
                </svg>
                <div class="absolute inset-0 flex flex-col items-center justify-center">
                  <span class="text-4xl font-extrabold" [style.color]="scoreColor()">
                    {{ score() | number:'1.0-0' }}%
                  </span>
                  <span class="text-sm text-muted-foreground mt-1 font-medium">
                    {{ riskLevel() }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Interpretation -->
            <div
              class="rounded-xl p-6 border"
              [class]="score() >= 60
                ? 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
                : score() >= 35
                  ? 'bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-800'
                  : 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800'"
            >
              @if (score() >= 60) {
                <h3 class="font-bold text-lg text-red-700 dark:text-red-400 mb-2">⚠️ Elevated Risk Detected</h3>
                <p class="text-sm text-muted-foreground mb-3">
                  Your responses suggest a higher-than-average risk of cognitive decline.
                  We strongly encourage you to consult a healthcare professional for a
                  thorough evaluation.
                </p>
                <p class="text-sm text-muted-foreground">
                  You can create an account on Tfakkarni to start your care journey.
                  A doctor will review your case and create your personalized medical folder.
                </p>
              } @else if (score() >= 35) {
                <h3 class="font-bold text-lg text-amber-700 dark:text-amber-400 mb-2">🔶 Moderate Indicators</h3>
                <p class="text-sm text-muted-foreground">
                  Some of your responses indicate mild cognitive changes that may be
                  worth monitoring. Consider discussing these with your doctor during
                  your next check-up.
                </p>
              } @else {
                <h3 class="font-bold text-lg text-green-700 dark:text-green-400 mb-2">✅ Low Risk</h3>
                <p class="text-sm text-muted-foreground">
                  Your responses suggest a low risk of cognitive decline at this time.
                  Continue maintaining a healthy lifestyle with regular exercise,
                  social engagement, and mental stimulation.
                </p>
              }
            </div>

            <!-- Category breakdown -->
            <div class="bg-white dark:bg-gray-800 rounded-xl p-6 border border-border/50">
              <h3 class="font-semibold mb-4">Breakdown by Category</h3>
              <div class="space-y-3">
                @for (cat of categoryScores(); track cat.category) {
                  <div>
                    <div class="flex items-center justify-between text-sm mb-1">
                      <span class="font-medium">{{ cat.category }}</span>
                      <span class="text-muted-foreground">{{ cat.percent | number:'1.0-0' }}%</span>
                    </div>
                    <div class="h-2 rounded-full bg-muted overflow-hidden">
                      <div
                        class="h-full rounded-full transition-all duration-700 ease-out"
                        [style.width.%]="cat.percent"
                        [style.backgroundColor]="cat.percent >= 60 ? '#ef4444' : cat.percent >= 35 ? '#f59e0b' : '#22c55e'"
                      ></div>
                    </div>
                  </div>
                }
              </div>
            </div>

            <!-- CTAs -->
            <div class="flex flex-wrap items-center justify-center gap-4">
              @if (score() >= 60) {
                <a routerLink="/signup">
                  <button z-button zSize="lg" class="text-lg px-8 py-6">
                    Create Your Care Account
                    <z-icon zType="arrow-right" class="ml-2" />
                  </button>
                </a>
              }
              <a routerLink="/landing">
                <button z-button zType="outline" zSize="lg">
                  Back to Home
                </button>
              </a>
              <button z-button zType="outline" zSize="lg" (click)="retakeQuiz()">
                <z-icon zType="rotate-ccw" class="mr-2" />
                Retake Quiz
              </button>
            </div>

            <div class="text-center text-xs text-muted-foreground mt-4">
              <p>This self-assessment is for informational purposes only and does not constitute medical advice.</p>
            </div>
          </div>
        }

      </main>
    </div>
  `,
  styles: [`
    .animate-fade-in {
      animation: fadeIn 0.5s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
})
export class QuizComponent {
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  readonly questions = QUESTIONS;
  readonly answerOptions = ANSWER_LABELS;
  readonly circumference = 2 * Math.PI * 45;

  phase = signal<'intro' | 'quiz' | 'results'>('intro');
  currentIndex = signal(0);
  answers = signal<Map<number, AnswerValue>>(new Map());

  currentQuestion = computed(() => this.questions[this.currentIndex()]);
  currentAnswer = computed(() => this.answers().get(this.currentQuestion().id) ?? null);
  progressPercent = computed(() => {
    const answered = this.answers().size;
    return (answered / this.questions.length) * 100;
  });

  score = computed(() => {
    const ans = this.answers();
    if (ans.size === 0) return 0;
    let totalWeighted = 0;
    let maxWeighted = 0;
    for (const q of this.questions) {
      const a = ans.get(q.id);
      if (a !== undefined) {
        totalWeighted += ANSWER_SCORES[a] * q.weight;
        maxWeighted += 3 * q.weight; // max score is 3 (always)
      }
    }
    return maxWeighted > 0 ? (totalWeighted / maxWeighted) * 100 : 0;
  });

  riskLevel = computed(() => {
    const s = this.score();
    if (s >= 60) return 'Elevated Risk';
    if (s >= 35) return 'Moderate';
    return 'Low Risk';
  });

  scoreColor = computed(() => {
    const s = this.score();
    if (s >= 60) return '#ef4444';
    if (s >= 35) return '#f59e0b';
    return '#22c55e';
  });

  strokeOffset = computed(() => {
    return this.circumference - (this.score() / 100) * this.circumference;
  });

  categoryScores = computed(() => {
    const ans = this.answers();
    const categories = new Map<string, { total: number; max: number }>();
    for (const q of this.questions) {
      const a = ans.get(q.id);
      if (a !== undefined) {
        const existing = categories.get(q.category) ?? { total: 0, max: 0 };
        existing.total += ANSWER_SCORES[a] * q.weight;
        existing.max += 3 * q.weight;
        categories.set(q.category, existing);
      }
    }
    return Array.from(categories.entries()).map(([category, data]) => ({
      category,
      percent: data.max > 0 ? (data.total / data.max) * 100 : 0,
    }));
  });

  startQuiz(): void {
    this.phase.set('quiz');
    this.currentIndex.set(0);
    this.answers.set(new Map());
  }

  selectAnswer(value: AnswerValue): void {
    const map = new Map(this.answers());
    map.set(this.currentQuestion().id, value);
    this.answers.set(map);
  }

  nextQuestion(): void {
    if (this.currentIndex() < this.questions.length - 1) {
      this.currentIndex.update(i => i + 1);
    } else {
      this.phase.set('results');
      if (isPlatformBrowser(this.platformId)) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }
  }

  previousQuestion(): void {
    if (this.currentIndex() > 0) {
      this.currentIndex.update(i => i - 1);
    }
  }

  retakeQuiz(): void {
    this.startQuiz();
    if (isPlatformBrowser(this.platformId)) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
}
