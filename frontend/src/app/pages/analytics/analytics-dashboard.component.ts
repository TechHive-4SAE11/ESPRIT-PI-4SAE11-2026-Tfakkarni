import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';  // ← AJOUTÉ
import { PredictionService } from '../../services/prediction.service';
import { DashboardStats, PatientRisk } from '../../models/prediction.model';
import { NgApexchartsModule } from 'ng-apexcharts';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.css']
})
export class AnalyticsDashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  isLoading = false;

  public chartOptionsCancellations: any = {};
  public chartOptionsDoctors: any = {};

  // AJOUT: Injection du Router
  constructor(
    private predictionService: PredictionService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    console.log('🟡 Tentative de chargement des stats...');
    this.isLoading = true;

    this.predictionService.getDashboardStats().subscribe({
      next: (data) => {
        console.log('✅ Données reçues:', data);
        console.log('📊 Structure:', {
          total: data.totalAppointments,
          globalRate: data.globalNoShowRate,
          highRiskCount: data.highRiskPatients?.length
        });
        this.stats = data;
        this.initCharts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('❌ Erreur complète:', err);
        console.error('🔍 Status:', err.status);
        console.error('📝 Message:', err.message);
        this.isLoading = false;
      }
    });
  }

  // ✅ NOUVELLE MÉTHODE POUR LE BOUTON RETOUR
  goBackToAppointments(): void {
    this.router.navigate(['/appointments']);
  }

  initCharts(): void {
    if (!this.stats) return;

    const dayLabels = Object.keys(this.stats.cancellationsByDay || {});
    const dayData = Object.values(this.stats.cancellationsByDay || {});

    this.chartOptionsCancellations = {
      series: [{
        name: 'Annulations (%)',
        data: dayData
      }],
      chart: {
        type: 'bar',
        height: 350,
        toolbar: { show: false }
      },
      plotOptions: {
        bar: {
          horizontal: false,
          columnWidth: '55%',
          endingShape: 'rounded'
        }
      },
      dataLabels: { enabled: false },
      xaxis: {
        categories: dayLabels,
        title: { text: 'Jour de la semaine' }
      },
      yaxis: {
        title: { text: 'Taux d\'annulation (%)' },
        max: 100
      },
      colors: ['#F97316'],
      title: {
        text: 'Annulations par jour de semaine',
        align: 'left',
        style: { fontSize: '16px', fontWeight: 'bold' }
      }
    };

    const docLabels = Object.keys(this.stats.noShowRateByDoctor || {});
    const docData = Object.values(this.stats.noShowRateByDoctor || {}).map(n =>
      typeof n === 'number' ? Math.round(n * 10) / 10 : 0
    );

    this.chartOptionsDoctors = {
      series: [{
        name: 'Taux d\'absence (%)',
        data: docData
      }],
      chart: {
        type: 'bar',
        height: 350,
        toolbar: { show: false }
      },
      plotOptions: {
        bar: {
          horizontal: false,
          columnWidth: '55%',
          endingShape: 'rounded'
        }
      },
      dataLabels: { enabled: false },
      xaxis: {
        categories: docLabels,
        title: { text: 'Médecin' }
      },
      yaxis: {
        title: { text: 'Taux d\'absence (%)' },
        max: 100
      },
      colors: ['#3B82F6'],
      title: {
        text: 'Taux d\'absence par médecin',
        align: 'left',
        style: { fontSize: '16px', fontWeight: 'bold' }
      }
    };
  }

  getRiskColorClass(score: number): string {
    if (score >= 61) return 'text-red-500 bg-red-50 border-red-200';
    if (score >= 31) return 'text-orange-500 bg-orange-50 border-orange-200';
    return 'text-green-500 bg-green-50 border-green-200';
  }

  getRiskTextColorClass(score: number): string {
    if (score >= 61) return 'text-red-600';
    if (score >= 31) return 'text-orange-600';
    return 'text-green-600';
  }
}
