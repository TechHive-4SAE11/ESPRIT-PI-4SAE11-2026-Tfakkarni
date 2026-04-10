import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TrainingService, Module } from '@/core/services/training.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';

@Component({
  selector: 'app-module-detail',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <div class="flex items-center mb-8">
        <z-button zType="outline" (click)="goBack()">← Retour</z-button>
      </div>

      <div *ngIf="isLoading()" class="flex justify-center p-8">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>

      <div *ngIf="errorMessage()" class="p-4 bg-red-100 text-red-700 rounded-md mb-6">
        {{ errorMessage() }}
      </div>

      <ng-container *ngIf="module() as mod">
        <div class="mb-8">
          <div class="flex gap-2 mb-3">
            <span class="px-3 py-1 bg-blue-100 text-blue-800 text-sm rounded-full uppercase font-semibold">{{ mod.category }}</span>
            <span class="px-3 py-1 bg-gray-100 text-gray-800 text-sm rounded-full uppercase font-semibold">{{ mod.difficulty }}</span>
          </div>
          <h1 class="text-4xl font-bold mb-4">{{ mod.title }}</h1>
          <p class="text-xl text-gray-600 mb-6">{{ mod.description }}</p>
          <div class="flex items-center text-gray-500 bg-gray-50 inline-block px-4 py-2 rounded-lg">
            <span class="mr-2 text-xl">⏱️</span> Durée estimée : {{ mod.duration }} minutes
          </div>
        </div>

        <z-card *ngIf="mod.videoUrl" class="mb-8 p-6">
          <h2 class="text-2xl font-semibold mb-4 border-b pb-2">Vidéo de formation</h2>
          <div class="aspect-w-16 aspect-h-9 relative" style="padding-bottom: 56.25%;">
            <iframe [src]="safeVideoUrl()"
                    class="absolute top-0 left-0 w-full h-full rounded-lg shadow-sm"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen>
            </iframe>
          </div>
        </z-card>

        <z-card *ngIf="mod.pdfUrl" class="mb-8 p-6">
          <h2 class="text-2xl font-semibold mb-4 border-b pb-2">Document de support (PDF)</h2>
          <div class="flex items-center justify-between bg-gray-50 p-4 rounded-lg">
            <span class="font-medium flex items-center"><span class="text-2xl mr-2">📄</span> {{ mod.title }}.pdf</span>
            <a [href]="mod.pdfUrl" target="_blank" class="bg-blue-600 text-white px-4 py-2 rounded shadow hover:bg-blue-700 transition">Télécharger PDF</a>
          </div>
        </z-card>

        <div class="flex justify-center mt-12 mb-8">
          <z-button zSize="lg" class="text-lg px-8 py-3 bg-green-600 hover:bg-green-700 w-full md:w-auto" (click)="!isCompleting() && markAsCompleted()">
            {{ isCompleting() ? 'Enregistrement...' : '✅ Marquer comme terminé' }}
          </z-button>
        </div>

        <div *ngIf="successMessage()" class="p-4 bg-green-100 text-green-700 rounded-md text-center font-medium">
          {{ successMessage() }}
        </div>
      </ng-container>
    </div>
  `
})
export class ModuleDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private trainingService = inject(TrainingService);
  private location = inject(Location);
  private sanitizer = inject(DomSanitizer);

  module = signal<Module | null>(null);
  isLoading = signal<boolean>(true);
  isCompleting = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  safeVideoUrl = signal<SafeResourceUrl | null>(null);

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadModule(parseInt(idParam, 10));
    } else {
      this.errorMessage.set("ID de module non fourni.");
      this.isLoading.set(false);
    }
  }

  loadModule(id: number) {
    this.isLoading.set(true);
    this.trainingService.getModuleById(id).subscribe({
      next: (data) => {
        this.module.set(data);
        if (data.videoUrl) {
          const videoId = this.extractYouTubeId(data.videoUrl);
          if (videoId) {
            this.safeVideoUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${videoId}`));
          } else {
            this.safeVideoUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(data.videoUrl));
          }
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set("Impossible de charger le module.");
        this.isLoading.set(false);
      }
    });
  }

  goBack() {
    this.location.back();
  }

  markAsCompleted() {
    const mod = this.module();
    if (!mod) return;

    this.isCompleting.set(true);
    this.trainingService.completeModule(1, mod.id, 100).subscribe({
      next: () => {
        this.isCompleting.set(false);
        this.successMessage.set("Félicitations ! Vous avez terminé ce module.");
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.isCompleting.set(false);
        this.errorMessage.set("Erreur lors de l'enregistrement de votre progression.");
        setTimeout(() => this.errorMessage.set(''), 3000);
      }
    });
  }

  private extractYouTubeId(url: string): string | null {
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);
    return (match && match[2].length === 11) ? match[2] : null;
  }
}
