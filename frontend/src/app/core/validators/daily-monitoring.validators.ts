/**
 * Zod schemas for Suivi Quotidien form validation
 * Usage: parse form data before saving → throws ZodError with field-level messages
 */

import { z } from 'zod';

// ─── Zod custom helpers ────────────────────────────────────────────────────────

/** Accepts only a positive integer string (no letters, no zero, no negatives) */
const positiveIntStr = (fieldLabel: string, max: number) =>
  z.string()
    .min(1, `${fieldLabel} est obligatoire`)
    .regex(/^\d+$/, `${fieldLabel} doit contenir uniquement des chiffres`)
    .refine(v => parseInt(v, 10) > 0,  { message: `${fieldLabel} doit être supérieure à 0` })
    .refine(v => parseInt(v, 10) <= max, { message: `${fieldLabel} trop élevée (max ${max})` });

/** Required non-blank text */
const requiredText = (fieldLabel: string) =>
  z.string()
    .min(1, `${fieldLabel} est obligatoire`)
    .refine(v => v.trim().length > 0, { message: `${fieldLabel} est obligatoire` });

/** Required time string HH:mm */
const requiredTime = (fieldLabel: string) =>
  z.string().min(1, `${fieldLabel} est obligatoire`);

// ─── Nutrition schema ──────────────────────────────────────────────────────────

export const NutritionSchema = z.object({
  mealType:     z.enum(['BREAKFAST','LUNCH','DINNER','SNACK'], {
    message: 'Type de repas invalide'
  }),
  entryTime:    requiredTime('Heure du repas'),
  description:  requiredText('Aliments consommés'),
  quantity:     z.enum(['COMPLET','DEMI','PEU','RIEN'], {
    message: 'Quantité invalide'
  }),
  appetite:     z.enum(['BON','MOYEN','FAIBLE']).optional(),
  hydrationRaw: positiveIntStr('Hydratation (ml)', 10000),
  notes:        z.string().optional(),
});

export type NutritionFormData = z.infer<typeof NutritionSchema>;

// ─── Medication schema ─────────────────────────────────────────────────────────

export const MedicationSchema = z.object({
  medicationId: z.number({
    message: 'Veuillez sélectionner un médicament',
  }).min(1, 'Veuillez sélectionner un médicament'),
  takenAt: requiredTime('Heure de prise'),
  status:  z.enum(['PRIS','OUBLIE','REFUSE','EN_RETARD'], {
    message: 'Statut invalide'
  }),
  notes: z.string().optional(),
});

export type MedicationFormData = z.infer<typeof MedicationSchema>;

// ─── Activity schema ───────────────────────────────────────────────────────────

export const ActivitySchema = z.object({
  activityType: z.enum(['PHYSIQUE','COGNITIVE','SOCIALE','HYGIENE','PROMENADE','AUTRE'], {
    message: "Type d'activité invalide"
  }),
  description:  requiredText('Description'),
  durationRaw:  positiveIntStr('Durée (minutes)', 1440),
  startTime:    requiredTime('Heure de début'),
  intensity:    z.enum(['FAIBLE','MODERE','ELEVE']).optional(),
  notes:        z.string().optional(),
});

export type ActivityFormData = z.infer<typeof ActivitySchema>;

// ─── Incident schema ───────────────────────────────────────────────────────────

export const IncidentSchema = z.object({
  incidentType:  z.enum(['CHUTE','CONFUSION','AGITATION','DEAMBULATION','CRISE','AUTRE'], {
    message: "Type d'incident invalide"
  }),
  description:   requiredText('Description'),
  severity:      z.enum(['LEGER','MODERE','GRAVE'], {
    message: 'Gravité invalide'
  }),
  occurredAt:    requiredTime('Heure'),
  location:      requiredText('Lieu'),
  injuryDetails: z.string().optional(),
  actionTaken:   z.string().optional(),
});

export type IncidentFormData = z.infer<typeof IncidentSchema>;

// ─── Validation result type ────────────────────────────────────────────────────

export interface ValidationErrors {
  [field: string]: string;
}

/**
 * Validate a form data object against the given Zod schema.
 * Returns { valid: true } if valid, or { valid: false, errors } if invalid.
 */
export function validateForm<T extends z.ZodTypeAny>(
  schema: T,
  data: unknown
): { valid: true } | { valid: false; errors: ValidationErrors } {
  const result = schema.safeParse(data);
  if (result.success) return { valid: true };

  const errors: ValidationErrors = {};
  for (const issue of result.error.issues) {
    const key = issue.path.join('.');
    if (!errors[key]) errors[key] = issue.message;
  }
  return { valid: false, errors };
}
