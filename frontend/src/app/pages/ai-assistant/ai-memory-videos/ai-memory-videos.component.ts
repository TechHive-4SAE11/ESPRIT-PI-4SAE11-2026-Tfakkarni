import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssistantAIService } from '@/core/services/assistant-ai.service';
import { UserApiService } from '@/core/services/user-api.service';
import {
  VideoGenerateRequest,
  VideoGenerateResponse,
  VideoFeedbackRequest,
} from '@/core/models/assistant-ai.model';

@Component({
  selector: 'app-ai-memory-videos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center gap-3">
        <div class="p-3 rounded-2xl bg-gradient-to-br from-rose-500 to-orange-500 text-white">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
        </div>
        <div>
          <h2 class="text-2xl font-bold bg-gradient-to-r from-rose-600 to-orange-600 bg-clip-text text-transparent">
            Memory Video Generator
          </h2>
          <p class="text-sm text-muted-foreground">Create personalized memory stimulation videos for patients</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-1 p-1 rounded-xl bg-muted/50 w-fit">
        <button (click)="activeTab = 'generate'"
          [class]="activeTab === 'generate'
            ? 'px-4 py-2 rounded-lg text-sm font-semibold bg-background shadow-sm text-foreground'
            : 'px-4 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground transition-colors'">
          ✨ Generate
        </button>
        <button (click)="activeTab = 'library'; loadPatientVideos()"
          [class]="activeTab === 'library'
            ? 'px-4 py-2 rounded-lg text-sm font-semibold bg-background shadow-sm text-foreground'
            : 'px-4 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground transition-colors'">
          📚 Library
        </button>
      </div>

      @if (activeTab === 'generate') {
        <div class="grid gap-6 lg:grid-cols-3">
          <!-- Config -->
          <div class="lg:col-span-1">
            <div class="rounded-2xl border border-border bg-card p-6 space-y-5 shadow-sm">
              <h3 class="font-semibold text-lg flex items-center gap-2">
                <span class="text-xl">🎬</span> Video Config
              </h3>

              <div class="space-y-2">
                <label class="text-sm font-medium">Sélection du Patient</label>
                @if (isLoadingPatients) {
                   <div class="w-full px-4 py-2.5 rounded-xl border border-input text-sm text-muted-foreground bg-muted/30">Chargement...</div>
                } @else {
                   <select [(ngModel)]="patientId" (change)="onPatientSelectionChange()"
                      class="w-full px-4 py-2.5 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/40">
                      <option [value]="0" disabled>-- Sélectionnez un patient --</option>
                      @for (patient of patients; track patient.id) {
                         <option [value]="patient.id">{{ patient.firstName }} {{ patient.lastName }}</option>
                      }
                   </select>
                }
              </div>

              <div class="space-y-2">
                <label class="text-sm font-medium">Topic <span class="text-muted-foreground">(optional, auto-généré si vide)</span></label>
                <input type="text" [(ngModel)]="topic" placeholder="e.g. Childhood memories, Garden flowers..."
                  class="w-full px-4 py-2.5 rounded-xl border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/40" />
              </div>

              <div class="space-y-2">
                <label class="text-sm font-medium">Memory Type</label>
                <div class="grid grid-cols-3 gap-2">
                  @for (t of memoryTypes; track t.value) {
                    <button (click)="memoryType = t.value"
                      [class]="memoryType === t.value
                        ? 'py-3 rounded-xl text-center text-xs font-bold bg-gradient-to-b from-rose-500 to-orange-500 text-white shadow-md'
                        : 'py-3 rounded-xl text-center text-xs font-medium border border-input bg-background hover:bg-accent transition-colors'">
                      <div class="text-xl mb-0.5">{{ t.emoji }}</div>
                      {{ t.label }}
                    </button>
                  }
                </div>
              </div>

              <div class="space-y-2">
                <label class="text-sm font-medium">Duration: {{ duration }}s</label>
                <input type="range" [(ngModel)]="duration" min="30" max="120" step="15"
                  class="w-full accent-rose-500" />
                <div class="flex justify-between text-xs text-muted-foreground">
                  <span>30s</span><span>120s</span>
                </div>
              </div>

              <button (click)="generateVideo()" [disabled]="isGenerating() || patientId === 0"
                class="w-full py-3 rounded-xl text-sm font-bold text-white bg-gradient-to-r from-rose-500 to-orange-500 hover:from-rose-600 hover:to-orange-600 disabled:opacity-50 shadow-lg shadow-rose-500/25 transition-all flex items-center justify-center gap-2">
                @if (isGenerating()) {
                  <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  Generating Script...
                } @else {
                  🎬 Generate Video
                }
              </button>
            </div>
          </div>

          <!-- Result -->
          <div class="lg:col-span-2 space-y-4">
            @if (errorMessage()) {
              <div class="p-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm">
                <strong>Error:</strong> {{ errorMessage() }}
              </div>
            }

            @if (generatedVideo(); as video) {
              <!-- Success -->
              <div class="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800">
                <p class="font-semibold text-emerald-800 dark:text-emerald-200">
                  ✅ Video script generated for {{ patientName }}! (Video #{{ video.videoId }}) • {{ video.storyboard?.length || 0 }} scenes • {{ video.duration }}s
                </p>
              </div>

              <!-- Script -->
              <div class="rounded-2xl border border-border bg-card p-6 shadow-sm">
                <h3 class="font-bold text-lg mb-3 flex items-center gap-2">
                  <span>📝</span> Narration Script
                </h3>
                <div class="p-4 rounded-xl bg-muted/30 border border-border/50 text-sm leading-relaxed whitespace-pre-wrap max-h-48 overflow-y-auto">
                  {{ video.script }}
                </div>
              </div>

              <!-- Storyboard -->
              @if (video.storyboard && video.storyboard.length > 0) {
                <div class="rounded-2xl border border-border bg-card p-6 shadow-sm">
                  <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
                    <span>🎞️</span> Storyboard
                  </h3>
                  <div class="space-y-3">
                    @for (scene of video.storyboard; track scene.sceneNumber) {
                      <div class="flex gap-4 p-4 rounded-xl bg-muted/20 border border-border/30 hover:bg-muted/40 transition-colors">
                        <div class="shrink-0 w-10 h-10 rounded-xl bg-gradient-to-br from-rose-500 to-orange-500 text-white flex items-center justify-center font-bold text-sm">
                          {{ scene.sceneNumber }}
                        </div>
                        <div class="flex-1 min-w-0 space-y-1.5">
                          <div class="flex items-center gap-2">
                            <span class="text-xs px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300 font-semibold">
                              {{ scene.durationSeconds }}s
                            </span>
                          </div>
                          <p class="text-sm font-medium text-foreground">{{ scene.description }}</p>
                          <p class="text-xs text-muted-foreground italic">"{{ scene.narration }}"</p>
                          @if (scene.visualPrompt) {
                            <p class="text-xs text-blue-600 dark:text-blue-400">🎨 {{ scene.visualPrompt }}</p>
                          }
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            } @else if (!isGenerating()) {
              <div class="rounded-2xl border-2 border-dashed border-border bg-muted/10 p-16 text-center">
                <div class="text-5xl mb-4">🎬</div>
                <h3 class="text-lg font-semibold mb-2 text-foreground">Ready to create</h3>
                <p class="text-sm text-muted-foreground max-w-sm mx-auto">
                  Configure the video parameters and click "Generate Video" to create a personalized memory stimulation script.
                </p>
              </div>
            }

            @if (isGenerating()) {
              <div class="space-y-4 animate-pulse">
                <div class="rounded-2xl border border-border bg-card p-6 space-y-3">
                  <div class="h-4 bg-muted rounded-full w-1/3"></div>
                  <div class="h-24 bg-muted/60 rounded-xl"></div>
                </div>
                <div class="rounded-2xl border border-border bg-card p-6 space-y-3">
                  <div class="h-4 bg-muted rounded-full w-1/4"></div>
                  @for (_ of [1, 2, 3]; track _) {
                    <div class="flex gap-4 p-4 rounded-xl bg-muted/20">
                      <div class="w-10 h-10 bg-muted rounded-xl shrink-0"></div>
                      <div class="flex-1 space-y-2">
                        <div class="h-3 bg-muted/60 rounded-full w-2/3"></div>
                        <div class="h-2 bg-muted/40 rounded-full w-full"></div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        </div>
      }

      @if (activeTab === 'library') {
        <div class="space-y-4">
          <div class="flex items-center gap-3">
            @if (isLoadingPatients) {
               <div class="px-4 py-2.5 rounded-xl border border-input text-sm text-muted-foreground">Chargement des patients...</div>
            } @else {
               <select [(ngModel)]="libraryPatientId"
                  class="px-4 py-2.5 rounded-xl border border-input bg-background text-sm w-56 focus:outline-none focus:ring-2 focus:ring-rose-500/40">
                  <option [value]="0" disabled>-- Sélectionnez un patient --</option>
                  @for (patient of patients; track patient.id) {
                     <option [value]="patient.id">{{ patient.firstName }} {{ patient.lastName }}</option>
                  }
               </select>
            }
            <button (click)="loadPatientVideos()" [disabled]="libraryPatientId === 0"
              class="px-4 py-2.5 rounded-xl text-sm font-semibold bg-rose-500 text-white hover:bg-rose-600 transition-colors disabled:opacity-50">
              Voir vidéos
            </button>
          </div>

          @if (isLoadingLibrary()) {
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              @for (_ of [1, 2, 3]; track _) {
                <div class="rounded-2xl border border-border bg-card p-5 animate-pulse space-y-3">
                  <div class="h-32 bg-muted rounded-xl"></div>
                  <div class="h-4 bg-muted rounded-full w-2/3"></div>
                  <div class="h-3 bg-muted/60 rounded-full w-1/2"></div>
                </div>
              }
            </div>
          } @else if (patientVideos().length > 0) {
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              @for (v of patientVideos(); track v.videoId) {
                <div class="rounded-2xl border border-border bg-card overflow-hidden shadow-sm hover:shadow-md transition-shadow group cursor-pointer"
                  (click)="viewVideoDetail(v)">
                  <!-- Thumbnail placeholder -->
                  <div class="h-32 bg-gradient-to-br from-rose-100 to-orange-100 dark:from-rose-900/20 dark:to-orange-900/20 flex items-center justify-center">
                    <div class="text-4xl">
                      {{ v.memoryType === 'PHOTO' ? '📸' : v.memoryType === 'STORY' ? '📖' : '🧩' }}
                    </div>
                  </div>
                  <div class="p-4 space-y-2">
                    <h4 class="font-semibold text-sm text-foreground group-hover:text-rose-600 transition-colors">{{ v.topic }}</h4>
                    <div class="flex items-center gap-2 text-xs text-muted-foreground">
                      <span class="px-2 py-0.5 rounded-full bg-muted font-medium">{{ v.memoryType }}</span>
                      <span>{{ v.duration }}s</span>
                      <span class="px-2 py-0.5 rounded-full font-semibold"
                        [class]="v.status === 'READY' ? 'bg-emerald-100 text-emerald-700' : v.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'">
                        {{ v.status }}
                      </span>
                    </div>
                    <p class="text-xs text-muted-foreground">{{ v.createdAt | date:'dd/MM/yyyy HH:mm' }}</p>
                  </div>
                </div>
              }
            </div>
          } @else {
            <div class="rounded-2xl border-2 border-dashed border-border bg-muted/10 p-12 text-center">
              <div class="text-4xl mb-3">📚</div>
              <p class="text-sm text-muted-foreground">No videos found for this patient.</p>
            </div>
          }
        </div>
      }

      <!-- Video Detail Modal -->
      @if (selectedVideo(); as v) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" (click)="selectedVideo.set(null)">
          <div class="w-full max-w-2xl max-h-[80vh] overflow-y-auto rounded-2xl bg-background shadow-2xl border border-border p-6"
            (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-xl font-bold">{{ v.topic }}</h3>
              <button (click)="selectedVideo.set(null)" class="p-1 rounded-lg hover:bg-muted transition-colors">✕</button>
            </div>
            <div class="space-y-4">
              <div class="flex gap-2 text-xs">
                <span class="px-2 py-1 rounded-full bg-rose-100 text-rose-700 font-semibold">{{ v.memoryType }}</span>
                <span class="px-2 py-1 rounded-full bg-muted font-medium">{{ v.duration }}s</span>
                <span class="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700 font-semibold">{{ v.status }}</span>
              </div>
              @if (v.videoUrl && v.status === 'READY') {
                <video 
                  [src]="v.videoUrl" 
                  controls 
                  class="w-full rounded-xl shadow-sm border border-border">
                </video>
              } @else if (v.status === 'FAILED') {
                <div class="p-4 rounded-xl bg-red-50 text-red-700 text-sm flex items-center justify-between border border-red-100">
                  <span>⚠️ Video generation failed. Please retry.</span>
                  <button (click)="retryVideo(v.videoId)" class="px-3 py-1.5 bg-red-100 font-semibold rounded-lg hover:bg-red-200">Retry</button>
                </div>
              } @else {
                <div class="p-12 rounded-xl border-2 border-dashed border-border flex flex-col items-center justify-center text-center">
                  <div class="animate-spin text-4xl mb-3">⏳</div>
                  <p class="font-medium text-muted-foreground">Generating video...</p>
                </div>
              }

              <!-- View Script -->
              <details class="mt-4 border border-border bg-muted/10 rounded-xl">
                <summary class="px-4 py-3 font-semibold cursor-pointer border-b border-border">View Script</summary>
                <div class="p-4 text-sm leading-relaxed whitespace-pre-wrap max-h-60 overflow-y-auto">
                  {{ v.script }}
                </div>
              </details>
              @if (v.storyboard && v.storyboard.length) {
                <h4 class="font-semibold">Storyboard ({{ v.storyboard.length }} scenes)</h4>
                <div class="space-y-3">
                  @for (s of v.storyboard; track s.sceneNumber) {
                    <div class="flex gap-3 p-3 rounded-xl bg-muted/20 border border-border/30">
                      <span class="shrink-0 w-8 h-8 rounded-lg bg-rose-500 text-white flex items-center justify-center text-xs font-bold">{{ s.sceneNumber }}</span>
                      <div class="text-sm">
                        <p class="font-medium">{{ s.description }}</p>
                        <p class="text-xs text-muted-foreground italic mt-0.5">"{{ s.narration }}"</p>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
})
export class AiMemoryVideosComponent implements OnInit {
  private readonly aiService: AssistantAIService;
  private readonly userService: UserApiService;

  activeTab: 'generate' | 'library' = 'generate';

  // Patients drop down state
  patients: any[] = [];
  isLoadingPatients = false;

  // Generate form
  patientId = 0; // 0 means not selected
  topic = '';
  memoryType: 'PHOTO' | 'STORY' | 'EXERCISE' = 'PHOTO';
  duration = 60;
  patientName = ''; // set dynamically via dropdown

  // Signals
  isGenerating = signal(false);
  generatedVideo = signal<VideoGenerateResponse | null>(null);
  errorMessage = signal<string | null>(null);

  // Library
  libraryPatientId = 0; // 0 means not selected
  isLoadingLibrary = signal(false);
  patientVideos = signal<VideoGenerateResponse[]>([]);
  selectedVideo = signal<VideoGenerateResponse | null>(null);

  memoryTypes = [
    { value: 'PHOTO' as const, label: 'Photo', emoji: '📸' },
    { value: 'STORY' as const, label: 'Story', emoji: '📖' },
    { value: 'EXERCISE' as const, label: 'Exercise', emoji: '🧩' },
  ];

  constructor(aiService: AssistantAIService, userService: UserApiService) {
    this.aiService = aiService;
    this.userService = userService;
  }

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.isLoadingPatients = true;
    this.userService.getUsersByRole('PATIENT').subscribe({
      next: (users) => {
        this.patients = users;
        this.isLoadingPatients = false;
        if (this.patients.length > 0) {
            this.patientId = this.patients[0].id;
            this.libraryPatientId = this.patients[0].id;
            this.onPatientSelectionChange();
        }
      },
      error: (err) => {
        console.error('Failed to load patients', err);
        this.isLoadingPatients = false;
      }
    });
  }

  onPatientSelectionChange(): void {
     const p = this.patients.find(x => x.id === Number(this.patientId));
     if (p) {
        this.patientName = p.firstName + ' ' + p.lastName;
     } else {
        this.patientName = '';
     }
  }

  generateVideo(): void {
    if (this.isGenerating() || this.patientId === 0) return;

    this.isGenerating.set(true);
    this.errorMessage.set(null);
    this.generatedVideo.set(null);

    const request: VideoGenerateRequest = {
      patientId: Number(this.patientId),
      topic: this.topic?.trim() || '',
      memoryType: this.memoryType,
      duration: this.duration,
      patientName: this.patientName || undefined,
    };

    this.aiService.generateVideo(request).subscribe({
      next: (video) => {
        this.generatedVideo.set(video);
        this.isGenerating.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to generate video');
        this.isGenerating.set(false);
      },
    });
  }

  loadPatientVideos(): void {
    this.isLoadingLibrary.set(true);
    this.aiService.getPatientVideos(this.libraryPatientId).subscribe({
      next: (videos) => {
        this.patientVideos.set(videos);
        this.isLoadingLibrary.set(false);
      },
      error: () => {
        this.patientVideos.set([]);
        this.isLoadingLibrary.set(false);
      },
    });
  }

  viewVideoDetail(video: VideoGenerateResponse): void {
    this.selectedVideo.set(video);
  }

  retryVideo(videoId: number): void {
    this.aiService.renderVideo(videoId).subscribe({
      next: () => {
        this.loadPatientVideos(); // Recharger la liste
      },
      error: (err) => {
        this.errorMessage.set('Failed to generate video: ' + (err.error?.message || err.message));
      }
    });
  }
}
