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
