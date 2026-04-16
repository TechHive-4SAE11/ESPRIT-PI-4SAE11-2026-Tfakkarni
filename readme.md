# Tfakkarni — Alzheimer Tracking & Care Platform

> **Tfakkarni** ("Remember me" in Arabic) is a comprehensive Alzheimer disease tracking and care platform built by **TechHive-4SAE11** at ESPRIT. It connects patients, caregivers (helpers), and doctors through a web application and IoT wearable devices to enable early detection, daily monitoring, and personalized cognitive exercises.

---

## Table of Contents

- [Project Overview](#project-overview)
- [User Roles & Flows](#user-roles--flows)
- [Architecture](#architecture)
- [Service Ports](#service-ports)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Frontend Rules (IMPORTANT)](#frontend-rules-important)
- [API Gateway Routes](#api-gateway-routes)
- [Project Structure](#project-structure)

---

## Project Overview

### Landing Page & Alzheimer Risk Quiz

Every visitor starts on the **landing page** which introduces the platform and invites them to take an **Alzheimer risk assessment quiz**. The quiz is powered by the **ML Service** which scores cognitive responses. At the end:

- **Low risk** — informational results, option to share with a doctor.
- **Mid-to-High risk** — a **pop-up appears** to immediately **book a session with a doctor** on the platform.

### Doctor Flow

- Doctors can view booked appointments.
- After a session, the doctor writes a **prescription** (medications, dosage, schedule).
- Prescriptions are stored in the **Medical Service** and visible to the patient's helper.

### Helper (Caregiver) Flow

The helper is a family member or caregiver who manages the patient's daily life:

- **Medication Tracking** — logs when the patient takes medicine (based on the doctor's prescription).
- **Activity Logging** — records daily activities (walks, meals, social interactions).
- **Manual Alerts** — creates custom alerts for the patient (e.g., allergy warnings, reminders).
- **Personalized Games** — builds custom memory games with images for the patient to play.
- **Progress Dashboard** — monitors quiz scores, game performance, and cognitive trends.

### Patient Flow

The patient (typically elderly) has a **simplified, elderly-friendly UI** with large buttons and emoji-based navigation:

- **Play Games** — cognitive exercises created by their helper.
- **View Scores** — see how they performed.
- The patient and helper share the same account (helper switches between views).

### IoT Bracelet Integration

Patients wear an **IoT bracelet** that continuously sends data to the platform:

- **GPS Location** — real-time tracking so helpers/doctors can locate the patient.
- **Heart Rate** — continuous heartbeat monitoring.
- **Alerts** — the bracelet can trigger automatic alerts (fall detection, abnormal heart rate, leaving a geofence).

All IoT data is received by the **Tracking Service** and alerts are managed by the **Alert Service**.

---

## User Roles & Flows

| Role        | Description                                                                        |
| ----------- | ---------------------------------------------------------------------------------- |
| **Visitor** | Takes the Alzheimer quiz on the landing page. Can register if at risk.             |
| **Patient** | Elderly user — plays games, views scores. Simplified UI. Wears IoT bracelet.       |
| **Helper**  | Caregiver — manages meds, activities, alerts, games. Shares account with patient.  |
| **Doctor**  | Medical professional — receives bookings, examines patients, writes prescriptions. |
| **Admin**   | Platform admin — manages users, system configuration.                              |

---

## Architecture

```
                          ┌─────────────────┐
                          │   Angular 18    │
                          │   Frontend      │
                          │   :4200         │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   API Gateway   │
                          │   :9090         │
                          │  (JWT + CORS)   │
                          └────────┬────────┘
                                   │
              ┌──────────┬─────────┼──────────┬──────────┬──────────┐
              │          │         │          │          │          │
     ┌────────▼──┐ ┌─────▼─────┐ ┌▼────────┐ ┌▼────────┐ ┌▼────────┐ ┌▼────────────┐
     │  User     │ │  Game     │ │Tracking │ │ Alert   │ │  ML     │ │  Medical    │
     │  Service  │ │  Service  │ │ Service │ │ Service │ │ Service │ │  Service    │
     │  :18081   │ │  :18082   │ │ :18083  │ │ :18084  │ │ :18085  │ │  :18086     │
     └─────┬─────┘ └─────┬─────┘ └────┬────┘ └────┬────┘ └────┬────┘ └──────┬──────┘
           │              │            │           │           │             │
           └──────────────┴────────────┴───────────┴───────────┴─────────────┘
                                       │
                              ┌────────▼────────┐        ┌──────────────┐
                              │   PostgreSQL    │        │   Keycloak   │
                              │   (Neon Cloud)  │        │   :8280      │
                              └─────────────────┘        └──────────────┘
                                                                │
                                                  Eureka Discovery :8761
```

---

## Service Ports

| Service                | Port    | URL / Notes                                                               |
| ---------------------- | ------- | ------------------------------------------------------------------------- |
| **Frontend (Angular)** | `4200`  | http://localhost:4200                                                     |
| **API Gateway**        | `9090`  | http://localhost:9090 — single entry point for all APIs                   |
| **Eureka Discovery**   | `8761`  | http://localhost:8761 — service registry dashboard                        |
| **User Service**       | `18081` | http://localhost:18081 — user management & Keycloak sync                  |
| **Game Service**       | `18082` | http://localhost:18082 — personalized memory games                        |
| **Tracking Service**   | `18083` | http://localhost:18083 — IoT data (GPS, heartbeat)                        |
| **Alert Service**      | `18084` | http://localhost:18084 — alerts (IoT, manual, medication)                 |
| **ML Service**         | `18085` | http://localhost:18085 — Alzheimer risk prediction & quiz scoring         |
| **Medical Service**    | `18086` | http://localhost:18086 — appointments, prescriptions, medication tracking |
| **Keycloak**           | `8280`  | http://localhost:8280 — identity & access management                      |
| **PostgreSQL (Neon)**  | `5432`  | Cloud-hosted on Neon (no local install needed)                            |

---

## Tech Stack

| Layer         | Technology                                                    |
| ------------- | ------------------------------------------------------------- |
| **Frontend**  | Angular 18 (SSR), Tailwind CSS, **Zard UI** (zardui.com)      |
| **Backend**   | Spring Boot 3.3.6, Spring Cloud 2023.0.4, Java 17             |
| **Gateway**   | Spring Cloud Gateway + OAuth2 Resource Server (JWT)           |
| **Discovery** | Netflix Eureka                                                |
| **Auth**      | Keycloak 26 (realm: `techhive`, client: `tfakkarni-frontend`) |
| **Database**  | PostgreSQL (Neon Cloud), Hibernate 6 with `ddl-auto: update`  |
| **IoT**       | Bracelet devices posting GPS/heartbeat/alerts via REST        |
| **ML**        | Risk scoring model served through ml-service                  |
| **Build**     | Maven (multi-module), pnpm (frontend)                         |

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Node.js 20+** & **pnpm**
- **Keycloak** (included in `/keycloak` folder)

### 1. Start Keycloak

```bash
cd keycloak/bin
.\kc.bat start-dev --http-port=8280      # Windows
./kc.sh start-dev --http-port=8280       # macOS / Linux
```

### 2. Start Eureka Discovery

```bash
cd backend/discovery-service
mvn spring-boot:run
```

### 3. Start Backend Services

Start each service in a separate terminal (order does not matter after Eureka is up):

```bash
cd backend/user-service      && mvn spring-boot:run
cd backend/game-service      && mvn spring-boot:run
cd backend/tracking-service  && mvn spring-boot:run
cd backend/alert-service     && mvn spring-boot:run
cd backend/ml-service        && mvn spring-boot:run
cd backend/medical-service   && mvn spring-boot:run
```

### 4. Start API Gateway

```bash
cd backend/api-gateway
mvn spring-boot:run
```

### 5. Start Frontend

```bash
cd frontend
pnpm install
pnpm start
```

Open http://localhost:4200.

---

## Frontend Rules (IMPORTANT)

> **All team members MUST use [Zard UI](https://zardui.com) components for the frontend.**

### Why?

Zard UI is our design system. It provides consistent, accessible, and pre-styled Angular components that match the Tfakkarni brand. Using raw HTML/CSS or other UI libraries will cause visual inconsistencies and will not be accepted in code reviews.

### How to use

1. Browse available components at **https://zardui.com**
2. Import the component from `@/shared/components/<component-name>`
3. Use the `z-` prefixed selectors in templates

### Example

```typescript
import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';

@Component({
  imports: [ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <z-card>
      <z-card-header>
        <z-card-title>My Feature</z-card-title>
      </z-card-header>
      <z-card-content>
        <z-button>Click me</z-button>
      </z-card-content>
    </z-card>
  `,
})
```

### Rules

- **DO** use Zard UI components (`z-card`, `z-button`, `z-icon`, `z-badge`, `z-input`, `z-dialog`, `z-progress-bar`, etc.)
- **DO** use Tailwind CSS for layout & spacing
- **DO NOT** install other UI libraries (PrimeNG, Angular Material, Bootstrap, etc.)
- **DO NOT** write custom CSS for things Zard UI already provides
- **EXCEPTION**: The patient view (elderly-friendly) uses simplified HTML with Tailwind for maximum accessibility — this is intentional

---

## API Gateway Routes

All frontend HTTP calls go through the API Gateway at `http://localhost:9090`:

| Route Pattern      | Target Service     | Description                            |
| ------------------ | ------------------ | -------------------------------------- |
| `/api/users/**`    | `user-service`     | User CRUD, Keycloak sync               |
| `/api/games/**`    | `game-service`     | Memory games CRUD, play, scores        |
| `/api/tracking/**` | `tracking-service` | IoT data ingestion (GPS, heartbeat)    |
| `/api/alerts/**`   | `alert-service`    | Alert management (IoT + manual + meds) |
| `/api/ml/**`       | `ml-service`       | Quiz scoring, risk prediction          |
| `/api/medical/**`  | `medical-service`  | Appointments, prescriptions, meds      |

---

## Project Structure

```
Tfakkarni/
├── readme.md
├── realm-export.json              # Keycloak realm config
├── keycloak/                      # Keycloak server (local)
│
├── backend/                       # Spring Boot microservices
│   ├── pom.xml                    # Parent POM (all modules)
│   ├── discovery-service/         # Eureka service registry         :8761
│   ├── api-gateway/               # Spring Cloud Gateway + JWT      :9090
│   ├── user-service/              # User management                 :18081
│   ├── game-service/              # Personalized memory games       :18082
│   ├── tracking-service/          # IoT data (GPS, heartbeat)       :18083
│   ├── alert-service/             # Alerts & notifications          :18084
│   ├── ml-service/                # ML risk prediction & quiz       :18085
│   └── medical-service/           # Appointments & prescriptions    :18086
│
└── frontend/                      # Angular 18 + Zard UI
    ├── src/
    │   ├── app/
    │   │   ├── core/              # Auth, guards, interceptors
    │   │   ├── shared/            # Zard UI components, pipes
    │   │   └── pages/             # Feature pages (landing, patient, doctor, admin)
    │   └── styles.css             # Tailwind base styles
    └── package.json
```

---

## Team Quick Reference

| What                    | Where / How                                                          |
| ----------------------- | -------------------------------------------------------------------- |
| Add a new REST endpoint | Create `@RestController` in the appropriate service                  |
| Add a gateway route     | `backend/api-gateway/src/main/resources/application.yml` → `routes:` |
| Add a frontend page     | `frontend/src/app/pages/<feature>/`                                  |
| Use a UI component      | Browse https://zardui.com → import from `@/shared/components/`       |
| Keycloak admin console  | http://localhost:8280/admin (admin/admin)                            |
| Eureka dashboard        | http://localhost:8761                                                |
| DB schema changes       | Hibernate `ddl-auto: update` handles it — just update your `@Entity` |

---

## Testing

### Core Medical Features (Unit Tests)
To run only the unit tests for Prescriptions, Medications, Care Plans, and Analytics (Standardized Pagination & RBAC validation), run this in the `frontend` directory:

```bash
# This runs the 4 core services and the 4 management components together
npm test -- --watch=false --include "src/app/core/services/{prescription,medication,care-plan,analytics}.service.spec.ts" --include "src/app/pages/**/*.{prescription,medication,care-plan,analytics-dashboard}.component.spec.ts"
```

## 🔍 Observability & Distributed Tracing

This project uses **Micrometer Tracing** and **Zipkin** for performance monitoring across the microservice ecosystem. Every request generates a unique **Trace ID** that follows the request from the Gateway to all downsteam services.

### How to start the Tracing Dashboard:

1.  **Launch Zipkin** (requires Docker):
    ```bash
    docker run -d -p 9411:9411 openzipkin/zipkin
    ```
2.  **Access the Dashboard**:
    Open [http://localhost:9411](http://localhost:9411) in your browser.
3.  **Visualization**:
    Perform any action in the application to generate traffic. In Zipkin, you will see a unified Trace ID representing the full lifecycle of the request across all microservices (Gateway -> Service A -> Service B).

