import { Component, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '@/core/auth';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { HelperViewComponent } from './helper-view/helper-view.component';
import { PatientViewComponent } from './patient-view/patient-view.component';

type ViewMode = 'helper' | 'patient';

const VIEW_MODE_KEY = 'tfk_view_mode';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    ZardIconComponent,
    ZardButtonComponent,
    HelperViewComponent,
    PatientViewComponent,
  ],
  template: `
    @if (viewMode() === 'patient') {
      <!-- Patient mode: simple fullscreen UI, no dashboard chrome -->
      <app-patient-view
        [keycloakId]="keycloakId"
        (switchToHelper)="switchView('helper')"
      />
    } @else {
      <!-- Helper mode: full dashboard layout -->
      <app-dashboard-layout
        [menuGroups]="menuGroups"
        [pageTitle]="currentPageTitle()"
        basePath="/patient"
      >
        <!-- View mode switcher -->
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-2 bg-muted rounded-lg p-1">
            <button
              class="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors text-muted-foreground hover:text-foreground"
              (click)="switchView('patient')"
            >
              <z-icon zType="user" class="h-4 w-4" />
              Patient View
            </button>
            <button
              class="flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors bg-background text-foreground shadow-sm"
            >
              <z-icon zType="heart" class="h-4 w-4" />
              Helper
            </button>
          </div>
        </div>

        <app-helper-view
          [keycloakId]="keycloakId"
          (pageChange)="onHelperPageChange($event)"
        />
      </app-dashboard-layout>
    }
  `,
})
export class PatientDashboardComponent implements OnInit {
  viewMode = signal<ViewMode>('patient');
  keycloakId = '';

  @ViewChild(HelperViewComponent) helperView?: HelperViewComponent;
  @ViewChild(PatientViewComponent) patientView?: PatientViewComponent;

  menuGroups: SidebarMenuGroup[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly keycloakService: KeycloakService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const kc = this.keycloakService.getKeycloakInstance();
    this.keycloakId = kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';

    // Restore saved view mode
    try {
      const saved = localStorage.getItem(VIEW_MODE_KEY);
      if (saved === 'helper' || saved === 'patient') {
        this.viewMode.set(saved);
      }
    } catch {}

    this.buildMenuGroups();
  }

  switchView(mode: ViewMode): void {
    this.viewMode.set(mode);
    try {
      localStorage.setItem(VIEW_MODE_KEY, mode);
    } catch {}
    this.buildMenuGroups();
  }

  onHelperPageChange(_page: string): void {
    // Page change tracked internally by the helper view
  }

  currentPageTitle(): string {
    if (this.viewMode() === 'helper') {
      return this.helperView?.currentPage() ?? 'Home';
    }
    return this.patientView?.currentPage() ?? 'Home';
  }

  private buildMenuGroups(): void {
    // Only helper mode uses the dashboard sidebar
    this.menuGroups = [
      {
        label: 'Helper',
        items: [
          { icon: 'house', label: 'Home', action: () => this.helperView?.setPage('Home') },
          { icon: 'gamepad-2', label: 'Manage Games', action: () => this.helperView?.setPage('My Games') },
          { icon: 'bar-chart-3', label: 'Progress', action: () => this.helperView?.setPage('Progress') },
          { icon: 'pill', label: 'My Prescriptions', action: () => this.helperView?.setPage('Prescriptions') },
          { icon: 'brain', label: 'Quiz Management', action: () => this.helperView?.setPage('Quizzes') },
          { icon: 'shield', label: 'Equipment', action: () => this.helperView?.setPage('Equipment') },
        ],
      },
      {
        label: 'Actions',
        items: [
          { icon: 'plus-circle', label: 'Create Game', action: () => this.helperView?.setPage('Create Game') },
          { icon: 'map-pin', label: 'Guess the Place', action: () => this.helperView?.setPage('Places') },
        ],
      },
    ];
  }
}
