# Resources: Kubernetes & Helm

All links checked 2026-10-11.

## Knowledge
- [Kubernetes Basics tutorial](https://kubernetes.io/docs/tutorials/kubernetes-basics/) — official interactive walkthrough (deploy, explore, expose, scale). Use for: first contact; companion to lessons 0001–0003.
- [Kubernetes Concepts](https://kubernetes.io/docs/concepts/) — the official explanation of every object. Use for: "what is X and why does it exist".
  - [Objects in Kubernetes](https://kubernetes.io/docs/concepts/overview/working-with-objects/) (apiVersion/kind/metadata/spec, desired state)
  - [Pods](https://kubernetes.io/docs/concepts/workloads/pods/), [Pod lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)
  - [Namespaces](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/), [Recommended labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
- [kubectl Quick Reference](https://kubernetes.io/docs/reference/kubectl/quick-reference/) — official cheat sheet. Use for: commands beyond [our cheat sheet](./reference/kubectl-cheatsheet.md).
- [`kubectl explain`](https://kubernetes.io/docs/reference/kubectl/generated/kubectl_explain/) — the built-in field documentation (`kubectl explain pod.spec.containers`). Use for: "what can I write here".
- [Debug Pods](https://kubernetes.io/docs/tasks/debug/debug-application/debug-pods/) — official troubleshooting guide. Use for: any pod that isn't Running/Ready.
- [k3d docs](https://k3d.io/) — local clusters in Docker. Use for: cluster create/stop/start/delete, image import, port mapping.
- [Helm — Using Helm](https://helm.sh/docs/intro/using_helm/) and [Chart Template Guide](https://helm.sh/docs/chart_template_guide/) — official. Use for: the Helm lessons; reading `helm/smart-booking/templates`.

## Skills (practice)
- This project: [`k8s/`](../../k8s/README.md) (plain YAML) and [`helm/smart-booking`](../../helm/smart-booking/README.md) (chart). Each lesson builds or breaks one piece.
- [CKAD curriculum (CNCF)](https://www.cncf.io/training/certification/ckad/) — the Kubernetes application developer certification. Its domains match this course; optional goal for later.

## Wisdom (communities)
- [Kubernetes Slack](https://slack.k8s.io/) — `#kubernetes-novice` for beginner questions, `#helm-users` for Helm.
- [r/kubernetes](https://www.reddit.com/r/kubernetes/) — design questions and "how do you do X in production".
- [Stack Overflow `kubernetes` tag](https://stackoverflow.com/questions/tagged/kubernetes) — concrete, reproducible errors (include `kubectl describe` output).

## Gaps
- No resource chosen yet for production topics (TLS with cert-manager, GitOps with Argo CD). Pick when the course reaches lesson 0010.
