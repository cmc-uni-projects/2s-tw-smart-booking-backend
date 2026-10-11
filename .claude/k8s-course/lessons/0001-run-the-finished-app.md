# Lesson 0001: Run the finished app (from zero to delete)

**Time:** ~25 min (most of it waiting for builds) · **Win:** you start the whole app on your own local cluster, read its logs, and delete it again, knowing what each command does.

← [Course index](../README.md) · Next: [0002 Your first Pod](./0002-your-first-pod.md) · Cheat sheet: **[k8s runbook](../reference/k8s-runbook.md)** · Terms: [Glossary](../reference/glossary.md)

---

## 1. The mental model (5 min)

You already know Docker Compose: one file starts a few containers on your machine. Kubernetes does the same job, but you describe **what you want** ("1 backend, 1 frontend, 1 MySQL, reachable on this URL") and it keeps making that true: it restarts crashed containers and waits for health checks. ([Kubernetes overview](https://kubernetes.io/docs/concepts/overview/))

Four tools, each with one job:

| Tool | Job | Compose equivalent |
|---|---|---|
| **Docker** | builds the images | same |
| **k3d** | creates a small Kubernetes cluster *inside Docker containers* on your laptop ([k3d.io](https://k3d.io/)) | (none: Compose has no cluster) |
| **kubectl** | talks to the cluster: list, logs, restart, delete ([kubectl docs](https://kubernetes.io/docs/reference/kubectl/)) | `docker compose ps / logs` |
| **Helm** | installs the app from a **chart** (a folder of templates + a values file) ([Helm intro](https://helm.sh/docs/intro/using_helm/)) | `docker compose up` with `docker-compose.yml` |

What runs where once it's up:

```
your machine
└─ Docker
   └─ k3d cluster "smart-booking"           ← k3d cluster create
      └─ namespace "smart-booking"           ← a folder for our objects
         ├─ pod backend   (Spring Boot)      ┐
         ├─ pod frontend  (nginx + React)    │ created by Helm from
         ├─ pod mysql     + volume (data)    │ helm/smart-booking/ +
         ├─ pod mailpit                      │ values-local.yaml
         └─ ingress  smart-booking.localhost ┘
             ↑
browser → localhost:8080 ──(k3d port mapping)──┘
```

Three words to keep: a **pod** is one running container (plus its settings). A **Deployment** keeps N pods of the same image alive; delete a pod and it creates a new one. A **volume** (PVC) is the disk that survives pod restarts: MySQL's data lives there. ([Pods](https://kubernetes.io/docs/concepts/workloads/pods/), [Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/), [PVC](https://kubernetes.io/docs/concepts/storage/persistent-volumes/))

## 2. Why the `export KUBECONFIG=…` line (2 min)

kubectl and helm read a **kubeconfig** file to know *which* cluster to talk to (its address + credentials). The default file `~/.kube/config` points at your other cluster, so we keep smart-booking's in its own file and point this terminal at it:

```bash
export KUBECONFIG=$(k3d kubeconfig write smart-booking)   # writes the file, prints its path
kubectl config current-context                            # → k3d-smart-booking
```

It only lasts for that terminal. New terminal → run it again. ([Organizing cluster access](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/))

## 3. Do it (15 min, mostly waiting)

The current test cluster still exists. Delete it first so you start from zero as in real life:

```bash
k3d cluster delete smart-booking
```

Then follow **[runbook sections 0 and 1](../reference/k8s-runbook.md#0-one-time-install)** exactly. While it builds, predict: *which pod will be ready last, and why?* (Answer in the quiz.)

Checkpoint, all must be true:
- [ ] `kubectl -n smart-booking get pods` shows 4 pods `1/1 Running`
- [ ] http://smart-booking.localhost:8080 opens; you can log in as `customer@travelmate.vn` / `Customer@123`
- [ ] **Forgot password** with `customer@travelmate.vn` → the email shows up in Mailpit ([runbook §4](../reference/k8s-runbook.md#4-emails-and-database))

## 4. Read the logs of what you just did (3 min)

In a second terminal (remember the export!):

```bash
kubectl -n smart-booking logs -f deploy/smart-booking-backend
```

Search for a hotel in the UI and watch the `→` / `←` lines appear. Copy the request id in brackets from one line and run:

```bash
kubectl -n smart-booking logs deploy/smart-booking-backend | grep <id>
```

That's one request's whole trip: controller → services → result. More log recipes: [runbook §3](../reference/k8s-runbook.md#3-logs).

## 5. Stop and delete (1 min)

| You want | Command | Data |
|---|---|---|
| Stop for today | `k3d cluster stop smart-booking` | kept |
| Continue tomorrow | `k3d cluster start smart-booking` (+ export) | kept |
| Fresh database | `helm -n smart-booking uninstall smart-booking` + `kubectl -n smart-booking delete pvc data-smart-booking-mysql-0`, then install again | DB wiped |
| Throw it all away | `k3d cluster delete smart-booking` | everything gone |

Try `stop` → `start` now and check that your login still works: data survived.

---

## Quiz: retrieval practice (answer before opening)

**1. You open a new terminal and run `kubectl get pods -n smart-booking`. It says "No resources found" or talks to the wrong cluster. Most likely cause?**
- A. The cluster was deleted overnight
- B. KUBECONFIG not set in this terminal
- C. Helm release needs an upgrade now

<details><summary>Answer</summary>B. The export only lives in the terminal where you ran it. Check with <code>kubectl config current-context</code>.</details>

**2. You changed backend Java code and rebuilt `smart-booking-backend:local`. The cluster still runs the old code. What's missing?**
- A. Import the image, then restart the deployment
- B. Delete the cluster, then create it again
- C. Run helm uninstall, then delete the PVC

<details><summary>Answer</summary>A. The cluster has its own copy of the image (<code>k3d image import …</code>), and since the tag didn't change, Kubernetes doesn't notice by itself: <code>kubectl rollout restart deploy/smart-booking-backend</code>. B and C work too, but throw away data for nothing. (<a href="../reference/k8s-runbook.md#deploy-a-code-change">runbook</a>)</details>

**3. Which command removes the app but lets you reinstall later with the same bookings in the database?**
- A. `k3d cluster delete smart-booking`
- B. `kubectl delete namespace smart-booking`
- C. `helm -n smart-booking uninstall smart-booking`

<details><summary>Answer</summary>C. Helm doesn't delete the MySQL volume (PVC) created by the StatefulSet, so the next install finds the old data. A and B delete the volume too.</details>

**4. On first start, which pod is ready last?**

<details><summary>Answer</summary>The backend. Its init container waits for MySQL to accept connections, then Liquibase creates the tables and loads the demo data, and only then does the readiness probe (<code>/actuator/health/readiness</code>) pass. The frontend (plain nginx) is ready within seconds.</details>

**5. The backend pod shows `CrashLoopBackOff`. Which command shows *why* it crashed?**

<details><summary>Answer</summary><code>kubectl -n smart-booking logs deploy/smart-booking-backend --previous</code> (logs of the crashed container), and <code>kubectl -n smart-booking describe pod &lt;name&gt;</code> for the events at the bottom.</details>

---

**Primary source:** [Kubernetes Basics tutorial](https://kubernetes.io/docs/tutorials/kubernetes-basics/), the official interactive walkthrough (deploy, explore, expose, scale). Do modules 1–3 (~30 min): they map 1:1 onto what you just did.

**Stuck or curious?** Ask me anything about this lesson: "what's the difference between a Service and an Ingress?", "why a StatefulSet for MySQL?", "how would this run on a real cloud cluster?". I'm your teacher; questions are the point.

Next: [0002 Your first Pod](./0002-your-first-pod.md). You treated the app as a black box here; from now on you build it yourself, one object at a time.
