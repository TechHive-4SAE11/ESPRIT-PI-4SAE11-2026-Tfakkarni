// src/app/core/models/prescription-template.model.ts

export interface TemplateMedicationDTO {
    id?: number;
    medicationName: string;
    dosage: string;
    frequency: string;
    duration: string;
    instructions: string;
}

export interface PrescriptionTemplateRequestDTO {
    name: string;
    description?: string;
    doctorId: string;
    medications: TemplateMedicationDTO[];
}

export interface PrescriptionTemplateResponseDTO {
    id: number;
    name: string;
    description: string | null;
    doctorId: string;
    medications: TemplateMedicationDTO[];
    createdAt: string;
    updatedAt: string;
}
