# Medical Service - Integration Guide

## API Gateway Configuration

The Medical Service is automatically discovered by Eureka and the API Gateway routes requests to it. Here's what needs to be configured:

### 1. Gateway Route Configuration

In `backend/api-gateway/src/main/resources/application.yml`, add the following route:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... other routes ...
        - id: medical-service
          uri: lb://medical-service
          predicates:
            - Path=/api/sessions/**
          filters:
            - StripPrefix=1
```

### 2. Service Discovery

The Medical Service registers with Eureka on startup:

```properties
spring.application.name=medical-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

### 3. JWT Token Validation

The API Gateway validates JWT tokens from Keycloak. Medical Service endpoints are protected by the gateway's OAuth2 Resource Server.

**Request Flow:**
```
Client → API Gateway (validates JWT) → Medical Service (trusts gateway)
```

### 4. Making Authenticated Requests

All requests through the API Gateway must include a valid Bearer token:

```bash
curl -X GET http://localhost:9090/api/sessions/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

Or direct to the service (development only):

```bash
curl -X GET http://localhost:18086/api/sessions/1
```

---

## Service Registration Details

### Eureka Instance Configuration

The service registers with the following details:

```properties
spring.application.name=medical-service
eureka.instance.instance-id=medical-service:18086
eureka.instance.prefer-ip-address=true
```

**Eureka Dashboard**: http://localhost:8761
- Service should appear as "MEDICAL-SERVICE"
- Status: UP
- Instance ID: medical-service:18086

---

## Inter-Service Communication

### From Other Services

To call the Medical Service from other microservices:

**Using RestTemplate (Spring)**
```java
@Autowired
private RestTemplate restTemplate;

// Make a call to the medical-service
String url = "http://medical-service/api/sessions/{id}";
MedicalSessionResponse response = restTemplate.getForObject(url, MedicalSessionResponse.class, 1L);
```

**Using WebClient (Spring WebFlux)**
```java
@Autowired
private WebClient webClient;

// Make a call using WebClient
webClient.get()
    .uri("http://medical-service/api/sessions/{id}", 1L)
    .retrieve()
    .bodyToMono(MedicalSessionResponse.class)
    .subscribe(response -> log.info("Session: {}", response));
```

**Using Feign Client**
```java
@FeignClient(name = "medical-service")
public interface MedicalServiceClient {
    @GetMapping("/api/sessions/{id}")
    MedicalSessionResponse getSession(@PathVariable Long id);
}
```

---

## Health Checks

### Actuator Health Endpoint

**Access:** http://localhost:18086/actuator/health

Response includes:
- Database connectivity status
- Disk space status
- Custom health indicators

### Custom Health Endpoint

**Access:** http://localhost:18086/api/health

Simple health check with service info.

---

## Metrics and Monitoring

The service exposes metrics via Spring Boot Actuator:

**Endpoint:** http://localhost:18086/actuator/metrics

Available metrics include:
- `http.server.requests` - HTTP request metrics
- `jpa.transactions` - JPA transaction metrics
- `db.connection.pool` - Database connection pool metrics
- `process.cpu.usage` - CPU usage
- `process.files.open` - Open file descriptors
- `jvm.memory.used` - JVM memory usage

### Scraping Metrics

For Prometheus integration, add to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'medical-service'
    static_configs:
      - targets: ['localhost:18086']
    metrics_path: '/actuator/prometheus'
```

---

## Database Connection Pooling

The Medical Service uses HikariCP for connection pooling. Configuration:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

---

## Logging and Debugging

### Log Levels

```yaml
logging:
  level:
    org.techhive.medicalservice: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Common Logs

**Service Startup:**
```
Starting Medical Service Application
Eureka registration triggered
Tomcat started on port 18086
```

**Session Creation:**
```
DEBUG o.t.m.s.i.MedicalSessionServiceImpl: Creating medical session for medical folder ID: 1
DEBUG o.t.m.c.MedicalSessionController: POST /api/sessions - Creating new medical session
```

**Session Retrieval:**
```
DEBUG o.t.m.s.i.MedicalSessionServiceImpl: Fetching medical session with ID: 1
```

---

## Deployment Checklist

- [ ] Eureka is running on `http://localhost:8761`
- [ ] PostgreSQL is accessible on configured host/port
- [ ] Database credentials are set in `application.yml` (or via environment variables)
- [ ] API Gateway is configured with route to medical-service
- [ ] Keycloak is running and accessible (for JWT validation at gateway)
- [ ] Service starts without errors
- [ ] Service appears in Eureka dashboard
- [ ] Health endpoint responds with UP status
- [ ] Sample API calls work through gateway: `http://localhost:9090/api/sessions/**`
- [ ] Logs show successful service registration

---

## Troubleshooting

### Service Not Registering with Eureka

**Check:**
1. Eureka server is running: `http://localhost:8761`
2. `eureka.client.register-with-eureka=true` in config
3. Service startup logs show registration attempts
4. Network connectivity between service and Eureka

**Solution:**
```bash
# Check service logs
mvn spring-boot:run 2>&1 | grep -i eureka

# Verify Eureka registration
curl http://localhost:8761/eureka/apps/medical-service
```

### Database Connection Failures

**Check:**
1. PostgreSQL is running
2. Connection string is correct
3. Database credentials are accurate
4. SSL requirements are met (sslmode=require)

**Solution:**
```bash
# Test connection with psql
psql -h ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb
```

### Cannot Access Through API Gateway

**Check:**
1. API Gateway route is configured correctly
2. Medical Service is registered in Eureka
3. Service is UP in Eureka dashboard
4. Check API Gateway logs for routing errors

**Solution:**
```bash
# Access directly (bypassing gateway)
curl http://localhost:18086/api/sessions/1

# Check if gateway can see the service
curl http://localhost:9090/actuator/routes | grep medical
```

### Validation Errors

**Check:**
1. Request body matches DTOs exactly
2. Field types are correct (numbers, dates, enums)
3. Required fields are provided

**Example:**
```json
{
  "medicalFolderId": 1,           // Must be a number
  "sessionDate": "2026-02-15T10:00:00",  // ISO 8601 format
  "duration": 60,                 // Positive integer
  "sessionType": "CONSULTATION"   // Valid enum value
}
```

---

## Version Information

- **Service Version**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.3.6
- **Spring Cloud**: 2023.0.4
- **Java**: 17+
- **PostgreSQL**: 13+

---

## Related Services

The Medical Service integrates with:
- **API Gateway** (port 9090) - Request routing and authentication
- **Discovery Service / Eureka** (port 8761) - Service registration
- **User Service** (port 18081) - User/patient information
- **PostgreSQL (Neon)**  - Data persistence
- **Keycloak** (port 8280) - OAuth2 token validation (via gateway)

---

## Support

For issues or questions:
1. Check service logs: `mvn spring-boot:run`
2. Review error responses in API responses
3. Check Eureka dashboard for service health
4. Verify database connectivity
5. Review configuration in `application.yml`
