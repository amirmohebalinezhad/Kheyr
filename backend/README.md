# Kheyr Backend & Admin

ASP.NET Core 10 backend for the Kheyr SMS client (Android + desktop). Implements all PRD Phase 1 API endpoints, SignalR realtime hub, and a Blazor Server admin panel.

## Projects

| Project | Description |
|---------|-------------|
| `Kheyr.Api` | REST API + SignalR (`/hubs/kheyr`) |
| `Kheyr.Admin` | Blazor Server admin panel |
| `Kheyr.Domain` | Entities and options |
| `Kheyr.Infrastructure` | EF Core, services, hubs |

## Quick start (local)

Requires [.NET 10 SDK](https://dotnet.microsoft.com/download).

```bash
cd backend
dotnet run --project src/Kheyr.Api
# API: http://localhost:5000 (or port in launchSettings)
dotnet run --project src/Kheyr.Admin
# Admin: http://localhost:5001
```

Default SQLite database: `kheyr.db` in the working directory.

### Admin login (dev)

- Username: `admin`
- Password: `admin`

### OTP (dev)

- Fixed code: `123456` (see `Otp:FixedCode` in `appsettings.json`)
- Request: `POST /api/v1/auth/otp/request` with `{"phone":"+15551234567"}`
- Verify: `POST /api/v1/auth/otp/verify` with `{"phone":"+15551234567","code":"123456"}`

### Android app configuration

Set `API_BASE_URL` in `app/build.gradle` to your API base URL (e.g. `http://10.0.2.2:5000` for emulator).

## API endpoints

All routes match the Android `KheyrApiService` client:

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/v1/spam-rules/latest` | No |
| POST | `/api/v1/spam-feedback` | Bearer |
| POST | `/api/v1/auth/otp/request` | No |
| POST | `/api/v1/auth/otp/verify` | No |
| POST | `/api/v1/auth/refresh` | No |
| POST | `/api/v1/auth/logout` | Bearer |
| POST | `/api/v1/devices` | Bearer |
| POST | `/api/v1/sync/initial` | Bearer |
| POST | `/api/v1/sync/upload` | Bearer |
| GET | `/api/v1/sync/updates?cursor=` | Bearer |
| POST | `/api/v1/pairing/session` | No |
| POST | `/api/v1/pairing/approve` | Bearer |
| POST | `/api/v1/pairing/revoke` | Bearer |
| POST | `/api/v1/desktop/sms/send` | Bearer |
| POST | `/api/v1/desktop/sms/status` | Bearer |
| POST | `/api/v1/direct/messages` | Bearer |
| POST | `/api/v1/privacy/delete` | Bearer |
| GET | `/api/v1/privacy/export` | Bearer |
| DELETE | `/api/v1/account` | Bearer |
| GET | `/api/v1/health` | No |

SignalR hub: `/hubs/kheyr?access_token=<jwt>`

Events: `PairingApproved`, `DeviceRevoked`, `DesktopSmsRequest`, `DesktopSmsStatus`, `DirectMessage`

## Admin panel

- **Dashboard** — active users, devices, sync adoption, relay failures
- **Spam Rules** — publish, validate, rollback global rule versions
- **Feedback** — Mark Spam / Mark Not Spam metrics
- **Devices** — paired Android and desktop devices
- **SMS Relay** — desktop send request status breakdown

## Docker (PostgreSQL)

```bash
cd backend
docker compose up --build
```

- API: http://localhost:8080
- Admin: http://localhost:8081

## Security model

- SMS bodies are stored as **client-encrypted ciphertext** only; the server never decrypts message content.
- Phone numbers are hashed for lookups; optional encrypted phone fields for sync metadata.
- JWT access tokens (60 min) + refresh tokens (30 days).
- Desktop pairing is time-limited; revoked devices lose API and SignalR access immediately.

## PostgreSQL configuration

Set in `appsettings.json` or environment:

```json
{
  "ConnectionStrings": { "Default": "Host=localhost;Database=kheyr;Username=kheyr;Password=kheyr" },
  "Database": { "UseSqlite": false }
}
```
