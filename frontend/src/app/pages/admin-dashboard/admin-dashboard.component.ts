import { Component, OnInit, signal, computed, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
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
import { ProfileComponent } from '@/pages/patient-dashboard/helper-view/profile/profile.component';
import { KeycloakService } from 'keycloak-angular';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardLayoutComponent,
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
          <div class="space-y-8">
            <div>
              <h2 class="text-2xl font-bold tracking-tight">Tableau de bord</h2>
              <p class="text-muted-foreground mt-1">Vue d'ensemble de la plateforme Tfakkarni</p>
            </div>

            <!-- Stats Cards -->
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              <z-card class="relative overflow-hidden">
                <div class="p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-muted-foreground">Utilisateurs</p>
                      <p class="text-3xl font-bold mt-1">{{ nonAdminUsers().length }}</p>
                      <p class="text-xs text-muted-foreground mt-1">
                        {{ countByRole('patient') }} patients · {{ countByRole('doctor') }} médecins
                      </p>
                    </div>
                    <div class="flex items-center justify-center h-12 w-12 rounded-xl bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400">
                      <z-icon zType="users" class="h-6 w-6" />
                    </div>
                  </div>
                </div>
              </z-card>

              <z-card class="relative overflow-hidden">
                <div class="p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-muted-foreground">Jeux créés</p>
                      <p class="text-3xl font-bold mt-1">{{ stats()?.totalGames ?? 0 }}</p>
                      <p class="text-xs text-muted-foreground mt-1">Jeux de mémoire actifs</p>
                    </div>
                    <div class="flex items-center justify-center h-12 w-12 rounded-xl bg-violet-100 text-violet-600 dark:bg-violet-900/30 dark:text-violet-400">
                      <z-icon zType="gamepad-2" class="h-6 w-6" />
                    </div>
                  </div>
                </div>
              </z-card>

              <z-card class="relative overflow-hidden">
                <div class="p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-muted-foreground">Tentatives</p>
                      <p class="text-3xl font-bold mt-1">{{ stats()?.totalAttempts ?? 0 }}</p>
                      <p class="text-xs text-muted-foreground mt-1">Sessions de jeu totales</p>
                    </div>
                    <div class="flex items-center justify-center h-12 w-12 rounded-xl bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400">
                      <z-icon zType="target" class="h-6 w-6" />
                    </div>
                  </div>
                </div>
              </z-card>

              <z-card class="relative overflow-hidden">
                <div class="p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-muted-foreground">Score moyen</p>
                      <p class="text-3xl font-bold mt-1">{{ (stats()?.averageScorePercentage ?? 0) | number:'1.0-0' }}%</p>
                      <p class="text-xs text-muted-foreground mt-1">Performance globale</p>
                    </div>
                    <div class="flex items-center justify-center h-12 w-12 rounded-xl bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400">
                      <z-icon zType="trending-up" class="h-6 w-6" />
                    </div>
                  </div>
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
                      @for (game of games(); track game.id) {
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
            <div>
              <h2 class="text-2xl font-bold tracking-tight">Statistiques de la plateforme</h2>
              <p class="text-muted-foreground mt-1">Analyse des performances et de l'activité</p>
            </div>

            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              <z-card class="p-6">
                <p class="text-sm font-medium text-muted-foreground">Total Jeux</p>
                <p class="text-3xl font-bold mt-1">{{ stats()?.totalGames ?? 0 }}</p>
              </z-card>
              <z-card class="p-6">
                <p class="text-sm font-medium text-muted-foreground">Total Tentatives</p>
                <p class="text-3xl font-bold mt-1">{{ stats()?.totalAttempts ?? 0 }}</p>
              </z-card>
              <z-card class="p-6">
                <p class="text-sm font-medium text-muted-foreground">Joueurs uniques</p>
                <p class="text-3xl font-bold mt-1">{{ stats()?.totalPlayers ?? 0 }}</p>
              </z-card>
              <z-card class="p-6">
                <p class="text-sm font-medium text-muted-foreground">Score moyen</p>
                <p class="text-3xl font-bold mt-1">{{ (stats()?.averageScorePercentage ?? 0) | number:'1.0-0' }}%</p>
              </z-card>
            </div>

            <div class="grid gap-6 md:grid-cols-2">
              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-4">Utilisateurs par rôle</h3>
                  <div class="space-y-4">
                    @for (entry of roleDistribution(); track entry.role) {
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <div class="h-3 w-3 rounded-full" [class]="entry.dotClass"></div>
                          <span>{{ entry.label }}</span>
                        </div>
                        <div class="flex items-center gap-2">
                          <span class="font-bold">{{ entry.count }}</span>
                          <span class="text-xs text-muted-foreground">
                            ({{ nonAdminUsers().length ? (entry.count / nonAdminUsers().length * 100 | number:'1.0-0') : 0 }}%)
                          </span>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              </z-card>

              <z-card>
                <div class="p-6">
                  <h3 class="text-lg font-semibold mb-4">Santé de la plateforme</h3>
                  <div class="space-y-4">
                    <div class="flex items-center justify-between">
                      <span class="text-muted-foreground">Jeux / Joueur</span>
                      <span class="font-bold">
                        {{ stats()?.totalPlayers ? ((stats()?.totalGames ?? 0) / (stats()?.totalPlayers ?? 1) | number:'1.0-1') : '0' }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-muted-foreground">Tentatives / Jeu</span>
                      <span class="font-bold">
                        {{ stats()?.totalGames ? ((stats()?.totalAttempts ?? 0) / (stats()?.totalGames ?? 1) | number:'1.0-1') : '0' }}
                      </span>
                    </div>
                    <div class="flex items-center justify-between">
                      <span class="text-muted-foreground">Comptes actifs</span>
                      <span class="font-bold">
                        {{ enabledCount() }} / {{ nonAdminUsers().length }}
                      </span>
                    </div>
                  </div>
                </div>
              </z-card>
            </div>
          </div>
        }
      }

      <!-- ══════════════════════════════════════════════════════════ -->
      <!-- PROFILE                                                     -->
      <!-- ══════════════════════════════════════════════════════════ -->
      @if (currentPage() === 'Mon Profil') {
        <app-profile [keycloakId]="adminKeycloakId" (goBack)="setPage('Home')" />
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

  // Search / Filter — both signals for reactivity
  searchQuery = signal('');
  roleFilter = signal<string>('all');

  // Pagination utilisateurs
  userPage    = signal(1);
  userPerPage = 8;

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
        { icon: 'bar-chart-3', label: 'Statistiques', action: () => this.setPage('Statistiques') },
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

  adminKeycloakId = '';

  private readonly platformId = inject(PLATFORM_ID);

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
    private readonly keycloakService: KeycloakService,
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
  }

  private loadUsers(): void {
    this.userApiService.getAllUsers().subscribe({
      next: users => this.users.set(users),
      error: err => console.error('Failed to load users', err),
    });
  }
}
