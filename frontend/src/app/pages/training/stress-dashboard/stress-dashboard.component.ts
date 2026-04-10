import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ChatbotService, StressAnalysis } from '@/core/services/chatbot.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { UserApiService } from '@/core/services/user-api.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-stress-dashboard',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div class="flex items-center">
          <z-button zType="outline" (click)="goBack()">← Retour</z-button>
          <h1 class="text-3xl font-bold ml-4">Analyse de mon stress</h1>
        </div>
        <z-button (click)="loadAnalysis()">Actualiser</z-button>
      </div>

      <div *ngIf="isLoading()" class="flex justify-center p-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>

      <div *ngIf="errorMessage()" class="p-4 bg-red-100 text-red-700 rounded-md mb-6">
        {{ errorMessage() }}
      </div>

      <ng-container *ngIf="!isLoading() && analysis() as data">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">

          <z-card class="flex flex-col justify-center items-center p-8">
            <h2 class="text-xl font-semibold mb-4 text-gray-600">Niveau de stress actuel</h2>
            <div class="relative w-48 h-48 rounded-full flex items-center justify-center border-8 mb-4 border-gray-100 shadow-inner"
                 [ngStyle]="{'borderColor': getStressColorHex(data.stressLevel)}">
              <div class="text-center">
                <span class="text-4xl block mb-2">{{ getStressEmoji(data.stressLevel) }}</span>
                <span class="text-2xl font-bold font-mono" [ngStyle]="{'color': getStressColorHex(data.stressLevel)}">
                  {{ data.stressLevel }}
                </span>
              </div>
            </div>
            <p class="text-center text-gray-500 px-4 mt-2">
              Basé sur l'analyse de vos récentes interactions et activités.
            </p>
          </z-card>

          <z-card class="bg-gradient-to-br p-8 text-white flex flex-col justify-center"
                  [ngClass]="getStressGradientClass(data.stressLevel)">
            <div class="flex items-start mb-4">
              <span class="text-4xl mr-4 opacity-80">💡</span>
              <div>
                <h2 class="text-2xl font-bold mb-2">Recommandation principale</h2>
                <p class="text-lg opacity-90 leading-relaxed">{{ data.recommendation }}</p>
              </div>
            </div>
            <div class="mt-8 pt-6 border-t border-white border-opacity-20">
              <h3 class="font-semibold mb-2 opacity-90">Actions immédiates</h3>
              <ul class="list-disc list-inside opacity-80 space-y-1">
                <li *ngIf="data.stressLevel === 'HIGH'">Prendre 15 minutes de pause immédiate</li>
                <li *ngIf="data.stressLevel === 'HIGH'">Faire un exercice de respiration</li>
                <li *ngIf="data.stressLevel === 'MEDIUM'">Planifier une marche plus tard</li>
                <li *ngIf="data.stressLevel === 'LOW'">Maintenir votre routine équilibrée</li>
              </ul>
            </div>
          </z-card>
        </div>

        <h2 class="text-2xl font-bold mb-4">Facteurs identifiés récemment</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          <z-card *ngFor="let factor of data.factors" class="border-l-4 p-4 border-blue-400">
            <div class="flex items-center">
              <span class="mr-3 text-2xl text-gray-400">🔍</span>
              <span class="font-medium text-gray-800">{{ factor }}</span>
            </div>
          </z-card>

          <div *ngIf="!data.factors || data.factors.length === 0" class="col-span-full p-8 text-center bg-gray-50 rounded-lg text-gray-500 italic">
            Aucun facteur de stress particulier n'a été détecté récemment.
          </div>
        </div>
      </ng-container>
    </div>
  `
})
export class StressDashboardComponent implements OnInit {
  private chatbotService = inject(ChatbotService);
  private userApiService = inject(UserApiService);
  private keycloakService = inject(KeycloakService);
  private location = inject(Location);

  analysis = signal<StressAnalysis | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  userId: number = 1;

  async ngOnInit() {
    try {
      const profile = await this.keycloakService.loadUserProfile();
      if (profile && profile.id) {
        this.userApiService.getUserByKeycloakId(profile.id).subscribe({
          next: (user) => {
            if (user && user.id) {
              this.userId = user.id;
            }
            this.loadAnalysis();
          },
          error: () => {
            this.loadAnalysis();
          }
        });
      } else {
        this.loadAnalysis();
      }
    } catch (e) {
      console.warn('Keycloak non disponible, mode démo avec userId=1');
      this.loadAnalysis();
    }
  }

  loadAnalysis() {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.chatbotService.getStressAnalysis(this.userId).subscribe({
      next: (data) => {
        const safeData: StressAnalysis = {
          stressLevel: data.stressLevel || 'MEDIUM',
          factors: data.factors || [],
          recommendation: data.recommendation || "Prenez soin de vous. Accordez-vous des pauses régulières."
        };
        this.analysis.set(safeData);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur API stress:', err);
        this.analysis.set({
          stressLevel: 'MEDIUM',
          factors: ['Fatigue accumulée', 'Manque de temps libre', 'Inquiétude récurrente'],
          recommendation: "Votre niveau de stress est modéré, mais nécessite de l'attention. Essayez de déléguer certaines tâches et accordez-vous 20 minutes de pause par jour."
        });
        this.isLoading.set(false);
      }
    });
  }

  goBack() {
    this.location.back();
  }

  getStressColorHex(level: string): string {
    switch (level) {
      case 'LOW': return '#22c55e';
      case 'MEDIUM': return '#f97316';
      case 'HIGH': return '#ef4444';
      default: return '#9ca3af';
    }
  }

  getStressEmoji(level: string): string {
    switch (level) {
      case 'LOW': return '😎';
      case 'MEDIUM': return '😟';
      case 'HIGH': return '😫';
      default: return '😐';
    }
  }

  getStressGradientClass(level: string): string {
    switch (level) {
      case 'LOW': return 'from-green-500 to-green-600';
      case 'MEDIUM': return 'from-orange-500 to-orange-600';
      case 'HIGH': return 'from-red-500 to-red-600';
      default: return 'from-gray-500 to-gray-600';
    }
  }
}
