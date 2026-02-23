import { Routes } from '@angular/router';
import { AuthGuard } from '@/core/auth';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'landing',
  },
  {
    path: 'landing',
    loadComponent: () =>
      import('@/pages/landing/landing.component').then(
        (m) => m.LandingComponent
      ),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('@/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'signup',
    loadComponent: () =>
      import('@/pages/signup/signup.component').then((m) => m.SignupComponent),
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('@/pages/admin-dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent
      ),
    canActivate: [AuthGuard],
    data: { roles: ['admin'] },
  },
  {
    path: 'doctor',
    loadComponent: () =>
      import('@/pages/doctor-dashboard/doctor-dashboard.component').then(
        (m) => m.DoctorDashboardComponent
      ),
    canActivate: [AuthGuard],
    data: { roles: ['doctor'] },
  },
  {
    path: 'patient',
    loadComponent: () =>
      import('@/pages/patient-dashboard/patient-dashboard.component').then(
        (m) => m.PatientDashboardComponent
      ),
    canActivate: [AuthGuard],
    data: { roles: ['patient'] },
  },
  {
    path: 'patient/play-memory/:gameId',
    loadComponent: () =>
      import(
        '@/pages/patient-dashboard/play-memory-game/play-memory-game.component'
      ).then((m) => m.PlayMemoryGameComponent),
    canActivate: [AuthGuard],
    data: { roles: ['patient'] },
  },
  {
    path: 'access-denied',
    loadComponent: () =>
      import('@/pages/access-denied/access-denied.component').then(
        (m) => m.AccessDeniedComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'landing',
  },
];
