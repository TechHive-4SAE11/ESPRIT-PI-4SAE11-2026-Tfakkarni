# 🧪 Prescription API - Testing Guide

## Quick Start

### 1. Start the Services

Make sure the following services are running:
- **Discovery Service** (Eureka) - Port 8761
- **Tracking Service** - Port 8083

### 2. Access Swagger UI

Open your browser and go to:
```
http://localhost:8083/swagger-ui.html
```

## ✅ Complete Testing Workflow

### Step 1: Create a Medical Folder

**Endpoint:** `POST /api/medical-folders`

**Request:**
```json
{
  "idPatient": "P12345",
  "idDoctor": "D67890",
  "diagnosis": "Memory issues and cognitive decline",
  "notes": "Patient needs regular monitoring"
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "idPatient": "P12345",
  "idDoctor": "D67890",
  "diagnosis": "Memory issues and cognitive decline",
  "notes": "Patient needs regular monitoring",
  "sessions": [],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

**📝 Note:** Save the `id` field (e.g., `1`) - you'll need it for the next step.

---

### Step 2: Create a Session

**Endpoint:** `POST /api/sessions`

**Request:**
```json
{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION"
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": [],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

**📝 Note:** Save the `id` field (e.g., `1`) - you'll need it for creating prescriptions.

---

### Step 3: Create a Prescription ✨

**Endpoint:** `POST /api/prescriptions`

**Request (Single Medication):**
```json
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Donepezil",
      "dosage": "10mg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take in the evening. May cause nausea initially."
    }
  ]
}
```

**Request (Multiple Medications):**
```json
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Donepezil",
      "dosage": "10mg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take in the evening"
    },
    {
      "medicationName": "Vitamin B12",
      "dosage": "1000mcg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take with breakfast"
    },
    {
      "medicationName": "Omega-3",
      "dosage": "1000mg",
      "frequency": "Twice daily",
      "duration": "Ongoing",
      "instructions": "Take with meals"
    }
  ]
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "sessionId": 1,
  "medications": [
    {
      "id": 1,
      "medicationName": "Donepezil",
      "dosage": "10mg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take in the evening",
      "createdAt": "2026-02-15T20:00:00"
    }
  ],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

---

## 🔍 Retrieve Prescriptions

### Get All Prescriptions for a Patient

**Endpoint:** `GET /api/prescriptions/patient/{idPatient}`

**Example:**
```
GET http://localhost:8083/api/prescriptions/patient/P12345
```

### Get All Prescriptions for a Session

**Endpoint:** `GET /api/prescriptions/session/{sessionId}`

**Example:**
```
GET http://localhost:8083/api/prescriptions/session/1
```

### Get All Prescriptions

**Endpoint:** `GET /api/prescriptions`

---

## ❌ Common Errors and Solutions

### Error 1: Session ID is required

**Response:**
```json
{
  "timestamp": "2026-02-15T20:10:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Session ID is required",
  "path": "/api/prescriptions"
}
```

**Solution:** Make sure your request includes `"sessionId"` field with a valid session ID.

---

### Error 2: Session not found

**Response:**
```json
{
  "timestamp": "2026-02-15T20:10:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Session not found with id: 999",
  "path": "/api/prescriptions"
}
```

**Solution:** 
1. Verify the session exists: `GET /api/sessions/999`
2. If it doesn't exist, create a session first (see Step 2)
3. Use the correct session ID from the response

---

### Error 3: Invalid JSON format

**Response:**
```json
{
  "timestamp": "2026-02-15T20:10:00",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON parse error: ...",
  "path": "/api/prescriptions"
}
```

**Solution:** 
- Make sure `medications` is an **array** (use `[` and `]`)
- Check for missing commas between fields
- Ensure all strings are in double quotes
- Validate JSON format using a JSON validator

---

## 🛠️ Using Swagger UI

### Benefits of Swagger UI:
1. **Interactive Testing** - Try API calls directly from the browser
2. **Schema Validation** - See the expected request/response format
3. **Error Details** - Get immediate feedback on validation errors
4. **Examples** - View sample requests and responses

### How to Use:
1. Go to `http://localhost:8083/swagger-ui.html`
2. Find the **Prescriptions** section
3. Click on `POST /api/prescriptions`
4. Click "Try it out"
5. Fill in the request body
6. Click "Execute"
7. See the response below

---

## 📋 Using cURL

### Create Prescription:
```bash
curl -X POST http://localhost:8083/api/prescriptions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "medications": [
      {
        "medicationName": "Donepezil",
        "dosage": "10mg",
        "frequency": "Once daily",
        "duration": "3 months",
        "instructions": "Take in the evening"
      }
    ]
  }'
```

### Get Prescriptions by Patient:
```bash
curl http://localhost:8083/api/prescriptions/patient/P12345
```

---

## 🔧 Debugging Tips

### 1. Check Server Logs
The service now logs detailed information:
```
INFO: Creating prescription for session ID: 1
INFO: Added 2 medications to prescription
INFO: Successfully created prescription with ID: 1
```

Or in case of errors:
```
ERROR: Session ID is null in request
ERROR: Error creating prescription: Session not found with id: 999
```

### 2. Verify Database
Check if the session exists:
```sql
SELECT * FROM sessions WHERE id = 1;
```

### 3. Test Step by Step
- Test medical folder creation first
- Then test session creation
- Finally test prescription creation

### 4. Use Valid Data
- Session ID must exist
- Medications array can be empty but must be an array
- All medication fields are strings

---

## 📊 Example: Full Workflow

```json
// 1. Create Medical Folder
POST /api/medical-folders
{
  "idPatient": "P12345",
  "idDoctor": "D67890",
  "diagnosis": "Alzheimer's disease",
  "notes": "Early stage diagnosis"
}
// Response: { "id": 1, ... }

// 2. Create Session
POST /api/sessions
{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation",
  "sessionType": "CONSULTATION"
}
// Response: { "id": 1, ... }

// 3. Create Prescription
POST /api/prescriptions
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Donepezil",
      "dosage": "10mg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take in the evening"
    }
  ]
}
// Response: { "id": 1, "sessionId": 1, "medications": [...], ... }
```

---

## ✅ Success Checklist

- [ ] Discovery service is running on port 8761
- [ ] Tracking service is running on port 8083
- [ ] Can access Swagger UI at http://localhost:8083/swagger-ui.html
- [ ] Created a medical folder successfully
- [ ] Created a session successfully
- [ ] Created a prescription with the correct format
- [ ] Received detailed error messages when something goes wrong
- [ ] Can retrieve prescriptions by patient ID

---

## 🎉 You're All Set!

The prescription API is now:
- ✅ Properly documented with Swagger
- ✅ Returning detailed error messages
- ✅ Logging all operations for debugging
- ✅ Using the correct request format with medications array

If you still encounter issues, check the server logs for detailed error messages!

