// src/app/core/models/prescription.model.ts

export enum MedicationStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  ONGOING = 'ONGOING',
  DISCONTINUED = 'DISCONTINUED'
}

export interface MedicationRequestDTO {
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string;
}

export interface MedicationResponseDTO {
  id: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string;
  status: MedicationStatus;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
}

export interface PrescriptionRequestDTO {
  sessionId: number;
  medications: MedicationRequestDTO[];
}

export interface PrescriptionResponseDTO {
  id: number;
  sessionId: number;
  doctorId: string; // The backend database ID of the doctor
  medications: MedicationResponseDTO[];
  createdAt: string;
  updatedAt: string;
}
