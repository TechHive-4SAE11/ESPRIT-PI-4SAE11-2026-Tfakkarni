import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '@/core/auth';
import { ZardButtonComponent } from '@/shared/components/button/button.component';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">

      <!-- Navbar -->
      <header class="border-b border-border/40 bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm sticky top-0 z-50">
        <div class="container mx-auto flex items-center justify-between px-6 py-4">
          <div class="flex items-center gap-2">
            <span class="text-2xl">🧠</span>
            <span class="text-xl font-bold text-primary">Tfakkarni</span>
          </div>
          <nav class="hidden md:flex items-center gap-6 text-sm text-muted-foreground">
            <a href="#quiz" class="hover:text-foreground transition-colors">Self-Test</a>
            <a href="#how-it-works" class="hover:text-foreground transition-colors">How It Works</a>
            <a href="#roles" class="hover:text-foreground transition-colors">Roles</a>
            <a href="#features" class="hover:text-foreground transition-colors">Features</a>
          </nav>
          <div class="flex items-center gap-3">
            <a routerLink="/login">
              <button z-button zType="outline">Sign In</button>
            </a>
            <a routerLink="/quiz">
              <button z-button>Take the Test</button>
            </a>
          </div>
        </div>
      </header>

      <main>

        <!-- ─── Hero Section ─── -->
        <section class="container mx-auto px-6 flex flex-col items-center text-center py-24 md:py-32">
          <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 text-primary text-sm font-medium mb-8">
            <span>🧠</span>
            <span>Alzheimer's Care & Monitoring Platform</span>
          </div>
          <h1 class="text-5xl md:text-6xl font-bold tracking-tight text-foreground mb-6 max-w-4xl">
            Supporting Alzheimer's Patients &
            <span class="text-primary"> Their Loved Ones</span>
          </h1>
          <p class="text-lg md:text-xl text-muted-foreground max-w-2xl mb-10">
            Tfakkarni accompanies Alzheimer's patients from the first doubts to daily care —
            with self-assessment, daily monitoring, memory exercises, and medical follow-up,
            all in one place.
          </p>
          <div class="flex flex-wrap items-center justify-center gap-4">
            <a routerLink="/quiz">
              <button z-button zSize="lg" class="text-lg px-8 py-6">
                Take the Alzheimer's Test
                <z-icon zType="arrow-right" class="ml-2" />
              </button>
            </a>
            <a routerLink="/login">
              <button z-button zType="outline" zSize="lg" class="text-lg px-8 py-6">
                Sign In
              </button>
            </a>
          </div>
        </section>

        <!-- ─── Quiz / Self-Test Section ─── -->
        <section id="quiz" class="bg-white dark:bg-gray-900 py-20">
          <div class="container mx-auto px-6 max-w-5xl">
            <div class="grid md:grid-cols-2 gap-12 items-center">
              <div>
                <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 text-sm font-medium mb-6">
                  <span>📋</span>
                  <span>Free Self-Assessment</span>
                </div>
                <h2 class="text-3xl md:text-4xl font-bold mb-5">
                  Do You Have Doubts About Alzheimer's?
                </h2>
                <p class="text-muted-foreground text-lg mb-6">
                  Answer our cognitive self-assessment quiz and receive an instant score.
                  If your result exceeds <strong>60%</strong>, our team will guide you through
                  creating your personal care account and connecting with a doctor.
                </p>
                <ul class="space-y-3 mb-8">
                  <li class="flex items-start gap-3">
                    <span class="mt-0.5 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                      <z-icon zType="check" class="h-3 w-3 text-green-600" />
                    </span>
                    <span class="text-muted-foreground">Quick, science-based questionnaire</span>
                  </li>
                  <li class="flex items-start gap-3">
                    <span class="mt-0.5 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                      <z-icon zType="check" class="h-3 w-3 text-green-600" />
                    </span>
                    <span class="text-muted-foreground">Immediate score with personalised guidance</span>
                  </li>
                  <li class="flex items-start gap-3">
                    <span class="mt-0.5 w-5 h-5 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center flex-shrink-0">
                      <z-icon zType="check" class="h-3 w-3 text-green-600" />
                    </span>
                    <span class="text-muted-foreground">Free, no account needed to get started</span>
                  </li>
                </ul>
                <a routerLink="/quiz">
                  <button z-button zSize="lg">
                    Start the Quiz
                    <z-icon zType="arrow-right" class="ml-2" />
                  </button>
                </a>
              </div>
              <!-- Score visual -->
              <div class="flex justify-center">
                <div class="relative w-64 h-64">
                  <div class="absolute inset-0 rounded-full bg-gradient-to-br from-amber-100 to-orange-200 dark:from-amber-900/30 dark:to-orange-800/30 flex flex-col items-center justify-center shadow-lg border-4 border-amber-200 dark:border-amber-700">
                    <span class="text-6xl font-extrabold text-amber-600 dark:text-amber-400">60%</span>
                    <span class="text-sm text-muted-foreground mt-2 font-medium">Recommended threshold</span>
                    <span class="text-xs text-muted-foreground">to open your care account</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ─── How It Works ─── -->
        <section id="how-it-works" class="container mx-auto px-6 py-20 max-w-5xl">
          <h2 class="text-3xl font-bold text-center mb-4">How It Works</h2>
          <p class="text-center text-muted-foreground mb-14 max-w-xl mx-auto">
            From the first assessment to daily care — a simple, structured journey for every user.
          </p>
          <div class="relative">
            <!-- Connecting line (desktop) -->
            <div class="hidden md:block absolute top-10 left-1/4 right-1/4 h-0.5 bg-primary/20 z-0"></div>
            <div class="grid md:grid-cols-4 gap-8 relative z-10">

              <div class="flex flex-col items-center text-center">
                <div class="w-20 h-20 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center mb-4 border-2 border-amber-300 dark:border-amber-700">
                  <z-icon zType="file" class="h-9 w-9 text-amber-600" />
                </div>
                <span class="text-xs font-bold text-amber-600 uppercase tracking-widest mb-1">Step 1</span>
                <h3 class="font-semibold text-lg mb-2">Take the Quiz</h3>
                <p class="text-sm text-muted-foreground">Answer the self-assessment questionnaire and get your Alzheimer's risk score instantly.</p>
              </div>

              <div class="flex flex-col items-center text-center">
                <div class="w-20 h-20 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center mb-4 border-2 border-blue-300 dark:border-blue-700">
                  <z-icon zType="plus-circle" class="h-9 w-9 text-blue-600" />
                </div>
                <span class="text-xs font-bold text-blue-600 uppercase tracking-widest mb-1">Step 2</span>
                <h3 class="font-semibold text-lg mb-2">Create an Account</h3>
                <p class="text-sm text-muted-foreground">Score above 60%? Register your care account. A doctor will create your full medical file.</p>
              </div>

              <div class="flex flex-col items-center text-center">
                <div class="w-20 h-20 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-4 border-2 border-green-300 dark:border-green-700">
                  <z-icon zType="activity" class="h-9 w-9 text-green-600" />
                </div>
                <span class="text-xs font-bold text-green-600 uppercase tracking-widest mb-1">Step 3</span>
                <h3 class="font-semibold text-lg mb-2">Daily Monitoring</h3>
                <p class="text-sm text-muted-foreground">Your helper logs meals, medications, and incidents every day using your account.</p>
              </div>

              <div class="flex flex-col items-center text-center">
                <div class="w-20 h-20 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center mb-4 border-2 border-purple-300 dark:border-purple-700">
                  <z-icon zType="gamepad-2" class="h-9 w-9 text-purple-600" />
                </div>
                <span class="text-xs font-bold text-purple-600 uppercase tracking-widest mb-1">Step 4</span>
                <h3 class="font-semibold text-lg mb-2">Memory Exercises</h3>
                <p class="text-sm text-muted-foreground">Play mini-games and cognitive exercises designed to stimulate and preserve memory.</p>
              </div>

            </div>
          </div>
        </section>

        <!-- ─── Roles Section ─── -->
        <section id="roles" class="bg-white dark:bg-gray-900 py-20">
          <div class="container mx-auto px-6 max-w-6xl">
            <h2 class="text-3xl font-bold text-center mb-4">Built for Everyone Involved</h2>
            <p class="text-center text-muted-foreground mb-14 max-w-xl mx-auto">
              Tfakkarni connects patients, their helpers, doctors, and administrators in a unified platform.
            </p>
            <div class="grid gap-8 md:grid-cols-2 lg:grid-cols-4">

              <!-- Patient -->
              <div class="rounded-xl p-7 shadow-sm border border-border/50 bg-gradient-to-b from-blue-50 to-white dark:from-blue-900/20 dark:to-gray-800 hover:shadow-md transition-shadow">
                <div class="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/40 flex items-center justify-center mb-5">
                  <z-icon zType="user" class="h-6 w-6 text-blue-600" />
                </div>
                <h3 class="text-lg font-bold mb-3 text-blue-700 dark:text-blue-400">Patient</h3>
                <ul class="space-y-2 text-sm text-muted-foreground">
                  <li class="flex items-start gap-2">
                    <span class="text-blue-400 mt-0.5">•</span>
                    Play memory mini-games and cognitive exercises
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-blue-400 mt-0.5">•</span>
                    View personal health progress
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-blue-400 mt-0.5">•</span>
                    Account shared with helper for daily tracking
                  </li>
                </ul>
              </div>

              <!-- Helper -->
              <div class="rounded-xl p-7 shadow-sm border border-border/50 bg-gradient-to-b from-orange-50 to-white dark:from-orange-900/20 dark:to-gray-800 hover:shadow-md transition-shadow">
                <div class="w-12 h-12 rounded-lg bg-orange-100 dark:bg-orange-900/40 flex items-center justify-center mb-5">
                  <z-icon zType="heart" class="h-6 w-6 text-orange-600" />
                </div>
                <h3 class="text-lg font-bold mb-3 text-orange-700 dark:text-orange-400">Helper</h3>
                <p class="text-xs text-muted-foreground mb-3 italic">
                  Family member or hospital caregiver — uses the patient's account
                </p>
                <ul class="space-y-2 text-sm text-muted-foreground">
                  <li class="flex items-start gap-2">
                    <span class="text-orange-400 mt-0.5">•</span>
                    Log daily nutrition (meals & hydration)
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-orange-400 mt-0.5">•</span>
                    Record medications taken
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-orange-400 mt-0.5">•</span>
                    Report falls and incidents
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-orange-400 mt-0.5">•</span>
                    Access advice on Alzheimer's care
                  </li>
                </ul>
              </div>

              <!-- Doctor -->
              <div class="rounded-xl p-7 shadow-sm border border-border/50 bg-gradient-to-b from-green-50 to-white dark:from-green-900/20 dark:to-gray-800 hover:shadow-md transition-shadow">
                <div class="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/40 flex items-center justify-center mb-5">
                  <z-icon zType="activity" class="h-6 w-6 text-green-600" />
                </div>
                <h3 class="text-lg font-bold mb-3 text-green-700 dark:text-green-400">Doctor</h3>
                <ul class="space-y-2 text-sm text-muted-foreground">
                  <li class="flex items-start gap-2">
                    <span class="text-green-400 mt-0.5">•</span>
                    Create and manage patient medical files
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-green-400 mt-0.5">•</span>
                    Monitor daily logs (nutrition, medication, incidents)
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-green-400 mt-0.5">•</span>
                    Track health score trends over time
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-green-400 mt-0.5">•</span>
                    Adjust care plans based on data
                  </li>
                </ul>
              </div>

              <!-- Admin -->
              <div class="rounded-xl p-7 shadow-sm border border-border/50 bg-gradient-to-b from-purple-50 to-white dark:from-purple-900/20 dark:to-gray-800 hover:shadow-md transition-shadow">
                <div class="w-12 h-12 rounded-lg bg-purple-100 dark:bg-purple-900/40 flex items-center justify-center mb-5">
                  <z-icon zType="shield" class="h-6 w-6 text-purple-600" />
                </div>
                <h3 class="text-lg font-bold mb-3 text-purple-700 dark:text-purple-400">Administrator</h3>
                <ul class="space-y-2 text-sm text-muted-foreground">
                  <li class="flex items-start gap-2">
                    <span class="text-purple-400 mt-0.5">•</span>
                    Create and manage patient accounts
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-purple-400 mt-0.5">•</span>
                    Assign doctors and helpers to patients
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-purple-400 mt-0.5">•</span>
                    Oversee the entire platform
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="text-purple-400 mt-0.5">•</span>
                    Access global analytics and reports
                  </li>
                </ul>
              </div>

            </div>

            <!-- Helper note -->
            <div class="mt-10 mx-auto max-w-2xl bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-xl p-5 flex items-start gap-4">
              <span class="text-2xl flex-shrink-0">💡</span>
              <p class="text-sm text-muted-foreground">
                <strong class="text-foreground">Why does the helper use the patient's account?</strong>
                Alzheimer's patients often cannot use digital tools independently.
                A trusted family member or hospital caregiver logs in on their behalf
                to carry out daily tracking — keeping all data under the patient's profile
                for the doctor to review.
              </p>
            </div>
          </div>
        </section>

        <!-- ─── Features Section ─── -->
        <section id="features" class="container mx-auto px-6 py-20 max-w-6xl">
          <h2 class="text-3xl font-bold text-center mb-4">Key Features</h2>
          <p class="text-center text-muted-foreground mb-14 max-w-xl mx-auto">
            Everything needed to support Alzheimer's patients and their caregivers, day by day.
          </p>
          <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center mb-4">
                <z-icon zType="file" class="h-5 w-5 text-amber-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Cognitive Self-Test</h3>
              <p class="text-sm text-muted-foreground">
                An accessible quiz available to anyone who suspects early-stage Alzheimer's.
                No account required — get your risk score in minutes.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center mb-4">
                <z-icon zType="pill" class="h-5 w-5 text-red-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Medication Tracking</h3>
              <p class="text-sm text-muted-foreground">
                Helpers record each medication taken daily. Doctors monitor compliance
                and adjust prescriptions accordingly.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-4">
                <z-icon zType="heart" class="h-5 w-5 text-green-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Nutrition Monitoring</h3>
              <p class="text-sm text-muted-foreground">
                Daily food and hydration logs help ensure patients maintain a healthy diet.
                Track meals, water intake, and nutritional balance.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center mb-4">
                <z-icon zType="alert-triangle" class="h-5 w-5 text-yellow-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Incident Reporting</h3>
              <p class="text-sm text-muted-foreground">
                Record falls, behavioural changes, or any health incidents as they happen.
                Doctors are kept informed in real time.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center mb-4">
                <z-icon zType="gamepad-2" class="h-5 w-5 text-purple-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Memory Mini-Games</h3>
              <p class="text-sm text-muted-foreground">
                Engaging cognitive exercises and games designed to stimulate memory,
                slow cognitive decline, and keep patients active.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center mb-4">
                <z-icon zType="file" class="h-5 w-5 text-blue-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Caregiver Advice</h3>
              <p class="text-sm text-muted-foreground">
                Helpers access a library of practical tips and expert advice on how
                to care for someone with Alzheimer's at home.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-teal-100 dark:bg-teal-900/30 flex items-center justify-center mb-4">
                <z-icon zType="file" class="h-5 w-5 text-teal-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Medical File Management</h3>
              <p class="text-sm text-muted-foreground">
                Doctors create and update comprehensive medical files (dossier médical)
                for each patient, including history, diagnoses, and care plans.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-indigo-100 dark:bg-indigo-900/30 flex items-center justify-center mb-4">
                <z-icon zType="bar-chart-3" class="h-5 w-5 text-indigo-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Health Score & Statistics</h3>
              <p class="text-sm text-muted-foreground">
                An overall health score is computed daily from all logged data.
                Charts and trends give doctors and helpers a clear picture of evolution.
              </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl p-7 border border-border/50 hover:shadow-md transition-shadow">
              <div class="w-11 h-11 rounded-lg bg-rose-100 dark:bg-rose-900/30 flex items-center justify-center mb-4">
                <z-icon zType="shield" class="h-5 w-5 text-rose-600" />
              </div>
              <h3 class="font-semibold text-base mb-2">Patient Management</h3>
              <p class="text-sm text-muted-foreground">
                Administrators create patient profiles and manage the whole care network —
                assigning doctors, helpers, and monitoring platform usage.
              </p>
            </div>

          </div>
        </section>

        <!-- ─── CTA Section ─── -->
        <section class="bg-primary py-20">
          <div class="container mx-auto px-6 text-center max-w-2xl">
            <h2 class="text-3xl font-bold text-primary-foreground mb-4">
              Start with a Free Self-Assessment
            </h2>
            <p class="text-primary-foreground/80 text-lg mb-8">
              If you or someone close to you shows signs of memory loss, take our quiz.
              It takes only a few minutes and could make all the difference.
            </p>
            <div class="flex flex-wrap items-center justify-center gap-4">
              <a routerLink="/quiz">
                <button z-button zType="outline" zSize="lg" class="bg-white text-primary hover:bg-white/90 text-lg px-8 py-6 border-white">
                  Take the Alzheimer's Quiz
                  <z-icon zType="arrow-right" class="ml-2" />
                </button>
              </a>
              <a routerLink="/login">
                <button z-button zSize="lg" class="bg-primary-foreground/10 text-primary-foreground border border-primary-foreground/30 hover:bg-primary-foreground/20 text-lg px-8 py-6">
                  Sign In
                </button>
              </a>
            </div>
          </div>
        </section>

      </main>

      <!-- Footer -->
      <footer class="border-t border-border/40 bg-white/50 dark:bg-gray-900/50">
        <div class="container mx-auto px-6 py-10">
          <div class="flex flex-col md:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-2">
              <span class="text-xl">🧠</span>
              <span class="font-bold text-primary">Tfakkarni</span>
            </div>
            <p class="text-sm text-muted-foreground text-center">
              Supporting Alzheimer's patients and their caregivers, every day.
            </p>
            <p class="text-sm text-muted-foreground">&copy; 2026 Tfakkarni. All rights reserved.</p>
          </div>
        </div>
      </footer>

    </div>
  `,
})
export class LandingComponent implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // If already logged in, redirect to role-based dashboard
    if (this.authService.isLoggedIn()) {
      this.authService.routeByRole();
    }
  }
}
