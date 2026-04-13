export interface Question {
  id: number;
  text: String;
}

export interface Questionnaire {
  id: number;
  title: string;
  questions: Question[];
}

export interface PatientAnswerDTO {
  questionId: number;
  answer: string;
}

export interface QuestionnaireSubmissionDTO {
  patientId: number;
  answers: PatientAnswerDTO[];
}
