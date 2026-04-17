# Step 2 — Deploy Backend Microservices

Now that the infrastructure is up, let's deploy all the backend microservices. They will automatically register with Eureka and fetch their configuration from the Config Service.

## Start the API Gateway

The gateway is the single entry point for all API calls:

```bash
cd /root/tfakkarni
docker compose up -d api-gateway
```

Wait for it to be ready:

```bash
./wait-for-service.sh api-gateway 9090 120
```

## Start all application services

Now bring up the rest of the backend:

```bash
docker compose up -d user-service game-service medical-service alert-service tracking-service ml-service medicament-validation-service iot-service assistant-service analytics-service
```

This starts **10 microservices** in parallel. They'll each take 30-90 seconds to boot.

Let's wait for a few key services:

```bash
./wait-for-service.sh user-service 18081 180
./wait-for-service.sh game-service 18082 180
./wait-for-service.sh medical-service 18086 180
./wait-for-service.sh ml-service 18085 180
```

## Check all containers

```bash
docker compose ps
```

You should see all services with status `Up (healthy)` or `Up`.

## Verify Eureka registrations

Check how many services have registered with Eureka:

```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | python3 -c "
import sys, json
data = json.load(sys.stdin)
apps = data.get('applications', {}).get('application', [])
print(f'\n=== {len(apps)} services registered with Eureka ===\n')
for app in apps:
    name = app['name']
    instances = app.get('instance', [])
    for inst in instances:
        print(f'  {name:30s} → {inst[\"homePageUrl\"]}')
print()
"
```

You should see all your microservices listed!
