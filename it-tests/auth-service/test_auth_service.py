"""Integration tests for auth-service login and refresh flows."""

import base64
import json
import os

import pytest
import requests

AUTH_EMAIL_ENV = "IT_AUTH_OWNER_EMAIL"
AUTH_PASSWORD_ENV = "IT_AUTH_OWNER_PASSWORD"
DEFAULT_AUTH_EMAIL = "owner@example.com"
DEFAULT_AUTH_PASSWORD = "owner-password"
ACCESS_TOKEN_TTL_SECONDS = 3_600
REFRESH_TOKEN_TTL_SECONDS = 604_800
TTL_TOLERANCE_SECONDS = 5
REQUEST_TIMEOUT_SECONDS = 10


@pytest.fixture
def auth_credentials() -> tuple[str, str]:
    return (
        os.environ.get(AUTH_EMAIL_ENV, DEFAULT_AUTH_EMAIL),
        os.environ.get(AUTH_PASSWORD_ENV, DEFAULT_AUTH_PASSWORD),
    )


def _decode_jwt_payload(token: str) -> dict:
    parts = token.split(".")
    assert len(parts) == 3
    payload = parts[1]
    padded_payload = payload + "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(padded_payload).decode("utf-8"))


def _assert_claims(payload: dict, *, expected_sub: str, expected_type: str, expected_ttl_seconds: int) -> None:
    assert payload["sub"] == expected_sub
    assert payload["type"] == expected_type
    assert payload["exp"] > payload["iat"]
    assert abs((payload["exp"] - payload["iat"]) - expected_ttl_seconds) <= TTL_TOLERANCE_SECONDS


def test_login_issues_tokens_with_expected_claims_and_expiry(
    auth_service_url: str, auth_credentials: tuple[str, str]
) -> None:
    email, password = auth_credentials

    response = requests.post(
        f"{auth_service_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert response.status_code == 200
    body = response.json()
    access_payload = _decode_jwt_payload(body["accessToken"])
    refresh_payload = _decode_jwt_payload(body["refreshToken"])
    expected_sub = email.strip().lower()

    _assert_claims(
        access_payload,
        expected_sub=expected_sub,
        expected_type="access",
        expected_ttl_seconds=ACCESS_TOKEN_TTL_SECONDS,
    )
    assert access_payload["role"] == "OWNER"
    _assert_claims(
        refresh_payload,
        expected_sub=expected_sub,
        expected_type="refresh",
        expected_ttl_seconds=REFRESH_TOKEN_TTL_SECONDS,
    )
    assert "role" not in refresh_payload


def test_login_rejects_wrong_credentials(auth_service_url: str, auth_credentials: tuple[str, str]) -> None:
    email, _ = auth_credentials

    response = requests.post(
        f"{auth_service_url}/api/v1/auth/login",
        json={"email": email, "password": "definitely-wrong-password"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert response.status_code == 401


def test_refresh_flow_returns_new_access_token(
    auth_service_url: str, auth_credentials: tuple[str, str]
) -> None:
    email, password = auth_credentials
    login_response = requests.post(
        f"{auth_service_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert login_response.status_code == 200
    refresh_token = login_response.json()["refreshToken"]

    refresh_response = requests.post(
        f"{auth_service_url}/api/v1/auth/refresh",
        json={"refreshToken": refresh_token},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert refresh_response.status_code == 200
    refreshed_access_payload = _decode_jwt_payload(refresh_response.json()["accessToken"])
    _assert_claims(
        refreshed_access_payload,
        expected_sub=email.strip().lower(),
        expected_type="access",
        expected_ttl_seconds=ACCESS_TOKEN_TTL_SECONDS,
    )
    assert refreshed_access_payload["role"] == "OWNER"


def test_refresh_rejects_invalid_token(auth_service_url: str) -> None:
    response = requests.post(
        f"{auth_service_url}/api/v1/auth/refresh",
        json={"refreshToken": "not.a.valid.jwt"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert response.status_code == 401
