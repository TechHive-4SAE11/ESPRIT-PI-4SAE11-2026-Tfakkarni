import {
  Component, Input, Output, EventEmitter, OnInit, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RatingService } from '@/core/services/rating.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-meeting-rating-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="fixed inset-0 z-[80] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
     (click)="onBackdropClick($event)">

  <div class="relative bg-white dark:bg-gray-900 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700
              w-full max-w-md meeting-rating-animate"
       (click)="$event.stopPropagation()">

    <!-- HEADER -->
    <div class="px-6 pt-6 pb-4 text-center border-b border-gray-100 dark:border-gray-800">
      <div class="text-4xl mb-2">🩺</div>
      <h2 class="text-xl font-bold text-gray-900 dark:text-white">Évaluer la réunion</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Consultation avec
        <strong class="text-gray-700 dark:text-gray-200">{{ cleanDoctorName }}</strong>
      </p>
    </div>

    <!-- BODY -->
    <div class="px-6 py-5 space-y-5">

      @if (alreadyRated) {
        <div class="bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 rounded-xl px-4 py-3 text-center">
          <p class="text-emerald-700 dark:text-emerald-400 text-sm font-medium">
            ✅ Vous avez déjà évalué cette réunion.
          </p>
        </div>
      } @else if (submitted) {
        <div class="text-center py-4">
          <div class="text-5xl mb-3">🎉</div>
          <p class="text-lg font-semibold text-gray-900 dark:text-white mb-1">Merci pour votre avis !</p>
          <p class="text-sm text-gray-500">Votre évaluation a été transmise.</p>
          <div class="mt-3 flex justify-center gap-1">
            @for (s of [1,2,3,4,5]; track s) {
              <span class="text-2xl" [class]="s <= selectedRating ? 'text-yellow-400' : 'text-gray-300'">★</span>
            }
          </div>
        </div>
      } @else {

        <!-- STARS -->
        <div>
          <p class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 text-center">
            Donnez une note
          </p>
          <div class="flex items-center justify-center gap-2">
            @for (star of [1,2,3,4,5]; track star) {
              <button
                type="button"
                (click)="selectStar(star)"
                (mouseenter)="hoveredStar = star"
                (mouseleave)="hoveredStar = 0"
                class="text-4xl transition-all duration-100 hover:scale-110 focus:outline-none select-none"
                [class]="(hoveredStar || selectedRating) >= star ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'"
              >★</button>
            }
          </div>
          @if (selectedRating > 0) {
            <p class="text-center text-sm font-medium mt-2"
               [ngClass]="{
                 'text-red-500':     selectedRating <= 2,
                 'text-yellow-500':  selectedRating === 3,
                 'text-emerald-500': selectedRating >= 4
               }">{{ starLabels[selectedRating - 1] }}</p>
          }
        </div>

        <!-- REVIEW -->
        @if (selectedRating > 0) {
          <div>
            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
              Votre avis
              @if (selectedRating <= 3) {
                <span class="text-red-500 ml-1">* obligatoire</span>
              } @else {
                <span class="text-gray-400 ml-1">(optionnel)</span>
              }
            </label>
            <textarea
              [(ngModel)]="review"
              [placeholder]="selectedRating <= 3
                ? 'Expliquez votre note — obligatoire pour 1–3 étoiles…'
                : 'Partagez votre expérience (optionnel)…'"
              rows="4"
              class="w-full rounded-xl border px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 transition
                     bg-white dark:bg-gray-800 text-gray-900 dark:text-white
                     border-gray-300 dark:border-gray-600
                     focus:ring-emerald-500 focus:border-emerald-500 placeholder-gray-400"
            ></textarea>
            @if (selectedRating <= 3 && showValidation && !review.trim()) {
              <p class="text-red-500 text-xs mt-1">⚠ L'avis est obligatoire pour une note ≤ 3 étoiles.</p>
            }
          </div>
        }

        <!-- ERROR -->
        @if (errorMsg) {
          <div class="bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 rounded-lg px-4 py-3">
            <p class="text-red-700 dark:text-red-400 text-sm font-medium">❌ {{ errorMsg }}</p>
            <p class="text-red-600/70 text-xs mt-1">Vérifiez que le backend est démarré et réessayez.</p>
          </div>
        }
      }
    </div>

    <!-- FOOTER -->
    <div class="px-6 pb-6 flex items-center gap-3">
      @if (submitted || alreadyRated) {
        <button type="button" (click)="close.emit()"
          class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm px-4 py-2.5 rounded-xl transition">
          Fermer
        </button>
      } @else {
        <button type="button" (click)="close.emit()" [disabled]="submitting"
          class="flex-1 bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700
                 disabled:opacity-40 text-gray-700 dark:text-gray-300 font-medium text-sm px-4 py-2.5 rounded-xl transition">
          Ignorer
        </button>
        <button
          type="button"
          (click)="submit()"
          [disabled]="selectedRating === 0 || submitting"
          class="flex-1 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed
                 text-white font-semibold text-sm px-4 py-2.5 rounded-xl transition
                 flex items-center justify-center gap-2">
          @if (submitting) {
            <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            Envoi en cours…
          } @else {
            ⭐ Envoyer l'évaluation
          }
        </button>
      }
    </div>

  </div>
</div>

<style>
  .meeting-rating-animate { animation: ratingFadeIn 0.2s ease-out; }
  @keyframes ratingFadeIn {
    from { opacity: 0; transform: scale(0.95) translateY(8px); }
    to   { opacity: 1; transform: scale(1)   translateY(0);    }
  }
</style>
  `,
})
export class MeetingRatingModalComponent implements OnInit {

  @Input() meetingId!: number;
  @Input() doctorKeycloakId!: string;
  @Input() patientKeycloakId!: string;
  @Input() doctorName = 'Médecin';

  @Output() close = new EventEmitter<void>();
  @Output() rated = new EventEmitter<number>();

  private ratingService = inject(RatingService);

  selectedRating = 0;
  hoveredStar    = 0;
  review         = '';
  submitting     = false;
  submitted      = false;
  alreadyRated   = false;
  errorMsg       = '';
  showValidation = false;

  readonly starLabels = [
    'Très insatisfait 😞',
    'Insatisfait 😕',
    'Moyen 😐',
    'Satisfait 😊',
    'Très satisfait 😄',
  ];

  /** Remove duplicate "Dr." prefix — doctorName from DB may already include it */
  get cleanDoctorName(): string {
    const name = (this.doctorName ?? '').trim();
    if (!name) return 'Médecin';
    return /^dr\.?\s/i.test(name) ? name : 'Dr. ' + name;
  }

  ngOnInit(): void {
    if (!this.meetingId || !this.patientKeycloakId) return;
    // Non-blocking check — if it fails, user can still submit
    this.ratingService.checkRated(this.meetingId, this.patientKeycloakId)
      .subscribe({ next: res => { this.alreadyRated = res.rated; } });
  }

  selectStar(star: number): void {
    this.selectedRating = star;
    this.errorMsg       = '';
    this.showValidation = false;
  }

  submit(): void {
    this.showValidation = true;
    if (this.selectedRating === 0) return;
    if (this.selectedRating <= 3 && !this.review.trim()) return;

    // Guard: prevent double submit
    if (this.submitting) return;

    this.submitting = true;
    this.errorMsg   = '';

    const payload = {
      meetingId:         this.meetingId,
      doctorKeycloakId:  this.doctorKeycloakId ?? '',
      patientKeycloakId: this.patientKeycloakId,
      rating:            this.selectedRating,
      review:            this.review.trim() || null,
    };

    console.log('[Rating] Submitting payload:', JSON.stringify(payload));

    this.ratingService.submitRating(payload).pipe(
      // finalize() ALWAYS runs — guarantees spinner stops even on error
      finalize(() => { this.submitting = false; })
    ).subscribe({
      next: () => {
        console.log('[Rating] ✅ Success');
        this.submitted = true;
        this.alreadyRated = true;
        this.rated.emit(this.selectedRating);
        setTimeout(() => this.close.emit(), 2500);
      },
      error: (err) => {
        console.error('[Rating] ❌ Error:', err);
        // Parse the error message from backend
        const serverMsg = err?.error?.error ?? err?.error?.message;
        if (serverMsg?.includes('déjà évalué')) {
          this.alreadyRated = true;
          this.errorMsg = '';
        } else if (err?.status === 0) {
          this.errorMsg = 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.';
        } else if (err?.status === 500) {
          this.errorMsg = `Erreur serveur (500). Le backend a planté — vérifiez les logs du tracking-service.`;
        } else {
          this.errorMsg = serverMsg ?? `Erreur ${err?.status ?? 'inconnue'}. Réessayez.`;
        }
      },
    });
  }

  onBackdropClick(event: MouseEvent): void {
    if (!this.submitting) this.close.emit();
  }
}
