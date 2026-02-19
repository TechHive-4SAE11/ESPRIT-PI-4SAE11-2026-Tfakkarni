import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { EquipmentService } from '@/core/services/equipment.service';
import { EquipmentDTO, EquipmentLoanDTO, EquipmentStatus, LoanStatus } from '@/core/models/equipment.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-patient-equipment',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
        🏥 Equipment
      </h1>
      <p class="text-lg text-slate-500 dark:text-slate-400 mb-6">View available medical equipment</p>

      @if (isLoading()) {
      <div class="text-center py-16">
        <p class="text-5xl mb-4 animate-pulse">⏳</p>
        <p class="text-slate-500 dark:text-slate-400 text-lg">Loading equipment...</p>
      </div>
      } @else {
      <!-- My Loans -->
      @if (myLoans().length > 0) {
      <div class="space-y-4 mb-8">
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white">My Borrowed Equipment</h2>
        @for (loan of myLoans(); track loan.id) {
        <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 p-6 shadow-lg">
          <div class="flex items-start justify-between mb-4">
            <div class="flex-1">
              <h3 class="text-xl font-bold text-slate-800 dark:text-white mb-2">
                {{ loan.equipmentName }}
              </h3>
              @if (loan.purpose) {
              <p class="text-slate-600 dark:text-slate-400 mb-2">{{ loan.purpose }}</p>
              }
              <div class="space-y-1 text-sm">
                <p class="text-slate-500 dark:text-slate-400">
                  <span class="font-semibold">Borrowed:</span> {{ loan.loanDate | date:'mediumDate' }}
                </p>
                <p [class]="isOverdue(loan) ? 'text-red-500 font-semibold' : 'text-slate-500 dark:text-slate-400'">
                  <span class="font-semibold">Due:</span> {{ loan.dueDate | date:'mediumDate' }}
                </p>
              </div>
            </div>
            @if (isOverdue(loan)) {
            <span class="text-3xl">⚠️</span>
            } @else {
            <span class="text-3xl">✅</span>
            }
          </div>
        </div>
        }
      </div>
      }

      <!-- Available Equipment -->
      <div class="space-y-4">
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white">Available Equipment</h2>
        @if (availableEquipment().length > 0) {
        @for (equipment of availableEquipment(); track equipment.id) {
        <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 p-6 shadow-lg">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h3 class="text-xl font-bold text-slate-800 dark:text-white mb-2">
                {{ equipment.name }}
              </h3>
              @if (equipment.description) {
              <p class="text-slate-600 dark:text-slate-400 mb-2">{{ equipment.description }}</p>
              }
              <div class="flex gap-2">
                <span class="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 text-sm font-semibold">
                  {{ equipment.category }}
                </span>
                @if (equipment.condition) {
                <span class="px-3 py-1 rounded-full bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 text-sm">
                  {{ equipment.condition }}
                </span>
                }
              </div>
            </div>
            <span class="text-3xl">🏥</span>
          </div>
        </div>
        }
        } @else {
        <div class="text-center py-16">
          <p class="text-5xl mb-4">😊</p>
          <h3 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No equipment available</h3>
          <p class="text-slate-500 dark:text-slate-400 text-lg">
            Check back later for available medical equipment.
          </p>
        </div>
        }
      </div>
      }
    </div>
  `,
})
export class PatientEquipmentComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly equipmentService = inject(EquipmentService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // State
  allEquipment = signal<EquipmentDTO[]>([]);
  myLoans = signal<EquipmentLoanDTO[]>([]);
  isLoading = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserInfo();
    }
  }

  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
          this.loadData();
        }),
        catchError(err => {
          console.error('[PatientEquipment] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadData(): void {
    this.loadAllEquipment();
    this.loadMyLoans();
  }

  private loadAllEquipment(): void {
    this.isLoading.set(true);
    this.equipmentService.getAvailableEquipment()
      .pipe(
        tap(equipment => this.allEquipment.set(equipment)),
        catchError(err => {
          console.error('[PatientEquipment] Failed to load equipment', err);
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadMyLoans(): void {
    const borrowerId = this.userNeonDbId();
    if (!borrowerId) return;

    this.equipmentService.getActiveLoansByBorrower(borrowerId)
      .pipe(
        tap(loans => this.myLoans.set(loans)),
        catchError(err => {
          console.error('[PatientEquipment] Failed to load loans', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  isOverdue(loan: EquipmentLoanDTO): boolean {
    if (!loan.dueDate) return false;
    return new Date(loan.dueDate) < new Date() && loan.status === LoanStatus.ACTIVE;
  }

  get availableEquipment(): () => EquipmentDTO[] {
    return () => {
      return this.allEquipment().filter(eq => eq.status === EquipmentStatus.AVAILABLE);
    };
  }
}
