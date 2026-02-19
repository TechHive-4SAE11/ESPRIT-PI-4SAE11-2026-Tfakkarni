import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@/core/auth';
import { DashboardLayoutComponent, type SidebarMenuGroup } from '@/shared/components/dashboard-layout';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';
import { UserApiService, type UserInfo } from '@/core/services/user-api.service';
import { GameService, type GameResponse, type OverviewStatsResponse } from '@/core/services/game.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DashboardLayoutComponent,
    ZardCardComponent,
    ZardIconComponent,
    ZardBadgeComponent,
    ZardTableImports,
    ZardSkeletonComponent,
  ],
  template: `
    <app-dashboard-layout
      [menuGroups]="menuGroups"
      [pageTitle]="currentPage()"
      basePath="/admin"
    >
      @switch (currentPage()) {
        @case ('Home') {
          <h2 class="text-2xl font-bold mb-6">Admin Overview</h2>
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Total Users</p>
                  <p class="text-3xl font-bold">{{ users().length }}</p>
                </div>
                <z-icon zType="user" class="text-primary h-8 w-8" />
              </div>
            </z-card>
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Total Games</p>
                  <p class="text-3xl font-bold">{{ stats()?.totalGames ?? 0 }}</p>
                </div>
                <z-icon zType="gamepad-2" class="text-primary h-8 w-8" />
              </div>
            </z-card>
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Total Attempts</p>
                  <p class="text-3xl font-bold">{{ stats()?.totalAttempts ?? 0 }}</p>
                </div>
                <z-icon zType="target" class="text-primary h-8 w-8" />
              </div>
            </z-card>
            <z-card class="p-6">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Avg Score</p>
                  <p class="text-3xl font-bold">{{ (stats()?.averageScorePercentage ?? 0) | number:'1.0-0' }}%</p>
                </div>
                <z-icon zType="trending-up" class="text-primary h-8 w-8" />
              </div>
            </z-card>
          </div>

          <!-- Recent users -->
          <z-card>
            <div class="p-6">
              <h3 class="text-lg font-semibold mb-4">Recent Users</h3>
              @if (users().length > 0) {
                <table z-table>
                  <thead z-table-header>
                    <tr z-table-row>
                      <th z-table-head>Name</th>
                      <th z-table-head>Email</th>
                      <th z-table-head>Role</th>
                      <th z-table-head>Joined</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (user of users().slice(0, 5); track user.id) {
                      <tr z-table-row>
                        <td z-table-cell>{{ user.firstName }} {{ user.lastName }}</td>
                        <td z-table-cell>{{ user.email }}</td>
                        <td z-table-cell>
                          <z-badge
                            [zType]="user.role === 'admin' ? 'default' : user.role === 'doctor' ? 'secondary' : 'outline'">
                            {{ user.role }}
                          </z-badge>
                        </td>
                        <td z-table-cell class="text-muted-foreground">{{ user.createdAt | date:'mediumDate' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <p class="text-muted-foreground">No users found.</p>
              }
            </div>
          </z-card>
        }

        @case ('Users') {
          <h2 class="text-2xl font-bold mb-6">All Users</h2>
          <z-card>
            <div class="p-6">
              @if (users().length > 0) {
                <table z-table>
                  <thead z-table-header>
                    <tr z-table-row>
                      <th z-table-head>ID</th>
                      <th z-table-head>Name</th>
                      <th z-table-head>Email</th>
                      <th z-table-head>Role</th>
                      <th z-table-head>Keycloak ID</th>
                      <th z-table-head>Joined</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (user of users(); track user.id) {
                      <tr z-table-row>
                        <td z-table-cell>{{ user.id }}</td>
                        <td z-table-cell>{{ user.firstName }} {{ user.lastName }}</td>
                        <td z-table-cell>{{ user.email }}</td>
                        <td z-table-cell>
                          <z-badge
                            [zType]="user.role === 'admin' ? 'default' : user.role === 'doctor' ? 'secondary' : 'outline'">
                            {{ user.role }}
                          </z-badge>
                        </td>
                        <td z-table-cell class="text-xs text-muted-foreground font-mono">{{ user.keycloakId | slice:0:8 }}...</td>
                        <td z-table-cell class="text-muted-foreground">{{ user.createdAt | date:'mediumDate' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <z-skeleton class="h-48 w-full" />
              }
            </div>
          </z-card>
        }

        @case ('All Games') {
          <h2 class="text-2xl font-bold mb-6">All Games</h2>
          <z-card>
            <div class="p-6">
              @if (games().length > 0) {
                <table z-table>
                  <thead z-table-header>
                    <tr z-table-row>
                      <th z-table-head>ID</th>
                      <th z-table-head>Title</th>
                      <th z-table-head>Description</th>
                      <th z-table-head>Images</th>
                      <th z-table-head>Created</th>
                    </tr>
                  </thead>
                  <tbody z-table-body>
                    @for (game of games(); track game.id) {
                      <tr z-table-row>
                        <td z-table-cell>{{ game.id }}</td>
                        <td z-table-cell class="font-medium">{{ game.title }}</td>
                        <td z-table-cell class="text-muted-foreground">{{ game.description }}</td>
                        <td z-table-cell>
                          <z-badge zType="secondary">{{ game.imageCount }} images</z-badge>
                        </td>
                        <td z-table-cell class="text-muted-foreground">{{ game.createdAt | date:'mediumDate' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <p class="text-muted-foreground">No games created yet.</p>
              }
            </div>
          </z-card>
        }

        @case ('Analytics') {
          <h2 class="text-2xl font-bold mb-6">Platform Analytics</h2>
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Total Games</p>
              <p class="text-3xl font-bold">{{ stats()?.totalGames ?? 0 }}</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Total Attempts</p>
              <p class="text-3xl font-bold">{{ stats()?.totalAttempts ?? 0 }}</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Unique Players</p>
              <p class="text-3xl font-bold">{{ stats()?.totalPlayers ?? 0 }}</p>
            </z-card>
            <z-card class="p-6">
              <p class="text-sm text-muted-foreground">Avg Score %</p>
              <p class="text-3xl font-bold">{{ (stats()?.averageScorePercentage ?? 0) | number:'1.0-0' }}%</p>
            </z-card>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <z-card>
              <div class="p-6">
                <h3 class="text-lg font-semibold mb-2">Users by Role</h3>
                <div class="space-y-2 mt-4">
                  <div class="flex justify-between items-center">
                    <span class="text-muted-foreground">Patients</span>
                    <span class="font-semibold">{{ countByRole('patient') }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span class="text-muted-foreground">Doctors</span>
                    <span class="font-semibold">{{ countByRole('doctor') }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span class="text-muted-foreground">Admins</span>
                    <span class="font-semibold">{{ countByRole('admin') }}</span>
                  </div>
                </div>
              </div>
            </z-card>
            <z-card>
              <div class="p-6">
                <h3 class="text-lg font-semibold mb-2">Platform Health</h3>
                <div class="space-y-2 mt-4">
                  <div class="flex justify-between items-center">
                    <span class="text-muted-foreground">Games per Player</span>
                    <span class="font-semibold">
                      {{ stats()?.totalPlayers ? ((stats()?.totalGames ?? 0) / (stats()?.totalPlayers ?? 1) | number:'1.0-1') : '0' }}
                    </span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span class="text-muted-foreground">Attempts per Game</span>
                    <span class="font-semibold">
                      {{ stats()?.totalGames ? ((stats()?.totalAttempts ?? 0) / (stats()?.totalGames ?? 1) | number:'1.0-1') : '0' }}
                    </span>
                  </div>
                </div>
              </div>
            </z-card>
          </div>
        }
      }
    </app-dashboard-layout>
  `,
})
export class AdminDashboardComponent implements OnInit {
  currentPage = signal('Home');
  users = signal<UserInfo[]>([]);
  games = signal<GameResponse[]>([]);
  stats = signal<OverviewStatsResponse | null>(null);

  menuGroups: SidebarMenuGroup[] = [
    {
      label: 'Navigation',
      items: [
        { icon: 'house', label: 'Home', action: () => this.setPage('Home') },
        { icon: 'users', label: 'Users', action: () => this.setPage('Users') },
        { icon: 'gamepad-2', label: 'All Games', action: () => this.setPage('All Games') },
        { icon: 'bar-chart-3', label: 'Analytics', action: () => this.setPage('Analytics') },
      ],
    },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly gameService: GameService,
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  setPage(page: string): void {
    this.currentPage.set(page);
  }

  countByRole(role: string): number {
    return this.users().filter(u => u.role === role).length;
  }

  private loadData(): void {
    this.userApiService.getAllUsers().subscribe({
      next: users => this.users.set(users),
      error: err => console.error('Failed to load users', err),
    });
    this.gameService.getAllGames().subscribe({
      next: games => this.games.set(games),
      error: err => console.error('Failed to load games', err),
    });
    this.gameService.getOverviewStats().subscribe({
      next: stats => this.stats.set(stats),
      error: err => console.error('Failed to load stats', err),
    });
  }
}
