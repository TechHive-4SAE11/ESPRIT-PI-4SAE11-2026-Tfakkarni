# Jenkins Pipeline Build Command Reference

## Updated Maven Build Command for User-Service

```bash
mvn clean package \
    -pl user-service \
    -am \
    -B \
    -Dspring.profiles.active=test \
    -Dspring.cloud.config.enabled=false \
    -Deureka.client.enabled=false \
    -DskipITs=true
```

## Parameter Explanation

| Parameter                             | Purpose                                            |
| ------------------------------------- | -------------------------------------------------- |
| `clean`                               | Removes previous build artifacts                   |
| `package`                             | Compiles, tests, and creates JAR                   |
| `-pl user-service`                    | Builds only user-service module                    |
| `-am`                                 | Also builds dependencies required by user-service  |
| `-B`                                  | Batch mode (no interactive input)                  |
| `-Dspring.profiles.active=test`       | Activates test profile (uses application-test.yml) |
| `-Dspring.cloud.config.enabled=false` | Disables Spring Cloud Config client                |
| `-Deureka.client.enabled=false`       | Disables Eureka service discovery                  |
| `-DskipITs=true`                      | Skips integration tests                            |

## Why These Changes Were Needed

### Original Problem Command:
```bash
mvn clean package -pl ${params.SERVICE_NAME} -am -B
```
- ❌ No test profile specified
- ❌ Used invalid environment variables (SPRING_CLOUD_CONFIG_ENABLED)
- ❌ No Keycloak properties provided
- ❌ Keycloak config bean tried to load anyway

### New Working Command:
```bash
mvn clean package \
    -pl ${params.SERVICE_NAME} \
    -am \
    -B \
    -Dspring.profiles.active=test \
    -Dspring.cloud.config.enabled=false \
    -Deureka.client.enabled=false \
    -DskipITs=true
```
- ✅ Activates test profile (provides keycloak properties)
- ✅ Uses proper Maven `-D` properties instead of env vars
- ✅ Keycloak bean only loads if enabled
- ✅ Cleaner, more explicit configuration

## Expected Build Output

When the pipeline runs, you should see:

```
[INFO] Building Tfakkarni Backend 0.0.1-SNAPSHOT
[INFO] user-service ....................................... SUCCESS
[INFO] ✅ Target JAR compiled successfully
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## If Build Still Fails

### Check 1: Verify test configuration exists
```bash
ls -la backend/user-service/src/test/resources/application-test.yml
```

### Check 2: Verify test class has annotation
```bash
grep -n "@ActiveProfiles" backend/user-service/src/test/java/org/techhive/userservice/UserServiceApplicationTests.java
```

### Check 3: Check KeycloakAdminConfig has conditional
```bash
grep -n "@ConditionalOnProperty" backend/user-service/src/main/java/org/techhive/userservice/config/KeycloakAdminConfig.java
```

### Check 4: Run locally to debug
```bash
cd backend
mvn clean package -pl user-service -am \
    -Dspring.profiles.active=test \
    -X  # Add -X for debug output
```

## Docker Build Command

After successful Maven build, the pipeline will:

```bash
docker build -t thelime1/tfakkarni:user-service-${BUILD_NUMBER} \
             -t thelime1/tfakkarni:user-service \
             -f backend/user-service/Dockerfile backend/
```

## Deployment Command

```bash
docker-compose -f docker-compose.backend.yml up -d --no-deps user-service
```

## Quick Checklist

- [ ] user-service Maven build passes ✅
- [ ] Docker image builds successfully  
- [ ] Image pushes to Docker Hub
- [ ] Container starts via docker-compose
- [ ] Service logs show no errors
- [ ] Service registers with Eureka (if applicable)

