# Techbleat Global Bank — Backend

A microservices-based banking platform built with Python (FastAPI), Java (Spring Boot), PostgreSQL, Redis, and Apache Kafka. Services are independently containerised and ready for Kubernetes deployment.

---

## Architecture

```
                         ┌──────────────────┐
                         │  Frontend :3000  │
                         └────────┬─────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
   ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │  User Service   │  │Transaction Service│  │Activity Service  │
   │  FastAPI :8000  │  │ Spring Boot :8080 │  │  FastAPI :8001   │
   └────────┬────────┘  └────────┬─────────┘  └────────┬─────────┘
            │                    │                       │
            ▼                    ▼                       ▲
   ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │   PostgreSQL    │  │      Redis       │  │      Kafka       │
   │     :5432       │  │      :6379       │  │      :9092       │
   └─────────────────┘  └──────────────────┘  └──────────────────┘
```

### Services

| Service | Language | Port | Responsibility |
|---|---|---|---|
| user-service | Python / FastAPI | 8000 | User registration and account creation |
| transaction-service | Java / Spring Boot | 8080 | Deposits, withdrawals, transfers, balance queries |
| activity-service | Python / FastAPI | 8001 | Activity log via Kafka consumer |

### Infrastructure

| Component | Version | Port | Purpose |
|---|---|---|---|
| PostgreSQL | 15 | 5432 | Persistent data store |
| Redis | 7 | 6379 | Balance caching |
| Apache Kafka | 8.1.1 | 9092 | Event streaming between services |

---

## Prerequisites

- Docker 20.10+
- `make`

No local Python, Java, or database installations needed — everything runs in containers.

---

## Quick start (docker-compose)

```bash
docker compose up --build
```

Starts all services together. Suitable for local development.

```bash
docker compose down        # stop
docker compose down -v     # stop + wipe database
```

---

## Individual containers (Makefile)

Each service is built and run independently on a shared Docker network (`techbleat-net`). This is the recommended approach for pre-production testing and maps directly to Kubernetes.

### Variables

| Variable | Default | Description |
|---|---|---|
| `HOST_IP` | `192.168.0.10` | LAN IP of the host machine (used for CORS and frontend API URLs) |
| `REGISTRY` | `techbleat` | Image registry prefix |
| `TAG` | `latest` | Image tag |
| `FRONTEND_DIR` | `../techbleat-global-bank-frontend` | Path to the frontend repo |

### Common commands

```bash
# Build all backend images
make build

# Build all images including the frontend
make build-all HOST_IP=192.168.0.10

# Start infrastructure (Postgres, Redis, Kafka)
make infra

# Start backend services
make run

# Start frontend container
make run-frontend

# Full stack in one command (infra + backend + frontend)
make deploy

# Check running containers
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# View logs
make logs-user
make logs-transaction
make logs-activity

# Stop app services only (keep infra running)
make stop

# Stop and remove everything
make stop-all

# Rebuild and restart app services
make restart
```

### Push images to a registry

```bash
make build push REGISTRY=myregistry.io/techbleat TAG=v1.0.0
```

---

## CORS

All backend services accept cross-origin requests only from `FRONTEND_ORIGIN`, which defaults to `http://<HOST_IP>:3000`.

If the frontend is on a different host or port, rebuild and restart:

```bash
make stop run FRONTEND=http://myapp.example.com
```

When running with docker-compose, update `FRONTEND_ORIGIN` in `docker-compose.yml` then run `docker compose up -d`.

---

## API Reference

### User Service — `http://localhost:8000`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Health check |
| POST | `/users` | Create a new user and bank account |
| GET | `/users` | List all registered users |

**Create user**
```bash
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{"id": "u001", "full_name": "Jane Doe", "email": "jane@example.com"}'
```

---

### Transaction Service — `http://localhost:8080`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Health check |
| POST | `/transactions/deposit` | Deposit funds |
| POST | `/transactions/withdraw` | Withdraw funds |
| POST | `/transactions/transfer` | Transfer between accounts |
| GET | `/balance/{userId}` | Get cached balance |
| GET | `/transactions/{userId}` | Get last 20 transactions |

Pass the user ID via the `X-User-Id` header on all write operations.

```bash
curl -X POST http://localhost:8080/transactions/deposit \
  -H "Content-Type: application/json" -H "X-User-Id: u001" \
  -d '{"amount": 500.00}'

curl -X POST http://localhost:8080/transactions/transfer \
  -H "Content-Type: application/json" -H "X-User-Id: u001" \
  -d '{"toUserId": "u002", "amount": 50.00}'
```

---

### Activity Service — `http://localhost:8001`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Health check |
| GET | `/activities/{userId}` | Get last 20 activity entries |

Activities are written automatically when the Activity Service consumes events from the Kafka topic `banking-transactions`.

---

## Environment variables

### User Service & Activity Service

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `postgresql://bankuser:bankpass@postgres:5432/bankingdb` | PostgreSQL connection string |
| `FRONTEND_ORIGIN` | `http://localhost:3000` | CORS allowed origin |

### Transaction Service

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/bankingdb` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `bankuser` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `bankpass` | DB password |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Kafka broker |
| `REDIS_HOST` | `redis` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `8080` | App port |
| `FRONTEND_ORIGIN` | `http://localhost:3000` | CORS allowed origin |

---

## Database schema

Initialised automatically on first Postgres startup via `db-init/init.sql`.

```
users          — id, full_name, email, created_at
accounts       — user_id, balance, updated_at
transactions   — id, user_id, transaction_type, amount, reference, created_at
activities     — id, user_id, activity_type, description, created_at
```

---

## Event flow

1. Client calls Transaction Service (deposit / withdraw / transfer)
2. Transaction Service writes to PostgreSQL and publishes to Kafka topic `banking-transactions`
3. Activity Service consumes the Kafka event and writes to the `activities` table
4. Balance reads are served from Redis; cache is updated on every write

---

## Kubernetes

Manifests are in `k8s/`. The layout maps directly to the individual container model above.

```
k8s/
├── namespace.yaml          — techbleat namespace
├── secret.yaml             — DB passwords (base64)
├── configmap.yaml          — non-sensitive config + init.sql
├── infra/
│   ├── postgres.yaml       — StatefulSet + PVC (2Gi) + headless Service
│   ├── redis.yaml          — Deployment + ClusterIP Service
│   └── kafka.yaml          — StatefulSet (KRaft) + headless Service
└── apps/
    ├── user-service.yaml         — Deployment + NodePort :30800
    ├── transaction-service.yaml  — Deployment + NodePort :30808
    └── activity-service.yaml     — Deployment + NodePort :30801
```

### Deploy to a cluster

```bash
# 1. Push images to a registry your cluster can pull from
make build push REGISTRY=myregistry.io/techbleat TAG=v1.0.0

# 2. Update image references in k8s/apps/*.yaml

# 3. Update FRONTEND_ORIGIN in k8s/configmap.yaml to your node IP

# 4. Apply all manifests
make k8s-deploy

# 5. Check status
make k8s-status

# 6. Tear down
make k8s-delete
```

When using NodePorts, update the frontend API URLs to:
```
VITE_USER_API=http://<node-ip>:30800
VITE_TX_API=http://<node-ip>:30808
VITE_ACTIVITY_API=http://<node-ip>:30801
```

---

## Makefile reference

| Target | Description |
|---|---|
| `make network` | Create `techbleat-net` Docker network |
| `make infra` | Start Postgres, Redis, Kafka |
| `make build` | Build backend service images |
| `make build-frontend` | Build frontend Nginx image |
| `make build-all` | Build all images (backend + frontend) |
| `make run` | Start backend service containers |
| `make run-frontend` | Start frontend container |
| `make deploy` | Start infra + backend + frontend |
| `make push` | Push images to registry |
| `make logs-user` | Tail user-service logs |
| `make logs-transaction` | Tail transaction-service logs |
| `make logs-activity` | Tail activity-service logs |
| `make stop` | Stop backend service containers |
| `make stop-all` | Stop all containers + remove network |
| `make restart` | Stop and restart backend services |
| `make k8s-deploy` | Apply all Kubernetes manifests |
| `make k8s-status` | Show pods and services in `techbleat` namespace |
| `make k8s-delete` | Delete the `techbleat` namespace |
