# Starting point: knows Docker/Compose, new to Kubernetes

The user ran this project with Docker Compose (builds, `.env`, logs, Mailpit, DBeaver on a mapped port) but said they are "not familiar with k8s"; their first question was why `export KUBECONFIG=…` is needed. So: don't re-teach images/containers/ports, do teach every Kubernetes object and the client/cluster split from scratch, using Compose as the bridge.

**Evidence:** earlier sessions (Compose stack, DBeaver connection) and the KUBECONFIG question, 2026-10-11.

**Implications:** the mission moved from "own the codebase" (parent workspace) to a dedicated Kubernetes + Helm mission, see [MISSION.md](../MISSION.md). First lessons stay concrete and hands-on (one object per lesson), with no Helm templating until the plain YAML is understood.
