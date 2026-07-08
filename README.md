# AgriTrack Backend

Spring Boot 4 / Java 17 backend for the AgriTrack tractor-rental platform.

## Getting started

1. Copy `.env.example` to `.env` and fill in real values (database, JWT
   secret, Cloudinary, Google, Firebase). `.env` is gitignored and is loaded
   automatically at startup (via `spring-dotenv`) for local development.
   In staging/production, set these as real environment variables instead
   of shipping a `.env` file.
2. `./mvnw spring-boot:run`

On first boot, Flyway applies any pending migrations under
`src/main/resources/db/migration`. The pre-existing schema was created
manually (`spring.jpa.hibernate.ddl-auto=none`); Flyway is baselined at
version 0 so it only manages tables added from this point forward
(sessions, login history, OTP verification, etc.) and never touches the
original tables.

## Configuration

All secrets are read from environment variables via `${VAR:default}`
placeholders in `src/main/resources/application.properties`. The fallback
values after the `:` are for local development convenience only — always
set real environment variables in any shared/staging/production
environment. See `.env.example` for the full list:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT: `JWT_SECRET`
- Cloudinary: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- Google Sign-In: `GOOGLE_CLIENT_ID`
- Firebase: `FIREBASE_CREDENTIALS_PATH`
- Email (SMTP): `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM_ADDRESS`
- SMS: `SMS_PROVIDER`, `SMS_API_KEY`, `SMS_API_SECRET`, `SMS_SENDER_ID`
- OTP: `DEV_OTP_MODE` — when `true`, forgot-password/MPIN OTPs are returned
  in the API response instead of being sent by email/SMS (no provider is
  wired in yet — see `AgriTrackBackend.OTP.OtpSender`). Set to `false` once
  a real sender is implemented.

## Authentication

- Login (`/api/v1/user/login`, `/api/v1/mpin/login`, `/api/v1/google/login`)
  returns a short-lived access token (15 min) plus a refresh token.
- `POST /api/v1/auth/refresh` rotates the refresh token and issues a new
  access token — the client should call this automatically on a 401.
- `POST /api/v1/auth/logout` / `/api/v1/auth/logout-all`, `GET
  /api/v1/auth/sessions`, `DELETE /api/v1/auth/session/{id}`, `GET
  /api/v1/auth/login-history` manage per-device sessions.
- Forgot password/MPIN: `POST /api/v1/user/forgot-password` →
  `/api/v1/user/verify-otp` → `/api/v1/user/reset-password` (mirrored under
  `/api/v1/mpin/forgot/*` for MPIN).

## Authorization

Every authenticated endpoint enforces ownership: a caller can only
read/modify/delete resources they own, derived from the JWT (`CurrentUser`
utility) rather than any id supplied by the client. See
`AgriTrackBackend.SECURITY.CurrentUser`.

## Tests

`./mvnw test`
