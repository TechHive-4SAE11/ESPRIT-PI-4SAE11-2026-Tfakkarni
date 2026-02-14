import { Component, signal, Input, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { ZardAvatarComponent } from '@/shared/components/avatar';
import { ZardBreadcrumbImports } from '@/shared/components/breadcrumb/breadcrumb.imports';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDividerComponent } from '@/shared/components/divider';
import { ZardIconComponent, type ZardIcon } from '@/shared/components/icon';
import { LayoutImports } from '@/shared/components/layout/layout.imports';
import { ZardMenuImports } from '@/shared/components/menu/menu.imports';
import { ZardTooltipImports } from '@/shared/components/tooltip';
import { AuthService } from '@/core/auth';

export interface SidebarMenuItem {
  icon: ZardIcon;
  label: string;
  route?: string;
  action?: () => void;
}

export interface SidebarMenuGroup {
  label: string;
  items: SidebarMenuItem[];
}

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [
    LayoutImports,
    ZardButtonComponent,
    ZardBreadcrumbImports,
    ZardMenuImports,
    ZardTooltipImports,
    ZardDividerComponent,
    ZardAvatarComponent,
    ZardIconComponent,
    RouterLink,
    RouterLinkActive,
  ],
  template: `
    <z-layout class="min-h-screen">
      <z-sidebar
        [zWidth]="250"
        [zCollapsible]="true"
        [zCollapsed]="sidebarCollapsed()"
        [zCollapsedWidth]="70"
        (zCollapsedChange)="onCollapsedChange($event)"
        class="p-0! border-r border-border"
      >
        <nav [class]="'flex h-full flex-col overflow-hidden ' + (sidebarCollapsed() ? 'gap-1 p-1 pt-4' : 'gap-4 p-4')">
          <!-- Logo section -->
          <div [class]="'flex items-center ' + (sidebarCollapsed() ? 'justify-center mb-2' : 'gap-2 mb-2')">
            @if (!sidebarCollapsed()) {
              <span class="text-xl font-bold text-primary">🧠 Tfakkarni</span>
              <span class="text-xs text-muted-foreground bg-muted px-1.5 py-0.5 rounded">beta</span>
            } @else {
              <span class="text-xl">🧠</span>
            }
          </div>

          <z-divider zSpacing="sm" />

          <!-- Menu groups -->
          @for (group of menuGroups; track group.label) {
            <z-sidebar-group>
              @if (!sidebarCollapsed()) {
                <z-sidebar-group-label>{{ group.label }}</z-sidebar-group-label>
              }
              @for (item of group.items; track item.label) {
                @if (item.route) {
                  <a
                    z-button
                    zType="ghost"
                    [routerLink]="item.route"
                    routerLinkActive="bg-accent text-accent-foreground"
                    [routerLinkActiveOptions]="{ exact: true }"
                    [class]="'w-full no-underline ' + (sidebarCollapsed() ? 'justify-center' : 'justify-start')"
                    [zTooltip]="sidebarCollapsed() ? item.label : ''"
                    zPosition="right"
                  >
                    <z-icon [zType]="item.icon" [class]="sidebarCollapsed() ? '' : 'mr-2'" />
                    @if (!sidebarCollapsed()) {
                      <span>{{ item.label }}</span>
                    }
                  </a>
                } @else {
                  <button
                    type="button"
                    z-button
                    zType="ghost"
                    [class]="sidebarCollapsed() ? 'justify-center' : 'justify-start'"
                    [zTooltip]="sidebarCollapsed() ? item.label : ''"
                    zPosition="right"
                    (click)="item.action && item.action()"
                  >
                    <z-icon [zType]="item.icon" [class]="sidebarCollapsed() ? '' : 'mr-2'" />
                    @if (!sidebarCollapsed()) {
                      <span>{{ item.label }}</span>
                    }
                  </button>
                }
              }
            </z-sidebar-group>
          }

          <!-- User section at bottom -->
          <div class="mt-auto">
            <z-divider zSpacing="sm" />
            <div
              z-menu
              [zMenuTriggerFor]="userMenu"
              zPlacement="rightBottom"
              [class]="
                'hover:bg-accent flex cursor-pointer items-center justify-center gap-2 rounded-md ' +
                (sidebarCollapsed() ? 'm-2 p-0' : 'p-2')
              "
            >
              <z-avatar zSrc="" [zAlt]="username" />

              @if (!sidebarCollapsed()) {
                <div class="flex-1 min-w-0">
                  <span class="font-medium text-sm truncate block">{{ username }}</span>
                  <div class="text-xs text-muted-foreground truncate">{{ roleBadge }}</div>
                </div>

                <z-icon zType="chevrons-up-down" class="ml-auto flex-shrink-0" />
              }
            </div>

            <ng-template #userMenu>
              <div z-menu-content class="w-48">
                <button type="button" z-menu-item>
                  <z-icon zType="user" class="mr-2" />
                  Profile
                </button>
                <button type="button" z-menu-item>
                  <z-icon zType="settings" class="mr-2" />
                  Settings
                </button>
                <z-divider zSpacing="sm" />
                <button type="button" z-menu-item (click)="logout()">
                  <z-icon zType="log-out" class="mr-2" />
                  Logout
                </button>
              </div>
            </ng-template>
          </div>
        </nav>
      </z-sidebar>

      <z-content class="min-h-screen bg-background">
        <div class="flex items-center mb-4">
          <button type="button" z-button zType="ghost" zSize="sm" class="-ml-2" (click)="toggleSidebar()">
            <z-icon zType="panel-left" />
          </button>

          <z-divider zOrientation="vertical" class="ml-2 h-4" />

          <z-breadcrumb zWrap="wrap" zAlign="start">
            <z-breadcrumb-item (click)="navigateToBase()" class="cursor-pointer">Home</z-breadcrumb-item>
            @if (pageTitle) {
              <z-breadcrumb-item>
                <span aria-current="page">{{ pageTitle }}</span>
              </z-breadcrumb-item>
            }
          </z-breadcrumb>
        </div>

        <ng-content />
      </z-content>
    </z-layout>
  `,
})
export class DashboardLayoutComponent implements OnInit {
  @Input() menuGroups: SidebarMenuGroup[] = [];
  @Input() pageTitle = '';
  @Input() basePath = '/';

  readonly sidebarCollapsed = signal(false);
  username = '';
  roleBadge = '';

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.authService.getUsername().then(name => {
      this.username = name;
    }).catch(() => {
      this.username = 'User';
    });
    const role = this.authService.getPrimaryRole();
    this.roleBadge = role ? role.charAt(0).toUpperCase() + role.slice(1) : '';
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(collapsed => !collapsed);
  }

  onCollapsedChange(collapsed: boolean) {
    this.sidebarCollapsed.set(collapsed);
  }

  navigateToBase() {
    this.router.navigate([this.basePath]);
  }

  logout() {
    this.authService.logout();
  }
}
