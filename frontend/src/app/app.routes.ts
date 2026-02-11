import { Routes } from '@angular/router';
import { AuthGuard } from '@/core/auth';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
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
    path: 'home',
    loadComponent: () =>
      import('@/pages/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'admin-dashboard',
    loadComponent: () =>
      import('@/pages/admin-dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent
      ),
    canActivate: [AuthGuard],
    data: { roles: ['admin'] },
  },
  {
    path: 'doctor-dashboard',
    loadComponent: () =>
      import('@/pages/doctor-dashboard/doctor-dashboard.component').then(
        (m) => m.DoctorDashboardComponent
      ),
    canActivate: [AuthGuard],
    data: { roles: ['doctor'] },
  },
  {
    path: 'patient-dashboard',
    loadComponent: () =>
      import('@/pages/patient-dashboard/patient-dashboard.component').then(
        (m) => m.PatientDashboardComponent
      ),
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
    redirectTo: 'home',
  },
];
