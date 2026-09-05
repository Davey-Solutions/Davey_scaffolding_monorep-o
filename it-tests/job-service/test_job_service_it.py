"""Integration tests for the job-service CRUD and filtering APIs."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import time
import uuid
from pathlib import Path

import pytest
import requests


REQUEST_TIMEOUT_SECONDS = 10


def _base64_url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _read_jwt_secret_from_dotenv() -> str | None:
    dotenv_path = Path(__file__).resolve().parents[2] / ".env"
    if not dotenv_path.exists():
        return None

    for line in dotenv_path.read_text(encoding="utf-8").splitlines():
        entry = line.strip()
        if not entry or entry.startswith("#") or "=" not in entry:
            continue
        key, value = entry.split("=", 1)
        if key.strip() == "JWT_SECRET":
            stripped = value.strip()
            if len(stripped) >= 2 and stripped[0] == stripped[-1] and stripped[0] in {"'", '"'}:
                stripped = stripped[1:-1]
            return stripped
    return None


def _jwt_secret() -> str:
    return (
        os.environ.get("IT_JOB_SERVICE_JWT_SECRET")
        or os.environ.get("JWT_SECRET")
        or _read_jwt_secret_from_dotenv()
        or ""
    )


def _build_hs256_token(secret: str) -> str:
    now = int(time.time())
    header = _base64_url(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode("utf-8"))
    payload = _base64_url(
        json.dumps({"sub": "it-job-service", "iat": now, "exp": now + 3600}, separators=(",", ":")).encode("utf-8")
    )
    signing_input = f"{header}.{payload}".encode("ascii")
    signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
    return f"{header}.{payload}.{_base64_url(signature)}"


@pytest.fixture(scope="session")
def auth_headers() -> dict[str, str]:
    secret = _jwt_secret()
    if not secret:
        pytest.skip("Set IT_JOB_SERVICE_JWT_SECRET or JWT_SECRET (or provide .env JWT_SECRET) to run job-service ITs")

    return {"Authorization": "Bearer " + _build_hs256_token(secret)}


@pytest.fixture(scope="session", autouse=True)
def job_service_is_running(job_service_url: str) -> None:
    try:
        response = requests.get(f"{job_service_url}/actuator/health", timeout=REQUEST_TIMEOUT_SECONDS)
    except requests.RequestException as exc:
        pytest.skip(f"Job service is not reachable at {job_service_url}: {exc}")
    if response.status_code != 200:
        pytest.skip(f"Job service health endpoint at {job_service_url} returned {response.status_code}")


@pytest.fixture()
def tracked_job_ids(auth_headers: dict[str, str], job_service_url: str):
    created_ids: list[str] = []
    yield created_ids
    for job_id in created_ids:
        requests.delete(
            f"{job_service_url}/api/v1/jobs/{job_id}",
            headers=auth_headers,
            timeout=REQUEST_TIMEOUT_SECONDS,
        )


def _create_job(
    job_service_url: str,
    auth_headers: dict[str, str],
    tracked_job_ids: list[str],
    *,
    status: str | None = None,
    paid: bool | None = None,
) -> dict:
    payload = {
        "customerName": f"Customer-{uuid.uuid4()}",
        "siteAddress": f"Site-{uuid.uuid4()}",
    }
    if status is not None:
        payload["status"] = status
    if paid is not None:
        payload["paid"] = paid
    response = requests.post(
        f"{job_service_url}/api/v1/jobs",
        json=payload,
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert response.status_code == 201, response.text

    body = response.json()
    tracked_job_ids.append(body["id"])
    return body


def _assert_validation_problem(response: requests.Response, expected_fields: list[str]) -> None:
    assert response.status_code == 400
    body = response.json()
    assert body.get("status") == 400
    assert body.get("title") == "Validation failed"
    detail = body.get("detail", "")
    for field in expected_fields:
        assert field in detail


def test_job_crud_and_default_status_paid(job_service_url: str, auth_headers: dict[str, str], tracked_job_ids: list[str]) -> None:
    created = _create_job(job_service_url, auth_headers, tracked_job_ids)
    job_id = created["id"]

    assert created["status"] == "PENDING"
    assert created["paid"] is False

    get_response = requests.get(
        f"{job_service_url}/api/v1/jobs/{job_id}",
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert get_response.status_code == 200
    fetched = get_response.json()
    assert fetched["id"] == job_id
    assert fetched["customerName"] == created["customerName"]
    assert fetched["siteAddress"] == created["siteAddress"]
    assert fetched["status"] == "PENDING"
    assert fetched["paid"] is False

    list_response = requests.get(
        f"{job_service_url}/api/v1/jobs",
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert list_response.status_code == 200
    list_ids = {job["id"] for job in list_response.json()}
    assert job_id in list_ids

    update_response = requests.put(
        f"{job_service_url}/api/v1/jobs/{job_id}",
        json={"customerName": "Updated Customer", "siteAddress": "Updated Site"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert update_response.status_code == 200
    updated = update_response.json()
    assert updated["customerName"] == "Updated Customer"
    assert updated["siteAddress"] == "Updated Site"
    assert updated["status"] == "PENDING"
    assert updated["paid"] is False

    delete_response = requests.delete(
        f"{job_service_url}/api/v1/jobs/{job_id}",
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert delete_response.status_code == 204
    tracked_job_ids.remove(job_id)

    missing_response = requests.get(
        f"{job_service_url}/api/v1/jobs/{job_id}",
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert missing_response.status_code == 404


@pytest.mark.parametrize(
    ("payload", "expected_fields"),
    [
        ({"customerName": "", "siteAddress": "Test Site"}, ["customerName"]),
        ({"customerName": "Test Customer", "siteAddress": ""}, ["siteAddress"]),
        ({"customerName": "Test Customer"}, ["siteAddress"]),
        ({"siteAddress": "Test Site"}, ["customerName"]),
    ],
)
def test_create_validation_errors(
    job_service_url: str, auth_headers: dict[str, str], payload: dict, expected_fields: list[str]
) -> None:
    response = requests.post(
        f"{job_service_url}/api/v1/jobs",
        json=payload,
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    _assert_validation_problem(response, expected_fields)


def test_create_invalid_status_returns_400(job_service_url: str, auth_headers: dict[str, str]) -> None:
    response = requests.post(
        f"{job_service_url}/api/v1/jobs",
        json={"customerName": "Test Customer", "siteAddress": "Test Site", "status": "NOT_A_REAL_STATUS"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert response.status_code == 400


def test_filtering_by_status_and_paid(job_service_url: str, auth_headers: dict[str, str], tracked_job_ids: list[str]) -> None:
    first_created = _create_job(job_service_url, auth_headers, tracked_job_ids)
    second_created = _create_job(job_service_url, auth_headers, tracked_job_ids)
    status_control = _create_job(job_service_url, auth_headers, tracked_job_ids, status="COMPLETED", paid=False)
    paid_control = _create_job(job_service_url, auth_headers, tracked_job_ids, status="PENDING", paid=True)

    matching_response = requests.get(
        f"{job_service_url}/api/v1/jobs",
        params={"status": "PENDING", "paid": "false"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert matching_response.status_code == 200
    matching_jobs = matching_response.json()
    assert len(matching_jobs) >= 2
    assert all(job["status"] == "PENDING" and job["paid"] is False for job in matching_jobs)
    matching_ids = {job["id"] for job in matching_jobs}
    assert first_created["id"] in matching_ids
    assert second_created["id"] in matching_ids
    assert status_control["id"] not in matching_ids
    assert paid_control["id"] not in matching_ids

    status_only_mismatch_response = requests.get(
        f"{job_service_url}/api/v1/jobs",
        params={"status": "COMPLETED", "paid": "false"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert status_only_mismatch_response.status_code == 200
    status_only_mismatch_ids = {job["id"] for job in status_only_mismatch_response.json()}
    assert first_created["id"] not in status_only_mismatch_ids
    assert second_created["id"] not in status_only_mismatch_ids

    paid_only_mismatch_response = requests.get(
        f"{job_service_url}/api/v1/jobs",
        params={"status": "PENDING", "paid": "true"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert paid_only_mismatch_response.status_code == 200
    paid_only_mismatch_ids = {job["id"] for job in paid_only_mismatch_response.json()}
    assert first_created["id"] not in paid_only_mismatch_ids
    assert second_created["id"] not in paid_only_mismatch_ids

    both_mismatch_response = requests.get(
        f"{job_service_url}/api/v1/jobs",
        params={"status": "COMPLETED", "paid": "true"},
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    assert both_mismatch_response.status_code == 200
    both_mismatch_ids = {job["id"] for job in both_mismatch_response.json()}
    assert first_created["id"] not in both_mismatch_ids
    assert second_created["id"] not in both_mismatch_ids


@pytest.mark.parametrize(
    ("payload", "expected_fields"),
    [
        ({"customerName": "", "siteAddress": "Test Site"}, ["customerName"]),
        ({"customerName": "Test Customer", "siteAddress": ""}, ["siteAddress"]),
        ({"customerName": "Test Customer"}, ["siteAddress"]),
        ({"siteAddress": "Test Site"}, ["customerName"]),
    ],
)
def test_update_validation_errors(
    job_service_url: str,
    auth_headers: dict[str, str],
    tracked_job_ids: list[str],
    payload: dict,
    expected_fields: list[str],
) -> None:
    created = _create_job(job_service_url, auth_headers, tracked_job_ids)
    response = requests.put(
        f"{job_service_url}/api/v1/jobs/{created['id']}",
        json=payload,
        headers=auth_headers,
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    _assert_validation_problem(response, expected_fields)
