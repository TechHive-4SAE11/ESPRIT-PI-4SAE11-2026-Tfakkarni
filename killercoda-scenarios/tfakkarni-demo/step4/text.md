# Step 4 — Explore the API Gateway & Eureka

Now that everything is running, let's explore how the microservice architecture works.

## Eureka Dashboard

Check all registered services:

```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | python3 -c "
import sys, json
data = json.load(sys.stdin)
apps = data.get('applications', {}).get('application', [])
for app in sorted(apps, key=lambda a: a['name']):
    name = app['name']
    count = len(app.get('instance', []))
    status = app['instance'][0]['status']
    print(f'  [{status:4s}] {name}')
print(f'\n  Total: {len(apps)} services')
"
```

## API Gateway routing

The API Gateway at port **9090** routes requests to the correct microservice. Here's how the routing works:

| URL Pattern | Routed to |
|-------------|-----------|
| `/api/users/**` | User Service (18081) |
| `/api/games/**` | Game Service (18082) |
| `/api/tracking/**` | Tracking Service (18083) |
| `/api/alerts/**` | Alert Service (18084) |
| `/api/ml/**` | ML Service (18085) |
| `/api/medical/**` | Medical Service (18086) |

## Test a direct call vs. gateway call

Direct call to the game service:

```bash
curl -s http://localhost:18082/actuator/info | python3 -m json.tool 2>/dev/null || echo "Service responding"
```

Same call through the gateway:

```bash
curl -s http://localhost:9090/actuator/health | python3 -m json.tool
```

The gateway adds **JWT validation**, **CORS handling**, and **load balancing** on top of simple routing.

## Check resource usage

See how much memory each service is using:

```bash
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | sort
```
