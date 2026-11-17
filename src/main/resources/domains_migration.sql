-- Custom domains schema migration (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create tables.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

CREATE TABLE IF NOT EXISTS "sitebuilder-data".site_domains (
    id bigserial PRIMARY KEY,
    site_id bigint NOT NULL REFERENCES "sitebuilder-data".sites (id) ON DELETE CASCADE,
    hostname varchar(253) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    ssl_status varchar(20) NOT NULL DEFAULT 'PENDING',
    verification_token varchar(128) NOT NULL,
    verified_at timestamp without time zone,
    last_checked_at timestamp without time zone,
    error_message varchar(500),
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT ux_site_domains_site_id UNIQUE (site_id),
    CONSTRAINT ux_site_domains_hostname UNIQUE (hostname)
);

ALTER TABLE "sitebuilder-data".site_domains
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS ssl_status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS verification_token varchar(128),
    ADD COLUMN IF NOT EXISTS verified_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS last_checked_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS error_message varchar(500),
    ADD COLUMN IF NOT EXISTS created_at timestamp without time zone NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone NOT NULL DEFAULT now();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ux_site_domains_site_id'
          AND conrelid = '"sitebuilder-data".site_domains'::regclass
    ) THEN
        ALTER TABLE "sitebuilder-data".site_domains
            ADD CONSTRAINT ux_site_domains_site_id UNIQUE (site_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ux_site_domains_hostname'
          AND conrelid = '"sitebuilder-data".site_domains'::regclass
    ) THEN
        ALTER TABLE "sitebuilder-data".site_domains
            ADD CONSTRAINT ux_site_domains_hostname UNIQUE (hostname);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_site_domains_status
    ON "sitebuilder-data".site_domains (status);
