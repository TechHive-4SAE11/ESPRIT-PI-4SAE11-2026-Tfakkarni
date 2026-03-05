import {
  Component,
  OnInit,
  signal,
  computed,
  Input,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { EquipmentService } from '@/core/services/equipment.service';
import {
  EquipmentDTO,
  EquipmentLoanDTO,
  EquipmentStatus,
  LoanStatus,
  EquipmentCategory,
  EquipmentCondition
} from '@/core/models/equipment.model';
import { UserApiService } from '@/core/services/user-api.service';

type TabType = 'equipment' | 'loans' | 'stats';

interface Notification {
  message: string;
  type: 'success' | 'error' | 'info';
}

interface EquipmentForm {
  name: string;
  description: string;
  category: string;
  condition: string;
  status: string;
}

@Component({
  selector: 'app-equipment-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-management.component.html',
})
export class EquipmentManagementComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly equipmentService = inject(EquipmentService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // ─── Enums exposed to template ─────────────────────────────
  equipmentStatuses: EquipmentStatus[] = [
    EquipmentStatus.AVAILABLE,
    EquipmentStatus.LOANED,
    EquipmentStatus.REQUESTED,
    EquipmentStatus.DONATED,
    EquipmentStatus.MAINTENANCE
  ];
  loanStatuses: LoanStatus[] = [
    LoanStatus.ACTIVE,
    LoanStatus.RETURNED,
    LoanStatus.OVERDUE,
    LoanStatus.CANCELLED
  ];
  equipmentCategories: EquipmentCategory[] = [
    EquipmentCategory.MOBILITY,
    EquipmentCategory.RESPIRATORY,
    EquipmentCategory.CARDIAC,
    EquipmentCategory.ORTHOPEDIC,
    EquipmentCategory.FURNITURE,
    EquipmentCategory.OTHER
  ];
  equipmentConditions: EquipmentCondition[] = [
    EquipmentCondition.NEW,
    EquipmentCondition.EXCELLENT,
    EquipmentCondition.GOOD,
    EquipmentCondition.FAIR,
    EquipmentCondition.POOR
  ];

  // ─── State ──────────────────────────────────────────────────
  activeTab = signal<TabType>('equipment');
  isLoading = signal<boolean>(false);
  isLoadingLoans = signal<boolean>(false);
  isSaving = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);
  notification = signal<Notification | null>(null);
  private notifTimeout: ReturnType<typeof setTimeout> | null = null;

  // ─── Data ───────────────────────────────────────────────────
  allEquipment = signal<EquipmentDTO[]>([]);
  displayedEquipment = signal<EquipmentDTO[]>([]);
  allLoans = signal<EquipmentLoanDTO[]>([]);
  displayedLoans = signal<EquipmentLoanDTO[]>([]);
  myActiveLoans = signal<EquipmentLoanDTO[]>([]);
  overdueLoans = signal<EquipmentLoanDTO[]>([]);
  dueSoonLoans = signal<EquipmentLoanDTO[]>([]);

  // ─── Filters ────────────────────────────────────────────────
  searchQuery = '';
  filterStatus = '';
  filterCategory = '';
  filterLoanStatus = '';

  // ─── Modals ─────────────────────────────────────────────────
  showEquipmentModal = signal<boolean>(false);
  showDonateModal = signal<boolean>(false);
  showExtendModal = signal<boolean>(false);
  showStatusModal = signal<boolean>(false);
  showDetailsModal = signal<boolean>(false);

  editingEquipment = signal<EquipmentDTO | null>(null);
  selectedEquipment = signal<EquipmentDTO | null>(null);
  selectedLoan = signal<EquipmentLoanDTO | null>(null);
  detailsEquipment = signal<EquipmentDTO | null>(null);

  // ─── Forms ──────────────────────────────────────────────────
  eqForm: EquipmentForm = { name: '', description: '', category: EquipmentCategory.OTHER, condition: EquipmentCondition.GOOD, status: EquipmentStatus.AVAILABLE };
  donateForm: EquipmentForm = { name: '', description: '', category: EquipmentCategory.OTHER, condition: EquipmentCondition.GOOD, status: EquipmentStatus.DONATED };
  extendDays = 7;
  newStatus: EquipmentStatus = EquipmentStatus.AVAILABLE;

  // ─── Computed ───────────────────────────────────────────────
  uniqueCategories = computed<string[]>(() => {
    const cats = this.allEquipment().map(e => e.category).filter(Boolean);
    return [...new Set(cats)];
  });

  // ─── Lifecycle ──────────────────────────────────────────────
  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserInfo();
    } else {
      // Load without user context
      this.loadAllEquipment();
      this.loadAllLoansPublic();
    }
  }

  // ─── User Info ──────────────────────────────────────────────
  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
          this.loadAllData();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] Failed to load user info', err);
          this.loadAllEquipment();
          this.loadAllLoansPublic();
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Data Loading Methods ────────────────────────────────────

  private loadAllData(): void {
    this.loadAllEquipment();
    // Only load user-specific data if borrowerId is available
    // loadOverdueLoans() is now lazy (triggered by button) to avoid 500s on init
    const borrowerId = this.userNeonDbId();
    if (borrowerId) {
      this.loadMyActiveLoans();
      this.loadAllMyLoans();
    }
  }

  loadAllEquipment(): void {
    this.isLoading.set(true);
    this.equipmentService.getAllEquipment()
      .pipe(
        tap(list => {
          this.allEquipment.set(list);
          this.displayedEquipment.set(list);
          this.applyEquipmentFilters();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] getAllEquipment failed', err);
          this.notify('Error loading equipment', 'error');
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadAvailableEquipment(): void {
    this.isLoading.set(true);
    this.equipmentService.getAvailableEquipment()
      .pipe(
        tap(list => {
          this.displayedEquipment.set(list);
          this.notify(`${list.length} equipment(s) available`, 'info');
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] getAvailableEquipment failed', err);
          this.notify('Error loading', 'error');
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadOverdueEquipment(): void {
    this.isLoading.set(true);
    this.equipmentService.getEquipmentWithOverdueLoans()
      .pipe(
        tap(list => {
          this.displayedEquipment.set(list);
          this.notify(`${list.length} equipment(s) with overdue loans`, 'info');
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] getEquipmentWithOverdueLoans failed', err);
          this.notify('Error loading', 'error');
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadMyActiveLoans(): void {
    const borrowerId = this.userNeonDbId();
    if (!borrowerId) return;
    this.equipmentService.getActiveLoansByBorrower(borrowerId)
      .pipe(
        tap(loans => this.myActiveLoans.set(loans)),
        catchError(err => {
          console.error('[EquipmentMgmt] getActiveLoansByBorrower failed', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadOverdueLoans(): void {
    this.equipmentService.getOverdueLoans()
      .pipe(
        tap(loans => this.overdueLoans.set(loans)),
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadAllMyLoans(): void {
    const borrowerId = this.userNeonDbId();
    if (!borrowerId) {
      this.loadAllLoansPublic();
      return;
    }
    this.isLoadingLoans.set(true);
    this.equipmentService.getLoansByBorrowerId(borrowerId)
      .pipe(
        tap(loans => {
          this.allLoans.set(loans);
          this.displayedLoans.set(loans);
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] getLoansByBorrowerId failed', err);
          this.notify('Error loading loans', 'error');
          return of([]);
        }),
        finalize(() => this.isLoadingLoans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadAllLoansPublic(): void {
    this.isLoadingLoans.set(true);
    this.equipmentService.getAllLoans()
      .pipe(
        tap(loans => {
          this.allLoans.set(loans);
          this.displayedLoans.set(loans);
        }),
        catchError(() => of([])),
        finalize(() => this.isLoadingLoans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  loadDueSoonLoans(): void {
    this.isLoadingLoans.set(true);
    this.equipmentService.getLoansDueSoon(3)
      .pipe(
        tap(loans => {
          this.dueSoonLoans.set(loans);
          this.displayedLoans.set(loans);
          this.activeTab.set('loans');
          this.notify(`${loans.length} loan(s) due in the next 3 days`, 'info');
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] getLoansDueSoon failed', err);
          this.notify('Error loading', 'error');
          return of([]);
        }),
        finalize(() => this.isLoadingLoans.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  checkOverdueLoans(): void {
    this.equipmentService.checkAndUpdateOverdueLoans()
      .pipe(
        tap(msg => {
          this.notify(msg || 'Overdue loans updated', 'success');
          this.loadAllData();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] checkAndUpdateOverdueLoans failed', err);
          this.notify('Error during verification', 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Filter Methods (ALL LOCAL – avoids 500 from backend filter endpoints) ──

  onSearchChange(_query: string): void {
    // Local filtering only – backend /search returns 500 on current env
    this.applyEquipmentFilters();
  }

  onFilterStatusChange(_status: string): void {
    // Local filtering only – backend /status & /category+status return 500
    this.applyEquipmentFilters();
  }

  onFilterCategoryChange(_category: string): void {
    // Local filtering only
    this.applyEquipmentFilters();
  }

  onLoanStatusFilterChange(status: string): void {
    // Local filtering on already-loaded loans signal
    if (!status) {
      this.displayedLoans.set(this.allLoans());
      return;
    }
    this.displayedLoans.set(
      this.allLoans().filter(l => l.status === status as LoanStatus)
    );
  }

  private applyEquipmentFilters(): void {
    let list = [...this.allEquipment()];
    if (this.filterStatus) list = list.filter(e => e.status === this.filterStatus);
    if (this.filterCategory) list = list.filter(e => e.category === this.filterCategory);
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(e => e.name?.toLowerCase().includes(q));
    }
    this.displayedEquipment.set(list);
  }

  // ─── CRUD Equipment ──────────────────────────────────────────

  openAddEquipmentModal(): void {
    this.editingEquipment.set(null);
    this.eqForm = { name: '', description: '', category: EquipmentCategory.OTHER, condition: EquipmentCondition.GOOD, status: EquipmentStatus.AVAILABLE };
    this.showEquipmentModal.set(true);
  }

  openEditModal(eq: EquipmentDTO): void {
    this.editingEquipment.set(eq);
    this.eqForm = {
      name: eq.name,
      description: eq.description || '',
      category: eq.category || EquipmentCategory.OTHER,
      condition: eq.condition || EquipmentCondition.GOOD,
      status: eq.status || EquipmentStatus.AVAILABLE
    };
    this.showEquipmentModal.set(true);
  }

  closeEquipmentModal(): void {
    this.showEquipmentModal.set(false);
    this.editingEquipment.set(null);
  }

  saveEquipment(): void {
    if (!this.eqForm.name.trim() || !this.eqForm.category.trim()) {
      this.notify('Name and category are required', 'error');
      return;
    }
    this.isSaving.set(true);
    const payload: EquipmentDTO = {
      name: this.eqForm.name,
      description: this.eqForm.description,
      category: this.eqForm.category as EquipmentCategory,
      condition: this.eqForm.condition as EquipmentCondition,
      status: this.eqForm.status as EquipmentStatus,
      donorId: this.userNeonDbId() ?? 1,
    };

    const editing = this.editingEquipment();
    const obs = editing?.id
      ? this.equipmentService.updateEquipment(editing.id, payload)
      : this.equipmentService.createEquipment(payload);

    obs.pipe(
      tap(() => {
        this.notify(editing ? 'Equipment updated successfully' : 'Equipment created successfully', 'success');
        this.closeEquipmentModal();
        this.loadAllEquipment();
      }),
      catchError(err => {
        console.error('[EquipmentMgmt] saveEquipment failed', err);
        this.notify('Error saving', 'error');
        return of(null);
      }),
      finalize(() => this.isSaving.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  confirmDelete(eq: EquipmentDTO): void {
    if (!confirm(`Delete "${eq.name}" ? This action is irreversible.`)) return;
    if (!eq.id) return;
    this.equipmentService.deleteEquipment(eq.id)
      .pipe(
        tap(() => {
          this.notify('Equipment deleted successfully', 'success');
          this.loadAllEquipment();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] deleteEquipment failed', err);
          const status = err?.status;
          let msg = 'Error deleting';
          if (status === 500 || status === 409) {
            msg = '⚠️ Cannot delete: this equipment has existing loans. Cancel or return them first.';
          } else if (err?.error?.message) {
            msg = err.error.message;
          }
          this.notify(msg, 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Donation ────────────────────────────────────────────────

  openDonateModal(): void {
    this.donateForm = { name: '', description: '', category: EquipmentCategory.OTHER, condition: EquipmentCondition.GOOD, status: EquipmentStatus.DONATED };
    this.showDonateModal.set(true);
  }

  closeDonateModal(): void {
    this.showDonateModal.set(false);
  }

  registerDonation(): void {
    if (!this.donateForm.name.trim() || !this.donateForm.category.trim()) {
      this.notify('Name and category are required', 'error');
      return;
    }
    this.isSaving.set(true);
    const payload: EquipmentDTO = {
      name: this.donateForm.name,
      description: this.donateForm.description,
      category: this.donateForm.category as EquipmentCategory,
      condition: this.donateForm.condition as EquipmentCondition,
      status: EquipmentStatus.DONATED,
      donorId: this.userNeonDbId() ?? 1,
    };
    this.equipmentService.registerDonation(payload)
      .pipe(
        tap(() => {
          this.notify('Donation recorded successfully!', 'success');
          this.closeDonateModal();
          this.loadAllEquipment();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] registerDonation failed', err);
          this.notify('Error recording donation', 'error');
          return of(null);
        }),
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Status Update ───────────────────────────────────────────

  openStatusModal(eq: EquipmentDTO): void {
    this.selectedEquipment.set(eq);
    this.newStatus = eq.status || EquipmentStatus.AVAILABLE;
    this.showStatusModal.set(true);
  }

  closeStatusModal(): void {
    this.showStatusModal.set(false);
    this.selectedEquipment.set(null);
  }

  confirmStatusUpdate(): void {
    const eq = this.selectedEquipment();
    if (!eq?.id) return;
    this.isSaving.set(true);
    this.equipmentService.updateEquipmentStatus(eq.id, this.newStatus)
      .pipe(
        tap(() => {
          this.notify(`Status updated to: ${this.newStatus}`, 'success');
          this.closeStatusModal();
          this.loadAllEquipment();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] updateEquipmentStatus failed', err);
          this.notify('Error updating status', 'error');
          return of(null);
        }),
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Details ─────────────────────────────────────────────────

  viewEquipmentDetails(eq: EquipmentDTO): void {
    if (!eq.id) {
      this.detailsEquipment.set(eq);
      this.showDetailsModal.set(true);
      return;
    }
    // Fetch fresh details from server
    this.equipmentService.getEquipmentById(eq.id)
      .pipe(
        tap(details => {
          this.detailsEquipment.set(details);
          this.showDetailsModal.set(true);
        }),
        catchError(err => {
          // Silently fallback to cached version - no error toast needed
          console.warn('[EquipmentMgmt] getEquipmentById failed, using cached data', err);
          this.detailsEquipment.set(eq);
          this.showDetailsModal.set(true);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  closeDetailsModal(): void {
    this.showDetailsModal.set(false);
    this.detailsEquipment.set(null);
  }

  // ─── Return ──────────────────────────────────────────────────

  returnEquipment(loanId: number): void {
    if (!confirm('Confirm equipment return?')) return;
    this.equipmentService.returnEquipment(loanId)
      .pipe(
        tap(() => {
          this.notify('Equipment returned successfully!', 'success');
          this.loadAllEquipment();
          this.loadAllMyLoans();
          this.loadMyActiveLoans();
          this.loadOverdueLoans();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] returnEquipment failed', err);
          this.notify('Error returning equipment', 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Extend Loan ─────────────────────────────────────────────

  openExtendModal(loan: EquipmentLoanDTO): void {
    this.selectedLoan.set(loan);
    this.extendDays = 7;
    this.showExtendModal.set(true);
  }

  closeExtendModal(): void {
    this.showExtendModal.set(false);
    this.selectedLoan.set(null);
  }

  confirmExtend(): void {
    const loan = this.selectedLoan();
    if (!loan?.id || !this.extendDays || this.extendDays < 1) {
      this.notify('Invalid number of days', 'error');
      return;
    }
    this.isSaving.set(true);
    this.equipmentService.extendLoan(loan.id, this.extendDays)
      .pipe(
        tap(() => {
          this.notify(`Loan extended by ${this.extendDays} day(s)`, 'success');
          this.closeExtendModal();
          this.loadAllMyLoans();
          this.loadMyActiveLoans();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] extendLoan failed', err);
          this.notify('Error extending loan', 'error');
          return of(null);
        }),
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Cancel Loan ─────────────────────────────────────────────

  cancelLoan(loanId: number): void {
    if (!confirm('Cancel this loan?')) return;
    this.equipmentService.cancelLoan(loanId)
      .pipe(
        tap(() => {
          this.notify('Loan cancelled', 'success');
          this.loadAllEquipment();
          this.loadAllMyLoans();
          this.loadMyActiveLoans();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] cancelLoan failed', err);
          this.notify('Error cancelling loan', 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Delete Loan ─────────────────────────────────────────────

  deleteLoan(loanId: number): void {
    if (!confirm('Delete this loan permanently?')) return;
    this.equipmentService.deleteLoan(loanId)
      .pipe(
        tap(() => {
          this.notify('Loan deleted', 'success');
          this.loadAllMyLoans();
        }),
        catchError(err => {
          console.error('[EquipmentMgmt] deleteLoan failed', err);
          this.notify('Error deleting', 'error');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  // ─── Tabs ────────────────────────────────────────────────────

  setActiveTab(tab: TabType): void {
    this.activeTab.set(tab);
    if (tab === 'loans' && this.displayedLoans().length === 0) {
      this.loadAllMyLoans();
    }
  }

  // ─── Helpers / UI ────────────────────────────────────────────

  countByStatus(status: string): number {
    return this.allEquipment().filter(e => e.status === status).length;
  }

  isLoanOverdue(loan: EquipmentLoanDTO): boolean {
    if (!loan.dueDate) return false;
    return new Date(loan.dueDate) < new Date() && loan.status === LoanStatus.ACTIVE;
  }

  getStatusBgClass(status?: EquipmentStatus | string): string {
    switch (status) {
      case EquipmentStatus.AVAILABLE: return 'bg-emerald-500';
      case EquipmentStatus.LOANED: return 'bg-blue-500';
      case EquipmentStatus.MAINTENANCE: return 'bg-red-500';
      case EquipmentStatus.DONATED: return 'bg-purple-500';
      case EquipmentStatus.REQUESTED: return 'bg-amber-500';
      default: return 'bg-slate-500';
    }
  }

  getStatusLabel(status?: EquipmentStatus | string): string {
    switch (status) {
      case EquipmentStatus.AVAILABLE: return '✅ Available';
      case EquipmentStatus.LOANED: return '📤 Loaned';
      case EquipmentStatus.MAINTENANCE: return '🔧 Maintenance';
      case EquipmentStatus.DONATED: return '🎁 Donated';
      case EquipmentStatus.REQUESTED: return '⏳ Requested';
      default: return '❓ Unknown';
    }
  }

  getStatusEmoji(status?: string): string {
    switch (status) {
      case 'AVAILABLE': return '✅';
      case 'LOANED': return '📤';
      case 'MAINTENANCE': return '🔧';
      case 'DONATED': return '🎁';
      case 'REQUESTED': return '⏳';
      default: return '❓';
    }
  }

  getCategoryEmoji(category?: string): string {
    if (!category) return '📦';
    const cat = category.toLowerCase();
    if (cat.includes('mobilit') || cat.includes('fauteuil') || cat.includes('walker')) return '♿';
    if (cat.includes('oxygène') || cat.includes('oxygen') || cat.includes('respirat')) return '🫁';
    if (cat.includes('cardiac') || cat.includes('cardiaq') || cat.includes('coeur')) return '❤️';
    if (cat.includes('orthopéd') || cat.includes('orthop')) return '🦴';
    if (cat.includes('moniteur') || cat.includes('monitor')) return '📺';
    if (cat.includes('lit') || cat.includes('bed')) return '🛏️';
    return '🏥';
  }

  getLoanStatusClass(status?: LoanStatus | string): string {
    switch (status) {
      case LoanStatus.ACTIVE: return 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300';
      case LoanStatus.RETURNED: return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300';
      case LoanStatus.OVERDUE: return 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300';
      case LoanStatus.CANCELLED: return 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-400';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  getLoanCardClass(status?: LoanStatus | string): string {
    switch (status) {
      case LoanStatus.ACTIVE: return 'bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800';
      case LoanStatus.RETURNED: return 'bg-emerald-50 dark:bg-emerald-900/10 border-emerald-200 dark:border-emerald-800';
      case LoanStatus.OVERDUE: return 'bg-red-50 dark:bg-red-900/10 border-red-300 dark:border-red-800';
      case LoanStatus.CANCELLED: return 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 opacity-60';
      default: return 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700';
    }
  }

  // ─── Notifications ───────────────────────────────────────────

  notify(message: string, type: 'success' | 'error' | 'info'): void {
    if (this.notifTimeout) clearTimeout(this.notifTimeout);
    this.notification.set({ message, type });
    this.notifTimeout = setTimeout(() => this.notification.set(null), 4500);
  }

  clearNotification(): void {
    if (this.notifTimeout) clearTimeout(this.notifTimeout);
    this.notification.set(null);
  }
}
