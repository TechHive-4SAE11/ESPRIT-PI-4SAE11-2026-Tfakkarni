import { Component, OnInit, signal, computed, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { DomSanitizer, type SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { AuthService } from '@/core/auth';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardDividerComponent } from '@/shared/components/divider';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';
import { GameService, type GameResponse, type OverviewStatsResponse } from '@/core/services/game.service';
import { AnalyticsService } from '@/core/services/analytics.service';
import type { PlatformOverviewResponse, DoctorEffectivenessResponse, BatchJobResult, DoctorMatchResponse, SeverePatientResponse } from '@/core/models/analytics.model';
import { ProfileComponent } from '@/pages/patient-dashboard/helper-view/profile/profile.component';
import { KeycloakService } from 'keycloak-angular';
import { DoctorRatingRankingComponent } from './doctor-rating-ranking.component';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayoutComponent,
    DoctorRatingRankingComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardTableImports,
    ZardSkeletonComponent,
    ZardButtonComponent,
    ZardInputDirective,
    ZardDividerComponent,
    ProfileComponent,
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/admin"
    >
      @switch (currentPage()) {

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- HOME OVERVIEW                                             -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Home') {
          <div class="space-y-6">
            <h2 class="text-xl font-bold tracking-tight">Tableau de bord</h2>

            <!-- Stats Cards -->
            <div class="grid gap-3 grid-cols-2 lg:grid-cols-4">
              <z-card>
                <div class="p-4">
                  <p class="text-xs text-muted-foreground">Utilisateurs</p>
                  <p class="text-2xl font-bold mt-1">{{ nonAdminUsers().length }}</p>
                  <p class="text-[11px] text-muted-foreground">{{ countByRole('patient') }} patients · {{ countByRole('doctor') }} médecins</p>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs text-muted-foreground">Jeux créés</p>
                  <p class="text-2xl font-bold mt-1">{{ stats()?.totalGames ?? 0 }}</p>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs text-muted-foreground">Tentatives</p>
                  <p class="text-2xl font-bold mt-1">{{ stats()?.totalAttempts ?? 0 }}</p>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs text-muted-foreground">Score moyen</p>
                  <p class="text-2xl font-bold mt-1">{{ (stats()?.averageScorePercentage ?? 0) | number:'1.0-0' }}%</p>
                </div>
              </z-card>
            </div>

            <!-- Bottom Row -->
            <div class="grid gap-6 lg:grid-cols-3">
              <z-card class="lg:col-span-2">
                <div class="p-6">
                  <div class="flex items-center justify-between mb-4">
                    <h3 class="text-lg font-semibold">Utilisateurs récents</h3>
                    <button z-button zType="outline" zSize="sm" (click)="setPage('Utilisateurs')">
                      Voir tout
                      <z-icon zType="arrow-right" class="ml-1 h-4 w-4" />
                    </button>
                  </div>
                  @if (nonAdminUsers().length > 0) {
                    <table z-table>
                      <thead z-table-header>
                        <tr z-table-row>
                          <th z-table-head>Nom</th>
                          <th z-table-head>Email</th>
                          <th z-table-head>Rôle</th>
                          <th z-table-head>Statut</th>
                        </tr>
                      </thead>
                      <tbody z-table-body>
                        @for (user of nonAdminUsers().slice(0, 5); track user.id) {
                          <tr z-table-row>
                            <td z-table-cell>
                              <div class="flex items-center gap-3">
                                <div class="flex items-center justify-center h-8 w-8 rounded-full text-xs font-bold"
                                     [class]="getAvatarClass(user.role)">
                                  {{ user.firstName.charAt(0) }}{{ user.lastName.charAt(0) }}
                                </div>
                                <span class="font-medium">{{ user.firstName }} {{ user.lastName }}</span>
                              </div>
                            </td>
                            <td z-table-cell class="text-muted-foreground">{{ user.email }}</td>
                            <td z-table-cell>
                              <z-badge [zType]="getRoleBadgeType(user.role)">
                                {{ getRoleLabel(user.role) }}
                              </z-badge>
                            </td>
                            <td z-table-cell>
                              <span class="inline-flex items-center gap-1.5 text-xs font-medium px-2 py-0.5 rounded-full"
                                    [class]="user.enabled
                                      ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400'
                                      : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'">
                                <span class="h-1.5 w-1.5 rounded-full" [class]="user.enabled ? 'bg-emerald-500' : 'bg-red-500'"></span>
                                {{ user.enabled ? 'Actif' : 'Désactivé' }}
                              </span>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  } @else {
                    <div class="flex flex-col items-center justify-center py-10 text-muted-foreground">
                      <z-icon zType="users" class="h-10 w-10 mb-2 opacity-40" />
                      <p>Aucun utilisateur trouvé</p>
                    </div>
                  }
                </div>
              </z-card>

              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-6">Répartition des rôles</h3>
                  <div class="space-y-5">
                    @for (entry of roleDistribution(); track entry.role) {
                      <div>
                        <div class="flex items-center justify-between mb-2">
                          <div class="flex items-center gap-2">
                            <div class="h-3 w-3 rounded-full" [class]="entry.dotClass"></div>
                            <span class="text-sm font-medium">{{ entry.label }}</span>
                          </div>
                          <span class="text-sm font-bold">{{ entry.count }}</span>
                        </div>
                        <div class="w-full bg-muted rounded-full h-2">
                          <div class="h-2 rounded-full transition-all duration-500"
                               [class]="entry.barClass"
                               [style.width.%]="nonAdminUsers().length ? (entry.count / nonAdminUsers().length * 100) : 0">
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              </z-card>
            </div>
          </div>
        }

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- USER MANAGEMENT                                           -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Utilisateurs') {
          <div class="space-y-6">
            <div>
              <h2 class="text-2xl font-bold tracking-tight">Gestion des utilisateurs</h2>
              <p class="text-muted-foreground mt-1">{{ filteredUsers().length }} utilisateur{{ filteredUsers().length > 1 ? 's' : '' }} trouvé{{ filteredUsers().length > 1 ? 's' : '' }}</p>
            </div>

            <!-- Search + Filter Bar -->
            <z-card>
              <div class="p-4 flex flex-col sm:flex-row gap-3">
                <div class="relative flex-1">
                  <z-icon zType="search" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <input z-input
                    type="text"
                    class="w-full pl-10"
                    placeholder="Rechercher par nom ou email..."
                    [ngModel]="searchQuery()"
                    (ngModelChange)="searchQuery.set($event)"
                  />
                </div>
                <div class="flex gap-2">
                @for (role of ['all', 'patient', 'doctor']; track role) {
                <button z-button
                [zType]="roleFilter() === role ? 'default' : 'outline'"
                zSize="sm"
                (click)="roleFilter.set(role); userPage.set(1)">
                {{ role === 'all' ? 'Tous' : getRoleLabel(role) }}
                </button>
                }
                </div>
              </div>
            </z-card>

            <!-- Users Table -->
            <z-card>
              <div class="p-0">
                @if (filteredUsers().length > 0) {
                  <table z-table>
                    <thead z-table-header>
                      <tr z-table-row>
                        <th z-table-head class="pl-6">Utilisateur</th>
                        <th z-table-head>Email</th>
                        <th z-table-head>Rôle</th>
                        <th z-table-head>Statut</th>
                        <th z-table-head>Date d'inscription</th>
                        <th z-table-head class="text-right pr-6">Actions</th>
                      </tr>
                    </thead>
                    <tbody z-table-body>
                      @for (user of pagedUsers(); track user.id) {
                        <tr z-table-row class="group">
                          <td z-table-cell class="pl-6">
                            <div class="flex items-center gap-3">
                              <div class="relative">
                                <div class="flex items-center justify-center h-9 w-9 rounded-full text-xs font-bold shrink-0"
                                     [class]="getAvatarClass(user.role)">
                                  {{ user.firstName.charAt(0) }}{{ user.lastName.charAt(0) }}
                                </div>
                                @if (!user.enabled) {
                                  <div class="absolute -bottom-0.5 -right-0.5 h-3.5 w-3.5 rounded-full bg-red-500 border-2 border-background"></div>
                                }
                              </div>
                              <div>
                                <p class="font-medium" [class]="!user.enabled ? 'text-muted-foreground line-through' : ''">{{ user.firstName }} {{ user.lastName }}</p>
                              </div>
                            </div>
                          </td>
                          <td z-table-cell class="text-muted-foreground">{{ user.email }}</td>
                          <td z-table-cell>
                            <z-badge [zType]="getRoleBadgeType(user.role)">
                              {{ getRoleLabel(user.role) }}
                            </z-badge>
                          </td>
                          <td z-table-cell>
                            <button
                              class="inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full transition-colors cursor-pointer"
                              [class]="user.enabled
                                ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-900/20 dark:text-emerald-400 dark:hover:bg-emerald-900/40'
                                : 'bg-red-50 text-red-700 hover:bg-red-100 dark:bg-red-900/20 dark:text-red-400 dark:hover:bg-red-900/40'"
                              (click)="toggleUserEnabled(user)">
                              <span class="h-1.5 w-1.5 rounded-full" [class]="user.enabled ? 'bg-emerald-500' : 'bg-red-500'"></span>
                              {{ user.enabled ? 'Actif' : 'Désactivé' }}
                            </button>
                          </td>
                          <td z-table-cell class="text-muted-foreground">{{ user.createdAt | date:'dd MMM yyyy, HH:mm' }}</td>
                          <td z-table-cell class="text-right pr-6">
                            <div class="flex items-center justify-end gap-1">
                              <!-- Toggle actif/inactif -->
                              <button z-button zType="ghost" zSize="sm"
                                class="h-8 w-8 p-0"
                                [title]="user.enabled ? 'Désactiver le compte' : 'Activer le compte'"
                                (click)="toggleUserEnabled(user)">
                                <z-icon [zType]="user.enabled ? 'x-circle' : 'check-circle'"
                                  class="h-4 w-4"
                                  [class]="user.enabled ? 'text-amber-500' : 'text-emerald-500'" />
                              </button>
                              <button z-button zType="ghost" zSize="sm" (click)="openEditModal(user)"
                                class="h-8 w-8 p-0" title="Modifier">
                                <z-icon zType="edit" class="h-4 w-4" />
                              </button>
                              <button z-button zType="ghost" zSize="sm" (click)="openDeleteModal(user)"
                                class="h-8 w-8 p-0 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer">
                                <z-icon zType="trash-2" class="h-4 w-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                } @else {
                  <div class="flex flex-col items-center justify-center py-16 text-muted-foreground">
                    <z-icon zType="search" class="h-12 w-12 mb-3 opacity-30" />
                    <p class="text-lg font-medium">Aucun résultat</p>
                    <p class="text-sm">Essayez un autre terme de recherche ou filtre</p>
                  </div>
                }

                <!-- ── Pagination ── -->
                @if (filteredUsers().length > userPerPage) {
                  <div class="flex items-center justify-between px-6 py-4 border-t">
                    <p class="text-sm text-muted-foreground">
                      Affichage
                      {{ (userPage() - 1) * userPerPage + 1 }}–{{ userPage() * userPerPage < filteredUsers().length ? userPage() * userPerPage : filteredUsers().length }}
                      sur {{ filteredUsers().length }}
                    </p>
                    <div class="flex items-center gap-1">
                      <!-- Prev -->
                      <button z-button zType="outline" zSize="sm"
                        class="h-8 w-8 p-0"
                        [disabled]="userPage() === 1"
                        (click)="setUserPage(userPage() - 1)">
                        <z-icon zType="chevron-left" class="h-4 w-4" />
                      </button>

                      <!-- Page numbers -->
                      @for (p of pagesArray(); track p) {
                        <button z-button
                          [zType]="p === userPage() ? 'default' : 'outline'"
                          zSize="sm"
                          class="h-8 w-8 p-0 text-xs"
                          (click)="setUserPage(p)">
                          {{ p }}
                        </button>
                      }

                      <!-- Next -->
                      <button z-button zType="outline" zSize="sm"
                        class="h-8 w-8 p-0"
                        [disabled]="userPage() === totalUserPages()"
                        (click)="setUserPage(userPage() + 1)">
                        <z-icon zType="chevron-right" class="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                }
              </div>
            </z-card>
          </div>
        }

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- ALL GAMES                                                 -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Jeux') {
          <div class="space-y-6">
            <div>
              <h2 class="text-2xl font-bold tracking-tight">Tous les jeux</h2>
              <p class="text-muted-foreground mt-1">{{ games().length }} jeu{{ games().length > 1 ? 'x' : '' }} sur la plateforme</p>
            </div>
            <z-card>
              <div class="p-0">
                @if (games().length > 0) {
                  <table z-table>
                    <thead z-table-header>
                      <tr z-table-row>
                        <th z-table-head class="pl-6">ID</th>
                        <th z-table-head>Titre</th>
                        <th z-table-head>Description</th>
                        <th z-table-head>Images</th>
                        <th z-table-head>Créé le</th>
                      </tr>
                    </thead>
                    <tbody z-table-body>
                      @for (game of pagedGames(); track game.id) {
                        <tr z-table-row>
                          <td z-table-cell class="pl-6 font-mono text-muted-foreground">#{{ game.id }}</td>
                          <td z-table-cell class="font-medium">{{ game.title }}</td>
                          <td z-table-cell class="text-muted-foreground max-w-[300px] truncate">{{ game.description }}</td>
                          <td z-table-cell>
                            <z-badge zType="secondary">{{ game.imageCount }} images</z-badge>
                          </td>
                          <td z-table-cell class="text-muted-foreground">{{ game.createdAt | date:'dd MMM yyyy' }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>

                  <!-- ── Game Pagination ── -->
                  @if (games().length > gamePerPage) {
                    <div class="flex items-center justify-between px-6 py-4 border-t">
                      <p class="text-sm text-muted-foreground">
                        Affichage
                        {{ (gamePage() - 1) * gamePerPage + 1 }}–{{ gamePage() * gamePerPage < games().length ? gamePage() * gamePerPage : games().length }}
                        sur {{ games().length }}
                      </p>
                      <div class="flex items-center gap-1">
                        <button z-button zType="outline" zSize="sm"
                          class="h-8 w-8 p-0"
                          [disabled]="gamePage() === 1"
                          (click)="setGamePage(gamePage() - 1)">
                          <z-icon zType="chevron-left" class="h-4 w-4" />
                        </button>
                        @for (p of gamePagesArray(); track p) {
                          <button z-button
                            [zType]="p === gamePage() ? 'default' : 'outline'"
                            zSize="sm"
                            class="h-8 w-8 p-0 text-xs"
                            (click)="setGamePage(p)">
                            {{ p }}
                          </button>
                        }
                        <button z-button zType="outline" zSize="sm"
                          class="h-8 w-8 p-0"
                          [disabled]="gamePage() === totalGamePages()"
                          (click)="setGamePage(gamePage() + 1)">
                          <z-icon zType="chevron-right" class="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  }
                } @else {
                  <div class="flex flex-col items-center justify-center py-16 text-muted-foreground">
                    <z-icon zType="gamepad-2" class="h-12 w-12 mb-3 opacity-30" />
                    <p>Aucun jeu créé pour le moment</p>
                  </div>
                }
              </div>
            </z-card>
          </div>
        }

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- ANALYTICS                                                 -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Statistiques') {
          <div class="space-y-6">
            <h2 class="text-xl font-bold tracking-tight">Analytique</h2>

            <!-- Platform Overview Cards -->
            <div class="grid gap-3 grid-cols-2 lg:grid-cols-4">
              <z-card class="p-4">
                <p class="text-xs text-muted-foreground">Score moyen</p>
                <p class="text-2xl font-bold mt-1">{{ (platformOverview()?.platformAvgScore ?? 0) | number:'1.0-0' }}</p>
              </z-card>
              <z-card class="p-4">
                <p class="text-xs text-muted-foreground">Tentatives de jeu</p>
                <p class="text-2xl font-bold mt-1">{{ platformOverview()?.totalGameAttempts ?? 0 }}</p>
              </z-card>
              <z-card class="p-4">
                <p class="text-xs text-muted-foreground">Incidents IoT</p>
                <p class="text-2xl font-bold mt-1">{{ platformOverview()?.totalIncidents ?? 0 }}</p>
              </z-card>
              <z-card class="p-4">
                <p class="text-xs text-muted-foreground">Médecins signalés</p>
                <p class="text-2xl font-bold mt-1" [class]="(platformOverview()?.redFlagDoctorCount ?? 0) > 0 ? 'text-red-600' : ''">{{ platformOverview()?.redFlagDoctorCount ?? 0 }}</p>
              </z-card>
            </div>

            <!-- Stage Distribution + Domain Weakness -->
            <div class="grid gap-6 md:grid-cols-2">
              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-4">Répartition des stades</h3>
                  @if (platformOverview()?.stageDistribution) {
                    <div class="space-y-3">
                      @for (entry of stageEntries(); track entry.stage) {
                        <div>
                          <div class="flex items-center justify-between mb-1.5">
                            <div class="flex items-center gap-2">
                              <div class="h-3 w-3 rounded-full" [class]="getStageColor(entry.stage)"></div>
                              <span class="text-sm font-medium">{{ getStageLabel(entry.stage) }}</span>
                            </div>
                            <span class="text-sm font-bold">{{ entry.count }}</span>
                          </div>
                          <div class="w-full bg-muted rounded-full h-2">
                            <div class="h-2 rounded-full transition-all duration-500"
                                 [class]="getStageBarColor(entry.stage)"
                                 [style.width.%]="stageTotal() > 0 ? (entry.count / stageTotal() * 100) : 0">
                            </div>
                          </div>
                        </div>
                      }
                    </div>
                  } @else {
                    <p class="text-sm text-muted-foreground">Aucune donnée disponible</p>
                  }
                </div>
              </z-card>

              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-4">Faiblesses cognitives</h3>
                  @if (domainWeaknessEntries().length) {
                    <div class="space-y-3">
                      @for (entry of domainWeaknessEntries(); track entry.domain) {
                        <div class="flex items-center justify-between">
                          <span class="text-sm">{{ entry.domain }}</span>
                          <div class="flex items-center gap-2">
                            <div class="w-24 bg-muted rounded-full h-2">
                              <div class="h-2 rounded-full bg-orange-500 transition-all duration-500"
                                   [style.width.%]="entry.pct">
                              </div>
                            </div>
                            <span class="text-sm font-bold w-10 text-right">{{ entry.pct | number:'1.0-0' }}%</span>
                          </div>
                        </div>
                      }
                    </div>
                  } @else {
                    <p class="text-sm text-muted-foreground">Aucune donnée disponible</p>
                  }
                </div>
              </z-card>
            </div>

            <!-- Doctor Ranking Table -->
            <z-card>
              <div class="p-6">
                <h3 class="text-lg font-semibold mb-4">Classement des médecins</h3>
                @if (doctorRanking().length) {
                  <table z-table>
                    <thead z-table-header>
                      <tr z-table-row>
                        <th z-table-head>Médecin</th>
                        <th z-table-head>Patients</th>
                        <th z-table-head>Stabilisation</th>
                        <th z-table-head>Déclin</th>
                        <th z-table-head>Présence RDV</th>
                        <th z-table-head>Signalements</th>
                      </tr>
                    </thead>
                    <tbody z-table-body>
                      @for (doc of doctorRanking(); track doc.doctorKeycloakId) {
                        <tr z-table-row>
                          <td z-table-cell class="font-medium">{{ doc.doctorName || doc.doctorKeycloakId }}</td>
                          <td z-table-cell>{{ doc.patientCount }}</td>
                          <td z-table-cell>
                            <span class="text-emerald-600 font-medium">{{ (doc.stabilizationRate * 100) | number:'1.0-0' }}%</span>
                          </td>
                          <td z-table-cell>
                            <span [class]="doc.declineRate > 0.3 ? 'text-red-600 font-medium' : 'text-muted-foreground'">
                              {{ (doc.declineRate * 100) | number:'1.0-0' }}%
                            </span>
                          </td>
                          <td z-table-cell>{{ (doc.appointmentShowRate * 100) | number:'1.0-0' }}%</td>
                          <td z-table-cell>
                            @if (doc.riskFlags.length > 0) {
                              <div class="flex flex-wrap gap-1">
                                @for (flag of doc.riskFlags; track flag) {
                                  <z-badge zType="destructive" class="text-xs">{{ flag }}</z-badge>
                                }
                              </div>
                            } @else {
                              <z-badge zType="outline" class="text-xs">Aucun</z-badge>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                } @else {
                  <p class="text-sm text-muted-foreground">Aucune donnée de classement disponible</p>
                }
              </div>
            </z-card>
            <z-card>
              <div class="p-6">
                <app-doctor-rating-ranking />
              </div>
            </z-card>
          </div>
        }

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- BATCH JOBS                                                -->
        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- DOCTOR-PATIENT MATCHING                                     -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Matching') {
          <div class="space-y-6">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-xl font-bold tracking-tight">Matching Médecin–Patient</h2>
                <p class="text-sm text-muted-foreground mt-1">Patients à risque et recommandations de médecins</p>
              </div>
              <button z-button zType="outline" zSize="sm" (click)="loadMatching()">
                <z-icon zType="refresh-cw" class="h-4 w-4 mr-1" /> Actualiser
              </button>
            </div>

            <!-- Ranked Doctors -->
            <z-card>
              <div class="p-6">
                <h3 class="text-lg font-semibold mb-4">Classement des médecins (score composite)</h3>
                <p class="text-xs text-muted-foreground mb-4">Score = Stabilisation×50% + Note patient×30% + Présence RDV×20%</p>
                @if (rankedDoctors().length) {
                  <table z-table>
                    <thead z-table-header>
                      <tr z-table-row>
                        <th z-table-head>#</th>
                        <th z-table-head>Médecin</th>
                        <th z-table-head>Score</th>
                        <th z-table-head>Note</th>
                        <th z-table-head>Stabilisation</th>
                        <th z-table-head>Présence RDV</th>
                        <th z-table-head>Patients</th>
                        <th z-table-head>Statut</th>
                      </tr>
                    </thead>
                    <tbody z-table-body>
                      @for (doc of rankedDoctors(); track doc.doctorKeycloakId; let i = $index) {
                        <tr z-table-row [class]="doc.hasRiskFlags ? 'opacity-60' : ''">
                          <td z-table-cell class="font-bold text-muted-foreground">{{ i + 1 }}</td>
                          <td z-table-cell class="font-medium">{{ doc.doctorName }}</td>
                          <td z-table-cell>
                            <span class="font-bold" [class]="doc.matchScore >= 60 ? 'text-emerald-600' : doc.matchScore >= 40 ? 'text-amber-600' : 'text-red-600'">
                              {{ doc.matchScore | number:'1.1-1' }}
                            </span>
                          </td>
                          <td z-table-cell>
                            <div class="flex items-center gap-1">
                              <span class="text-amber-500">★</span>
                              <span>{{ doc.averageRating | number:'1.1-1' }}</span>
                              <span class="text-xs text-muted-foreground">({{ doc.totalRatings }})</span>
                            </div>
                          </td>
                          <td z-table-cell>
                            <span class="text-emerald-600">{{ doc.stabilizationRate | number:'1.0-0' }}%</span>
                          </td>
                          <td z-table-cell>{{ doc.appointmentShowRate | number:'1.0-0' }}%</td>
                          <td z-table-cell>{{ doc.currentPatientCount }}</td>
                          <td z-table-cell>
                            @if (doc.hasRiskFlags) {
                              <z-badge zType="destructive">Signalé</z-badge>
                            } @else {
                              <z-badge zType="outline">OK</z-badge>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                } @else {
                  <p class="text-sm text-muted-foreground">Aucune donnée. Lancez les tâches de calcul d'abord.</p>
                }
              </div>
            </z-card>

            <!-- Severe Patients with Recommendations -->
            <z-card>
              <div class="p-6">
                <h3 class="text-lg font-semibold mb-4">Patients à risque — Recommandations</h3>
                @if (severePatients().length) {
                  <table z-table>
                    <thead z-table-header>
                      <tr z-table-row>
                        <th z-table-head>Patient</th>
                        <th z-table-head>Stade</th>
                        <th z-table-head>Score global</th>
                        <th z-table-head>Score cognitif</th>
                        <th z-table-head>Médecin actuel</th>
                        <th z-table-head>Médecin recommandé</th>
                        <th z-table-head>Score match</th>
                      </tr>
                    </thead>
                    <tbody z-table-body>
                      @for (p of severePatients(); track p.patientKeycloakId) {
                        <tr z-table-row>
                          <td z-table-cell class="font-medium">{{ p.patientName }}</td>
                          <td z-table-cell>
                            <z-badge [zType]="p.stage === 'SEVERE' ? 'destructive' : 'default'">{{ getStageLabel(p.stage) }}</z-badge>
                          </td>
                          <td z-table-cell>{{ p.overallScore | number:'1.0-0' }}</td>
                          <td z-table-cell>{{ p.cognitiveScore | number:'1.0-0' }}</td>
                          <td z-table-cell class="text-muted-foreground">{{ p.currentDoctorName || 'Non assigné' }}</td>
                          <td z-table-cell>
                            @if (p.recommendedDoctorName) {
                              <span class="font-medium text-emerald-600">{{ p.recommendedDoctorName }}</span>
                            } @else {
                              <span class="text-muted-foreground">—</span>
                            }
                          </td>
                          <td z-table-cell>
                            @if (p.recommendedDoctorMatchScore) {
                              <span class="font-bold">{{ p.recommendedDoctorMatchScore | number:'1.1-1' }}</span>
                            } @else {
                              <span class="text-muted-foreground">—</span>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                } @else {
                  <div class="flex flex-col items-center py-8 text-muted-foreground">
                    <z-icon zType="check" class="h-10 w-10 mb-2 opacity-40" />
                    <p>Aucun patient à risque élevé détecté</p>
                  </div>
                }
              </div>
            </z-card>
          </div>
        }

        @case ('Tâches') {
          <div class="space-y-6">
            <h2 class="text-xl font-bold tracking-tight">Tâches planifiées</h2>

            <div class="grid gap-4 md:grid-cols-3">
              <!-- Run All -->
              <z-card>
                <div class="p-6 space-y-4">
                  <div class="flex items-center gap-3">
                    <div class="flex items-center justify-center h-10 w-10 rounded-xl bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400">
                      <z-icon zType="zap" class="h-5 w-5" />
                    </div>
                    <div>
                      <h4 class="font-semibold">Tout recalculer</h4>
                      <p class="text-xs text-muted-foreground">Scores patients + efficacité médecins</p>
                    </div>
                  </div>
                  <button z-button class="w-full" [disabled]="jobRunning()" (click)="runJob('all')">
                    @if (jobRunning() === 'all') {
                      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      En cours...
                    } @else {
                      Lancer
                    }
                  </button>
                </div>
              </z-card>

              <!-- Patient Scores -->
              <z-card>
                <div class="p-6 space-y-4">
                  <div class="flex items-center gap-3">
                    <div class="flex items-center justify-center h-10 w-10 rounded-xl bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">
                      <z-icon zType="brain" class="h-5 w-5" />
                    </div>
                    <div>
                      <h4 class="font-semibold">Scores patients</h4>
                      <p class="text-xs text-muted-foreground">Recalculer tous les scores + feature gates</p>
                    </div>
                  </div>
                  <button z-button class="w-full" zType="outline" [disabled]="jobRunning()" (click)="runJob('patients')">
                    @if (jobRunning() === 'patients') {
                      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-foreground mr-2"></div>
                      En cours...
                    } @else {
                      Lancer
                    }
                  </button>
                </div>
              </z-card>

              <!-- Doctor Effectiveness -->
              <z-card>
                <div class="p-6 space-y-4">
                  <div class="flex items-center gap-3">
                    <div class="flex items-center justify-center h-10 w-10 rounded-xl bg-violet-100 text-violet-600 dark:bg-violet-900/30 dark:text-violet-400">
                      <z-icon zType="heart" class="h-5 w-5" />
                    </div>
                    <div>
                      <h4 class="font-semibold">Efficacité médecins</h4>
                      <p class="text-xs text-muted-foreground">Recalculer les métriques de qualité</p>
                    </div>
                  </div>
                  <button z-button class="w-full" zType="outline" [disabled]="jobRunning()" (click)="runJob('doctors')">
                    @if (jobRunning() === 'doctors') {
                      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-foreground mr-2"></div>
                      En cours...
                    } @else {
                      Lancer
                    }
                  </button>
                </div>
              </z-card>
            </div>

            <!-- Last Job Result -->
            @if (lastJobResult()) {
              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-3">Dernier résultat</h3>
                  <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <div>
                      <p class="text-xs text-muted-foreground">Tâche</p>
                      <p class="font-medium">{{ lastJobResult()!.jobName }}</p>
                    </div>
                    <div>
                      <p class="text-xs text-muted-foreground">Statut</p>
                      <z-badge [zType]="lastJobResult()!.status === 'SUCCESS' ? 'default' : 'destructive'">
                        {{ lastJobResult()!.status }}
                      </z-badge>
                    </div>
                    <div>
                      <p class="text-xs text-muted-foreground">Traités</p>
                      <p class="font-medium">{{ lastJobResult()!.processedCount }} ({{ lastJobResult()!.errorCount }} erreurs)</p>
                    </div>
                    <div>
                      <p class="text-xs text-muted-foreground">Durée</p>
                      <p class="font-medium">{{ lastJobResult()!.durationMs }}ms</p>
                    </div>
                  </div>
                </div>
              </z-card>
            }
          </div>
        }

        <!-- ══════════════════════════════════════════════════════════ -->
        <!-- MONITORING                                                -->
        <!-- ══════════════════════════════════════════════════════════ -->
        @case ('Monitoring') {
          <div class="space-y-6">

            <!-- Header -->
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-2xl font-bold tracking-tight">Monitoring</h2>
                <p class="text-muted-foreground mt-1">Métriques temps réel des microservices — actualisation toutes les 30 s</p>
              </div>
              <a [href]="grafanaDashboardUrl" target="_blank" rel="noopener noreferrer">
                <button z-button zType="outline" zSize="sm">
                  <z-icon zType="arrow-up-right" class="h-4 w-4 mr-1" />
                  Ouvrir Grafana
                </button>
              </a>
            </div>

            <!-- Services Up/Down — stat, full width -->
            <z-card>
              <div class="p-4">
                <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">Services Up / Down</p>
                <iframe [src]="grafanaPanel(1)" class="w-full border-0 rounded" style="height:120px" loading="lazy"></iframe>
              </div>
            </z-card>

            <!-- HTTP metrics row -->
            <div class="grid gap-4 md:grid-cols-2">
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">HTTP Request Rate (req/s)</p>
                  <iframe [src]="grafanaPanel(4)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">HTTP Avg Response Time (ms)</p>
                  <iframe [src]="grafanaPanel(5)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
            </div>

            <!-- CPU + Error rate row -->
            <div class="grid gap-4 md:grid-cols-2">
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">Process CPU Usage (%)</p>
                  <iframe [src]="grafanaPanel(8)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">HTTP Error Rate (5xx)</p>
                  <iframe [src]="grafanaPanel(9)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
            </div>

            <!-- JVM Memory row -->
            <div class="grid gap-4 md:grid-cols-2">
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">JVM Heap Memory (MB)</p>
                  <iframe [src]="grafanaPanel(2)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
              <z-card>
                <div class="p-4">
                  <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">JVM Non-Heap Memory (MB)</p>
                  <iframe [src]="grafanaPanel(3)" class="w-full border-0 rounded" style="height:260px" loading="lazy"></iframe>
                </div>
              </z-card>
            </div>

            <!-- Scrape target status table — full width -->
            <z-card>
              <div class="p-4">
                <p class="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-3">État des services (Scrape Targets)</p>
                <iframe [src]="grafanaPanel(10)" class="w-full border-0 rounded" style="height:320px" loading="lazy"></iframe>
              </div>
            </z-card>
          </div>
        }

        @case ('Mon Profil') {
          <app-profile [keycloakId]="adminKeycloakId" (goBack)="setPage('Home')" />
        }
      }

      <!-- ══════════════════════════════════════════════════════════ -->
      <!-- EDIT USER MODAL                                           -->
      <!-- ══════════════════════════════════════════════════════════ -->
      @if (showEditModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" (click)="closeModals()"></div>
          <div class="relative bg-background border rounded-xl shadow-2xl w-full max-w-md mx-4 animate-in fade-in zoom-in-95 max-h-[90vh] overflow-y-auto">
            <div class="p-6">
              <div class="flex items-center justify-between mb-6">
                <h3 class="text-lg font-semibold">Modifier l'utilisateur</h3>
                <button z-button zType="ghost" zSize="sm" class="h-8 w-8 p-0" (click)="closeModals()">
                  <z-icon zType="x" class="h-4 w-4" />
                </button>
              </div>

              @if (selectedUser()) {
                <div class="space-y-4">
                  <!-- User avatar display -->
                  <div class="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                    <div class="flex items-center justify-center h-10 w-10 rounded-full text-sm font-bold"
                         [class]="getAvatarClass(selectedUser()!.role)">
                      {{ selectedUser()!.firstName.charAt(0) }}{{ selectedUser()!.lastName.charAt(0) }}
                    </div>
                    <div>
                      <p class="font-medium">{{ selectedUser()!.firstName }} {{ selectedUser()!.lastName }}</p>
                      <p class="text-sm text-muted-foreground">{{ selectedUser()!.email }}</p>
                    </div>
                  </div>

                  <!-- Profile Fields -->
                  <div class="grid grid-cols-2 gap-3">
                    <div>
                      <label class="text-sm font-medium mb-1.5 block">Prénom</label>
                      <input z-input type="text" class="w-full" [(ngModel)]="editForm.firstName" />
                    </div>
                    <div>
                      <label class="text-sm font-medium mb-1.5 block">Nom</label>
                      <input z-input type="text" class="w-full" [(ngModel)]="editForm.lastName" />
                    </div>
                  </div>
                  <div>
                    <label class="text-sm font-medium mb-1.5 block">Email</label>
                    <input z-input type="email" class="w-full" [(ngModel)]="editForm.email" />
                  </div>

                  <z-divider />

                  <!-- Role change -->
                  <div>
                    <label class="text-sm font-medium mb-1.5 block">Rôle</label>
                    <div class="flex gap-2">
                      @for (r of ['patient', 'doctor']; track r) {
                        <button z-button
                          [zType]="editForm.role === r ? 'default' : 'outline'"
                          zSize="sm"
                          class="flex-1"
                          (click)="editForm.role = r">
                          {{ getRoleLabel(r) }}
                        </button>
                      }
                    </div>
                  </div>

                  <z-divider />

                  <!-- Reset Password Section -->
                  <div>
                    <label class="text-sm font-medium mb-1.5 block flex items-center gap-2">
                      <z-icon zType="lock" class="h-4 w-4" />
                      Réinitialiser le mot de passe
                    </label>
                    <input z-input
                      type="password"
                      class="w-full"
                      placeholder="Nouveau mot de passe (laisser vide pour ne pas changer)"
                      [(ngModel)]="editForm.newPassword"
                    />
                    <p class="text-xs text-muted-foreground mt-1">Minimum 6 caractères. Laisser vide pour ne pas modifier.</p>
                  </div>
                </div>
              }

              @if (modalMessage()) {
                <div class="mt-4 text-sm px-3 py-2 rounded-md"
                     [class]="modalSuccess() ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400' : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'">
                  {{ modalMessage() }}
                </div>
              }

              <div class="flex justify-end gap-2 mt-6">
                <button z-button zType="outline" (click)="closeModals()">Annuler</button>
                <button z-button [disabled]="isSubmitting()" (click)="updateUser()">
                  @if (isSubmitting()) {
                    <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  }
                  Sauvegarder
                </button>
              </div>
            </div>
          </div>
        </div>
      }

      <!-- ══════════════════════════════════════════════════════════ -->
      <!-- DELETE USER MODAL                                         -->
      <!-- ══════════════════════════════════════════════════════════ -->
      @if (showDeleteModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" (click)="closeModals()"></div>
          <div class="relative bg-background border rounded-xl shadow-2xl w-full max-w-sm mx-4 animate-in fade-in zoom-in-95">
            <div class="p-6">
              <div class="flex items-center justify-center h-12 w-12 rounded-full bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400 mx-auto mb-4">
                <z-icon zType="trash-2" class="h-6 w-6" />
              </div>
              <h3 class="text-lg font-semibold text-center">Supprimer l'utilisateur ?</h3>
              <p class="text-sm text-muted-foreground text-center mt-2">
                Êtes-vous sûr de vouloir supprimer
                <strong>{{ selectedUser()?.firstName }} {{ selectedUser()?.lastName }}</strong> ?
                Cette action est irréversible.
              </p>

              @if (modalMessage()) {
                <div class="mt-4 text-sm px-3 py-2 rounded-md bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400">
                  {{ modalMessage() }}
                </div>
              }

              <div class="flex gap-2 mt-6">
                <button z-button zType="outline" class="flex-1" (click)="closeModals()">Annuler</button>
                <button z-button class="flex-1 bg-red-600 hover:bg-red-700 text-white border-red-600"
                  [disabled]="isSubmitting()" (click)="deleteUser()">
                  @if (isSubmitting()) {
                    <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  }
                  Supprimer
                </button>
              </div>
            </div>
          </div>
        </div>
      }

    </app-dashboard-layout>
  `,
  styles: [`
    :host ::ng-deep .animate-in {
      animation: animateIn 0.2s ease-out;
    }
    @keyframes animateIn {
      from { opacity: 0; transform: scale(0.95); }
      to { opacity: 1; transform: scale(1); }
    }
  `],
})
export class AdminDashboardComponent implements OnInit {
  // ─── State ──────────────────────────────────────────────
  currentPage = signal('Home');
  users = signal<UserInfo[]>([]);
  games = signal<GameResponse[]>([]);
  stats = signal<OverviewStatsResponse | null>(null);

  // Analytics state
  platformOverview = signal<PlatformOverviewResponse | null>(null);
  doctorRanking = signal<DoctorEffectivenessResponse[]>([]);
  lastJobResult = signal<BatchJobResult | null>(null);
  jobRunning = signal<string | null>(null);

  // Matching state
  rankedDoctors = signal<DoctorMatchResponse[]>([]);
  severePatients = signal<SeverePatientResponse[]>([]);

  // Search / Filter — both signals for reactivity
  searchQuery = signal('');
  roleFilter = signal<string>('all');

  // Pagination utilisateurs
  userPage    = signal(1);
  userPerPage = 8;

  // Pagination jeux
  gamePage    = signal(1);
  gamePerPage = 8;

  // Modal state
  showEditModal = signal(false);
  showDeleteModal = signal(false);
  selectedUser = signal<UserInfo | null>(null);
  isSubmitting = signal(false);
  modalMessage = signal('');
  modalSuccess = signal(false);

  // Edit form (includes reset password)
  editForm = { firstName: '', lastName: '', email: '', role: '', newPassword: '' };

  // ─── Menu ───────────────────────────────────────────────
  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'Navigation',
      items: [
        { icon: 'house', label: 'Accueil', action: () => this.setPage('Home') },
        { icon: 'users', label: 'Utilisateurs', action: () => this.setPage('Utilisateurs') },
        { icon: 'gamepad-2', label: 'Jeux', action: () => this.setPage('Jeux') },
        { icon: 'bar-chart-3', label: 'Analytique', action: () => this.setPage('Statistiques') },
        { icon: 'target', label: 'Matching', action: () => this.setPage('Matching') },
        { icon: 'zap', label: 'Tâches', action: () => this.setPage('Tâches') },
        { icon: 'activity', label: 'Monitoring', action: () => this.setPage('Monitoring') },
      ],
    },
    {
      label: 'Compte',
      items: [
        { icon: 'user', label: 'Mon Profil', action: () => this.setPage('Mon Profil') },
      ],
    },
  ];

  // ─── Computed — exclude admins from all user lists ─────
  nonAdminUsers = computed(() => this.users().filter(u => u.role !== 'admin'));

  filteredUsers = computed(() => {
    let result = this.nonAdminUsers();
    const filter = this.roleFilter();
    if (filter !== 'all') {
      result = result.filter(u => u.role === filter);
    }
    const q = this.searchQuery().toLowerCase().trim();
    if (q) {
      result = result.filter(u =>
        u.firstName.toLowerCase().includes(q) ||
        u.lastName.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q)
      );
    }
    return result;
  });

  pagedUsers    = computed(() => {
    const page  = this.userPage();
    const start = (page - 1) * this.userPerPage;
    return this.filteredUsers().slice(start, start + this.userPerPage);
  });

  totalUserPages = computed(() => Math.max(1, Math.ceil(this.filteredUsers().length / this.userPerPage)));

  roleDistribution = computed(() => {
    const all = this.nonAdminUsers();
    return [
      { role: 'patient', label: 'Patients', count: all.filter(u => u.role === 'patient').length, dotClass: 'bg-blue-500', barClass: 'bg-blue-500' },
      { role: 'doctor', label: 'Médecins', count: all.filter(u => u.role === 'doctor').length, dotClass: 'bg-violet-500', barClass: 'bg-violet-500' },
    ];
  });

  enabledCount = computed(() => this.nonAdminUsers().filter(u => u.enabled).length);

  pagesArray = computed(() => {
    const total = this.totalUserPages();
    return Array.from({ length: total }, (_, i) => i + 1);
  });

  // Game pagination computed
  pagedGames = computed(() => {
    const page = this.gamePage();
    const start = (page - 1) * this.gamePerPage;
    return this.games().slice(start, start + this.gamePerPage);
  });
  totalGamePages = computed(() => Math.max(1, Math.ceil(this.games().length / this.gamePerPage)));
  gamePagesArray = computed(() => Array.from({ length: this.totalGamePages() }, (_, i) => i + 1));

  // Analytics computed
  stageEntries = computed(() => {
    const dist = this.platformOverview()?.stageDistribution;
    if (!dist) return [];
    return Object.entries(dist).map(([stage, count]) => ({ stage, count }));
  });
  stageTotal = computed(() => this.stageEntries().reduce((sum, e) => sum + e.count, 0));

  domainWeaknessEntries = computed(() => {
    const weakness = this.platformOverview()?.cognitiveDomainWeakness;
    if (!weakness) return [];
    return Object.entries(weakness)
      .map(([domain, pct]) => ({ domain, pct }))
      .sort((a, b) => b.pct - a.pct);
  });

  adminKeycloakId = '';

  private readonly platformId = inject(PLATFORM_ID);

  private readonly GRAFANA_SOLO = 'http://localhost:8500/d-solo/tfakkarni-monitoring/tfakkarni-e28094-microservices-monitoring';
  readonly grafanaDashboardUrl = 'http://localhost:8500/d/tfakkarni-monitoring/tfakkarni-e28094-microservices-monitoring?orgId=1';

  grafanaPanel(panelId: number): SafeResourceUrl {
    const url = `${this.GRAFANA_SOLO}?orgId=1&from=now-1h&to=now&refresh=30s&panelId=${panelId}&theme=dark`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
    private readonly analyticsService: AnalyticsService,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    // Skip API calls during SSR (no token available server-side)
    if (!isPlatformBrowser(this.platformId)) return;
    const kc = this.keycloakService.getKeycloakInstance();
    this.adminKeycloakId = kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';
    this.loadData();
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  setUserPage(p: number): void {
    if (p >= 1 && p <= this.totalUserPages()) this.userPage.set(p);
  }

  setGamePage(p: number): void {
    if (p >= 1 && p <= this.totalGamePages()) this.gamePage.set(p);
  }

  countByRole(role: string): number {
    return this.nonAdminUsers().filter(u => u.role === role).length;
  }

  // ─── Helpers ────────────────────────────────────────────
  getRoleBadgeType(role: string): 'default' | 'secondary' | 'outline' | 'destructive' {
    switch (role) {
      case 'admin': return 'default';
      case 'doctor': return 'secondary';
      default: return 'outline';
    }
  }

  getRoleLabel(role: string): string {
    switch (role) {
      case 'patient': return 'Patient';
      case 'doctor': return 'Médecin';
      case 'admin': return 'Admin';
      default: return role;
    }
  }

  getAvatarClass(role: string): string {
    switch (role) {
      case 'admin': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
      case 'doctor': return 'bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-400';
      default: return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
    }
  }

  // ─── Stage Helpers ───────────────────────────────────────
  getStageLabel(stage: string): string {
    const map: Record<string, string> = {
      LOW_RISK: 'Risque faible', EARLY: 'Stade précoce',
      MODERATE: 'Stade modéré', SEVERE: 'Stade sévère', UNKNOWN: 'Inconnu',
    };
    return map[stage] ?? stage;
  }
  getStageColor(stage: string): string {
    const map: Record<string, string> = {
      LOW_RISK: 'bg-emerald-500', EARLY: 'bg-amber-500',
      MODERATE: 'bg-orange-500', SEVERE: 'bg-red-500', UNKNOWN: 'bg-gray-400',
    };
    return map[stage] ?? 'bg-gray-400';
  }
  getStageBarColor(stage: string): string {
    return this.getStageColor(stage);
  }

  // ─── Batch Jobs ─────────────────────────────────────────
  runJob(type: string): void {
    this.jobRunning.set(type);
    const obs = type === 'all'
      ? this.analyticsService.runAllJobs()
      : type === 'patients'
        ? this.analyticsService.runPatientScores()
        : this.analyticsService.runDoctorEffectiveness();

    obs.pipe(finalize(() => this.jobRunning.set(null))).subscribe({
      next: result => {
        this.lastJobResult.set(result);
        this.loadAnalytics();
      },
      error: err => {
        console.error('Job failed', err);
        this.lastJobResult.set({
          jobName: type, status: 'FAILED', processedCount: 0, errorCount: 1,
          startedAt: new Date().toISOString(), completedAt: new Date().toISOString(),
          durationMs: 0, message: err?.error?.message || 'Job execution failed',
        });
      },
    });
  }

  // ─── Enable/Disable User ────────────────────────────────
  toggleUserEnabled(user: UserInfo): void {
    const newEnabled = !user.enabled;
    // Mise à jour optimiste immédiate dans le signal
    this.users.update(list =>
      list.map(u => u.keycloakId === user.keycloakId ? { ...u, enabled: newEnabled } : u)
    );
    this.userApiService.toggleEnabled(user.keycloakId, newEnabled).subscribe({
      error: (err) => {
        // Annuler la mise à jour optimiste en cas d'erreur
        this.users.update(list =>
          list.map(u => u.keycloakId === user.keycloakId ? { ...u, enabled: !newEnabled } : u)
        );
        console.error('Failed to toggle user', err);
      },
    });
  }

  // ─── Modal Management ───────────────────────────────────
  openEditModal(user: UserInfo): void {
    this.selectedUser.set(user);
    this.editForm = {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      newPassword: '',
    };
    this.modalMessage.set('');
    this.showEditModal.set(true);
  }

  openDeleteModal(user: UserInfo): void {
    this.selectedUser.set(user);
    this.modalMessage.set('');
    this.showDeleteModal.set(true);
  }

  closeModals(): void {
    this.showEditModal.set(false);
    this.showDeleteModal.set(false);
    this.selectedUser.set(null);
    this.modalMessage.set('');
  }

  // ─── CRUD Operations ───────────────────────────────────
  updateUser(): void {
    const user = this.selectedUser();
    if (!user) return;

    const f = this.editForm;
    if (!f.firstName.trim() || !f.lastName.trim() || !f.email.trim()) {
      this.modalMessage.set('Veuillez remplir tous les champs');
      this.modalSuccess.set(false);
      return;
    }

    if (f.newPassword && f.newPassword.length < 6) {
      this.modalMessage.set('Le mot de passe doit contenir au moins 6 caractères');
      this.modalSuccess.set(false);
      return;
    }

    this.isSubmitting.set(true);

    // Step 1: Update profile
    this.userApiService.updateProfile(user.keycloakId, {
      firstName: f.firstName.trim(),
      lastName: f.lastName.trim(),
      email: f.email.trim(),
    }).subscribe({
      next: () => {
        // Step 2: Update role if changed
        const roleChanged = f.role !== user.role;
        const passwordToReset = f.newPassword.trim();

        const afterRole = () => {
          // Step 3: Reset password if provided
          if (passwordToReset) {
            this.userApiService.adminResetPassword(user.keycloakId, passwordToReset).subscribe({
              next: () => this.finishUpdate(),
              error: (err) => {
                this.isSubmitting.set(false);
                this.modalMessage.set(err?.error?.error || 'Échec de la réinitialisation du mot de passe');
                this.modalSuccess.set(false);
              },
            });
          } else {
            this.finishUpdate();
          }
        };

        if (roleChanged) {
          this.userApiService.updateRole(user.keycloakId, f.role).subscribe({
            next: () => afterRole(),
            error: (err) => {
              this.isSubmitting.set(false);
              this.modalMessage.set(err?.error?.error || 'Échec de la mise à jour du rôle');
              this.modalSuccess.set(false);
            },
          });
        } else {
          afterRole();
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.modalMessage.set(err?.error?.error || 'Échec de la mise à jour');
        this.modalSuccess.set(false);
      },
    });
  }

  private finishUpdate(): void {
    this.isSubmitting.set(false);
    this.modalMessage.set('Utilisateur mis à jour avec succès');
    this.modalSuccess.set(true);
    this.loadUsers();
    setTimeout(() => this.closeModals(), 1200);
  }

  deleteUser(): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isSubmitting.set(true);
    this.userApiService.deleteUser(user.keycloakId)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.loadUsers();
          this.closeModals();
        },
        error: (err) => {
          this.modalMessage.set(err?.error?.error || 'Échec de la suppression');
          this.modalSuccess.set(false);
        },
      });
  }

  // ─── Data Loading ───────────────────────────────────────
  private loadData(): void {
    this.loadUsers();
    this.gameService.getAllGames().subscribe({
      next: games => this.games.set(games),
      error: err => console.error('Failed to load games', err),
    });
    this.gameService.getOverviewStats().subscribe({
      next: stats => this.stats.set(stats),
      error: err => console.error('Failed to load stats', err),
    });
    this.loadAnalytics();
    this.loadMatching();
  }

  loadMatching(): void {
    this.analyticsService.getRankedDoctors().subscribe({
      next: data => this.rankedDoctors.set(data),
      error: () => {},
    });
    this.analyticsService.getSeverePatients().subscribe({
      next: data => this.severePatients.set(data),
      error: () => {},
    });
  }

  private loadAnalytics(): void {
    this.analyticsService.getPlatformOverview().subscribe({
      next: data => this.platformOverview.set(data),
      error: () => {},
    });
    this.analyticsService.getDoctorRanking().subscribe({
      next: data => this.doctorRanking.set(data),
      error: () => {},
    });
  }

  private loadUsers(): void {
    this.userApiService.getAllUsers().subscribe({
      next: users => this.users.set(users),
      error: err => console.error('Failed to load users', err),
    });
  }
}
