#!/usr/bin/env python3
"""Sync the current free ngrok HTTPS URL into a Keycloak browser client.

The script loads project settings from .env without shell-sourcing it, discovers
the current ngrok HTTPS public URL from the local ngrok API, and creates or
updates a Keycloak public SPA client.

Examples:
  python3 scripts/sync-keycloak-ngrok-client.py --dry-run
  python3 scripts/sync-keycloak-ngrok-client.py
  python3 scripts/sync-keycloak-ngrok-client.py --ngrok-url https://example.ngrok-free.app
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DOTENV_PATH = PROJECT_ROOT / ".env"

DEFAULT_KEYCLOAK_URL = "https://lemur-12.cloud-iam.com/auth"
DEFAULT_KEYCLOAK_REALM = "tfakkarni"
# This project's backend obtains the admin token from the application realm,
# not master: /realms/tfakkarni/protocol/openid-connect/token.
DEFAULT_KEYCLOAK_ADMIN_REALM = "tfakkarni"
DEFAULT_KEYCLOAK_ADMIN_USERNAME = "admin"
DEFAULT_KEYCLOAK_CLIENT_ID = "ngrock"
DEFAULT_KEYCLOAK_EXTRA_CLIENT_IDS = ("tfakkarni-frontend", "tfakkarni-frontend-docker")
DEFAULT_NGROK_API_URL = "http://127.0.0.1:4040/api/tunnels"


class SyncError(RuntimeError):
    """A safe-to-print operational error."""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Create or update a Keycloak public browser client with the current "
            "free ngrok HTTPS URL."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=f"""Environment:
  KEYCLOAK_URL             Keycloak base URL; defaults to .env or {DEFAULT_KEYCLOAK_URL}
  KEYCLOAK_REALM           Target realm; default {DEFAULT_KEYCLOAK_REALM}
  KEYCLOAK_ADMIN_REALM     Admin login realm; default {DEFAULT_KEYCLOAK_ADMIN_REALM}
  KEYCLOAK_ADMIN_USERNAME  Admin username; default {DEFAULT_KEYCLOAK_ADMIN_USERNAME}
  KEYCLOAK_ADMIN_PASSWORD  Admin password; required unless --dry-run skips Keycloak writes
  KEYCLOAK_CLIENT_ID       Client ID to create/update; default {DEFAULT_KEYCLOAK_CLIENT_ID}
  KEYCLOAK_EXTRA_CLIENT_IDS Additional comma-separated client IDs to sync;
                            default {",".join(DEFAULT_KEYCLOAK_EXTRA_CLIENT_IDS)}
  NGROK_API_URL            Local ngrok tunnels API; default {DEFAULT_NGROK_API_URL}

Output intentionally omits passwords, bearer tokens, and ngrok auth tokens.
""",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print safe planned changes without calling Keycloak write endpoints.",
    )
    parser.add_argument(
        "--ngrok-url",
        help="Use this HTTPS ngrok URL instead of reading the local ngrok API.",
    )
    return parser.parse_args()


def load_dotenv(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values

    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export ") :].lstrip()
        if "=" not in line:
            raise SyncError(f"Invalid .env line {line_number}: expected KEY=VALUE")

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if not key:
            raise SyncError(f"Invalid .env line {line_number}: empty variable name")

        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]

        values[key] = value

    return values


def env_value(dotenv: dict[str, str], name: str, default: str | None = None) -> str | None:
    value = os.environ.get(name)
    if value is not None:
        return value
    return dotenv.get(name, default)


def env_csv(dotenv: dict[str, str], name: str, default: tuple[str, ...]) -> list[str]:
    raw_value = env_value(dotenv, name)
    if raw_value is None:
        return list(default)
    return [item.strip() for item in raw_value.split(",") if item.strip()]


def unique_preserving_order(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        if value not in seen:
            seen.add(value)
            result.append(value)
    return result


def normalize_base_url(url: str, name: str) -> str:
    url = url.strip().rstrip("/")
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise SyncError(f"{name} must be an absolute http(s) URL")
    return url


def normalize_https_ngrok_url(url: str) -> str:
    normalized = url.strip().rstrip("/")
    parsed = urllib.parse.urlparse(normalized)
    if parsed.scheme != "https" or not parsed.netloc:
        raise SyncError("ngrok URL must be an absolute HTTPS URL")
    return normalized


def request_json(
    method: str,
    url: str,
    *,
    data: dict[str, Any] | None = None,
    form: dict[str, str] | None = None,
    token: str | None = None,
    expected: tuple[int, ...] = (200,),
    timeout: int = 20,
) -> Any:
    headers: dict[str, str] = {"Accept": "application/json"}
    body: bytes | None = None

    if form is not None:
        body = urllib.parse.urlencode(form).encode("utf-8")
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    elif data is not None:
        body = json.dumps(data, separators=(",", ":")).encode("utf-8")
        headers["Content-Type"] = "application/json"

    if token is not None:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status = response.getcode()
            payload = response.read()
            content_type = response.headers.get("Content-Type", "")
    except urllib.error.HTTPError as exc:
        safe_message = exc.reason or "HTTP error"
        try:
            error_payload = exc.read().decode("utf-8", errors="replace")
            if error_payload:
                parsed_error = json.loads(error_payload)
                safe_message = parsed_error.get("error_description") or parsed_error.get("error") or safe_message
        except (OSError, ValueError, json.JSONDecodeError):
            pass
        raise SyncError(f"{method} {url} failed with HTTP {exc.code}: {safe_message}") from exc
    except urllib.error.URLError as exc:
        reason = getattr(exc, "reason", exc)
        raise SyncError(f"{method} {url} failed: {reason}") from exc

    if status not in expected:
        raise SyncError(f"{method} {url} returned HTTP {status}; expected {expected}")
    if not payload:
        return None
    if "json" not in content_type.lower():
        return payload.decode("utf-8", errors="replace")
    return json.loads(payload.decode("utf-8"))


def discover_ngrok_url(api_url: str) -> str:
    tunnels_payload = request_json("GET", api_url)
    tunnels = tunnels_payload.get("tunnels") if isinstance(tunnels_payload, dict) else None
    if not isinstance(tunnels, list):
        raise SyncError("ngrok API response did not include a tunnels list")

    https_tunnels: list[dict[str, Any]] = []
    for tunnel in tunnels:
        public_url = tunnel.get("public_url") if isinstance(tunnel, dict) else None
        if isinstance(public_url, str) and public_url.startswith("https://"):
            https_tunnels.append(tunnel)

    if not https_tunnels:
        raise SyncError("No HTTPS ngrok tunnel was found in the local ngrok API")

    def tunnel_score(tunnel: dict[str, Any]) -> int:
        config = tunnel.get("config")
        addr = config.get("addr") if isinstance(config, dict) else ""
        return 0 if "30080" in str(addr) else 1

    selected = sorted(https_tunnels, key=tunnel_score)[0]
    return normalize_https_ngrok_url(str(selected["public_url"]))


def build_client_payload(client_id: str, ngrok_url: str) -> dict[str, Any]:
    redirect_uris = [
        ngrok_url,
        f"{ngrok_url}/",
        f"{ngrok_url}/home",
        f"{ngrok_url}/*",
        "http://localhost:30080",
        "http://localhost:30080/",
        "http://localhost:30080/home",
        "http://localhost:30080/*",
        "http://127.0.0.1:30080",
        "http://127.0.0.1:30080/",
        "http://127.0.0.1:30080/home",
        "http://127.0.0.1:30080/*",
    ]
    web_origins = [
        ngrok_url,
        "http://localhost:30080",
        "http://127.0.0.1:30080",
        "+",
    ]
    return {
        "clientId": client_id,
        "name": client_id,
        "description": "Local frontend testing client synced from the current free ngrok URL.",
        "enabled": True,
        "protocol": "openid-connect",
        "publicClient": True,
        "bearerOnly": False,
        "standardFlowEnabled": True,
        "implicitFlowEnabled": False,
        # The current Angular login page uses the password grant against this
        # public client for local/dev testing.
        "directAccessGrantsEnabled": True,
        "serviceAccountsEnabled": False,
        "authorizationServicesEnabled": False,
        "frontchannelLogout": True,
        "rootUrl": ngrok_url,
        "baseUrl": "/",
        "redirectUris": redirect_uris,
        "webOrigins": web_origins,
        "attributes": {
            "pkce.code.challenge.method": "S256",
            "post.logout.redirect.uris": "+",
        },
    }


def get_admin_token(keycloak_url: str, admin_realm: str, username: str, password: str) -> str:
    token_url = (
        f"{keycloak_url}/realms/{urllib.parse.quote(admin_realm, safe='')}"
        "/protocol/openid-connect/token"
    )
    payload = request_json(
        "POST",
        token_url,
        form={
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": username,
            "password": password,
        },
    )
    token = payload.get("access_token") if isinstance(payload, dict) else None
    if not token:
        raise SyncError("Keycloak token endpoint did not return an access token")
    return str(token)


def find_client(admin_clients_url: str, token: str, client_id: str) -> dict[str, Any] | None:
    query = urllib.parse.urlencode({"clientId": client_id})
    clients = request_json("GET", f"{admin_clients_url}?{query}", token=token)
    if not isinstance(clients, list):
        raise SyncError("Keycloak clients search did not return a list")
    if not clients:
        return None
    return clients[0]


def create_or_update_client(
    keycloak_url: str,
    realm: str,
    admin_realm: str,
    username: str,
    password: str,
    payload: dict[str, Any],
) -> str:
    token = get_admin_token(keycloak_url, admin_realm, username, password)
    realm_path = urllib.parse.quote(realm, safe="")
    admin_clients_url = f"{keycloak_url}/admin/realms/{realm_path}/clients"
    existing_client = find_client(admin_clients_url, token, str(payload["clientId"]))

    if existing_client:
        keycloak_internal_id = existing_client.get("id")
        if not keycloak_internal_id:
            raise SyncError("Existing Keycloak client was missing its internal id")
        update_payload = dict(existing_client)
        existing_attributes = update_payload.get("attributes")
        merged_attributes = dict(existing_attributes) if isinstance(existing_attributes, dict) else {}
        merged_attributes.update(payload.get("attributes", {}))
        update_payload.update(payload)
        update_payload["attributes"] = merged_attributes
        update_url = f"{admin_clients_url}/{urllib.parse.quote(str(keycloak_internal_id), safe='')}"
        request_json("PUT", update_url, data=update_payload, token=token, expected=(204,))
        return "updated"

    request_json("POST", admin_clients_url, data=payload, token=token, expected=(201, 204))
    return "created"


def print_plan(
    *,
    dry_run: bool,
    action: str,
    keycloak_url: str,
    realm: str,
    client_id: str,
    ngrok_url: str,
    payload: dict[str, Any],
) -> None:
    print(f"Client ID: {client_id}")
    print(f"Realm: {realm}")
    print(f"Keycloak URL: {keycloak_url}")
    print(f"ngrok URL: {ngrok_url}")
    print(f"Action: {action}")
    if dry_run:
        print("Dry run: no Keycloak write endpoints were called.")
        print("Planned redirect URIs:")
        for uri in payload["redirectUris"]:
            print(f"  - {uri}")
        print("Planned web origins:")
        for origin in payload["webOrigins"]:
            print(f"  - {origin}")


def main() -> int:
    args = parse_args()
    try:
        dotenv = load_dotenv(DOTENV_PATH)
        keycloak_url = normalize_base_url(
            env_value(dotenv, "KEYCLOAK_URL", DEFAULT_KEYCLOAK_URL) or DEFAULT_KEYCLOAK_URL,
            "KEYCLOAK_URL",
        )
        realm = env_value(dotenv, "KEYCLOAK_REALM", DEFAULT_KEYCLOAK_REALM) or DEFAULT_KEYCLOAK_REALM
        admin_realm = (
            env_value(dotenv, "KEYCLOAK_ADMIN_REALM", DEFAULT_KEYCLOAK_ADMIN_REALM)
            or DEFAULT_KEYCLOAK_ADMIN_REALM
        )
        admin_username = (
            env_value(dotenv, "KEYCLOAK_ADMIN_USERNAME", DEFAULT_KEYCLOAK_ADMIN_USERNAME)
            or DEFAULT_KEYCLOAK_ADMIN_USERNAME
        )
        admin_password = env_value(dotenv, "KEYCLOAK_ADMIN_PASSWORD")
        client_id = env_value(dotenv, "KEYCLOAK_CLIENT_ID", DEFAULT_KEYCLOAK_CLIENT_ID) or DEFAULT_KEYCLOAK_CLIENT_ID
        extra_client_ids = env_csv(dotenv, "KEYCLOAK_EXTRA_CLIENT_IDS", DEFAULT_KEYCLOAK_EXTRA_CLIENT_IDS)
        client_ids = unique_preserving_order([client_id, *extra_client_ids])
        ngrok_api_url = normalize_base_url(
            env_value(dotenv, "NGROK_API_URL", DEFAULT_NGROK_API_URL) or DEFAULT_NGROK_API_URL,
            "NGROK_API_URL",
        )

        if args.ngrok_url:
            ngrok_url = normalize_https_ngrok_url(args.ngrok_url)
        else:
            ngrok_url = discover_ngrok_url(ngrok_api_url)

        if args.dry_run:
            for target_client_id in client_ids:
                payload = build_client_payload(target_client_id, ngrok_url)
                print_plan(
                    dry_run=True,
                    action="would create or update",
                    keycloak_url=keycloak_url,
                    realm=realm,
                    client_id=target_client_id,
                    ngrok_url=ngrok_url,
                    payload=payload,
                )
                if target_client_id != client_ids[-1]:
                    print()
            return 0

        if not admin_password:
            raise SyncError("KEYCLOAK_ADMIN_PASSWORD is required for non-dry-run sync")

        for target_client_id in client_ids:
            payload = build_client_payload(target_client_id, ngrok_url)
            action = create_or_update_client(
                keycloak_url=keycloak_url,
                realm=realm,
                admin_realm=admin_realm,
                username=admin_username,
                password=admin_password,
                payload=payload,
            )
            print_plan(
                dry_run=False,
                action=action,
                keycloak_url=keycloak_url,
                realm=realm,
                client_id=target_client_id,
                ngrok_url=ngrok_url,
                payload=payload,
            )
            if target_client_id != client_ids[-1]:
                print()
        return 0
    except SyncError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
