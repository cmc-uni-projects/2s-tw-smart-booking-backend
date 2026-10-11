# Database Migrations (Liquibase) & Seed Data

The schema is owned by **Liquibase** (`liquibase-core`, version managed by Spring Boot 3.5 → 4.31). Hibernate only **validates** it (`spring.jpa.hibernate.ddl-auto=validate`): if an entity and the database disagree, the app refuses to start.

## Files

```
src/main/resources/db/changelog/
  db.changelog-master.yaml          ← include list, in order
  changes/
    0001-baseline-schema.sql        ← the 25 tables as Hibernate generated them (FK-safe order)
    0002-reference-data.sql         ← roles (CUSTOMER/ADMIN/OWNER) + 20 amenities — every environment
    0100-demo-data.sql              ← demo dataset — context `demo` only (generated)
scripts/generate_demo_seed.py       ← regenerates 0100 (see "Editing the demo data")
```

All changesets are **formatted SQL** (`--liquibase formatted sql`), author `travelmate`.

## Contexts: what runs where

| Setting | Runs |
|---|---|
| `spring.liquibase.contexts=${LIQUIBASE_CONTEXTS:prod}` (default **prod**) | baseline + reference data |
| `LIQUIBASE_CONTEXTS=demo` (local `.env`) | baseline + reference data + **demo data** |

Changesets without a context run everywhere; `context:demo` ones only when the context is active. Because the default is `prod`, forgetting the variable never loads demo accounts into a real database.

> Note: `DotenvEnvironmentPostProcessor` puts `.env` **ahead of** real environment variables, so set `LIQUIBASE_CONTEXTS` in the `.env` the app reads.

## Existing databases (the deployed TiDB)

Every changeset has a precondition with `onFail:MARK_RAN`:
- **baseline** — skipped if table `users` already exists;
- **roles / amenities** — skipped if the table already has rows;
- **demo users** — skipped if any seed email already exists; the other demo changesets require the demo admin's fixed UUID.

So on first deploy against the existing DB, Liquibase creates `DATABASECHANGELOG`/`DATABASECHANGELOGLOCK`, **marks** all changesets as ran without executing them, and the app starts. Verified locally against a Hibernate-created database (all 9 marked as ran, validation passed) and against an empty one (all applied, 294 rows).

`config/DataInitializer` was **removed** — Liquibase now provides roles and amenities. ⚠️ A brand-new *production* database therefore has **no admin account**: create one with a one-off SQL insert (BCrypt hash) or a dedicated changeset with its own context.

## Demo dataset (`context:demo`)

| | |
|---|---|
| Accounts | `admin@` (Admin@123), `owner@`, `owner2@` (Owner@123), `customer@`, `customer2@`, `customer3@` (Customer@123) — all `@travelmate.vn`, verified, fixed UUIDs `00000000-0000-4000-8000-00000000000N` |
| Owner applications | 2 approved (the owners), 1 **pending** (customer3) → shows in Admin › approvals |
| Properties | 8 approved across Hà Nội, Sa Pa, Hạ Long, Đà Lạt, TP.HCM, Đà Nẵng, Hội An, Phú Quốc + 1 **pending** (Mũi Né); details, policies, photos, amenities |
| Rooms | 15 (villas/homestays use one `WHOLE` room), weekday + weekend prices, photos, amenities |
| Promotions | WELCOME10, GOLD15 (rank-gated), FLASH200K (platform) · HANOI50K, DANANG20 (owner) · SUMMER2026 (expired) |
| Bookings | 8: completed (with reviews), upcoming confirmed, one cancelled with a **pending refund request**; payments for all |
| Consistency | totals follow `BookingService` (Sat/Sun nights = weekend price); points = completed spend / 1,000 → ranks SILVER/DIAMOND/GOLD; property `rating`/`reviewCount` = seeded reviews |
| Images | public Unsplash URLs. `FileStorageService.generateSignedUrl` now returns `http(s)` URLs unchanged (only S3 keys are presigned). |
| Text | descriptions/reviews in Vietnamese (database content is not translated by the UI) |

## Everyday tasks

**Reset local DB to a clean demo state**
```bash
cd 2s-tw-smart-booking-backend
docker compose stop backend mysql && docker compose rm -f backend mysql
docker volume rm smart-booking-backend_mysql-data
docker compose up -d
```

**Change the schema**
1. Change the entity.
2. Add a **new** file, e.g. `changes/0003-add-room-view-column.sql`:
   ```sql
   --liquibase formatted sql
   --changeset yourname:0003-add-room-view-column
   ALTER TABLE rooms ADD COLUMN viewType varchar(50) NULL;
   --rollback ALTER TABLE rooms DROP COLUMN viewType;
   ```
3. Include it in `db.changelog-master.yaml` (before the demo file).
4. Restart; if Hibernate validation fails, the entity and SQL disagree.

**Never edit a changeset that has already run** anywhere — Liquibase stores an MD5 checksum and will refuse to start (`Validation Failed: checksum changed`). Write a new changeset instead.

**Editing the demo data** — before it has been applied somewhere you care about, edit `scripts/generate_demo_seed.py` and run `python3 scripts/generate_demo_seed.py`, then reset your local DB. After that, add a new `context:demo` changeset (`0101-…`) rather than regenerating.

## References
- Liquibase formatted SQL — https://docs.liquibase.com/concepts/changelogs/sql-format.html
- Preconditions — https://docs.liquibase.com/concepts/changelogs/preconditions.html
- Contexts — https://docs.liquibase.com/concepts/changelogs/attributes/contexts.html
- Spring Boot + Liquibase — https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase
