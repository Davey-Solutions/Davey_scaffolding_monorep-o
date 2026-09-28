# VPS setup runbook (v1)

This runbook prepares a fresh Ubuntu VPS for the v1 production deployment model in [§5.3 of the architecture](../arch/architecture.md#53-deployment): one server running Docker Compose.

## 1) Assumptions

- OS: Ubuntu 24.04 LTS (or another recent Ubuntu/Debian with equivalent packages).
- You can SSH to the server as `root` for initial setup.
- You have a domain ready to point at this VPS later (HTTPS/reverse-proxy setup is covered by DAV-37).

## 2) Create a non-root deploy user

Run as `root`:

```bash
adduser deploy
usermod -aG sudo deploy
```

## 3) Install Docker Engine + Compose plugin

Run as `root`:

```bash
apt-get update
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
```

Allow the deploy user to run Docker without `sudo`:

```bash
usermod -aG docker deploy
```

## 4) Configure firewall

Run as `root`:

```bash
apt-get update
apt-get install -y ufw
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable
ufw status
```

## 5) Prepare app directory and environment

Create the app directory as `deploy`, then switch to that user:

```bash
install -d -o deploy -g deploy /home/deploy/app
su - deploy
cd ~/app
```

Copy the required deployment files into `~/app` from your local checkout. From your local machine at the repository root, run:

- Prerequisite: `rsync` installed on your local machine.
- The compose stack uses prebuilt images (`ghcr.io/...`) and bind-mounts a script from `infra/postgres/`, so copy the compose file, optional `.env.example`, and the `infra/postgres/` directory to preserve expected paths.
- Required files for this deployment mode are: `docker-compose.yml`, `infra/postgres/init.sh`, and `.env` (generated on the VPS from `.env.example`, if provided). No compose override files are used in this runbook.

```bash
COMPOSE_FILE="docker-compose.yml"
test -f "$COMPOSE_FILE"
ssh deploy@<your-vps-host> "mkdir -p ~/app/infra/postgres"
if [ -f .env.example ]; then
  rsync -av "$COMPOSE_FILE" .env.example deploy@<your-vps-host>:~/app/
else
  rsync -av "$COMPOSE_FILE" deploy@<your-vps-host>:~/app/
fi
rsync -av infra/postgres/ deploy@<your-vps-host>:~/app/infra/postgres/
```

Then on the VPS, create `.env`:

```bash
if [ -f .env.example ]; then
  cp .env.example .env
else
  cat > .env <<'EOF'
JWT_SECRET=
POSTGRES_PASSWORD=
AUTH_DB_PASSWORD=
JOBS_DB_PASSWORD=
EOF
fi
chmod 600 .env
```

Set production secrets in `.env` (minimum required):

- `JWT_SECRET` (must be long and random; at least 32 characters)
- `POSTGRES_PASSWORD`
- `AUTH_DB_PASSWORD`
- `JOBS_DB_PASSWORD`
- Optional bootstrap owner credentials:
  - `AUTH_BOOTSTRAP_OWNER_EMAIL`
  - `AUTH_BOOTSTRAP_OWNER_PASSWORD`

Before continuing, confirm required keys are present and non-empty in `.env`.

```bash
for key in JWT_SECRET POSTGRES_PASSWORD AUTH_DB_PASSWORD JOBS_DB_PASSWORD; do
  grep -Eq "^${key}=.+$" .env || { echo "Missing required key: ${key}"; exit 1; }
done
```

## 6) Start and verify the stack

As `deploy`:

```bash
docker compose pull
docker compose up -d
docker compose ps
```

Health checks (run these on the VPS shell where Docker Compose is running):

```bash
docker compose ps gateway auth-service job-service
```

In the output, each service should be up/running and, where health checks are configured, show `healthy`. If a service is missing, restarting, exited, or unhealthy, inspect logs:

```bash
docker compose logs --tail=200 gateway auth-service job-service
```

## 7) Repeatable update command

For subsequent deploys on the same VPS:

```bash
cd ~/app
docker compose pull
docker compose up -d
docker image prune -f
```
