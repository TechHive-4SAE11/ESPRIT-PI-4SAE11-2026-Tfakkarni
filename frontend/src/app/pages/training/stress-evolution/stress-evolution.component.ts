import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { TrainingService } from '@/core/services/training.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

Chart.register(...registerables);

@Component({
  selector: 'app-stress-evolution',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <div class="flex items-center mb-8">
        <button z-button zType="outline" zSize="sm" (click)="goBack()">
          <z-icon zType="arrow-left" class="mr-2" /> Retour
        </button>
        <h1 class="text-2xl font-bold ml-4">📈 Évolution du stress</h1>
      </div>

      <z-card class="p-6">
        <h2 class="text-xl font-bold mb-4">Module : {{ moduleTitle }}</h2>
        <p class="text-gray-500 mb-6">Complété le {{ completedDate | date:'dd/MM/yyyy' }}</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div class="text-center p-4 bg-green-50 rounded-lg">
            <div class="text-3xl font-bold text-green-600">{{ stressBefore }}%</div>
            <div class="text-gray-600">Stress avant le module</div>
          </div>
          <div class="text-center p-4 bg-blue-50 rounded-lg">
            <div class="text-3xl font-bold text-blue-600">{{ stressAfter }}%</div>
            <div class="text-gray-600">Stress après le module</div>
          </div>
        </div>

        <div class="mb-6">
          <canvas id="stressChart"></canvas>
        </div>

        <div class="text-center p-4 bg-gray-50 rounded-lg">
          <p class="text-lg">
            📉 <span class="font-bold text-green-600">{{ stressImprovement }} points</span> de réduction du stress !
          </p>
          <p class="text-gray-500 mt-2">{{ impactMessage }}</p>
        </div>
      </z-card>
    </div>
  `
})
export class StressEvolutionComponent implements OnInit {
  private trainingService = inject(TrainingService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  moduleId: number = 0;
  moduleTitle: string = '';
  completedDate: string = '';
  stressBefore: number = 0;
  stressAfter: number = 0;
  stressImprovement: number = 0;
  impactMessage: string = '';

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.moduleId = +params['moduleId'] || 0;
      this.moduleTitle = params['moduleTitle'] || '';
      this.completedDate = params['completedDate'] || '';
      this.stressBefore = +params['stressBefore'] || 0;
      this.stressAfter = +params['stressAfter'] || 0;
      this.stressImprovement = +params['stressImprovement'] || 0;
      this.impactMessage = params['impactMessage'] || '';
      this.createChart();
    });
  }

  createChart() {
    const ctx = document.getElementById('stressChart') as HTMLCanvasElement;
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Avant le module', 'Après le module'],
        datasets: [{
          label: 'Niveau de stress (%)',
          data: [this.stressBefore, this.stressAfter],
          backgroundColor: ['#ef4444', '#22c55e'],
          borderRadius: 10,
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'top' },
          tooltip: { callbacks: { label: (ctx) => `${ctx.raw}%` } }
        },
        scales: {
          y: { min: 0, max: 100, title: { display: true, text: 'Stress (%)' } }
        }
      }
    });
  }

  goBack() {
    this.router.navigate(['/training/modules']);
  }
}
