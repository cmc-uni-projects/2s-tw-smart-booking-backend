# Teacher's notes: k8s course

## User preferences
- Lessons and references as **Markdown** in this folder (same rule as the parent workspace); quiz answers in `<details>`.
- Explain from first principles and relate to Docker Compose, which they know.

## Working notes
- 2026-10-11: Course started on request ("teach me all about k8s… use this project as practice… generate k8s code"). Generated the full plain-YAML app in `k8s/` (verified on a throwaway k3d cluster: UI, login, email to Mailpit, log file) so lessons can *read, apply, break and fix* real files, then write their own.
- Pedagogy: whole → parts → abstraction. 0001 drives the finished Helm install (black box); 0002–0006 rebuild it from plain YAML one object at a time; 0008–0009 rebuild it as a Helm chart. Each lesson has a "break it" exercise for a tight feedback loop (the cluster's error is the feedback).
- Real bug found while verifying: empty `GOOGLE_CLIENT_ID` → Spring crash → CrashLoopBackOff. Reuse as the debugging exercise in lesson 0005 (set it back to "" and diagnose).
- Cluster name for practice: `smart-booking` (runbook). `k8s/` uses namespace `sb` and host `sb.localhost`, so it coexists with the Helm install in namespace `smart-booking`.
- The other kube context `k3d-ecommerce` belongs to another project: never touch it; keep smart-booking in its own kubeconfig.
- Local images `smart-booking-backend:local` and `smart-booking-frontend:k8s` were rebuilt 2026-10-11 and are present in Docker.
