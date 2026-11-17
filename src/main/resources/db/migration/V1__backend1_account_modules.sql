-- Additive Backend 1 schema migration
ALTER TABLE IF EXISTS "sitebuilder-data".users
    ADD COLUMN IF NOT EXISTS avatar_url text;
ALTER TABLE IF EXISTS "sitebuilder-data".users
    ADD COLUMN IF NOT EXISTS google_subject varchar(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_google_subject ON "sitebuilder-data".users (google_subject);

CREATE TABLE IF NOT EXISTS "sitebuilder-data".workspace_members
(
    id                    bigserial primary key,
    workspace_id          bigint      not null references "sitebuilder-data".workspaces (id) on delete cascade,
    user_id               bigint      not null references "sitebuilder-data".users (id) on delete cascade,
    role                  varchar(20) not null default 'MEMBER',
    status                varchar(20) not null default 'ACTIVE',
    invitation_token_hash varchar(64),
    invitation_expires_at timestamptz,
    created_at            timestamptz not null default now(),
    constraint uk_workspace_member unique (workspace_id, user_id)
);

CREATE TABLE IF NOT EXISTS "sitebuilder-data".notifications
(
    id         bigserial primary key,
    user_id    bigint        not null references "sitebuilder-data".users (id) on delete cascade,
    type       varchar(40)   not null,
    title      varchar(200)  not null,
    message    varchar(2000) not null,
    created_at timestamptz   not null default now(),
    read_at    timestamptz
);

ALTER TABLE IF EXISTS "sitebuilder-data".notifications
    ADD COLUMN IF NOT EXISTS category varchar(40);

CREATE INDEX IF NOT EXISTS idx_notifications_user_category_created
    ON "sitebuilder-data".notifications (user_id, category, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON "sitebuilder-data".notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON "sitebuilder-data".notifications (user_id, read_at);

CREATE TABLE IF NOT EXISTS "sitebuilder-data".support_tickets
(
    id         bigserial primary key,
    user_id    bigint         not null references "sitebuilder-data".users (id) on delete cascade,
    subject    varchar(160)   not null,
    category   varchar(40)    not null,
    message    varchar(10000) not null,
    status     varchar(20)    not null default 'OPEN',
    created_at timestamptz    not null default now()
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_user_created
    ON "sitebuilder-data".support_tickets (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS "sitebuilder-data".oauth_states
(
    id         bigserial primary key,
    state_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    return_to  varchar(500)
);

ALTER TABLE IF EXISTS "sitebuilder-data".oauth_states
    ADD COLUMN IF NOT EXISTS intent varchar(20) NOT NULL DEFAULT 'LOGIN';

ALTER TABLE IF EXISTS "sitebuilder-data".oauth_states
    ADD COLUMN IF NOT EXISTS linked_user_id bigint references "sitebuilder-data".users (id);
