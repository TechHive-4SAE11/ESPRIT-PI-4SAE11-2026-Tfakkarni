# Complete CRUD Operations - All Three Entities

> **Base URL:** `http://localhost:18086`

---

## 1. MEDICAL FOLDER CRUD

### 1.1 CREATE Medical Folder
```
POST /api/medical-folders
Content-Type: application/json

{
  "patientId": "patient123",
  "doctorId": "doctor456"
}
```

**Response: 201 Created**
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

### 1.2 READ Medical Folder by ID
```
GET /api/medical-folders/1
```

**Response: 200 OK**
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

### 1.3 UPDATE Medical Folder (Full)
```
PUT /api/medical-folders/1
Content-Type: application/json

{
  "patientId": "patient456",
  "doctorId": "doctor789"
}
```

**Response: 200 OK**
```json
{
  "id": 1,
  "patientId": "patient456",
  "doctorId": "doctor789",
  "createdAt": "2026-02-19T00:17:12",
  "updatedAt": "2026-02-19T00:18:00"
}
```

---

### 1.4 UPDATE Medical Folder (Partial - PATCH)

**Request:**
```
PATCH /api/medical-folders/1
Content-Type: application/json

{
  "doctorId": "doctor_new"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "patientId": "patient456",
  "doctorId": "doctor_new",
  "createdAt": "2026-02-19T00:17:12",
  "updatedAt": "2026-02-19T00:18:10"
}
```

---

### 1.5 DELETE Medical Folder
```
DELETE /api/medical-folders/1
```

**Response: 204 No Content**
```
(No body)
```

---

## 2. DIAGNOSTICS CRUD

### 2.1 CREATE Diagnostics
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

**Response: 201 Created**
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

### 2.2 READ Diagnostics by ID
```
GET /api/diagnostics/1
```

**Response: 200 OK**
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

### 2.3 READ Diagnostics by Medical Folder
```
GET /api/diagnostics?medicalFolderId=1
```

**Response: 200 OK**
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

### 2.4 UPDATE Diagnostics (Full)
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

**Response: 200 OK**
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

### 2.5 UPDATE Diagnostics (Partial - PATCH)
```
PATCH /api/diagnostics/1
Content-Type: application/json

{
  "stage": "Advanced"
}
```

**Response: 200 OK**
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

### 2.6 DELETE Diagnostics
```
DELETE /api/diagnostics/1
```

**Response: 204 No Content**
```
(No body)
```

---

## 3. MEDICAL HISTORY CRUD

### 3.1 CREATE Medical History
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

**Response: 201 Created**
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

### 3.2 READ Medical History by ID
```
GET /api/medical-history/1
```

**Response: 200 OK**
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

### 3.3 READ Medical History by Medical Folder
```
GET /api/medical-history?medicalFolderId=1
```

**Response: 200 OK**
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

### 3.4 UPDATE Medical History (Full)
```
PUT /api/medical-history/1
Content-Type: application/json

{
  "allergies": "Penicillin, Sulfa drugs, Aspirin",
  "conditions": "Hypertension, Diabetes Type 2, Heart Disease",
  "surgeries": "Appendectomy 2015, Knee surgery 2018, Cataract surgery 2023"
}
```

**Response: 200 OK**
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

### 3.5 UPDATE Medical History (Partial - PATCH)
```
PATCH /api/medical-history/1
Content-Type: application/json

{
  "allergies": "Penicillin, Sulfa drugs, Aspirin, NSAIDs"
}
```

**Response: 200 OK**
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

### 3.6 DELETE Medical History
```
DELETE /api/medical-history/1
```

**Response: 204 No Content**
```
(No body)
```

---

## Summary Table

| Entity | Operation | Method | Endpoint | Status |
|---|---|---|---|---|
| **Medical Folder** | Create | POST | /api/medical-folders | 201 |
| | Read (by ID) | GET | /api/medical-folders/{id} | 200 |
| | Update (Full) | PUT | /api/medical-folders/{id} | 200 |
| | Update (Partial) | PATCH | /api/medical-folders/{id} | 200 |
| | Delete | DELETE | /api/medical-folders/{id} | 204 |
| **Diagnostics** | Create | POST | /api/diagnostics | 201 |
| | Read (by ID) | GET | /api/diagnostics/{id} | 200 |
| | Read (by Folder) | GET | /api/diagnostics?medicalFolderId=X | 200 |
| | Update (Full) | PUT | /api/diagnostics/{id} | 200 |
| | Update (Partial) | PATCH | /api/diagnostics/{id} | 200 |
| | Delete | DELETE | /api/diagnostics/{id} | 204 |
| **Medical History** | Create | POST | /api/medical-history | 201 |
| | Read (by ID) | GET | /api/medical-history/{id} | 200 |
| | Read (by Folder) | GET | /api/medical-history?medicalFolderId=X | 200 |
| | Update (Full) | PUT | /api/medical-history/{id} | 200 |
| | Update (Partial) | PATCH | /api/medical-history/{id} | 200 |
| | Delete | DELETE | /api/medical-history/{id} | 204 |

---

## Postman Import Tips

1. **Create a new collection** for each entity
2. **Save requests** as you test them
3. **Use variables** for IDs: `{{medicalFolderId}}`, `{{diagnosticsId}}`, `{{medicalHistoryId}}`
4. **Set pre-request scripts** to create prerequisites:
   - Create Medical Folder first
   - Use returned ID for Diagnostics/Medical History

---

## Quick Test Flow

1. Create Medical Folder → Save ID from response
2. Create Diagnostics using folder ID
3. Create Medical History using folder ID
4. Get individual records by ID (all three)
5. Query by medicalFolderId (Diagnostics & Medical History)
6. Update Medical Folder (PUT and PATCH)
7. Update Diagnostics (PUT and PATCH)
8. Update Medical History (PUT and PATCH)
9. Delete Diagnostics
10. Delete Medical History
11. Delete Medical Folder

---

## Error Responses

### 404 Not Found
```json
{
  "status": 404,
  "message": "Diagnostics not found with id: 999"
}
```

### 400 Bad Request
```json
{
  "status": 400,
  "message": "Validation failed - required fields missing"
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Medical folder not found with id: 999"
}
```

All endpoints are tested and ready! ✅

