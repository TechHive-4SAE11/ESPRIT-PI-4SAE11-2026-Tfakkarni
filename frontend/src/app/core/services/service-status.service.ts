import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';

export interface Microservice {
  name: string;
  description: string;
  port: number;
  swaggerUrl: string;
  healthUrl: string;
  status: 'UP' | 'DOWN' | 'CHECKING';
}

@Injectable({
  providedIn: 'root'
})
export class ServiceStatusService {

  private readonly services: Microservice[] = [
    { name: 'User Service', description: 'Gestion des utilisateurs et authentification', port: 18081, swaggerUrl: 'http://localhost:18081/swagger-ui.html', healthUrl: 'http://localhost:18081/actuator/health', status: 'CHECKING' },
    { name: 'Game Service', description: 'Quiz, jeux mémoire et questions', port: 18082, swaggerUrl: 'http://localhost:18082/swagger-ui.html', healthUrl: 'http://localhost:18082/actuator/health', status: 'CHECKING' },
    { name: 'Tracking Service', description: 'Suivi des prescriptions et scores', port: 18083, swaggerUrl: 'http://localhost:18083/swagger-ui.html', healthUrl: 'http://localhost:18083/actuator/health', status: 'CHECKING' },
    { name: 'Alert Service', description: 'Gestion des alertes de sécurité', port: 18084, swaggerUrl: 'http://localhost:18084/swagger-ui.html', healthUrl: 'http://localhost:18084/actuator/health', status: 'CHECKING' },
    { name: 'ML Service', description: 'Modèles de Machine Learning', port: 18085, swaggerUrl: 'http://localhost:18085/swagger-ui.html', healthUrl: 'http://localhost:18085/actuator/health', status: 'CHECKING' },
    { name: 'Medical Service', description: 'Équipements et dossiers médicaux', port: 18086, swaggerUrl: 'http://localhost:18086/swagger-ui.html', healthUrl: 'http://localhost:18086/actuator/health', status: 'CHECKING' },
    { name: 'Validation Service', description: 'Validation des médicaments et interactions', port: 18087, swaggerUrl: 'http://localhost:18087/swagger-ui.html', healthUrl: 'http://localhost:18087/actuator/health', status: 'CHECKING' },
    { name: 'IoT Service', description: 'Intégration montres connectées & objets', port: 18088, swaggerUrl: 'http://localhost:18088/swagger-ui.html', healthUrl: 'http://localhost:18088/actuator/health', status: 'CHECKING' },
    { name: 'Assistant Service', description: 'Assistant vocal, IA générative & vidéos', port: 18089, swaggerUrl: 'http://localhost:18089/swagger-ui.html', healthUrl: 'http://localhost:18089/actuator/health', status: 'CHECKING' },
    { name: 'Analytics Service', description: 'Analyse des données et statistiques', port: 18090, swaggerUrl: 'http://localhost:18090/swagger-ui.html', healthUrl: 'http://localhost:18090/actuator/health', status: 'CHECKING' }
  ];

  constructor(private http: HttpClient) {}

  getServices(): Microservice[] {
    return this.services;
  }

  checkHealth(url: string): Observable<'UP' | 'DOWN'> {
    return new Observable<'UP' | 'DOWN'>(observer => {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000);

      // En mode "no-cors", le navigateur contourne la vérification CORS.
      // Il ne permet pas de lire le contenu de la réponse (statut 0), mais
      // si la requête aboutit, cela prouve que le serveur est bien allumé.
      fetch(url, { mode: 'no-cors', signal: controller.signal })
        .then(() => {
          clearTimeout(timeoutId);
          observer.next('UP');
          observer.complete();
        })
        .catch(() => {
          clearTimeout(timeoutId);
          observer.next('DOWN');
          observer.complete();
        });

      // Cleanup
      return () => {
        clearTimeout(timeoutId);
        controller.abort();
      };
    });
  }
}
