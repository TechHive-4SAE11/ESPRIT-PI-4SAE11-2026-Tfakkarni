# Build from repository root: docker build -f frontend/Dockerfile -t tfakkarni/frontend .
# Frontend is its own image/container; backend stack is tfakkarni-backend (see backend/Dockerfile.stack).
FROM node:20-bookworm-slim AS build
WORKDIR /workspace
ENV NPM_CONFIG_LEGACY_PEER_DEPS=true

ARG API_BASE_URL=http://localhost:9090
ARG KEYCLOAK_URL=https://lemur-12.cloud-iam.com/auth
ARG KEYCLOAK_CLIENT_ID=tfakkarni-frontend

# .npmrc enables legacy-peer-deps (Keycloak / Apex / FullCalendar peers vs npm 9+ strict resolver in Linux images).
COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./frontend/
RUN cd frontend && npm ci --legacy-peer-deps \
    || (echo "npm ci failed; falling back to npm install --legacy-peer-deps" && npm install --legacy-peer-deps)

# Create .env where the set-env script expects it
RUN printf 'API_BASE_URL=%s\nKEYCLOAK_URL=%s\nKEYCLOAK_CLIENT_ID=%s\n' "${API_BASE_URL}" "${KEYCLOAK_URL}" "${KEYCLOAK_CLIENT_ID}" > frontend/.env

COPY frontend ./frontend
WORKDIR /workspace/frontend
RUN npm run build

# Angular "application" builder → dist/<project>/browser; fallback for older layouts
RUN mkdir -p /static && \
    ls -R dist/ && \
    if [ -d dist/frontend/browser ]; then cp -a dist/frontend/browser/. /static/; \
    else cp -a dist/frontend/. /static/; fi

FROM nginx:1.27-alpine
COPY --from=build /static /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
