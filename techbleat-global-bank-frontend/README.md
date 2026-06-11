# Techbleat Global Bank Frontend

Customer-facing React frontend for the Techbleat Global Bank proof of concept.

## Prerequisites

- Node.js 18+ (Node 20 recommended)
- Your backend services running locally:
  - User service: `http://localhost:8000`
  - Transaction service: `http://localhost:8080`
  - Activity service: `http://localhost:8001`

## Run locally

```bash
npm install
npm run dev
```

Then open:

```text
http://localhost:3000
```

## Notes

This project expects CORS to be enabled on your backend services for `http://localhost:3000`.

The login screen uses a registered user ID from the FastAPI service.
