import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon/icon.component';

@Component({
  selector: 'app-kyc-callback',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="flex items-center justify-center min-h-screen bg-background">
      <z-card class="w-full max-w-md">
        <div class="p-8 text-center space-y-6">
          <div class="mx-auto w-16 h-16 rounded-full bg-green-100 flex items-center justify-center">
            <z-icon zType="check" class="w-8 h-8 text-green-600" />
          </div>
          <div class="space-y-2">
            <h2 class="text-2xl font-bold">Verification Submitted</h2>
            <p class="text-muted-foreground">
              Your identity verification has been submitted. You can now log in
              and your verification status will be checked automatically.
            </p>
          </div>
          <button z-button class="w-full" zSize="lg" (click)="goToLogin()">
            Go to Login
          </button>
        </div>
      </z-card>
    </div>
  `,
})
export class KycCallbackComponent implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    // This page serves as a landing after Didit KYC redirect
    if (!isPlatformBrowser(this.platformId)) return;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
