import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-accompagnement',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <div class="flex items-center mb-8">
        <button z-button zType="outline" zSize="sm" (click)="goBack()">
          <z-icon zType="arrow-left" class="mr-2" /> Retour
        </button>
        <h1 class="text-3xl font-bold ml-4">Accompagnement</h1>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

        <!-- Carte Chatbot -->
        <z-card class="p-6 text-center hover:shadow-xl transition-shadow cursor-pointer" (click)="goTo('/training/chat')">
          <div class="text-6xl mb-4">💬</div>
          <h2 class="text-xl font-bold mb-2">Assistant IA</h2>
          <p class="text-gray-500 text-sm">Posez vos questions sur la maladie d'Alzheimer, le stress, et obtenez des conseils personnalisés.</p>
          <z-button class="mt-4">Commencer</z-button>
        </z-card>

        <!-- Carte Modules -->
        <z-card class="p-6 text-center hover:shadow-xl transition-shadow cursor-pointer" (click)="goTo('/training/modules')">
          <div class="text-6xl mb-4">📚</div>
          <h2 class="text-xl font-bold mb-2">Modules de formation</h2>
          <p class="text-gray-500 text-sm">Accédez aux formations sur Alzheimer, communication, gestion du stress et prévention de l'épuisement.</p>
          <z-button class="mt-4">Voir les modules</z-button>
        </z-card>

        <!-- Carte Stress -->
        <z-card class="p-6 text-center hover:shadow-xl transition-shadow cursor-pointer" (click)="goTo('/training/stress')">
          <div class="text-6xl mb-4">📊</div>
          <h2 class="text-xl font-bold mb-2">Analyse de mon stress</h2>
          <p class="text-gray-500 text-sm">Évaluez votre niveau de stress, identifiez les facteurs et recevez des recommandations personnalisées.</p>
          <z-button class="mt-4">Analyser</z-button>
        </z-card>

      </div>
    </div>
  `
})
export class AccompagnementComponent {
  constructor(private router: Router) {}

  goTo(route: string) {
    this.router.navigate([route]);
  }

  goBack() {
    window.history.back();
  }
}
