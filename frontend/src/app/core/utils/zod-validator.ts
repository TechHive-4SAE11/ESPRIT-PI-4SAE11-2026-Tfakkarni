import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ZodSchema } from 'zod';

export function createZodValidator(schema: ZodSchema): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    try {
      const result = schema.safeParse(control.value);

      if (result.success) {
        return null;
      } else {
        const error = result.error;
        const errors: ValidationErrors = {};

        // Defensive access to issues array to prevent crashes
        // @ts-ignore
        const issues = error.errors || error.issues || [];

        if (Array.isArray(issues)) {
          issues.forEach((err: any) => {
            // Use path as key if present, otherwise generic 'zodError' or 'required'
            const path = err.path && Array.isArray(err.path) ? err.path.join('.') : null;
            const key = path || 'required';
            errors[key] = err.message;
            errors['invalid'] = true;
          });
        } else {
          // Fallback if issues structure is unexpected
          errors['invalid'] = true;
        }

        return errors;
      }
    } catch (e) {
      console.error('Zod validator crashed', e);
      return { invalid: true };
    }
  };
}
