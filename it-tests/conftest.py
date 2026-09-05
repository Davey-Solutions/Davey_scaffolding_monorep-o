"""Shared fixtures for the IT test suites.

Environment variables (§5.4.2):
  IT_BASE_URL           – gateway base URL (default: http://localhost:8080)
  IT_JOB_SERVICE_BASE_URL  – job-service base URL (default: http://localhost:8081)
  IT_AUTH_SERVICE_BASE_URL – auth-service base URL (default: http://localhost:8082)
  IT_GATEWAY_BASE_URL      – gateway base URL used in per-service suite
                             (default: IT_BASE_URL, then http://localhost:8080)
"""

import os
import pytest


@pytest.fixture(scope="session")
def base_url() -> str:
    """Base URL for the API gateway (whole-system suite)."""
    return os.environ.get("IT_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def job_service_url() -> str:
    """Base URL for the job-service (per-service suite)."""
    return os.environ.get("IT_JOB_SERVICE_BASE_URL", "http://localhost:8082")


@pytest.fixture(scope="session")
def auth_service_url() -> str:
    """Base URL for the auth-service (per-service suite)."""
    return os.environ.get("IT_AUTH_SERVICE_BASE_URL", "http://localhost:8081")


@pytest.fixture(scope="session")
def gateway_url() -> str:
    """Base URL for the gateway (per-service gateway suite)."""
    return os.environ.get("IT_GATEWAY_BASE_URL", os.environ.get("IT_BASE_URL", "http://localhost:8080"))
