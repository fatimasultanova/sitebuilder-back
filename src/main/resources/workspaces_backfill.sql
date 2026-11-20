-- Workspaces backfill (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create rows.
--
-- Railway does not execute .sql files on deploy. This file must be applied
-- manually to the database that DATABASE_URL points to, once, before the new
-- backend version starts. Re-running it is safe.
--
-- New registrations create their workspace in AuthService.register(); this
-- script only covers users created before that behaviour existed.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

-- One workspace per existing owner that does not already have one.
-- The NOT EXISTS guard makes the insert safe to re-run.
INSERT INTO "sitebuilder-data".workspaces (name, owner_id, created_at)
SELECT COALESCE(NULLIF(trim(u.name), ''), split_part(u.email, '@', 1)) || '''s workspace', u.id, now()
FROM "sitebuilder-data".users u
WHERE NOT EXISTS (SELECT 1 FROM "sitebuilder-data".workspaces w WHERE w.owner_id = u.id);
