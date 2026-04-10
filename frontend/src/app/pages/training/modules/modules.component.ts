import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TrainingService, Module } from '@/core/services/training.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';

@Component({
  selector: 'app-modules',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardButtonComponent],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div class="flex items-center">
          <z-button zType="outline" (click)="goBack()">← Retour</z-button>
          <h1 class="text-3xl font-bold ml-4">Modules de formation</h1>
        </div>
      </div>

      <div class="flex gap-4 mb-6">
        <select [(ngModel)]="selectedCategory" class="p-2 border rounded-md">
          <option value="">Toutes les catégories</option>
          <option value="education">Éducation</option>
          <option value="stress">Stress</option>
          <option value="communication">Communication</option>
          <option value="activities">Activités</option>
        </select>
        <select [(ngModel)]="selectedDifficulty" class="p-2 border rounded-md">
          <option value="">Toutes les difficultés</option>
          <option value="BEGINNER">Débutant</option>
          <option value="INTERMEDIATE">Intermédiaire</option>
          <option value="ADVANCED">Avancé</option>
        </select>
      </div>

      <div *ngIf="isLoading()" class="flex justify-center p-8">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>

      <div *ngIf="!isLoading()" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <z-card *ngFor="let mod of filteredModules()" class="flex flex-col h-full hover:shadow-lg transition-shadow">
          <div class="p-5 flex flex-col h-full">
            <div class="flex justify-between items-start mb-2">
              <span class="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full uppercase font-semibold">{{ mod.category }}</span>
              <span class="px-2 py-1 bg-gray-100 text-gray-800 text-xs rounded-full uppercase font-semibold">{{ mod.difficulty }}</span>
            </div>
            <h3 class="text-xl font-bold mb-2">{{ mod.title }}</h3>
            <p class="text-gray-600 mb-4 flex-1 line-clamp-3">{{ mod.description }}</p>
            <div class="flex items-center justify-between mt-auto pt-4 border-t border-gray-100">
              <span class="text-sm text-gray-500 flex items-center">
                <span class="mr-1">⏱️</span> {{ mod.duration }} min
              </span>
              <z-button (click)="viewDetail(mod.id)">Voir détail</z-button>
            </div>
          </div>
        </z-card>
      </div>

      <div *ngIf="!isLoading() && filteredModules().length === 0" class="text-center p-12 bg-gray-50 rounded-lg">
        <p class="text-gray-500 text-lg">Aucun module trouvé pour ces critères.</p>
      </div>
    </div>
  `
})
export class ModulesComponent implements OnInit {
  private trainingService = inject(TrainingService);
  private router = inject(Router);
  private location = inject(Location);

  modules = signal<Module[]>([]);
  isLoading = signal<boolean>(true);

  selectedCategory = signal<string>('');
  selectedDifficulty = signal<string>('');

  filteredModules = computed(() => {
    return this.modules().filter(m => {
      const matchCat = !this.selectedCategory() || m.category === this.selectedCategory();
      const matchDiff = !this.selectedDifficulty() || m.difficulty === this.selectedDifficulty();
      return matchCat && matchDiff;
    });
  });

  ngOnInit() {
    this.loadModules();
  }

  loadModules() {
    this.isLoading.set(true);
    this.trainingService.getModules().subscribe({
      next: (data) => {
        this.modules.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  goBack() {
    this.location.back();
  }

  viewDetail(id: number) {
    this.router.navigate(['/training/modules', id]);
  }
}
