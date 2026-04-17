# Step 1 — Deploy Infrastructure Services

The Tfakkarni platform uses **Spring Cloud** for microservice orchestration. Before starting the application services, we need to bring up the infrastructure layer:

- **Eureka Discovery Service** — service registry where all microservices register themselves
- **Config Service** — centralized configuration server

## Start infrastructure

First, navigate to the project directory:

```bash
cd /root/tfakkarni
```

Rename the env file so Docker Compose picks it up:

```bash
cp env-file .env
```

Pull and start only the infrastructure services:

```bash
docker compose up -d discovery-service config-service
```

## Wait for services to be healthy

The discovery service takes about 30-60 seconds to start. Let's wait for it:

```bash
./wait-for-service.sh discovery-service 8761 120
```

Now wait for the config service:

```bash
./wait-for-service.sh config-service 8888 120
```

## Verify Eureka is running

Open the Eureka dashboard to see the service registry:

```bash
curl -s http://localhost:8761/actuator/health | python3 -m json.tool
```

You should see `{"status":"UP"}`.

> **Tip**: The Eureka web dashboard is also available at port 8761 if you open it in a browser tab.
