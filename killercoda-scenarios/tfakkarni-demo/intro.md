# Tfakkarni — Alzheimer Tracking Platform

**Tfakkarni** ("Remember me" in Arabic) is an Alzheimer disease tracking & care platform built at ESPRIT. It connects **patients**, **caregivers (helpers)**, and **doctors** through a web application and IoT wearable devices.

## What you'll explore

In this scenario, you will:

1. **Deploy the full microservice stack** using Docker Compose (13+ services)
2. **Explore the Eureka service registry** to see all registered services
3. **Test the AI-powered Alzheimer risk quiz** via the REST API
4. **Interact with the medical and game services** through the API Gateway
5. **See the Angular frontend** with Zard UI components

## Architecture Overview

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────────┐
│   Angular    │────▶│  API Gateway │────▶│  Eureka Discovery   │
│   Frontend   │     │   (9090)     │     │      (8761)         │
└─────────────┘     └──────┬───────┘     └─────────────────────┘
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
     ┌────────────┐ ┌───────────┐ ┌──────────────┐
     │   User     │ │   Game    │ │   Medical    │  ... + 7 more
     │  Service   │ │  Service  │ │   Service    │
     │  (18081)   │ │  (18082)  │ │   (18086)    │
     └────────────┘ └───────────┘ └──────────────┘
```

## Services included

| Service | Port | Description |
|---------|------|-------------|
| Discovery (Eureka) | 8761 | Service registry |
| Config Service | 8888 | Centralized configuration |
| API Gateway | 9090 | Single entry point, JWT validation |
| User Service | 18081 | User management & Keycloak sync |
| Game Service | 18082 | Memory games CRUD, play, scores |
| Tracking Service | 18083 | IoT GPS & heartbeat data |
| Alert Service | 18084 | Alerts & notifications |
| ML Service | 18085 | Alzheimer risk prediction |
| Medical Service | 18086 | Appointments, prescriptions |
| Medicament Validation | 18087 | Drug interaction checks |
| IoT Service | 18088 | IoT device management |
| Assistant Service | 18089 | AI care assistant |
| Analytics Service | 18090 | Patient analytics & trends |
| Frontend | 18080 | Angular SPA |

Let's get started!
