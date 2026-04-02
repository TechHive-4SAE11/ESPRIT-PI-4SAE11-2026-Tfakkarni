# Medical Service - API Testing Guide

This document provides examples for testing all endpoints of the Medical Service.

## Base URL
```
http://localhost:18086
```

## 1. Create a Medical Session

### Request
```http
POST /api/sessions HTTP/1.1
Host: localhost:18086
Content-Type: application/json

{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Aspirin", "Lisinopril"]
}
```

### Response (201 Created)
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Aspirin", "Lisinopril"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

---

## 2. Get a Single Medical Session

### Request
```http
GET /api/sessions/1 HTTP/1.1
Host: localhost:18086
```

### Response (200 OK)
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Aspirin", "Lisinopril"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

---

## 3. Get Sessions by Medical Folder ID (Non-Paginated)

### Request
```http
GET /api/sessions?medicalFolderId=1 HTTP/1.1
Host: localhost:18086
```

### Response (200 OK)
```json
[
  {
    "id": 1,
    "medicalFolderId": 1,
    "sessionDate": "2026-02-15T10:00:00",
    "duration": 60,
    "notes": "Initial consultation",
    "sessionType": "CONSULTATION",
    "prescriptions": ["Aspirin"],
    "createdAt": "2026-02-15T20:00:00",
    "updatedAt": "2026-02-15T20:00:00"
  },
  {
    "id": 2,
    "medicalFolderId": 1,
    "sessionDate": "2026-02-22T14:00:00",
    "duration": 45,
    "notes": "Follow-up appointment",
    "sessionType": "FOLLOW_UP",
    "prescriptions": ["Lisinopril"],
    "createdAt": "2026-02-22T20:00:00",
    "updatedAt": "2026-02-22T20:00:00"
  }
]
```

---

## 4. Get Sessions by Medical Folder ID (Paginated)

### Request
```http
GET /api/sessions?medicalFolderId=1&page=0&size=10 HTTP/1.1
Host: localhost:18086
```

### Response (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "medicalFolderId": 1,
      "sessionDate": "2026-02-15T10:00:00",
      "duration": 60,
      "notes": "Initial consultation",
      "sessionType": "CONSULTATION",
      "prescriptions": ["Aspirin"],
      "createdAt": "2026-02-15T20:00:00",
      "updatedAt": "2026-02-15T20:00:00"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "first": true,
  "numberOfElements": 1,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

---

## 5. Full Update Medical Session (PUT)

### Request
```http
PUT /api/sessions/1 HTTP/1.1
Host: localhost:18086
Content-Type: application/json

{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-16T11:00:00",
  "duration": 90,
  "notes": "Extended consultation with additional tests",
  "sessionType": "FOLLOW_UP",
  "prescriptions": ["Aspirin", "Lisinopril", "Metformin"]
}
```

### Response (200 OK)
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-16T11:00:00",
  "duration": 90,
  "notes": "Extended consultation with additional tests",
  "sessionType": "FOLLOW_UP",
  "prescriptions": ["Aspirin", "Lisinopril", "Metformin"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-16T15:30:00"
}
```

---

## 6. Partial Update Medical Session (PATCH)

### Request
```http
PATCH /api/sessions/1 HTTP/1.1
Host: localhost:18086
Content-Type: application/json

{
  "duration": 75,
  "notes": "Notes updated during therapy session"
}
```

### Response (200 OK)
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-16T11:00:00",
  "duration": 75,
  "notes": "Notes updated during therapy session",
  "sessionType": "FOLLOW_UP",
  "prescriptions": ["Aspirin", "Lisinopril", "Metformin"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-16T16:00:00"
}
```

---

## 7. Delete Medical Session

### Request
```http
DELETE /api/sessions/1 HTTP/1.1
Host: localhost:18086
```

### Response (204 No Content)
*(Empty body)*

---

## 8. Health Check

### Request
```http
GET /api/health HTTP/1.1
Host: localhost:18086
```

### Response (200 OK)
```json
{
  "status": "UP",
  "service": "medical-service",
  "message": "Medical service is running"
}
```

---

## 9. Actuator Health

### Request
```http
GET /actuator/health HTTP/1.1
Host: localhost:18086
```

### Response (200 OK)
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1099511627776,
        "free": 849911627776,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

---

## Error Examples

### Example 1: Not Found (404)

**Request**
```http
GET /api/sessions/999 HTTP/1.1
Host: localhost:18086
```

**Response (404 Not Found)**
```json
{
  "timestamp": "2026-02-15T20:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Medical session not found with ID: 999",
  "path": "/api/sessions/999",
  "validationErrors": null
}
```

### Example 2: Validation Error (400)

**Request**
```http
POST /api/sessions HTTP/1.1
Host: localhost:18086
Content-Type: application/json

{
  "medicalFolderId": -1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 0,
  "sessionType": "CONSULTATION"
}
```

**Response (400 Bad Request)**
```json
{
  "timestamp": "2026-02-15T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/sessions",
  "validationErrors": [
    {
      "field": "duration",
      "message": "Duration must be at least 1 minute"
    }
  ]
}
```

---

## cURL Examples

### Create Session
```bash
curl -X POST http://localhost:18086/api/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "medicalFolderId": 1,
    "sessionDate": "2026-02-15T10:00:00",
    "duration": 60,
    "notes": "Initial consultation",
    "sessionType": "CONSULTATION",
    "prescriptions": ["Aspirin"]
  }'
```

### Get Session
```bash
curl -X GET http://localhost:18086/api/sessions/1
```

### Update Session
```bash
curl -X PUT http://localhost:18086/api/sessions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "medicalFolderId": 1,
    "sessionDate": "2026-02-15T11:00:00",
    "duration": 90,
    "notes": "Updated notes",
    "sessionType": "FOLLOW_UP",
    "prescriptions": ["Aspirin", "Lisinopril"]
  }'
```

### Partial Update Session
```bash
curl -X PATCH http://localhost:18086/api/sessions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "duration": 75,
    "notes": "Just updated notes"
  }'
```

### Delete Session
```bash
curl -X DELETE http://localhost:18086/api/sessions/1
```

### Get Sessions by Medical Folder
```bash
curl -X GET "http://localhost:18086/api/sessions?medicalFolderId=1"
```

### Get Sessions with Pagination
```bash
curl -X GET "http://localhost:18086/api/sessions?medicalFolderId=1&page=0&size=10"
```

---

## Session Types

The `sessionType` field must be one of:
- `CONSULTATION` - Initial consultation with doctor
- `FOLLOW_UP` - Follow-up appointment
- `THERAPY` - Therapy session
- `EMERGENCY` - Emergency medical session

---

## Testing Tips

1. **Use Postman or Insomnia**: Import the cURL examples or manually create requests
2. **VS Code REST Client**: If using the REST Client extension, create an `.http` file with the above examples
3. **Check Logs**: Monitor service logs for DEBUG-level information about requests
4. **Validate JSONB**: Confirm prescriptions are stored correctly as JSON arrays in PostgreSQL

Example query to verify data in PostgreSQL:
```sql
SELECT id, medical_folder_id, session_type, prescriptions FROM medical_session;
```

---

## Testing Flow Example

1. Create a session
   ```
   POST /api/sessions
   ```
2. Verify it was created (get returns 200)
   ```
   GET /api/sessions/1
   ```
3. Retrieve all sessions for the folder
   ```
   GET /api/sessions?medicalFolderId=1
   ```
4. Update the session with new information
   ```
   PUT /api/sessions/1
   ```
5. Partially update just the notes
   ```
   PATCH /api/sessions/1
   ```
6. Delete the session
   ```
   DELETE /api/sessions/1
   ```
7. Verify it's gone (GET returns 404)
   ```
   GET /api/sessions/1
   ```

---

## Notes

- All timestamps are in ISO 8601 format (`yyyy-MM-dd'T'HH:mm:ss`)
- Prescriptions are returned as an array of strings
- Pagination uses 0-based indexing (first page is 0)
- All endpoints return appropriate HTTP status codes
- The service automatically manages `createdAt` and `updatedAt` timestamps
