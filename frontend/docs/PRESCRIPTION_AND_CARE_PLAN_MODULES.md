# Prescription and Care Plan Modules Documentation

This document provides a comprehensive overview of the **Prescription Management** and **Care Plan Management** modules implemented in the Doctor Dashboard.

## 1. Technology Stack & Dependencies

The modules are built using the following core technologies and libraries:

### Core Framework
- **Angular v18+**: The application uses the latest Angular features including **Standalone Components** and **Signals** for reactive state management.
- **Reactive Forms (`@angular/forms`)**: Used for building complex, dynamic forms with validation logic.
- **RxJS**: Used for handling asynchronous data streams (HTTP requests) and side effects.

### Validation
- **Zod (`zod`)**: A TypeScript-first schema declaration and validation library. It is used to define strict validation schemas for forms, ensuring type safety and robust error handling.
- **Custom Zod Validator**: A utility (`createZodValidator`) bridges Zod schemas with Angular's `AbstractControl` validators.

### Styling & UI
- **Tailwind CSS**: Utility-first CSS framework used for all styling (layout, spacing, typography, colors).
- **Lucide Angular (`lucide-angular`)**: Provides the icon set used throughout the UI (e.g., pill, activity, trash-2, edit icons).
- **Zard UI Components**: A custom internal UI library providing standardized components:
  - `z-button`: Styled buttons with variants (primary, outline, destructive, ghost).
  - `z-card`: Container components for layout structure.
  - `z-icon`: Wrapper for Lucide icons.
- **Class Variance Authority (`class-variance-authority`)**: Used internally by Zard components to manage component variants and styles.
- **clsx** & **tailwind-merge**: Utilities for conditionally constructing and merging Tailwind class strings.

---

## 2. Module Architecture

Both modules follow a similar architectural pattern:

### Smart Component Pattern
The logic is encapsulated within a single "Smart" container component for each feature:
- `PrescriptionManagementComponent`
- `CarePlanManagementComponent`

These components are responsible for:
1.  **State Management**: Using Angular Signals (`signal`, `computed`) to track lists, loading states, and UI interactions.
2.  **Data Fetching**: interacting with backend services (`PrescriptionService`, `CarePlanService`, `SessionService`).
3.  **Form Management**: creating and updating the Reactive Forms.

### Inline Dialog Implementation
Instead of using a global dialog service for complex forms, these modules use an **Inline Dialog Strategy**. 
- The dialog HTML structure is embedded directly in the component's template (`.html`) inside an `@if` block.
- This allows direct access to the component's scope (signals, form controls) without complex data passing.
- **Styling**: Uses fixed positioning (`fixed inset-0`) with a blurred backdrop (`backdrop-blur-sm bg-black/50`) to create a modal overlay effect.

---

## 3. Prescription Management Module

**File:** `prescription-management.component.ts`

### Key Features
- **List View**: Displays patient prescriptions with medication summaries.
- **Create/Edit Form**: A dynamic form allowing doctors to prescribe multiple medications in a single session.
- **FormArray for Medications**: Uses Angular `FormArray` to dynamically add or remove medication fields.

### Data Flow
1.  **Initialization**: Loads existing prescriptions for the patient.
2.  **Session Loading**: Fetches available consultation sessions (Doctor-Patient interactions) to link the prescription to.
3.  **Form Submission**:
    -   Validates the entire form using Zod schemas.
    -   Constructs a DTO (`PrescriptionRequestDTO`).
    -   Calls `createPrescription` or `updatePrescription` on the service.

### Validation Rules (Zod)
- **Session**: Required.
- **Medication List**: Must have at least one medication.
- **Medication Fields**: Name, Dosage, Frequency, Duration are required strings.

---

## 4. Care Plan Management Module

**File:** `care-plan-management.component.ts`

### Key Features
- **Activity Tracking**: Manages differents types of activities:
    -   **Physical Activity**: Requires frequency and duration.
    -   **Nutrition Plan**: Simpler description-based tracking.
- **Conditional Validation**: The form dynamically updates validators based on the `activityType` selected.
    -   *Example*: If "Nutrition Plan" is selected, Frequency and Duration fields are optional/hidden.

### Data Flow
1.  **Computed Signals**: `physicalActivities` and `nutritionActivities` are computed derived signals that filter the raw list for the view.
2.  **Dynamic Validators**: Implementation of `updateActivityValidators` which subscribes to `valueChanges` on the `activityType` control to toggle required validators on/off in real-time.

---

## 5. Implementation Details to Note

### Angular Signals
Instead of using `BehaviorSubject`, this codebase uses Angular Signals for simpler reactivity:
```typescript
// Defining state
prescriptions = signal<PrescriptionResponseDTO[]>([]);

// Updating state
this.prescriptions.set(data);

// Reading state in template
@for (p of prescriptions(); track p.id)
```

### Zod Integration
The `createZodValidator` utility is key to linking Zod with Angular Forms:
```typescript
// In component
sessionId: [null, createZodValidator(this.prescriptionSchema.shape.sessionId)]
```
This enables the form control to use the Zod schema logic (including custom error messages) directly within the Angular validation lifecycle.

### Session Management
Both modules depend on `SessionService` and `MedicalFolderService`. A critical piece of logic is filtering available sessions:
- Create loops fetch all sessions for the patient/doctor pair.
- Filters show only sessions **without** an existing prescription/care plan, preventing duplicates for the same consultation.
