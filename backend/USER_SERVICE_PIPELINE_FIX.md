# User Service - Pipeline Build Fix Guide

## Issue
The Jenkins pipeline for user-service was failing with:
```
java.lang.IllegalArgumentException: Could not resolve placeholder 'keycloak.server-url' in value "${keycloak.server-url}"
```

## Root Cause
The `KeycloakAdminConfig` bean was always being instantiated during tests, requiring Keycloak properties that were not provided in the test environment.

## Solution Implemented

### 1. **Test Configuration File** (`src/test/resources/application-test.yml`)
- Created dedicated test configuration with all required properties
- Set `keycloak.enabled: false` to conditionally disable Keycloak configuration
- Configured H2 in-memory database for testing
- Disabled Spring Cloud Config and Eureka client

### 2. **Keycloak Config Bean Conditional** (`KeycloakAdminConfig.java`)
- Added `@ConditionalOnProperty` annotation
- Configuration only loads when `keycloak.enabled=true` (default)
- During tests with `keycloak.enabled=false`, bean is not instantiated

### 3. **Test Class Activation** (`UserServiceApplicationTests.java`)
- Added `@ActiveProfiles("test")` annotation
- Activates test configuration profile during Spring Boot Test context

### 4. **Jenkins Pipeline Improvements** (`Jenkinsfile.microservice`)
- Replaced invalid environment variables with proper Maven `-D` properties
- Added `-Dspring.profiles.active=test` to activate test profile
- Added `-Dspring.cloud.config.enabled=false` to skip config server
- Added `-Deureka.client.enabled=false` to skip Eureka
- Improved error handling and logging
- Added better post-build feedback

## Files Changed

| File                                                                      | Change                                             |
| ------------------------------------------------------------------------- | -------------------------------------------------- |
| `backend/user-service/src/test/resources/application-test.yml`            | Created new test configuration                     |
| `backend/user-service/src/main/java/.../config/KeycloakAdminConfig.java`  | Added @ConditionalOnProperty                       |
| `backend/user-service/src/test/java/.../UserServiceApplicationTests.java` | Added @ActiveProfiles("test")                      |
| `Jenkinsfile.microservice`                                                | Updated Maven build command with proper properties |

## Testing the Fix Locally

### Run tests with test profile:
```bash
cd backend
mvn clean package \
  -pl user-service \
  -am \
  -Dspring.profiles.active=test \
  -Dspring.cloud.config.enabled=false \
  -Deureka.client.enabled=false
```

### Run with test configuration:
```bash
mvn clean test \
  -pl user-service \
  -Dspring.profiles.active=test
```

## Key Configuration Properties (Test Profile)

| Property                        | Value                | Purpose                        |
| ------------------------------- | -------------------- | ------------------------------ |
| `spring.cloud.config.enabled`   | `false`              | Skip Spring Cloud Config       |
| `eureka.client.enabled`         | `false`              | Skip Eureka discovery          |
| `keycloak.enabled`              | `false`              | Disable Keycloak configuration |
| `spring.datasource.url`         | `jdbc:h2:mem:testdb` | Use H2 in-memory DB            |
| `spring.jpa.hibernate.ddl-auto` | `create-drop`        | Auto-create/drop schema        |

## Troubleshooting

### If tests still fail:

1. **Clear Maven cache:**
   ```bash
   rm -rf ~/.m2/repository
   mvn clean install
   ```

2. **Check test resources are in classpath:**
   ```bash
   # Verify file exists
   ls -la backend/user-service/src/test/resources/application-test.yml
   ```

3. **Run with debug output:**
   ```bash
   mvn clean test -X -pl user-service
   ```

4. **Check active profiles:**
   Add this to your test class:
   ```java
   @Autowired
   private Environment env;
   
   @Test
   void testActiveProfiles() {
       String[] profiles = env.getActiveProfiles();
       System.out.println("Active Profiles: " + Arrays.toString(profiles));
   }
   ```

### Common Issues:

| Issue                         | Solution                                                   |
| ----------------------------- | ---------------------------------------------------------- |
| Properties not found          | Ensure `@ActiveProfiles("test")` is on test class          |
| H2 database errors            | Check `spring.datasource.url` matches `jdbc:h2:mem:testdb` |
| Keycloak bean still loading   | Verify `keycloak.enabled: false` in test yml               |
| Maven not picking up test yml | Check file path: `src/test/resources/application-test.yml` |

## Jenkins Pipeline Execution Flow

```
1. Checkout Code
   ↓
2. Build & Test (with test profile)
   - Executes: mvn clean package -Dspring.profiles.active=test ...
   - Uses: application-test.yml
   - Keycloak bean: NOT loaded (disabled)
   ↓
3. SonarQube Analysis
   - Runs quality checks
   - Skips tests with -DskipTests=true
   ↓
4. Docker Build & Push
   - Builds Docker image with JAR
   ↓
5. Deploy
   - Deploys container via docker-compose
```

## Verification Steps

After deploying the changes:

1. **Trigger Jenkins job** for user-service
2. **Monitor Build Stage:**
   - Should see: `✅ Target JAR compiled successfully`
3. **Check test results:**
   - No property placeholder resolution errors
4. **Verify deployment:**
   - Docker container starts successfully
   - Service registers with Eureka (if enabled in production)

## Additional Notes

- The test profile is isolated and doesn't affect production builds
- Keycloak is conditionally loaded based on `keycloak.enabled` property
- RestTemplate bean from KeycloakAdminConfig is only created when Keycloak is enabled
- This approach allows flexible configuration across different environments

