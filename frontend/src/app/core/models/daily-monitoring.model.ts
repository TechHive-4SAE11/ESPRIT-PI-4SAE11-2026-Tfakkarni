// ─── Enums ────────────────────────────────────────────────────────────────────
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
export type QuantityLevel = 'COMPLET' | 'DEMI' | 'PEU' | 'RIEN';
export type AppetiteLevel = 'BON' | 'MOYEN' | 'FAIBLE';
export type IntakeStatus = 'PRIS' | 'OUBLIE' | 'REFUSE' | 'EN_RETARD';
export type ActivityType = 'PHYSIQUE' | 'COGNITIVE' | 'SOCIALE' | 'HYGIENE' | 'PROMENADE' | 'AUTRE';
export type IntensityLevel = 'FAIBLE' | 'MODERE' | 'ELEVE';
export type IncidentType = 'CHUTE' | 'CONFUSION' | 'AGITATION' | 'DEAMBULATION' | 'CRISE' | 'AUTRE';
export type SeverityLevel = 'LEGER' | 'MODERE' | 'GRAVE';

// ─── Available Medication (from prescriptions) ────────────────────────────────
export interface AvailableMedication {
  id: number;
  medicationName: string;
  dosage?: string;
  frequency?: string;
  instructions?: string;
}

// ─── Nutrition ────────────────────────────────────────────────────────────────
export interface NutritionEntryRequest {
  mealType: MealType;
  description: string;
  quantity: QuantityLevel;
  appetite: AppetiteLevel;
  hydrationMl?: number;
  notes?: string;
  entryTime?: string;
}
export interface NutritionEntryResponse {
  id: number;
  mealType: MealType;
  description: string;
  quantity: QuantityLevel;
  appetite: AppetiteLevel;
  hydrationMl?: number;
  notes?: string;
  entryTime?: string;
}

// ─── Medication Intake (linked to Medication entity) ─────────────────────────
export interface MedicationIntakeLogRequest {
  medicationId: number;
  takenAt?: string;
  status: IntakeStatus;
  notes?: string;
}
export interface MedicationIntakeLogResponse {
  id: number;
  medicationId: number;
  medicationName: string;
  dosage?: string;
  frequency?: string;
  takenAt?: string;
  status: IntakeStatus;
  notes?: string;
}

// ─── Activity ────────────────────────────────────────────────────────────────
export interface ActivityEntryRequest {
  activityType: ActivityType;
  description: string;
  durationMinutes?: number;
  intensity?: IntensityLevel;
  notes?: string;
  startTime?: string;
}
export interface ActivityEntryResponse {
  id: number;
  activityType: ActivityType;
  description: string;
  durationMinutes?: number;
  intensity?: IntensityLevel;
  notes?: string;
  startTime?: string;
}

// ─── Incident ────────────────────────────────────────────────────────────────
export interface IncidentEntryRequest {
  incidentType: IncidentType;
  description: string;
  severity: SeverityLevel;
  location?: string;
  actionTaken?: string;
  injuryDetails?: string;
  occurredAt?: string;
}
export interface IncidentEntryResponse {
  id: number;
  incidentType: IncidentType;
  description: string;
  severity: SeverityLevel;
  location?: string;
  actionTaken?: string;
  injuryDetails?: string;
  occurredAt?: string;
}

// ─── Daily Log ────────────────────────────────────────────────────────────────
export interface DailyLogResponse {
  id: number;
  patientKeycloakId: string;
  logDate: string;
  globalNotes?: string;
  createdAt: string;
  updatedAt: string;
  nutritionEntries: NutritionEntryResponse[];
  medicationIntakes: MedicationIntakeLogResponse[];
  activityEntries: ActivityEntryResponse[];
  incidentEntries: IncidentEntryResponse[];
}
