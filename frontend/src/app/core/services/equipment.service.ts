import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EquipmentDTO,
  EquipmentLoanDTO,
  EquipmentStatus,
  LoanStatus
} from '@/core/models/equipment.model';

@Injectable({
  providedIn: 'root',
})
export class EquipmentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medical/equipment`;
  private readonly loansUrl = `${environment.apiBaseUrl}/api/medical/loans`;

  constructor(private readonly http: HttpClient) { }

  // ─── Equipment CRUD ─────────────────────────────────────────

  createEquipment(equipment: EquipmentDTO): Observable<EquipmentDTO> {
    return this.http.post<EquipmentDTO>(this.baseUrl, equipment);
  }

  registerDonation(equipment: EquipmentDTO): Observable<EquipmentDTO> {
    return this.http.post<EquipmentDTO>(`${this.baseUrl}/donate`, equipment);
  }

  updateEquipment(id: number, equipment: EquipmentDTO): Observable<EquipmentDTO> {
    return this.http.put<EquipmentDTO>(`${this.baseUrl}/${id}`, equipment);
  }

  deleteEquipment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getEquipmentById(id: number): Observable<EquipmentDTO> {
    return this.http.get<EquipmentDTO>(`${this.baseUrl}/${id}`);
  }

  getAllEquipment(): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(this.baseUrl);
  }

  getEquipmentByStatus(status: EquipmentStatus): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/status/${status}`);
  }

  getEquipmentByCategory(category: string): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/category/${encodeURIComponent(category)}`);
  }

  getEquipmentByDonorId(donorId: number): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/donor/${donorId}`);
  }

  getAvailableEquipment(): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/available`);
  }

  searchEquipment(name: string): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/search?name=${encodeURIComponent(name)}`);
  }

  getEquipmentByCategoryAndStatus(category: string, status: EquipmentStatus): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/category/${encodeURIComponent(category)}/status/${status}`);
  }

  getEquipmentDonatedAfter(date: string): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/donated-after?date=${encodeURIComponent(date)}`);
  }

  countEquipmentByStatus(status: EquipmentStatus): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/status/${status}/count`);
  }

  getEquipmentWithOverdueLoans(): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/overdue`);
  }

  isEquipmentAvailable(id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/${id}/available`);
  }

  updateEquipmentStatus(id: number, status: EquipmentStatus): Observable<EquipmentDTO> {
    return this.http.patch<EquipmentDTO>(`${this.baseUrl}/${id}/status?status=${status}`, {});
  }

  getEquipmentSuggestions(keyword: string): Observable<EquipmentDTO[]> {
    return this.http.get<EquipmentDTO[]>(`${this.baseUrl}/suggestions?keyword=${encodeURIComponent(keyword)}`);
  }

  // ─── Equipment Loan CRUD ─────────────────────────────────────

  createLoan(loan: EquipmentLoanDTO): Observable<EquipmentLoanDTO> {
    return this.http.post<EquipmentLoanDTO>(this.loansUrl, loan);
  }

  borrowEquipment(loan: EquipmentLoanDTO): Observable<EquipmentLoanDTO> {
    return this.http.post<EquipmentLoanDTO>(`${this.loansUrl}/borrow`, loan);
  }

  updateLoan(id: number, loan: EquipmentLoanDTO): Observable<EquipmentLoanDTO> {
    return this.http.put<EquipmentLoanDTO>(`${this.loansUrl}/${id}`, loan);
  }

  deleteLoan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.loansUrl}/${id}`);
  }

  getLoanById(id: number): Observable<EquipmentLoanDTO> {
    return this.http.get<EquipmentLoanDTO>(`${this.loansUrl}/${id}`);
  }

  getAllLoans(): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(this.loansUrl);
  }

  getLoansByEquipmentId(equipmentId: number): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/equipment/${equipmentId}`);
  }

  getLoansByBorrowerId(borrowerId: number): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/borrower/${borrowerId}`);
  }

  getLoansByStatus(status: LoanStatus): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/status/${status}`);
  }

  getActiveLoansByBorrower(borrowerId: number): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/borrower/${borrowerId}/active`);
  }

  getOverdueLoans(): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/overdue`);
  }

  getLoansDueBetween(startDate: string, endDate: string): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(
      `${this.loansUrl}/due-between?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`
    );
  }

  getCurrentLoanForEquipment(equipmentId: number): Observable<EquipmentLoanDTO> {
    return this.http.get<EquipmentLoanDTO>(`${this.loansUrl}/equipment/${equipmentId}/current`);
  }

  isEquipmentLoaned(equipmentId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.loansUrl}/equipment/${equipmentId}/loaned`);
  }

  countActiveLoansByBorrower(borrowerId: number): Observable<number> {
    return this.http.get<number>(`${this.loansUrl}/borrower/${borrowerId}/active/count`);
  }

  returnEquipment(loanId: number): Observable<EquipmentLoanDTO> {
    return this.http.post<EquipmentLoanDTO>(`${this.loansUrl}/${loanId}/return`, {});
  }

  extendLoan(loanId: number, days: number): Observable<EquipmentLoanDTO> {
    return this.http.post<EquipmentLoanDTO>(`${this.loansUrl}/${loanId}/extend`, { days });
  }

  cancelLoan(loanId: number): Observable<EquipmentLoanDTO> {
    return this.http.post<EquipmentLoanDTO>(`${this.loansUrl}/${loanId}/cancel`, {});
  }

  getLoansByBorrowerAndStatus(borrowerId: number, status: LoanStatus): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/borrower/${borrowerId}/status/${status}`);
  }

  checkAndUpdateOverdueLoans(): Observable<string> {
    return this.http.post<string>(`${this.loansUrl}/check-overdue`, {});
  }

  getLoansDueSoon(days: number = 3): Observable<EquipmentLoanDTO[]> {
    return this.http.get<EquipmentLoanDTO[]>(`${this.loansUrl}/due-soon?days=${days}`);
  }
}
