# Quick CRUD Reference - Copy & Paste into Postman

## BASE URL
```
http://localhost:18086
```

---

## MEDICAL FOLDER

### CREATE
**POST** `http://localhost:18086/api/medical-folders`
```json
{
  "patientId": "patient123",
  "doctorId": "doctor456"
}
```

### READ
**GET** `http://localhost:18086/api/medical-folders/1`

### UPDATE (FULL)
**PUT** `http://localhost:18086/api/medical-folders/1`
```json
{
  "patientId": "patient456",
  "doctorId": "doctor789"
}
```

### UPDATE (PARTIAL)
**PATCH** `http://localhost:18086/api/medical-folders/1`
```json
{
  "doctorId": "doctor_new"
}
```

### DELETE
**DELETE** `http://localhost:18086/api/medical-folders/1`

---

## DIAGNOSTICS

### CREATE
**POST** `http://localhost:18086/api/diagnostics`
```json
{
  "medicalFolderId": 1,
  "diseaseName": "Alzheimer",
  "stage": "Early",
  "comorbidities": "Diabetes, Hypertension",
  "diagnosisDate": "2026-02-18T10:00:00"
}
```

### READ BY ID
**GET** `http://localhost:18086/api/diagnostics/1`

### READ BY FOLDER
**GET** `http://localhost:18086/api/diagnostics?medicalFolderId=1`

### UPDATE (FULL)
**PUT** `http://localhost:18086/api/diagnostics/1`
```json
{
  "diseaseName": "Alzheimer Modified",
  "stage": "Middle",
  "comorbidities": "Diabetes, Hypertension, Heart Disease",
  "diagnosisDate": "2026-02-17T10:00:00"
}
```

### UPDATE (PARTIAL)
**PATCH** `http://localhost:18086/api/diagnostics/1`
```json
{
  "stage": "Advanced"
}
```

### DELETE
**DELETE** `http://localhost:18086/api/diagnostics/1`

---

## MEDICAL HISTORY

### CREATE
**POST** `http://localhost:18086/api/medical-history`
```json
{
  "medicalFolderId": 1,
  "allergies": "Penicillin, Sulfa drugs",
  "conditions": "Hypertension, Diabetes Type 2",
  "surgeries": "Appendectomy 2015, Knee surgery 2018"
}
```

### READ BY ID
**GET** `http://localhost:18086/api/medical-history/1`

### READ BY FOLDER
**GET** `http://localhost:18086/api/medical-history?medicalFolderId=1`

### UPDATE (FULL)
**PUT** `http://localhost:18086/api/medical-history/1`
```json
{
  "allergies": "Penicillin, Sulfa drugs, Aspirin",
  "conditions": "Hypertension, Diabetes Type 2, Heart Disease",
  "surgeries": "Appendectomy 2015, Knee surgery 2018, Cataract surgery 2023"
}
```

### UPDATE (PARTIAL)
**PATCH** `http://localhost:18086/api/medical-history/1`
```json
{
  "allergies": "Penicillin, Sulfa drugs, Aspirin, NSAIDs"
}
```

### DELETE
**DELETE** `http://localhost:18086/api/medical-history/1`

---

## QUICK TEST ORDER

1. **POST** /api/medical-folders → Get ID
2. **POST** /api/diagnostics (use folder ID)
3. **POST** /api/medical-history (use folder ID)
4. **GET** /api/diagnostics/1
5. **GET** /api/medical-history/1
6. **PATCH** /api/diagnostics/1
7. **PATCH** /api/medical-history/1
8. **DELETE** /api/diagnostics/1
9. **DELETE** /api/medical-history/1
10. **DELETE** /api/medical-folders/1

✅ Backend running on port 18086
