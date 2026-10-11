# Kubernetes & Helm course, with Smart Booking as the practice app

Why we're doing this: [MISSION.md](./MISSION.md). Sources and communities: [RESOURCES.md](./RESOURCES.md).

**Method: whole → parts → abstraction.** First drive the finished app (Helm, black box). Then rebuild it from plain YAML, one object per lesson, breaking each piece on purpose to read the cluster's errors. Finally rebuild it as your own Helm chart and take it to a real cluster.

## Lessons

| # | Lesson | Win | Project files |
|---|---|---|---|
| 0001 | [Run the finished app](./lessons/0001-run-the-finished-app.md) | start, log, stop, delete the whole app with Helm | `helm/smart-booking` |
| 0002 | [Your first Pod](./lessons/0002-your-first-pod.md) | write a Pod by hand, debug ImagePullBackOff | `k8s/00-namespace.yaml` |
| 0003 | Deployments & Services | self-healing, labels/selectors, service DNS | `k8s/10-mailpit.yaml` |
| 0004 | Secrets, StatefulSets & volumes | MySQL whose data survives pod deletion | `k8s/20-mysql.yaml` |
| 0005 | Configuring and probing the backend | ConfigMap/env, init container, probes; fix a CrashLoopBackOff | `k8s/30-…`, `k8s/31-backend.yaml` |
| 0006 | Frontend + Ingress | the whole app on plain YAML at http://sb.localhost:8080 | `k8s/40-…`, `k8s/50-ingress.yaml` |
| 0007 | Day-2 operations | roll out, roll back, scale, exec, events | all |
| 0008 | Why Helm | templates + values; read our chart against `k8s/` | `helm/smart-booking` |
| 0009 | Write your own chart | turn `k8s/` into a chart: `helm create/template/lint/install` | your chart |
| 0010 | To a real cluster | registry & tags, TLS, secrets, resources, namespaces | `values-dev.yaml` |

Lessons 0003+ are written one at a time, after the previous one, so they fit what you already know. Ask for the next one when you're ready.

## Reference
- [k8s runbook](./reference/k8s-runbook.md): every command to start, log, stop and delete the app on k3d
- [kubectl cheat sheet](./reference/kubectl-cheatsheet.md): verbs + pod status meanings
- [Glossary](./reference/glossary.md): one meaning per term

## The practice code
- [`k8s/`](../../k8s/README.md): the whole app as plain, commented YAML (namespace `sb`)
- [`helm/smart-booking/`](../../helm/smart-booking/README.md): the same app as a Helm chart (namespace `smart-booking`)

Progress notes: `learning-records/` (what you've shown you know) and [NOTES.md](./NOTES.md) (teacher's notes).
