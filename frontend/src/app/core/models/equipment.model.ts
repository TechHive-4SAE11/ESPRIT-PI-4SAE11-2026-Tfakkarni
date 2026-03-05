// Equipment Models
export enum EquipmentStatus {
  AVAILABLE = 'AVAILABLE',
  LOANED = 'LOANED',
  REQUESTED = 'REQUESTED',
  DONATED = 'DONATED',
  MAINTENANCE = 'MAINTENANCE'
}

export enum EquipmentCategory {
  MOBILITY = 'MOBILITY',
  RESPIRATORY = 'RESPIRATORY',
  CARDIAC = 'CARDIAC',
  ORTHOPEDIC = 'ORTHOPEDIC',
  FURNITURE = 'FURNITURE',
  MONITORING = 'MONITORING',
  THERAPEUTIC = 'THERAPEUTIC',
  SURGICAL = 'SURGICAL',
  OTHER = 'OTHER'
}

export enum EquipmentCondition {
  NEW = 'NEW',
  EXCELLENT = 'EXCELLENT',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR'
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
  category: EquipmentCategory;
  status?: EquipmentStatus;
  donationDate?: string;
  condition?: EquipmentCondition;
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
