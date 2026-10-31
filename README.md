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
