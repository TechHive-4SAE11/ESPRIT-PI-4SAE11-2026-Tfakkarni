# Medical Service

## Overview

The **Medical Service** is a Spring Boot microservice that manages medical sessions for the Tfakkarni Alzheimer tracking platform. It provides REST APIs for creating, retrieving, updating, and deleting medical sessions associated with medical folders (patient records).

## Features

- **Medical Session Management**: CRUD operations for medical sessions
- **Session Types**: Support for CONSULTATION, FOLLOW_UP, THERAPY, and EMERGENCY session types
- **Prescriptions Storage**: Store prescriptions as JSONB data in PostgreSQL
- **Pagination Support**: Retrieve sessions with optional pagination
- **Eureka Service Discovery**: Auto-registers with Eureka discovery service
- **Global Exception Handling**: Centralized error handling with structured error responses
- **Validation**: Comprehensive input validation using Jakarta Bean Validation
- **Actuator Health Check**: Built-in health endpoint via Spring Boot Actuator

## Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.3.6
- **Spring Cloud**: 2023.0.4
- **Database**: PostgreSQL (Neon Cloud)
- **Service Discovery**: Netflix Eureka
- **ORM**: Hibernate 6 with Spring Data JPA
- **Build Tool**: Maven

## Port

- **Service Port**: 18086
- **API Base Path**: `/api`

## Project Structure

```
medical-service/
├── src/main/java/org/techhive/medicalservice/
│   ├── controller/              # REST Controllers
│   │   ├── MedicalSessionController.java
│   │   └── HealthController.java
│   ├── service/                 # Business Logic
│   │   ├── MedicalSessionService.java
│   │   └── impl/MedicalSessionServiceImpl.java
│   ├── repository/              # Data Access
│   │   └── MedicalSessionRepository.java
│   ├── entity/                  # JPA Entities
│   │   ├── MedicalSession.java
│   │   └── SessionType.java
│   ├── dto/                     # Data Transfer Objects
│   │   ├── CreateMedicalSessionRequest.java
│   │   ├── UpdateMedicalSessionRequest.java
│   │   └── MedicalSessionResponse.java
│   ├── mapper/                  # Entity to DTO Mapping
│   │   └── MedicalSessionMapper.java
│   ├── exception/               # Exception Handling
│   │   ├── ResourceNotFoundException.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   └── MedicalServiceApplication.java
├── src/main/resources/
│   └── application.yml          # Configuration
├── src/test/java/
│   └── org/techhive/medicalservice/
│       └── service/MedicalSessionServiceTest.java
└── pom.xml
```

## Database Schema

### medical_session Table

```sql
CREATE TABLE medical_session (
    id BIGSERIAL PRIMARY KEY,
    medical_folder_id BIGINT NOT NULL,
    session_date TIMESTAMP NOT NULL,
    duration INT NOT NULL,
    notes VARCHAR(2000),
    session_type VARCHAR(50) NOT NULL,
    prescriptions JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## API Endpoints

### Create Medical Session
```
POST /api/sessions
Content-Type: application/json

{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Medication A", "Medication B"]
}
```

**Response**: 201 Created
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation and assessment",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Medication A", "Medication B"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

### Get Medical Session by ID
```
GET /api/sessions/{id}
```

**Response**: 200 OK

### Get Sessions by Medical Folder ID (with pagination)
```
GET /api/sessions?medicalFolderId=1&page=0&size=20
```

**Response**: 200 OK (Page object)

Or without pagination:
```
GET /api/sessions?medicalFolderId=1
```

**Response**: 200 OK (List)

### Update Medical Session (Full)
```
PUT /api/sessions/{id}
Content-Type: application/json

{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T11:00:00",
  "duration": 90,
  "notes": "Updated consultation notes",
  "sessionType": "FOLLOW_UP",
  "prescriptions": ["New Medication"]
}
```

**Response**: 200 OK

### Partial Update Medical Session
```
PATCH /api/sessions/{id}
Content-Type: application/json

{
  "duration": 90,
  "notes": "Updated notes"
}
```

**Response**: 200 OK

### Delete Medical Session
```
DELETE /api/sessions/{id}
```

**Response**: 204 No Content

### Health Check
```
GET /api/health
```

**Response**: 200 OK
```json
{
  "status": "UP",
  "service": "medical-service",
  "message": "Medical service is running"
}
```

### Actuator Health
```
GET /actuator/health
```

**Response**: 200 OK

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2026-02-15T20:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Medical session not found with ID: 1",
  "path": "/api/sessions/1",
  "validationErrors": null
}
```

### Error Codes

- **404 Not Found**: Resource not found
- **400 Bad Request**: Validation error
- **500 Internal Server Error**: Unexpected server error

## Configuration (application.yml)

```yaml
spring:
  application:
    name: medical-service
  datasource:
    url: jdbc:postgresql://ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require&channel_binding=require
    username: neondb_owner
    password: ${DB_PASSWORD:REDACTED}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 18086

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL (Neon)
- Eureka Discovery Service running on `http://localhost:8761`

### Build
```bash
mvn clean package -DskipTests
```

### Run
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/medical-service-0.0.1-SNAPSHOT.jar
```

## Testing

Run the test suite:
```bash
mvn test
```

## Key Classes

### Entities
- **MedicalSession**: Main entity with JSONB support for prescriptions
- **SessionType**: Enum with values: CONSULTATION, FOLLOW_UP, THERAPY, EMERGENCY

### DTOs
- **CreateMedicalSessionRequest**: Request DTO for session creation
- **UpdateMedicalSessionRequest**: Request DTO for session updates (partial)
- **MedicalSessionResponse**: Response DTO with all session data

### Services
- **MedicalSessionService**: Interface defining business operations
- **MedicalSessionServiceImpl**: Implementation with logging and transaction management

### Repository
- **MedicalSessionRepository**: Spring Data JPA repository with custom queries for:
  - `findByMedicalFolderId(Long)`: Get all sessions for a folder
  - `findByMedicalFolderId(Long, Pageable)`: Get paginated sessions for a folder

### Exception Handling
- **GlobalExceptionHandler**: Centralized exception handler for:
  - ResourceNotFoundException (404)
  - MethodArgumentNotValidException (400 with validation details)
  - General Exception (500)

## JSONB Prescriptions Storage

Prescriptions are stored as JSONB in PostgreSQL for flexibility. The entity provides helper methods:

- `getPrescriptionsAsJson()`: Returns prescriptions as a List<String>
- `setPrescriptionsFromJson(List<String>)`: Sets prescriptions from a list, automatically serializing to JSON

Example:
```java
session.setPrescriptionsFromJson(Arrays.asList("Medication A", "Medication B"));
// Stored as: ["Medication A", "Medication B"]

List<String> meds = session.getPrescriptionsAsJson();
// Returns: [Medication A, Medication B]
```

## Logging

Logging is configured for DEBUG level for the medical-service package and related Spring components. Logs include:
- Service operations (create, read, update, delete)
- HTTP requests/responses
- Security events
- Health checks

## Validation Rules

| Field | Rules |
| --- | --- |
| `medicalFolderId` | Required, positive integer |
| `sessionDate` | Required, not null |
| `duration` | Required, minimum 1 minute |
| `notes` | Optional, max 2000 characters |
| `sessionType` | Required, one of: CONSULTATION, FOLLOW_UP, THERAPY, EMERGENCY |
| `prescriptions` | Optional, stored as JSONB |

## Integration with API Gateway

The service is registered with Eureka and accessible through the API Gateway at:
```
http://localhost:9090/api/sessions/**
```

The gateway proxies all requests to the medical-service running on port 18086.

## Future Enhancements

- Add OpenAPI/Swagger documentation
- Implement caching for frequently accessed sessions
- Add audit logging for compliance
- Implement soft delete for sessions
- Add session status tracking (scheduled, completed, cancelled)
- Integrate with notification service for appointment reminders

## License

Part of the Tfakkarni Alzheimer Tracking Platform © TechHive-4SAE11 @ ESPRIT
