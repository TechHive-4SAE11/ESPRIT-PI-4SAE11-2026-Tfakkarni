# Medication Status Management System

## Overview

Automated system for tracking medication lifecycle based on prescription dates and duration. Medications automatically transition between `ACTIVE`, `EXPIRED`, and `ONGOING` states using scheduled cron jobs.

---

## Medication Status Lifecycle

### Status Enum: `MedicationStatus`

```java
public enum MedicationStatus {
    ACTIVE,       // Currently valid prescription (today is between startDate and endDate)
    EXPIRED,      // Past endDate
    ONGOING,      // No end date (long-term medication)
    DISCONTINUED  // Manually stopped by doctor
}
```

### Status Flow

```
Prescription Created
       ↓
   [ACTIVE]
       ↓
   ┌───────────────┐
   │ Has endDate?  │
   └───────────────┘
      ↓         ↓
    YES        NO
      ↓         ↓
  Past end?  [ONGOING]
      ↓
   [EXPIRED]
```

---

## Architecture

### 1. Entity Changes: `Medication.java`

**New Fields:**
```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private MedicationStatus status = MedicationStatus.ACTIVE;

@Column(name = "start_date")
private LocalDate startDate;

@Column(name = "end_date")
private LocalDate endDate;

@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

**Auto-Initialization on Create:**
```java
@PrePersist
protected void initializeDates() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (updatedAt == null) updatedAt = LocalDateTime.now();
    if (status == null) status = MedicationStatus.ACTIVE;
    
    // Auto-calculate dates from prescription session
    if (startDate == null && prescription != null && prescription.getSession() != null) {
        startDate = prescription.getSession().getSessionDate().toLocalDate();
        if (duration != null) {
            endDate = DurationParser.calculateEndDate(startDate, duration);
        }
    }
}

@PreUpdate
protected void updateTimestamp() {
    updatedAt = LocalDateTime.now();
}
```

### 2. Duration Parser: `DurationParser.java`

**Supported Formats:**
- `"3 months"` / `"3 mois"`
- `"30 days"` / `"30 jours"`
- `"2 weeks"` / `"2 semaines"`
- `"1 year"` / `"1 année"`
- `"ongoing"` / `"permanent"` / `"à vie"` → NO end date

**Key Methods:**
```java
// Calculate end date from start + duration string
LocalDate calculateEndDate(LocalDate startDate, String duration)

// Check if duration is indefinite
boolean isOngoing(String duration)

// Determine status based on dates
MedicationStatus determineStatus(LocalDate startDate, LocalDate endDate, LocalDate today)
```

**Examples:**
```java
DurationParser.calculateEndDate(LocalDate.of(2024, 1, 15), "3 months")
// → 2024-04-15

DurationParser.isOngoing("permanent")
// → true

DurationParser.determineStatus(
    LocalDate.of(2024, 1, 1),  // start
    LocalDate.of(2024, 3, 1),  // end
    LocalDate.of(2024, 4, 1)   // today
)
// → EXPIRED
```

### 3. Scheduled Service: `MedicationStatusScheduler.java`

**Cron Jobs:**

| Job | Schedule | Description |
|-----|----------|-------------|
| `updateMedicationStatuses()` | `0 0 0 * * *` (Daily at midnight) | Primary status update job |
| `periodicStatusUpdate()` | `0 0 */6 * * *` (Every 6 hours) | Redundant safety check |

**Logic:**
1. Fetch all medications from database
2. Skip `DISCONTINUED` meds (manual status)
3. For each medication:
   - If missing `startDate`: calculate from `prescription.session.sessionDate`
   - If missing `endDate` & has `duration`: calculate using `DurationParser`
   - Determine new status: `DurationParser.determineStatus(startDate, endDate, today)`
   - If status changed: save & log change
4. Log summary statistics

**Example Log Output:**
```
Medication status update completed:
  Total processed: 42
  Active: 28
  Expired: 10
  Ongoing: 3
  Discontinued: 1
  Status changes: 5
```

### 4. Admin Controller: `MedicationStatusController.java`

**Endpoints:**

#### POST `/api/admin/medication-status/update`
Manually trigger status update (same logic as cron job)
```bash
curl -X POST http://localhost:9090/api/admin/medication-status/update
```

#### POST `/api/admin/medication-status/initialize`
Initialize dates for existing medications (migration tool)
```bash
curl -X POST http://localhost:9090/api/admin/medication-status/initialize
```

#### POST `/api/admin/medication-status/{id}/discontinue`
Mark medication as discontinued with reason
```bash
curl -X POST http://localhost:9090/api/admin/medication-status/123/discontinue \
  -H "Content-Type: application/json" \
  -d '{"reason": "Patient allergic reaction"}'
```

#### GET `/api/admin/medication-status/stats`
Get status distribution statistics
```json
{
  "totalMedications": 42,
  "activeCount": 28,
  "expiredCount": 10,
  "ongoingCount": 3,
  "discontinuedCount": 1
}
```

#### GET `/api/admin/medication-status/health`
System health check
```json
{
  "status": "healthy",
  "message": "Medication status scheduler is running",
  "schedulerEnabled": true
}
```

### 5. Service Integration: `PrescriptionService.java`

**Auto-initialization on Create/Update:**
```java
// Called when creating prescription
private void initializeMedicationDates(Prescription prescription) {
    LocalDate sessionDate = prescription.getSession().getSessionDate().toLocalDate();
    
    for (Medication medication : prescription.getMedications()) {
        // Set start date
        if (medication.getStartDate() == null) {
            medication.setStartDate(sessionDate);
        }
        
        // Calculate end date
        if (medication.getEndDate() == null && medication.getDuration() != null) {
            LocalDate endDate = DurationParser.calculateEndDate(sessionDate, medication.getDuration());
            medication.setEndDate(endDate);
            
            MedicationStatus status = DurationParser.determineStatus(
                medication.getStartDate(), 
                endDate, 
                LocalDate.now()
            );
            medication.setStatus(status);
        } else if (DurationParser.isOngoing(medication.getDuration())) {
            medication.setStatus(MedicationStatus.ONGOING);
        }
    }
}
```

### 6. Configuration: `application.yml`

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 2  # Thread pool for scheduled tasks
```

### 7. Scheduling Config: `SchedulingConfig.java`

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Empty - just enables @Scheduled annotations
}
```

---

## API Response Changes

### Updated DTO: `MedicationResponseDTO`

**New Fields:**
```java
MedicationStatus status;     // Current lifecycle status
LocalDate startDate;         // When prescription became active
LocalDate endDate;           // When prescription expires (null for ONGOING)
```

**Full Response:**
```json
{
  "id": 123,
  "medicationName": "Donepezil",
  "dosage": "10mg",
  "frequency": "Once daily",
  "duration": "3 months",
  "instructions": "Take with food",
  "status": "ACTIVE",
  "startDate": "2024-01-15",
  "endDate": "2024-04-15",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

## Doctor Management Endpoints

### New MedicationController (`/api/medications`)

Allows doctors to manually manage medication status.

#### PATCH `/api/medications/{medicationId}/status`
Update medication status (e.g., discontinue)

**Request Body:**
```json
{
  "status": "DISCONTINUED",
  "reason": "Patient experienced side effects"
}
```

**Response:**
```json
{
  "success": true,
  "medicationId": 123,
  "oldStatus": "ACTIVE",
  "newStatus": "DISCONTINUED",
  "endDate": "2024-02-22",
  "message": "Medication status updated successfully"
}
```

**Behavior:**
- Changes medication status
- If discontinuing: sets `endDate` to today and appends reason to instructions
- Updates `updatedAt` timestamp

#### GET `/api/medications/{medicationId}`
Get medication details

**Response:**
```json
{
  "id": 123,
  "medicationName": "Donepezil",
  "dosage": "10mg",
  "frequency": "Once daily",
  "duration": "3 months",
  "instructions": "Take with food\n\n[DISCONTINUED] Patient experienced side effects",
  "status": "DISCONTINUED",
  "startDate": "2024-01-15",
  "endDate": "2024-02-22",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-02-22T14:30:00"
}
```

---

## Testing Guide

### 1. Manual Testing via API

#### Create a Prescription
```bash
POST http://localhost:9090/api/medical/prescriptions
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Donepezil",
      "dosage": "10mg",
      "frequency": "Once daily",
      "duration": "3 months",
      "instructions": "Take with food"
    }
  ]
}
```

**Expected Result:**
- Medication created with `status: ACTIVE`
- `startDate` = session date
- `endDate` = startDate + 3 months

#### Verify Status Calculation
```bash
GET http://localhost:9090/api/medical/prescriptions/{id}
```

Check response includes:
```json
"medications": [
  {
    "status": "ACTIVE",
    "startDate": "2024-01-15",
    "endDate": "2024-04-15"
  }
]
```

#### Trigger Manual Update
```bash
POST http://localhost:9090/api/admin/medication-status/update
```

**Expected Logs:**
```
Starting medication status update job
Medication 123 status changed: ACTIVE → EXPIRED (Patient: 456, Duration: 3 months)
Medication status update completed:
  Total processed: 1
  Expired: 1
  Status changes: 1
```

#### Discontinue Medication
```bash
POST http://localhost:9090/api/admin/medication-status/123/discontinue
{
  "reason": "Patient experienced side effects"
}
```

**Result:**
- Status changes to `DISCONTINUED`
- End date set to today
- Reason appended to instructions

### 2. Test Cases

| Scenario | Duration | Expected Status | Notes |
|----------|----------|-----------------|-------|
| Fresh prescription | `"30 days"` | `ACTIVE` | Created today |
| Expired prescription | `"1 month"` | `EXPIRED` | Created 2 months ago |
| Long-term medication | `"ongoing"` | `ONGOING` | No end date |
| Discontinued by doctor | N/A | `DISCONTINUED` | Manual action |
| Duration format: French | `"3 mois"` | `ACTIVE` | Multi-language support |
| Duration format: weeks | `"2 weeks"` | `ACTIVE` | 14 days from start |

### 3. Duration Parsing Tests

```java
// Test various formats
DurationParser.calculateEndDate(start, "3 months")    // +3 months
DurationParser.calculateEndDate(start, "30 days")     // +30 days
DurationParser.calculateEndDate(start, "2 semaines")  // +14 days (French)
DurationParser.calculateEndDate(start, "1 year")      // +1 year
DurationParser.isOngoing("permanent")                 // true
DurationParser.isOngoing("ongoing")                   // true
DurationParser.isOngoing("à vie")                     // true (French)
```

---

## Monitoring & Logs

### Key Log Messages

**Successful Update:**
```
Starting medication status update job
Medication 123 status changed: ACTIVE → EXPIRED (Patient: 456, Duration: 3 months)
Medication status update completed: Total processed: 42, Status changes: 5
```

**No Changes:**
```
Starting medication status update job
Medication status update completed: Total processed: 42, Status changes: 0
```

**Discontinuation:**
```
Medication Donepezil discontinued: Patient experienced side effects
```

---

## Migration Steps (Existing Data)

If you have existing medications without `startDate` or `endDate`:

### Option 1: Automatic (Recommended)
```bash
POST http://localhost:9090/api/admin/medication-status/initialize
```
This will:
- Calculate `startDate` from `prescription.session.sessionDate`
- Calculate `endDate` from `startDate` + `duration`
- Set appropriate status

### Option 2: Manual SQL
```sql
-- Update existing medications with dates
UPDATE medications m
SET 
  start_date = (
    SELECT s.session_date::date 
    FROM prescriptions p 
    JOIN sessions s ON p.session_id = s.id 
    WHERE p.id = m.prescription_id
  ),
  status = 'ACTIVE'
WHERE start_date IS NULL;

-- Calculate end dates (PostgreSQL interval syntax)
UPDATE medications
SET end_date = start_date + (
  CASE 
    WHEN duration LIKE '%month%' THEN (regexp_replace(duration, '[^0-9]', '', 'g')::int || ' months')::interval
    WHEN duration LIKE '%day%' THEN (regexp_replace(duration, '[^0-9]', '', 'g')::int || ' days')::interval
    WHEN duration LIKE '%week%' THEN (regexp_replace(duration, '[^0-9]', '', 'g')::int || ' weeks')::interval
    WHEN duration LIKE '%year%' THEN (regexp_replace(duration, '[^0-9]', '', 'g')::int || ' years')::interval
  END
)
WHERE end_date IS NULL AND duration IS NOT NULL;
```

---

## Troubleshooting

### Issue: Cron jobs not running
**Check:**
1. `@EnableScheduling` present in `SchedulingConfig.java`
2. Service annotated with `@Service`
3. Methods have `@Scheduled` annotation
4. Application started successfully

**Verify:**
```bash
GET http://localhost:9090/api/admin/medication-status/health
```

### Issue: Status not updating
**Debug:**
1. Manually trigger: `POST /api/admin/medication-status/update`
2. Check logs for errors
3. Verify `startDate` and `endDate` are set
4. Check medication is not `DISCONTINUED` (skipped in update)

### Issue: Duration parsing fails
**Common causes:**
- Unsupported format (only supports: days, weeks, months, years)
- Missing space: `"3months"` should be `"3 months"`
- Typos in duration string

**Test parsing:**
```java
String duration = "3 months";
LocalDate end = DurationParser.calculateEndDate(LocalDate.now(), duration);
System.out.println("End date: " + end);
```

---

## Performance Considerations

### Batch Size
Current implementation processes ALL medications in one transaction. For large datasets (>10,000 meds), consider batch processing:

```java
@Async
public void updateMedicationStatusesInBatches(int batchSize) {
    Pageable pageable = PageRequest.of(0, batchSize);
    Page<Medication> page;
    
    do {
        page = medicationRepository.findByStatusNot(MedicationStatus.DISCONTINUED, pageable);
        processPage(page.getContent());
        pageable = pageable.next();
    } while (page.hasNext());
}
```

### Cron Schedule Tuning
- **Daily (midnight)**: Sufficient for most cases
- **Every 6 hours**: May be overkill - consider reducing to 12 hours
- **On-demand**: Use admin endpoints for immediate updates

---

## Future Enhancements

1. **Notification System**: Alert helpers when medication status changes to EXPIRED
2. **Grace Period**: Keep meds ACTIVE for X days after end date (refill window)
3. **Auto-renewal**: Flag medications for prescription renewal 1 week before expiry
4. **Status History**: Track all status transitions in separate table
5. **Audit Trail**: Log who/when/why medications were discontinued
6. **Smart Scheduling**: Update only medications near expiry instead of all

---

## Files Modified

| File | Changes |
|------|---------|
| `entity/Medication.java` | Added status, startDate, endDate fields + @PrePersist logic |
| `entity/MedicationStatus.java` | New enum (ACTIVE, EXPIRED, ONGOING, DISCONTINUED) |
| `util/DurationParser.java` | New utility for duration parsing |
| `service/MedicationStatusScheduler.java` | New scheduled service with cron jobs |
| `service/PrescriptionService.java` | Added initializeMedicationDates() integration |
| `controller/MedicationStatusController.java` | New admin endpoints |
| `dto/MedicationResponseDTO.java` | Added status, startDate, endDate fields |
| `mapper/PrescriptionMapper.java` | Updated to include new fields in DTOs |
| `config/SchedulingConfig.java` | New config to enable @Scheduled |
| `resources/application.yml` | Added task scheduling pool config |

---

## Summary

✅ **Automated medication lifecycle tracking**
✅ **Daily cron jobs** (midnight + every 6 hours)
✅ **Multi-language duration support** (English & French)
✅ **Admin endpoints** for manual operations
✅ **Automatic date calculation** on prescription creation
✅ **Status transitions**: ACTIVE → EXPIRED → DISCONTINUED
✅ **API responses** include status fields

The system is **production-ready** and requires no manual intervention for status updates. Medications automatically transition based on prescription dates and durations.
