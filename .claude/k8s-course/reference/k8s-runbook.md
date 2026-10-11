# Runbook: Smart Booking on a local Kubernetes cluster (k3d + Helm)

Copy-paste commands for every step. The lesson explaining them is [0001](../lessons/0001-run-the-finished-app.md); terms are in the [glossary](./glossary.md). Plain-YAML version (no Helm): [`k8s/README.md`](../../../k8s/README.md).

All paths below start from the folder that contains both repos:

```bash
cd ~/Documents/github-projects/cmc-uni-projects/2s-tw-smart-booking
```

| Name used below | Value |
|---|---|
| Cluster | `smart-booking` (k3d) |
| Namespace | `smart-booking` |
| Helm release | `smart-booking` |
| UI | http://smart-booking.localhost:8080 |
| Demo logins | `admin@travelmate.vn` / `Admin@123` · `owner@travelmate.vn` / `Owner@123` · `customer@travelmate.vn` / `Customer@123` |

---

## 0. One-time install

You need **Docker**, **k3d**, **kubectl** and **Helm**. Docker, k3d and kubectl are already in `~/.local/bin`. Check:

```bash
docker version --format '{{.Server.Version}}'
k3d version
kubectl version --client
helm version            # "command not found" → install it (next block)
```

Install Helm 3 into `~/.local/bin` (official binary, [install docs](https://helm.sh/docs/intro/install/)):

```bash
mkdir -p /tmp/helm-install && cd /tmp/helm-install
curl -fsSLO https://get.helm.sh/helm-v3.16.4-linux-amd64.tar.gz
curl -fsSL  https://get.helm.sh/helm-v3.16.4-linux-amd64.tar.gz.sha256sum | sha256sum -c -
tar -xzf helm-v3.16.4-linux-amd64.tar.gz linux-amd64/helm
mv linux-amd64/helm ~/.local/bin/helm && cd - && rm -rf /tmp/helm-install
helm version
```

---

## 1. First start (from nothing)

### 1.1 Build the two images

```bash
# backend
cd 2s-tw-smart-booking-backend
docker build -t smart-booking-backend:local .
cd ..

# frontend: API on the same host → relative URL; Mapbox token read from the frontend .env
cd 2s-tw-smart-booking-frontend
docker build -t smart-booking-frontend:k8s \
  --build-arg VITE_API_URL=/api/v1 \
  --build-arg VITE_MAPBOX_TOKEN="$(grep '^VITE_MAPBOX_TOKEN=' .env | cut -d= -f2-)" .
cd ..
```

### 1.2 Create the cluster

```bash
k3d cluster create smart-booking \
  --port "8080:80@loadbalancer" \
  --kubeconfig-update-default=false --kubeconfig-switch-context=false
```

`--port 8080:80@loadbalancer` makes http://…:8080 on your machine reach the cluster's ingress. The two `kubeconfig` flags keep your `~/.kube/config` (other clusters) untouched.

### 1.3 Point kubectl/helm at it (**every new terminal**)

```bash
export KUBECONFIG=$(k3d kubeconfig write smart-booking)
kubectl config current-context        # must print: k3d-smart-booking
```

### 1.4 Copy the images into the cluster

```bash
k3d image import smart-booking-backend:local smart-booking-frontend:k8s -c smart-booking
```

### 1.5 Install the app

```bash
cd 2s-tw-smart-booking-backend
helm upgrade --install smart-booking helm/smart-booking \
  -n smart-booking --create-namespace \
  -f helm/smart-booking/values-local.yaml
```

### 1.6 Wait until it's ready (first time ~2–3 min: MySQL init + Liquibase)

```bash
kubectl -n smart-booking get pods -w          # Ctrl+C when all are 1/1 Running
# or block until the backend is ready:
kubectl -n smart-booking rollout status deploy/smart-booking-backend --timeout=5m
```

Open **http://smart-booking.localhost:8080**.

---

## 2. Daily use

| I want to… | Command |
|---|---|
| Stop for the day (keeps data, frees CPU/RAM) | `k3d cluster stop smart-booking` |
| Start again | `k3d cluster start smart-booking` then `export KUBECONFIG=$(k3d kubeconfig write smart-booking)` |
| See what's running | `kubectl -n smart-booking get pods` |
| See all clusters | `k3d cluster list` |

### Deploy a code change

Same tag, so tell Kubernetes to restart the pod with the new image:

```bash
# backend changed
cd 2s-tw-smart-booking-backend && docker build -t smart-booking-backend:local . && cd ..
k3d image import smart-booking-backend:local -c smart-booking
kubectl -n smart-booking rollout restart deploy/smart-booking-backend

# frontend changed: same, with the build command from 1.1, then
k3d image import smart-booking-frontend:k8s -c smart-booking
kubectl -n smart-booking rollout restart deploy/smart-booking-frontend
```

### Change a setting (values file or chart)

```bash
cd 2s-tw-smart-booking-backend
helm upgrade --install smart-booking helm/smart-booking -n smart-booking -f helm/smart-booking/values-local.yaml
```

---

## 3. Logs

```bash
# live backend logs (Ctrl+C to quit)
kubectl -n smart-booking logs -f deploy/smart-booking-backend

# last 200 lines / last 10 minutes
kubectl -n smart-booking logs deploy/smart-booking-backend --tail=200
kubectl -n smart-booking logs deploy/smart-booking-backend --since=10m

# only problems
kubectl -n smart-booking logs deploy/smart-booking-backend | grep -E " WARN | ERROR "

# one request's whole trace (id in brackets, e.g. [a936daf2])
kubectl -n smart-booking logs deploy/smart-booking-backend | grep a936daf2

# logs of the previous container, after a crash/restart
kubectl -n smart-booking logs deploy/smart-booking-backend --previous

# daily log file inside the pod
kubectl -n smart-booking exec deploy/smart-booking-backend -- ls /app/logs
kubectl -n smart-booking exec deploy/smart-booking-backend -- tail -f /app/logs/backend-$(date +%F).log

# other components
kubectl -n smart-booking logs deploy/smart-booking-frontend      # nginx access log
kubectl -n smart-booking logs statefulset/smart-booking-mysql
```

The files in `/app/logs` disappear when the pod restarts. To keep them, set in `values-local.yaml`:

```yaml
backend:
  logging:
    persistence:
      enabled: true
```

How to read a log line (`→` call start, `←` result, `✗` error): [docs/11-logging.md](../../docs/11-logging.md).

---

## 4. Emails and database

Ports `18025` and `3308` avoid clashing with the Docker Compose stack (8025, 3307). Each command runs until Ctrl+C.

```bash
# Mailpit (emails sent by the app) → http://localhost:18025
kubectl -n smart-booking port-forward svc/smart-booking-mailpit 18025:8025

# MySQL → DBeaver on localhost:3308, database smart_booking, user smart_booking
kubectl -n smart-booking port-forward svc/smart-booking-mysql 3308:3306
# the password (local test value from values-local.yaml):
kubectl -n smart-booking get secret smart-booking -o jsonpath='{.data.DB_USER_PASSWORD}' | base64 -d; echo
```

---

## 5. Troubleshooting

| Symptom | Look at |
|---|---|
| Pod `Pending` / `ContainerCreating` for minutes | `kubectl -n smart-booking describe pod <name>` → *Events* at the bottom |
| `ErrImagePull` / `ImagePullBackOff` | image not imported: redo step 1.4 |
| `CrashLoopBackOff` | `kubectl -n smart-booking logs <pod> --previous` |
| Backend `0/1 Running` for long | still starting (probe waits for Liquibase); `logs -f` to watch |
| `Init:0/1` on backend | waiting for MySQL; check `kubectl -n smart-booking get pods` |
| Page doesn't load at all | cluster stopped? `k3d cluster list`; port 8080 used by something else? |
| kubectl talks to the wrong cluster / "connection refused" | forgot step 1.3 in this terminal |
| Recent problems in the namespace | `kubectl -n smart-booking get events --sort-by=.lastTimestamp` |

---

## 6. Stop and delete

From least to most destructive:

| Goal | Command | Data |
|---|---|---|
| Pause | `k3d cluster stop smart-booking` | kept |
| Restart one component | `kubectl -n smart-booking rollout restart deploy/smart-booking-backend` | kept |
| Remove the app, keep the database | `helm -n smart-booking uninstall smart-booking` | DB volume kept (reinstall → same data) |
| Wipe the database only (fresh demo data on next install) | `helm -n smart-booking uninstall smart-booking` then `kubectl -n smart-booking delete pvc data-smart-booking-mysql-0` | **DB deleted** |
| Remove everything in the namespace | `kubectl delete namespace smart-booking` | **all app data deleted** |
| **Delete the whole cluster** | `k3d cluster delete smart-booking` | **everything deleted** |

After deleting the cluster, optionally also:

```bash
rm -f ~/.config/k3d/kubeconfig-smart-booking.yaml         # leftover kubeconfig file
docker rmi smart-booking-backend:local smart-booking-frontend:k8s   # the images (rebuild in 1.1)
unset KUBECONFIG                                           # this terminal back to ~/.kube/config
```

Check nothing is left: `k3d cluster list` and `docker ps | grep k3d-smart-booking` print nothing for smart-booking.
