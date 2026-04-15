// ── AI Assistant Models ──────────────────────────────────────────────

// ─── Quiz Generation ───
export interface QuizGenerateRequest {
  topic: string;
  numberOfQuestions: number;
  difficultyLevel?: number | null; // 1-3, optional: AI decides if not provided
  caregiverId?: number; // optional, internal usage
}

export interface GeneratedAnswer {
  id?: number;
  text: string;
  isCorrect: boolean;
  explanation: string;
  questionId?: number;
}

export interface GeneratedQuestion {
  id?: number;
  text: string;
  difficultyLevel: number;
  mediaAttachment?: string;
  quizId?: number;
  answers: GeneratedAnswer[];
}

export interface GeneratedQuiz {
  id: number;
  topic: string;
  totalScore: number;
  dateTaken: string;
  caregiverId: number;
  levelReached: number;
  questions: GeneratedQuestion[];
}

// ─── Equipment Recommendation ───
export interface EquipmentRecommendRequest {
  patientId: number;
  condition: string;
  severity: string;
}

export interface EquipmentRecommendation {
  equipmentId: number;
  equipmentName: string;
  category: string;
  justification: string;
  relevanceScore: number;
  usageInstructions: string;
}

export interface EquipmentRecommendResponse {
  patientId: number;
  condition: string;
  severity: string;
  recommendations: EquipmentRecommendation[];
  generalAdvice: string;
}

// ─── Voice Assistant ───
export interface VoiceCommandRequest {
  command: string;
  userId: number;
  patientName?: string;
  sessionId?: string;
}

export interface VoiceCommandResponse {
  type: 'ACTION' | 'INFO' | 'ERROR' | 'QUIZ_START';
  message: string;
  data?: any;
  sessionId?: string;
}

// ─── Video Generation ───
export interface VideoGenerateRequest {
  patientId: number;
  topic: string;
  memoryType: 'PHOTO' | 'STORY' | 'EXERCISE';
  duration: number;
  patientName?: string;
  patientAge?: number;
  interests?: string;
}

export interface StoryboardScene {
  sceneNumber: number;
  description: string;
  narration: string;
  durationSeconds: number;
  visualPrompt: string;
}

export interface VideoGenerateResponse {
  videoId: number;
  patientId: number;
  topic: string;
  memoryType: string;
  duration: number;
  status: 'GENERATING' | 'READY' | 'FAILED' | 'SCRIPT_ONLY';
  videoUrl?: string;
  thumbnailUrl?: string;
  script: string;
  storyboard: StoryboardScene[];
  createdAt: string;
}

export interface VideoFeedbackRequest {
  patientId: number;
  rating: number;
  reaction: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  comments?: string;
  engagedFully?: boolean;
}
