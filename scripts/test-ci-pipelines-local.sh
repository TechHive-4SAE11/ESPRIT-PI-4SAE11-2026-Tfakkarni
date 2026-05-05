#!/usr/bin/env bash
set -Eeuo pipefail

# Local CI-only verifier for Jenkins CI pipeline responsibilities.
# It intentionally does NOT run CD/deploy and does NOT push Docker images.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${CI_TEST_LOG_DIR:-$ROOT/logs/ci-pipeline-tests}"
mkdir -p "$LOG_DIR"
SUMMARY="$LOG_DIR/summary.tsv"
if [[ "${CI_TEST_APPEND_SUMMARY:-0}" != "1" ]]; then
  : > "$SUMMARY"
fi

SERVICES=(
  discovery-service:discovery
  config-service:config
  api-gateway:gateway
  user-service:user
  game-service:game
  tracking-service:tracking
  alert-service:alert
  ml-service:ml
  medical-service:medical
  medicament-validation-service:medicament-validation
  iot-service:iot
  assistant-service:assistant
  analytics-service:analytics
)

run_step() {
  local name="$1"; shift
  local log="$LOG_DIR/${name}.log"
  echo "[$(date -Iseconds)] START $name" | tee -a "$LOG_DIR/long-agent-report.md"
  if ( cd "$ROOT" && "$@" ) >"$log" 2>&1; then
    echo -e "$name\tPASS\t$log" | tee -a "$SUMMARY"
    echo "[$(date -Iseconds)] PASS $name" | tee -a "$LOG_DIR/long-agent-report.md"
    return 0
  else
    local ec=$?
    echo -e "$name\tFAIL($ec)\t$log" | tee -a "$SUMMARY"
    echo "[$(date -Iseconds)] FAIL $name exit=$ec log=$log" | tee -a "$LOG_DIR/long-agent-report.md"
    tail -80 "$log" | sed 's/^/  | /' | tee -a "$LOG_DIR/long-agent-report.md"
    return "$ec"
  fi
}

prepare_test_config() {
  local svc="$1"
  mkdir -p "$ROOT/backend/$svc/src/test/resources"
  cat > "$ROOT/backend/$svc/src/test/resources/firebase-test.json" <<'JSON'
{"type":"service_account","project_id":"test","private_key_id":"test","private_key":"not-a-real-private-key-for-tests","client_email":"test@example.test","client_id":"test","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":"https://example.test/cert"}
JSON
  cat > "$ROOT/backend/$svc/src/test/resources/application-test.yml" <<'YAML'
spring:
  cloud:
    config:
      enabled: false
  ai:
    huggingface:
      api-key: dummy-hf-key
      chat:
        model: test-model
    openai:
      api-key: dummy-openai-key
      chat:
        api-key: dummy-openai-key
        options:
          model: gpt-4o-mini
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
eureka:
  client:
    enabled: false
keycloak:
  enabled: false
  server-url: http://localhost:8080
  realm: tfakkarni
  client-id: backend
  client-secret: dummy-secret
  frontend-client-id: frontend
  admin:
    username: test-admin
    password: test-password
mailtrap:
  token: dummy-token
  inbox-id: dummy-inbox
  from: noreply@example.test
didit:
  api-key: dummy-didit-key
  workflow-id: dummy-workflow
claude:
  api-key: dummy-claude-key
  api-url: http://localhost/claude-test
  model: test-model
daily:
  api-key: dummy-daily-key
  api-url: http://localhost/daily-test
gemini:
  api:
    key: dummy-gemini-key
elevenlabs:
  api:
    key: dummy-elevenlabs-key
  api-key: dummy-elevenlabs-key
  voice-id-en: test-voice
  voice-id-tn: test-voice
  model-id: test-model
telegram:
  bot-token: dummy-telegram-token
  default-chat-id: "0"
alert:
  fallback-email: alerts@example.test
tracking-service:
  url: http://localhost:8081
firebase:
  config-file: src/test/resources/firebase-test.json
notification:
  scheduler:
    medication-reminder-cron: "0 0 0 * * *"
openfda:
  api:
    base-url: http://localhost/openfda-test
    max-pages: 1
    page-size: 1
medicament:
  scheduler:
    enabled: false
    load-on-startup: false
    refresh-cron: "0 0 0 * * *"
meeting:
  room-expiry-minutes: 60
followup:
  scheduler:
    cron: "0 0 0 * * *"
recaptcha:
  secret:
    key: dummy-secret
  verify:
    url: http://localhost/recaptcha-test
YAML
}

test_backend_service() {
  local svc_tag="$1"
  local svc="${svc_tag%%:*}"
  local tag="${svc_tag##*:}"
  prepare_test_config "$svc"
  run_step "backend_${svc}_maven_package" bash -lc "cd backend && mvn clean package -pl '$svc' -am -B -Dspring.profiles.active=test -Dkeycloak.enabled=false -Dspring.cloud.config.enabled=false" || return $?
  run_step "backend_${svc}_docker_build" docker build -t "tfakkarni-ci-local:${tag}" -f "backend/${svc}/Dockerfile" backend/
}

test_frontend() {
  run_step "frontend_npm_install" bash -lc "cd frontend && npm install --legacy-peer-deps"
  run_step "frontend_ng_build" bash -lc "cd frontend && npx ng build --configuration=production --progress=false"
  run_step "frontend_docker_build" docker build -t "tfakkarni-ci-local:frontend" -f frontend/Dockerfile .
}

main() {
  echo "# Long agent report" > "$LOG_DIR/long-agent-report.md"
  echo "Updated: $(TZ='Africa/Tunis' date -Iseconds)" >> "$LOG_DIR/long-agent-report.md"
  echo "Status: RUNNING" >> "$LOG_DIR/long-agent-report.md"
  echo "Mode: SINGLE_LONG_TASK" >> "$LOG_DIR/long-agent-report.md"
  echo "Scope: CI only, no CD/deploy, no Docker push" >> "$LOG_DIR/long-agent-report.md"
  echo >> "$LOG_DIR/long-agent-report.md"

  local selector="${1:-all}"
  local failures=0
  if [[ "$selector" == "frontend" ]]; then
    test_frontend || failures=$((failures+1))
  elif [[ "$selector" == all || "$selector" == backend ]]; then
    for svc in "${SERVICES[@]}"; do
      test_backend_service "$svc" || failures=$((failures+1))
    done
    if [[ "$selector" == all ]]; then
      test_frontend || failures=$((failures+1))
    fi
  else
    test_backend_service "$selector" || failures=$((failures+1))
  fi

  {
    echo
    echo "## Summary"
    cat "$SUMMARY"
    echo
    if (( failures == 0 )); then
      echo "Status: DONE"
    else
      echo "Status: FAILURES=$failures"
    fi
    echo "Updated: $(TZ='Africa/Tunis' date -Iseconds)"
  } >> "$LOG_DIR/long-agent-report.md"
  exit "$failures"
}

main "$@"
