export interface DoctorNotification {
  id: number;
  doctorKeycloakId: string;
  patientKeycloakId: string;
  patientName: string;
  incidentType: string;
  severity: 'MODERE' | 'GRAVE';
  description: string;
  location?: string;
  actionTaken?: string;
  occurredAt?: string;
  logDate: string;
  read: boolean;
  createdAt: string;
  readAt?: string;
}
