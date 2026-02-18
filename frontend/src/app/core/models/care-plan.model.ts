export enum CareActivityType {
  PHYSICAL_ACTIVITY = 'PHYSICAL_ACTIVITY',
  NUTRITION_PLAN = 'NUTRITION_PLAN',
}

export interface CareActivityRequestDTO {
  activityName: string;
  activityType: CareActivityType;
  description: string;
  frequency: string;
  duration: string;
  completionStatus: string;
}

export interface CareActivityResponseDTO {
  id: number;
  activityName: string;
  activityType: CareActivityType;
  description: string;
  frequency: string;
  duration: string;
  completionStatus: string;
  createdAt: string;
}

export interface CarePlanRequestDTO {
  sessionId: number;
  activities: CareActivityRequestDTO[];
}

export interface CarePlanResponseDTO {
  id: number;
  sessionId: number;
  doctorId: string; // The backend database ID of the doctor
  activities: CareActivityResponseDTO[];
  createdAt: string;
  updatedAt: string;
}
