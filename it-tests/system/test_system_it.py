"""Whole-system integration tests through the API gateway."""

from __future__ import annotations

from collections.abc import Iterator
import os
import uuid

import pytest
import requests

AUTH_EMAIL_ENV = "IT_AUTH_OWNER_EMAIL"
AUTH_PASSWORD_ENV = "IT_AUTH_OWNER_PASSWORD"
REQUEST_TIMEOUT_SECONDS = 10


def _auth_header(token: str) -> dict[str, str]:
    scheme = "Bearer"
    return {"Authorization": f"{scheme} {token}"}


@pytest.fixture(scope="session")
def auth_credentials() -> tuple[str, str]:
    email = os.environ.get(AUTH_EMAIL_ENV)
    password = os.environ.get(AUTH_PASSWORD_ENV)
    if not email or not password:
        pytest.skip(f"Set {AUTH_EMAIL_ENV} and {AUTH_PASSWORD_ENV} to run system ITs")
    return (email, password)


@pytest.fixture(scope="session", autouse=True)
def gateway_is_running(base_url: str) -> None:
    try:
        response = requests.get(
            f"{base_url}/actuator/health",
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
    except requests.RequestException as exc:
        pytest.skip(f"Gateway is not reachable at {base_url}: {exc}")
    if response.status_code != 200:
        pytest.skip(f"Gateway health endpoint at {base_url} returned {response.status_code}")


@pytest.fixture
def access_token(base_url: str, auth_credentials: tuple[str, str]) -> str:
    email, password = auth_credentials
    login_response = requests.post(
        f"{base_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert login_response.status_code == 200, login_response.text
    login_body = login_response.json()
    assert "accessToken" in login_body, login_body
    return login_body["accessToken"]


@pytest.fixture()
def tracked_job_ids(base_url: str, access_token: str) -> Iterator[list[str]]:
    created_ids: list[str] = []
    yield created_ids
    headers = _auth_header(access_token)
    for job_id in created_ids:
        requests.delete(
            f"{base_url}/api/v1/jobs/{job_id}",
            headers=headers,
            timeout=REQUEST_TIMEOUT_SECONDS,
        )


def _assert_health_is_up(url: str) -> None:
    response = requests.get(url, timeout=REQUEST_TIMEOUT_SECONDS)
    assert response.status_code == 200, response.text
    body = response.json()
    assert body.get("status") == "UP", body


def _create_job(base_url: str, headers: dict[str, str], tracked_job_ids: list[str]) -> dict:
    create_response = requests.post(
        f"{base_url}/api/v1/jobs",
        json={
            "customerName": f"System IT Customer {uuid.uuid4()}",
            "siteAddress": f"System IT Site {uuid.uuid4()}",
        },
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert create_response.status_code == 201, create_response.text
    created = create_response.json()
    assert "id" in created, created
    tracked_job_ids.append(created["id"])
    return created


def test_all_health_endpoints_are_green(base_url: str, auth_service_url: str, job_service_url: str) -> None:
    _assert_health_is_up(f"{base_url}/actuator/health")
    _assert_health_is_up(f"{auth_service_url}/actuator/health")
    _assert_health_is_up(f"{job_service_url}/actuator/health")


def test_health_endpoints_are_public(base_url: str) -> None:
    _assert_health_is_up(f"{base_url}/actuator/health")
    protected_response = requests.get(
        f"{base_url}/api/v1/jobs",
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert protected_response.status_code == 401


def test_auth_flow_end_to_end_through_gateway(
    base_url: str, auth_credentials: tuple[str, str]
) -> None:
    unauthenticated_jobs_response = requests.get(
        f"{base_url}/api/v1/jobs",
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert unauthenticated_jobs_response.status_code == 401

    email, password = auth_credentials
    login_response = requests.post(
        f"{base_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert login_response.status_code == 200, login_response.text
    login_body = login_response.json()
    assert "accessToken" in login_body
    assert "refreshToken" in login_body

    authenticated_jobs_response = requests.get(
        f"{base_url}/api/v1/jobs",
        headers=_auth_header(login_body["accessToken"]),
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert authenticated_jobs_response.status_code == 200
    assert isinstance(authenticated_jobs_response.json(), list)


def test_job_lifecycle_through_gateway_as_logged_in_user(
    base_url: str, access_token: str, tracked_job_ids: list[str]
) -> None:
    headers = _auth_header(access_token)
    created = _create_job(base_url, headers, tracked_job_ids)
    job_id = created["id"]
    assert created["status"] == "PENDING"
    assert created["paid"] is False

    get_response = requests.get(
        f"{base_url}/api/v1/jobs/{job_id}",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert get_response.status_code == 200
    fetched = get_response.json()
    assert fetched["id"] == job_id

    list_response = requests.get(
        f"{base_url}/api/v1/jobs",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert list_response.status_code == 200
    listed_jobs = list_response.json()
    assert isinstance(listed_jobs, list), listed_jobs
    assert all(isinstance(job, dict) for job in listed_jobs), listed_jobs
    assert all("id" in job for job in listed_jobs), listed_jobs
    listed_ids = {job["id"] for job in listed_jobs}
    assert job_id in listed_ids

    update_response = requests.put(
        f"{base_url}/api/v1/jobs/{job_id}",
        json={
            "customerName": "System IT Updated Customer",
            "siteAddress": "System IT Updated Site",
            "status": "COMPLETED",
            "paid": True,
        },
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert update_response.status_code == 200, update_response.text
    updated = update_response.json()
    assert updated["customerName"] == "System IT Updated Customer"
    assert updated["siteAddress"] == "System IT Updated Site"
    assert updated["status"] == "COMPLETED"
    assert updated["paid"] is True

    delete_response = requests.delete(
        f"{base_url}/api/v1/jobs/{job_id}",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert delete_response.status_code == 204
    tracked_job_ids.remove(job_id)

    missing_response = requests.get(
        f"{base_url}/api/v1/jobs/{job_id}",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert missing_response.status_code == 404


def test_deleted_job_is_not_readable_through_gateway(
    base_url: str, access_token: str, tracked_job_ids: list[str]
) -> None:
    headers = _auth_header(access_token)
    created = _create_job(base_url, headers, tracked_job_ids)
    job_id = created["id"]

    delete_response = requests.delete(
        f"{base_url}/api/v1/jobs/{job_id}",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert delete_response.status_code == 204
    tracked_job_ids.remove(job_id)

    missing_response = requests.get(
        f"{base_url}/api/v1/jobs/{job_id}",
        headers=headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert missing_response.status_code == 404
