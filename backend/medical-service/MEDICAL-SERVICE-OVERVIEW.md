# Medical Service — Overview

This document summarizes the **medical-service** (port **18086**) so you can work on it without touching the user-service.

---

## What It Does

The medical service manages **patient medical records** for the Tfakkarni platform. It has three main domains:

| Domain | Purpose |
|--------|--------|
| **Medical folders** | Links a **patient** to a **doctor**. One folder = one patient–doctor pair. Created by the doctor (doctor ID from JWT). |
| **Medical history** | Per-folder: allergies, conditions, surgeries (text). |
| **Diagnostics** | Per-folder: disease name, stage, comorbidities, diagnosis date. |

---

## Architecture (in the service)

```
MedicalFolder (1) ──┬── (N) MedicalHistory  (allergies, conditions, surgeries)
                    └── (N) Diagnostics      (disease, stage, comorbidities, date)
```

- **MedicalFolder**: `id`, `patientId`, `doctorId`, `createdAt`, `updatedAt`.
- **MedicalHistory**: belongs to one `MedicalFolder`; fields: allergies, conditions, surgeries.
- **Diagnostics**: belongs to one `MedicalFolder`; fields: diseaseName, stage, comorbidities, diagnosisDate.

All stored in **PostgreSQL (Neon)**. Same DB config as in `application.yml` (medical-service has its own URL/password).

---

## API Endpoints (as implemented in medical-service)

Base URL through gateway: **http://localhost:9090** (then paths below).

### Medical folders (`/api/medical-folders`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/medical-folders` | List all medical folders |
| GET | `/api/medical-folders/doctor/{doctorId}` | List folders for a doctor |
| POST | `/api/medical-folders` | Create folder (body: `patientId`; doctor from JWT) |
| GET | `/api/medical-folders/{id}` | Get folder by ID |
| PUT | `/api/medical-folders/{id}` | Full update |
| PATCH | `/api/medical-folders/{id}` | Partial update |
| DELETE | `/api/medical-folders/{id}` | Delete folder |

**POST create:** Gateway may send `patientId` in query or body; medical-service also supports reading doctor from **JWT** (Bearer token).

### Medical history (`/api/medical-history`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/medical-history` | Create (body: medicalFolderId, allergies, conditions, surgeries) |
| GET | `/api/medical-history/{id}` | Get by ID |
| GET | `/api/medical-history?medicalFolderId=` | List by medical folder |
| PUT | `/api/medical-history/{id}` | Full update |
| PATCH | `/api/medical-history/{id}` | Partial update |
| DELETE | `/api/medical-history/{id}` | Delete |

### Diagnostics (`/api/diagnostics`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/diagnostics` | Create (body: medicalFolderId, diseaseName, stage, comorbidities, diagnosisDate) |
| GET | `/api/diagnostics/{id}` | Get by ID |
| GET | `/api/diagnostics?medicalFolderId=` | List by medical folder |
| PUT | `/api/diagnostics/{id}` | Full update |
| PATCH | `/api/diagnostics/{id}` | Partial update |
| DELETE | `/api/diagnostics/{id}` | Delete |

---

## Gateway Routing (important)

| Frontend path | Gateway route | Target service |
|---------------|--------------|----------------|
| `/api/medical-folders` | ✅ Configured | medical-service |
| `/api/medical-folders/**` | ✅ Configured | medical-service |
| `/api/medical/**` | ✅ Configured | medical-service |
| `/api/medical-history` | ❌ **Not configured** | — |
| `/api/diagnostics` | ❌ **Not configured** | — |

- **Medical folders** are reachable at `http://localhost:9090/api/medical-folders` and `.../api/medical-folders/**`.
- **Medical history** and **diagnostics** are exposed by medical-service at `/api/medical-history` and `/api/diagnostics`, but the gateway only has `/api/medical/**`. So:
  - `/api/medical-history` and `/api/diagnostics` do **not** match `/api/medical/**` and are **not** routed to medical-service.

To use medical-history and diagnostics from the frontend, add in **api-gateway** `application.yml`:

```yaml
- id: medical-history
  uri: lb://medical-service
  predicates:
    - Path=/api/medical-history/**
- id: diagnostics
  uri: lb://medical-service
  predicates:
    - Path=/api/diagnostics/**
```

---

## Project structure (medical-service only)

```
medical-service/
├── controller/
│   ├── MedicalFolderController.java   → /api/medical-folders
│   ├── MedicalHistoryController.java  → /api/medical-history
│   ├── DiagnosticsController.java     → /api/diagnostics
│   └── HealthController.java           → /api/health
├── service/ (+ impl)
│   ├── MedicalFolderService
│   ├── MedicalHistoryService
│   └── DiagnosticsService
├── repository/
│   ├── MedicalFolderRepository
│   ├── MedicalHistoryRepository
│   └── DiagnosticsRepository
├── entity/
│   ├── MedicalFolder
│   ├── MedicalHistory
│   └── Diagnostics
├── dto/ (Create*, Update*, *Response)
├── mapper/, exception/, converter/
└── application.yml
```

---

## Configuration

- **Port:** 18086  
- **Eureka:** registers as `medical-service`  
- **Database:** Neon PostgreSQL (see `application.yml`; use your own Neon URL/user/password if different from user-service).  
- **Auth:** MedicalFolderController reads doctor from JWT (Bearer) for `POST /api/medical-folders`.

---

## Quick reference

- **Create a medical folder:** POST to gateway `/api/medical-folders` with body `{ "patientId": "..." }` and `Authorization: Bearer <token>` (doctor = token subject).  
- **List folders for doctor:** GET `/api/medical-folders/doctor/{doctorId}`.  
- **Medical history / diagnostics:** Implemented in medical-service; add gateway routes above to call them from the frontend.

No user-service code is involved in these flows; this overview focuses only on the medical service.
