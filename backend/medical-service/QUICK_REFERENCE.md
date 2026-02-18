# Medical Service - Quick Reference Guide

## 🚀 Quick Start

### Build
```bash
cd backend/medical-service
mvn clean package -DskipTests
```

### Run
```bash
# Option 1: Maven Spring Boot plugin
mvn spring-boot:run

# Option 2: JAR file
DB_PASSWORD=your_password java -jar target/medical-service-0.0.1-SNAPSHOT.jar
```

### Verify Running
```bash
curl http://localhost:18086/api/health
# Response: {"status":"UP","service":"medical-service","message":"Medical service is running"}
```

---

## 🎯 Core Endpoints

### Create Session
```bash
POST /api/sessions
Content-Type: application/json

{
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Aspirin"]
}
```

### Get Session
```bash
GET /api/sessions/1
```

### List Sessions
```bash
# Non-paginated
GET /api/sessions?medicalFolderId=1

# Paginated
GET /api/sessions?medicalFolderId=1&page=0&size=10
```

### Update Session (Full)
```bash
PUT /api/sessions/1
Content-Type: application/json

{ ... all fields ... }
```

### Update Session (Partial)
```bash
PATCH /api/sessions/1
Content-Type: application/json

{ "duration": 90, "notes": "Updated" }
```

### Delete Session
```bash
DELETE /api/sessions/1
```

---

## 📂 Project Structure

```
medical-service/
├── controller/          # REST endpoints
├── service/             # Business logic
├── repository/          # Data access
├── entity/              # JPA models
├── dto/                 # Request/Response
├── mapper/              # Entity ↔ DTO
├── exception/           # Error handling
├── application.yml      # Configuration
├── pom.xml             # Dependencies
└── README.md           # Documentation
```

---

## 🔧 Key Classes

| Class | Purpose |
|-------|---------|
| `MedicalSession` | JPA entity (medical_session table) |
| `SessionType` | Enum: CONSULTATION, FOLLOW_UP, THERAPY, EMERGENCY |
| `MedicalSessionService` | Business operations interface |
| `MedicalSessionServiceImpl` | Business logic implementation |
| `MedicalSessionRepository` | Spring Data JPA repository |
| `MedicalSessionMapper` | Entity ↔ DTO conversion |
| `MedicalSessionController` | REST controller |
| `GlobalExceptionHandler` | Centralized error handling |

---

## 📝 SessionType Values

```java
enum SessionType {
    CONSULTATION,  // Initial consultation
    FOLLOW_UP,     // Follow-up appointment
    THERAPY,       // Therapy session
    EMERGENCY      // Emergency session
}
```

---

## ✅ Validation Rules

| Field | Required | Min | Max | Pattern |
|-------|----------|-----|-----|---------|
| medicalFolderId | Yes | 1 | - | - |
| sessionDate | Yes | - | - | ISO 8601 |
| duration | Yes | 1 | - | minutes |
| notes | No | - | 2000 | chars |
| sessionType | Yes | - | - | enum |
| prescriptions | No | - | - | JSON array |

---

## 🌐 API Gateway Integration

### Through Gateway (Production)
```bash
curl http://localhost:9090/api/sessions/1 \
  -H "Authorization: Bearer <jwt-token>"
```

### Direct Access (Development)
```bash
curl http://localhost:18086/api/sessions/1
```

---

## 🗄️ Database

### Connection
```
Host: ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech
Port: 5432
Database: neondb
User: neondb_owner
SSL: Required (sslmode=require)
```

### Schema
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

### Test Query
```sql
SELECT id, medical_folder_id, session_type, duration, prescriptions
FROM medical_session
WHERE medical_folder_id = 1
ORDER BY session_date DESC;
```

---

## 🔍 Debugging

### View Logs
```bash
# With Maven
mvn spring-boot:run -X

# With JAR
java -jar target/medical-service-0.0.1-SNAPSHOT.jar --debug=true
```

### Check Service Registration
```bash
curl http://localhost:8761/eureka/apps/medical-service | json_pp
```

### Test Individual Endpoints
```bash
# Health
curl http://localhost:18086/api/health

# Actuator health
curl http://localhost:18086/actuator/health

# Actuator metrics
curl http://localhost:18086/actuator/metrics
```

### Database Connection Test
```bash
psql -h ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb \
     -c "SELECT version();"
```

---

## 📊 Response Examples

### Success (200 OK)
```json
{
  "id": 1,
  "medicalFolderId": 1,
  "sessionDate": "2026-02-15T10:00:00",
  "duration": 60,
  "notes": "Initial consultation",
  "sessionType": "CONSULTATION",
  "prescriptions": ["Aspirin", "Lisinopril"],
  "createdAt": "2026-02-15T20:00:00",
  "updatedAt": "2026-02-15T20:00:00"
}
```

### Created (201 Created)
Same as 200 OK response

### Not Found (404)
```json
{
  "timestamp": "2026-02-15T20:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Medical session not found with ID: 999",
  "path": "/api/sessions/999"
}
```

### Validation Error (400)
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

### No Content (204 Delete)
Empty response body

---

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### cURL Examples

**Create**
```bash
curl -X POST http://localhost:18086/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"medicalFolderId":1,"sessionDate":"2026-02-15T10:00:00","duration":60,"sessionType":"CONSULTATION"}'
```

**Get**
```bash
curl http://localhost:18086/api/sessions/1
```

**Update**
```bash
curl -X PUT http://localhost:18086/api/sessions/1 \
  -H "Content-Type: application/json" \
  -d '{"medicalFolderId":1,"sessionDate":"2026-02-15T11:00:00","duration":90,"sessionType":"FOLLOW_UP"}'
```

**Delete**
```bash
curl -X DELETE http://localhost:18086/api/sessions/1
```

### Using Postman
1. Import endpoints from `API_TESTING_GUIDE.md`
2. Create collection for 6 operations
3. Set base URL: `http://localhost:18086`
4. Test each endpoint

---

## 🛠️ Common Tasks

### Change Port
Edit `application.yml`:
```yaml
server:
  port: 18086  # Change this
```

### Enable SQL Debug
Edit `application.yml`:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Change Database
Edit `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://NEW_HOST:5432/NEW_DB
    username: NEW_USER
    password: NEW_PASSWORD
```

### Add Logging
Edit `application.yml`:
```yaml
logging:
  level:
    org.techhive.medicalservice: DEBUG
    org.springframework: DEBUG
```

---

## 📦 Maven Commands

```bash
# Clean and build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run tests
mvn test

# Format code
mvn spotless:apply

# Check dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

---

## 🔐 Environment Variables

```bash
# Set DB password
export DB_PASSWORD=your_password

# Or set on command line
java -Dspring.datasource.password=your_password -jar medical-service-*.jar
```

---

## 📋 Service Information

| Property | Value |
|----------|-------|
| Service Name | medical-service |
| Port | 18086 |
| API Base | /api/sessions |
| Base Package | org.techhive.medicalservice |
| Database | PostgreSQL (Neon) |
| Discovery | Eureka (8761) |
| Gateway Route | /api/sessions/** |

---

## 🚨 Troubleshooting

| Issue | Solution |
|-------|----------|
| Port already in use | Change port in application.yml |
| Can't connect to DB | Verify credentials, SSL mode, network |
| Service not in Eureka | Check Eureka URL, verify startup logs |
| 404 Not Found | Verify session ID exists |
| Validation error | Check field types and formats |
| JSONB error | Ensure PostgreSQL >= 9.4 |

---

## 📚 Documentation

- `README.md` - Full service documentation
- `API_TESTING_GUIDE.md` - API examples
- `INTEGRATION_GUIDE.md` - Integration setup
- `IMPLEMENTATION_SUMMARY.md` - Technical details
- `REQUIREMENTS_CHECKLIST.md` - Requirements validator

---

## 🎓 Key Concepts

**Layering**
- Service Layer handles business logic
- Controller handles HTTP concerns
- Repository handles data access
- Mapper handles DTO conversions

**JSONB Prescriptions**
- Stored as JSON in PostgreSQL
- Queried like objects in SQL
- Flexible, schema-less structure
- Indexed for performance

**Error Handling**
- Global @ControllerAdvice for all exceptions
- Structured error responses
- Proper HTTP status codes
- Validation error details

**Pagination**
- Offset-based pagination via Spring Data
- Page 0 is first page
- Default size 20, customizable
- Returns Page object with metadata

---

## 💡 Tips & Tricks

1. **Set JSON format preference** in IDE for better JSON viewing
2. **Use Postman for complex testing** with save/history
3. **Monitor logs with `grep`**: `mvn spring-boot:run 2>&1 | grep -E "ERROR|WARN"`
4. **Test prescriptions**: Send as array: `["med1", "med2"]`
5. **Use ISO 8601 dates**: Always YYYY-MM-DDTHH:MM:SS format
6. **Validate enum values**: Only CONSULTATION, FOLLOW_UP, THERAPY, EMERGENCY
7. **Duration constraint**: Must be >= 1 minute
8. **Notes limit**: Max 2000 characters

---

## 📊 Monitoring

**Health Endpoints**
- Custom: `GET http://localhost:18086/api/health`
- Actuator: `GET http://localhost:18086/actuator/health`
- Metrics: `GET http://localhost:18086/actuator/metrics`

**Database Monitoring**
```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Slow queries
SELECT query, mean_exec_time FROM pg_stat_statements 
ORDER BY mean_exec_time DESC LIMIT 5;
```

---

## 🔄 Workflow

1. **Develop** → Edit source code
2. **Build** → `mvn clean package`
3. **Test Locally** → `mvn spring-boot:run`
4. **Test Endpoints** → cURL/Postman
5. **Commit** → Git commit/push
6. **Deploy** → Run JAR on server

---

## 📞 Support Resources

- Spring Boot Docs: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- PostgreSQL JSONB: https://www.postgresql.org/docs/current/datatype-json.html
- Eureka Discovery: https://cloud.spring.io/spring-cloud-netflix/
- Project Repository: Check GitHub organization

---

*Medical Service - Quick Reference*
*Last Updated: February 2026*
