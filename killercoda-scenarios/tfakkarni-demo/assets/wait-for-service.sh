#!/bin/bash
# wait-for-service.sh — Wait until a service health endpoint returns UP
# Usage: ./wait-for-service.sh <service-name> <port> [timeout-seconds]

SERVICE_NAME="${1:-service}"
PORT="${2:-8080}"
TIMEOUT="${3:-120}"

echo "⏳ Waiting for $SERVICE_NAME on port $PORT (timeout: ${TIMEOUT}s)..."

ELAPSED=0
INTERVAL=5

while [ $ELAPSED -lt $TIMEOUT ]; do
  # Try actuator health first, fall back to root
  if curl -sf "http://localhost:$PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ $SERVICE_NAME is UP (took ${ELAPSED}s)"
    exit 0
  elif curl -sf -o /dev/null "http://localhost:$PORT" 2>/dev/null; then
    echo "✅ $SERVICE_NAME is responding (took ${ELAPSED}s)"
    exit 0
  fi

  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
  echo "   ... still waiting ($ELAPSED/${TIMEOUT}s)"
done

echo "❌ $SERVICE_NAME did not start within ${TIMEOUT}s"
exit 1
