export interface Prediction {
  riskScore: number;
  riskLevel: string;
  factors: { [key: string]: any };
  recommendation: string;
}

export interface PatientRisk {
  appointmentId: number;
  patientId: string;
  title: string;
  date: string;
  time: string;
  doctorId: string;
  riskScore: number;
  riskLevel: string;
  recommendation: string;
}

export interface DashboardStats {
  totalAppointments: number;
  globalNoShowRate: number;
  monthlyNoShowRate: number;
  highRiskPatients: PatientRisk[];
  upcomingAppointments: PatientRisk[];
  cancellationsByDay: { [key: string]: number };
  noShowRateByDoctor: { [key: string]: number };
}
