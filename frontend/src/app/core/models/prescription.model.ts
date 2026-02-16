// src/app/core/models/prescription.model.ts

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
  createdAt: string;
}

export interface PrescriptionRequestDTO {
  sessionId: number;
  medications: MedicationRequestDTO[];
}

export interface PrescriptionResponseDTO {
  id: number;
  sessionId: number;
  medications: MedicationResponseDTO[];
  createdAt: string;
  updatedAt: string;
}
