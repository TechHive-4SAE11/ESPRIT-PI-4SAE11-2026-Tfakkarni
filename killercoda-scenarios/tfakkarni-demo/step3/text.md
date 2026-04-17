# Step 3 — Deploy the Frontend

The Tfakkarni frontend is an **Angular 18** SPA with **Tailwind CSS** and **Zard UI** components, served via Nginx.

## Start the frontend

```bash
cd /root/tfakkarni
docker compose up -d frontend
```

Wait for it:

```bash
./wait-for-service.sh frontend 18080 60
```

## Test the frontend is serving

```bash
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:18080
```

You should see `HTTP 200`.

## Access the UI

You can access the frontend at:

[ACCESS FRONTEND]({{TRAFFIC_HOST1_18080}})

The landing page features:
- **Alzheimer risk quiz** — visitors answer questions and get an AI-powered risk assessment
- **Login/Register** — connect with Keycloak authentication
- **Information sections** — about the platform and its features

### Demo accounts

| Email | Password | Role |
|-------|----------|------|
| `doc@doc.com` | `123456` | Doctor |
| `patient@patient.com` | `123456` | Patient/Helper |
| `admin@admin.com` | `123456` | Admin |

> **Note**: Authentication goes through Keycloak (cloud-hosted), so you need internet access to log in.
