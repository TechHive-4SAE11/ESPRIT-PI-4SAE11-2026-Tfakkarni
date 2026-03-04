import {
  Component, OnInit, signal, Input, inject, DestroyRef, computed
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { EquipmentService } from '@/core/services/equipment.service';
import { EquipmentDTO, EquipmentLoanDTO, EquipmentStatus, LoanStatus } from '@/core/models/equipment.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-patient-equipment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patient-equipment.component.html'
})
export class PatientEquipmentComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly equipService = inject(EquipmentService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // ─── DATA ─────────────────────────────────────────────────────
  allEquipment = signal<EquipmentDTO[]>([]);
  myLoans = signal<EquipmentLoanDTO[]>([]);
  allLoans = signal<EquipmentLoanDTO[]>([]);      // historique complet
  suggestions = signal<EquipmentDTO[]>([]);
  activeLoanCount = signal<number>(0);

  // ─── LOADING ──────────────────────────────────────────────────
  isLoading = signal<boolean>(false);
  isLoadingLoans = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  // ─── UI STATE ─────────────────────────────────────────────────
  activeTab = signal<'catalogue' | 'mes-prets' | 'historique'>('catalogue');
  searchKeyword = '';
  categoryFilter = '';
  selectedEquipment = signal<EquipmentDTO | null>(null);
  showBorrowModal = signal<boolean>(false);
  showReturnModal = signal<boolean>(false);
  selectedLoan = signal<EquipmentLoanDTO | null>(null);
  extendDays = 7;

  // ─── BORROW FORM ──────────────────────────────────────────────
  borrowForm = {
    purpose: '',
    notes: '',
    dueDate: ''
  };
  today = new Date().toISOString().split('T')[0];

  // ─── NOTIFICATION ─────────────────────────────────────────────
  notification = signal<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // ─── USER ─────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);

  // ─── COMPUTED ─────────────────────────────────────────────────
  availableEquipment = computed(() =>
    this.allEquipment().filter(e => e.status === EquipmentStatus.AVAILABLE)
  );
  activeLoans = computed(() =>
    this.myLoans().filter(l => l.status === LoanStatus.ACTIVE || l.status === LoanStatus.OVERDUE)
  );
  overdueLoans = computed(() =>
    this.myLoans().filter(l => this.isOverdue(l))
  );
  filteredEquipment = computed(() => {
    let list = this.allEquipment();
    if (this.categoryFilter) list = list.filter(e => e.category === this.categoryFilter);
    if (this.searchKeyword.trim().length >= 2) {
      const kw = this.searchKeyword.toLowerCase();
      list = list.filter(e => e.name.toLowerCase().includes(kw) || (e.description ?? '').toLowerCase().includes(kw));
    }
    return list;
  });
  categories = computed(() => [...new Set(this.allEquipment().map(e => e.category))].filter(Boolean));

  // ─── LIFECYCLE ────────────────────────────────────────────────
  ngOnInit(): void {
    if (this.keycloakId) this.loadUserInfo();
  }

  private notify(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.notification.set({ message, type });
    setTimeout(() => this.notification.set(null), 3500);
  }

  // ─── INIT ─────────────────────────────────────────────────────
  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId).pipe(
      tap(u => {
        this.userNeonDbId.set(u.id);
        this.loadAllEquipment();
        this.loadMyLoans(u.id);
        this.loadActiveLoanCount(u.id);
      }),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── EQUIPMENT — getAllEquipment / getAvailableEquipment ──────
  loadAllEquipment(): void {
    this.isLoading.set(true);
    this.equipService.getAvailableEquipment().pipe(
      tap(eq => this.allEquipment.set(eq)),
      catchError(() => of([])),
      finalize(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── SEARCH — searchEquipment ─────────────────────────────────
  onSearch(): void {
    if (this.searchKeyword.trim().length >= 2) {
      this.equipService.searchEquipment(this.searchKeyword).pipe(
        tap(res => this.allEquipment.set(res)),
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
    } else if (!this.searchKeyword.trim()) {
      this.loadAllEquipment();
    }
  }

  // ─── SUGGESTIONS — getEquipmentSuggestions ───────────────────
  loadSuggestions(): void {
    if (!this.searchKeyword.trim()) return;
    this.equipService.getEquipmentSuggestions(this.searchKeyword).pipe(
      tap(s => this.suggestions.set(s)),
      catchError(() => of([])),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── FILTER BY CATEGORY — getEquipmentByCategory ─────────────
  filterByCategory(cat: string): void {
    this.categoryFilter = cat;
    if (!cat) { this.loadAllEquipment(); return; }
    this.equipService.getEquipmentByCategory(cat).pipe(
      tap(res => this.allEquipment.set(res)),
      catchError(() => of([])),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── MY LOANS — getLoansByBorrowerId / getActiveLoansByBorrower
  loadMyLoans(borrowerId: number): void {
    this.isLoadingLoans.set(true);
    // Prêts actifs
    this.equipService.getActiveLoansByBorrower(borrowerId).pipe(
      tap(loans => this.myLoans.set(loans)),
      catchError(() => of([])),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
    // Tout l'historique
    this.equipService.getLoansByBorrowerId(borrowerId).pipe(
      tap(loans => this.allLoans.set(loans)),
      catchError(() => of([])),
      finalize(() => this.isLoadingLoans.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── COUNT — countActiveLoansByBorrower ──────────────────────
  loadActiveLoanCount(borrowerId: number): void {
    this.equipService.countActiveLoansByBorrower(borrowerId).pipe(
      tap(c => this.activeLoanCount.set(c || 0)),
      catchError(() => of(0)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── BORROW — borrowEquipment ─────────────────────────────────
  openBorrowModal(equipment: EquipmentDTO): void {
    this.selectedEquipment.set(equipment);
    this.borrowForm = { purpose: '', notes: '', dueDate: '' };
    this.showBorrowModal.set(true);
  }

  confirmBorrow(): void {
    const eq = this.selectedEquipment();
    const uid = this.userNeonDbId();
    if (!eq?.id || !uid || !this.borrowForm.dueDate) {
      this.notify('Please fill all required fields.', 'error');
      return;
    }
    this.isSubmitting.set(true);
    const loan: EquipmentLoanDTO = {
      equipmentId: eq.id,
      borrowerId: uid,
      dueDate: new Date(this.borrowForm.dueDate).toISOString(),
      purpose: this.borrowForm.purpose,
      notes: this.borrowForm.notes
    };
    this.equipService.borrowEquipment(loan).pipe(
      tap(() => {
        this.notify(`✅ "${eq.name}" borrowed successfully!`, 'success');
        this.showBorrowModal.set(false);
        this.loadAllEquipment();
        this.loadMyLoans(uid);
        this.loadActiveLoanCount(uid);
      }),
      catchError((err) => {
        console.error('Erreur Borrow:', err);
        this.notify('Error during borrowing.', 'error');
        return of(null);
      }),
      finalize(() => this.isSubmitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── RETURN — returnEquipment ─────────────────────────────────
  returnLoan(loan: EquipmentLoanDTO): void {
    if (!loan.id || !confirm(`Return "${loan.equipmentName}" ?`)) return;
    this.equipService.returnEquipment(loan.id).pipe(
      tap(() => {
        this.notify(`✅ "${loan.equipmentName}" returned!`, 'success');
        const uid = this.userNeonDbId();
        if (uid) { this.loadMyLoans(uid); this.loadActiveLoanCount(uid); }
        this.loadAllEquipment();
      }),
      catchError(() => { this.notify('Error during return.', 'error'); return of(null); }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── EXTEND — extendLoan ──────────────────────────────────────
  extendLoan(loan: EquipmentLoanDTO): void {
    if (!loan.id) return;
    this.selectedLoan.set(loan);
  }
  confirmExtend(): void {
    const loan = this.selectedLoan();
    if (!loan?.id) return;
    this.equipService.extendLoan(loan.id, this.extendDays).pipe(
      tap(() => {
        this.notify(`Loan extended by ${this.extendDays} days!`, 'success');
        this.selectedLoan.set(null);
        const uid = this.userNeonDbId();
        if (uid) this.loadMyLoans(uid);
      }),
      catchError(() => { this.notify('Error during extension.', 'error'); return of(null); }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── CANCEL — cancelLoan ──────────────────────────────────────
  cancelLoan(loan: EquipmentLoanDTO): void {
    if (!loan.id || !confirm(`Cancel loan for "${loan.equipmentName}" ?`)) return;
    this.equipService.cancelLoan(loan.id).pipe(
      tap(() => {
        this.notify('Loan cancelled.', 'info');
        const uid = this.userNeonDbId();
        if (uid) { this.loadMyLoans(uid); this.loadActiveLoanCount(uid); }
        this.loadAllEquipment();
      }),
      catchError(() => { this.notify('Error during cancellation.', 'error'); return of(null); }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── UTILS ────────────────────────────────────────────────────
  isOverdue(loan: EquipmentLoanDTO): boolean {
    if (!loan.dueDate) return false;
    return new Date(loan.dueDate) < new Date() && loan.status === LoanStatus.ACTIVE;
  }

  getLoanStatusBadge(loan: EquipmentLoanDTO): { label: string; cls: string } {
    if (this.isOverdue(loan)) return { label: '⚠️ Overdue', cls: 'bg-red-100 text-red-700 border-red-300 dark:bg-red-900/30 dark:text-red-300' };
    switch (loan.status) {
      case LoanStatus.ACTIVE: return { label: '✅ Active', cls: 'bg-emerald-100 text-emerald-700 border-emerald-300 dark:bg-emerald-900/30 dark:text-emerald-300' };
      case LoanStatus.RETURNED: return { label: '↩️ Returned', cls: 'bg-slate-100 text-slate-600 border-slate-300 dark:bg-slate-700 dark:text-slate-300' };
      case LoanStatus.CANCELLED: return { label: '✕ Cancelled', cls: 'bg-orange-100 text-orange-700 border-orange-300 dark:bg-orange-900/30 dark:text-orange-300' };
      default: return { label: loan.status ?? '?', cls: 'bg-slate-100 text-slate-600' };
    }
  }

  getEquipmentStatusBadge(status?: EquipmentStatus): { label: string; cls: string } {
    switch (status) {
      case EquipmentStatus.AVAILABLE: return { label: '✅ Available', cls: 'bg-emerald-100 text-emerald-700' };
      case EquipmentStatus.LOANED: return { label: '🔄 Loaned', cls: 'bg-blue-100 text-blue-700' };
      case EquipmentStatus.MAINTENANCE: return { label: '🔧 Maintenance', cls: 'bg-amber-100 text-amber-700' };
      case EquipmentStatus.DONATED: return { label: '🎁 Donated', cls: 'bg-violet-100 text-violet-700' };
      default: return { label: status ?? '?', cls: 'bg-slate-100 text-slate-600' };
    }
  }

  setTab(tab: 'catalogue' | 'mes-prets' | 'historique'): void {
    this.activeTab.set(tab);
  }

  get LoanStatus(): typeof LoanStatus { return LoanStatus; }
}
