# Glossary

Use these terms consistently in every lesson.

| Term | Meaning in this project |
|---|---|
| **Property** | A listing: hotel, resort, villa or homestay (`Property` entity, `properties` table). UI often says "hotel". |
| **Room** | A *room type* inside a property (e.g. "Deluxe Double"), not a single physical room. `roomAmount` = how many exist (unused by booking logic). |
| **WHOLE room** | The single room record of a VILLA/HOMESTAY; booking one = booking the whole place. |
| **Owner** | User with role `OWNER`, approved through an **owner application**. Also a customer. |
| **Owner application** | Request to become an owner (`OwnerApplication`, table `registerOwner`). |
| **Property submission** | Owner's request to list a property; reviewed by admin (`PENDING → APPROVE/REJECTED`). |
| **Booking** | A reservation of one room for a date range. Has a 1:1 **Payment**. |
| **Night** | Each date from check-in (inclusive) to check-out (exclusive). Priced individually. |
| **Weekend price** | Price for Saturday & Sunday nights (`Room.weekendPrice`). |
| **Overlap check** | Query rejecting a booking if the room has a CONFIRMED/PENDING_PAYMENT booking intersecting the dates. |
| **Payment window** | 5 minutes to pay before the scheduler auto-cancels. |
| **Owner code / Admin code** | Promotion tied to a property vs. platform-wide (`property == null`). One of each may stack. |
| **Penalty / Refund amount** | Computed at cancellation from the property's cancellation policy. |
| **Refund request** | Customer's claim (with bank info) for the refund amount; admin approves/rejects. |
| **Points / Membership rank** | 1 point per 1,000 VND on checkout; BRONZE → SILVER (1k) → GOLD (5k) → DIAMOND (10k). |
| **Suspend** | Admin hides an approved property/room/user (`SUSPENDED`, `isActive=false`). |
| **Audit log / System log** | `SystemLog` row recording who changed a property/room, with old/new JSON. |
| **JWT / access token** | Signed token (HS512, 24h) proving identity; sent as `Authorization: Bearer …`. |
| **Blacklist** | Table of logged-out tokens rejected by the filter. |
| **Filter chain** | Spring Security's ordered list of servlet filters each request passes through. |
| **`hasRole('X')`** | Checks for authority `ROLE_X`. |
| **IDOR** | Insecure Direct Object Reference: accessing someone else's object by changing an ID in the URL. |
| **DTO** | Data Transfer Object — the JSON shape exchanged with the frontend (vs. the JPA entity). |
| **Service file (FE)** | `src/services/*.service.js` — the only place the frontend should call the API from. |

## Kubernetes

Used in [lesson 0002](../lessons/0002-run-it-on-kubernetes.md) and the [k8s runbook](./k8s-runbook.md).

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
