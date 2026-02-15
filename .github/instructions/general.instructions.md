---
description: General project context and coding guidelines for the Tfakkarni Alzheimer tracking platform
applyTo: "**/*"
---

# Tfakkarni — AI Coding Instructions

## Project Overview

**Tfakkarni** ("Remember me" in Arabic) is an Alzheimer disease tracking & care platform built by **TechHive-4SAE11** at ESPRIT. It connects patients, caregivers (helpers), and doctors through a web app and IoT wearable devices.

### Core Flows

1. **Landing Page Quiz** — visitors take an Alzheimer risk quiz scored by the ML Service. Mid-to-high risk triggers a pop-up to book a doctor session.
2. **Doctor** — views appointments, examines patients, writes prescriptions (medications, dosage, schedule).
3. **Helper (Caregiver)** — logs medication usage, tracks activities, creates manual alerts (e.g. allergies), builds personalized memory games, monitors patient progress.
4. **Patient (Elderly)** — plays cognitive games, views scores. Uses a simplified elderly-friendly UI with large buttons and emoji navigation. Shares the same account as their helper.
5. **IoT Bracelet** — posts GPS location, heart rate, and automatic alerts (fall detection, abnormal heart rate, geofence exit) to the platform.

### User Roles

| Role    | Description                                                                  |
| ------- | ---------------------------------------------------------------------------- |
| visitor | Takes the quiz on the landing page, can register if at risk                  |
| patient | Elderly user — plays games, views scores, wears IoT bracelet                 |
| helper  | Caregiver — manages meds, activities, alerts, games (shares patient account) |
| doctor  | Medical professional — appointments, prescriptions                           |
| admin   | Platform administrator                                                       |

---

## Architecture & Services

| Service            | Port | Spring App Name   | Gateway Route      | Purpose                                      |
| ------------------ | ---- | ----------------- | ------------------ | -------------------------------------------- |
| Frontend (Angular) | 4200 | —                 | —                  | SPA served by Angular CLI / SSR              |
| API Gateway        | 9090 | api-gateway       | —                  | Single entry point, JWT validation, CORS     |
| Eureka Discovery   | 8761 | discovery-service | —                  | Service registry                             |
| User Service       | 8081 | user-service      | /api/users/\*\*    | User management & Keycloak sync              |
| Game Service       | 8082 | game-service      | /api/games/\*\*    | Personalized memory games CRUD, play, scores |
| Tracking Service   | 8083 | tracking-service  | /api/tracking/\*\* | IoT data ingestion (GPS, heartbeat)          |
| Alert Service      | 8084 | alert-service     | /api/alerts/\*\*   | Alerts & notifications (IoT + manual + meds) |
| ML Service         | 8085 | ml-service        | /api/ml/\*\*       | Alzheimer risk prediction & quiz scoring     |
| Medical Service    | 8086 | medical-service   | /api/medical/\*\*  | Appointments, prescriptions, medication logs |
| Keycloak           | 8180 | —                 | —                  | Identity & access management                 |
| PostgreSQL (Neon)  | 5432 | —                 | —                  | Cloud-hosted database                        |

### Tech Stack

- **Frontend**: Angular 18 (standalone components, signals API, SSR), Tailwind CSS, **Zard UI** (zardui.com)
- **Backend**: Spring Boot 3.3.6, Spring Cloud 2023.0.4, Java 17, Maven multi-module
- **Gateway**: Spring Cloud Gateway + OAuth2 Resource Server (JWT from Keycloak)
- **Discovery**: Netflix Eureka
- **Auth**: Keycloak 26 — realm: `techhive`, client: `tfakkarni-frontend`
- **Database**: PostgreSQL on Neon Cloud, Hibernate 6 with `ddl-auto: update`
- **IoT**: Bracelet devices posting GPS/heartbeat/alerts via REST to tracking-service & alert-service

### Base Packages

Each backend service follows the pattern `org.techhive.<servicename>`:

- `org.techhive.userservice`
- `org.techhive.gameservice`
- `org.techhive.trackingservice`
- `org.techhive.alertservice`
- `org.techhive.mlservice`
- `org.techhive.medicalservice`

---

## Frontend Guidelines

### MANDATORY: Use Zard UI Components

All frontend UI **must** use [Zard UI](https://zardui.com) components. Do NOT use Angular Material, PrimeNG, Bootstrap, or any other UI library.

- Browse components at **https://zardui.com**
- Import from `@/shared/components/<component-name>`
- Use `z-` prefixed selectors in templates

Available components include: `z-card`, `z-card-header`, `z-card-title`, `z-card-description`, `z-card-content`, `z-card-footer`, `z-button`, `z-icon`, `z-badge`, `z-input`, `z-dialog`, `z-progress-bar`, `z-separator`, `z-tabs`, `z-alert`, `z-avatar`, `z-tooltip`, etc.

### Valid ZardIcon Types

When using `<z-icon zType="...">`, only use valid icon names. Known valid types:
`house`, `gamepad-2`, `play-circle`, `bar-chart-3`, `plus-circle`, `heart`, `trophy`, `target`, `brain`, `play`, `plus`, `arrow-left`, `upload`, `x`, `check`, `loader-2`, `trash-2`, `rotate-ccw`, `user`, `settings`, `log-out`, `search`, `bell`, `calendar`, `clock`, `map-pin`, `activity`, `shield`, `eye`, `edit`, `save`, `menu`, `chevron-down`, `chevron-right`, `chevron-left`, `alert-triangle`, `info`, `mail`, `phone`, `star`, `filter`, `download`, `external-link`, `copy`, `refresh-cw`, `more-horizontal`, `more-vertical`, `lock`, `unlock`, `image`, `file`, `folder`, `globe`, `zap`, `thermometer`, `pill`

Do NOT use icon names that aren't on this list (e.g. `heart-handshake`, `repeat` are invalid).

**Exception**: The patient view (elderly-friendly) uses simplified HTML + Tailwind with emoji icons for maximum accessibility — this is intentional and acceptable.

### Angular Conventions

- Use **standalone components** (no NgModules)
- Use **signals** (`signal()`, `computed()`, `effect()`) for reactive state — avoid BehaviorSubject where signals suffice
- Use `@Component({ standalone: true, imports: [...] })` pattern
- Path aliases: `@/core/...`, `@/shared/...`, `@/pages/...`
- Auth: `keycloak-angular` v16, tokens stored in `localStorage` key `tfk_tokens`
- View mode (helper/patient) stored in `localStorage` key `tfk_view_mode`
- HTTP calls go through `http://localhost:9090/api/...` (API Gateway)

### Styling

- Use **Tailwind CSS** for all layout and spacing
- Follow the existing color tokens: `bg-background`, `text-foreground`, `bg-muted`, `text-muted-foreground`, `bg-primary`, `text-primary-foreground`, etc.
- Do NOT write component-scoped CSS for things Tailwind & Zard UI already handle

---

## Backend Guidelines

### Creating a New Service

1. Create folder: `backend/<service-name>/`
2. Add `pom.xml` with parent `org.techhive:tfakkarni-backend:0.0.1-SNAPSHOT`
3. Add `src/main/java/org/techhive/<servicename>/<ServiceName>Application.java`
4. Add `src/main/resources/application.yml` (port, datasource, eureka config)
5. Register module in `backend/pom.xml` `<modules>` section
6. Add gateway route in `backend/api-gateway/src/main/resources/application.yml`

### Spring Boot Conventions

- Use `@RestController` with `@RequestMapping("/api/<resource>")`
- Use Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) on entities & DTOs
- Use `@Entity` with `@Table(name = "...")` — Hibernate auto-creates/updates tables
- For PostgreSQL byte arrays: use `@Column(columnDefinition = "BYTEA")` — do NOT use `@Lob` (causes OID/bigint issues with Hibernate 6)
- All services register with Eureka and are accessed through the API Gateway in production
- Keep `ddl-auto: update` during development

### Error Handling

- Return proper HTTP status codes (400, 404, 409, 500)
- Use `@ControllerAdvice` / `@ExceptionHandler` for centralized error handling
- Never silently swallow exceptions — always log or propagate

---

## Key File Locations

| What                            | Path                                                     |
| ------------------------------- | -------------------------------------------------------- |
| Parent POM                      | `backend/pom.xml`                                        |
| API Gateway config              | `backend/api-gateway/src/main/resources/application.yml` |
| Gateway security                | `backend/api-gateway/src/.../SecurityConfig.java`        |
| Frontend routes                 | `frontend/src/app/app.routes.ts`                         |
| Frontend app config             | `frontend/src/app/app.config.ts`                         |
| Auth service                    | `frontend/src/app/core/auth/`                            |
| Zard UI shared components       | `frontend/src/app/shared/components/`                    |
| Patient dashboard (host)        | `frontend/src/app/pages/patient-dashboard/`              |
| Helper view                     | `frontend/src/app/pages/patient-dashboard/helper-view/`  |
| Patient view (elderly-friendly) | `frontend/src/app/pages/patient-dashboard/patient-view/` |
| Game service entities           | `backend/game-service/src/.../gameservice/`              |
| Keycloak realm export           | `realm-export.json`                                      |

---

## Common Pitfalls

- **Image uploads**: Don't use `@Lob` on `byte[]` fields with PostgreSQL + Hibernate 6 — use `@Column(columnDefinition = "BYTEA")` instead
- **Token refresh**: Always call `keycloakService.updateToken(30)` before important POST/PUT calls
- **Icon names**: Only use valid ZardIcon type strings — check the list above
- **Silent errors**: Never `console.error()` without showing user feedback — use signal-based error/success messages
- **Patient UI**: The patient view intentionally skips the dashboard layout for elderly accessibility — don't wrap it in `<app-dashboard-layout>`
- **Servers**: Do NOT start/restart servers automatically — the developer will manage them manually
