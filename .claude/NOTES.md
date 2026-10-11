# Teacher's Notes

## User preferences
- 2026-10-10: Wants **everything as `.md` files** inside `2s-tw-smart-booking-backend/.claude/` (overrides the default HTML lessons). Lessons use `<details>` blocks for quiz answers so retrieval practice still works in Markdown.
- Codebase comments/UI are Vietnamese; docs are written in English — ask whether Vietnamese docs are preferred.

## Workspace layout
- `docs/` = project documentation (static knowledge about the code).
- `reference/` = compressed cheat sheets & glossary (revisited often).
- `lessons/` = numbered lessons, one tangible win each.
- `learning-records/` = created lazily when the user demonstrates understanding.

## Working notes
- Mission is a DRAFT — confirm at start of next session.
- Unknown: user's prior experience with Spring / React. Lesson 0001 quiz results will tell. Ask.
- 2026-10-10: Local Docker stack is up (docs/08). Lessons can now include hands-on steps against http://localhost:5173 / :8386 and Mailpit.
- Candidate next lessons: (2) JWT + SecurityFilterChain deep dive, (3) Booking state machine & pricing, (4) run both apps locally (fix port/env mismatch), (5) fix IDOR on bookings.
- 2026-10-11: User says they are **not familiar with Kubernetes** (asked why `export KUBECONFIG` is needed). Explain k8s from first principles, tie to Docker Compose which they know. Asked for a full start/logs/stop/delete doc → lesson 0002 + `reference/k8s-runbook.md`.
- Mission's "Out of scope: DevOps" is now partly outdated (they want to deploy with Helm). Ask whether to add "run/deploy on Kubernetes" to MISSION.md.
- Their other cluster context `k3d-ecommerce` lives in `~/.kube/config`; smart-booking is kept in a separate kubeconfig on purpose. Never touch ecommerce.
