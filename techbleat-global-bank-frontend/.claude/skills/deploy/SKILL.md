---
description: Build and run the full Techbleat Global Bank stack (frontend + all backend services) in Docker containers
---

# Deploy: Techbleat Global Bank (Full Stack)

Deploys the complete app — Nginx frontend + three backend microservices + Postgres, Redis, and Kafka — as individual Docker containers on a shared network. No docker-compose required.

## Directory layout

```
techbleat-app/
├── techbleat-global-bank-frontend/   ← React/Vite frontend
└── techbleat-global-bank-backend/    ← Makefile + microservices + k8s/
```

All `make` commands run from `techbleat-global-bank-backend/`.

## Key variables

| Variable | Default | Override with |
|---|---|---|
| `HOST_IP` | `192.168.0.10` | `make ... HOST_IP=x.x.x.x` |
| `REGISTRY` | `techbleat` | `make ... REGISTRY=myregistry.io/org` |
| `TAG` | `latest` | `make ... TAG=v1.2.3` |
| `FRONTEND_DIR` | `../techbleat-global-bank-frontend` | `make ... FRONTEND_DIR=/path/to/frontend` |

`HOST_IP` is the LAN IP of the machine where containers run. The frontend is built with this IP baked into the API URLs (Vite build-time env vars), and the backend uses it for CORS.

## Full deployment — fresh machine

```bash
cd techbleat-global-bank-backend

# 1. Build all images (backend services + frontend)
make build-all HOST_IP=192.168.0.10

# 2. Start infrastructure (Postgres, Redis, Kafka)
make infra

# 3. Wait for Postgres to be ready
until docker exec postgres pg_isready -U bankuser -d bankingdb; do sleep 2; done

# 4. Start backend services
make run

# 5. Start frontend
make run-frontend
```

App is live at: `http://<HOST_IP>:3000`

## Verify all containers are up

```bash
make k8s-status   # if on k8s
# or for plain Docker:
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected output:

```
NAMES                 STATUS    PORTS
frontend              Up        0.0.0.0:3000->80/tcp
activity-service      Up        0.0.0.0:8001->8001/tcp
transaction-service   Up        0.0.0.0:8080->8080/tcp
user-service          Up        0.0.0.0:8000->8000/tcp
kafka                 Up        0.0.0.0:9092->9092/tcp
redis                 Up        0.0.0.0:6379->6379/tcp
postgres              Up        0.0.0.0:5432->5432/tcp
```

## Health checks

```bash
curl http://localhost:8000/health   # user-service     → {"status":"ok"}
curl http://localhost:8001/health   # activity-service → {"status":"ok"}
curl http://localhost:8080/health   # transaction-service → 200 OK
curl http://localhost:3000          # frontend → 200 HTML
```

## Seed a demo user

After services are healthy, create at least one user so login works:

```bash
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{"id":"user1","full_name":"Demo User","email":"user1@techbleat.com"}'
```

Then log in at `http://<HOST_IP>:3000` with user ID `user1`.

## Rebuild after a code change

```bash
# Backend service change (e.g. user-service):
make build-user && docker rm -f user-service && make run-user

# Frontend change:
make build-frontend HOST_IP=192.168.0.10 && docker rm -f frontend && make run-frontend

# Full rebuild:
make stop-all && make build-all HOST_IP=192.168.0.10 && make infra && make run && make run-frontend
```

## Tear down

```bash
make stop-all        # stops and removes all containers + network
# add -v flag to also remove postgres volume:
docker volume rm postgres_data
```

## Ports

| Container | Host port | Purpose |
|---|---|---|
| frontend | 3000 | React app (Nginx) |
| user-service | 8000 | User registration / listing |
| transaction-service | 8080 | Deposits, withdrawals, transfers |
| activity-service | 8001 | Activity log |
| postgres | 5432 | Database |
| redis | 6379 | Balance cache |
| kafka | 9092 | Event streaming |

## CORS

`FRONTEND_ORIGIN` in each backend service is set to `http://<HOST_IP>:3000` at container start via the Makefile. If the HOST_IP changes, rebuild all images and restart services:

```bash
make stop-all && make build-all HOST_IP=<new-ip> && make deploy HOST_IP=<new-ip>
```

## Kubernetes

Manifests are in `k8s/`. Apply the full stack with:

```bash
make k8s-deploy
make k8s-status
```

NodePorts exposed:
- Frontend would be served separately (build image, push, create Deployment + NodePort)
- user-service: 30800
- transaction-service: 30808
- activity-service: 30801

Update `k8s/configmap.yaml` `FRONTEND_ORIGIN` and frontend `.env.local` API URLs to use the node IP + NodePorts before deploying.

## Gotchas

- **"Failed to fetch" after login** — CORS mismatch. Ensure `HOST_IP` matches the IP your browser uses to reach the app. Rebuild if it changed.
- **Login fails (user not found)** — seed a user via the curl command above. The DB starts empty.
- **Transaction service slow to start** — Spring Boot takes ~30 s. The `/health` endpoint returns 404 until fully started; wait for a 200.
- **Kafka not ready** — Kafka can take 20–30 s. The transaction and activity services will retry automatically.
- **Frontend shows stale API URLs** — VITE_* vars are baked in at build time. A config change requires `make build-frontend` and a container restart, not just a page refresh.
