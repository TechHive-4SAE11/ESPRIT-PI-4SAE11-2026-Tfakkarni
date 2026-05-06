# Tfakkarni Unified Quality + Pipeline Orchestration

Timezone: Africa/Tunis
Mode: **one SHORT orchestrator with two parallel LONG lanes**.
User talks to SHORT only. SHORT owns this tracker, verifies evidence, and reports to user. LONG lanes write durable reports and do not message the user directly.

## Agent topology decision

Chosen shape: **2 levels: SHORT -> LONG1 + LONG2**.

Reason: the current work has two independent external systems and mostly separate risk domains:
- **LONG1 / Sonar lane:** SonarQube project listing + backend coverage push toward ~70% overall.
- **LONG2 / Jenkins lane:** start Jenkins, run pipelines, debug until green in Jenkins UI.

A third level (`LONG -> children`) is not needed at startup because Maven/Sonar/Jenkins are heavy. LONG lanes may later use bounded children only if SHORT verifies resources are healthy and scopes do not conflict.

## Global sequencing rules

- [x] Stop/check Kubernetes first when requested. Result: Kubernetes API was refused, kubelet/k3s inactive, no kind clusters or K8s processes; nothing active to shut down. SonarQube container was left running because it is not Kubernetes.
- [x] Keep SonarQube running for Sonar work.
- [ ] Start/keep Jenkins only for Jenkins pipeline work.
- [ ] Run Sonar and Jenkins lanes in parallel if resource gate permits.
- [ ] **Deploy work stays last** and must not start until LONG1 and LONG2 are DONE or SHORT explicitly reorders it.
- [ ] Before any deploy phase, rerun resource gate and Kubernetes readiness checks.

## LONG1 — SonarQube coverage + project visibility

Report file: `logs/long1-sonar-report.md`
Session log: `logs/long1-sonar-session.log`

### SonarQube project visibility

- [x] SonarQube is UP at http://localhost:9095.
- [x] Existing projects before adding missing services: assistant, game, medical, medicament-validation, tracking.
- [x] Added/listed all remaining backend service projects in SonarQube UI via API, without coverage work:
  - alert-service
  - analytics-service
  - api-gateway
  - config-service
  - discovery-service
  - iot-service
  - ml-service
  - user-service
- [x] Verified final backend project count: 13 `tfakkarni-*` projects, missing: none.

### Why UI shows 32-50% while earlier notes said ~70%

- SonarQube UI main **coverage** combines line and branch coverage.
- Current services have ~70%+ **line_coverage**, but low **branch_coverage**, so overall coverage stays lower.
- Baseline from Sonar API:
  - tracking-service: 40.0% overall; 72.4% line; 10.7% branch
  - medicament-validation-service: 43.2% overall; 80.3% line; 18.8% branch
  - medical-service: 44.6% overall; 70.2% line; 21.8% branch
  - game-service: 52.4% overall; 74.7% line; 15.9% branch
  - assistant-service: 36.2% overall; 71.7% line; 10.0% branch

### Coverage target services

- [ ] tracking-service: target ~70% overall coverage in SonarQube UI
- [ ] medicament-validation-service: target ~70% overall coverage in SonarQube UI
- [ ] medical-service: target ~70% overall coverage in SonarQube UI
- [ ] game-service: target ~70% overall coverage in SonarQube UI
- [ ] assistant-service: target ~70% overall coverage in SonarQube UI

### LONG1 plan

1. [ ] Baseline each target locally with Maven/Jacoco and SonarQube measures.
2. [ ] Identify branch-heavy low-coverage files for each target.
3. [ ] Add meaningful tests, prioritizing branch coverage and not fake/no-assertion tests.
4. [ ] Re-run Maven tests per service after each batch.
5. [ ] Re-run Sonar analysis per service so UI updates.
6. [ ] Query SonarQube API for coverage/line/branch numbers.
7. [ ] Update `logs/long1-sonar-report.md` with evidence after every service/milestone.

## LONG2 — Jenkins pipelines green in UI

Report file: `logs/long2-jenkins-report.md`
Session log: `logs/long2-jenkins-session.log`

### LONG2 target

- [ ] Start Jenkins locally without exposing secrets.
- [ ] Verify Jenkins UI/API is reachable.
- [ ] Inventory configured jobs/pipelines and expected per-service jobs.
- [ ] Run pipelines requested/available in Jenkins.
- [ ] Debug failures until pipelines are green in Jenkins UI.
- [ ] Keep deploy/Kubernetes rollout stages parked until LONG1 and LONG2 finish and SHORT authorizes deploy-last phase.
- [ ] Update `logs/long2-jenkins-report.md` with job names, build numbers, URLs/status, failures, fixes, and remaining blockers.

## Deploy phase — parked until end

- [ ] Do not start deploy work while LONG1 or LONG2 is still running.
- [ ] After LONG1 and LONG2 are done, SHORT decides whether deploy phase should start.
- [ ] Deploy phase must verify Kubernetes state first; if Kubernetes is stopped, start only what is required and keep resources bounded.

## SHORT loop duties

- [ ] Maintain this `task.md` as source of truth.
- [ ] Maintain `logs/short-loop-state.md`.
- [ ] Monitor LONG1 and LONG2 report files and process status.
- [ ] Verify evidence before ticking task items.
- [ ] Send user progress updates, especially on blockers/stalls.
- [ ] If one lane blocks for ~20 minutes, park blocker and steer resources to the other lane.

## Current status

- Current phase: unified orchestration startup.
- Running containers observed: SonarQube healthy; Jenkins status to be checked/started.
- LONG1: to be started/steered for coverage push and Sonar verification.
- LONG2: to be started for Jenkins pipeline green UI.
- Deploy: parked until both LONG lanes finish.
