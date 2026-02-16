# Tracking Service - Medical Prescription Management System

A comprehensive microservice for managing medical folders, consultation sessions, and prescriptions in the Tfakkarni healthcare platform.

## 🏥 Overview

The Tracking Service provides a complete solution for managing patient medical records, doctor-patient relationships, consultation sessions, and prescription tracking. It features a hierarchical data structure with Medical Folders containing Sessions, which in turn contain Prescriptions.

## 🚀 Features

- **Medical Folder Management**: Link patients with doctors and organize medical records
- **Session Tracking**: Record and manage consultation sessions with detailed notes
- **Prescription Management**: Create, track, and manage medication prescriptions
- **RESTful API**: Complete CRUD operations for all entities
- **Service Discovery**: Integrated with Eureka for microservices architecture
- **API Documentation**: Interactive Swagger/OpenAPI documentation
- **PostgreSQL Database**: Robust data persistence with automatic schema management

## 📊 Data Model

### Entity Hierarchy

```
MedicalFolder (Patient ↔ Doctor)
    └── Session (Consultation)
            └── Prescription (Medication)
```

### Entities

#### 1. MedicalFolder
Links a patient with a doctor and contains all their consultation sessions.

**Fields:**
- `id` (Long) - Primary key
- `idPatient` (String) - Patient identifier
- `idDoctor` (String) - Doctor identifier
- `createdAt` (LocalDateTime) - Creation timestamp
- `updatedAt` (LocalDateTime) - Last update timestamp
- `sessions` (List<Session>) - Associated consultation sessions

#### 2. Session
Represents a medical consultation session.

**Fields:**
- `id` (Long) - Primary key
- `medicalFolder` (MedicalFolder) - Foreign key to medical folder
- `sessionDate` (LocalDateTime) - Date and time of the session
- `notes` (Text) - Consultation notes
- `createdAt` (LocalDateTime) - Creation timestamp
- `updatedAt` (LocalDateTime) - Last update timestamp
- `prescriptions` (List<Prescription>) - Associated prescriptions

#### 3. Prescription
Contains medication details prescribed during a session.

**Fields:**
- `id` (Long) - Primary key
- `session` (Session) - Foreign key to session
- `medicationName` (String) - Name of the medication
- `dosage` (String) - Dosage information
- `frequency` (String) - How often to take the medication
- `duration` (String) - How long to take the medication
- `instructions` (Text) - Additional instructions
- `createdAt` (LocalDateTime) - Creation timestamp
- `updatedAt` (LocalDateTime) - Last update timestamp

## 🔌 API Endpoints

### Medical Folders (`/api/medical-folders`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a new medical folder |
| GET | `/` | Get all medical folders |
| GET | `/{id}` | Get medical folder by ID |
| GET | `/patient/{idPatient}` | Get all folders for a patient |
| GET | `/doctor/{idDoctor}` | Get all folders for a doctor |
| GET | `/patient/{idPatient}/doctor/{idDoctor}` | Get folders by patient and doctor |
| PUT | `/{id}` | Update a medical folder |
| DELETE | `/{id}` | Delete a medical folder |

**Example Request (Create):**
```json
POST /api/medical-folders
{
  "idPatient": "patient123",
  "idDoctor": "doctor456"
}
```

**Example Response:**
```json
{
  "id": 1,
  "idPatient": "patient123",
  "idDoctor": "doctor456",
  "createdAt": "2026-02-15T10:30:00",
  "updatedAt": "2026-02-15T10:30:00"
}
```

### Sessions (`/api/sessions`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a new session |
| GET | `/` | Get all sessions |
| GET | `/{id}` | Get session by ID |
| GET | `/medical-folder/{medicalFolderId}` | Get all sessions for a medical folder |
| PUT | `/{id}` | Update a session |
| DELETE | `/{id}` | Delete a session |

**Example Request (Create):**
```json
POST /api/sessions
{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T14:00:00",
  "notes": "Regular checkup. Patient reports feeling well. Blood pressure normal."
}
```

**Example Response:**
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T14:00:00",
  "notes": "Regular checkup. Patient reports feeling well. Blood pressure normal.",
  "createdAt": "2026-02-15T14:00:00",
  "updatedAt": "2026-02-15T14:00:00"
}
```

### Prescriptions (`/api/prescriptions`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a new prescription |
| GET | `/` | Get all prescriptions |
| GET | `/{id}` | Get prescription by ID |
| GET | `/session/{sessionId}` | Get all prescriptions for a session |
| GET | `/patient/{idPatient}` | Get all prescriptions for a patient |
| PUT | `/{id}` | Update a prescription |
| DELETE | `/{id}` | Delete a prescription |

**Example Request (Create):**
```json
POST /api/prescriptions
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Amoxicillin",
      "dosage": "500mg",
      "frequency": "3 times daily",
      "duration": "7 days",
      "instructions": "Take with food. Complete the full course."
    },
    {
      "medicationName": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "Twice daily",
      "duration": "5 days",
      "instructions": "Take after meals"
    }
  ]
}
```

**Example Response:**
```json
{
  "id": 1,
  "sessionId": 1,
  "medications": [
    {
      "id": 1,
      "medicationName": "Amoxicillin",
      "dosage": "500mg",
      "frequency": "3 times daily",
      "duration": "7 days",
      "instructions": "Take with food. Complete the full course.",
      "createdAt": "2026-02-15T19:00:00"
    },
    {
      "id": 2,
      "medicationName": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "Twice daily",
      "duration": "5 days",
      "instructions": "Take after meals",
      "createdAt": "2026-02-15T19:00:00"
    }
  ],
  "createdAt": "2026-02-15T19:00:00",
  "updatedAt": "2026-02-15T19:00:00"
}
  "createdAt": "2026-02-15T14:05:00",
  "updatedAt": "2026-02-15T14:05:00"
}
```

## ⚙️ Configuration

### Port
The service runs on **port 8083**

### Database Configuration

Update `application.yml` with your PostgreSQL credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/your-database
    username: your-username
    password: your-password
```

Current configuration uses Neon PostgreSQL:
- Host: `ep-young-recipe-ag0a1sn7-pooler.c-2.eu-central-1.aws.neon.tech`
- Database: `neondb`
- SSL: Required with channel binding

### Eureka Configuration

The service automatically registers with Eureka Discovery Service:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## 📚 API Documentation

### Swagger UI
Once the service is running, access interactive API documentation at:
```
http://localhost:8083/swagger-ui.html
```

### OpenAPI Specification
View the OpenAPI JSON specification at:
```
http://localhost:8083/v3/api-docs
```

## 🛠️ Technologies

- **Spring Boot 3.3.6** - Application framework
- **Spring Data JPA** - Data persistence
- **Spring Cloud Netflix Eureka** - Service discovery
- **PostgreSQL** - Database
- **Lombok** - Reduce boilerplate code
- **SpringDoc OpenAPI 2.3.0** - API documentation (Swagger)
- **Maven** - Build tool

## 📦 Installation & Setup

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- Eureka Discovery Service running on port 8761

### Build the Service

```bash
cd backend/tracking-service
mvn clean install
```

### Run the Service

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/tracking-service-0.0.1-SNAPSHOT.jar
```

### Database Setup

The service uses JPA with `ddl-auto: update`, so tables will be created automatically on first run.

Manual table creation is not required, but if needed, the schema includes:
- `medical_folders` table
- `sessions` table
- `prescriptions` table

## 🔄 Workflow Example

### 1. Create a Medical Folder
Link a patient with a doctor:
```bash
POST /api/medical-folders
{
  "idPatient": "P001",
  "idDoctor": "D001"
}
```

### 2. Create a Session
Record a consultation:
```bash
POST /api/sessions
{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T14:00:00",
  "notes": "Annual checkup"
}
```

### 3. Add Prescriptions
Prescribe medications:
```bash
POST /api/prescriptions
{
  "sessionId": 1,
  "medications": [
    {
      "medicationName": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "Twice daily",
      "duration": "5 days",
      "instructions": "Take after meals"
    }
  ]
}
```

### 4. Retrieve Patient History
```bash
GET /api/medical-folders/patient/P001
GET /api/prescriptions/patient/P001
```

## 🔧 Development

### Project Structure

```
tracking-service/
├── src/
│   ├── main/
│   │   ├── java/org/techhive/trackingservice/
│   │   │   ├── controller/
│   │   │   │   ├── MedicalFolderController.java
│   │   │   │   ├── SessionController.java
│   │   │   │   └── PrescriptionController.java
│   │   │   ├── dto/
│   │   │   │   ├── MedicalFolderRequestDTO.java
│   │   │   │   ├── MedicalFolderResponseDTO.java
│   │   │   │   ├── SessionRequestDTO.java
│   │   │   │   ├── SessionResponseDTO.java
│   │   │   │   ├── PrescriptionRequestDTO.java
│   │   │   │   └── PrescriptionResponseDTO.java
│   │   │   ├── entity/
│   │   │   │   ├── MedicalFolder.java
│   │   │   │   ├── Session.java
│   │   │   │   └── Prescription.java
│   │   │   ├── repository/
│   │   │   │   ├── MedicalFolderRepository.java
│   │   │   │   ├── SessionRepository.java
│   │   │   │   └── PrescriptionRepository.java
│   │   │   ├── service/
│   │   │   │   ├── MedicalFolderService.java
│   │   │   │   ├── SessionService.java
│   │   │   │   └── PrescriptionService.java
│   │   │   └── TrackingServiceApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── pom.xml
```

### Adding New Features

1. Create entity in `entity/` package
2. Create repository interface in `repository/`
3. Implement business logic in `service/`
4. Create DTOs in `dto/`
5. Implement REST endpoints in `controller/`

## 🧪 Testing

Run all tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn clean test jacoco:report
```

## 📝 Notes

- All timestamps are automatically managed with `@PrePersist` and `@PreUpdate`
- Cascading deletes are enabled for related entities
- CORS is enabled for all origins (`@CrossOrigin(origins = "*")`)
- Lazy loading is used for entity relationships to optimize performance
- Transactions are managed with `@Transactional` annotations

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📄 License

This project is part of the Tfakkarni platform by TechHive-4SAE11.

## 📞 Support

For issues or questions, please contact the development team or create an issue in the repository.

---

**Service Name:** tracking-service  
**Port:** 8083  
**Version:** 0.0.1-SNAPSHOT  
**Last Updated:** February 15, 2026

