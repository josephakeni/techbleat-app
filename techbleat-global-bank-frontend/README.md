# Techbleat Global Bank — Frontend

Customer-facing React/Vite SPA for the Techbleat Global Bank proof of concept. Connects to three backend microservices for user management, transactions, and activity logging.

---

## Screens

| Screen | Route | Description |
|---|---|---|
| Login | default | Sign in with a registered user ID |
| Register | Register tab | Create a new user account |
| Dashboard | Dashboard tab | Balance, accounts, quick actions |
| Transfer | Transfer tab | Deposit, withdraw, send money |
| Report | Report tab | Statements, transaction history |

---

## Development (local)

### Prerequisites

- Node.js 18+ (Node 20 recommended)
- Backend services running (see `techbleat-global-bank-backend/`)

### Run dev server

```bash
npm install
npm run dev
```

App available at `http://localhost:3000`.

### Connect to a remote backend

If the backend runs on a different machine (e.g. a Proxmox VM at `192.168.0.10`), create `.env.local` to override the API URLs:

```bash
# .env.local
VITE_USER_API=http://192.168.0.10:8000
VITE_TX_API=http://192.168.0.10:8080
VITE_ACTIVITY_API=http://192.168.0.10:8001
```

Restart the dev server after changing this file. The backend must also have `FRONTEND_ORIGIN` set to `http://<your-machine-ip>:3000` — see the backend README for how to configure CORS.

---

## Production build (Docker / Nginx)

The `Dockerfile` builds a static Nginx image. API URLs are baked in at build time via build args.

```bash
docker build \
  --build-arg VITE_USER_API=http://192.168.0.10:8000 \
  --build-arg VITE_TX_API=http://192.168.0.10:8080 \
  --build-arg VITE_ACTIVITY_API=http://192.168.0.10:8001 \
  -t techbleat/frontend:latest .

docker run -d --name frontend -p 3000:80 techbleat/frontend:latest
```

Or use the backend Makefile from the `techbleat-global-bank-backend/` directory:

```bash
make build-frontend HOST_IP=192.168.0.10
make run-frontend
```

App available at `http://192.168.0.10:3000`.

---

## Full stack deployment

Use the `deploy` skill in Claude Code:

```
/deploy
```

Or run manually from `techbleat-global-bank-backend/`:

```bash
make build-all HOST_IP=192.168.0.10   # build backend + frontend images
make infra                             # start Postgres, Redis, Kafka
make run                               # start backend services
make run-frontend                      # start frontend container
```

See `techbleat-global-bank-backend/README.md` for the full Makefile reference and Kubernetes deployment instructions.

---

## Environment variables

All variables use the `VITE_` prefix (required by Vite to expose them to the browser bundle).

| Variable | Default | Description |
|---|---|---|
| `VITE_USER_API` | `http://localhost:8000` | User service base URL |
| `VITE_TX_API` | `http://localhost:8080` | Transaction service base URL |
| `VITE_ACTIVITY_API` | `http://localhost:8001` | Activity service base URL |

Set via `.env` (committed defaults), `.env.local` (local overrides, git-ignored), or Docker build args for containerised builds.

---

## Seeding demo users

The database starts empty. Register a user through the UI Register screen, or seed one directly:

```bash
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{"id":"user1","full_name":"Demo User","email":"user1@techbleat.com"}'
```

Then log in with user ID `user1`.

---

## Gotchas

- **"Failed to fetch" on login** — the frontend loaded but can't reach the backend. Check that the `VITE_*` API URLs point to the correct host and that the backend CORS `FRONTEND_ORIGIN` matches the URL you're using in the browser.
- **VITE_* changes require a rebuild** — these are build-time env vars. Changing `.env.local` and restarting the dev server is enough in dev mode, but a Docker image rebuild is required for production.
- **React controlled inputs** — when driving with Playwright, use `page.fill()` not `element.value =`; the latter does not trigger React's `onChange`.
