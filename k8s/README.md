# Plain Kubernetes manifests (learning version)

The whole app as raw YAML, one file per concept, numbered in the order the lessons introduce them. The Helm chart in [`../helm/smart-booking`](../helm/smart-booking) produces almost the same objects from templates. These files are what it looks like *without* Helm.

| File | Objects | Lesson |
|---|---|---|
| `00-namespace.yaml` | Namespace `sb` | [0002](../.claude/k8s-course/lessons/0002-your-first-pod.md) |
| `10-mailpit.yaml` | Deployment + Service `mailpit` | 0003 |
| `20-mysql.yaml` | Service + StatefulSet `mysql` (+ PVC `data-mysql-0`) | 0004 |
| `30-backend-config.yaml` | ConfigMap `backend-config` | 0005 |
| `31-backend.yaml` | Deployment + Service `backend` | 0005 |
| `40-frontend.yaml` | Deployment (2 replicas) + Service `frontend` | 0006 |
| `50-ingress.yaml` | Ingress: `sb.localhost` → `/api…` backend, `/` frontend | 0006 |
| *(not in git)* | Secret `sb-secrets`, created with the command below | 0004 |

It runs in its own namespace `sb` with host `sb.localhost`, so it can sit next to the Helm install (`smart-booking` / `smart-booking.localhost`) in the same k3d cluster.

## Run everything

Prerequisites: the k3d cluster and the two images from the [runbook §1.1–1.4](../.claude/k8s-course/reference/k8s-runbook.md#1-first-start-from-nothing).

```bash
export KUBECONFIG=$(k3d kubeconfig write smart-booking)
cd 2s-tw-smart-booking-backend/k8s

kubectl apply -f 00-namespace.yaml

# Secrets never go into git: create them from the command line (random values for a local cluster)
kubectl -n sb create secret generic sb-secrets \
  --from-literal=DB_USER_PASSWORD="$(openssl rand -hex 12)" \
  --from-literal=MYSQL_ROOT_PASSWORD="$(openssl rand -hex 12)" \
  --from-literal=JWT_SECRET="$(openssl rand -hex 64)" \
  --from-literal=MAIL_PASSWORD= \
  --from-literal=GEMINI_API_KEY= \
  --from-literal=R2_ACCESS_KEY=unused \
  --from-literal=R2_SECRET_KEY=unused \
  --from-literal=GOOGLE_CLIENT_SECRET= \
  --from-literal=FACEBOOK_CLIENT_SECRET=

kubectl apply -f .                       # every file in this folder (alphabetical, so the namespace comes first)
kubectl -n sb get pods -w                # wait for 5 pods Running and 1/1 (backend ~2 min)
```

Open **http://sb.localhost:8080** (demo logins in the [runbook](../.claude/k8s-course/reference/k8s-runbook.md)). Emails: `kubectl -n sb port-forward svc/mailpit 18025:8025` → http://localhost:18025.

## Change, check, delete

```bash
kubectl diff -f .                         # what would change (before apply)
kubectl apply -f .                        # make the cluster match the files
kubectl -n sb rollout restart deploy/backend   # after ConfigMap/Secret changes or a new image
kubectl -n sb logs -f deploy/backend      # backend logs

kubectl delete -f 50-ingress.yaml         # remove one piece
kubectl delete namespace sb               # remove everything, including the database disk and the Secret
```

The Secret holds the MySQL passwords. MySQL only reads them on its first start (empty disk). If you recreate the Secret with new values but keep the PVC, the backend can't log in. Delete the namespace to start fresh.
