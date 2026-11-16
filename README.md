# sitebuilder-back

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

## Backend 1 account and workspace API

All routes below use the `/api` prefix and require `Authorization: Bearer <session-token>` except auth register/login/recovery/verification and OAuth start/callback. List endpoints use zero-based `page` and `size` parameters (default 20, maximum 100) and return Spring `Page` metadata.

### Authentication

- `POST /api/auth/register` (201), `/login` (200), `/logout` (204)
- `POST /api/auth/forgot-password`, `/reset-password` (204), `/resend-verification`, `/verify-email`
- `POST /api/auth/oauth/google/start` with `{ "intent": "login", "returnTo": "/sites" }` returns `authorizationUrl`; callback accepts `{ "code": "...", "state": "..." }` and returns the regular session response.
- Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `GOOGLE_REDIRECT_URI` to enable Google sign-in. Google email must be verified. OAuth state expires after ten minutes and is single use. The configured Google callback URI must exactly match the Google Cloud Console redirect URI.
- Sessions are opaque random bearer credentials stored as SHA-256 hashes in `auth_sessions`; logout revokes the presented session. This is not a JWT implementation.

### Profile

- `GET /api/profile`, `PATCH /api/profile` (`{ "name": "...", "surname": "..." }`)
- `POST /api/profile/avatar` multipart field `avatar`; accepts JPEG, PNG, or WebP, maximum 2 MB.
- `DELETE /api/profile/avatar` (204)

Avatar content is currently stored as a data URL in `users.avatar_url`. Replace this with an object-storage reference before production use at scale.

### Workspaces and members

- `GET /api/workspaces?page=0&size=20`, `GET /api/workspaces/{id}`, `POST /api/workspaces`, `PUT /api/workspaces/{id}`, `DELETE /api/workspaces/{id}`
- `GET /api/workspaces/{id}/members?page=0&size=20`
- `POST /api/workspaces/{id}/members/invitations` with `{ "email": "person@example.com", "role": "MEMBER" }`
- `POST /api/workspaces/{id}/members/{memberId}/resend-invitation`
- `POST /api/workspaces/invitations/accept` with `{ "token": "..." }`
- `PATCH /api/workspaces/{id}/members/{memberId}` with `{ "role": "EDITOR" }`; `DELETE` the same path to remove.

Roles accepted for invite/change: `ADMIN`, `EDITOR`, `MEMBER`, `VIEWER`. Only the workspace owner can manage workspace settings and members. Invited users must already have accounts. Invitation tokens expire after seven days.

### Notifications

- `GET /api/notifications?page=0&size=20&unread=true`
- `GET /api/notifications/unread-count` → `{ "count": 0 }`
- `PATCH /api/notifications/{id}/read`, `POST /api/notifications/read-all`
- `GET` and `PATCH /api/notifications/preferences` with optional boolean fields `inApp`, `email`, `securityAlerts`, `websiteUpdates`, `billingUpdates`.

Notification storage is available; this module does not yet create delivery events for other product domains. Email preference records intent and does not itself send notification email.

### User support

- `POST /api/support/requests` with `subject`, `category`, and `message` (201)
- `GET /api/support/requests?page=0&size=20`
- `GET /api/support/requests/{id}`

Ticket lists/details are restricted to the authenticated requester. Admin ticket operations remain outside this API.

### Database update

`src/main/resources/db/migration/V1__backend1_account_modules.sql` contains additive PostgreSQL DDL for the new tables and account columns. The existing project uses Hibernate `ddl-auto: update`; the SQL file is provided for controlled/manual deployment and is not automatically executed by Flyway. Review/apply it through the project's database release process before deploying the code.

