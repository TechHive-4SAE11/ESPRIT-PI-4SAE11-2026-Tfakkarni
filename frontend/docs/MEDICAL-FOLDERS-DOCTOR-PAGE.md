# Medical Folders — Doctor Page Frontend

Overview of **http://localhost:4200/doctor?page=medical-folders**: how the URL, dashboard, and medical folder list work.

---

## URL and routing

| What | Value |
|------|--------|
| **Route** | `path: 'doctor'` → `DoctorDashboardComponent` |
| **Query param** | `page=medical-folders` |
| **Internal page name** | `'Medical Folders'` (display title in sidebar) |

- **`/doctor`** loads the doctor dashboard (auth guard: role `doctor`).
- **`?page=medical-folders`** is read in `restoreCurrentPage()` and mapped to `currentPage = 'Medical Folders'`, so the dashboard shows the Medical Folders view instead of Home.

Mapping (in `doctor-dashboard.component.ts`):

- **URL → state:** `fromQueryPage('medical-folders')` → `'Medical Folders'`
- **State → URL:** `toQueryPage('Medical Folders')` → `'medical-folders'`

So opening `/doctor?page=medical-folders` or clicking the sidebar **Medical Folders** both show the same view and keep the URL in sync.

---

## Dashboard layout and “Medical Folders” view

- **Component:** `DoctorDashboardComponent` (`pages/doctor-dashboard/doctor-dashboard.component.ts`).
- **Layout:** `app-dashboard-layout` with `basePath="/doctor"` and `menuGroups` (sidebar: Home, Patients, Medical Folders, Patient Progress, Prescriptions, Care Plans).
- **Content:** A single `<div>` with `@switch (currentPage())`. For `currentPage() === 'Medical Folders'` it renders the **Medical Folders** block.

That block includes:

1. **Search (doctor-scoped)**  
   - Input bound to `searchInput()`, `onSearchInput(value)`.  
   - Uses `medicalFolderService.getAll()` (all folders), then filters by `patientId` or `doctorId` containing the term; shows up to 5 results in a dropdown.  
   - Choosing a result calls `openFolderFromSearch(folder)`: sets `searchSelectedFolderId(folder.id)`, clears search, and `setPage('Medical Folders')` so the list view opens with that folder’s detail pre-selected.

2. **Medical folder list**  
   - `<app-medical-folder-list>` with:
     - `[initialFolderId]="searchSelectedFolderId()"` — if user picked a folder from search, that folder’s detail is opened on load.
     - `[doctorId]="doctorIdString()"` — doctor’s **Keycloak ID** (from `currentDoctor()?.keycloakId`).
     - `[doctor]="currentDoctor()"` — full doctor `UserInfo` (for form display).
     - `(detailClosed)="searchSelectedFolderId.set(null)"` — when detail is closed, clear the pre-selected id.

---

## Medical folder list component

**Component:** `MedicalFolderListComponent`  
**Path:** `pages/medical-folders/medical-folder-list/`

### Inputs

| Input | Type | Purpose |
|-------|------|--------|
| `initialFolderId` | `number \| null` | If set, on init loads that folder and opens detail view. |
| `doctorId` | `string \| null` | **Keycloak ID** of the doctor. When set, list is loaded via `getByDoctorId(doctorId)` instead of `getAll()`. |
| `doctor` | `UserInfo \| null` | Passed into the create/edit form so “Doctor Name” can be shown. |

### Data loading

- **On init:** `loadPatientNames()` (patients by role for name resolution), `loadFolders()`, and if `initialFolderId` is set, `getById(initialFolderId)` then open detail.
- **loadFolders():**
  - If `doctorId()` is set: `medicalFolderService.getByDoctorId(doctorId)` → **GET** `environment.apiBaseUrl/api/medical-folders/doctor/{doctorId}`.
  - Else: `medicalFolderService.getAll()` → **GET** `.../api/medical-folders`.
- **Backend:** Both endpoints are implemented in **medical-service** (`MedicalFolderController`). `doctorId` is the **Keycloak subject** (same as in JWT and in folder’s `doctorId`).

### List UI (template)

- **Search:** Local filter on already-loaded folders by `patientId` or patient name (from `patientNameMap`).
- **Create:** Button opens a dialog with `MedicalFolderFormComponent`; on submit calls `medicalFolderService.create({ patientId })` (doctor comes from JWT on backend).
- **Table:** Columns Patient ID, Patient Name, Created, Updated, Actions (View, Edit, Delete).
- **Pagination:** Client-side, `PAGE_SIZE = 10`.
- **View:** Sets `folderToView` and `showDetail` → shows `app-medical-folder-detail`.
- **Edit:** Dialog with `MedicalFolderFormComponent` and `folder` set; submit calls `medicalFolderService.update(folder.id, data)`.
- **Delete:** Confirm dialog then `medicalFolderService.delete(folder.id)`.

Patient names come from `UserApiService.getUsersByRole('patient')` and are stored in `patientNameMap` (key = `keycloakId`); `getPatientName(patientId)` uses it, so `patientId` in the folder is expected to be the patient’s **Keycloak ID**.

---

## Medical folder form (create / edit)

**Component:** `MedicalFolderFormComponent`  
**Path:** `pages/medical-folders/medical-folder-form/`

- **Doctor:** Read-only, from `@Input() doctor` → `doctorName()` (e.g. “FirstName LastName”). No `doctorId` sent in create payload; backend takes it from JWT.
- **Patient:** Searchable input; options from `UserApiService.getUsersByRole('patient')`; selection sets `form.patchValue({ patientId: patient.keycloakId })`. So **patientId** is always a **Keycloak ID**.
- **Create:** Submits `{ patientId: string }` only.
- **Edit:** Same form; can send `patientId` and/or `doctorId` in update (backend accepts both in update DTO).

---

## API service (frontend)

**Service:** `MedicalFolderService`  
**File:** `core/services/medical-folder.service.ts`

- **Base URL:** `environment.apiBaseUrl + '/api/medical-folders'` (e.g. `http://localhost:9090/api/medical-folders` via gateway).
- **Methods used on doctor medical-folders page:**
  - `getAll()` → GET `/api/medical-folders`
  - `getByDoctorId(doctorId)` → GET `/api/medical-folders/doctor/{doctorId}`
  - `getById(id)` → GET `/api/medical-folders/{id}`
  - `create(data)` → POST `/api/medical-folders` (body: `{ patientId }`)
  - `update(id, data)` → PUT `/api/medical-folders/{id}`
  - `delete(id)` → DELETE `/api/medical-folders/{id}`

**Note:** `getMedicalFoldersByPatient(patientId)` → GET `/api/medical-folders/patient/{patientId}` is used by Prescription Management and Care Plan Management. The **medical-service** backend does **not** expose this endpoint (only `doctor/{doctorId}` exists). So those two features will get 404 unless you add a patient-by-id endpoint or proxy elsewhere.

---

## Flow summary for `/doctor?page=medical-folders`

1. User opens `/doctor?page=medical-folders` (or clicks Medical Folders in sidebar).
2. `DoctorDashboardComponent` sets `currentPage` to `'Medical Folders'` and renders the search + `<app-medical-folder-list>`.
3. Dashboard provides `doctorIdString()` (Keycloak ID) and `currentDoctor()` after loading doctor from `getUserByKeycloakId(keycloakId)`.
4. `MedicalFolderListComponent` calls `getByDoctorId(doctorId)` so the list shows only that doctor’s folders.
5. User can search (by patient/doctor id in all folders), create (patient = Keycloak ID, doctor from JWT), view, edit, delete. All API calls go to the gateway then to **medical-service**.

---

## Files reference

| Area | File(s) |
|------|--------|
| Route | `app.routes.ts` — `path: 'doctor'` |
| Dashboard + page switch | `pages/doctor-dashboard/doctor-dashboard.component.ts` |
| List + load by doctor | `pages/medical-folders/medical-folder-list/medical-folder-list.component.ts` (.html) |
| Create/Edit form | `pages/medical-folders/medical-folder-form/medical-folder-form.component.ts` |
| Detail view | `pages/medical-folders/medical-folder-detail/medical-folder-detail.component.ts` |
| API | `core/services/medical-folder.service.ts` |
| Doctor info | `core/services/user-api.service.ts` (UserInfo), auth `getKeycloakId()` |

This describes only the medical folder frontend for the doctor page; no user-service or other backend logic is included.
