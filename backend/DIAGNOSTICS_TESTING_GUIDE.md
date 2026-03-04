# Medical Service - Diagnostics & Medical History API Testing Guide

## Overview
This guide provides complete Postman test cases for the Diagnostics and Medical History endpoints. All endpoints are associated with a Medical Folder ID.

## Prerequisites
- Medical Service running on `http://localhost:18086`
- A Medical Folder created (use ID = 1 for these examples)
- Postman or similar API testing tool

## Base URL
```
http://localhost:18086
```

---

## 1. Create Medical Folder (Prerequisite)

### Request
```
POST /api/medical-folders
Content-Type: application/json

{
  "patientId": "patient123",
  "doctorId": "doctor456"
}
```

### Response (201 Created)
```json
{
  "id": 1,
  "patientId": "patient123",
  "doctorId": "doctor456",
  "createdAt": "2026-02-19T00:17:12",
  "updatedAt": "2026-02-19T00:17:12"
}
```

---

## 2. Diagnostics Endpoints

### 2.1 Create Diagnostics

**Request:**
```
POST /api/diagnostics
Content-Type: application/json

{
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer",
  "stage": "Early",
  "comorbidities": "Diabetes, Hypertension",
  "diagnosisDate": "2026-02-18T10:00:00"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer",
  "stage": "Early",
  "comorbidities": "Diabetes, Hypertension",
  "diagnosisDate": "2026-02-18T10:00:00",
  "createdAt": "2026-02-19T00:17:18",
  "updatedAt": "2026-02-19T00:17:18"
}
```

---

### 2.2 Get Diagnostics by ID

**Request:**
```
GET /api/diagnostics/1
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer",
  "stage": "Early",
  "comorbidities": "Diabetes, Hypertension",
  "diagnosisDate": "2026-02-18T10:00:00",
  "createdAt": "2026-02-19T00:17:18",
  "updatedAt": "2026-02-19T00:17:18"
}
```

---

### 2.3 Get Diagnostics by Medical Folder

**Request:**
```
GET /api/diagnostics?medicalFolderId=1
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "medicalFolderId": 1,
    "diseaseName": "Alzheimer",
    "stage": "Early",
    "comorbidities": "Diabetes, Hypertension",
    "diagnosisDate": "2026-02-18T10:00:00",
    "createdAt": "2026-02-19T00:17:18",
    "updatedAt": "2026-02-19T00:17:18"
  }
]
```

---

### 2.4 Update Diagnostics (Full PUT)

**Request:**
```
PUT /api/diagnostics/1
Content-Type: application/json

{
  "diseaseName": "Alzheimer Modified",
  "stage": "Middle",
  "comorbidities": "Diabetes, Hypertension, Heart Disease",
  "diagnosisDate": "2026-02-17T10:00:00"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer Modified",
  "stage": "Middle",
  "comorbidities": "Diabetes, Hypertension, Heart Disease",
  "diagnosisDate": "2026-02-17T10:00:00",
  "createdAt": "2026-02-19T00:17:18",
  "updatedAt": "2026-02-19T00:17:30"
}
```

---

### 2.5 Partial Update Diagnostics (PATCH)

**Request:**
```
PATCH /api/diagnostics/1
Content-Type: application/json

{
  "stage": "Advanced"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer Modified",
  "stage": "Advanced",
  "comorbidities": "Diabetes, Hypertension, Heart Disease",
  "diagnosisDate": "2026-02-17T10:00:00",
  "createdAt": "2026-02-19T00:17:18",
  "updatedAt": "2026-02-19T00:17:35"
}
```

---

### 2.6 Delete Diagnostics

**Request:**
```
DELETE /api/diagnostics/1
```

**Response (204 No Content)**
- No body returned

---

## 3. Medical History Endpoints

### 3.1 Create Medical History

**Request:**
```
POST /api/medical-history
Content-Type: application/json

{
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs",
  "conditions": "Hypertension, Diabetes Type 2",
  "surgeries": "Appendectomy 2015, Knee surgery 2018"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs",
  "conditions": "Hypertension, Diabetes Type 2",
  "surgeries": "Appendectomy 2015, Knee surgery 2018",
  "createdAt": "2026-02-19T00:17:32",
  "updatedAt": "2026-02-19T00:17:32"
}
```

---

### 3.2 Get Medical History by ID

**Request:**
```
GET /api/medical-history/1
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs",
  "conditions": "Hypertension, Diabetes Type 2",
  "surgeries": "Appendectomy 2015, Knee surgery 2018",
  "createdAt": "2026-02-19T00:17:32",
  "updatedAt": "2026-02-19T00:17:32"
}
```

---

### 3.3 Get Medical History by Medical Folder

**Request:**
```
GET /api/medical-history?medicalFolderId=1
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "medicalFolderId": 1,
    "allergies": "Penicillin, Sulfa drugs",
    "conditions": "Hypertension, Diabetes Type 2",
    "surgeries": "Appendectomy 2015, Knee surgery 2018",
    "createdAt": "2026-02-19T00:17:32",
    "updatedAt": "2026-02-19T00:17:32"
  }
]
```

---

### 3.4 Update Medical History (Full PUT)

**Request:**
```
PUT /api/medical-history/1
Content-Type: application/json

{
  "allergies": "Penicillin, Sulfa drugs, Aspirin",
  "conditions": "Hypertension, Diabetes Type 2, Heart Disease",
  "surgeries": "Appendectomy 2015, Knee surgery 2018, Cataract surgery 2023"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs, Aspirin",
  "conditions": "Hypertension, Diabetes Type 2, Heart Disease",
  "surgeries": "Appendectomy 2015, Knee surgery 2018, Cataract surgery 2023",
  "createdAt": "2026-02-19T00:17:32",
  "updatedAt": "2026-02-19T00:17:40"
}
```

---

### 3.5 Partial Update Medical History (PATCH)

**Request:**
```
PATCH /api/medical-history/1
Content-Type: application/json

{
  "allergies": "Penicillin, Sulfa drugs, Aspirin, NSAIDs"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs, Aspirin, NSAIDs",
  "conditions": "Hypertension, Diabetes Type 2, Heart Disease",
  "surgeries": "Appendectomy 2015, Knee surgery 2018, Cataract surgery 2023",
  "createdAt": "2026-02-19T00:17:32",
  "updatedAt": "2026-02-19T00:17:45"
}
```

---

### 3.6 Delete Medical History

**Request:**
```
DELETE /api/medical-history/1
```

**Response (204 No Content)**
- No body returned

---

## Error Responses

### 404 Not Found
**When requesting non-existent resource:**
```json
{
  "status": 404,
  "message": "Diagnostics not found with id: 999"
}
```

### 400 Bad Request
**When required fields are missing:**
```json
{
  "status": 400,
  "message": "Validation failed"
}
```

### 500 Internal Server Error
**When medical folder doesn't exist:**
```json
{
  "status": 500,
  "message": "Medical folder not found with id: 999"
}
```

---

## Summary of All Endpoints

| HTTP Method | Endpoint | Purpose | Status Code |
|---|---|---|---|
| POST | /api/diagnostics | Create diagnostics | 201 |
| GET | /api/diagnostics/{id} | Get diagnostics by ID | 200 |
| GET | /api/diagnostics?medicalFolderId=X | List diagnostics for folder | 200 |
| PUT | /api/diagnostics/{id} | Full update diagnostics | 200 |
| PATCH | /api/diagnostics/{id} | Partial update diagnostics | 200 |
| DELETE | /api/diagnostics/{id} | Delete diagnostics | 204 |
| POST | /api/medical-history | Create medical history | 201 |
| GET | /api/medical-history/{id} | Get medical history by ID | 200 |
| GET | /api/medical-history?medicalFolderId=X | List medical history for folder | 200 |
| PUT | /api/medical-history/{id} | Full update medical history | 200 |
| PATCH | /api/medical-history/{id} | Partial update medical history | 200 |
| DELETE | /api/medical-history/{id} | Delete medical history | 204 |

---

## Implementation Notes

✅ All endpoints tested and working
✅ Proper HTTP status codes (201 Created, 200 OK, 204 No Content)
✅ Full CRUD operations supported
✅ Timestamps auto-generated (createdAt, updatedAt)
✅ Validation on required fields (medicalFolderId, diseaseName for diagnostics)
✅ Error handling with appropriate exceptions
✅ Database persistence via PostgreSQL

