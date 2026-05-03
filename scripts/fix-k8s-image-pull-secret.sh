#!/usr/bin/env bash
set -euo pipefail
cd /home/kali/Desktop/ESPRIT-PI-4SAE11-2026-Tfakkarni
mkdir -p logs
REPORT=logs/deployment-pull-secret-report.md
LOG=logs/deployment-pull-secret-commands.log
SECRET_NAME=dockerhub-pull-secret
NS=tfakkarni
{
  echo "# Deployment pull-secret report"
  echo
  echo "Updated: $(TZ='Africa/Tunis' date -Iseconds)"
  echo
  echo "## Actions"
} > "$REPORT"
{
  echo "[$(TZ='Africa/Tunis' date -Iseconds)] starting pull-secret fix"
} > "$LOG"

# Load only Docker Hub env keys safely; do NOT source the whole .env because
# unrelated secrets may contain shell metacharacters and break `source`.
readarray -t docker_env < <(python3 - <<'PY'
from pathlib import Path
keys = {'DOCKERHUB_USERNAME', 'DOCKERHUB_PASSWORD'}
vals = {}
for raw in Path('.env').read_text(errors='ignore').splitlines():
    line = raw.strip()
    if not line or line.startswith('#') or '=' not in line:
        continue
    k, v = line.split('=', 1)
    k = k.strip().removeprefix('export ').strip()
    if k in keys:
        vals[k] = v.strip().strip('"').strip("'")
for k in sorted(keys):
    print(f'{k}={vals.get(k, "")}')
PY
)
for kv in "${docker_env[@]}"; do
  case "$kv" in
    DOCKERHUB_USERNAME=*) DOCKERHUB_USERNAME="${kv#DOCKERHUB_USERNAME=}" ;;
    DOCKERHUB_PASSWORD=*) DOCKERHUB_PASSWORD="${kv#DOCKERHUB_PASSWORD=}" ;;
  esac
done

if [ -z "${DOCKERHUB_USERNAME:-}" ] || [ -z "${DOCKERHUB_PASSWORD:-}" ]; then
  {
    echo "- BLOCKED: .env does not provide DOCKERHUB_USERNAME and DOCKERHUB_PASSWORD."
  } >> "$REPORT"
  exit 2
fi

kubectl get namespace "$NS" >/dev/null

echo "- Verified Docker Hub credential variables exist in .env (values hidden)." >> "$REPORT"

# Create/update image pull secret without printing credentials.
kubectl create secret docker-registry "$SECRET_NAME" \
  --namespace "$NS" \
  --docker-server='https://index.docker.io/v1/' \
  --docker-username="$DOCKERHUB_USERNAME" \
  --docker-password="$DOCKERHUB_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f - >> "$LOG" 2>&1

echo "- Created/updated imagePullSecret \`$SECRET_NAME\` in namespace \`$NS\`." >> "$REPORT"

# Patch default SA idempotently.
kubectl patch serviceaccount default -n "$NS" \
  -p "{\"imagePullSecrets\":[{\"name\":\"$SECRET_NAME\"}]}" >> "$LOG" 2>&1 || true

echo "- Patched default service account with imagePullSecret \`$SECRET_NAME\`." >> "$REPORT"

# Patch every deployment to include imagePullSecrets idempotently.
mapfile -t deployments < <(kubectl get deploy -n "$NS" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}')
patched=0
for d in "${deployments[@]}"; do
  [ -z "$d" ] && continue
  kubectl patch deployment "$d" -n "$NS" --type=merge \
    -p "{\"spec\":{\"template\":{\"spec\":{\"imagePullSecrets\":[{\"name\":\"$SECRET_NAME\"}]}}}}" >> "$LOG" 2>&1 || true
  patched=$((patched+1))
done

echo "- Patched $patched deployments with imagePullSecret \`$SECRET_NAME\`." >> "$REPORT"

# Restart deployments to trigger repull.
if [ "$patched" -gt 0 ]; then
  kubectl rollout restart deployment -n "$NS" >> "$LOG" 2>&1 || true
  echo "- Restarted deployments in namespace \`$NS\` to retry image pulls." >> "$REPORT"
fi

{
  echo
  echo "## Immediate status after restart"
  echo
  echo '```text'
  kubectl get pods -n "$NS" -o wide || true
  echo '```'
  echo
  echo "## Waiting for pull retry"
} >> "$REPORT"

# Bounded wait/status checks (do not fail if not ready).
for i in 1 2 3 4 5 6; do
  sleep 20
  {
    echo
    echo "### Check $i — $(TZ='Africa/Tunis' date -Iseconds)"
    echo
    echo '```text'
    kubectl get pods -n "$NS" --no-headers || true
    echo '```'
  } >> "$REPORT"
done

NODE_IP=$(kubectl get node tfakkarni-devops-control-plane -o jsonpath='{.status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null || echo 172.18.0.2)
{
  echo
  echo "## Link probes"
  echo
  for entry in "Frontend http://$NODE_IP:30080" "API Gateway http://$NODE_IP:30090" "Discovery http://$NODE_IP:30761"; do
    name=${entry%% http*}; url=${entry#* }
    status=$(curl -sS -I --max-time 5 "$url" 2>&1 | head -1 || true)
    echo "- $name: \`$url\` — ${status:-no response}"
  done
  echo
  echo "## Final pod summary"
  echo
  echo '```text'
  kubectl get pods -n "$NS" -o wide || true
  echo '```'
  echo
  echo "## Pull-secret verification"
  echo
  echo '```text'
  kubectl get secret "$SECRET_NAME" -n "$NS" || true
  kubectl get sa default -n "$NS" -o jsonpath='{.imagePullSecrets}' || true
  echo
  echo '```'
} >> "$REPORT"

echo "[$(TZ='Africa/Tunis' date -Iseconds)] done" >> "$LOG"
