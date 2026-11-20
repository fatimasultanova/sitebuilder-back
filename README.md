# sitebuilder-back

## Activity, deployments and custom domains

### Before starting this version

Hibernate runs with `ddl-auto=validate`, so the activity table must exist first.
Apply the supplied migration once per database, then deploy:

```bash
psql "$DATABASE_URL" -f src/main/resources/activity_migration.sql
```

Also apply the workspace backfill once per database (Railway does not run .sql
files on deploy), so pre-existing users get a workspace:

```bash
psql "$DATABASE_URL" -f src/main/resources/workspaces_backfill.sql
```

The table stores who acted, which site the event belongs to, a human-readable
summary and a timestamp. It deliberately has no foreign key to `sites`, so the
trail survives a site deletion and a deleted site cannot erase its own history.
Events are written in the same transaction as the mutation they describe; a
rolled-back site change never leaves an event.

### Account endpoints

```text
GET  /api/activity?page=0&size=100      account activity (also /activity)
GET  /api/usage                        measured website/deployment counts
GET  /api/usage/limits                 plan limit for websites
GET  /api/sites/{id}/publication       current published state
POST /api/sites/{id}/publication       queue a publish (202 + deployment)
DELETE /api/sites/{id}/publication     unpublish (204)
```

The account is always taken from the authenticated principal, never from a
query parameter. Activity pages are capped at 200 rows. Usage only returns
figures that were actually measured; absent metrics stay absent rather than
becoming zero.

### Deployment rules

- Only one `QUEUED`/`BUILDING` deployment may exist per site. A second request
  receives `409`.
- A site lock serialises deploy, rollback and unpublish. If an unpublish wins
  while a build is waiting, the pipeline re-reads the deployment state and does
  not publish it.
- Rollback is allowed only for a finished `SUPERSEDED` deployment and is refused
  while another build is in progress.
- Unpublish retires live rows to `SUPERSEDED`, stops pending rows and sets the
  site back to `DRAFT`, which makes the public lookup return `404`.
- Public snapshots under `/api/public/sites/**` allow any read-only origin
  without credentials. Account endpoints keep the dashboard allowlist.

### Domain verification

- Hostnames are validated as real public names, not by counting dots. Multi-label
  public suffixes such as `example.co.uk` are handled correctly, and a public
  suffix by itself (`co.uk`) is rejected.
- Repeating `PUT` with the same hostname is idempotent and does not rotate the
  verification token or reset certificate state.
- A DNS resolver outage preserves a previously verified state. A successful TXT
  lookup is ownership proof only; it is not proof that A/CNAME routing or HTTPS
  is working.
- `LoggingSslProvisioner` still only logs. `ssl` remains `pending` until a real
  certificate provider is integrated; no code claims a certificate exists.

### Verification

```bash
./gradlew test bootJar
```

The suite covers controller security/CORS, persistence and rollback of activity
events against H2 in PostgreSQL mode, deployment conflicts and unpublish,
multi-label apex detection, idempotent domain PUT and resolver-outage handling.

## Email (Brevo on Railway)

Auth emails (email verification and password reset) are sent through the Brevo
transactional REST API over HTTPS. SMTP is intentionally not used: Railway's
plan blocks all outbound SMTP ports, so `JavaMailSender` is not on the classpath
and no mail is sent over SMTP.

### Brevo setup

1. Create a Brevo account and verify the sender address (or, preferably, a whole
   domain). An unverified sender is rejected with HTTP 400.
2. Create an API key under **SMTP & API > API Keys**. It must start with
   `xkeysib-` and have **Transactional email = All**. Do not use an SMTP key.
3. Check **Security > Authorized IPs**. If IP restriction is enabled, Railway's
   outbound IP will not be authorized and Brevo returns HTTP 401. Disable the
   restriction or allow the Railway egress IP.
4. A Gmail-address sender can work after verification but may land in spam.
   A verified own domain with DKIM/SPF configured is the reliable option.

### Railway variables

| Variable | Default | Notes |
| --- | --- | --- |
| `MAIL_PROVIDER` | `brevo` | Only `brevo` is implemented. |
| `BREVO_API_KEY` | empty | Required only when email delivery is enabled. |
| `BREVO_BASE_URL` | `https://api.brevo.com` | |
| `BREVO_CONNECT_TIMEOUT` | `3s` | |
| `BREVO_READ_TIMEOUT` | `8s` | |
| `BREVO_MAX_RETRIES` | `2` | Connection failures/connect timeouts and 429 only. |
| `AUTH_EMAIL_ENABLED` | `false` | `true` enables real Brevo delivery. |
| `AUTH_EMAIL_FROM` | `Site Builder <no-reply@sitebuilder.local>` | `Name <email>` or plain email. |
| `FRONTEND_BASE_URL` | `http://localhost:3000` | No trailing slash, no path. |
| `AUTH_EMAIL_LOCALE` | `az` | `az` or `en`. |
| `AUTH_RESEND_COOLDOWN` | `60s` | Minimum time between verification resends. |

With `AUTH_EMAIL_ENABLED=false` the app starts even if `BREVO_API_KEY` is blank
or `AUTH_EMAIL_FROM` is invalid. When it is `true` with `MAIL_PROVIDER=brevo`,
startup fails with a readable message if the API key is missing or the sender
cannot be parsed.

### Free-plan limit

The Brevo free plan allows about 300 emails/day. Quota/rate-limit errors are
logged as WARN. For higher volume, upgrade the plan or use your own SMTP relay
over an allowed HTTPS provider.

### Testing locally

Disable delivery and copy the link from the application log:

```bash
AUTH_EMAIL_ENABLED=false ./gradlew bootRun
```

`LoggingEmailSender` logs the verification/reset link at INFO. This is the only
place where a link is ever logged.

To test with a real key (no email is sent unless `AUTH_EMAIL_ENABLED=true`):

```bash
AUTH_EMAIL_ENABLED=true \
MAIL_PROVIDER=brevo \
BREVO_API_KEY=xkeysib-... \
AUTH_EMAIL_FROM='Site Builder <verified-sender@example.com>' \
FRONTEND_BASE_URL='https://your-frontend.example' \
./gradlew bootRun
```

Emails are published as events and sent asynchronously only after the database
transaction commits. A rollback never sends an email, and a Brevo failure never
fails or slows the register/resend/forgot-password HTTP response.
