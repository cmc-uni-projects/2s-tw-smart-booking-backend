# Glossary: Kubernetes & Helm

One meaning per term, used the same way in every lesson. Grows with the course.

| Term | Meaning |
|---|---|
| **Kubernetes (k8s)** | System that runs containers from a description of the desired state and keeps it true (restarts, health checks). |
| **Cluster** | The machines (here: Docker containers) Kubernetes runs on. Ours: `smart-booking`. |
| **k3d** | Tool that creates a small Kubernetes cluster (k3s) inside Docker on your laptop. |
| **kubectl** | Command-line client for a cluster: get, logs, describe, exec, delete. |
| **kubeconfig** | File telling kubectl/helm which cluster to talk to and how to log in. Chosen via `KUBECONFIG`, default `~/.kube/config`. |
| **Context** | One cluster + user entry in a kubeconfig. `kubectl config current-context` shows the active one. |
| **Namespace** | A folder inside the cluster grouping objects. Ours: `smart-booking` (hence `-n smart-booking`). |
| **Pod** | One running container (plus settings). Disposable: replaced, not repaired. |
| **Deployment** | Keeps N identical pods running; `rollout restart` replaces them. Backend and frontend. |
| **StatefulSet** | Like a Deployment, but with a stable name and its own disk per pod. MySQL. |
| **PVC (PersistentVolumeClaim)** | A disk that survives pod restarts. `data-smart-booking-mysql-0` holds the database. |
| **Service** | Stable internal address for a set of pods (e.g. `smart-booking-mysql:3306`). |
| **Ingress** | Routes outside HTTP traffic by host/path to Services: `/api` → backend, `/` → frontend. |
| **ConfigMap / Secret** | Settings / credentials injected into pods as environment variables. |
| **Probe** | Health check the cluster calls: *readiness* (send traffic?) and *liveness* (restart?). |
| **Init container** | Runs before the main container; ours waits for MySQL. |
| **Helm / chart / release** | Helm installs a *chart* (templates + `values*.yaml`); one installation is a *release* (`smart-booking`). |
| **port-forward** | `kubectl port-forward` tunnels a cluster Service to `localhost` (Mailpit, MySQL). |
| **CrashLoopBackOff / ImagePullBackOff** | Pod keeps crashing / can't get its image. See runbook §5. |
