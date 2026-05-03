# Tfakkarni Linux DevOps migration notes

## Linux prerequisites

Required on a Linux runner/workstation:

- Docker Engine with the Docker Compose plugin (`docker compose`, not legacy `docker-compose`)
- kubectl configured for the target cluster
- A local Kubernetes cluster for development, such as kind or minikube
- JDK 17+ with `javac` available (a JRE is not enough for Maven tests/builds)
- Maven 3.9+
- Node.js 20+ and pnpm for frontend work

Docker Engine installation usually needs sudo/root:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin || sudo apt-get install -y docker.io docker-compose
sudo usermod -aG docker "$USER"
```

Log out/in after adding the Docker group. On Kali rolling, the Compose v2 package may be named `docker-compose` while still providing `docker compose`.

## Kubernetes bootstrap

Use the Linux deploy script from the repository root:

```bash
scripts/deploy-k8s.sh
```

By default the script skips `k8s/02-secrets.yml` because committed values are placeholders. Create real secrets first, for example:

```bash
kubectl -n tfakkarni create secret generic tfakkarni-secrets   --from-literal=NEON_GAME_DB_URL='jdbc:postgresql://...'   --from-literal=NEON_GAME_DB_USERNAME='...'   --from-literal=NEON_GAME_DB_PASSWORD='...'   --dry-run=client -o yaml | kubectl apply -f -
```

## Per-service Kubernetes CD

Jenkins now deploys immutable Docker Hub images to Kubernetes with:

```bash
kubectl -n tfakkarni set image deployment/<service-name> <service-name>=<dockerhub-user>/tfakkarni:<service-tag>-<build-number>
kubectl -n tfakkarni rollout status deployment/<service-name> --timeout=180s
```

Examples:

```bash
kubectl -n tfakkarni set image deployment/game-service game-service=thelime1/tfakkarni:game-42
kubectl -n tfakkarni rollout status deployment/game-service --timeout=180s
kubectl -n tfakkarni set image deployment/frontend frontend=thelime1/tfakkarni:frontend-42
kubectl -n tfakkarni rollout status deployment/frontend --timeout=180s
```

## Monitoring alerts

Grafana alert provisioning is under `devops/grafana/provisioning/alerting/`.
Set these environment variables in `.env` or the Grafana container environment:

```bash
GRAFANA_TELEGRAM_BOT_TOKEN=123456:token
GRAFANA_TELEGRAM_CHAT_ID=123456789
```

No Telegram secrets are committed. Provisioned alerts cover:

- service down: Prometheus `up == 0`
- high CPU: `process_cpu_usage > 0.80`
- long HTTP requests: p95 `http_server_requests_seconds_bucket > 2s`

Alert messages are intentionally human-readable and include recovery notifications.

## SonarQube / JaCoCo

Game-service Jenkins analysis uses a service-specific Sonar key and explicit JaCoCo XML path:

```bash
mvn sonar:sonar -pl game-service -am   -Dsonar.projectKey=tfakkarni-game-service   -Dsonar.coverage.jacoco.xmlReportPaths=game-service/target/site/jacoco/jacoco.xml
```

`devops/init-sonarqube.js` keeps the global strict gate behavior and creates/assigns a game-service gate requiring at least 70% coverage.
