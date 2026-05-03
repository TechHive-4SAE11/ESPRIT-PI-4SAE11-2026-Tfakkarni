# Tfakkarni long task progress

Updated: 2026-05-03
Project: `/home/kali/Desktop/ESPRIT-PI-4SAE11-2026-Tfakkarni`
Branch: `devops/linux-k8s-monitoring-cd`
Fork workflow:
- origin: `lime1-agent/ESPRIT-PI-4SAE11-2026-Tfakkarni`
- upstream: `TechHive-4SAE11/ESPRIT-PI-4SAE11-2026-Tfakkarni`
- Rule: do not push directly to upstream.

## Current git snapshot

```text
M Jenkinsfile.frontend
 M Jenkinsfile.microservice
 M backend/game-service/src/test/java/org/techhive/gameservice/service/CustomGameServiceTest.java
 M backend/game-service/src/test/resources/application-test.properties
 M backend/pom.xml
 M devops/grafana/provisioning/datasources/prometheus.yml
 M devops/init-sonarqube.js
 M docker-compose.devops.yml
 M k8s/02-secrets.yml
 M k8s/03-infrastructure.yml
 M k8s/04-microservices.yml
 M k8s/05-frontend.yml
?? backend/game-service/src/test/java/org/techhive/gameservice/client/
?? backend/game-service/src/test/java/org/techhive/gameservice/controller/DataPointControllerTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/AudioTranslationServiceTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/DataPointServiceTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/MemoryPlaceServiceTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/MemoryTagServiceTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/PatientContextServiceTest.java
?? backend/game-service/src/test/java/org/techhive/gameservice/service/TmdbServiceTest.java
?? devops/grafana/provisioning/alerting/
?? docs/devops-linux-k8s-monitoring.md
?? scripts/
```

## Prerequisite snapshot

```text
docker=Docker version 28.5.2+dfsg3, build 9cc6dea35e9a963f281434761c656fba4ac43aed
compose=Docker Compose version 2.40.3-3
kubectl=Client Version: v1.36.0
Kustomize Version: v5.8.1
mvn=Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)
Maven home: /home/kali/.local/opt/apache-maven-3.9.11
java=openjdk version "21.0.11" 2026-04-21 LTS
node=v22.22.2
pnpm=10.33.2
kind=kind v0.27.0 go1.23.6 linux/amd64
minikube=
docker ps access=permission denied while trying to connect to Docker daemon socket as current user
```

## Agent operating model

- **Hierarchy**: `short -> long -> child workers`.
- **short agent**: user-facing orchestrator/controller. Reads `task.md` and long-agent reports, verifies claims with tools, ticks checklist items only after evidence, steers long when blocked, and sends the user/Discord updates about what is happening now.
- **long agent**: implementation coordinator. Works through unchecked `task.md` items, runs long commands/tests/builds, fixes blockers, writes structured reports for short, and may spawn/manage child workers for independent parallel subtasks.
- **child workers**: optional parallel workers spawned by long only after a resource gate. They report to long, not directly to short/user, under `logs/children/` and/or named tmux sessions such as `tfakkarni-child-<service>`.
- **Child cap**: max 3 children at once. Prefer one child per independent service/task. Do not let children edit the same module/file concurrently.
- **Resource gate before children**: long must check CPU/load/RAM/process state first and stay single-worker if Kubernetes/Maven/Surefire/Hermes are saturating CPU or available RAM is low.
- **Report contract**: long writes aggregate reports under `logs/long-agent-report.md`, includes child worker status/report paths, and supporting logs under `logs/`; short uses those reports plus direct verification before updating `task.md`.
- **Long runtime**: interactive tmux session `tfakkarni-long`, so short can send steering/follow-up instructions into the running long agent.
- **Short runtime**: scheduled orchestrator job `tfakkarni-short-orchestrator` (`f883005c5817`) runs every 5 minutes while enabled, verifies long reports, ticks `task.md`, and reports back here when appropriate. Discord webhook reports are sent **only when short verifies and newly ticks one or more `task.md` checkboxes**.
- **Blocking rule**: if long/children are blocked by permissions, missing sudo, environment, or a risky decision, long reports `BLOCKED:` with exact command/error/context. short can steer it, including using the already-known sudo credentials when appropriate. If the same blocker persists for ~20 minutes, park it, document it, and switch to the easiest other unblocked task.
- **Work ordering rule**: prefer easiest/quickest/lowest-risk tasks first, then move toward harder tasks unless dependencies force another order.
- **Ultimate goal**: finish every actionable item in `task.md`, then commit logical chunks, push to `origin`, and open a PR to `upstream/main` if auth allows.

## Progress checklist

### Git workflow
- [x] Repo located locally.
- [x] Fork remotes configured.
- [x] Working branch exists: `devops/linux-k8s-monitoring-cd`.
- [x] Sync/rebase final branch with latest `upstream/main` before PR. Verified 2026-05-03 18:21 Africa/Tunis: fetched `upstream/main` and `origin/devops-linux-k8s-monitoring-cd`; `upstream/main` is already an ancestor of HEAD, so no rebase was needed.
- [x] Commit logical chunks. Verified 2026-05-03 18:26 Africa/Tunis: committed backend coverage tests as `de3b913` and devops/Kubernetes stabilization as `7c9ca06`; final tracker/docs commit follows with this checkbox update.
- [x] Push branch to `origin`. Verified 2026-05-03 18:27 Africa/Tunis: first push to `origin devops/linux-k8s-monitoring-cd` failed with remote `directory file conflict`; retried against existing tracked remote branch using `git push origin HEAD:devops-linux-k8s-monitoring-cd`, then fetched and verified local/origin both at `37f8247`.
- [x] Open PR to `upstream/main` if GitHub auth allows. Verified 2026-05-03 18:28 Africa/Tunis: `gh pr create --repo TechHive-4SAE11/ESPRIT-PI-4SAE11-2026-Tfakkarni --base main --head lime1-agent:devops-linux-k8s-monitoring-cd ...` created PR #78 at https://github.com/TechHive-4SAE11/ESPRIT-PI-4SAE11-2026-Tfakkarni/pull/78; `gh pr view 78` returned state OPEN, base `main`, head `lime1-agent:devops-linux-k8s-monitoring-cd`.

### Initial inspection / documentation
- [x] Inspected repo assets: Jenkinsfiles, compose files, k8s manifests, Grafana/Prometheus/Sonar files, game-service tests.
- [x] Added Linux DevOps notes: `docs/devops-linux-k8s-monitoring.md`.
- [x] Final report exact commands used.

### Linux migration / Docker / compose
- [x] Added Linux-compatible Kubernetes deploy script: `scripts/deploy-k8s.sh`.
- [x] Script supports `--dry-run=client|server`.
- [x] Script skips placeholder secrets by default unless dry-run or `APPLY_PLACEHOLDER_SECRETS=true`.
- [x] Docker Hub image naming migrated in K8s manifests to `thelime1/tfakkarni:<service-tag>`.
- [x] Run `docker compose` config validation: `devops`, `backend`, and `frontend` configs passed.
- [x] Build changed/local images where feasible. Verified 2026-05-03 17:08 Africa/Tunis: local Docker images built for changed feasible targets `thelime1/tfakkarni:assistant` and `thelime1/tfakkarni:frontend`; inspect/build evidence in `logs/assistant-service-image-build.log`, `logs/frontend-image-build.log`, `logs/assistant-service-image-inspect.log`, `logs/frontend-image-inspect.log`, and `logs/local-built-images-summary.log`.

### Kubernetes CD
- [x] Kubernetes manifests updated to pull Docker Hub images consistently.
- [x] Added `imagePullPolicy: Always` to service/frontend images.
- [x] Added liveness probes to microservices.
- [x] Added Zipkin health probes/resource request improvements.
- [x] Added deploy script and rollout command docs.
- [x] Updated `Jenkinsfile.microservice` to deploy each service independently with `kubectl set image` + `rollout status`.
- [x] Updated `Jenkinsfile.frontend` similarly for frontend deployment.
- [x] Validate K8s manifests with `kubectl apply --dry-run=client` via `scripts/deploy-k8s.sh --dry-run=client`.
- [x] Deploy to local Kubernetes; current kube context is now `kind-tfakkarni-devops`, namespace/resources exist, Docker Hub pull secret was applied, and private image pulling was verified at least for `thelime1/tfakkarni:alert` / `thelime1/tfakkarni:analytics` before test cleanup.
- [x] Verify pods/services/rollouts if deployment runs. Verified 2026-05-03 18:14 Africa/Tunis: `kubectl get deploy/pods/endpoints` for discovery-service, config-service, api-gateway, and zipkin showed all deployments `1/1`, pods `1/1 Running`, endpoints populated, and rollout status succeeded for all four services.

### Monitoring / Telegram alerts
- [x] Added Grafana alert provisioning folder: `devops/grafana/provisioning/alerting/`.
- [x] Added Telegram contact point using env vars, no hardcoded token/chat id.
- [x] Added notification policy.
- [x] Added alert rule: service down (`up{job!="prometheus"} == 0`).
- [x] Added alert rule: high CPU (`process_cpu_usage > 0.80`).
- [x] Added alert rule: high latency / long HTTP requests via p95 histogram query.
- [x] Added fixed Prometheus datasource UID: `prometheus`.
- [x] Passed YAML parse validation for Grafana provisioning files.
- [x] Verify Grafana can load provisioning in container if Docker is available. Verified 2026-05-03 16:32 Africa/Tunis: `GRAFANA_TELEGRAM_BOT_TOKEN='0000000000:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' GRAFANA_TELEGRAM_CHAT_ID='dummy-chat-id' docker compose -f docker-compose.devops.yml up -d prometheus grafana` brought Prometheus and Grafana healthy; `curl http://localhost:3000/api/health` returned Grafana 11.1.0 database ok; `logs/grafana-provisioning-container-tail-dummy-string-chatid-short.log` shows `starting to provision alerting` then `finished to provision alerting`. Also fixed `contact-points.yml` so `chatid` is quoted as a string for Grafana 11.1.

### SonarQube / JaCoCo / game-service coverage
- [x] Updated backend Sonar/JaCoCo paths in `backend/pom.xml`.
- [x] Updated Jenkins microservice Sonar analysis to use per-service Sonar project key and explicit JaCoCo XML path.
- [x] Expanded `devops/init-sonarqube.js` to preserve strict gate behavior and create/assign game-service gate with >=70% coverage condition.
- [x] Existing JaCoCo report generated under `backend/game-service/target/site/jacoco/`.
- [x] Added/modified many game-service tests.
- [x] Run `mvn test -pl game-service -am`: BUILD SUCCESS; 158 tests, 0 failures/errors/skips.
- [x] Generate/verify JaCoCo coverage percentage >=70% for game-service. Verified 2026-05-03 17:29 Africa/Tunis: `cd backend && mvn -pl game-service test jacoco:report -DskipTests=false` passed (174 tests, 0 failures/errors/skips, BUILD SUCCESS) and direct parse shows JaCoCo CSV line coverage 2655/3552 = 74.7466%, XML line coverage 2639/3531 = 74.7380%.
- [x] Run Sonar analysis if SonarQube is available.

### SonarQube / JaCoCo / requested service coverage expansion
- [x] Raise `assistant-service` test/JaCoCo coverage toward ~70% and verify the generated report at `backend/assistant-service/target/site/jacoco/jacoco.xml`. Verified 2026-05-03 13:49 Africa/Tunis: focused Maven/Jacoco run passed (28 tests, 0 failures/errors/skips) and `jacoco.csv` line coverage is 929/1289 = 72.07%.
- [x] Raise `medical-service` test/JaCoCo coverage toward ~70% and verify the generated report at `backend/medical-service/target/site/jacoco/jacoco.xml`. Verified 2026-05-03 14:43 Africa/Tunis: focused Maven/Jacoco run passed (`CoachingNotificationServiceTest`: 5 tests, 0 failures/errors/skips) and generated JaCoCo line coverage is XML 2854/4065 = 70.21%, CSV 2908/4127 = 70.46%.
- [x] Raise `medicament-validation-service` test/JaCoCo coverage toward ~70% and verify the generated report at `backend/medicament-validation-service/target/site/jacoco/jacoco.xml`. Verified 2026-05-03 13:19 Africa/Tunis: focused Maven/Jacoco run passed (23 tests, 0 failures/errors/skips) and `jacoco.csv` line coverage is 168/234 = 71.79%.
- [x] Raise `tracking-service` test/JaCoCo coverage toward ~70% and verify the generated report at `backend/tracking-service/target/site/jacoco/jacoco.xml`. Verified 2026-05-03 15:46 Africa/Tunis: focused Maven/Jacoco run passed (122 tests, 0 failures/errors/skips) and generated JaCoCo XML line coverage is 2813/3963 = 70.98%.
- [x] Ensure SonarQube displays coverage for these services using per-service project keys and explicit JaCoCo XML report paths.
- [x] Keep pipelines focused on reporting/visualizing coverage in SonarQube; do not add blocking quality-gate enforcement for these requested services. Verified 2026-05-03 15:52 Africa/Tunis: `Jenkinsfile.microservice` publishes JUnit/JaCoCo artifacts and runs `mvn sonar:sonar` with per-service keys/XML paths, but has no `waitForQualityGate`/blocking gate stage; `devops/init-sonarqube.js` only assigns a 70% coverage gate to `tfakkarni-game-service`, not assistant/medical/medicament-validation/tracking services.
- [x] Run targeted Maven verification for the four services: `mvn -pl assistant-service,medical-service,medicament-validation-service,tracking-service test jacoco:report -DskipTests=false`. Verified 2026-05-03 16:23 Africa/Tunis: `logs/four-service-targeted-verification.log` shows Reactor Summary `tracking-service`, `medical-service`, `medicament-validation-service`, and `assistant-service` all SUCCESS, with `BUILD SUCCESS`, total time 04:33.

### Verification already completed before this tracker
- [x] `bash -n scripts/deploy-k8s.sh` passed.
- [x] YAML parse check passed for K8s manifests, Grafana provisioning, and `docker-compose.devops.yml`.

## Known remaining work

1. Run the full verification commands now that progress is tracked here.
2. Fix any test/build/config failures using root-cause debugging.
3. Commit in logical chunks:
   - DevOps/K8s/Jenkins/monitoring docs and scripts.
   - Sonar/JaCoCo setup.
   - game-service coverage tests.
4. Push to `origin`, not upstream.
5. Open PR to `upstream/main`.

## Notes / constraints

- Do not commit real secrets.
- Telegram/Grafana values remain env vars: `GRAFANA_TELEGRAM_BOT_TOKEN`, `GRAFANA_TELEGRAM_CHAT_ID`.
- Real Kubernetes secrets should be created out-of-band; `k8s/02-secrets.yml` contains placeholders only.
