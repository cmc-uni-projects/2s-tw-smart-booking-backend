# smart-booking Helm chart

Deploys the TravelMate / Smart Booking System on Kubernetes:

| Component | Kind | Notes |
|---|---|---|
| backend | Deployment + Service | Spring Boot, port 8080, probes on `/actuator/health/{liveness,readiness}`, logs to stdout + daily files in `/app/logs` |
| frontend | Deployment + Service | nginx serving the React build, port 80 |
| ingress | Ingress | one host: `/api`, `/oauth2`, `/login/oauth2`, `/images` → backend, everything else → frontend |
| mysql (optional) | StatefulSet + PVC | MySQL 8, utf8mb4, Vietnam time zone; or use `externalDatabase` |
| mailpit (optional) | Deployment | catches emails of the `local` profile |

Object storage is external (Cloudflare R2 or any S3 endpoint): `config.storage.*` + `R2_*` secrets.

## 1. Build the images

```bash
# backend (from 2s-tw-smart-booking-backend)
docker build -t smart-booking-backend:local .
# frontend (from 2s-tw-smart-booking-frontend): the API is on the same host, so use a relative URL
docker build -t smart-booking-frontend:k8s --build-arg VITE_API_URL=/api/v1 --build-arg VITE_MAPBOX_TOKEN=$VITE_MAPBOX_TOKEN .
```

For a real cluster, tag and push to your registry and set `backend.image` / `frontend.image` (+ `global.imagePullSecrets`).

## 2a. Local cluster (k3d)

```bash
k3d cluster create smart-booking --port "8080:80@loadbalancer"
k3d image import smart-booking-backend:local smart-booking-frontend:k8s -c smart-booking
helm upgrade --install smart-booking helm/smart-booking -n smart-booking --create-namespace -f helm/smart-booking/values-local.yaml
```

Open http://smart-booking.localhost:8080 (browsers resolve `*.localhost` to 127.0.0.1). Emails: `kubectl -n smart-booking port-forward svc/smart-booking-mailpit 8025` → http://localhost:8025.

## 2b. Dev / shared cluster

Create the Secret (command in `values-dev.yaml`), edit hosts/registry/R2 in `values-dev.yaml`, then:

```bash
helm upgrade --install smart-booking helm/smart-booking -n smart-booking --create-namespace -f helm/smart-booking/values-dev.yaml
```

## Values you will touch

| Value | Meaning |
|---|---|
| `publicUrl` | public URL; CORS, OAuth2 redirect, email links. Ingress host defaults to its host |
| `backend.profile` | `local` (Mailpit, demo-friendly) or `dev` (real SMTP) |
| `backend.liquibaseContexts` | `demo` loads demo accounts/hotels/bookings, `prod` reference data only |
| `backend.logging.*` | `appLevel`, `serviceLevel` (INFO = inputs/outputs of every call), `slowThresholdMs`, `persistence` for `/app/logs` |
| `existingSecret` / `secrets.*` | credentials (keys listed in `values.yaml`) |
| `mysql.enabled` / `externalDatabase.*` | in-cluster MySQL or your own |
| `ingress.className`, `ingress.tls` | ingress controller and TLS |

## Operations

```bash
kubectl -n smart-booking logs deploy/smart-booking-backend -f          # live logs (with request ids)
kubectl -n smart-booking port-forward svc/smart-booking-mysql 3307:3306  # DB client on localhost:3307
helm -n smart-booking uninstall smart-booking                          # MySQL PVC is kept; delete it to wipe data
```

Keep `backend.replicas: 1`: scheduled jobs (auto-cancel unpaid bookings, check-in reminders) and the AI chat session state run in-process and would be duplicated by more replicas.
