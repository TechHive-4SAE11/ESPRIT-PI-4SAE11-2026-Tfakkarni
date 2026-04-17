# Step 6 — Explore Medical & Game Services

Let's explore two core microservices through the API Gateway.

## Medical Service

The medical service handles **appointments**, **prescriptions**, **medication logs**, and **care plans**.

Check what endpoints are available:

```bash
echo "=== Medical Service Endpoints ==="
curl -s http://localhost:18086/actuator/mappings | python3 -c "
import sys, json
data = json.load(sys.stdin)
contexts = data.get('contexts', {})
for ctx_name, ctx in contexts.items():
    mappings = ctx.get('mappings', {}).get('dispatcherServlets', {}).get('dispatcherServlet', [])
    for m in mappings:
        details = m.get('details', {})
        handler = m.get('handler', '')
        patterns = details.get('requestMappingConditions', {}).get('patterns', [])
        methods = details.get('requestMappingConditions', {}).get('methods', [])
        if patterns and '/api/' in str(patterns) and 'error' not in str(patterns):
            method = methods[0] if methods else 'ANY'
            for p in patterns:
                print(f'  {method:6s} {p}')
" 2>/dev/null | sort | head -30
```

## Game Service

The game service manages **personalized memory games** for patients — an important part of cognitive care.

Check game service endpoints:

```bash
echo "=== Game Service Endpoints ==="
curl -s http://localhost:18082/actuator/mappings | python3 -c "
import sys, json
data = json.load(sys.stdin)
contexts = data.get('contexts', {})
for ctx_name, ctx in contexts.items():
    mappings = ctx.get('mappings', {}).get('dispatcherServlets', {}).get('dispatcherServlet', [])
    for m in mappings:
        details = m.get('details', {})
        patterns = details.get('requestMappingConditions', {}).get('patterns', [])
        methods = details.get('requestMappingConditions', {}).get('methods', [])
        if patterns and '/api/' in str(patterns) and 'error' not in str(patterns):
            method = methods[0] if methods else 'ANY'
            for p in patterns:
                print(f'  {method:6s} {p}')
" 2>/dev/null | sort | head -30
```

## View Docker logs

Check recent logs from any service:

```bash
echo "=== Recent Medical Service Logs ==="
docker logs tfakkarni-medical --tail 20
```

```bash
echo "=== Recent Game Service Logs ==="
docker logs tfakkarni-game --tail 20
```

## Service-to-service communication

The services communicate with each other through **Eureka + OpenFeign**. For example:
- The **medical service** calls the **user service** to verify patient identity
- The **alert service** calls the **tracking service** to correlate location with alerts
- The **analytics service** aggregates data from **game**, **medical**, and **tracking** services

This is all handled transparently via **Spring Cloud** — services discover each other through Eureka and make REST calls using Feign clients.

## Full architecture check

Let's do a final sweep of all services:

```bash
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║        Tfakkarni Platform — Service Status           ║"
echo "╠══════════════════════════════════════════════════════╣"

for svc in \
  "Discovery:8761" "Config:8888" "Gateway:9090" \
  "User:18081" "Game:18082" "Tracking:18083" \
  "Alert:18084" "ML:18085" "Medical:18086" \
  "MedValidation:18087" "IoT:18088" "Assistant:18089" \
  "Analytics:18090" "Frontend:18080"; do

  name="${svc%%:*}"
  port="${svc##*:}"
  if curl -sf -o /dev/null "http://localhost:$port/actuator/health" 2>/dev/null || \
     curl -sf -o /dev/null "http://localhost:$port" 2>/dev/null; then
    printf "║  ✅ %-18s  port %-5s  UP              ║\n" "$name" "$port"
  else
    printf "║  ❌ %-18s  port %-5s  DOWN            ║\n" "$name" "$port"
  fi
done

echo "╚══════════════════════════════════════════════════════╝"
echo ""
```

Congratulations! You now have the entire Tfakkarni platform running.
