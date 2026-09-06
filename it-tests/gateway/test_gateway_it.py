"""Integration tests for gateway routing and JWT enforcement."""

import os

import pytest
import requests

AUTH_EMAIL_ENV = "IT_AUTH_OWNER_EMAIL"
AUTH_PASSWORD_ENV = "IT_AUTH_OWNER_PASSWORD"
DEFAULT_AUTH_EMAIL = "owner@example.com"
DEFAULT_AUTH_PASSWORD = "owner-password"
REQUEST_TIMEOUT_SECONDS = 10


@pytest.fixture(scope="session")
def auth_credentials() -> tuple[str, str]:
    return (
        os.environ.get(AUTH_EMAIL_ENV, DEFAULT_AUTH_EMAIL),
        os.environ.get(AUTH_PASSWORD_ENV, DEFAULT_AUTH_PASSWORD),
    )


@pytest.fixture(scope="session", autouse=True)
def gateway_is_running(gateway_url: str) -> None:
    try:
        response = requests.get(f"{gateway_url}/actuator/health", timeout=REQUEST_TIMEOUT_SECONDS)
    except requests.RequestException as exc:
        pytest.skip(f"Gateway is not reachable at {gateway_url}: {exc}")
    if response.status_code != 200:
        pytest.skip(f"Gateway health endpoint at {gateway_url} returned {response.status_code}")


@pytest.fixture
def access_token(gateway_url: str, auth_credentials: tuple[str, str]) -> str:
    email, password = auth_credentials
    login_response = requests.post(
        f"{gateway_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert login_response.status_code == 200, login_response.text
    return login_response.json()["accessToken"]


def test_gateway_health_endpoint_is_healthy(gateway_url: str) -> None:
    response = requests.get(f"{gateway_url}/actuator/health", timeout=REQUEST_TIMEOUT_SECONDS)

    assert response.status_code == 200
    assert response.json().get("status") == "UP"


def test_gateway_rejects_missing_jwt_on_protected_route(gateway_url: str) -> None:
    response = requests.get(f"{gateway_url}/api/v1/jobs", timeout=REQUEST_TIMEOUT_SECONDS)

    assert response.status_code == 401


def test_gateway_rejects_invalid_jwt_on_protected_route(gateway_url: str) -> None:
    response = requests.get(
        f"{gateway_url}/api/v1/jobs",
        headers={"Authorization": "******"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert response.status_code == 401


def test_gateway_routes_to_auth_and_job_services(
    gateway_url: str, access_token: str
) -> None:
    jobs_response = requests.get(
        f"{gateway_url}/api/v1/jobs",
        headers={"Authorization": f"******"},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )

    assert jobs_response.status_code == 200
    assert isinstance(jobs_response.json(), list)
