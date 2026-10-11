# Smart Booking System: Learning Workspace & Project Docs

Teaching workspace for the TravelMate / Smart Booking System (backend: this repo; frontend: `../../2s-tw-smart-booking-frontend`).

## Start here
1. [MISSION.md](./MISSION.md): why we're learning this (draft, please confirm)
2. [lessons/0001-the-big-picture.md](./lessons/0001-the-big-picture.md): first lesson

## Project documentation (`docs/`)
| Doc | What's inside |
|---|---|
| [01 Project overview](./docs/01-project-overview.md) | what the product does, roles, capabilities |
| [02 Tech stack](./docs/02-tech-stack.md) | every library (BE + FE), used or not, env vars |
| [03 Architecture](./docs/03-architecture.md) | system diagram, layers, request lifecycle, security, jobs |
| [04 Domain model](./docs/04-domain-model.md) | ER diagram, key entities, status enums |
| [05 Business flows](./docs/05-business-flows.md) | auth, owner onboarding, booking → payment → refund, loyalty, AI… |
| [06 API map](./docs/06-api-map.md) | every endpoint with auth rule and FE caller |
| [07 Gotchas & risks](./docs/07-gotchas-and-risks.md) | setup mismatches, security holes, logic bugs: great exercises |
| [08 Running locally](./docs/08-running-locally.md) | docker compose for both repos, URLs, accounts, what doesn't work |
| [09 Database migrations](./docs/09-database-migrations.md) | Liquibase changelogs, contexts, demo seed data, how to change the schema |
| [10 i18n](./docs/10-i18n.md) | Vietnamese/English across frontend, API messages, notifications and emails; coding rules and checks |
| [11 Logging](./docs/11-logging.md) | AOP logging aspect, request ids, log levels, masking |

## Quick reference (`reference/`)
- [Glossary](./reference/glossary.md)
- [Code map: "where is X?"](./reference/code-map.md)

## Lessons (`lessons/`)
| # | Lesson | Win |
|---|---|---|
| 0001 | [The big picture](./lessons/0001-the-big-picture.md) | trace a request through both repos |

## Other
- [RESOURCES.md](./RESOURCES.md): trusted sources & communities
- [NOTES.md](./NOTES.md): teacher's notes / your preferences
- `learning-records/`: created once you demonstrate what you've learned
