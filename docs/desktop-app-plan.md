# Kheyr Desktop App — Implementation Plan

> Status: planning. Target: **Phase 1 full desktop client** per [prd.md](../prd.md) §5.7, §7.3, §11.4.
> Stack decision: **.NET 10 + Avalonia UI**, in this repo under `desktop/`.

## 1. Goal & scope

A native cross-platform (Windows / macOS / Linux) desktop SMS client that:

1. Pairs with the Android phone via QR (PRD §11.4.2).
2. Receives the account's sync content key securely during pairing (PRD §8.2).
3. Mirrors conversations by downloading synced ciphertext and **decrypting locally** (PRD §8.1.4).
4. Shows All Messages / Spam / Archived / Pinned, pinned sorted by pin date, search, conversation view, emoji-only rendering (PRD §11.4.3).
5. Sends SMS **through the phone** via the relay, with waiting-for-phone / status / retry (PRD §11.4.4).
6. Provides settings: sync status, device info, logout, notifications, local data delete, theme (PRD §11.4.5).

The backend stays **zero-knowledge**: it only ever stores ciphertext and public-key-wrapped key blobs.

## 2. What already exists (do not rebuild)

The backend ([backend/](../backend/)) already implements the transport. Verified contract:

**REST `/api/v1`** (`Kheyr.Api/Controllers/ApiControllers.cs`):

| Area | Endpoints |
| --- | --- |
| auth | `POST otp/request`, `otp/verify`, `refresh`, `logout` |
| devices | `POST /devices` (register w/ `public_key`), `GET /devices` |
| spam-rules | `GET spam-rules/latest` |
| spam-feedback | `POST spam-feedback` |
| sync | `POST sync/initial`, `POST sync/upload`, `GET sync/updates?cursor=` |
| pairing | `POST pairing/session`, `POST pairing/approve`, `POST pairing/revoke` |
| desktop relay | `POST desktop/sms/send`, `POST desktop/sms/status` |
| direct (P2) | `POST direct/messages` |
| privacy | `POST privacy/delete`, `GET privacy/export` |
| account | `DELETE account` |
| health | `GET health` |

**SignalR `/hubs/kheyr`** (`Kheyr.Infrastructure/Hubs/KheyrHub.cs`) — groups `user:{id}`, `device:{id}`, `pairing:{id}`; client method `JoinPairingSession(sessionId)`; server→client events:

- `PairingApproved` — carries auth **tokens** to the `pairing:{id}` group on approval.
- `DesktopSmsRequest` — relay send dispatched to the phone's `device:{androidId}`.
- `DesktopSmsStatus` — send result back to the desktop's `device:{id}`.
- `DeviceRevoked`, `DirectMessage`.

So pairing + auth handoff + sync + relay transport + presence groups **already exist**. The desktop is mostly a *client* of this contract plus the crypto.

## 3. Crypto & key model (the part that needs wiring)

Per PRD §8.2 and the Android sync layer:

- **Content key**: a random AES-256 key generated on Android, now stored in `EncryptedSharedPreferences` and **exportable** via `SyncEncryptionKeyStore.keyMaterial()` (see [SyncEncryptionKeyStore.kt](../app/src/main/java/com/kheyr/sms/sync/SyncEncryptionKeyStore.kt)).
- **Body envelope**: `SmsBodyEncryptor` produces AES-256-GCM and the wire form is now self-describing — `algorithm.nonceBase64.ciphertextBase64` (see `EncryptedSmsBody.wireFormat()` / [SmsBodyEncryptor.kt](../app/src/main/java/com/kheyr/sms/sync/crypto/SmsBodyEncryptor.kt)). The backend stores this blob opaquely.

### Pairing key handoff (to implement)

1. Desktop generates an asymmetric keypair; private key → OS secure store (DPAPI / Keychain / libsecret).
2. `POST /pairing/session { public_key }` → `session_id` + `qr_payload`; desktop calls `JoinPairingSession(session_id)` and shows the QR (the QR already carries `desktopPublicKeyBase64`, modeled in [DesktopPairing.kt](../app/src/main/java/com/kheyr/sms/sync/DesktopPairing.kt)).
3. Phone scans → `POST /pairing/approve`. **New on Android:** wrap the content key with the desktop's public key (RSA-OAEP-SHA256) and send `wrapped_content_key` in the approve body.
4. Backend includes `wrapped_content_key` in the `PairingApproved` SignalR payload (relays the opaque blob — stays zero-knowledge).
5. Desktop unwraps with its private key → stores the content key in the OS secure store.

### Interop notes (.NET ↔ Android), to capture as tests

- **GCM tag placement**: Java `Cipher` appends the 16-byte GCM tag to the ciphertext. .NET `AesGcm.Decrypt` wants tag separate → desktop splits `ciphertext = body[..^16]`, `tag = body[^16..]`.
- **Key wrap padding**: Android `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` ⇄ .NET `RSAEncryptionPadding.OaepSHA256`.
- Base64 is standard (not URL-safe); `.` is a safe envelope delimiter (not in the base64 alphabet).
- Decide RSA-2048 (simplest, MVP) vs X25519+HKDF/ECIES (modern, hardening) — RSA-OAEP recommended for P1.

## 4. Gaps to close outside the desktop app

These are required for the desktop to actually work; track as companion tasks.

**Android** ([app/](../app/)):

1. Wrap + send the content key on `pairing/approve` (uses `SyncEncryptionKeyStore.keyMaterial()` + desktop public key).
2. Add `SmsBodyEncryptor.decryptBody(...)` (today it only encrypts) — needed to decrypt relay send requests.
3. Add a **SignalR client**: handle `DesktopSmsRequest` (decrypt target+body with content key → send via `SmsManager` → `POST /desktop/sms/status`), maintain presence. Today [DesktopRelayRequestHandler.kt](../app/src/main/java/com/kheyr/sms/desktop/DesktopRelayRequestHandler.kt) is a pure model with no transport.
4. Upload thread display data the desktop needs: `encrypted_phone_number` + `encrypted_contact_name` (encrypted with the content key) and `phone_number_hash`, on initial sync and `thread_updated`. Currently Android sends none of these, so the desktop would have no name/number to show. (`PhoneIdentifierHasher` exists for the hash; the encrypted fields need wiring.)

**Backend** ([backend/](../backend/)):

5. Thread `wrapped_content_key` through `pairing/approve` into the `PairingApproved` payload (or expose `GET /devices/me/key`). Backend treats it as an opaque blob.
6. (If not already) ensure `sync/updates` emits thread state + the encrypted thread fields so the desktop can render folders.

## 5. Project structure (in this repo)

```
desktop/
  Kheyr.Desktop.slnx
  src/
    Kheyr.Client/          # .NET lib: API client, SignalR client, crypto, sync engine, local store
    Kheyr.Desktop/         # Avalonia .NET 10 app (Views, ViewModels, DI)
  tests/
    Kheyr.Client.Tests/    # crypto interop + sync-apply unit tests
```

- Add a shared **`Kheyr.Contracts`** project (wire DTOs) referenced by both `backend/` and `desktop/` to keep the JSON contract in lockstep, or keep desktop DTOs independent and pin them with interop tests. (Recommend shared contracts.)
- **Local store**: SQLite (e.g., `Microsoft.Data.Sqlite`), optionally SQLCipher-encrypted, mirroring threads/messages decrypted-at-rest-or-on-read. The content key + DB key live in OS secure storage.
- **Realtime**: `Microsoft.AspNetCore.SignalR.Client`.
- **Crypto**: `System.Security.Cryptography` (`AesGcm`, `RSA`).
- **MVVM**: CommunityToolkit.Mvvm; theme light/dark to mirror Android.

## 6. Milestones (full client)

### M0 — Foundation
- Avalonia .NET 10 app skeleton; builds on Windows + CI matrix for macOS/Linux.
- `Kheyr.Client` lib; `Kheyr.Contracts`; DI; settings store; OS secure-storage abstraction.
- Crypto module + **interop tests**: encrypt on Android ↔ decrypt in .NET; RSA wrap/unwrap round-trip.

### M1 — Pairing & key exchange
- Desktop keypair + QR screen; `pairing/session`; `JoinPairingSession`; await `PairingApproved`.
- Android: wrap + send content key on approve. Backend: passthrough `wrapped_content_key`.
- Store tokens + content key securely; show paired account; logout. Auto-logout on `DeviceRevoked`.
- _Acceptance_: PRD §11.4.2 (cannot access before pairing; expires; revocable; auto-logout).

### M2 — Read-only mirror
- Initial + incremental sync (`sync/updates?cursor=`); apply deltas to local store; live `message_created`.
- Decrypt bodies (envelope) + thread number/name (once Android uploads them).
- Folders: All / Spam / Archived / Pinned (pinned by pin date), thread search, conversation view, emoji-only rendering.
- _Acceptance_: PRD §11.4.3 (order matches Android; spam visible; deletes disappear; bodies decrypt locally).

### M3 — Send via relay
- Compose/reply; SIM selector; encrypt target+body with content key → `desktop/sms/send`.
- Status via `DesktopSmsStatus`: sending / waiting-for-phone / sent / failed; retry.
- Android side: SignalR handler decrypts + sends + reports status (gap #2/#3 above).
- _Acceptance_: PRD §11.4.4 (sent by phone not desktop; accurate status; SIM respected; retry).

### M4 — Settings, presence, polish
- Settings: sync status/connection, device info, logout, notification prefs, local data delete, theme.
- Presence (phone online/offline) surfaced; reconnect/backoff; local notifications.
- _Acceptance_: PRD §11.4.5 + §13.4 reliability items (auto-reconnect; offline indicator; consistent sync state).

## 7. Security model

- Backend remains zero-knowledge: stores only ciphertext + public-key-wrapped key blobs; never the content key or plaintext.
- Content key wrapped per device with that device's public key; desktop private key in OS secure storage.
- **Revocation caveat**: a revoked desktop still holds the content key it already received. `pairing/revoke` cuts its tokens (refresh tokens are revoked server-side), but true forward secrecy needs **content-key rotation on revoke** (re-encrypt going forward). Decide for P1 vs P3 (PRD §13.3.3 "Rotate encryption keys, if feasible").
- Harden the QR channel with a short verification fingerprint (SAS) shown on phone + desktop to defeat public-key substitution (post-MVP).
- Local data delete must purge the SQLite store, the content key, and tokens from secure storage.

## 8. Key risks (PRD §17 + crypto)

| Risk | Mitigation |
| --- | --- |
| Crypto interop drift (Java GCM tag, RSA-OAEP) | Lock with interop tests in M0; fixed envelope format. |
| Desktop send depends on phone online | Waiting-for-phone state, relay queue + expiry (already modeled), clear status. |
| Key sharing complexity | RSA-OAEP for MVP; X25519 later; design reviewed in M1. |
| Revoked device retains key | Document; add content-key rotation as hardening. |
| Cross-platform secure storage variance | Abstract per-OS (DPAPI/Keychain/libsecret) behind one interface; test each. |

## 9. Open decisions for the team

1. Key-wrap algorithm: RSA-OAEP (recommended, simple) vs X25519/ECIES.
2. Content-key rotation on revoke in P1, or accept the caveat and defer to P3.
3. Encrypt thread contact name/number (recoverable) **and** hash, or hash-only (would limit desktop display).
4. Local store encryption: SQLCipher vs plaintext SQLite guarded by OS file perms + secure-stored content.
