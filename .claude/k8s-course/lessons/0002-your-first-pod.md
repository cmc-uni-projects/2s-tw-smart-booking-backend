# Lesson 0002: Your first Pod

**Time:** ~20 min · **Win:** you run Mailpit (the app's email catcher) on Kubernetes from a YAML file **you typed yourself**, open its inbox, break it on purpose, read the cluster's error, fix it, and delete it.

← [Course index](../README.md) · Previous: [0001 Run the finished app](./0001-run-the-finished-app.md) · Next: 0003 Deployments & Services *(coming)* · Cheat sheet: **[kubectl](../reference/kubectl-cheatsheet.md)** · Terms: [Glossary](../reference/glossary.md)

---

## 1. Desired state, in one paragraph (3 min)

With Docker you give **orders**: `docker run`, `docker stop`. With Kubernetes you hand in a **description** ("a container from image X should be running") and the cluster works to make reality match it, and to keep it matching. That description is an **object**, almost always written as YAML and sent with `kubectl apply`. ([Objects in Kubernetes](https://kubernetes.io/docs/concepts/overview/working-with-objects/))

Every object has the same four top-level fields:

```yaml
apiVersion: v1        # which API group/version defines this kind
kind: Pod             # what it is
metadata:             # who it is: name, namespace, labels
  name: mailpit
spec:                 # what you want: differs per kind
  ...
```

After you apply it, the cluster adds a fifth: `status`, what *actually is*. Kubernetes keeps comparing `spec` (wanted) with `status` (real).

The smallest thing you can run is a **Pod**: one or more containers that are scheduled together and share a network address. In this project every pod has one container. ([Pods](https://kubernetes.io/docs/concepts/workloads/pods/))

## 2. Get a cluster and a namespace (2 min)

If `k3d cluster list` doesn't show `smart-booking`, create it (runbook [§1.2](../reference/k8s-runbook.md#12-create-the-cluster)). Then, in **this** terminal:

```bash
export KUBECONFIG=$(k3d kubeconfig write smart-booking)
cd ~/Documents/github-projects/cmc-uni-projects/2s-tw-smart-booking/2s-tw-smart-booking-backend
kubectl apply -f k8s/00-namespace.yaml        # → namespace/sb created
kubectl get namespaces
```

A **namespace** is a folder for objects. Ours is `sb`; you'll type `-n sb` a lot. ([Namespaces](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/))

## 3. Write the Pod (5 min): type it, don't paste

Typing it is the point: you'll remember the shape. Create `~/k8s-practice/pod-mailpit.yaml`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: mailpit
  namespace: sb
  labels:
    app: mailpit
spec:
  containers:
    - name: mailpit
      image: axllent/mailpit:v1.24
      ports:
        - containerPort: 8025      # web inbox (1025 is SMTP)
```

Not sure what a field means or what else you could write? Ask the cluster: `kubectl explain pod.spec.containers` ([kubectl explain](https://kubernetes.io/docs/reference/kubectl/generated/kubectl_explain/)).

## 4. Run it and look at it (5 min)

```bash
kubectl apply -f ~/k8s-practice/pod-mailpit.yaml   # send the description
kubectl -n sb get pods -w                          # watch: Pending → ContainerCreating → Running (Ctrl+C)
kubectl -n sb get pod mailpit -o wide              # + pod IP and node
kubectl -n sb describe pod mailpit                 # everything, with Events at the bottom
kubectl -n sb logs mailpit                         # the container's stdout
kubectl -n sb exec mailpit -- ls /                 # run a command inside it
kubectl -n sb port-forward pod/mailpit 18025:8025  # open http://localhost:18025 (Ctrl+C to stop)
```

Read the **Events** section of `describe` from top to bottom: *Scheduled* (a node was chosen), *Pulling/Pulled* (image), *Created*, *Started*. That's the pod's life story, and the first place to look whenever something is wrong.

## 5. Break it, read the error, fix it (3 min)

In your file, change the image to a tag that doesn't exist, `axllent/mailpit:v0.0.0-nope`, and apply again. Then:

```bash
kubectl -n sb get pod mailpit           # STATUS?
kubectl -n sb describe pod mailpit      # last Events?
```

Write down the STATUS and the reason in the last event *before* opening the answer.

<details><summary>What you should see</summary>STATUS <code>ErrImagePull</code>, then <code>ImagePullBackOff</code>: the kubelet couldn't download the image and now waits longer and longer between retries. The event says something like <code>Failed to pull image "axllent/mailpit:v0.0.0-nope": … not found</code>. Put <code>v1.24</code> back, apply, and it recovers by itself (RESTARTS goes to 1: the container was replaced): desired state again. (The container image is one of the few pod fields you may change in place; most need a delete and re-create. A Deployment does that for you, next lesson.)</details>

## 6. Delete it, and notice what doesn't happen (1 min)

```bash
kubectl -n sb delete pod mailpit
kubectl -n sb get pods                  # "No resources found"
```

Nothing brings it back. A bare Pod is not self-healing: if its node dies or someone deletes it, it's gone. That's why the real manifests never create Pods directly. They create a **Deployment**, which creates the pods and replaces them. Next lesson: [`k8s/10-mailpit.yaml`](../../../k8s/10-mailpit.yaml).

**Bonus trick:** let kubectl write the YAML skeleton for you, then edit it:

```bash
kubectl -n sb run mailpit --image=axllent/mailpit:v1.24 --port=8025 --dry-run=client -o yaml
```

---

## Quiz: retrieval practice (answer before opening)

**1. Which top-level field do *you* never write, because the cluster fills it in?**
- A. `metadata`
- B. `spec`
- C. `status`

<details><summary>Answer</summary>C. <code>spec</code> is what you want; <code>status</code> is what the cluster observed.</details>

**2. A pod shows `ImagePullBackOff`. Where do you look first?**
- A. Container logs with `kubectl logs`
- B. Events in `kubectl describe pod`
- C. Full YAML from `kubectl get`

<details><summary>Answer</summary>B. The container never started, so there are no logs yet. The events say which image failed and why (wrong tag, private registry, no network).</details>

**3. You run `kubectl get pods` and see nothing, but you know mailpit is running. Most likely?**
- A. You forgot `-n sb`
- B. The pod has crashed
- C. Port-forward is not active

<details><summary>Answer</summary>A. Without <code>-n</code>, kubectl looks in the <code>default</code> namespace. (Or this terminal has no KUBECONFIG export, see lesson 0001.)</details>

**4. From memory, without scrolling up: write the four fields you write in every object, and what `metadata` must contain at least.**

<details><summary>Answer</summary><code>apiVersion</code>, <code>kind</code>, <code>metadata</code>, <code>spec</code>. <code>metadata.name</code> at least (and <code>namespace</code>, or it goes to <code>default</code>).</details>

**5. Why would deleting a Pod in production be a bad way to "restart" an app, if the app is a bare Pod?**

<details><summary>Answer</summary>Nothing re-creates a bare Pod. It stays deleted until someone applies it again. A Deployment replaces deleted pods automatically.</details>

---

**Primary source:** [Pods](https://kubernetes.io/docs/concepts/workloads/pods/) in the official Kubernetes docs (~10 min). Read the first sections up to "Working with Pods", and see if you can now explain why "Pods are designed as relatively ephemeral, disposable entities".

**Questions?** Ask me anything about this lesson: "what does the kubelet do?", "why can a pod have several containers?", "what's the difference between `apply` and `create`?". I'm your teacher; questions are how this sticks.
