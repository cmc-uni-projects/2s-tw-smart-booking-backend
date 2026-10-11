# kubectl cheat sheet (grows with the course)

Pattern: `kubectl <verb> <kind>/<name> -n <namespace> [flags]`. Our namespaces: `sb` (plain YAML, [`k8s/`](../../../k8s/README.md)) and `smart-booking` (Helm). Official full list: [kubectl Quick Reference](https://kubernetes.io/docs/reference/kubectl/quick-reference/).

## Which cluster am I talking to?

```bash
export KUBECONFIG=$(k3d kubeconfig write smart-booking)   # every new terminal
kubectl config current-context                            # → k3d-smart-booking
```

## Send descriptions (declarative)

| Command | Does |
|---|---|
| `kubectl apply -f file.yaml` | create or update the objects in the file |
| `kubectl apply -f k8s/` | every file in the folder (alphabetical) |
| `kubectl diff -f file.yaml` | show what `apply` would change |
| `kubectl delete -f file.yaml` | delete the objects in the file |
| `kubectl explain pod.spec.containers` | built-in docs for any field |
| `kubectl run NAME --image=IMG --dry-run=client -o yaml` | print a Pod YAML skeleton |

## Look (lesson 0002)

| Command | Shows |
|---|---|
| `kubectl get pods -n sb` | list (`-w` to watch, `-o wide` for IP/node, `-o yaml` for everything) |
| `kubectl get all -n sb` | pods, services, deployments, statefulsets… |
| `kubectl describe pod NAME -n sb` | details + **Events** (first stop when something's wrong) |
| `kubectl logs NAME -n sb` | container stdout (`-f` follow, `--previous` the crashed one, `--tail=100`) |
| `kubectl exec NAME -n sb -- CMD` | run a command in the container (`-it … -- sh` for a shell) |
| `kubectl port-forward pod/NAME 18025:8025 -n sb` | tunnel to `localhost` |
| `kubectl get events -n sb --sort-by=.lastTimestamp` | recent events in the namespace |

## Pod STATUS → what it means

| STATUS | Meaning | Look at |
|---|---|---|
| `Pending` | no node chosen yet (resources? volume?) | `describe` → Events |
| `ContainerCreating` | pulling image / mounting volumes | `describe` → Events |
| `ErrImagePull` / `ImagePullBackOff` | image can't be downloaded (tag, registry, not imported) | `describe` → Events |
| `CreateContainerConfigError` | a referenced ConfigMap/Secret key is missing | `describe` → Events |
| `Running` but `0/1` READY | started, readiness probe not passing yet | `logs`, `describe` |
| `CrashLoopBackOff` | container keeps exiting; kubelet waits longer each time | `logs --previous` |
| `Completed` | finished with exit 0 (init containers, jobs) | normal |

Official guide: [Debug Pods](https://kubernetes.io/docs/tasks/debug/debug-application/debug-pods/).
