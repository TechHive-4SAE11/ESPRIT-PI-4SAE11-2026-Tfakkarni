export type AlzheimerStage = 'UNKNOWN' | 'LOW_RISK' | 'EARLY' | 'MODERATE' | 'SEVERE';
export type ScoreTrend = 'IMPROVING' | 'STABLE' | 'DECLINING' | 'INSUFFICIENT_DATA';
export type IotLevel = 'DISABLED' | 'BASIC' | 'FULL' | 'EMERGENCY';
export type GameComplexity = 'STANDARD' | 'SIMPLIFIED' | 'MINIMAL';
export type MonitoringLevel = 'OPTIONAL' | 'RECOMMENDED' | 'REQUIRED';
export type EscalationLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type UiMode = 'STANDARD' | 'SIMPLIFIED' | 'ELDERLY_MAX';

export interface CognitiveDomainDTO {
  domainName: string;
  correctCount: number;
  incorrectCount: number;
  accuracyPct: number;
  trend: ScoreTrend;
}

export interface PatientScoreResponse {
  patientKeycloakId: string;
  cognitiveScore: number;
  dailyFunctioningScore: number;
  medicalStabilityScore: number;
  iotRiskScore: number;
  engagementScore: number;
  overallScore: number;
  stage: AlzheimerStage;
  scoreTrend: ScoreTrend;
  computedAt: string;
  cognitiveDomains: CognitiveDomainDTO[];
}

export interface ScoreHistoryEntry {
  id: number;
  patientKeycloakId: string;
  cognitiveScore: number;
  dailyFunctioningScore: number;
  medicalStabilityScore: number;
  iotRiskScore: number;
  engagementScore: number;
  overallScore: number;
  stage: AlzheimerStage;
  recordedAt: string;
}

export interface FeatureGateResponse {
  patientKeycloakId: string;
  stage: AlzheimerStage;
  iotEnabled: boolean;
  iotLevel: IotLevel;
  gameComplexity: GameComplexity;
  monitoringLevel: MonitoringLevel;
  notificationEscalation: EscalationLevel;
  uiMode: UiMode;
  safeZoneRequired: boolean;
  meetingSuggestedFrequencyDays: number;
  computedAt: string;
}

export interface DoctorEffectivenessResponse {
  doctorKeycloakId: string;
  doctorName: string;
  patientCount: number;
  stabilizationRate: number;
  declineRate: number;
  avgComplianceImprovement: number;
  sessionFrequency: number;
  coachingCompletionRate: number;
  appointmentShowRate: number;
  riskFlags: string[];
  computedAt: string;
}

export interface PlatformOverviewResponse {
  totalPatients: number;
  totalDoctors: number;
  stageDistribution: Record<AlzheimerStage, number>;
  platformAvgScore: number;
  cognitiveDomainWeakness: Record<string, number>;
  totalGameAttempts: number;
  totalIncidents: number;
  redFlagDoctorCount: number;
}

export interface BatchJobResult {
  jobName: string;
  status: string;
  processedCount: number;
  errorCount: number;
  startedAt: string;
  completedAt: string;
  durationMs: number;
  message: string;
}

export interface CorrelationPoint {
  date: string;
  avgGameScore: number;
  medicationAdherence: number;
  incidentCount: number;
}

export interface PrescriptionImpactPoint {
  date: string;
  avgScore?: number;
  medAdherence: number;
  hasNewPrescription: boolean;
}

export interface PrescriptionMarker {
  date: string;
  description: string;
  prescriptionId: number;
}

export interface PrescriptionImpactResponse {
  patientKeycloakId: string;
  impactTimeline: PrescriptionImpactPoint[];
  markers: PrescriptionMarker[];
}

export interface CorrelationStatsResponse {
  patientKeycloakId: string;
  correlationTimeline: CorrelationPoint[];
  keyInsight: string;
  adherenceCorrelation: number;
}

// ── Doctor–Patient Matching ──

export interface DoctorMatchResponse {
  doctorKeycloakId: string;
  doctorName: string;
  matchScore: number;
  averageRating: number;
  totalRatings: number;
  stabilizationRate: number;
  declineRate: number;
  appointmentShowRate: number;
  currentPatientCount: number;
  hasRiskFlags: boolean;
}

export interface SeverePatientResponse {
  patientKeycloakId: string;
  patientName: string;
  stage: AlzheimerStage;
  overallScore: number;
  cognitiveScore: number;
  currentDoctorKeycloakId: string | null;
  currentDoctorName: string;
  recommendedDoctorKeycloakId: string | null;
  recommendedDoctorName: string | null;
  recommendedDoctorMatchScore: number;
}
