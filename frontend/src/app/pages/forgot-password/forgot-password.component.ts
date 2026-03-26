import {
  Component, inject, signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { UserApiService } from '@/core/services/user-api.service';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';

type Step = 'email' | 'otp' | 'done';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink, ZardButtonComponent, ZardCardComponent, ZardInputDirective],
  template: `
<div class="auth-shell p-4">
  <div class="auth-blob auth-blob-left"></div>
  <div class="auth-blob auth-blob-right"></div>

  <div class="w-full max-w-md auth-panel">
    <!-- Brand -->
    <div class="text-center mb-6">
      <div class="inline-flex items-center gap-3 brand-chip">
        <span class="brand-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 4a6.5 6.5 0 0 0-6.5 6.5v3.7a3.8 3.8 0 0 0 3.8 3.8h.2v2h5v-2h.2a3.8 3.8 0 0 0 3.8-3.8v-3.7A6.5 6.5 0 0 0 12 4Z" />
            <path d="M9.7 10.2 12 12l2.3-1.8" />
          </svg>
        </span>
        <h1 class="text-3xl font-bold tracking-tight">tfakkarni</h1>
      </div>
      <p class="text-sm text-slate-600 mt-3">Réinitialisation de mot de passe</p>
    </div>

    <!-- ══ STEP 1 : Email ══ -->
    @if (step() === 'email') {
      <z-card class="auth-card" zTitle="Mot de passe oublié"
              zDescription="Entrez votre email pour recevoir un code de vérification à 6 chiffres">
        <div class="space-y-5">
          <div class="space-y-2">
            <label class="text-sm font-medium">Adresse email</label>
            <input z-input type="email" [(ngModel)]="email"
              placeholder="votre&#64;email.com" class="w-full auth-input" />
          </div>

          @if (errorMessage()) {
            <div class="text-sm text-red-700 bg-red-50 border border-red-200 p-3 rounded-lg">
              {{ errorMessage() }}
            </div>
          }
          @if (successMessage()) {
            <div class="text-sm text-green-700 bg-green-50 border border-green-200 p-3 rounded-lg flex items-center gap-2">
              <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
              </svg>
              {{ successMessage() }}
            </div>
          }

          <button z-button class="w-full auth-submit" zSize="lg"
            [disabled]="isLoading()" (click)="sendOtp()">
            @if (isLoading()) { Envoi en cours... } @else { Envoyer le code }
          </button>

          <div class="text-center text-sm">
            <a routerLink="/login" class="font-semibold text-cyan-700 hover:text-cyan-800">
              ← Retour à la connexion
            </a>
          </div>
        </div>
      </z-card>
    }

    <!-- ══ STEP 2 : OTP + nouveau mot de passe ══ -->
    @if (step() === 'otp') {
      <z-card class="auth-card" zTitle="Vérification du code"
              zDescription="Consultez votre boîte mail et entrez le code à 6 chiffres">

        <!-- Email target -->
        <div class="flex items-center gap-2 text-sm text-slate-600 bg-slate-50 rounded-lg px-3 py-2 mb-4 border border-slate-200">
          <svg class="h-4 w-4 shrink-0 text-violet-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
          </svg>
          Code envoyé à <strong class="text-slate-900">{{ email }}</strong>
        </div>

        <div class="space-y-5">
          <!-- OTP -->
          <div class="space-y-2">
            <label class="text-sm font-medium">Code de vérification</label>
            <input z-input type="text" inputmode="numeric" maxlength="6"
              [(ngModel)]="otpCode"
              placeholder="● ● ● ● ● ●"
              class="w-full auth-input text-center text-3xl tracking-[0.6em] font-bold py-4" />
          </div>

          <!-- New password -->
          <div class="space-y-2">
            <label class="text-sm font-medium">Nouveau mot de passe</label>
            <input z-input type="password" [(ngModel)]="newPassword"
              placeholder="Minimum 6 caractères" class="w-full auth-input" />
          </div>

          <!-- Confirm password -->
          <div class="space-y-2">
            <label class="text-sm font-medium">Confirmer le mot de passe</label>
            <input z-input type="password" [(ngModel)]="confirmPassword"
              placeholder="Retapez le nouveau mot de passe" class="w-full auth-input" />
          </div>

          @if (errorMessage()) {
            <div class="text-sm text-red-700 bg-red-50 border border-red-200 p-3 rounded-lg">
              {{ errorMessage() }}
            </div>
          }

          <button z-button class="w-full auth-submit" zSize="lg"
            [disabled]="isLoading()" (click)="verifyAndReset()">
            @if (isLoading()) { Vérification... } @else { Réinitialiser le mot de passe }
          </button>

          <div class="text-center text-sm flex items-center justify-center gap-3">
            <button class="text-cyan-700 hover:text-cyan-800 font-medium"
              [disabled]="isLoading()" (click)="goBackToEmail()">
              Renvoyer le code
            </button>
            <span class="text-slate-300">|</span>
            <a routerLink="/login" class="font-semibold text-cyan-700 hover:text-cyan-800">
              Retour à la connexion
            </a>
          </div>
        </div>
      </z-card>
    }

    <!-- ══ STEP 3 : Succès ══ -->
    @if (step() === 'done') {
      <z-card class="auth-card">
        <div class="text-center space-y-5 py-4">
          <div class="flex items-center justify-center h-16 w-16 mx-auto rounded-full bg-green-100">
            <svg class="h-8 w-8 text-green-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
            </svg>
          </div>
          <div>
            <h3 class="text-xl font-bold text-slate-900">Mot de passe réinitialisé !</h3>
            <p class="text-sm text-slate-600 mt-2">
              Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.
            </p>
          </div>
          <button z-button class="w-full" zSize="lg" (click)="goToLogin()">
            Se connecter
          </button>
        </div>
      </z-card>
    }

    <div class="mt-6 text-center text-xs text-slate-500">
      <p>&copy; 2026 tfakkarni. All rights reserved.</p>
    </div>
  </div>
</div>
  `,
  styleUrls: ['../login/login.component.css'],
})
export class ForgotPasswordComponent {
  readonly router      = inject(Router);
  private readonly userApi = inject(UserApiService);

  step            = signal<Step>('email');
  email           = '';
  otpCode         = '';
  newPassword     = '';
  confirmPassword = '';
  isLoading       = signal(false);
  errorMessage    = signal('');
  successMessage  = signal('');

  sendOtp(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    if (!this.email || !this.email.includes('@')) {
      this.errorMessage.set('Veuillez entrer une adresse email valide');
      return;
    }
    this.isLoading.set(true);
    this.userApi.forgotPassword(this.email.trim())
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('Code envoyé ! Vérifiez votre boîte mail.');
          setTimeout(() => { this.step.set('otp'); this.successMessage.set(''); }, 1500);
        },
        error: (err) => {
          this.errorMessage.set(err?.error?.error || 'Erreur lors de l\'envoi. Réessayez.');
        },
      });
  }

  goBackToEmail(): void {
    this.otpCode = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.errorMessage.set('');
    this.step.set('email');
  }

  verifyAndReset(): void {
    this.errorMessage.set('');
    if (!this.otpCode || this.otpCode.replace(/\s/g, '').length !== 6) {
      this.errorMessage.set('Le code doit contenir exactement 6 chiffres');
      return;
    }
    if (!this.newPassword || this.newPassword.length < 6) {
      this.errorMessage.set('Le mot de passe doit contenir au moins 6 caractères');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas');
      return;
    }
    this.isLoading.set(true);
    this.userApi.verifyAndResetPassword(this.email.trim(), this.otpCode.trim(), this.newPassword)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: () => this.step.set('done'),
        error: (err) => {
          this.errorMessage.set(err?.error?.error || 'Code incorrect ou expiré. Réessayez.');
        },
      });
  }

  goToLogin(): void { this.router.navigate(['/login']); }
}
