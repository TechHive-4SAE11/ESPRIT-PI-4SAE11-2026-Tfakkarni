#!/usr/bin/env bash
set -euo pipefail

# Sync the current free ngrok HTTPS URL into the Keycloak frontend clients.
# This wrapper keeps execution from the project root so .env is loaded by the
# Python script without shell-sourcing secrets.
#
# Usage:
#   scripts/sync-keycloak-ngrok-client.sh
#   scripts/sync-keycloak-ngrok-client.sh --dry-run
#   scripts/sync-keycloak-ngrok-client.sh --ngrok-url https://example.ngrok-free.app

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"
exec python3 scripts/sync-keycloak-ngrok-client.py "$@"
