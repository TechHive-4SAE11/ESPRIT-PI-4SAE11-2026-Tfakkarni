import { Component, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '@/environments/environment';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardCardComponent } from '@/shared/components/card/card.component';
import { ZardInputDirective } from '@/shared/components/input/input.directive';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ZardButtonComponent,
    ZardCardComponent,
    ZardInputDirective,
  ],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  confirmPassword = '';
  selectedRole: 'doctor' | 'patient' = 'patient';
  selectedGender: 'male' | 'female' = 'male';
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  kycUrl = '';
  showKycPrompt = false;
  private readonly platformId = inject(PLATFORM_ID);

  private readonly API_URL = `${environment.apiBaseUrl}/api/users`;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly userApiService: UserApiService,
  ) { }

  async onSubmit(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Validation
    if (!this.firstName || !this.lastName || !this.email || !this.password) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    try {
      const registerUrl = `${this.API_URL}/register`;
      console.log('[SIGNUP] Sending registration request to backend:', registerUrl);

      const response = await firstValueFrom(this.http.post<any>(registerUrl, {
        firstName: this.firstName,
        lastName: this.lastName,
        email: this.email,
        password: this.password,
        role: this.selectedRole,
        gender: this.selectedRole === 'patient' ? this.selectedGender : undefined,
      }));

      console.log('[SIGNUP] Registration successful:', response);

      // If doctor, start KYC flow
      if (this.selectedRole === 'doctor') {
        this.successMessage = 'Account created! Starting identity verification...';
        try {
          // We need the keycloakId — fetch user by email
          const user = await firstValueFrom(
            this.http.get<any>(`${this.API_URL}/role/doctor`)
          );
          const doctorUser = (user as any[]).find((u: any) => u.email === this.email);

          if (doctorUser?.keycloakId) {
            const kycResult = await firstValueFrom(
              this.userApiService.startKyc(doctorUser.keycloakId)
            );
            if (kycResult.url) {
              this.kycUrl = kycResult.url;
              this.showKycPrompt = true;
              this.successMessage = 'Account created! Please complete identity verification to access your dashboard.';
              return; // Don't redirect — show KYC prompt
            }
          }
        } catch (kycError) {
          console.warn('[SIGNUP] KYC initiation failed, proceeding to login:', kycError);
        }
      }

      this.successMessage = 'Account created successfully! Redirecting to login...';

      setTimeout(() => {
        this.router.navigate(['/login']);
      }, 2000);

    } catch (error: any) {
      console.error('[SIGNUP] Error caught:', error);
      console.error('[SIGNUP] Error status:', error?.status);
      console.error('[SIGNUP] Error body:', error?.error);

      if (error.status === 409) {
        this.errorMessage = error.error?.error || 'User already exists with this username or email';
      } else if (error.status === 0) {
        this.errorMessage = 'Cannot connect to the server. Make sure the backend is running.';
      } else {
        this.errorMessage = error.error?.error || `Registration failed (status=${error?.status})`;
      }
    } finally {
      this.isLoading = false;
    }
  }

  openKycVerification(): void {
    if (this.kycUrl) {
      window.open(this.kycUrl, '_blank');
    }
  }

  skipKycAndLogin(): void {
    this.router.navigate(['/login']);
  }
}
