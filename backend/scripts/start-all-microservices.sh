#!/usr/bin/env bash
# Run every Spring Boot microservice in one container (same network namespace).
# Order: Eureka → Config → others in parallel. Assistant uses SERVER_PORT=18089 (iot uses 18088).

set -u

JDIR="${TFAKKARNI_JARS:-/opt/tfakkarni/jars}"
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:-http://127.0.0.1:8761/eureka/}"
export SPRING_CONFIG_IMPORT="${SPRING_CONFIG_IMPORT:-optional:configserver:http://127.0.0.1:8888}"
export EUREKA_INSTANCE_PREFER_IP_ADDRESS="${EUREKA_INSTANCE_PREFER_IP_ADDRESS:-true}"
export EUREKA_INSTANCE_HOSTNAME="${EUREKA_INSTANCE_HOSTNAME:-127.0.0.1}"

wait_tcp() {
  local host=$1 port=$2 max=${3:-120}
  local i=0
  while (( i < max )); do
    if bash -c "echo >/dev/tcp/${host}/${port}" 2>/dev/null; then
      return 0
    fi
    sleep 1
    ((i++)) || true
  done
  echo "Timeout waiting for ${host}:${port}" >&2
  return 1
}

pids=()
cleanup() {
  echo "Stopping Java processes..." >&2
  for pid in "${pids[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  sleep 2
  for pid in "${pids[@]:-}"; do
    kill -9 "$pid" 2>/dev/null || true
  done
}
trap cleanup SIGTERM SIGINT

echo "Starting discovery-service..." >&2
java -jar "${JDIR}/discovery-service.jar" &
pids+=("$!")
wait_tcp 127.0.0.1 8761

echo "Starting config-service..." >&2
java -jar "${JDIR}/config-service.jar" &
pids+=("$!")
wait_tcp 127.0.0.1 8888

echo "Starting remaining services..." >&2
for svc in api-gateway user-service game-service tracking-service alert-service ml-service medical-service medicament-validation-service iot-service analytics-service; do
  java -jar "${JDIR}/${svc}.jar" &
  pids+=("$!")
  sleep 1
done

# Config server may map assistant to 18088; iot already uses 18088 in this stack.
echo "Starting assistant-service on port 18089..." >&2
SERVER_PORT=18089 java -jar "${JDIR}/assistant-service.jar" &
pids+=("$!")

echo "All services started. PIDs: ${pids[*]}" >&2
wait
