import {
  Component, OnInit, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RatingService, DoctorRankingResponse } from '@/core/services/rating.service';

@Component({
  selector: 'app-doctor-rating-ranking',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="space-y-8">

  <!-- ── PODIUM TOP 3 ── -->
  @if (ranking().length > 0) {
    <div>
      <h3 class="text-lg font-bold mb-6 text-center tracking-tight">🏆 Podium des Médecins</h3>

      <div class="flex items-end justify-center gap-4">

        <!-- 🥈 2nd -->
        @if (ranking().length > 1) {
          <div class="flex flex-col items-center gap-2">
            <div class="text-2xl">🥈</div>
            <div class="w-20 h-20 rounded-full bg-gradient-to-br from-gray-300 to-gray-400
                        flex items-center justify-center text-2xl font-bold text-white shadow-lg">
              {{ initials(ranking()[1].doctorName) }}
            </div>
            <div class="text-center">
              <p class="text-sm font-semibold text-foreground max-w-[90px] truncate">{{ ranking()[1].doctorName }}</p>
              <div class="flex items-center justify-center gap-0.5 mt-1">
                @for (s of [1,2,3,4,5]; track s) {
                  <span class="text-sm" [class]="s <= ranking()[1].averageRating ? 'text-yellow-400' : 'text-gray-300'">★</span>
                }
              </div>
              <p class="text-xs text-muted-foreground mt-0.5">{{ ranking()[1].averageRating | number:'1.1-1' }} / 5</p>
              <p class="text-xs text-muted-foreground">{{ ranking()[1].totalRatings }} avis</p>
            </div>
            <!-- Podium block -->
            <div class="w-24 h-16 bg-gradient-to-t from-gray-400 to-gray-300 rounded-t-lg flex items-center justify-center">
              <span class="text-white font-bold text-xl">2</span>
            </div>
          </div>
        }

        <!-- 🥇 1st (tallest) -->
        @if (ranking().length > 0) {
          <div class="flex flex-col items-center gap-2">
            <div class="text-3xl">🥇</div>
            <div class="w-24 h-24 rounded-full bg-gradient-to-br from-yellow-400 to-yellow-500
                        flex items-center justify-center text-3xl font-bold text-white shadow-xl ring-4 ring-yellow-300">
              {{ initials(ranking()[0].doctorName) }}
            </div>
            <div class="text-center">
              <p class="text-base font-bold text-foreground max-w-[110px] truncate">{{ ranking()[0].doctorName }}</p>
              <div class="flex items-center justify-center gap-0.5 mt-1">
                @for (s of [1,2,3,4,5]; track s) {
                  <span class="text-base" [class]="s <= ranking()[0].averageRating ? 'text-yellow-400' : 'text-gray-300'">★</span>
                }
              </div>
              <p class="text-sm font-semibold text-yellow-600 dark:text-yellow-400 mt-0.5">
                {{ ranking()[0].averageRating | number:'1.1-1' }} / 5
              </p>
              <p class="text-xs text-muted-foreground">{{ ranking()[0].totalRatings }} avis</p>
            </div>
            <!-- Podium block (tallest) -->
            <div class="w-28 h-24 bg-gradient-to-t from-yellow-500 to-yellow-400 rounded-t-lg flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-2xl">1</span>
            </div>
          </div>
        }

        <!-- 🥉 3rd -->
        @if (ranking().length > 2) {
          <div class="flex flex-col items-center gap-2">
            <div class="text-2xl">🥉</div>
            <div class="w-20 h-20 rounded-full bg-gradient-to-br from-orange-300 to-orange-400
                        flex items-center justify-center text-2xl font-bold text-white shadow-lg">
              {{ initials(ranking()[2].doctorName) }}
            </div>
            <div class="text-center">
              <p class="text-sm font-semibold text-foreground max-w-[90px] truncate">{{ ranking()[2].doctorName }}</p>
              <div class="flex items-center justify-center gap-0.5 mt-1">
                @for (s of [1,2,3,4,5]; track s) {
                  <span class="text-sm" [class]="s <= ranking()[2].averageRating ? 'text-yellow-400' : 'text-gray-300'">★</span>
                }
              </div>
              <p class="text-xs text-muted-foreground mt-0.5">{{ ranking()[2].averageRating | number:'1.1-1' }} / 5</p>
              <p class="text-xs text-muted-foreground">{{ ranking()[2].totalRatings }} avis</p>
            </div>
            <!-- Podium block -->
            <div class="w-24 h-12 bg-gradient-to-t from-orange-500 to-orange-400 rounded-t-lg flex items-center justify-center">
              <span class="text-white font-bold text-xl">3</span>
            </div>
          </div>
        }
      </div>
    </div>
  }

  <!-- ── FULL RANKING TABLE ── -->
  <div>
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-bold">📊 Classement complet des médecins</h3>
      <button (click)="loadRanking()"
        class="text-xs text-muted-foreground hover:text-foreground underline underline-offset-2 transition">
        🔄 Actualiser
      </button>
    </div>

    @if (loading()) {
      <div class="flex justify-center py-10">
        <div class="w-8 h-8 border-2 border-muted border-t-yellow-400 rounded-full animate-spin"></div>
      </div>
    } @else if (ranking().length === 0) {
      <div class="bg-card border border-border rounded-xl p-8 text-center">
        <div class="text-4xl mb-3">⭐</div>
        <p class="text-muted-foreground text-sm font-medium">Aucune évaluation reçue pour le moment.</p>
        <p class="text-muted-foreground/60 text-xs mt-1">Les patients peuvent évaluer les médecins après chaque réunion.</p>
      </div>
    } @else {
      <div class="overflow-hidden rounded-xl border border-border">
        <table class="w-full text-sm">
          <thead class="bg-muted/50">
            <tr class="border-b border-border">
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground">Rang</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground">Médecin</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground">Note moyenne</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground">Total avis</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground">Derniers avis</th>
            </tr>
          </thead>
          <tbody>
            @for (doc of ranking(); track doc.doctorKeycloakId) {
              <tr class="border-b border-border last:border-0 hover:bg-muted/30 transition">

                <!-- Rank -->
                <td class="px-4 py-3">
                  <span class="text-lg">{{ rankEmoji(doc.rank) }}</span>
                </td>

                <!-- Name -->
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div class="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold shrink-0"
                         [ngClass]="rankAvatarClass(doc.rank)">
                      {{ initials(doc.doctorName) }}
                    </div>
                    <div>
                      <p class="font-semibold text-foreground">{{ doc.doctorName }}</p>
                      <p class="text-xs text-muted-foreground font-mono">{{ doc.doctorKeycloakId | slice:0:12 }}…</p>
                    </div>
                  </div>
                </td>

                <!-- Stars + score -->
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1.5">
                    <div class="flex items-center gap-0.5">
                      @for (s of [1,2,3,4,5]; track s) {
                        <span class="text-base leading-none"
                              [class]="s <= doc.averageRating ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'">★</span>
                      }
                    </div>
                    <span class="text-sm font-bold text-foreground">{{ doc.averageRating | number:'1.1-1' }}</span>
                  </div>
                  <!-- Progress bar -->
                  <div class="w-28 h-1.5 bg-muted rounded-full mt-1.5">
                    <div class="h-1.5 rounded-full bg-yellow-400 transition-all duration-500"
                         [style.width.%]="(doc.averageRating / 5) * 100"></div>
                  </div>
                </td>

                <!-- Count -->
                <td class="px-4 py-3">
                  <span class="font-semibold text-foreground">{{ doc.totalRatings }}</span>
                  <span class="text-muted-foreground text-xs ml-1">avis</span>
                </td>

                <!-- Recent reviews -->
                <td class="px-4 py-3">
                  @if (doc.recentReviews && doc.recentReviews.length > 0) {
                    <button (click)="toggleReviews(doc.doctorKeycloakId)"
                      class="text-xs text-emerald-600 dark:text-emerald-400 underline underline-offset-2 hover:no-underline transition">
                      @if (expandedDoctorId === doc.doctorKeycloakId) { ▲ Masquer }
                      @else { 💬 Voir {{ doc.recentReviews.length }} avis }
                    </button>
                  } @else {
                    <span class="text-xs text-muted-foreground italic">Aucun avis écrit</span>
                  }
                </td>
              </tr>

              <!-- Expanded reviews -->
              @if (expandedDoctorId === doc.doctorKeycloakId && doc.recentReviews.length) {
                <tr class="bg-muted/20">
                  <td colspan="5" class="px-6 py-4">
                    <div class="space-y-3">
                      @for (rev of doc.recentReviews; track rev.id) {
                        @if (rev.review) {
                          <div class="flex gap-3 items-start bg-white dark:bg-gray-900 rounded-xl border border-border p-3 shadow-sm">
                            <div class="shrink-0 flex items-center gap-0.5">
                              @for (s of [1,2,3,4,5]; track s) {
                                <span class="text-sm" [class]="s <= rev.rating ? 'text-yellow-400' : 'text-gray-300'">★</span>
                              }
                            </div>
                            <div class="flex-1 min-w-0">
                              <p class="text-sm text-foreground leading-relaxed">{{ rev.review }}</p>
                              <p class="text-xs text-muted-foreground mt-1">
                                👤 {{ rev.patientName }} —
                                {{ rev.createdAt | date:'dd/MM/yyyy HH:mm' }}
                              </p>
                            </div>
                          </div>
                        }
                      }
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    }
  </div>

</div>
  `,
})
export class DoctorRatingRankingComponent implements OnInit {

  private ratingService = inject(RatingService);

  ranking = signal<DoctorRankingResponse[]>([]);
  loading = signal(true);
  expandedDoctorId: string | null = null;

  ngOnInit(): void {
    this.loadRanking();
  }

  loadRanking(): void {
    this.loading.set(true);
    this.ratingService.getRanking().subscribe({
      next: (data) => { this.ranking.set(data); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  toggleReviews(id: string): void {
    this.expandedDoctorId = this.expandedDoctorId === id ? null : id;
  }

  initials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    return parts.length >= 2
      ? (parts[0][0] + parts[1][0]).toUpperCase()
      : name.substring(0, 2).toUpperCase();
  }

  rankEmoji(rank: number): string {
    return rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : `#${rank}`;
  }

  rankAvatarClass(rank: number): string {
    if (rank === 1) return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400';
    if (rank === 2) return 'bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-300';
    if (rank === 3) return 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400';
    return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
  }
}
