#!/usr/bin/env bash
# Postgres initialisation script.
# Creates per-service databases and dedicated users.
# Passwords are taken from environment variables with safe development defaults.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-SQL
    create user jobs_user with password '${JOBS_DB_PASSWORD:-jobs_pass}';
    create database jobs_db owner jobs_user;

    create user auth_user with password '${AUTH_DB_PASSWORD:-auth_pass}';
    create database users_db owner auth_user;
SQL
