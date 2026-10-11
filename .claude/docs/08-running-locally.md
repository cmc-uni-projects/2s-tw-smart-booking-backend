# Running Locally (Docker Compose)

Two independent compose files, one per repo. The browser talks to the backend directly on `localhost:8386`, so the two stacks don't share a Docker network.

## Start

```bash
# 1. Backend (first build ~3-5 min)
cd 2s-tw-smart-booking-backend
cp .env.example .env            # first time only; set JWT_SECRET (openssl rand -hex 64) and DB passwords
docker compose up -d --build

# 2. Frontend
cd ../2s-tw-smart-booking-frontend
docker compose up -d --build
```

| URL | What |
|---|---|
| http://localhost:5173 | React app (nginx serving the production build) |
| http://localhost:8386/api/v1 | Spring Boot API (container port 8080) |
| http://localhost:8025 | **Mailpit**: every email the app sends (verification, booking…) lands here |
| http://localhost:9000 | MinIO S3 API (stands in for Cloudflare R2). No web console, see below |
| localhost:3307 | MySQL 8 (`smart_booking`; credentials in backend `.env`) |

Demo accounts (Liquibase context `demo`, set via `LIQUIBASE_CONTEXTS=demo` in `.env`): `admin@` / `Admin@123`, `owner@` & `owner2@` / `Owner@123`, `customer@`, `customer2@`, `customer3@` / `Customer@123` — all `@travelmate.vn`. The dataset (8 hotels, rooms, promotions, bookings in every state) is described in [09-database-migrations.md](./09-database-migrations.md).

## Registering with email + password (no Google/Facebook needed)

1. http://localhost:5173/register → fill in the form. Password must satisfy the **backend** rule: ≥ 8 chars, upper + lower case, digit, and one of `@#$%^&+=!.*` (the form's own hint says 6 chars, which is wrong; see gotcha #35).
2. The account is created as `INACTIVE`; login returns 403 "Tài khoản chưa được xác thực" until verified.
3. Open **Mailpit http://localhost:8025** → email "Xác thực Email - Smart Booking" → click the link (`http://localhost:5173/verify-email?token=…`). **The link expires after 10 minutes.**
4. Log in.

### Configuration profiles

Config is YAML: `application.yml` (shared) + one file per profile. Pick with `SPRING_PROFILES_ACTIVE` in `.env` (compose passes it on).

| Profile | When | Email | Liquibase contexts |
|---|---|---|---|
| `local` (default) | your machine / docker compose | always the local mail server (Mailpit); `MAIL_HOST`/`MAIL_PORT` are ignored | `demo` (demo dataset) |
| `dev` | shared dev environment | real SMTP from `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail by default) | `prod` |

Outside Docker the local profile expects Mailpit on `localhost:1025` (override with `LOCAL_MAIL_HOST` / `LOCAL_MAIL_PORT`).

### Sending real emails (profile `dev`)

1. Turn on 2-Step Verification on the Gmail account, then create an **App Password** (Google Account → Security → App passwords). A normal password will be rejected.
2. In the backend `.env`: `SPRING_PROFILES_ACTIVE=dev`, `MAIL_USERNAME=<gmail>`, `MAIL_PASSWORD=<app password>` (`MAIL_HOST`/`MAIL_PORT` default to Gmail).
3. `docker compose up -d backend` (no rebuild needed).
4. Check inbox/spam. If nothing arrives: `docker compose logs backend | grep -i mail`.

`MAIL_USERNAME` is also the From address. Gmail limits a personal account to roughly 500 recipients/day. Careful: the demo accounts use real-looking `@travelmate.vn` addresses.

### Logs

Besides `docker compose logs backend`, every line is written to `logs/backend-YYYY-MM-DD.log` in the backend folder (one file per day, Vietnam time, kept 30 days). See [11](./11-logging.md).

Browse stored files (no web console in 2025 community builds):
```bash
docker compose exec minio sh -c 'mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD && mc ls --recursive local/smart-booking'
```

## Stop / reset

```bash
docker compose down        # stop, keep data
docker compose down -v     # stop AND wipe MySQL + MinIO volumes (re-seeds on next start)
docker compose logs -f backend
```

## How the backend compose works

| Service | Image | Why |
|---|---|---|
| `mysql` | mysql:8.0 | replaces TiDB Cloud; **Liquibase** creates the tables and loads reference + demo data on startup |
| `minio` | bitnamilegacy/minio:2025.7.23 | replaces R2; root user/password = `R2_ACCESS_KEY/SECRET` (secret ≥ 8 chars); `MINIO_DEFAULT_BUCKETS` creates the bucket; `MINIO_DOMAIN=s3.localhost` enables virtual-hosted URLs. The official `minio/minio` image is no longer on Docker Hub, and the Bitnami legacy image is frozen, so use it for local dev only. |
| `mailpit` | axllent/mailpit | mail sink for the `local` profile (`LOCAL_MAIL_HOST=mailpit` set by compose) |
| `backend` | built from `Dockerfile` | `.env` is **mounted at `/app/.env`** because `DotenvEnvironmentPostProcessor` calls `Dotenv.load()` and crashes if the file is missing |

**S3 hostnames:** `FileStorageService` builds virtual-hosted URLs (`<bucket>.s3.localhost:9000`). Inside the container, `extra_hosts` maps `s3.localhost` and `smart-booking.s3.localhost` to the Docker host. In the browser, `*.localhost` resolves to 127.0.0.1 automatically. So presigned image URLs work on both sides. If you rename `R2_BUCKET_NAME`, compose picks it up.

## Frontend build

- Multi-stage: `node:22-alpine` → `npm run build` → `nginx:1.27-alpine` with SPA fallback (`nginx.conf`).
- Vite bakes `VITE_*` vars in at **build time**: change them → rebuild (`docker compose up -d --build`).
- Sets **both** `VITE_REACT_APP_API_URL` and `VITE_API_BASE_URL` (the code uses both names). Override with `LOCAL_API_URL=… docker compose up -d --build`.
- `VITE_MAPBOX_TOKEN` is taken from the frontend `.env` if present (Explore map).
- The local `.env` files are excluded from the Docker build context (`.dockerignore`).

## What does NOT work locally (by design or due to app bugs)

| Feature | Why | Fix |
|---|---|---|
| Google / Facebook login | dummy client IDs (**on hold**) | put real OAuth credentials in `.env`, add redirect `http://localhost:8386/login/oauth2/code/{google,facebook}` |
| AI free chat | dummy `GEMINI_API_KEY` (**on hold**) | real key in `.env` (the guided city/capacity search still hits the DB) |
| Some images | frontend `utils/image.util.js` turns bare keys into the **team's public r2.dev URL** | gotcha #33 |
| Payments | simulated everywhere, nothing to configure | — |
