import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardTableImports } from '@/shared/components/table/table.imports';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { EquipmentService } from '@/core/services/equipment.service';
import { EquipmentDTO, EquipmentLoanDTO, EquipmentStatus, LoanStatus } from '@/core/models/equipment.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-equipment-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent,
    ZardBadgeComponent,
    ZardTableImports,
    ZardSkeletonComponent
  ],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <h2 class="text-2xl font-bold">Equipment Management</h2>
        <button z-button (click)="showAvailable.set(true)">
          <z-icon zType="search" class="mr-2" />
          Browse Available
        </button>
      </div>

      <!-- Stats -->
      <div class="grid gap-4 md:grid-cols-4">
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Total Equipment</p>
              <p class="text-3xl font-bold">{{ allEquipment().length }}</p>
            </div>
            <z-icon zType="shield" class="text-primary h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Available</p>
              <p class="text-3xl font-bold">{{ availableCount() }}</p>
            </div>
            <z-icon zType="check" class="text-green-500 h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">My Loans</p>
              <p class="text-3xl font-bold">{{ myLoans().length }}</p>
            </div>
            <z-icon zType="clock" class="text-blue-500 h-8 w-8" />
          </div>
        </z-card>
        <z-card class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-muted-foreground">Overdue</p>
              <p class="text-3xl font-bold">{{ overdueCount() }}</p>
            </div>
            <z-icon zType="triangle-alert" class="text-red-500 h-8 w-8" />
          </div>
        </z-card>
      </div>

      <!-- My Active Loans -->
      @if (myLoans().length > 0) {
      <z-card>
        <div class="p-6">
          <h3 class="text-lg font-semibold mb-4">My Active Loans</h3>
          <table z-table>
            <thead z-table-header>
              <tr z-table-row>
                <th z-table-head>Equipment</th>
                <th z-table-head>Category</th>
                <th z-table-head>Loan Date</th>
                <th z-table-head>Due Date</th>
                <th z-table-head>Status</th>
                <th z-table-head>Actions</th>
              </tr>
            </thead>
            <tbody z-table-body>
              @for (loan of myLoans(); track loan.id) {
              <tr z-table-row>
                <td z-table-cell class="font-medium">{{ loan.equipmentName }}</td>
                <td z-table-cell class="text-muted-foreground">
                  {{ getEquipmentCategory(loan.equipmentId) }}
                </td>
                <td z-table-cell class="text-muted-foreground">
                  {{ loan.loanDate | date:'short' }}
                </td>
                <td z-table-cell>
                  <span [class]="isOverdue(loan) ? 'text-red-500 font-semibold' : ''">
                    {{ loan.dueDate | date:'short' }}
                  </span>
                </td>
                <td z-table-cell>
                  <z-badge [zType]="getLoanStatusBadgeType(loan.status)">
                    {{ loan.status }}
                  </z-badge>
                </td>
                <td z-table-cell>
                  <div class="flex gap-2">
                    @if (loan.status === LoanStatus.ACTIVE) {
                    <button z-button zType="ghost" zSize="sm" (click)="returnEquipment(loan.id!)">
                      <z-icon zType="check" class="mr-1" />
                      Return
                    </button>
                    }
                  </div>
                </td>
              </tr>
              }
            </tbody>
          </table>
        </div>
      </z-card>
      }

      <!-- Available Equipment -->
      @if (showAvailable()) {
      <z-card>
        <div class="p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold">Available Equipment</h3>
            <button z-button zType="ghost" zSize="sm" (click)="showAvailable.set(false)">
              <z-icon zType="x" class="mr-1" />
              Close
            </button>
          </div>
          @if (isLoading()) {
          <z-skeleton class="h-32 w-full" />
          } @else if (availableEquipment().length > 0) {
          <table z-table>
            <thead z-table-header>
              <tr z-table-row>
                <th z-table-head>Name</th>
                <th z-table-head>Category</th>
                <th z-table-head>Description</th>
                <th z-table-head>Condition</th>
                <th z-table-head>Actions</th>
              </tr>
            </thead>
            <tbody z-table-body>
              @for (equipment of availableEquipment(); track equipment.id) {
              <tr z-table-row>
                <td z-table-cell class="font-medium">{{ equipment.name }}</td>
                <td z-table-cell>
                  <z-badge zType="secondary">{{ equipment.category }}</z-badge>
                </td>
                <td z-table-cell class="text-muted-foreground">
                  {{ equipment.description || '-' }}
                </td>
                <td z-table-cell>
                  {{ equipment.condition || '-' }}
                </td>
                <td z-table-cell>
                  <button z-button zType="ghost" zSize="sm" (click)="borrowEquipment(equipment)">
                    <z-icon zType="plus" class="mr-1" />
                    Borrow
                  </button>
                </td>
              </tr>
              }
            </tbody>
          </table>
          } @else {
          <div class="text-center py-8">
            <z-icon zType="shield" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p class="text-muted-foreground">No available equipment at the moment.</p>
          </div>
          }
        </div>
      </z-card>
      }

      <!-- All Equipment List -->
      @if (!showAvailable()) {
      <z-card>
        <div class="p-6">
          <h3 class="text-lg font-semibold mb-4">All Equipment</h3>
          @if (isLoading()) {
          <z-skeleton class="h-32 w-full" />
          } @else if (allEquipment().length > 0) {
          <table z-table>
            <thead z-table-header>
              <tr z-table-row>
                <th z-table-head>Name</th>
                <th z-table-head>Category</th>
                <th z-table-head>Status</th>
                <th z-table-head>Donation Date</th>
                <th z-table-head>Actions</th>
              </tr>
            </thead>
            <tbody z-table-body>
              @for (equipment of allEquipment(); track equipment.id) {
              <tr z-table-row>
                <td z-table-cell class="font-medium">{{ equipment.name }}</td>
                <td z-table-cell>
                  <z-badge zType="secondary">{{ equipment.category }}</z-badge>
                </td>
                <td z-table-cell>
                  <z-badge [zType]="getEquipmentStatusBadgeType(equipment.status)">
                    {{ equipment.status }}
                  </z-badge>
                </td>
                <td z-table-cell class="text-muted-foreground">
                  {{ equipment.donationDate | date:'short' }}
                </td>
                <td z-table-cell>
                  <button z-button zType="ghost" zSize="sm" (click)="viewEquipment(equipment)">
                    <z-icon zType="eye" class="mr-1" />
                    View
                  </button>
                </td>
              </tr>
              }
            </tbody>
          </table>
          } @else {
          <div class="text-center py-8">
            <z-icon zType="shield" class="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p class="text-muted-foreground">No equipment found.</p>
          </div>
          }
        </div>
      </z-card>
      }
    </div>
  `,
})
export class EquipmentManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly equipmentService = inject(EquipmentService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // State
  allEquipment = signal<EquipmentDTO[]>([]);
  myLoans = signal<EquipmentLoanDTO[]>([]);
  isLoading = signal<boolean>(false);
  showAvailable = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);

  LoanStatus = LoanStatus;
  EquipmentStatus = EquipmentStatus;

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
          console.error('[EquipmentManagement] Failed to load user info', err);
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
    this.equipmentService.getAllEquipment()
      .pipe(
        tap(equipment => this.allEquipment.set(equipment)),
        catchError(err => {
          console.error('[EquipmentManagement] Failed to load equipment', err);
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
          console.error('[EquipmentManagement] Failed to load loans', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  borrowEquipment(equipment: EquipmentDTO): void {
    const borrowerId = this.userNeonDbId();
    if (!borrowerId || !equipment.id) return;

    const dueDate = new Date();
    dueDate.setDate(dueDate.getDate() + 30); // 30 days from now

    const loan: EquipmentLoanDTO = {
      equipmentId: equipment.id,
      borrowerId: borrowerId,
      dueDate: dueDate.toISOString(),
      status: LoanStatus.ACTIVE
    };

    this.equipmentService.borrowEquipment(loan)
      .pipe(
        tap(() => {
          this.loadData();
          this.showAvailable.set(false);
        }),
        catchError(err => {
          console.error('[EquipmentManagement] Failed to borrow equipment', err);
          alert('Failed to borrow equipment. Please try again.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  returnEquipment(loanId: number): void {
    if (!confirm('Are you sure you want to return this equipment?')) return;

    this.equipmentService.returnEquipment(loanId)
      .pipe(
        tap(() => this.loadData()),
        catchError(err => {
          console.error('[EquipmentManagement] Failed to return equipment', err);
          alert('Failed to return equipment. Please try again.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  viewEquipment(equipment: EquipmentDTO): void {
    // TODO: Implement view equipment details
    console.log('View equipment:', equipment);
  }

  get availableEquipment(): () => EquipmentDTO[] {
    return () => {
      return this.allEquipment().filter(eq => eq.status === EquipmentStatus.AVAILABLE);
    };
  }

  get availableCount(): () => number {
    return () => this.availableEquipment().length;
  }

  get overdueCount(): () => number {
    return () => {
      return this.myLoans().filter(loan => {
        if (!loan.dueDate) return false;
        return new Date(loan.dueDate) < new Date() && loan.status === LoanStatus.ACTIVE;
      }).length;
    };
  }

  isOverdue(loan: EquipmentLoanDTO): boolean {
    if (!loan.dueDate) return false;
    return new Date(loan.dueDate) < new Date() && loan.status === LoanStatus.ACTIVE;
  }

  getEquipmentCategory(equipmentId: number): string {
    const equipment = this.allEquipment().find(eq => eq.id === equipmentId);
    return equipment?.category || '-';
  }

  getLoanStatusBadgeType(status?: LoanStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case LoanStatus.ACTIVE:
        return 'default';
      case LoanStatus.RETURNED:
        return 'secondary';
      case LoanStatus.OVERDUE:
        return 'destructive';
      case LoanStatus.CANCELLED:
        return 'outline';
      default:
        return 'secondary';
    }
  }

  getEquipmentStatusBadgeType(status?: EquipmentStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case EquipmentStatus.AVAILABLE:
        return 'default';
      case EquipmentStatus.LOANED:
        return 'secondary';
      case EquipmentStatus.MAINTENANCE:
        return 'destructive';
      default:
        return 'outline';
    }
  }
}
