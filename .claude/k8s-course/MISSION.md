# Mission: Deploy the Smart Booking System to Kubernetes, and understand every line

## Why
Learn Kubernetes and Helm by using this project (Spring Boot + React + MySQL) as the practice app: be able to deploy it to a cluster yourself, change it, debug it, and explain why each object exists, instead of copying commands.

## Success looks like
- Write the manifests for one component (Deployment, Service, ConfigMap/Secret, probes) without copying, and get it running.
- Deploy the whole app from the plain YAML in `k8s/`, then from your own Helm chart.
- Diagnose a broken pod (ImagePullBackOff, CrashLoopBackOff, Pending, not Ready) with `get / describe / logs / events` in under 5 minutes.
- Roll out a new backend version, roll it back, and change config safely.
- Explain what changes when moving from the local k3d cluster to a real one (registry, TLS, secrets, resources).

## Constraints
- Local practice cluster: k3d on this laptop. Never touch the other cluster context (`k3d-ecommerce`).
- Lessons as Markdown files in this folder; short, one tangible win each.
- Starting point: knows Docker and Docker Compose; new to Kubernetes.

## Out of scope (for now)
- Running Kubernetes itself (control-plane internals, kubeadm, etcd operations), service meshes, writing operators.
- Cloud-provider specifics until the "real cluster" lesson.
