# 📋 Sprint 3 DevOps — Gap Analysis

Analysis of what's **missing** in your project vs. the professor's email requirements.

---

## Semaine S11 — Required for Next Week

### 1. CI Pipeline + CD Pipeline (Back & Front) — ❌ MISSING

> [!CAUTION]
> **No CI/CD pipeline exists at all.** There is no `Jenkinsfile`, no `.gitlab-ci.yml`, no GitHub Actions workflow (`.github/workflows/`), and no Azure Pipelines config.

**What you need:**
- A **CI pipeline for the backend** (Spring Boot / Maven): checkout → build → test → package
- A **CI pipeline for the frontend** (Angular): checkout → install → lint → test → build
- A **CD pipeline for the backend**: build Docker image → push to registry (DockerHub) → deploy
- A **CD pipeline for the frontend**: build Docker image → push to registry → deploy
- Typically done with **Jenkins** (most common at ESPRIT) or GitLab CI

---

### 2. Unit Tests in the Pipeline — ⚠️ PARTIALLY DONE

> [!NOTE]
> You **DO** have unit tests written in both back and front. The issue is they are NOT integrated into any pipeline.

**What exists ✅:**
- **Backend:** ~50+ test files across all services (game-service, medical-service, tracking-service, ml-service, assistant-service, iot-service, medicament-validation-service, user-service)
- **Frontend:** ~20 `.spec.ts` test files (services, components, validators)

**What's missing ❌:**
- A pipeline stage that **runs** `mvn test` (backend) and `ng test --watch=false` (frontend) automatically
- The professor said: *"On évalue l'intégration de ces tests dans les pipelines et non pas les scénarios de test réalisés"* — they care about the **pipeline integration**, not the test scenarios themselves

---

### 3. SonarQube — Code Quality — ❌ COMPLETELY MISSING

> [!CAUTION]
> **Zero SonarQube configuration found.** No `sonar-project.properties`, no Sonar plugin in any `pom.xml`, no SonarQube service in Docker Compose.

**What you need:**
- A **SonarQube server** (add to Docker Compose or use SonarCloud)
- `sonar-maven-plugin` configured in backend `pom.xml`
- A SonarQube scanner configured for the Angular frontend
- **Screenshots BEFORE refactoring** (bugs, smells, vulnerabilities)
- **Code refactoring** based on SonarQube findings
- **Screenshots AFTER refactoring** showing improved quality
- Integration in the CI pipeline (`mvn sonar:sonar`)

---

### 4. Test Coverage in SonarQube — ❌ COMPLETELY MISSING

> [!CAUTION]
> **No JaCoCo plugin configured** in any backend `pom.xml`. No coverage reporting for frontend either.

**What you need:**
- **Backend:** Add `jacoco-maven-plugin` to generate coverage reports (`jacoco.xml`)
- **Frontend:** Configure `karma.conf.js` / Angular CLI to generate `lcov` coverage reports
- **SonarQube integration:** Point SonarQube to these coverage reports so it displays test coverage %
- This gets pushed to SonarQube dashboard for visibility

---

### 5. Monitoring — ❌ COMPLETELY MISSING

> [!CAUTION]
> **No monitoring stack found.** No Prometheus, no Grafana, no ELK stack. Only a basic Zipkin for tracing exists.

**What exists ✅:**
- **Zipkin** for distributed tracing (in `docker-compose.backend.yml`)
- Spring Boot **Actuator** health endpoints (already configured on all services)

**What's missing ❌:**
- **Prometheus** — to scrape metrics from all services (`/actuator/prometheus` endpoint)
- **Grafana** — dashboards to visualize backend, frontend, and infrastructure metrics
- Monitoring of **all project tools**: databases, Keycloak, API Gateway, Eureka, etc.
- Monitoring of **back and front projects**: JVM metrics, HTTP request metrics, error rates, etc.
- Docker Compose services for Prometheus + Grafana with proper configuration files

---

## Semaine S12 — Required for the Week After

### 6. Kubernetes with kubeadm (Distributed) — ❌ MOSTLY MISSING

> [!WARNING]
> You have a **KillerCoda scenario** (`killercoda-scenarios/tfakkarni-demo/`) which is a demo/tutorial, but NO actual Kubernetes manifests for deployment.

**What you need:**
- **kubeadm cluster setup** — multi-node (master + workers) using WSL (preferred by professor)
- All team members must use the **same virtualization tool**
- Kubernetes manifests: `Deployment`, `Service`, `ConfigMap`, `Ingress` for all microservices
- A **distributed solution** — not single-node Minikube, must be kubeadm-based

---

### 7. Excellence Part — ❌ NOT YET DEFINED

**What you need:**
- New tools/features **not covered in the course**
- Complexity determines the grade weight
- Can be individual OR group work
- Examples: Helm charts, ArgoCD (GitOps), Istio service mesh, Vault for secrets, EFK/ELK logging stack, Ansible automation, Terraform IaC, Nexus artifact repository, etc.

---

## Summary Table

| # | Requirement | Status | Priority |
|---|------------|--------|----------|
| 1 | CI/CD Pipelines (back + front) | ❌ Missing | 🔴 S11 |
| 2 | Unit tests in pipeline | ⚠️ Tests exist, not in pipeline | 🔴 S11 |
| 3 | SonarQube (before/after screenshots) | ❌ Missing | 🔴 S11 |
| 4 | Test coverage in SonarQube | ❌ Missing (no JaCoCo) | 🔴 S11 |
| 5 | Monitoring (Prometheus/Grafana) | ❌ Missing | 🔴 S11 |
| 6 | Kubernetes with kubeadm | ❌ Missing | 🟡 S12 |
| 7 | Excellence part | ❌ Not defined | 🟡 S12 |

> [!IMPORTANT]
> **Items 1–5 are due next week (S11, online session).** You essentially need to build the entire DevOps stack from scratch. The only things you have going for you are: ✅ Docker Compose already set up, ✅ Unit tests already written, ✅ Actuator health endpoints configured.
