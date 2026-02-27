export type ReminderType = 'CONFIRMATION' | 'PREPARATION' | 'FEEDBACK';
export type ReminderChannel = 'SMS' | 'EMAIL' | 'PUSH';
export type ReminderStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface Reminder {
  id?: number;
  appointmentId: number;
  patientId: string;
  reminderType: ReminderType;
  reminderTime: string; // ISO date-time string
  channel: ReminderChannel;
  patientPhone?: string;
  patientEmail?: string;
  message?: string;
  sent?: boolean;
  sentAt?: string | null;
  status?: ReminderStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateReminderDTO {
  appointmentId?: number;
  patientId: string;
  reminderType: ReminderType;
  reminderTime: string;
  channel: ReminderChannel;
  patientPhone?: string;
  patientEmail?: string;
  message?: string;
}

export interface UpdateReminderDTO extends CreateReminderDTO {}