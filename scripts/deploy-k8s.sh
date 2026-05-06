#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${KUBE_NAMESPACE:-tfakkarni}"
MANIFEST_DIR="${MANIFEST_DIR:-k8s}"
DRY_RUN_MODE="${DRY_RUN_MODE:-}"

usage() {
  cat <<'EOF'
Usage: scripts/deploy-k8s.sh [--dry-run[=client|server]]

Environment:
  KUBE_NAMESPACE              Kubernetes namespace, default: tfakkarni
  MANIFEST_DIR                Manifest directory, default: k8s
  APPLY_PLACEHOLDER_SECRETS   Set true only for dry-run/demo clusters. Default: false
EOF
}

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN_MODE="server" ;;
    --dry-run=client) DRY_RUN_MODE="client" ;;
    --dry-run=server) DRY_RUN_MODE="server" ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $arg" >&2; usage >&2; exit 2 ;;
  esac
done

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl is required. Install it first: https://kubernetes.io/docs/tasks/tools/" >&2
  exit 1
fi

kubectl_apply() {
  if [[ -n "${DRY_RUN_MODE}" ]]; then
    kubectl apply --dry-run="${DRY_RUN_MODE}" -f "$1"
  else
    kubectl apply -f "$1"
  fi
}

echo "Deploying Tfakkarni manifests to namespace ${NAMESPACE}${DRY_RUN_MODE:+ (dry-run=${DRY_RUN_MODE})}"

# Server dry-run does not persist the namespace, so validate namespace first and then
# use client dry-run for namespaced resources unless the namespace already exists.
kubectl_apply "${MANIFEST_DIR}/00-namespace.yml"
if [[ "${DRY_RUN_MODE}" == "server" ]] && ! kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
  echo "Namespace ${NAMESPACE} does not exist; switching remaining validation to dry-run=client."
  DRY_RUN_MODE="client"
fi

kubectl_apply "${MANIFEST_DIR}/01-configmap.yml"

if [[ "${APPLY_PLACEHOLDER_SECRETS:-false}" == "true" || -n "${DRY_RUN_MODE}" ]]; then
  kubectl_apply "${MANIFEST_DIR}/02-secrets.yml"
else
  echo "Skipping ${MANIFEST_DIR}/02-secrets.yml by default because it contains placeholders."
  echo "Create/update real secrets first, or set APPLY_PLACEHOLDER_SECRETS=true only for dry-run/demo clusters."
fi

# Apply core infrastructure before the rest of the app.  Kubernetes does not
# provide a native "start this Deployment before that Deployment" ordering rule,
# so this deploy script enforces the practical dependency order by waiting for
# each foundation service to become Available before applying downstream
# microservices and the frontend.
kubectl_apply "${MANIFEST_DIR}/03-infrastructure.yml"

if [[ -z "${DRY_RUN_MODE}" ]]; then
  echo "Waiting for foundation services in dependency order: discovery-service (Eureka) -> config-service -> api-gateway"
  kubectl -n "${NAMESPACE}" rollout status deployment/discovery-service --timeout=300s
  kubectl -n "${NAMESPACE}" rollout status deployment/config-service --timeout=300s
  kubectl -n "${NAMESPACE}" rollout status deployment/api-gateway --timeout=300s
else
  echo "Dry-run mode: skipping rollout waits for foundation services."
fi

kubectl_apply "${MANIFEST_DIR}/04-microservices.yml"
kubectl_apply "${MANIFEST_DIR}/05-frontend.yml"

cat <<EOF

Applied manifests${DRY_RUN_MODE:+ in dry-run mode}. Deployment ordering rule:
  1. discovery-service (Eureka)
  2. config-service
  3. api-gateway
  4. remaining microservices
  5. frontend

Useful rollout commands:
  kubectl -n ${NAMESPACE} get pods,svc
  kubectl -n ${NAMESPACE} rollout status deployment/discovery-service
  kubectl -n ${NAMESPACE} rollout status deployment/config-service
  kubectl -n ${NAMESPACE} rollout status deployment/api-gateway
  kubectl -n ${NAMESPACE} rollout status deployment/game-service
  kubectl -n ${NAMESPACE} rollout status deployment/frontend

Per-service image update example:
  kubectl -n ${NAMESPACE} set image deployment/game-service game-service=thelime1/tfakkarni:game-<build-number>
EOF
