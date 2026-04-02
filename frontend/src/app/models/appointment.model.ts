export interface Appointment {
  id?: number;
  title: string;
  description?: string;
  patientId: string;
  doctorId?: string;
  startTime: Date;
  endTime: Date;
  status: 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  type: 'CONSULTATION' | 'FOLLOW_UP';
  notes?: string;
  createdAt?: Date;
  createdBy?: string;
}
