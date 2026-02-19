// Equipment Models
export enum EquipmentStatus {
  AVAILABLE = 'AVAILABLE',
  LOANED = 'LOANED',
  REQUESTED = 'REQUESTED',
  DONATED = 'DONATED',
  MAINTENANCE = 'MAINTENANCE'
}

export enum LoanStatus {
  ACTIVE = 'ACTIVE',
  RETURNED = 'RETURNED',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED'
}

export interface EquipmentDTO {
  id?: number;
  name: string;
  description?: string;
  category: string;
  status?: EquipmentStatus;
  donationDate?: string;
  condition?: string;
  donorId: number;
  loans?: EquipmentLoanDTO[];
}

export interface EquipmentLoanDTO {
  id?: number;
  equipmentId: number;
  equipmentName?: string;
  borrowerId: number;
  loanDate?: string;
  dueDate: string;
  returnDate?: string;
  purpose?: string;
  notes?: string;
  status?: LoanStatus;
}
