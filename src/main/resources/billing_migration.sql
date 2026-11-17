-- Billing schema migration (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create columns/tables.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

-- ---------------------------------------------------------------------------
-- plans
-- ---------------------------------------------------------------------------
ALTER TABLE "sitebuilder-data".plans
    ADD COLUMN IF NOT EXISTS code varchar(32),
    ADD COLUMN IF NOT EXISTS price_monthly numeric(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS price_yearly numeric(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS currency varchar(3) NOT NULL DEFAULT 'AZN',
    ADD COLUMN IF NOT EXISTS features jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS rank integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;

-- Keep legacy rows compatible with the new plan codes.
UPDATE "sitebuilder-data".plans
SET code = 'STARTER'
WHERE code IS NULL AND upper(name) = 'STARTER';

UPDATE "sitebuilder-data".plans
SET code = 'PRO'
WHERE code IS NULL AND upper(name) = 'PRO';

UPDATE "sitebuilder-data".plans
SET code = 'AGENCY'
WHERE code IS NULL AND upper(name) = 'AGENCY';

CREATE UNIQUE INDEX IF NOT EXISTS ux_plans_code
    ON "sitebuilder-data".plans (code);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM "sitebuilder-data".plans
        WHERE code IS NULL
    ) THEN
        ALTER TABLE "sitebuilder-data".plans ALTER COLUMN code SET NOT NULL;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- subscriptions
-- ---------------------------------------------------------------------------
ALTER TABLE "sitebuilder-data".subscriptions
    ADD COLUMN IF NOT EXISTS billing_cycle varchar(20) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS current_period_end timestamp without time zone,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end boolean NOT NULL DEFAULT false;

-- ---------------------------------------------------------------------------
-- payment_sessions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".payment_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id bigint NOT NULL REFERENCES "sitebuilder-data".workspaces (id),
    plan_id bigint NOT NULL REFERENCES "sitebuilder-data".plans (id),
    billing_cycle varchar(20) NOT NULL,
    amount numeric(10, 2) NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'AZN',
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    failure_reason varchar(500),
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_sessions_workspace_id
    ON "sitebuilder-data".payment_sessions (workspace_id);

CREATE INDEX IF NOT EXISTS idx_payment_sessions_status
    ON "sitebuilder-data".payment_sessions (status);

-- ---------------------------------------------------------------------------
-- payments
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".payments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id bigint NOT NULL REFERENCES "sitebuilder-data".workspaces (id),
    payment_session_id uuid REFERENCES "sitebuilder-data".payment_sessions (id),
    subscription_id bigint REFERENCES "sitebuilder-data".subscriptions (id),
    amount numeric(10, 2) NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'AZN',
    status varchar(20) NOT NULL,
    provider varchar(50) NOT NULL DEFAULT 'fake',
    provider_reference varchar(255),
    failure_reason varchar(500),
    paid_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_workspace_id
    ON "sitebuilder-data".payments (workspace_id);

CREATE INDEX IF NOT EXISTS idx_payments_session_id
    ON "sitebuilder-data".payments (payment_session_id);

-- ---------------------------------------------------------------------------
-- invoices
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".invoices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id bigint NOT NULL REFERENCES "sitebuilder-data".workspaces (id),
    payment_id uuid REFERENCES "sitebuilder-data".payments (id),
    invoice_number varchar(64) NOT NULL,
    amount numeric(10, 2) NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'AZN',
    status varchar(20) NOT NULL DEFAULT 'ISSUED',
    issued_at timestamp without time zone NOT NULL DEFAULT now(),
    due_at timestamp without time zone,
    paid_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_invoice_number
    ON "sitebuilder-data".invoices (invoice_number);

CREATE INDEX IF NOT EXISTS idx_invoices_workspace_id
    ON "sitebuilder-data".invoices (workspace_id);

-- ---------------------------------------------------------------------------
-- payment_methods
-- Never store full card numbers or CVV values here.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".payment_methods (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id bigint NOT NULL REFERENCES "sitebuilder-data".workspaces (id),
    brand varchar(50) NOT NULL,
    last4 varchar(4) NOT NULL,
    exp_month integer NOT NULL,
    exp_year integer NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_methods_last4 CHECK (last4 ~ '^[0-9]{4}$'),
    CONSTRAINT chk_payment_methods_exp_month CHECK (exp_month BETWEEN 1 AND 12)
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_workspace_id
    ON "sitebuilder-data".payment_methods (workspace_id);

-- ---------------------------------------------------------------------------
-- Seed plans
-- ---------------------------------------------------------------------------
INSERT INTO "sitebuilder-data".plans (
    code,
    name,
    price,
    price_monthly,
    price_yearly,
    currency,
    features,
    rank,
    active,
    max_sites,
    custom_domain_allowed,
    remove_branding,
    team_allowed
)
VALUES
    (
        'STARTER',
        'STARTER',
        0.00,
        0.00,
        0.00,
        'AZN',
        '{"maxSites":1,"customDomain":false,"removeBranding":false,"team":false}'::jsonb,
        1,
        true,
        1,
        false,
        false,
        false
    ),
    (
        'PRO',
        'PRO',
        19.00,
        19.00,
        190.00,
        'AZN',
        '{"maxSites":5,"customDomain":true,"removeBranding":false,"team":false}'::jsonb,
        2,
        true,
        5,
        true,
        false,
        false
    ),
    (
        'AGENCY',
        'AGENCY',
        49.00,
        49.00,
        490.00,
        'AZN',
        '{"maxSites":-1,"customDomain":true,"removeBranding":true,"team":true}'::jsonb,
        3,
        true,
        -1,
        true,
        true,
        true
    )
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    price_monthly = EXCLUDED.price_monthly,
    price_yearly = EXCLUDED.price_yearly,
    currency = EXCLUDED.currency,
    features = EXCLUDED.features,
    rank = EXCLUDED.rank,
    active = EXCLUDED.active,
    max_sites = EXCLUDED.max_sites,
    custom_domain_allowed = EXCLUDED.custom_domain_allowed,
    remove_branding = EXCLUDED.remove_branding,
    team_allowed = EXCLUDED.team_allowed;
