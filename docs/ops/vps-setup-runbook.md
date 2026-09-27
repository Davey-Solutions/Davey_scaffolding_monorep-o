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
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
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

Switch to the deploy user and create a working directory:

```bash
su - deploy
mkdir -p ~/app
cd ~/app
```

Copy the deployment artifact into `~/app` from your local checkout (the compose stack also needs files under `infra/`, not only `docker-compose.yml`). From your local machine at the repository root, run:

```bash
rsync -av --exclude '.git' --exclude '.env' ./ deploy@<your-vps-host>:~/app/
```

Then on the VPS, create `.env`:

```bash
cp .env.example .env
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

In the `STATUS` column, each service should be `Up` and, where health checks are configured, include `(healthy)`. If a service is missing, restarting, exited, or unhealthy, inspect logs:

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
