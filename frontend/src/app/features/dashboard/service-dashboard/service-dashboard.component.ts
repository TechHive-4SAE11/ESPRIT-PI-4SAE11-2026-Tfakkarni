import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ServiceStatusService, Microservice } from '../../../core/services/service-status.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-service-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  templateUrl: './service-dashboard.component.html',
  styleUrls: ['./service-dashboard.component.css']
})
export class ServiceDashboardComponent implements OnInit {

  // Signaux pour gérer l'état réactif
  services = signal<Microservice[]>([]);
  searchTerm = signal<string>('');
  isRefreshing = signal<boolean>(false);

  // Computed pour le filtrage
  filteredServices = computed(() => {
    const term = this.searchTerm().toLowerCase();
    return this.services().filter(s => 
      s.name.toLowerCase().includes(term) || 
      s.description.toLowerCase().includes(term) ||
      s.port.toString().includes(term)
    );
  });

  constructor(private statusService: ServiceStatusService) {}

  ngOnInit(): void {
    // Initialisation
    this.services.set(this.statusService.getServices());
    this.refreshAllStatuses();
  }

  refreshAllStatuses(): void {
    this.isRefreshing.set(true);
    
    // Mettre tous les statuts en "CHECKING"
    this.services.update(list => list.map(s => ({ ...s, status: 'CHECKING' })));

    // Préparer les appels HTTP observables
    const checks = this.services().map(service => 
      this.statusService.checkHealth(service.healthUrl)
    );

    // Exécuter en parallèle
    forkJoin(checks).subscribe({
      next: (results) => {
        this.services.update(list => 
          list.map((s, index) => ({ ...s, status: results[index] }))
        );
        this.isRefreshing.set(false);
      },
      error: () => {
        this.isRefreshing.set(false);
      }
    });
  }

  openSwagger(url: string): void {
    window.open(url, '_blank');
  }

  // Helper pour l'icone en fonction du nom
  getIconForService(name: string): string {
    const lower = name.toLowerCase();
    if (lower.includes('user')) return '👥';
    if (lower.includes('game')) return '🎮';
    if (lower.includes('medical')) return '🏥';
    if (lower.includes('alert')) return '🚨';
    if (lower.includes('assistant')) return '🤖';
    if (lower.includes('tracking')) return '📈';
    if (lower.includes('ml')) return '🧠';
    if (lower.includes('validation')) return '💊';
    if (lower.includes('iot')) return '⌚';
    if (lower.includes('analytics')) return '📊';
    return '📦';
  }
}
