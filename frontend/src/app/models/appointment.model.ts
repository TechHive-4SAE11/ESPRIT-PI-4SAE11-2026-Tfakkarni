export type AppointmentStatus =
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';

export interface Appointment {
  id?: number;
  title: string;
  description?: string;
  patientId: string;
  doctorId?: string;
  startTime: Date | string;
  endTime: Date | string;
  status: AppointmentStatus;
  type: 'CONSULTATION' | 'FOLLOW_UP';
  notes?: string;
  createdAt?: Date;
  createdBy?: string;
}
