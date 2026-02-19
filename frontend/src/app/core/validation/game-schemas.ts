import { z } from 'zod';

// ─── Common Rules ──────────────────────────────────────────

const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

/** Estimate decoded size from a base64 string length */
function base64DecodedSize(base64: string): number {
  const padding = (base64.match(/=+$/) || [''])[0].length;
  return Math.floor((base64.length * 3) / 4) - padding;
}

// ─── Tag Schemas ──────────────────────────────────────────

export const tagNameSchema = z
  .string()
  .min(3, 'Tag name must be at least 3 characters')
  .max(10, 'Tag name must be at most 10 characters')
  .regex(/^[a-zA-Z0-9]+$/, 'Tag name must contain only letters and numbers');

export const tagSchema = z.object({
  name: tagNameSchema,
  color: z.string().min(1, 'Color is required'),
});

// ─── Game Title & Description ──────────────────────────────

export const gameTitleSchema = z
  .string()
  .min(3, 'Title must be at least 3 characters')
  .max(20, 'Title must be at most 20 characters')
  .regex(/^[a-zA-Z0-9 ]+$/, 'Title can only contain letters, numbers, and spaces');

export const gameDescriptionSchema = z
  .string()
  .max(100, 'Description must be at most 100 characters')
  .optional()
  .or(z.literal(''));

// ─── Custom Game Schemas ──────────────────────────────────

export const customGameSchema = z.object({
  title: gameTitleSchema,
  description: gameDescriptionSchema,
});

// ─── Data Point Schemas ───────────────────────────────────

export const photoNameSchema = z
  .string()
  .min(1, 'Photo name is required')
  .max(20, 'Photo name must be at most 20 characters')
  .regex(/^[a-zA-Z0-9 ]+$/, 'Name can only contain letters, numbers, and spaces');

export const photoSchema = z.object({
  name: photoNameSchema,
  imageBase64: z
    .string()
    .min(1, 'Image is required')
    .refine(
      (val) => base64DecodedSize(val) <= MAX_IMAGE_SIZE,
      'Image must be 5MB or less'
    ),
  contentType: z.string().min(1, 'Content type is required'),
});

export const placeNameSchema = z
  .string()
  .min(1, 'Place name is required')
  .max(20, 'Place name must be at most 20 characters')
  .regex(/^[a-zA-Z0-9 ]+$/, 'Name can only contain letters, numbers, and spaces');

export const placeSchema = z.object({
  name: placeNameSchema,
  latitude: z.number({ error: 'Latitude is required' }),
  longitude: z.number({ error: 'Longitude is required' }),
  hint: z.string().max(100, 'Hint must be at most 100 characters').optional().or(z.literal('')),
});

export const movieMemorySchema = z.object({
  originalTitle: z.string().min(1, 'Movie title is required'),
  correctAnswer: z
    .string()
    .min(1, 'Character name is required')
    .max(20, 'Character name must be at most 20 characters'),
});

export const questionMemorySchema = z.object({
  questionText: z
    .string()
    .min(1, 'Question is required')
    .max(500, 'Question must be at most 500 characters'),
  correctAnswer: z
    .string()
    .min(1, 'Answer is required')
    .max(500, 'Answer must be at most 500 characters'),
});

// ─── Movie Game Schemas ───────────────────────────────────

export const movieGameSchema = z.object({
  title: gameTitleSchema,
  description: gameDescriptionSchema,
});

export const movieItemSchema = z.object({
  correctAnswer: z
    .string()
    .min(1, 'Character name is required')
    .max(20, 'Character name must be at most 20 characters'),
});

// ─── Personal Question Game Schemas ───────────────────────

export const personalQuestionGameSchema = z.object({
  title: gameTitleSchema,
  description: gameDescriptionSchema,
});

export const questionItemSchema = z.object({
  questionText: z
    .string()
    .min(1, 'Question is required')
    .max(500, 'Question must be at most 500 characters'),
  correctAnswer: z
    .string()
    .min(1, 'Answer is required')
    .max(500, 'Answer must be at most 500 characters'),
});

// ─── MiniGame (Image Game) Schemas ─────────────────────────

export const miniGameSchema = z.object({
  title: gameTitleSchema,
  description: gameDescriptionSchema,
});

export const gameImageSchema = z.object({
  name: z
    .string()
    .min(1, 'Image name is required')
    .max(20, 'Image name must be at most 20 characters')
    .regex(/^[a-zA-Z0-9 ]+$/, 'Name can only contain letters, numbers, and spaces'),
  imageBase64: z
    .string()
    .min(1, 'Image is required')
    .refine(
      (val) => base64DecodedSize(val) <= MAX_IMAGE_SIZE,
      'Image must be 5MB or less'
    ),
  contentType: z.string().min(1, 'Content type is required'),
});

// ─── Helper ──────────────────────────────────────────────

/** Extract the first error message from a Zod result */
export function getFirstError(result: { success: boolean; error?: any }): string | null {
  if (result.success) return null;
  return result.error?.errors?.[0]?.message ?? result.error?.issues?.[0]?.message ?? 'Validation failed';
}

/** Get field-specific errors from a Zod result */
export function getFieldErrors(result: { success: boolean; error?: any }): Record<string, string> {
  if (result.success) return {};
  const errors: Record<string, string> = {};
  const issues = result.error?.errors ?? result.error?.issues ?? [];
  for (const err of issues) {
    const field = err.path?.join('.') ?? '';
    if (field && !errors[field]) {
      errors[field] = err.message;
    }
  }
  return errors;
}
