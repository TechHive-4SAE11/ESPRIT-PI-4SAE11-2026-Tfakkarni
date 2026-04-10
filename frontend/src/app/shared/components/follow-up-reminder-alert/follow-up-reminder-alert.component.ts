import {
  Component,
  Input,
  OnInit,
  signal,
  inject,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import {
  FollowUpReminderService,
  type FollowUpReminder,
} from '@/core/services/follow-up-reminder.service';

/**
 * Alert banner displayed inside the helper dashboard when a patient's
 * daily follow-up has not been completed.
 *
 * Usage:
 *   <app-follow-up-reminder-alert [keycloakId]="patientKeycloakId" />
 */
@Component({
  selector: 'app-follow-up-reminder-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    @for (reminder of reminders(); track reminder.id) {
      <div class="mb-3 rounded-xl border-2 border-amber-300 dark:border-amber-600
                  bg-amber-50 dark:bg-amber-900/20 p-4 shadow-md">
        <div class="flex items-start gap-3">

          <!-- Icon -->
          <div class="shrink-0 mt-0.5">
            <div class="w-10 h-10 rounded-full bg-amber-100 dark:bg-amber-900/40
                        flex items-center justify-center text-xl">
              ⚠️
            </div>
          </div>

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2 mb-1">
              <h4 class="font-bold text-amber-800 dark:text-amber-300 text-sm">
                Suivi quotidien incomplet
              </h4>
              <span class="text-xs text-amber-600 dark:text-amber-400 shrink-0">
                {{ formatDate(reminder.reminderDate) }}
              </span>
            </div>

            <p class="text-sm text-amber-700 dark:text-amber-300/80 mb-2">
              {{ reminder.message }}
            </p>

            <!-- Missing category badges -->
            <div class="flex flex-wrap gap-1.5 mb-2">
              @for (cat of getMissingCategories(reminder); track cat) {
                <span
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                  [ngClass]="{
                    'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-300': cat === 'NUTRITION',
                    'bg-blue-100   dark:bg-blue-900/30   text-blue-700   dark:text-blue-300':   cat === 'MEDICATION',
                    'bg-green-100  dark:bg-green-900/30  text-green-700  dark:text-green-300':  cat === 'ACTIVITY'
                  }">
                  {{ getCategoryIcon(cat) }} {{ getCategoryLabel(cat) }}
                </span>
              }
            </div>

            <!-- Dismiss button -->
            <button
              (click)="dismiss(reminder)"
              class="text-xs px-3 py-1.5 rounded-lg
                     bg-amber-200 dark:bg-amber-800
                     text-amber-800 dark:text-amber-200
                     hover:bg-amber-300 dark:hover:bg-amber-700
                     transition-colors font-medium">
              ✓ Compris
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class FollowUpReminderAlertComponent implements OnInit {
  @Input() keycloakId = '';

  reminders = signal<FollowUpReminder[]>([]);

  private readonly service   = inject(FollowUpReminderService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    if (!this.keycloakId) return;
    this.service
      .getUnreadReminders(this.keycloakId)
      .pipe(
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(list => this.reminders.set(list));
  }

  dismiss(reminder: FollowUpReminder): void {
    this.service
      .markAsRead(reminder.id)
      .pipe(
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() =>
        this.reminders.update(list => list.filter(r => r.id !== reminder.id)),
      );
  }

  getMissingCategories(reminder: FollowUpReminder): string[] {
    return reminder.missingCategories
      ? reminder.missingCategories.split(',').map(s => s.trim()).filter(Boolean)
      : [];
  }

  getCategoryIcon(cat: string): string {
    return { NUTRITION: '🍽️', MEDICATION: '💊', ACTIVITY: '🏃' }[cat] ?? '•';
  }

  getCategoryLabel(cat: string): string {
    return { NUTRITION: 'Alimentation', MEDICATION: 'Médicaments', ACTIVITY: 'Activités' }[cat] ?? cat;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
  }
}
