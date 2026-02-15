# Tracking Service

A microservice for tracking user activities and sessions in the Tfakkarni platform.

## Features

- Track user activities (login, logout, game events, etc.)
- Monitor user sessions
- Query activities by user, type, and date range
- Manage active/inactive sessions

## Port

The service runs on port **8083**

## API Endpoints

### Activity Tracking

#### Create Activity
```
POST /api/tracking/activities
Content-Type: application/json

{
  "userId": "user123",
  "activityType": "LOGIN",
  "description": "User logged in",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0..."
}
```

#### Get All Activities
```
GET /api/tracking/activities
```

#### Get Activities by User
```
GET /api/tracking/activities/user/{userId}
```

#### Get Activities by User and Type
```
GET /api/tracking/activities/user/{userId}/type/{activityType}
```

#### Get Activities by Date Range
```
GET /api/tracking/activities/date-range?startDate=2026-01-01T00:00:00&endDate=2026-12-31T23:59:59
```

#### Get Activities by User and Date Range
```
GET /api/tracking/activities/user/{userId}/date-range?startDate=2026-01-01T00:00:00&endDate=2026-12-31T23:59:59
```

#### Delete Activity
```
DELETE /api/tracking/activities/{id}
```

### Session Management

#### Create Session
```
POST /api/tracking/sessions
Content-Type: application/json

{
  "userId": "user123",
  "sessionId": "session-uuid-123",
  "ipAddress": "192.168.1.1",
  "deviceInfo": "Chrome on Windows"
}
```

#### Get All Sessions
```
GET /api/tracking/sessions
```

#### Get Sessions by User
```
GET /api/tracking/sessions/user/{userId}
```

#### Get Session by Session ID
```
GET /api/tracking/sessions/session/{sessionId}
```

#### Get Active Sessions by User
```
GET /api/tracking/sessions/user/{userId}/active
```

#### Get All Active Sessions
```
GET /api/tracking/sessions/active
```

#### End Session
```
PUT /api/tracking/sessions/session/{sessionId}/end
```

#### Delete Session
```
DELETE /api/tracking/sessions/{id}
```

## Database Configuration

Update the `application.yml` file with your PostgreSQL database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tracking_db
    username: your_username
    password: your_password
```

## Running the Service

1. Ensure PostgreSQL is running
2. Create a database named `tracking_db`
3. Run the service:
   ```
   mvn spring-boot:run
   ```

## Dependencies

- Spring Boot 3.3.6
- Spring Data JPA
- PostgreSQL
- Lombok
- Eureka Client (Service Discovery)

## Service Registration

The service automatically registers with Eureka Discovery Service at `http://localhost:8761/eureka/`
