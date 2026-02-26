import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  signal,
  inject,
  DestroyRef,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardInputDirective } from '@/shared/components/input';
import { ZardDividerComponent } from '@/shared/components/divider';
import { UserApiService, UserInfo } from '@/core/services/user-api.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardInputDirective,
    ZardDividerComponent,
  ],
  template: `
    <div class="flex items-center gap-2 mb-6">
      <button z-button zType="ghost" zSize="sm" (click)="goBack.emit()">
        <z-icon zType="arrow-left" class="mr-1" />
        Retour
      </button>
      <h2 class="text-2xl font-bold">Mon Profil</h2>
    </div>

    @if (isLoading()) {
      <div class="flex items-center justify-center py-20">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    } @else {
      <div class="grid gap-6 max-w-2xl">

        <!-- ═══════ Profile Info Section ═══════ -->
        <z-card class="p-6">
          <div class="flex items-center gap-3 mb-6">
            <div class="flex items-center justify-center h-12 w-12 rounded-full bg-primary/10 text-primary">
              <z-icon zType="user" class="h-6 w-6" />
            </div>
            <div>
              <h3 class="text-lg font-semibold">Informations personnelles</h3>
              <p class="text-sm text-muted-foreground">Modifiez vos informations de profil</p>
            </div>
          </div>

          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Prénom</label>
              <input z-input
                type="text"
                class="w-full"
                placeholder="Votre prénom"
                [(ngModel)]="firstName"
              />
            </div>
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Nom</label>
              <input z-input
                type="text"
                class="w-full"
                placeholder="Votre nom"
                [(ngModel)]="lastName"
              />
            </div>
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Email</label>
              <input z-input
                type="email"
                class="w-full"
                placeholder="votre&#64;email.com"
                [(ngModel)]="email"
              />
            </div>
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Rôle</label>
              <input z-input
                type="text"
                class="w-full bg-muted/50 cursor-not-allowed"
                [ngModel]="role"
                disabled
              />
            </div>
          </div>

          @if (profileMessage()) {
            <div class="mt-4 text-sm px-3 py-2 rounded-md"
                 [class]="profileSuccess() ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400' : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'">
              {{ profileMessage() }}
            </div>
          }

          <div class="mt-6 flex justify-end">
            <button z-button
              [disabled]="isSavingProfile()"
              (click)="saveProfile()">
              @if (isSavingProfile()) {
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              }
              <z-icon zType="check" class="mr-2 h-4 w-4" />
              Enregistrer
            </button>
          </div>
        </z-card>

        <!-- ═══════ Password Section ═══════ -->
        <z-card class="p-6">
          <div class="flex items-center gap-3 mb-6">
            <div class="flex items-center justify-center h-12 w-12 rounded-full bg-orange-100 text-orange-600 dark:bg-orange-900/20 dark:text-orange-400">
              <z-icon zType="lock" class="h-6 w-6" />
            </div>
            <div>
              <h3 class="text-lg font-semibold">Changer le mot de passe</h3>
              <p class="text-sm text-muted-foreground">Sécurisez votre compte avec un nouveau mot de passe</p>
            </div>
          </div>

          <div class="space-y-4">
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Nouveau mot de passe</label>
              <input z-input
                type="password"
                class="w-full"
                placeholder="Minimum 6 caractères"
                [(ngModel)]="newPassword"
              />
            </div>
            <div>
              <label class="text-sm font-medium text-foreground mb-1.5 block">Confirmer le nouveau mot de passe</label>
              <input z-input
                type="password"
                class="w-full"
                placeholder="Retapez le nouveau mot de passe"
                [(ngModel)]="confirmPassword"
              />
            </div>
          </div>

          @if (passwordMessage()) {
            <div class="mt-4 text-sm px-3 py-2 rounded-md"
                 [class]="passwordSuccess() ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400' : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'">
              {{ passwordMessage() }}
            </div>
          }

          <div class="mt-6 flex justify-end">
            <button z-button
              [disabled]="isChangingPassword()"
              (click)="changePassword()">
              @if (isChangingPassword()) {
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              }
              <z-icon zType="lock" class="mr-2 h-4 w-4" />
              Changer le mot de passe
            </button>
          </div>
        </z-card>

      </div>
    }
  `,
})
export class ProfileComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';
  @Output() goBack = new EventEmitter<void>();

  // Profile form fields
  firstName = '';
  lastName = '';
  email = '';
  role = '';

  // Password form fields
  newPassword = '';
  confirmPassword = '';

  // State signals
  isLoading = signal(true);
  isSavingProfile = signal(false);
  isChangingPassword = signal(false);
  profileMessage = signal('');
  profileSuccess = signal(false);
  passwordMessage = signal('');
  passwordSuccess = signal(false);

  ngOnInit(): void {
    this.loadUserProfile();
  }

  private loadUserProfile(): void {
    if (!this.keycloakId) {
      this.isLoading.set(false);
      return;
    }

    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (user: UserInfo) => {
          this.firstName = user.firstName;
          this.lastName = user.lastName;
          this.email = user.email;
          this.role = user.role;
        },
        error: (err) => {
          console.error('[Profile] Failed to load user', err);
          this.profileMessage.set('Impossible de charger le profil');
          this.profileSuccess.set(false);
        },
      });
  }

  saveProfile(): void {
    this.profileMessage.set('');

    if (!this.firstName.trim() || !this.lastName.trim() || !this.email.trim()) {
      this.profileMessage.set('Veuillez remplir tous les champs');
      this.profileSuccess.set(false);
      return;
    }

    this.isSavingProfile.set(true);

    this.userApiService.updateProfile(this.keycloakId, {
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      email: this.email.trim(),
    })
      .pipe(
        finalize(() => this.isSavingProfile.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updatedUser) => {
          this.firstName = updatedUser.firstName;
          this.lastName = updatedUser.lastName;
          this.email = updatedUser.email;
          this.profileMessage.set('Profil mis à jour avec succès');
          this.profileSuccess.set(true);
        },
        error: (err) => {
          const msg = err?.error?.error || 'Échec de la mise à jour du profil';
          this.profileMessage.set(msg);
          this.profileSuccess.set(false);
        },
      });
  }

  changePassword(): void {
    this.passwordMessage.set('');

    if (!this.newPassword || !this.confirmPassword) {
      this.passwordMessage.set('Veuillez remplir tous les champs');
      this.passwordSuccess.set(false);
      return;
    }

    if (this.newPassword.length < 6) {
      this.passwordMessage.set('Le mot de passe doit contenir au moins 6 caractères');
      this.passwordSuccess.set(false);
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordMessage.set('Les mots de passe ne correspondent pas');
      this.passwordSuccess.set(false);
      return;
    }

    this.isChangingPassword.set(true);

    this.userApiService.adminResetPassword(this.keycloakId, this.newPassword)
      .pipe(
        finalize(() => this.isChangingPassword.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.passwordMessage.set('Mot de passe modifié avec succès');
          this.passwordSuccess.set(true);
          this.newPassword = '';
          this.confirmPassword = '';
        },
        error: (err) => {
          const msg = err?.error?.error || 'Échec du changement de mot de passe';
          this.passwordMessage.set(msg);
          this.passwordSuccess.set(false);
        },
      });
  }
}
