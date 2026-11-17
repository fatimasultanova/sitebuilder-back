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
| Variable                | Default                                      | Notes                                              |
|-------------------------|----------------------------------------------|----------------------------------------------------|
| `MAIL_PROVIDER`         | `brevo`                                      | Only `brevo` is implemented.                       |
| `BREVO_API_KEY`         | empty                                        | Required only when email delivery is enabled.      |
| `BREVO_BASE_URL`        | `https://api.brevo.com`                      |                                                    |
| `BREVO_CONNECT_TIMEOUT` | `3s`                                         |                                                    |
| `BREVO_READ_TIMEOUT`    | `8s`                                         |                                                    |
| `BREVO_MAX_RETRIES`     | `2`                                          | Connection failures/connect timeouts and 429 only. |
| `AUTH_EMAIL_ENABLED`    | `false`                                      | `true` enables real Brevo delivery.                |
| `AUTH_EMAIL_FROM`       | `Site Builder <no-reply@sitebuilder.local>`  | `Name <email>` or plain email.                     |
| `FRONTEND_BASE_URL`     | `http://localhost:3000`                      | No trailing slash, no path.                        |
| `AUTH_EMAIL_LOCALE`     | `az`                                         | `az` or `en`.                                      |
| `AUTH_RESEND_COOLDOWN`  | `60s`                                        | Minimum time between verification resends.         |

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

All routes below use the `/api` prefix and require
`Authorization: Bearer <session-token>` except public authentication flows.

Hibernate runs with `ddl-auto=validate`, so required database objects must exist
before startup. List endpoints use zero-based `page` and `size` parameters
(default 20, maximum 100).

Workspace, workspace-member and support-ticket lists return:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Authentication

- `POST /api/auth/register` (201), `/login` (200), `/logout` (204)
- `POST /api/auth/forgot-password`, `/reset-password`, `/resend-verification`, `/verify-email`
- `POST /api/auth/oauth/google/start`, `/callback`, `/link`
- Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `GOOGLE_REDIRECT_URI` to enable Google OAuth.
- Password login requires a verified email address.
- Sessions are opaque random bearer credentials stored as hashes in `auth_sessions`; this is not JWT.

### Profile

- `GET /api/profile`, `PATCH /api/profile`
- `POST /api/profile/avatar` multipart field `avatar`
- `DELETE /api/profile/avatar` (204)

Avatar accepts JPEG, PNG, or WebP. Maximum file size is 5 MB and maximum multipart
request size is 6 MB. Oversized uploads return 413 and a missing `avatar` part
returns 422.

Avatar content is currently stored as a data URL in `users.avatar_url`.

### Workspaces and members

- `GET /api/workspaces?page=0&size=20`
- `GET /api/workspaces/{id}`
- `POST /api/workspaces`
- `PUT /api/workspaces/{id}`
- `DELETE /api/workspaces/{id}`
- `GET /api/workspaces/{id}/members?page=0&size=20`
- `GET /api/workspaces/{id}/members/{memberId}`
- `POST /api/workspaces/{id}/members`
- `POST /api/workspaces/{id}/members/{memberId}/invite/resend`
- `POST /api/workspaces/invitations/accept`
- `PATCH /api/workspaces/{id}/members/{memberId}`
- `DELETE /api/workspaces/{id}/members/{memberId}`

Supported member roles are `admin`, `editor`, and `viewer`. Role input is
case-insensitive. The default role is `viewer`.

Invited users must already have accounts. Invitation tokens expire after seven
days. Accepting an invitation records `joinedAt`.

Only the workspace owner can manage workspace settings and members.

### Notifications

- `GET /api/notifications?page=0&size=20&unread=false`
- `GET /api/notifications/unread-count`
- `PATCH /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `GET` and `PATCH /api/notifications/preferences`

Notifications keep a separate pagination format:

```json
{
  "notifications": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Notification creation uses safe fallbacks for invalid category/type/title/message
values, and missing recipients are ignored. If in-app notifications are disabled,
the notification is not stored.

### User support

- `POST /api/support/requests` with `subject`, `category`, and `message` (201)
- `GET /api/support/requests?page=0&size=20`
- `GET /api/support/requests/{id}`

Supported categories: `account`, `website`, `billing`, `technical`, `other`.

Ticket lists/details are restricted to the authenticated requester.

### Database update

`src/main/resources/db/migration/V1__backend1_account_modules.sql` contains
additive PostgreSQL DDL for Backend 1.

The project uses Hibernate `ddl-auto=validate`; the migration is not automatically
executed by Flyway and must be applied through the database release process.

### Verification

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Manual smoke tests:

```text
GET /api/workspaces
GET /api/support/requests
GET /api/notifications
```

All three should return `200 OK` for an authenticated user.
