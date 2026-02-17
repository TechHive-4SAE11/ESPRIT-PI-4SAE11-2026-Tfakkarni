import { Component, OnInit, signal, Input, Output, EventEmitter, ViewChild, ElementRef, PLATFORM_ID, Inject, NgZone } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { PlaceService, type PlaceQuizResponse } from '@/core/services/place.service';
import { environment } from '../../../../../environments/environment';

/** Dynamically load the Google Maps JS API (once) */
function loadGoogleMapsApi(apiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof google !== 'undefined' && google.maps) {
      resolve();
      return;
    }
    const existing = document.getElementById('google-maps-script');
    if (existing) {
      existing.addEventListener('load', () => resolve());
      return;
    }
    const script = document.createElement('script');
    script.id = 'google-maps-script';
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=streetView`;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load Google Maps API'));
    document.head.appendChild(script);
  });
}

@Component({
  selector: 'app-guess-place',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gradient-to-b from-green-50 to-white dark:from-slate-900 dark:to-slate-800 px-4 py-6">
      <div class="max-w-2xl mx-auto">
        <!-- Back button -->
        <button
          (click)="goBack.emit()"
          class="flex items-center gap-2 text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white mb-6 text-lg transition-colors"
        >
          <span class="text-2xl">←</span>
          <span>Back</span>
        </button>

        <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2 text-center">
          📍 Guess the Place!
        </h1>
        <p class="text-lg text-slate-500 dark:text-slate-400 mb-6 text-center">
          Look around in the Street View and pick the right place name
        </p>

        @switch (gameState()) {
          @case ('loading') {
            <div class="text-center py-16">
              <p class="text-5xl mb-4 animate-bounce">🔄</p>
              <p class="text-xl text-slate-500 dark:text-slate-400">Loading quiz...</p>
            </div>
          }

          @case ('error') {
            <div class="text-center py-16">
              <p class="text-5xl mb-4">😕</p>
              <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">Can't Start Quiz</h2>
              <p class="text-slate-500 dark:text-slate-400 text-lg mb-6">{{ errorMessage() }}</p>
              <button
                (click)="goBack.emit()"
                class="px-8 py-4 rounded-2xl bg-blue-500 hover:bg-blue-600 text-white text-lg font-bold shadow-lg transition-all"
              >
                ← Go Back
              </button>
            </div>
          }

          @case ('playing') {
            @if (quiz(); as q) {
              <!-- Interactive Street View Panorama -->
              <div class="rounded-2xl overflow-hidden shadow-lg mb-6 border-4 border-white dark:border-slate-700 relative">
                <div
                  #streetViewContainer
                  class="w-full h-[300px] sm:h-[400px] bg-slate-200 dark:bg-slate-700"
                ></div>
                @if (panoramaLoading()) {
                  <div class="absolute inset-0 flex items-center justify-center bg-slate-200 dark:bg-slate-700">
                    <p class="text-3xl animate-bounce">🌍</p>
                  </div>
                }
                @if (streetViewUnavailable()) {
                  <div class="absolute inset-0 flex flex-col items-center justify-center bg-slate-200 dark:bg-slate-700">
                    <p class="text-5xl mb-3">🗺️</p>
                    <p class="text-lg text-slate-500 dark:text-slate-400 font-medium">Street View not available here</p>
                    <p class="text-sm text-slate-400 dark:text-slate-500 mt-1">Try to guess from the hint!</p>
                  </div>
                }
              </div>

              <!-- Drag / look around hint -->
              @if (!streetViewUnavailable() && !panoramaLoading()) {
                <div class="rounded-2xl bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 p-3 mb-4 text-center">
                  <p class="text-base">
                    <span class="text-lg mr-1">👆</span>
                    <span class="text-blue-700 dark:text-blue-300 font-medium">Drag to look around!</span>
                  </p>
                </div>
              }

              @if (q.hint) {
                <div class="rounded-2xl bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 p-4 mb-6 text-center">
                  <p class="text-lg">
                    <span class="text-xl mr-2">💡</span>
                    <span class="text-amber-800 dark:text-amber-200 font-medium">{{ q.hint }}</span>
                  </p>
                </div>
              }

              <!-- Choices -->
              <p class="text-center text-lg font-semibold text-slate-700 dark:text-slate-300 mb-4">
                Where is this place?
              </p>
              <div class="space-y-3">
                @for (choice of q.choices; track choice) {
                  <button
                    (click)="checkAnswer(choice)"
                    [disabled]="answered()"
                    class="w-full p-5 sm:p-6 rounded-2xl text-xl font-bold shadow-sm transition-all active:scale-[0.98]"
                    [class]="getChoiceClass(choice)"
                  >
                    {{ choice }}
                  </button>
                }
              </div>
            }
          }

          @case ('correct') {
            <div class="text-center py-12">
              <p class="text-7xl mb-4">🎉</p>
              <h2 class="text-3xl sm:text-4xl font-bold text-green-600 dark:text-green-400 mb-2">Correct!</h2>
              <p class="text-xl text-slate-500 dark:text-slate-400 mb-8">
                Great job! You remembered the place!
              </p>
              <div class="space-y-3">
                <button
                  (click)="loadQuiz()"
                  class="w-full px-8 py-5 rounded-2xl bg-green-500 hover:bg-green-600 text-white text-xl font-bold shadow-lg transition-all"
                >
                  🔄 Next Place
                </button>
                <button
                  (click)="goBack.emit()"
                  class="w-full px-8 py-4 rounded-2xl bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-300 text-lg font-bold transition-all"
                >
                  ← Back to Home
                </button>
              </div>
            </div>
          }

          @case ('wrong') {
            <div class="text-center py-12">
              <p class="text-7xl mb-4">🤔</p>
              <h2 class="text-3xl sm:text-4xl font-bold text-orange-600 dark:text-orange-400 mb-2">Not Quite!</h2>
              <p class="text-xl text-slate-500 dark:text-slate-400 mb-2">
                The correct answer was:
              </p>
              <p class="text-2xl font-bold text-slate-800 dark:text-white mb-8">
                {{ quiz()?.correctName }}
              </p>
              <div class="space-y-3">
                <button
                  (click)="loadQuiz()"
                  class="w-full px-8 py-5 rounded-2xl bg-blue-500 hover:bg-blue-600 text-white text-xl font-bold shadow-lg transition-all"
                >
                  🔄 Try Another
                </button>
                <button
                  (click)="goBack.emit()"
                  class="w-full px-8 py-4 rounded-2xl bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-300 text-lg font-bold transition-all"
                >
                  ← Back to Home
                </button>
              </div>
            </div>
          }
        }
      </div>
    </div>
  `,
})
export class GuessPlaceComponent implements OnInit {
  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();
  @ViewChild('streetViewContainer') streetViewContainer!: ElementRef<HTMLDivElement>;

  gameState = signal<'loading' | 'playing' | 'correct' | 'wrong' | 'error'>('loading');
  quiz = signal<PlaceQuizResponse | null>(null);
  answered = signal(false);
  selectedAnswer = signal<string | null>(null);
  errorMessage = signal('');
  panoramaLoading = signal(true);
  streetViewUnavailable = signal(false);

  private readonly apiKey = environment.googleMapsApiKey;
  private readonly isBrowser: boolean;
  private panorama: google.maps.StreetViewPanorama | null = null;
  private mapsApiLoaded = false;

  constructor(
    private readonly placeService: PlaceService,
    private readonly ngZone: NgZone,
    @Inject(PLATFORM_ID) platformId: object,
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.loadQuiz();
  }

  loadQuiz(): void {
    this.gameState.set('loading');
    this.answered.set(false);
    this.selectedAnswer.set(null);
    this.quiz.set(null);
    this.panoramaLoading.set(true);
    this.streetViewUnavailable.set(false);

    this.placeService.getPlaceQuiz(this.keycloakId).subscribe({
      next: quiz => {
        this.quiz.set(quiz);
        this.gameState.set('playing');
        // Wait for Angular to render the container, then init panorama
        setTimeout(() => this.initPanorama(quiz.latitude, quiz.longitude), 0);
      },
      error: err => {
        console.error('[GuessPlace] Failed to load quiz', err);
        this.errorMessage.set(err?.error?.error || 'Could not load the quiz. Make sure at least 3 places have been saved.');
        this.gameState.set('error');
      },
    });
  }

  private async initPanorama(lat: number, lng: number): Promise<void> {
    if (!this.isBrowser) return;

    try {
      // Load Google Maps JS API if not already loaded
      if (!this.mapsApiLoaded) {
        await loadGoogleMapsApi(this.apiKey);
        this.mapsApiLoaded = true;
      }

      const location = { lat, lng };
      const sv = new google.maps.StreetViewService();

      // Check if Street View is available near the coordinates (radius 500m)
      sv.getPanorama({ location, radius: 500 }, (data, status) => {
        this.ngZone.run(() => {
          if (status === google.maps.StreetViewStatus.OK && data?.location?.latLng) {
            this.streetViewUnavailable.set(false);
            this.panoramaLoading.set(false);
            this.panorama = new google.maps.StreetViewPanorama(
              this.streetViewContainer.nativeElement,
              {
                position: data.location.latLng,
                pov: { heading: 0, pitch: 0 },
                zoom: 1,
                // Allow looking around but disable moving to other locations
                addressControl: false,
                showRoadLabels: false,
                linksControl: false,
                fullscreenControl: false,
                enableCloseButton: false,
                // Keep pan/zoom controls for the elderly-friendly drag interaction
                panControl: true,
                zoomControl: true,
                motionTracking: false,
                motionTrackingControl: false,
              }
            );
          } else {
            // No Street View coverage at this location
            this.panoramaLoading.set(false);
            this.streetViewUnavailable.set(true);
          }
        });
      });
    } catch (err) {
      console.error('[GuessPlace] Failed to init Street View', err);
      this.panoramaLoading.set(false);
      this.streetViewUnavailable.set(true);
    }
  }

  checkAnswer(choice: string): void {
    if (this.answered()) return;
    this.answered.set(true);
    this.selectedAnswer.set(choice);

    const correct = this.quiz()?.correctName;
    if (choice === correct) {
      setTimeout(() => this.gameState.set('correct'), 800);
    } else {
      setTimeout(() => this.gameState.set('wrong'), 800);
    }
  }

  getChoiceClass(choice: string): string {
    if (!this.answered()) {
      return 'bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 text-slate-800 dark:text-white hover:border-blue-400 dark:hover:border-blue-500 hover:shadow-md';
    }

    const correct = this.quiz()?.correctName;
    if (choice === correct) {
      return 'bg-green-100 dark:bg-green-900/40 border-2 border-green-500 text-green-800 dark:text-green-300';
    }
    if (choice === this.selectedAnswer() && choice !== correct) {
      return 'bg-red-100 dark:bg-red-900/40 border-2 border-red-500 text-red-800 dark:text-red-300';
    }
    return 'bg-slate-50 dark:bg-slate-800/50 border-2 border-slate-200 dark:border-slate-700 text-slate-400 dark:text-slate-500 opacity-60';
  }
}
