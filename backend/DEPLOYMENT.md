# Chule Digita — Backend Production Deployment Guide
## Phase 3: Supabase PostgreSQL 17 Migration

This guide details the deployment configuration and runtime specifications for the Express backend migrated to **Supabase PostgreSQL 17.11**.

---

## 1. Hosting Architecture & Platform Selection

Because the backend uses persistent bidirectional WebSockets (`socket.io`) for real-time mobile and web push notifications alongside standard HTTP REST endpoints, serverless platforms with execution time caps (such as AWS Lambda or Vercel Functions) are **not** recommended.

### Recommended Target Platforms

1. **Render (Recommended)**
   - **Service Type**: Web Service (Node.js runtime).
   - **Build Command**: `npm install --omit=dev`
   - **Start Command**: `node src/server.js`
   - **Health Check Path**: `/api/health`
   - **WebSocket Support**: Full persistent WebSocket support enabled by default.
   - **Secret Files**: Mount `certs/prod-ca-2021.crt` or rely on the bundled CA root.

2. **Railway**
   - **Service Type**: Node.js Service with automatic GitHub deploy triggers.
   - **Start Command**: `npm start`
   - **Networking**: Public domain with automatic HTTPS/WSS termination.

3. **Fly.io / AWS ECS / DigitalOcean App Platform**
   - Standard container or Node.js VM instance.

---

## 2. Required Production Environment Variables

Never commit real secrets or `.env` to Git. Configure these variables directly in your hosting provider's dashboard:

| Variable | Description | Example / Format | Enforced in Production? |
|---|---|---|---|
| `NODE_ENV` | Runtime environment | `production` | **Yes** |
| `PORT` | HTTP/WebSocket listening port | `5000` (or injected by platform) | Default `5000` |
| `JWT_SECRET` | Cryptographic signing secret for JWTs | High-entropy hex (`openssl rand -hex 32`) | **Yes** (>= 32 chars, no placeholders) |
| `JWT_EXPIRES_IN` | Token validity duration | `7d` | Default `7d` |
| `CORS_ORIGIN` | Comma-separated list of trusted client origins | `https://app.chuledigital.com,https://admin.chuledigital.com` | **Yes** (wildcard `*` forbidden) |
| `DATABASE_URL` | Supabase Session Pooler or direct connection URL | `postgresql://postgres.[ref]:[pw]@aws-0-eu-central-1.pooler.supabase.com:6543/postgres` | **Yes** (or discrete `PG*` below) |
| `PGHOST` | Supabase host (if not using `DATABASE_URL`) | `aws-0-eu-west-2.pooler.supabase.com` | If `DATABASE_URL` absent |
| `PGPORT` | PostgreSQL port | `5432` (direct) or `6543` (pooler) | Default `5432` |
| `PGUSER` | Database user | `postgres.[project_ref]` | If `DATABASE_URL` absent |
| `PGPASSWORD` | Database password | Secure password | If `DATABASE_URL` absent |
| `PGDATABASE` | Database name | `postgres` | Default `postgres` |
| `PGSSLROOTCERT` | Path to Supabase root CA certificate | `certs/prod-ca-2021.crt` | Verified on startup |

---

## 3. Database Connectivity & TLS Verification

- The backend connects to Supabase PostgreSQL using the official Node.js `pg` driver with a shared `Pool`.
- Strict TLS certificate verification is enforced when connecting to Supabase endpoints using `certs/prod-ca-2021.crt` (`rejectUnauthorized: true`).
- 64-bit PostgreSQL integers (`INT8`/`BIGINT`, OID 20) are parsed to JavaScript numbers via `types.setTypeParser` to preserve complete backward-compatibility with web and Android clients.
- `keepAlive: true` is configured to prevent connection resets and pool starvation.

---

## 4. Health Checks & Monitoring Endpoints

- **Liveness Check**: `GET /api/health`
  - Returns `200 OK` if the Express HTTP server is active and receiving traffic.
- **Readiness Check**: `GET /api/health/ready`
  - Performs a lightweight query (`SELECT 1`) against Supabase PostgreSQL.
  - Returns `200 OK` when healthy, or `503 Service Unavailable` if the database is unreachable.

---

## 5. Security & Authorization Checklist

- [x] **Row Level Security (RLS)**: Public tables have `rowsecurity=true` enabled in Supabase PostgreSQL.
- [x] **Parameterized Queries**: All queries across all models strictly use parameterized `$1`, `$2` placeholders, preventing SQL injection.
- [x] **Privileged Credentials**: Privileged database connection strings and keys exist only on the backend server and are never exposed to Android or Web clients.
- [x] **Audit Integrity**: Financial records marked `VERIFIED` cannot be deleted, ensuring accounting compliance.
- [x] **Safe Logging**: Passwords, tokens, and authorization credentials are redacted from logs.
