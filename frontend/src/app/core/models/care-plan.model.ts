export interface CareActivityRequestDTO {
  activityName: string;
  description: string;
  frequency: string;
  duration: string;
  completionStatus: string;
}

export interface CareActivityResponseDTO {
  id: number;
  activityName: string;
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
  activities: CareActivityResponseDTO[];
  createdAt: string;
  updatedAt: string;
}
