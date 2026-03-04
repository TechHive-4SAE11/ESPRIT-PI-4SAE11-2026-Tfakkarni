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
    path: 'kyc-callback',
    loadComponent: () =>
      import('@/pages/kyc-callback/kyc-callback.component').then(
        (m) => m.KycCallbackComponent
      ),
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
    path: 'doctor/calendar-sync',
    loadComponent: () => import('@/pages/doctor/calendar-sync/calendar-sync.component')
      .then(m => m.CalendarSyncComponent),
    canActivate: [AuthGuard],
    data: { roles: ['doctor'] }
  },
  {
    path: 'doctor/calendar',
    loadComponent: () => import('@/pages/doctor/doctor-calendar/doctor-calendar.component')
      .then(m => m.DoctorCalendarComponent),
    canActivate: [AuthGuard],
    data: { roles: ['doctor'] }
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

  // ===== ROUTES POUR LES ANALYSES =====
  {
    path: 'analytics/absences',
    loadComponent: () => import('@/pages/analytics/analytics-dashboard.component')
      .then(m => m.AnalyticsDashboardComponent),
    canActivate: [AuthGuard]
  },

  // ===== ROUTES POUR LES RENDEZ-VOUS =====
  {
    path: 'appointments',
    loadComponent: () =>
      import('@/pages/appointments/appointments-list/appointments-list.component').then(
        (m) => m.AppointmentsListComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'appointments/new',
    loadComponent: () =>
      import('@/pages/appointments/appointment-add/appointment-add.component').then(
        (m) => m.AppointmentAddComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'appointments/edit/:id',
    loadComponent: () =>
      import('@/pages/appointments/appointment-edit/appointment-edit.component').then(
        (m) => m.AppointmentEditComponent
      ),
    canActivate: [AuthGuard],
  },

  // ===== ROUTES POUR LES RAPPELS =====
  {
    path: 'appointments/:appointmentId/reminders/new',
    loadComponent: () =>
      import('@/pages/appointments/create-reminder/create-reminder.component').then(
        (m) => m.CreateReminderComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'reminders/:reminderId/edit',
    loadComponent: () =>
      import('@/pages/appointments/edit-reminder/edit-reminder.component').then(
        (m) => m.EditReminderComponent
      ),
    canActivate: [AuthGuard],
  },

  // ===== ROUTE GÉNÉRIQUE (À METTRE APRÈS LES SPÉCIFIQUES) =====
  {
    path: 'appointments/:id',
    loadComponent: () =>
      import('@/pages/appointments/appointment-detail/appointment-detail.component').then(
        (m) => m.AppointmentDetailComponent
      ),
    canActivate: [AuthGuard],
  },

  {
    path: '**',
    redirectTo: 'landing',
  },
];