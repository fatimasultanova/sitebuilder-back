BEGIN;

-- =========================================================
-- USERS
-- Backend 1 additions only.
-- email_verified_at belongs to security_migration.sql.
-- =========================================================

ALTER TABLE IF EXISTS "sitebuilder-data".users
    ADD COLUMN IF NOT EXISTS avatar_url text;

ALTER TABLE IF EXISTS "sitebuilder-data".users
    ADD COLUMN IF NOT EXISTS google_subject varchar(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_google_subject
    ON "sitebuilder-data".users (google_subject);


-- =========================================================
-- WORKSPACE MEMBERS
-- =========================================================

CREATE TABLE IF NOT EXISTS "sitebuilder-data".workspace_members
(
    id                    bigserial PRIMARY KEY,

    workspace_id          bigint NOT NULL
        REFERENCES "sitebuilder-data".workspaces (id)
            ON DELETE CASCADE,

    user_id               bigint NOT NULL
        REFERENCES "sitebuilder-data".users (id)
            ON DELETE CASCADE,

    role                  varchar(20) NOT NULL DEFAULT 'VIEWER',
    status                varchar(20) NOT NULL DEFAULT 'ACTIVE',

    invitation_token_hash varchar(64),
    invitation_expires_at timestamptz,

    created_at            timestamptz NOT NULL DEFAULT now(),

    joined_at             timestamptz,

    CONSTRAINT uk_workspace_member
        UNIQUE (workspace_id, user_id)
);

-- Handles databases where workspace_members already existed
-- before joined_at was introduced.
ALTER TABLE IF EXISTS "sitebuilder-data".workspace_members
    ADD COLUMN IF NOT EXISTS joined_at timestamptz;


-- =========================================================
-- NOTIFICATIONS
-- =========================================================

CREATE TABLE IF NOT EXISTS "sitebuilder-data".notifications
(
    id         bigserial PRIMARY KEY,

    user_id    bigint NOT NULL
        REFERENCES "sitebuilder-data".users (id)
            ON DELETE CASCADE,

    type       varchar(40) NOT NULL,
    title      varchar(200) NOT NULL,
    message    varchar(2000) NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now(),
    read_at    timestamptz
);

ALTER TABLE IF EXISTS "sitebuilder-data".notifications
    ADD COLUMN IF NOT EXISTS category varchar(40);

CREATE INDEX IF NOT EXISTS idx_notifications_user_category_created
    ON "sitebuilder-data".notifications
        (user_id, category, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON "sitebuilder-data".notifications
        (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON "sitebuilder-data".notifications
        (user_id, read_at);


-- =========================================================
-- SUPPORT TICKETS
-- =========================================================

CREATE TABLE IF NOT EXISTS "sitebuilder-data".support_tickets
(
    id         bigserial PRIMARY KEY,

    user_id    bigint NOT NULL
        REFERENCES "sitebuilder-data".users (id)
            ON DELETE CASCADE,

    subject    varchar(160) NOT NULL,
    category   varchar(40) NOT NULL,
    message    varchar(10000) NOT NULL,

    status     varchar(20) NOT NULL DEFAULT 'OPEN',

    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_user_created
    ON "sitebuilder-data".support_tickets
        (user_id, created_at DESC);


-- =========================================================
-- GOOGLE OAUTH STATES
-- =========================================================

CREATE TABLE IF NOT EXISTS "sitebuilder-data".oauth_states
(
    id         bigserial PRIMARY KEY,
    state_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    return_to  varchar(500)
);

ALTER TABLE IF EXISTS "sitebuilder-data".oauth_states
    ADD COLUMN IF NOT EXISTS intent varchar(20)
        NOT NULL DEFAULT 'LOGIN';

ALTER TABLE IF EXISTS "sitebuilder-data".oauth_states
    ADD COLUMN IF NOT EXISTS linked_user_id bigint
        REFERENCES "sitebuilder-data".users (id)
            ON DELETE CASCADE;

COMMIT;
