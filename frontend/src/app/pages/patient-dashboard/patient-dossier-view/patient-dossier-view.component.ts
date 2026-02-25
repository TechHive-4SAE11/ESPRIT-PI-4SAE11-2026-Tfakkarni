import { Component, Input, Output, EventEmitter, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MedicalFolderService } from '@/core/services/medical-folder.service';
import type { MedicalFolder } from '@/core/services/medical-folder.service';
import { UserApiService } from '@/core/services/user-api.service';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { MedicalFolderDetailComponent } from '@/pages/medical-folders/medical-folder-detail/medical-folder-detail.component';

@Component({
  selector: 'app-patient-dossier-view',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardIconComponent,
    ZardSkeletonComponent,
    MedicalFolderDetailComponent,
  ],
  template: `
    <div class="space-y-4">
      @if (selectedFolderId() == null) {
        <div class="flex items-center justify-between gap-4">
          <h2 class="text-xl font-bold">Mon dossier médical</h2>
          @if (showBack) {
            <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
              <z-icon zType="arrow-left" class="mr-1" />
              Retour
            </button>
          }
        </div>
        <p class="text-muted-foreground text-sm">Vos dossiers médicaux. Cliquez sur un dossier pour voir le détail.</p>

        @if (loading()) {
          <z-skeleton class="h-32 w-full" />
        } @else if (folders().length === 0) {
          <z-card class="p-8 text-center">
            <p class="text-muted-foreground">Aucun dossier médical pour le moment.</p>
            <p class="text-sm text-muted-foreground mt-2">Votre médecin peut créer un dossier pour vous.</p>
          </z-card>
        } @else {
          <div class="grid gap-3">
            @for (f of folders(); track f.id) {
              <button type="button" (click)="openFolder(f.id)"
                class="w-full text-left rounded-xl border border-border bg-card p-4 hover:bg-muted/50 transition-colors">
                <div class="flex items-center justify-between gap-2">
                  <div class="flex items-center gap-3">
                    <z-icon zType="folder" class="h-8 w-8 text-primary" />
                    <div>
                      <p class="font-semibold">Dossier #{{ f.id }}</p>
                      <p class="text-sm text-muted-foreground">Médecin: {{ getDoctorName(f.doctorId) }} · Créé le {{ f.createdAt | date:'short' }}</p>
                    </div>
                  </div>
                  <z-icon zType="chevron-right" class="h-5 w-5 text-muted-foreground" />
                </div>
              </button>
            }
          </div>
        }
      } @else {
        <app-medical-folder-detail
          [folderId]="selectedFolderId()!"
          [readOnly]="true"
          (back)="closeDetail()"
        />
      }
    </div>
  `,
})
export class PatientDossierViewComponent implements OnInit {
  private readonly medicalFolderService = inject(MedicalFolderService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';
  @Input() showBack = true;
  @Output() goBack = new EventEmitter<void>();

  loading = signal(true);
  folders = signal<MedicalFolder[]>([]);
  selectedFolderId = signal<number | null>(null);
  doctorNameMap = signal<Record<string, string>>({});

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadFolders();
    }
  }

  loadFolders(): void {
    this.loading.set(true);
    this.medicalFolderService.getMedicalFoldersByPatient(this.keycloakId).subscribe({
      next: (list) => {
        this.folders.set(list);
        this.loadDoctorNames(list);
        this.loading.set(false);
      },
      error: () => {
        this.folders.set([]);
        this.loading.set(false);
      },
    });
  }

  getDoctorName(doctorId: string): string {
    return this.doctorNameMap()[doctorId] || doctorId;
  }

  private loadDoctorNames(folders: MedicalFolder[]): void {
    const ids = [...new Set(folders.map((f) => f.doctorId).filter(Boolean))];
    if (ids.length === 0) {
      this.doctorNameMap.set({});
      return;
    }
    forkJoin(
      ids.map((id) =>
        this.userApiService.getUserByKeycloakId(id).pipe(
          catchError(() => of(null)),
        )
      )
    ).subscribe((users) => {
      const map: Record<string, string> = {};
      ids.forEach((id, i) => {
        const u = users[i];
        map[id] = u ? `${u.firstName} ${u.lastName}`.trim() || id : id;
      });
      this.doctorNameMap.set(map);
    });
  }

  openFolder(id: number): void {
    this.selectedFolderId.set(id);
  }

  closeDetail(): void {
    this.selectedFolderId.set(null);
  }
}
