#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${KUBE_NAMESPACE:-tfakkarni}"
MANIFEST_DIR="${MANIFEST_DIR:-${ROOT_DIR}/k8s}"
NGROK_PORT="${NGROK_PORT:-30080}"
NGROK_API_URL="${NGROK_API_URL:-http://127.0.0.1:4040}"
KIND_CLUSTER_NAME="${KIND_CLUSTER_NAME:-tfakkarni-devops}"
APPLY_PLACEHOLDER_SECRETS="${APPLY_PLACEHOLDER_SECRETS:-true}"
IMAGE_PULL_SECRET_NAME="${IMAGE_PULL_SECRET_NAME:-dockerhub-pull-secret}"
WAIT_FOR_ROLLOUT="${WAIT_FOR_ROLLOUT:-true}"
START_NGROK="${START_NGROK:-true}"
SYNC_KEYCLOAK="${SYNC_KEYCLOAK:-true}"
DRY_RUN_MODE=""
ACTION="deploy"
NGROK_PID_FILE="${ROOT_DIR}/.ngrok-tfakkarni.pid"
PORT_FORWARD_PID_FILE="${ROOT_DIR}/.kubectl-frontend-port-forward.pid"

usage() {
  cat <<'EOF'
Usage:
  ./deploy.sh [--dry-run[=client|server]] [--skip-ngrok] [--skip-keycloak-sync]
  ./deploy.sh --down

What deploy does:
  1. Verifies Docker and Kubernetes access.
  2. Applies k8s manifests in dependency order.
  3. Waits for frontend and core rollouts by default.
  4. Exposes frontend on localhost:30080 if needed.
  5. Starts ngrok for localhost:30080.
  6. Syncs the ngrok HTTPS URL into Keycloak frontend clients.

Environment:
  KUBE_NAMESPACE              Namespace, default: tfakkarni
  APPLY_PLACEHOLDER_SECRETS   Apply k8s/02-secrets.yml, default: true for demo
  WAIT_FOR_ROLLOUT            Wait for rollout status, default: true
  START_NGROK                 Start/reuse ngrok, default: true
  SYNC_KEYCLOAK               Run Keycloak client sync, default: true
  IMAGE_PULL_SECRET_NAME      Kubernetes Docker Hub pull secret, default: dockerhub-pull-secret
EOF
}

for arg in "$@"; do
  case "$arg" in
    --down) ACTION="down" ;;
    --dry-run) DRY_RUN_MODE="server" ;;
    --dry-run=client) DRY_RUN_MODE="client" ;;
    --dry-run=server) DRY_RUN_MODE="server" ;;
    --skip-ngrok) START_NGROK="false" ;;
    --skip-keycloak-sync) SYNC_KEYCLOAK="false" ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $arg" >&2; usage >&2; exit 2 ;;
  esac
done

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required but was not found in PATH." >&2
    exit 1
  fi
}

stop_pid_file() {
  local file="$1"
  if [[ -f "$file" ]]; then
    local pid
    pid="$(cat "$file" 2>/dev/null || true)"
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
    rm -f "$file"
  fi
}

down() {
  stop_pid_file "$PORT_FORWARD_PID_FILE"
  stop_pid_file "$NGROK_PID_FILE"
  if command -v kubectl >/dev/null 2>&1; then
    kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
  fi
  echo "Tfakkarni deployment cleanup requested for namespace ${NAMESPACE}."
}

start_docker_if_needed() {
  need_cmd docker
  if docker info >/dev/null 2>&1; then
    return
  fi
  if command -v systemctl >/dev/null 2>&1; then
    echo "Docker is not reachable; trying to start docker.service."
    sudo -n systemctl start docker 2>/dev/null || sudo systemctl start docker || true
  fi
  docker info >/dev/null
}

ensure_kubernetes() {
  need_cmd kubectl
  if kubectl cluster-info >/dev/null 2>&1; then
    return
  fi
  if command -v kind >/dev/null 2>&1; then
    echo "No reachable Kubernetes API; creating/reusing kind cluster ${KIND_CLUSTER_NAME}."
    if ! kind get clusters | grep -qx "$KIND_CLUSTER_NAME"; then
      cat <<EOF | kind create cluster --name "$KIND_CLUSTER_NAME" --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 30080
        protocol: TCP
      - containerPort: 30090
        hostPort: 30090
        protocol: TCP
      - containerPort: 30761
        hostPort: 30761
        protocol: TCP
EOF
    fi
    kubectl cluster-info >/dev/null
    return
  fi
  echo "Kubernetes is not reachable and kind is not installed." >&2
  exit 1
}

kubectl_apply() {
  if [[ -n "$DRY_RUN_MODE" ]]; then
    kubectl apply --dry-run="$DRY_RUN_MODE" -f "$1"
  else
    kubectl apply -f "$1"
  fi
}

load_dockerhub_credentials() {
  [[ -z "$DRY_RUN_MODE" ]] || return 1
  [[ -f "${ROOT_DIR}/.env" ]] || return 1
  readarray -t docker_env < <(python3 - <<'PY' "${ROOT_DIR}/.env"
from pathlib import Path
import sys

keys = {"DOCKERHUB_USERNAME", "DOCKERHUB_PASSWORD"}
values = {}
for raw in Path(sys.argv[1]).read_text(errors="ignore").splitlines():
    line = raw.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    key, value = line.split("=", 1)
    key = key.strip().removeprefix("export ").strip()
    if key in keys:
        values[key] = value.strip().strip('"').strip("'")
for key in sorted(keys):
    print(f"{key}={values.get(key, '')}")
PY
  )
  for kv in "${docker_env[@]}"; do
    case "$kv" in
      DOCKERHUB_USERNAME=*) DOCKERHUB_USERNAME="${kv#DOCKERHUB_USERNAME=}" ;;
      DOCKERHUB_PASSWORD=*) DOCKERHUB_PASSWORD="${kv#DOCKERHUB_PASSWORD=}" ;;
    esac
  done
  [[ -n "${DOCKERHUB_USERNAME:-}" && -n "${DOCKERHUB_PASSWORD:-}" ]]
}

configure_image_pull_secret() {
  [[ -z "$DRY_RUN_MODE" ]] || return 0
  if ! load_dockerhub_credentials; then
    echo "Docker Hub credentials were not found in .env; continuing without imagePullSecret."
    return 0
  fi
  echo "Creating/updating Kubernetes Docker Hub imagePullSecret ${IMAGE_PULL_SECRET_NAME}."
  kubectl create secret docker-registry "$IMAGE_PULL_SECRET_NAME" \
    --namespace "$NAMESPACE" \
    --docker-server='https://index.docker.io/v1/' \
    --docker-username="$DOCKERHUB_USERNAME" \
    --docker-password="$DOCKERHUB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl patch serviceaccount default -n "$NAMESPACE" \
    -p "{\"imagePullSecrets\":[{\"name\":\"$IMAGE_PULL_SECRET_NAME\"}]}" >/dev/null || true
}

patch_deployments_image_pull_secret() {
  [[ -z "$DRY_RUN_MODE" ]] || return 0
  kubectl get secret "$IMAGE_PULL_SECRET_NAME" -n "$NAMESPACE" >/dev/null 2>&1 || return 0
  mapfile -t deployments < <(kubectl get deployments -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
  for deployment in "${deployments[@]}"; do
    [[ -n "$deployment" ]] || continue
    kubectl patch deployment "$deployment" -n "$NAMESPACE" --type=merge \
      -p "{\"spec\":{\"template\":{\"spec\":{\"imagePullSecrets\":[{\"name\":\"$IMAGE_PULL_SECRET_NAME\"}]}}}}" >/dev/null
  done
}

load_kind_cached_images() {
  [[ -z "$DRY_RUN_MODE" ]] || return 0
  command -v kind >/dev/null 2>&1 || return 0
  kind get clusters | grep -qx "$KIND_CLUSTER_NAME" || return 0

  mapfile -t manifest_images < <(python3 - <<'PY' "$MANIFEST_DIR"
from pathlib import Path
import re
import sys

images = []
seen = set()
for path in sorted(Path(sys.argv[1]).glob("*.yml")):
    for line in path.read_text(errors="ignore").splitlines():
        match = re.match(r"\s*image:\s*([^#\s]+)", line)
        if not match:
            continue
        image = match.group(1).strip().strip('"').strip("'")
        if image not in seen:
            seen.add(image)
            images.append(image)
print("\n".join(images))
PY
  )

  for image in "${manifest_images[@]}"; do
    [[ -n "$image" ]] || continue
    if docker image inspect "$image" >/dev/null 2>&1; then
      echo "Loading cached Docker image into kind: ${image}"
      kind load docker-image --name "$KIND_CLUSTER_NAME" "$image"
    fi
  done
}

wait_rollout() {
  [[ "$WAIT_FOR_ROLLOUT" == "true" && -z "$DRY_RUN_MODE" ]] || return 0
  kubectl -n "$NAMESPACE" rollout status "$1" --timeout="${2:-420s}"
}

ensure_local_frontend_port() {
  [[ -z "$DRY_RUN_MODE" ]] || return 0
  if curl -fsS "http://127.0.0.1:${NGROK_PORT}/" >/dev/null 2>&1; then
    return 0
  fi
  stop_pid_file "$PORT_FORWARD_PID_FILE"
  echo "Starting kubectl port-forward svc/frontend ${NGROK_PORT}:80 for ngrok."
  kubectl -n "$NAMESPACE" port-forward svc/frontend "${NGROK_PORT}:80" >/tmp/tfakkarni-frontend-port-forward.log 2>&1 &
  echo $! > "$PORT_FORWARD_PID_FILE"
  for _ in $(seq 1 30); do
    curl -fsS "http://127.0.0.1:${NGROK_PORT}/" >/dev/null 2>&1 && return 0
    sleep 2
  done
  echo "Frontend did not become reachable on localhost:${NGROK_PORT}." >&2
  tail -40 /tmp/tfakkarni-frontend-port-forward.log >&2 || true
  exit 1
}

ngrok_https_url() {
  python3 - "$NGROK_API_URL/api/tunnels" <<'PY'
import json, sys, urllib.request
payload = json.load(urllib.request.urlopen(sys.argv[1], timeout=5))
for tunnel in payload.get("tunnels", []):
    url = tunnel.get("public_url", "")
    if url.startswith("https://"):
        print(url.rstrip("/"))
        raise SystemExit(0)
raise SystemExit(1)
PY
}

start_ngrok() {
  [[ "$START_NGROK" == "true" && -z "$DRY_RUN_MODE" ]] || return 0
  need_cmd ngrok
  if ngrok_https_url >/dev/null 2>&1; then
    return 0
  fi
  stop_pid_file "$NGROK_PID_FILE"
  local config_args=()
  [[ -f "${ROOT_DIR}/ngrok.yml" ]] && config_args=(--config "${ROOT_DIR}/ngrok.yml")
  echo "Starting ngrok for http://127.0.0.1:${NGROK_PORT}."
  ngrok http "${config_args[@]}" "http://127.0.0.1:${NGROK_PORT}" --log=stdout >/tmp/tfakkarni-ngrok.log 2>&1 &
  echo $! > "$NGROK_PID_FILE"
  for _ in $(seq 1 30); do
    ngrok_https_url >/dev/null 2>&1 && return 0
    sleep 2
  done
  echo "ngrok did not expose an HTTPS tunnel." >&2
  tail -80 /tmp/tfakkarni-ngrok.log >&2 || true
  exit 1
}

sync_keycloak() {
  [[ "$SYNC_KEYCLOAK" == "true" && -z "$DRY_RUN_MODE" ]] || return 0
  local public_url
  public_url="$(ngrok_https_url)"
  echo "Syncing Keycloak frontend clients to ngrok URL: ${public_url}"
  "${ROOT_DIR}/scripts/sync-keycloak-ngrok-client.sh" --ngrok-url "$public_url"
}

deploy() {
  start_docker_if_needed
  ensure_kubernetes
  load_kind_cached_images

  echo "Deploying Tfakkarni to namespace ${NAMESPACE}${DRY_RUN_MODE:+ (dry-run=${DRY_RUN_MODE})}."
  kubectl_apply "${MANIFEST_DIR}/00-namespace.yml"
  if [[ "$DRY_RUN_MODE" == "server" ]] && ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    DRY_RUN_MODE="client"
  fi
  kubectl_apply "${MANIFEST_DIR}/01-configmap.yml"
  if [[ "$APPLY_PLACEHOLDER_SECRETS" == "true" || -n "$DRY_RUN_MODE" ]]; then
    kubectl_apply "${MANIFEST_DIR}/02-secrets.yml"
  fi
  configure_image_pull_secret
  kubectl_apply "${MANIFEST_DIR}/03-infrastructure.yml"
  patch_deployments_image_pull_secret
  wait_rollout deployment/discovery-service 420s
  wait_rollout deployment/config-service 420s
  wait_rollout deployment/api-gateway 420s
  kubectl_apply "${MANIFEST_DIR}/04-microservices.yml"
  kubectl_apply "${MANIFEST_DIR}/05-frontend.yml"
  patch_deployments_image_pull_secret
  kubectl_apply "${MANIFEST_DIR}/06-ingress.yml"
  wait_rollout deployment/frontend 420s

  ensure_local_frontend_port
  start_ngrok
  sync_keycloak

  if [[ -z "$DRY_RUN_MODE" ]]; then
    kubectl -n "$NAMESPACE" get pods,svc
    if [[ "$START_NGROK" == "true" ]]; then
      echo "Frontend ngrok URL: $(ngrok_https_url)"
    fi
  fi
}

if [[ "$ACTION" == "down" ]]; then
  down
else
  deploy
fi
