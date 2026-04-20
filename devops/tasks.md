# 🚀 Sprint 3 — DevOps Task Tracker

> **Last updated:** 2026-04-20
> **S11 Deadline:** Next week (online coaching session)
> **S12 Deadline:** Week after S11

**Progress:** 30 / 94 tasks completed

---

## 📅 WEEK S11 — Due Next Week (Online Session)

---

### 1. CI/CD Pipelines (Group Work)

> [!IMPORTANT]
> One CI pipeline + one CD pipeline **per technology** (backend & frontend). Typically done with Jenkins.

#### 1.1 Jenkins Server Setup
- [x] Install Jenkins (Docker or local) — ✅ Added to `docker-compose.devops.yml`
- [ ] Configure Jenkins with required plugins (Pipeline, Git, Maven, NodeJS, Docker, SonarQube Scanner)
- [ ] Create Jenkins credentials for Git repository access
- [ ] Create Jenkins credentials for Docker Hub (or other registry)
- [ ] Verify Jenkins can pull from your GitHub repo

#### 1.2 Backend CI Pipeline (Spring Boot / Maven)
- [x] Create `Jenkinsfile.backend` for backend CI — ✅ Created
- [x] **Stage: Checkout** — Clone the repository — ✅ In pipeline
- [x] **Stage: Build** — Run `mvn clean compile` — ✅ In pipeline
- [x] **Stage: Unit Tests** — Run `mvn test` — ✅ In pipeline with JUnit reports
- [x] **Stage: Package** — Run `mvn package -DskipTests` — ✅ In pipeline
- [x] **Stage: SonarQube Analysis** — Run `mvn sonar:sonar` — ✅ In pipeline
- [ ] Verify the pipeline runs end-to-end successfully
- [ ] Take screenshot of successful pipeline run

#### 1.3 Backend CD Pipeline
- [x] Create CD pipeline (extend Jenkinsfile with CD stages) — ✅ In `Jenkinsfile.backend`
- [x] **Stage: Docker Build** — Build Docker images for each microservice — ✅ All 13 services
- [x] **Stage: Docker Push** — Push images to Docker Hub — ✅ In pipeline
- [x] **Stage: Deploy** — Deploy using `docker-compose up -d` — ✅ In pipeline
- [ ] Verify images appear on Docker Hub after pipeline run
- [ ] Take screenshot of successful CD pipeline run

#### 1.4 Frontend CI Pipeline (Angular)
- [x] Create `Jenkinsfile.frontend` for frontend CI — ✅ Created
- [x] **Stage: Checkout** — Clone the repository — ✅ In pipeline
- [x] **Stage: Install** — Run `npm ci` — ✅ In pipeline
- [ ] **Stage: Lint** — Run `ng lint` (if configured)
- [x] **Stage: Unit Tests** — Run `ng test --watch=false --browsers=ChromeHeadless` — ✅ In pipeline
- [x] **Stage: Build** — Run `ng build --configuration=production` — ✅ In pipeline
- [x] **Stage: SonarQube Analysis** — Run sonar-scanner — ✅ In pipeline
- [ ] Verify the pipeline runs end-to-end successfully
- [ ] Take screenshot of successful pipeline run

#### 1.5 Frontend CD Pipeline
- [x] Create CD pipeline for frontend — ✅ In `Jenkinsfile.frontend`
- [x] **Stage: Docker Build** — Build Docker image — ✅ In pipeline
- [x] **Stage: Docker Push** — Push to Docker Hub — ✅ In pipeline
- [x] **Stage: Deploy** — Deploy using docker-compose — ✅ In pipeline
- [ ] Verify image appears on Docker Hub
- [ ] Take screenshot of successful CD pipeline run

---

### 2. Unit Tests in Pipeline (Group Work)

> [!NOTE]
> Tests already exist! The key is **integrating them into the pipeline**, not writing new test scenarios.

#### 2.1 Backend Test Integration
- [ ] Ensure `mvn test` runs successfully locally for all services before pipeline integration
- [x] Add `mvn test` stage to backend CI pipeline — ✅ Done in Jenkinsfile.backend
- [x] Verify test results appear in Jenkins (JUnit report plugin) — ✅ `junit` step configured
- [x] Configure Jenkins to publish test results (`**/target/surefire-reports/*.xml`) — ✅ Done
- [ ] Take screenshot of test results in Jenkins dashboard

#### 2.2 Frontend Test Integration
- [ ] Ensure `ng test --watch=false --browsers=ChromeHeadless` runs locally
- [x] Add test stage to frontend CI pipeline — ✅ Done in Jenkinsfile.frontend
- [ ] Configure `karma.conf.js` for headless CI execution (ChromeHeadless)
- [ ] Configure Jenkins to publish frontend test results
- [ ] Take screenshot of test results in Jenkins dashboard

---

### 3. SonarQube — Code Quality (Group Work)

> [!IMPORTANT]
> You must present **BEFORE** (screenshot before corrections) and **AFTER** (screenshot after refactoring).

#### 3.1 SonarQube Server Setup
- [x] Add SonarQube service to Docker Compose — ✅ In `docker-compose.devops.yml`
- [ ] Start SonarQube and access it at `http://localhost:9000`
- [ ] Create a SonarQube project for the backend
- [ ] Create a SonarQube project for the frontend
- [ ] Generate authentication token for Jenkins integration

#### 3.2 Backend SonarQube Integration
- [x] Add `sonar-maven-plugin` to backend parent `pom.xml` — ✅ Done
- [x] Configure `sonar.projectKey`, `sonar.host.url` properties — ✅ In parent pom properties
- [ ] Run `mvn sonar:sonar` locally to verify it works
- [x] Add SonarQube stage to backend CI pipeline — ✅ In Jenkinsfile.backend
- [ ] **📸 SCREENSHOT: Take "BEFORE" screenshot of SonarQube dashboard** (bugs, smells, vulnerabilities, debt)

#### 3.3 Frontend SonarQube Integration
- [x] Create `sonar-project.properties` in `frontend/` directory — ✅ Created
- [ ] Install `sonar-scanner` (or use Docker image)
- [ ] Run scanner locally to verify it works
- [x] Add SonarQube stage to frontend CI pipeline — ✅ In Jenkinsfile.frontend
- [ ] **📸 SCREENSHOT: Take "BEFORE" screenshot of frontend SonarQube dashboard**

#### 3.4 Refactoring & Quality Improvement
- [ ] Review SonarQube issues (bugs, code smells, vulnerabilities)
- [ ] Fix critical / major issues in backend code
- [ ] Fix critical / major issues in frontend code
- [ ] Re-run SonarQube analysis after fixes
- [ ] **📸 SCREENSHOT: Take "AFTER" screenshot showing improved quality metrics**
- [ ] Prepare side-by-side comparison (before vs. after) for presentation

---

### 4. Test Coverage in SonarQube (Group Work)

> [!NOTE]
> SonarQube needs coverage reports (JaCoCo for Java, lcov for Angular) to display test coverage %.

#### 4.1 Backend Coverage (JaCoCo)
- [x] Add `jacoco-maven-plugin` to backend parent `pom.xml` — ✅ Done (v0.8.12)
- [ ] Run `mvn test` and verify `target/site/jacoco/jacoco.xml` is generated
- [x] Configure SonarQube to pick up JaCoCo reports — ✅ `sonar.coverage.jacoco.xmlReportPaths` set
- [ ] Verify coverage % appears on SonarQube dashboard
- [ ] Take screenshot of coverage metrics

#### 4.2 Frontend Coverage (lcov)
- [ ] Configure `angular.json` or test command with `--code-coverage` flag
- [ ] Run `ng test --watch=false --code-coverage` and verify `coverage/lcov.info` is generated
- [x] Configure `sonar-project.properties` with `sonar.javascript.lcov.reportPaths` — ✅ Done
- [ ] Verify coverage % appears on SonarQube dashboard for frontend
- [ ] Take screenshot of coverage metrics

---

### 5. Monitoring (Group Work)

> [!IMPORTANT]
> Monitor **all project tools** (DBs, Keycloak, Eureka, Gateway...) AND both back/front projects.

#### 5.1 Prometheus Setup
- [x] Create `prometheus.yml` configuration file — ✅ Created
- [x] Add scrape targets for all backend microservices (`/actuator/prometheus`) — ✅ All 10 services
- [x] Add scrape targets for infrastructure services (Eureka, Config, Gateway) — ✅ Done
- [x] Add `prometheus` service to Docker Compose — ✅ In `docker-compose.devops.yml`
- [ ] Verify Prometheus can scrape all targets at `http://localhost:9091/targets`

#### 5.2 Spring Boot Actuator + Micrometer
- [x] Add `micrometer-registry-prometheus` dependency to parent `pom.xml` — ✅ Done (all services inherit)
- [x] Expose Prometheus endpoint via config — ✅ Already `include: "*"` in shared `application.yml`
- [ ] Verify each service exposes metrics at `/actuator/prometheus`

#### 5.3 Grafana Setup
- [x] Add `grafana` service to Docker Compose (port 3000) — ✅ In `docker-compose.devops.yml`
- [x] Configure Prometheus as a data source in Grafana — ✅ Auto-provisioned
- [x] Import/Create dashboard for **JVM metrics** (heap, threads, GC) — ✅ In dashboard JSON
- [x] Import/Create dashboard for **HTTP metrics** (request rate, latency, errors) — ✅ In dashboard JSON
- [x] Import/Create dashboard for **infrastructure** (service status table) — ✅ In dashboard JSON
- [ ] Create dashboard for **frontend** monitoring (nginx metrics)
- [ ] Take screenshots of Grafana dashboards for presentation

#### 5.4 Additional Monitoring (Optional but Recommended)
- [ ] Monitor database health/connections (if applicable)
- [ ] Monitor Keycloak metrics
- [ ] Configure Grafana alerts for critical thresholds

---

## 📅 WEEK S12 — Due the Week After

---

### 6. Kubernetes with kubeadm — Distributed Solution (Group Work)

> [!WARNING]
> Must use **kubeadm** (not Minikube). All group members must use the **same virtualization tool** (WSL preferred).

#### 6.1 Cluster Setup with kubeadm
- [ ] All team members agree on virtualization tool (WSL preferred)
- [ ] Set up master node with `kubeadm init`
- [ ] Set up worker node(s) with `kubeadm join`
- [ ] Install CNI plugin (e.g., Calico, Flannel)
- [ ] Verify cluster is healthy: `kubectl get nodes` shows all nodes Ready

#### 6.2 Kubernetes Manifests
- [x] Create `k8s/` directory in the project root — ✅ Created
- [x] Create `Namespace` for the project — ✅ `00-namespace.yml`
- [x] Create `Deployment` + `Service` for each backend microservice — ✅ `03-infrastructure.yml` + `04-microservices.yml`
- [x] Create `Deployment` + `Service` for frontend — ✅ `05-frontend.yml`
- [x] Create `ConfigMap` for shared configuration — ✅ `01-configmap.yml`
- [x] Create `Secret` for sensitive data — ✅ `02-secrets.yml` (template, fill values)
- [ ] Create `Ingress` (or NodePort services) for external access — ✅ NodePorts already set (30080, 30090, 30761)

#### 6.3 Deploy & Verify
- [ ] Push Docker images to registry (Docker Hub)
- [ ] Apply all manifests: `kubectl apply -f k8s/`
- [ ] Verify all pods are running: `kubectl get pods`
- [ ] Verify services are accessible
- [ ] Test end-to-end: frontend → gateway → microservices
- [ ] Take screenshots of `kubectl get pods,svc,nodes` for presentation

---

### 7. Excellence Part (Individual or Group Work)

> [!TIP]
> The grade depends on **complexity**. Pick something meaningful, not trivial.

#### 7.1 Choose & Implement Excellence Feature(s)
- [ ] Decide on tool/feature not covered in course (brainstorm ideas below)
- [ ] Implement the chosen tool/feature
- [ ] Document what was done and why
- [ ] Prepare demo for presentation

**Possible ideas (pick one or more):**
- Helm Charts for Kubernetes deployments
- ArgoCD / GitOps continuous delivery
- Istio Service Mesh
- HashiCorp Vault for secrets management
- EFK/ELK Stack for centralized logging
- Ansible playbooks for automated setup
- Terraform for Infrastructure as Code
- Nexus/Artifactory as artifact repository
- GitHub Actions as alternative CI/CD
- Trivy/Snyk for container security scanning
- Canary deployments / Blue-Green deployments
- Custom Grafana alerting with Slack/Discord integration

---

## 📊 Progress Summary

| Section | Tasks | Done | % |
|---------|-------|------|---|
| 1. CI/CD Pipelines | 24 | 14 | 58% |
| 2. Unit Tests in Pipeline | 10 | 4 | 40% |
| 3. SonarQube Quality | 16 | 6 | 38% |
| 4. Test Coverage | 10 | 3 | 30% |
| 5. Monitoring | 14 | 9 | 64% |
| 6. Kubernetes (kubeadm) | 16 | 6 | 38% |
| 7. Excellence | 4 | 0 | 0% |
| **TOTAL** | **94** | **42** | **45%** |

---

## 📝 Notes & Reminders

- **S11 is online** — have everything running and ready to demo via screen share
- **Screenshots are mandatory** for SonarQube before/after
- Focus on **pipeline integration** of tests, not writing new test scenarios
- All Kubernetes work must use **kubeadm**, not Minikube
- All team members must use **WSL** (or the same tool) for Kubernetes
- Excellence part grading depends on **complexity** — more complex = more points

---

## 🗂 Files Created/Modified

| File | Description |
|------|-------------|
| `backend/pom.xml` | Added JaCoCo, SonarQube plugin, micrometer-prometheus |
| `docker-compose.devops.yml` | SonarQube + Prometheus + Grafana + Jenkins |
| `devops/prometheus/prometheus.yml` | Scrape targets for all 13 services |
| `devops/grafana/provisioning/datasources/prometheus.yml` | Auto-provision Prometheus datasource |
| `devops/grafana/provisioning/dashboards/dashboards.yml` | Auto-provision dashboard files |
| `devops/grafana/dashboards/tfakkarni-monitoring.json` | 10-panel monitoring dashboard |
| `frontend/sonar-project.properties` | SonarQube scanner config for Angular |
| `Jenkinsfile.backend` | Backend CI/CD pipeline (9 stages) |
| `Jenkinsfile.frontend` | Frontend CI/CD pipeline (9 stages) |
| `k8s/00-namespace.yml` | Kubernetes namespace |
| `k8s/01-configmap.yml` | Shared ConfigMap |
| `k8s/02-secrets.yml` | Secrets template |
| `k8s/03-infrastructure.yml` | Discovery, Config, Gateway, Zipkin |
| `k8s/04-microservices.yml` | All 10 business microservices |
| `k8s/05-frontend.yml` | Frontend deployment |
