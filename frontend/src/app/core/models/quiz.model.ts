// Quiz Models
export interface QuizDTO {
  id?: number;
  topic: string;
  totalScore?: number;
  /** Highest difficulty level the patient reached in this attempt (1=Easy, 2=Medium, 3=Hard) */
  levelReached?: number;
  dateTaken?: string;
  caregiverId: number;
  questions?: QuestionDTO[];
}

export interface QuestionDTO {
  id?: number;
  text: string;
  difficultyLevel: number;
  mediaAttachment?: string;
  quizId: number;
  answers?: AnswerDTO[];
}

export interface AnswerDTO {
  id?: number;
  text: string;
  isCorrect: boolean;
  explanation?: string;
  questionId: number;
}

export interface ValidationRequestDTO {
  questionId: number;
  answerId: number;
}

export interface ValidationResponseDTO {
  valid: boolean;
  answerId: number;
  questionId: number;
  explanation?: string;
}

export interface SubmissionRequestDTO {
  quizId: number;
  questionId: number;
  answerId: number;
}

export interface SubmissionResponseDTO {
  correct: boolean;
  quizId: number;
  questionId: number;
  answerId: number;
  explanation?: string;
  feedback?: string;
}

export interface CountResponseDTO {
  count: number;
  questionId?: number;
}

export interface BooleanResponseDTO {
  value: boolean;
  message: string;
}
