# Docker Documentation — Tfakkarni Project

## Table of Contents

1. [What Is Docker?](#1-what-is-docker)
2. [Core Docker Concepts](#2-core-docker-concepts)
3. [Dockerfile Deep Dive](#3-dockerfile-deep-dive)
4. [Docker Compose Deep Dive](#4-docker-compose-deep-dive)
5. [Project Docker Architecture Overview](#5-project-docker-architecture-overview)
6. [Dockerfiles in This Project](#6-dockerfiles-in-this-project)
   - [Backend Generic Dockerfile](#61-backend-generic-dockerfile-backenddockerfile)
   - [Backend Stack Dockerfile](#62-backend-stack-dockerfile-backenddockerfilestack)
   - [Per-Service Dockerfiles](#63-per-service-dockerfiles-backendservice-namedockerfile)
   - [Frontend Dockerfile](#64-frontend-dockerfile-frontenddockerfile)
   - [IoT Streamer Dockerfile](#65-iot-streamer-dockerfile-iotdockerfile)
7. [Docker Compose Files in This Project](#7-docker-compose-files-in-this-project)
   - [Backend Compose](#71-backend-compose-docker-composebackendyml)
   - [Frontend Compose](#72-frontend-compose-docker-composefrontendyml)
8. [.dockerignore Files](#8-dockerignore-files)
9. [Nginx Configuration (Frontend)](#9-nginx-configuration-frontend)
10. [How to Run the Project with Docker](#10-how-to-run-the-project-with-docker)
11. [Useful Docker Commands](#11-useful-docker-commands)
12. [Port Mapping Reference](#12-port-mapping-reference)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. What Is Docker?

**Docker** is an open-source platform that automates the deployment, scaling, and management of applications inside lightweight, portable **containers**. A container packages your application code together with all of its dependencies (runtime, libraries, system tools, configuration files) so it runs reliably regardless of the host environment.

### Why Docker?

| Problem Without Docker | Solution With Docker |
|---|---|
| "It works on my machine" syndrome | Containers are identical everywhere — dev, CI, production |
| Complex environment setup (Java 17, Node 20, Python 3.12, Nginx…) | Each service's Dockerfile declares its own environment |
| Dependency conflicts between services | Containers are isolated — each has its own filesystem & process tree |
| Hard to reproduce production bugs locally | Run the exact same images locally that run in production |
| Slow onboarding for new developers | One `docker compose up` command starts the entire platform |

### Docker vs Virtual Machines

```
┌─────────────────────────────────────────────┐
│               Virtual Machines              │
│  ┌────────┐ ┌────────┐ ┌────────┐          │
│  │  App A │ │  App B │ │  App C │          │
│  │  Libs  │ │  Libs  │ │  Libs  │          │
│  │ GuestOS│ │ GuestOS│ │ GuestOS│          │
│  └────────┘ └────────┘ └────────┘          │
│          Hypervisor (VMware, VBox)           │
│               Host OS + Hardware            │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│                 Containers                  │
│  ┌────────┐ ┌────────┐ ┌────────┐          │
│  │  App A │ │  App B │ │  App C │          │
│  │  Libs  │ │  Libs  │ │  Libs  │          │
│  └────────┘ └────────┘ └────────┘          │
│            Docker Engine (daemon)            │
│               Host OS + Hardware            │
└─────────────────────────────────────────────┘
```

Containers share the host kernel, making them **much lighter** (MB vs GB), **faster to start** (seconds vs minutes), and more resource-efficient than VMs.

---

## 2. Core Docker Concepts

### Image

A **Docker image** is a read-only template containing instructions for creating a container. Think of it as a snapshot of a filesystem plus metadata (what command to run, what port to expose, etc.). Images are built from Dockerfiles and can be stored in registries (Docker Hub, GitHub Container Registry).

### Container

A **container** is a running instance of an image. You can start, stop, restart, and delete containers. Multiple containers can run from the same image. Each container has its own isolated filesystem, network, and process space.

### Dockerfile

A **Dockerfile** is a text file containing a series of instructions that Docker uses to build an image. Each instruction creates a **layer** in the image. Layers are cached, so unchanged layers are reused on subsequent builds (this is why ordering matters).

### Docker Compose

**Docker Compose** is a tool for defining and running multi-container applications. You describe your services in a `docker-compose.yml` (or any `.yml`) file, and a single command (`docker compose up`) creates and starts all of them with the correct networking, volumes, dependencies, and environment variables.

### Volume

A **volume** is a persistent storage mechanism. Container filesystems are ephemeral (lost when the container is removed), but volumes persist data between container restarts and removals.

### Network

Docker creates isolated **networks** so containers can communicate by service name (DNS resolution). Containers on different networks cannot reach each other by default.

### Registry

A **registry** is a storage and distribution system for Docker images. Docker Hub is the default public registry. You can also use private registries.

### Build Context

The **build context** is the set of files and directories sent to the Docker daemon when building an image. It's typically the directory containing the Dockerfile. The `.dockerignore` file controls what's excluded.

---

## 3. Dockerfile Deep Dive

### Common Instructions

| Instruction | Purpose | Example |
|---|---|---|
| `FROM` | Sets the base image | `FROM eclipse-temurin:17-jre-jammy` |
| `WORKDIR` | Sets the working directory inside the container | `WORKDIR /app` |
| `COPY` | Copies files from build context into the image | `COPY pom.xml .` |
| `RUN` | Executes a command during build (creates a new layer) | `RUN mvn package -DskipTests` |
| `ENV` | Sets environment variables | `ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"` |
| `EXPOSE` | Documents which port the container listens on | `EXPOSE 8761` |
| `ENTRYPOINT` | Defines the command that runs when the container starts | `ENTRYPOINT ["java", "-jar", "app.jar"]` |
| `CMD` | Provides default arguments to ENTRYPOINT (or a default command) | `CMD ["--server.port=8080"]` |
| `ARG` | Defines build-time variables (available only during build) | `ARG MODULE=discovery-service` |

### Multi-Stage Builds

Multi-stage builds use multiple `FROM` instructions. Each `FROM` starts a new stage. You can copy artifacts from earlier stages into later ones, keeping the final image small:

```dockerfile
# Stage 1: Build (has Maven + JDK — large image)
FROM maven:3.9.9-eclipse-temurin-17 AS build
RUN mvn package

# Stage 2: Run (only JRE — small image)
FROM eclipse-temurin:17-jre-jammy
COPY --from=build /workspace/target/app.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

The final image only contains what's in the last stage. The build tools (Maven, npm, etc.) are discarded.

### Layer Caching

Docker caches each layer. If a layer's instruction and its input files haven't changed, Docker reuses the cached version. This is why Dockerfiles copy dependency files (e.g., `pom.xml`, `package.json`) before source code — dependencies change less often, so the dependency-install layer stays cached.

### BuildKit Cache Mounts

```dockerfile
RUN --mount=type=cache,target=/root/.m2 mvn package
```

This keeps the Maven local repository (`~/.m2`) in a persistent cache volume across builds. Without it, every build re-downloads all dependencies.

---

## 4. Docker Compose Deep Dive

### Key Compose Concepts

| Concept | Description |
|---|---|
| `services` | Each service maps to one container (e.g., `discovery-service`, `api-gateway`) |
| `build` | Specifies the build context and Dockerfile path |
| `image` | Names the built image for tagging/pushing |
| `ports` | Maps host ports to container ports (`"host:container"`) |
| `networks` | Assigns the service to one or more networks |
| `depends_on` | Controls startup order (with optional health conditions) |
| `healthcheck` | Defines how Docker checks if a container is healthy |
| `env_file` | Loads environment variables from a file (e.g., `.env`) |
| `environment` | Sets individual environment variables |
| `restart` | Restart policy (`no`, `always`, `unless-stopped`, `on-failure`) |
| `deploy.resources.limits` | Resource constraints (memory, CPU) |

### Service Dependencies & Health Checks

```yaml
depends_on:
  config-service: { condition: service_healthy }
```

This means the service won't start until `config-service` passes its health check. This is critical in microservice architectures where services depend on Config Server and Eureka being ready.

### Networks

```yaml
networks:
  tfakkarni-network:
    name: tfakkarni-network
    driver: bridge
```

All services share the `tfakkarni-network` bridge network. Within this network, services discover each other by their **service name** (e.g., `http://discovery-service:8761`).

---

## 5. Project Docker Architecture Overview

Tfakkarni uses a **microservices architecture** with Docker. The project is split into two Docker Compose files:

```
                        ┌────────────────────────┐
                        │      Frontend          │
                        │  (Angular + Nginx)     │
                        │  Port: 18080 → 80      │
                        └──────────┬─────────────┘
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────┐
│                     tfakkarni-network (bridge)                   │
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐     │
│  │  Discovery    │   │   Config     │   │   API Gateway    │     │
│  │  (Eureka)    │◄──│   Service    │◄──│   Port: 9090     │     │
│  │  Port: 8761  │   │   Port: 8888 │   └────────┬─────────┘     │
│  └──────────────┘   └──────────────┘            │               │
│                                                  │               │
│         ┌────────────────────────────────────────┤               │
│         │            │            │              │               │
│         ▼            ▼            ▼              ▼               │
│  ┌────────────┐┌───────────┐┌──────────┐┌────────────┐          │
│  │   User     ││   Game    ││ Medical  ││   Alert    │          │
│  │  :18081   ││  :18082  ││ :18086  ││  :18084   │          │
│  └────────────┘└───────────┘└──────────┘└────────────┘          │
│  ┌────────────┐┌───────────┐┌──────────┐┌────────────┐          │
│  │ Tracking   ││    ML     ││  Med-Val ││    IoT     │          │
│  │  :18083   ││  :18085  ││ :18087  ││  :18088   │          │
│  └────────────┘└───────────┘└──────────┘└────────────┘          │
│  ┌────────────┐┌───────────┐┌───────────────────────┐           │
│  │ Assistant  ││ Analytics ││   IoT Streamer        │           │
│  │  :18089   ││  :18090  ││  (Python mock)        │           │
│  └────────────┘└───────────┘└───────────────────────┘           │
└──────────────────────────────────────────────────────────────────┘
```

### Startup Order

1. **Discovery Service** (Eureka) — must be healthy first
2. **Config Service** — depends on Discovery, must be healthy
3. **API Gateway** — depends on Config Service
4. **All Microservices** — depend on Config Service
5. **IoT Streamer** — independent Python container

---

## 6. Dockerfiles in This Project

### 6.1 Backend Generic Dockerfile (`backend/Dockerfile`)

**Location:** `backend/Dockerfile`
**Purpose:** A parameterized Dockerfile that can build **any single microservice** using a `MODULE` build argument.

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE=discovery-service
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package -pl "${MODULE}"

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG MODULE=discovery-service
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends findutils && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/target /workspace-target
RUN JAR="$(find /workspace-target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -1)" && \
    test -n "$JAR" && cp "$JAR" /app/app.jar && rm -rf /workspace-target
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**How it works:**

| Step | What Happens |
|---|---|
| **Stage 1 — Build** | Uses `maven:3.9.9-eclipse-temurin-17` as the build environment. Copies the entire backend source. Runs `mvn package` only for the specified `MODULE` (`-pl` flag = "project list"). The `.m2` cache mount persists Maven dependencies across builds. |
| **Stage 2 — Runtime** | Uses the minimal `eclipse-temurin:17-jre-jammy` image (JRE only, no JDK). Copies the built JAR from Stage 1. Uses `find` to locate the runnable Spring Boot JAR (excluding `-plain.jar` variants). |
| **`MaxRAMPercentage=75.0`** | Tells the JVM to use at most 75% of the container's memory limit, leaving room for OS overhead and avoiding OOM kills. |

**Usage:**
```bash
docker build --build-arg MODULE=medical-service -t tfakkarni/medical-service backend/
```

---

### 6.2 Backend Stack Dockerfile (`backend/Dockerfile.stack`)

**Location:** `backend/Dockerfile.stack`
**Purpose:** Builds **ALL microservices** into a **single Docker image**. Useful for running the entire backend in one container (dev/demo).

**How it works:**

| Step | What Happens |
|---|---|
| **Layer 1 — POM caching** | Copies only `pom.xml` files first (parent + all modules). Runs `mvn dependency:go-offline` to download all dependencies. This layer is cached and reused unless a POM changes. |
| **Layer 2 — Full build** | Copies all source code and runs `mvn package` for all modules. The `.m2` cache mount avoids re-downloading dependencies. |
| **JAR collection** | Iterates over all 13 modules, finds each runnable JAR, and copies them to `/jars/<service-name>.jar`. |
| **Runtime stage** | Uses `eclipse-temurin:17-jre-jammy`. Installs `tini` (a tiny init system for proper signal handling). Copies all JARs and the `start-all-microservices.sh` script. |
| **Entrypoint** | `tini` runs the startup script that launches all services in the correct order. |

**All services built into this image:**
- discovery-service, config-service, api-gateway
- user-service, game-service, tracking-service, alert-service
- ml-service, medical-service, medicament-validation-service
- iot-service, assistant-service, analytics-service

**Exposed ports:** 8761, 8888, 9090, 18081–18090

**Usage:**
```bash
docker build -f backend/Dockerfile.stack -t tfakkarni/tfakkarni-backend:local backend/
docker run -p 8761:8761 -p 9090:9090 --env-file .env tfakkarni/tfakkarni-backend:local
```

---

### 6.3 Per-Service Dockerfiles (`backend/<service-name>/Dockerfile`)

Each microservice has its own Dockerfile that follows the same pattern. These are used by `docker-compose.backend.yml`.

**Services with individual Dockerfiles:**

| Service | Dockerfile | Exposed Port |
|---|---|---|
| Discovery Service (Eureka) | `backend/discovery-service/Dockerfile` | 8761 |
| Config Service | `backend/config-service/Dockerfile` | 8888 |
| API Gateway | `backend/api-gateway/Dockerfile` | 9090 |
| User Service | `backend/user-service/Dockerfile` | 18081 |
| Game Service | `backend/game-service/Dockerfile` | 18082 |
| Tracking Service | `backend/tracking-service/Dockerfile` | 18083 |
| Alert Service | `backend/alert-service/Dockerfile` | 18084 |
| ML Service | `backend/ml-service/Dockerfile` | 18085 |
| Medical Service | `backend/medical-service/Dockerfile` | 18086 |
| Medicament Validation Service | `backend/medicament-validation-service/Dockerfile` | 18087 |
| IoT Service | `backend/iot-service/Dockerfile` | 18088 |
| Assistant Service | `backend/assistant-service/Dockerfile` | 18089 |
| Analytics Service | `backend/analytics-service/Dockerfile` | 18090 |

**Common pattern for all per-service Dockerfiles:**

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1. Copy ALL module POMs (Maven multi-module needs the full reactor structure)
COPY pom.xml .
COPY discovery-service/pom.xml discovery-service/
COPY config-service/pom.xml config-service/
# ... (all other modules)

# 2. Cache dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 3. Copy ONLY this service's source and build it
COPY <service-name>/ <service-name>/
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B -pl <service-name>

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/<service-name>/target /tmp/target
RUN find /tmp/target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -1 | xargs -I{} cp {} /app/app.jar && rm -rf /tmp/target
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key design decisions:**

1. **Why copy ALL `pom.xml` files?** — Maven multi-module projects require the parent POM and all module POMs to resolve the reactor structure, even when building a single module with `-pl`.

2. **Why `dependency:go-offline` before copying source?** — This separates the "download dependencies" step from the "compile code" step. Since dependencies change rarely, this layer is almost always cached, making rebuilds after code changes very fast.

3. **Why install `curl`?** — The Docker Compose healthchecks use `curl` to probe the Spring Boot Actuator health endpoint.

4. **Why `find` + `xargs` for the JAR?** — Spring Boot builds produce both a runnable fat JAR and a `-plain.jar` (without embedded dependencies). The `find` command locates the correct runnable JAR regardless of its exact version-stamped filename.

---

### 6.4 Frontend Dockerfile (`frontend/Dockerfile`)

**Location:** `frontend/Dockerfile`
**Build context:** Repository root (not `frontend/`)
**Purpose:** Builds the Angular 18 SPA and serves it with Nginx.

```dockerfile
FROM node:20-bookworm-slim AS build
WORKDIR /workspace
ENV NPM_CONFIG_LEGACY_PEER_DEPS=true

ARG API_BASE_URL=http://localhost:9090
ARG KEYCLOAK_URL=https://lemur-12.cloud-iam.com/auth
ARG KEYCLOAK_CLIENT_ID=tfakkarni-frontend

COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./frontend/
RUN cd frontend && npm ci --legacy-peer-deps || (... fallback ...)

RUN printf 'API_BASE_URL=%s\nKEYCLOAK_URL=%s\nKEYCLOAK_CLIENT_ID=%s\n' \
    "${API_BASE_URL}" "${KEYCLOAK_URL}" "${KEYCLOAK_CLIENT_ID}" > frontend/.env

COPY frontend ./frontend
WORKDIR /workspace/frontend
RUN npm run build

# Copy compiled static assets
RUN mkdir -p /static && \
    if [ -d dist/frontend/browser ]; then cp -a dist/frontend/browser/. /static/; \
    else cp -a dist/frontend/. /static/; fi

FROM nginx:1.27-alpine
COPY --from=build /static /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**How it works:**

| Step | What Happens |
|---|---|
| **Stage 1 — Build** | Uses `node:20-bookworm-slim`. Installs npm dependencies with `--legacy-peer-deps` (needed for Keycloak/Apex/FullCalendar peer dependency conflicts). Creates a `.env` file with configurable API and Keycloak URLs passed as build args. Runs `npm run build` to produce the production Angular bundle. |
| **Stage 2 — Serve** | Uses `nginx:1.27-alpine` (tiny, ~5MB). Copies the compiled static files into Nginx's default serve directory. Uses a custom `nginx.conf` for SPA routing. |

**Build arguments:**

| Argument | Default | Purpose |
|---|---|---|
| `API_BASE_URL` | `http://localhost:9090` | Backend API Gateway URL |
| `KEYCLOAK_URL` | `https://lemur-12.cloud-iam.com/auth` | Keycloak authentication server |
| `KEYCLOAK_CLIENT_ID` | `tfakkarni-frontend` | Keycloak client identifier |

**Usage:**
```bash
docker build -f frontend/Dockerfile \
  --build-arg API_BASE_URL=http://my-server:9090 \
  -t tfakkarni/frontend .
```

---

### 6.5 IoT Streamer Dockerfile (`iot/Dockerfile`)

**Location:** `iot/Dockerfile`
**Purpose:** Runs the Python IoT data simulator that sends mock GPS and heart rate data to the backend.

```dockerfile
FROM python:3.12-slim
WORKDIR /app
RUN pip install --no-cache-dir requests pyserial
COPY iot/iot_steamer.py .
ENV MOCK_GPS=true
ENV MOCK_BPM=true
ENTRYPOINT ["python", "-u", "iot_steamer.py"]
```

**How it works:**

- Uses `python:3.12-slim` as the base image.
- Installs only two dependencies: `requests` (HTTP calls) and `pyserial` (serial port, unused in Docker but part of the codebase).
- Sets `MOCK_GPS=true` and `MOCK_BPM=true` because Docker containers have no physical serial port — the script generates fake sensor data.
- The `-u` flag runs Python in unbuffered mode so logs appear in real-time in `docker logs`.

---

## 7. Docker Compose Files in This Project

### 7.1 Backend Compose (`docker-compose.backend.yml`)

**Location:** `docker-compose.backend.yml` (project root)
**Compose project name:** `tfakkarni-app`

This file defines **16 services** organized into three groups:

#### Infrastructure Services

| Service | Container Name | Port | Starts After |
|---|---|---|---|
| `discovery-service` | `tfakkarni-discovery` | 8761 | — (starts first) |
| `config-service` | `tfakkarni-config` | 8888 | discovery-service (healthy) |
| `api-gateway` | `tfakkarni-gateway` | 9090 | config-service (healthy) |

#### Business Microservices

| Service | Container Name | Port | Starts After |
|---|---|---|---|
| `user-service` | `tfakkarni-user` | 18081 | config-service (healthy) |
| `game-service` | `tfakkarni-game` | 18082 | config-service (healthy) |
| `tracking-service` | `tfakkarni-tracking` | 18083 | config-service (healthy) |
| `alert-service` | `tfakkarni-alert` | 18084 | config-service (healthy) |
| `ml-service` | `tfakkarni-ml` | 18085 | config-service (healthy) |
| `medical-service` | `tfakkarni-medical` | 18086 | config-service (healthy) |
| `medicament-validation-service` | `tfakkarni-medicament-validation` | 18087 | config-service (healthy) |
| `iot-service` | `tfakkarni-iot` | 18088 | config-service (healthy) |
| `assistant-service` | `tfakkarni-assistant` | 18089 | config-service (healthy) |
| `analytics-service` | `tfakkarni-analytics` | 18090 | config-service (healthy) |

#### IoT Simulator

| Service | Container Name | Port | Notes |
|---|---|---|---|
| `iot-streamer` | `tfakkarni-iot-streamer` | — | Python mock, no exposed port, 128M memory limit |

#### Common Configuration for All Backend Services

- **`env_file: [.env]`** — All services load secrets (DB credentials, Keycloak config, API keys) from a `.env` file in the project root.
- **`environment`** — Each service gets `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` and `SPRING_CLOUD_CONFIG_URI` pointing to the internal Docker network hostnames.
- **`healthcheck`** — All services use `curl -f http://localhost:<port>/actuator/health` to verify readiness. Checks run every 30s with a 10s timeout, 60s startup grace period, and 3 retries.
- **`restart: unless-stopped`** — Containers auto-restart on crash unless explicitly stopped.
- **`deploy.resources.limits.memory: 512M`** — Each Java service is capped at 512MB RAM (IoT streamer gets 128MB).
- **`networks: [tfakkarni-network]`** — All services share the same bridge network for inter-service communication.

---

### 7.2 Frontend Compose (`docker-compose.frontend.yml`)

**Location:** `docker-compose.frontend.yml` (project root)

```yaml
services:
  frontend:
    container_name: tfakkarni-frontend
    build:
      context: .
      dockerfile: frontend/Dockerfile
      args:
        API_BASE_URL: http://localhost:9090
        KEYCLOAK_URL: https://lemur-12.cloud-iam.com/auth
        KEYCLOAK_CLIENT_ID: tfakkarni-frontend-docker
    image: tfakkarni/frontend:latest
    ports: ["18080:80"]
    networks: [tfakkarni-network]
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 256M
```

- **Port mapping:** Host `18080` → Container `80` (Nginx)
- **Memory:** 256MB (Nginx serving static files needs very little)
- **Network:** Same `tfakkarni-network` as the backend (for potential SSR or health checks)
- **Build args:** Configurable API and Keycloak URLs baked into the Angular build at compile time

---

## 8. .dockerignore Files

### Root `.dockerignore`

**Location:** `.dockerignore`

Excludes from the build context:
- `.git`, `.gitignore` — Version control metadata
- `.idea`, `.vscode`, `*.iml` — IDE configuration
- `*.md` (except `frontend/README.md`) — Documentation
- `frontend/node_modules`, `frontend/dist`, `frontend/.angular` — Frontend build artifacts
- `backend/**/target` — Java build output (rebuilt inside Docker)
- `keycloak/` — Local Keycloak installation
- `.github/` — CI/CD configuration
- `_agent/` — Agent/memory files

### Backend `.dockerignore`

**Location:** `backend/.dockerignore`

Excludes from backend service builds:
- `.git`, `.gitignore`, `.idea`, `.vscode`, `*.iml`
- `*.md` — Documentation
- `**/target` — Maven build output (rebuilt inside Docker)
- `scripts/` — Helper scripts not needed in individual service images
- `_*.txt` — Internal file listings

**Why `.dockerignore` matters:**
- Reduces build context size → faster `COPY . .` operations
- Prevents cache invalidation from irrelevant file changes
- Keeps secrets and unnecessary files out of images

---

## 9. Nginx Configuration (Frontend)

**Location:** `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback: all routes → index.html (Angular handles routing)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets for 7 days
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

- **`try_files $uri $uri/ /index.html`** — Essential for Angular SPA routing. Any URL that doesn't match a real file gets served `index.html`, letting Angular's router handle it client-side.
- **Static asset caching** — JavaScript, CSS, images, and fonts are cached for 7 days with `immutable` flag (Angular's hashed filenames make this safe).

---

## 10. How to Run the Project with Docker

### Prerequisites

1. **Docker Desktop** installed and running
2. **`.env` file** in the project root with required secrets (database URL, Keycloak config, API keys)

### Start Everything

```bash
# Build and start all backend services
docker compose -f docker-compose.backend.yml up -d --build

# Build and start the frontend
docker compose -f docker-compose.frontend.yml up -d --build
```

### Start Without Rebuilding

```bash
docker compose -f docker-compose.backend.yml up -d
docker compose -f docker-compose.frontend.yml up -d
```

### Stop Everything

```bash
docker compose -f docker-compose.backend.yml down
docker compose -f docker-compose.frontend.yml down
```

### Access Points

| Service | URL |
|---|---|
| Frontend | http://localhost:18080 |
| API Gateway | http://localhost:9090 |
| Eureka Dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |

---

## 11. Useful Docker Commands

### Viewing Logs

```bash
# All backend logs (follow mode)
docker compose -f docker-compose.backend.yml logs -f

# Single service logs
docker compose -f docker-compose.backend.yml logs -f tracking-service
docker compose -f docker-compose.frontend.yml logs -f frontend
```

### Restarting a Single Service

```bash
docker compose -f docker-compose.backend.yml restart medical-service
```

### Checking Container Status

```bash
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Full Clean Rebuild

```bash
# Stop, remove containers and locally built images, rebuild
docker compose -f docker-compose.backend.yml down --rmi local
docker compose -f docker-compose.frontend.yml down --rmi local
docker compose -f docker-compose.backend.yml up -d --build
docker compose -f docker-compose.frontend.yml up -d --build
```

### Entering a Running Container

```bash
docker exec -it tfakkarni-medical bash
```

### Viewing Resource Usage

```bash
docker stats --no-stream
```

---

## 12. Port Mapping Reference

| Service | Host Port | Container Port | Protocol |
|---|---|---|---|
| Discovery (Eureka) | 8761 | 8761 | HTTP |
| Config Service | 8888 | 8888 | HTTP |
| API Gateway | 9090 | 9090 | HTTP |
| User Service | 18081 | 18081 | HTTP |
| Game Service | 18082 | 18082 | HTTP |
| Tracking Service | 18083 | 18083 | HTTP |
| Alert Service | 18084 | 18084 | HTTP |
| ML Service | 18085 | 18085 | HTTP |
| Medical Service | 18086 | 18086 | HTTP |
| Medicament Validation | 18087 | 18087 | HTTP |
| IoT Service | 18088 | 18088 | HTTP |
| Assistant Service | 18089 | 18089 | HTTP |
| Analytics Service | 18090 | 18090 | HTTP |
| Frontend (Nginx) | 18080 | 80 | HTTP |

---

## 13. Troubleshooting

### Container Won't Start

1. Check logs: `docker compose -f docker-compose.backend.yml logs <service-name>`
2. Verify `.env` file exists and has all required variables
3. Check if the dependency services are healthy: `docker ps`
4. Ensure ports aren't already in use: `netstat -ano | findstr :<port>` (Windows)

### Service Not Registering with Eureka

- The service needs `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/`
- Discovery service must be healthy before other services start
- Check discovery service logs for registration events

### Build Fails on `mvn package`

- Clear Maven cache: remove the BuildKit cache volume with `docker builder prune`
- Ensure all module POMs are listed in the Dockerfile's COPY instructions
- Check if a new module was added but not yet included in the Dockerfile

### Frontend Shows Blank Page

- Check browser console for API/Keycloak connection errors
- Verify `API_BASE_URL` build argument points to the correct gateway URL
- Ensure the API Gateway container is running and healthy

### Out of Memory (OOM) Kills

- Check `docker stats` for memory usage
- Increase `deploy.resources.limits.memory` in the compose file
- The `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0` setting ensures Java respects the container memory limit

### Slow Builds

- Ensure Docker BuildKit is enabled (`DOCKER_BUILDKIT=1`)
- The `.m2` cache mount (`--mount=type=cache,target=/root/.m2`) should persist across builds
- Don't modify `pom.xml` files unnecessarily — it invalidates the dependency cache layer
- Use `.dockerignore` to exclude large directories from the build context
