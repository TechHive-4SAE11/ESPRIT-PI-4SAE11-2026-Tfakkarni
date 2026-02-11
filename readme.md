# Commands to Run the Tfakkarni Platform

keycloak:
```bash
.\kc.bat start-dev --http-port=8180
```

# Tfakkarni Platform - Service Ports

| Service               | Port | URL                   |
| --------------------- | ---- | --------------------- |
| **Frontend**          | 4200 | http://localhost:4200 |
| **API Gateway**       | 9090 | http://localhost:9090 |
| **Eureka Discovery**  | 8761 | http://localhost:8761 |
| **User Service**      | 8081 | http://localhost:8081 |
| **Keycloak**          | 8180 | http://localhost:8180 |
| **PostgreSQL (Neon)** | 5432 | Cloud-hosted (Neon)   |
