# OpenFeign — Inter-Service Communication in Tfakkarni

## What is Feign?

**OpenFeign** (Spring Cloud OpenFeign) is a declarative HTTP client for Java microservices. Instead of writing boilerplate `RestTemplate` or `WebClient` code to call other services, you define a **Java interface** annotated with Spring MVC annotations (`@GetMapping`, `@PostMapping`, etc.) and Feign generates the HTTP client implementation at runtime.

### Key Benefits

| Benefit | Description |
|---------|-------------|
| **Declarative** | Define REST calls as simple Java interfaces — no manual HTTP code |
| **Eureka Integration** | Resolves service URLs automatically via service discovery (no hardcoded IPs) |
| **Load Balancing** | Built-in client-side load balancing when multiple instances are registered |
| **Fallbacks** | Supports fallback classes for graceful degradation when a service is down |
| **Spring Native** | Uses familiar `@GetMapping`/`@PostMapping` annotations — zero learning curve |

### How It Works

```
┌─────────────────┐     Feign (HTTP)     ┌─────────────────┐
│  analytics-svc  │ ──────────────────►  │   game-service   │
│                 │  GET /api/games/...  │                  │
│  @Autowired     │ ◄────────────────── │  @RestController │
│  GameClient     │     JSON response   │                  │
└─────────────────┘                      └─────────────────┘
```

1. A service declares a `@FeignClient` interface pointing to the target service name
2. At startup, Feign creates a proxy that resolves the target via **Eureka Discovery**
3. Calling a method on the interface triggers an HTTP request to the target service
4. The response is automatically deserialized into the return type

---

## Where Feign Is Used in Tfakkarni

### Services with `@EnableFeignClients`

| Service | Calls To | Purpose |
|---------|----------|---------|
| **game-service** | user-service, analytics-service | Resolve player identity, check feature gates |
| **analytics-service** | user-service, game-service, tracking-service, medical-service, iot-service, alert-service | Aggregate data from all services to compute patient composite scores |
| **assistant-service** | medical-service, game-service | Manage equipment loans, generate quizzes via AI |
| **medical-service** | user-service, tracking-service, game-service, ml-service | Enrich medical folders with patient data, AI clinical analysis |
| **tracking-service** | user-service, medicament-validation-service | Validate medication names, resolve user info |
| **iot-service** | analytics-service, alert-service | Check IoT feature gates, forward alerts |
| **ml-service** | *(enabled but clients not yet defined)* | Future inter-service calls |

### Dependency Graph (Feign Calls)

```
                          ┌──────────────┐
                          │ user-service │
                          └──────┬───────┘
                 ┌───────────────┼───────────────┐
                 ▼               ▼               ▼
          ┌────────────┐  ┌───────────┐  ┌──────────────┐
          │game-service│  │tracking-  │  │medical-      │
          │            │  │service    │  │service       │
          └─────┬──────┘  └─────┬─────┘  └──────┬───────┘
                │               │               │
                ▼               ▼               ▼
          ┌─────────────────────────────────────────┐
          │          analytics-service              │
          │  (aggregates scores from all services)  │
          └────────────────┬────────────────────────┘
                           ▲
                    ┌──────┴──────┐
                    │ iot-service │
                    └─────────────┘
```

---

## Feign Client Examples from Our Code

### 1. Simple Client — Game → User Service

```java
@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/keycloak/{keycloakId}")
    UserResponse getUserByKeycloakId(@PathVariable("keycloakId") String keycloakId);
}
```

- `name = "user-service"` — resolved via Eureka (no hardcoded URL)
- `fallback` — if user-service is down, `UserServiceClientFallback` returns `null` instead of crashing

### 2. Multi-Endpoint Client — Analytics → Tracking Service

```java
@FeignClient(name = "tracking-service", fallback = TrackingServiceClientFallback.class)
public interface TrackingServiceClient {

    @GetMapping("/api/prescriptions/patient/{idPatient}")
    List<PrescriptionResponseDTO> getPrescriptionsByPatient(@PathVariable("idPatient") String idPatient);

    @GetMapping("/api/statistics/{patientId}/medication-compliance")
    Map<String, Object> getMedicationCompliance(
            @PathVariable("patientId") String patientId,
            @RequestParam("days") int days);

    @GetMapping("/api/statistics/{patientId}/streak")
    Map<String, Object> getStreak(@PathVariable("patientId") String patientId);

    @GetMapping("/api/daily-monitoring/{patientId}")
    Map<String, Object> getDailyLog(
            @PathVariable("patientId") String patientId,
            @RequestParam("date") String date);
}
```

### 3. Client with Explicit URL — Assistant → Medical Service

```java
@FeignClient(name = "medical-service", url = "${feign.medical-service.url:http://localhost:18086}")
public interface MedicalServiceClient {

    @GetMapping("/api/medical/equipment")
    List<EquipmentDTO> getAllEquipment();

    @PostMapping("/api/medical/loans/borrow")
    EquipmentLoanDTO borrowEquipment(@RequestBody EquipmentLoanDTO loanDTO);
}
```

- Uses `url` for direct connection (bypasses Eureka) — useful for development/testing

---

## Fallback Pattern (Resilience)

When a target service is unavailable, Feign fallbacks prevent cascading failures:

```java
@Component
public class GameServiceClientFallback implements GameServiceClient {

    @Override
    public GameStatsResponse getPlayerStats(String keycloakId) {
        return new GameStatsResponse(); // Return empty data instead of error
    }

    @Override
    public ScoreAnalyticsResponse getScoreAnalytics(String keycloakId) {
        return new ScoreAnalyticsResponse(); // Graceful degradation
    }
}
```

**Services with fallbacks:** game-service, analytics-service (6 fallbacks), tracking-service (2 fallbacks), iot-service (2 fallbacks)

---

## Why We Chose Feign

| Alternative | Why Feign Wins |
|-------------|----------------|
| **RestTemplate** | Feign is declarative — no manual URL building, no `exchange()` boilerplate |
| **WebClient** | Feign integrates natively with Eureka discovery and Spring Cloud |
| **gRPC** | Feign uses standard REST/JSON — simpler to debug and test with Postman |
| **Direct HTTP** | Feign resolves service names via Eureka — no hardcoded `localhost:port` in production |

### In Summary

Feign is used in **7 of our microservices** to enable clean, type-safe, discovery-aware inter-service communication. Combined with Eureka and fallback classes, it makes our microservice architecture resilient and maintainable.

---

## Maven Dependency

Every service using Feign includes:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

And the main application class is annotated with:

```java
@SpringBootApplication
@EnableFeignClients
public class GameServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
```
