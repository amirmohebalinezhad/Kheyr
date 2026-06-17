# Product Requirements Document

## Product: Modern SMS App with Spam Filtering, Sync, Desktop SMS, and Direct Messages

## Implementation Progress

Last updated: 2026-06-17

### Android Phase 0 Started

- [x] Created Kotlin/Jetpack Compose Android app scaffold.
- [x] Added default SMS role request flow and SMS/contact/notification permission request flow.
- [x] Added default SMS manifest hooks for SMS delivery, SENDTO compose intents, and respond-via-message service.
- [x] Added local SMS thread loading prototype from Android `Telephony.Sms`.
- [x] Added dual-SIM discovery model and active SIM repository prototype.
- [x] Added multipart SMS sending helper with optional subscription/SIM selection.
- [x] Added local spam scoring engine with number-prefix, keyword, URL, sender/contact, regex, OTP negative-score, and short-code rule support.
- [x] Added inbox thread sorting for pinned-first behavior, pin-date ordering, and spam/archive hiding.
- [x] Added notification settings models for content privacy, unknown senders, ringtone, vibration, and per-thread mute/ringtone overrides.
- [x] Added GitHub Actions debug APK build artifact for every push, pull request, and manual workflow run.
- [x] Persisted SMS threads and messages locally with Room entities, DAO inserts, and thread summaries.
- [x] Added Room-backed conversation message loading sorted chronologically.
- [x] Added Room-backed message search across SMS body and sender address.
- [x] Added Room-backed unread clearing for incoming messages in a thread.
- [x] Added Room-backed failed outgoing message lookup for retry surfaces.
- [x] Added Room-backed per-thread mute updates for notification overrides.
- [x] Added Room-backed spam metadata persistence and lookup for sync/classification audits.
- [x] Suppressed notifications for classified spam in the receiver path.
- [x] Implemented client-side AES-GCM SMS body encryption for sync upload payloads.
- [x] Added sync uploader queue handling for initial backfill, changes, deletes, spam, pin, archive, and notification settings events.
- [x] Added emoji-only conversation presentation helper for bubble-free message rendering.
- [x] Added notification policy resolver that suppresses spam and blocked senders before notification rendering.
- [x] Added notification policy support for disabling notifications from unknown senders.
- [x] Added notification policy support for silent unknown-sender notifications.
- [x] Added notification privacy handling for sender-only and hidden-content modes.
- [x] Added notification ringtone precedence for thread-specific ringtones over global ringtones.
- [x] Added notification vibration suppression when a thread or unknown sender is silent.
- [x] Added desktop pairing data models for QR pairing payloads and paired device revocation.
- [x] Added desktop platform model covering Windows, macOS, and Linux.
- [x] Added desktop SMS send gate that forwards valid requests only when the Android phone is online.
- [x] Added desktop send validation for revoked devices, missing recipients, and empty message bodies.
- [x] Added anonymous spam feedback payloads for user Mark Spam and Mark Not Spam corrections.
- [x] Added spam rule update validation for newer server-managed global rule payloads.
- [x] Added salted phone identifier hashing for minimized sync metadata.
- [x] Added SIM routing policy for per-thread defaults, global defaults, and user selection fallback.
- [x] Added main drawer model with PRD-required navigation item order.
- [x] Added thread row presentation mapping for unread, pinned, muted, SIM, and spam badges.
- [x] Added retry policy for failed outgoing SMS send attempts.
- [x] Added initial sync backfill planner that skips history deleted before sync opt-in.
- [x] Added conversation header model for contact title, SIM indicator, call, info, and search actions.
- [x] Added advanced local search filter for sender, content, and timestamp ranges.
- [x] Added battery restriction warning model for background sync reliability.
- [x] Added core analytics metric names for activation, sync, desktop pairing, SMS relay, and spam feedback.
- [x] Added cloud data deletion request model for privacy tooling.
- [x] Added local backup plan model for Phase 3 import and export backups.
- [x] Added picture message eligibility guard that only allows pictures in Direct Message mode.
- [x] Added direct message badge labels that clearly distinguish SMS from Direct Messages.
- [x] Added direct message mode resolver that falls back to SMS when recipients are not registered.
- [x] Added desktop failed-send retry model for SMS relay requests.
- [x] Added desktop folder model for All Messages, Spam, Archived, and Pinned views.
- [x] Added desktop thread ordering helper that mirrors Android pinned-first sorting.
- [x] Added theme preference resolver for system, light, and dark appearance choices.
- [x] Added local data delete plan for desktop logout and privacy settings.
- [x] Added desktop device settings state for viewing and revoking paired desktop devices.
- [x] Added spam rule version display helper for settings and spam details.
- [x] Added settings category order matching Notifications through About from the PRD.
- [x] Added desktop waiting-for-phone state model for offline Android relay requests.
- [x] Added pairing expiry policy for time-limited QR pairing sessions.
- [x] Added device revocation result model for Android-managed desktop device removal.
- [x] Added encrypted field policy describing which SMS fields must be encrypted or hashed before upload.
- [x] Added thread state sync payload for spam, pin, archive, and read states.
- [x] Added delete event sync model for removing messages from the server after sync opt-in.
- [x] Added initial sync progress model for visible upload status after sync is enabled.
- [x] Added sync enablement gate requiring explicit user opt-in before upload.
- [x] Added vibration decision model that suppresses vibration for muted and silent conversations.
- [x] Added ringtone override resolver that prioritizes thread ringtones over global defaults.
- [x] Added unknown sender notification policy for normal, silent, and suppressed alerts.
- [x] Added notification content formatter for preview, sender-only, and hidden-content privacy modes.
- [x] Added spam feedback anonymizer that strips raw SMS bodies before upload.
- [x] Added spam false-positive restore planner for Mark Not Spam actions.
- [x] Added spam folder unread counter that counts unread spam threads only.
- [x] Added spam reason formatter for user-visible spam details.
- [x] Added spam classification thresholds for normal, suspicious, and spam scores.
- [x] Added desktop SIM routing request model that carries optional desktop-selected SIM preferences.
- [x] Added per-thread SIM preference model for overriding the global default SIM.
- [x] Added SIM failure message mapper for clear dual-SIM send errors.
- [x] Added emoji-only style resolver that keeps timestamps and status visible without normal bubbles.
- [x] Added conversation search matcher that returns matching message ids for a query.
- [x] Added conversation message action resolver for copy, delete, spam correction, and retry availability.
- [x] Added message status timeline validator for sending, sent, delivered, and failed transitions.
- [x] Added multipart SMS planner that estimates message segment counts for long outgoing bodies.
- [x] Added outgoing SMS draft validation for recipient, body, and optional SIM selection.
- [x] Added thread deletion sync planner that emits delete events only after sync is enabled.
- [x] Added thread bulk action request model for delete, archive, spam, and read operations.
- [x] Added thread search matcher across contact names, phone numbers, and last message previews.
- [x] Added spam rules download state for onboarding and cached-rule fallback.
- [x] Added SMS import progress model for existing local message backfill.
- [x] Added default SMS role removal warning state for users who lose default app status.
- [x] Added permission explainer copy for SMS, contacts, and notifications during onboarding.
- [x] Added onboarding gate that blocks full SMS features until default SMS role and required permissions are granted.
- [x] Added phase 2 direct message availability model with SMS fallback visibility.
- [x] Added desktop conversation mock model for validating Avalonia UI direction against Android state.
- [x] Added unread thread state model for unread count and read sync timestamp.
- [x] Added pinned thread state model with pin timestamp ordering support.
- [x] Added archive state model for archive visibility and sync metadata.
- [x] Added message action model for copy, delete, spam correction, and retry actions.
- [x] Added thread action model for pin, archive, delete, spam, read, mute, and notification settings actions.
- [x] Added spam protection settings model exposing enabled state, rule version, and threshold.
- [x] Added appearance settings model for system, light, and dark theme choices.
- [x] Added unknown sender settings model for normal, silent, and suppressed notifications.
- [x] Added privacy and security settings state for notification privacy and local encryption availability.
- [x] Added clear SMS send failure reason model for user-facing errors.
- [x] Added dual-SIM selection state model for available SIMs and selected/default SIM.
- [x] Added user spam correction model for Mark Spam and Mark Not Spam actions.
- [x] Added spam rule cache state model for latest valid cached rule fallback.
- [x] Added spam decision details model with score, classification, rule version, and reasons.
- [x] Added sync cursor model for initial and incremental sync checkpoints.
- [x] Added encrypted sync record model that keeps SMS body ciphertext separate from minimized metadata.
- [x] Added desktop SMS relay status model including waiting-for-phone and retryable failure.
- [x] Added qR pairing session state model with expiry and approval states.
- [x] Added desktop device list model for paired-device management and revocation UI.
- [x] Added user-visible sync status model for disabled, pending, syncing, synced, and failed states.
- [x] Added onboarding step model for default SMS, permissions, sync, and spam rules.
- [x] Added settings category model in the PRD-required order.
- [x] Added spam folder UI model with unread count and empty-state text.
- [x] Added conversation search state model with query, matches, and selected match.
- [x] Added retryable failed-message action model for conversation rows.
- [x] Added message status presentation labels for sending, sent, delivered, and failed states.
- [x] Added conversation UI state model for header, messages, composer, and search.
- [x] Added conversation message UI model for bubble, timestamp, and status rendering.
- [x] Implement encrypted local storage at rest using SQLCipher or Android encrypted file-backed stores.
- [x] Added SMS composer state reducer for send validation, SIM selection, completion, and retryable failures.
- [x] Added sync retry policy with exponential backoff for failed upload and download.
- [x] Added realtime connection state model for WebSocket sync and desktop relay push.
- [x] Added sync update applier for downloaded encrypted message and thread deltas.
- [x] Added sync downloader for incremental server pull using cursor checkpoints.
- [x] Added sync encryption key store policy for Android Keystore-backed key management.
- [x] Added SMS import planner for batched existing-message backfill during onboarding.
- [x] Added onboarding sync opt-in model for optional account creation during setup.
- [x] Added permission denial state resolving limited-mode behavior for denied permissions.
- [x] Added welcome screen copy for default SMS requirement explainer.
- [x] Added logout plan defining credential, sync key, and queue cleanup steps.
- [x] Added account deletion request model with local wipe prerequisites.
- [x] Added device registration payload for Android device name, platform, and public key.
- [x] Added auth token state for access and refresh token expiry tracking.
- [x] Added OTP verification state for code entry, resend cooldown, and verification outcome.
- [x] Added phone login request model with E.164 normalization for OTP sign-in.
- [x] Added incoming message presentation rules for SIM badge and relative timestamps.
- [x] Added compose FAB state model for new conversation navigation.
- [x] Added thread avatar presentation helper for initials and contact-photo availability.
- [x] Added thread long-press menu model for pin, archive, delete, spam, read, and mute actions.
- [x] Added thread list UI state model for drawer folder and search query.
- [x] Refactored MainActivity conversation screen to use bubble layout and retryable send models.
- [x] Added conversation screen mapper binding messages, header, and composer state.
- [x] Added retryable send UI model for failed outgoing conversation messages.
- [x] Build full Compose conversation UI and retryable send statuses.
- [x] Wired encrypted Room database builder using SQLCipher support factory.
- [x] Added local database passphrase store backed by Android encrypted preferences.

---

## 1. Product Overview

The product is a modern Android SMS replacement app with a Telegram-like interface. It allows users to view SMS threads, send and receive SMS, classify spam using server-managed global spam rules, move spam messages to a Spam folder, suppress spam notifications, customize notification behavior per contact, sync SMS messages securely to a server, and use a native desktop app to read and send SMS through the Android phone.

The Android app must become the user’s default SMS app to access and manage SMS properly.

The MVP is SMS-only. MMS is excluded. Direct internet-based messages and picture messages are Phase 2 features.

---

# 2. Core Product Decisions

## 2.1 Confirmed Decisions

1. Android app must work as the default SMS app.
2. UI should be inspired by Telegram, but not copy Telegram branding or assets.
3. MMS is removed from scope.
4. Dual SIM support is mandatory.
5. Spam rules are global.
6. Spam scoring rules are downloaded from server.
7. Spam rules support number prefix scoring.
8. OTP messages are handled through negative spam scores.
9. No custom user spam rules.
10. No country/operator-specific spam customization.
11. Spam folder must sync to server and desktop.
12. After sync is enabled, all current local SMS messages must sync.
13. Deleted message history should not sync.
14. SMS body must be client-encrypted before upload.
15. Desktop app must be native .NET 10 with Avalonia UI.
16. Desktop must support Windows, macOS, and Linux.
17. Desktop can send SMS through the Android phone.
18. Pinned threads sort by date of pin.
19. Direct messages are Phase 2.
20. Direct messages are not encrypted in Phase 2.
21. Emoji-only messages should appear without normal message bubble.
22. User needs a setting to show or hide SMS content in notifications.

---

# 3. Product Goals

## 3.1 Main Goals

1. Build a reliable Android SMS replacement app.
2. Provide a clean Telegram-like SMS experience.
3. Reduce SMS notification noise using spam detection.
4. Give users control over contact-based notification behavior.
5. Support full dual SIM SMS sending and receiving.
6. Provide secure client-encrypted sync.
7. Provide desktop access to SMS threads.
8. Allow SMS sending from desktop through the Android phone.
9. Add direct app-to-app messages in Phase 2.

## 3.2 Non-Goals

1. MMS support.
2. RCS support.
3. End-to-end encryption for direct messages in Phase 2.
4. Custom spam rules by user.
5. Country/operator-specific spam rule customization.
6. Carrier-level spam blocking.
7. WhatsApp/Telegram social network replacement in MVP.
8. Group internet messaging in MVP or Phase 2 unless added later.

---

# 4. Target Users

## 4.1 Primary Users

Users who receive many SMS messages and want a better inbox, spam filtering, and notification control.

## 4.2 Secondary Users

Business users who want to manage SMS messages from desktop while the SIM remains inside their Android phone.

## 4.3 Power Users

Users who need custom notification behavior per contact, silent unknown senders, pinned threads, dual SIM routing, and cross-device sync.

---

# 5. High-Level User Stories

## 5.1 SMS Inbox

As a user, I want to see all SMS conversations in a modern thread list so I can manage messages easily.

Acceptance criteria:

1. App shows SMS threads ordered by latest message.
2. Pinned threads appear above normal threads.
3. Pinned threads sort by date of pin.
4. Spam threads are hidden from main inbox.
5. Archived threads are hidden from main inbox unless opened from Archived.
6. Thread list shows contact name or number, last message, time, unread count, avatar, pinned status, muted status, and SIM information where relevant.

## 5.2 Send SMS

As a user, I want to send SMS from the app using one of my SIM cards.

Acceptance criteria:

1. User can send SMS to a contact or phone number.
2. User can reply in an existing thread.
3. App supports multipart SMS.
4. App shows sending, sent, failed, and delivered status where available.
5. User can retry failed messages.
6. User can select SIM before sending.
7. User can set default SIM globally.
8. User can set default SIM per contact/thread.

## 5.3 Receive SMS

As a user, I want incoming SMS to appear in the correct conversation.

Acceptance criteria:

1. Incoming SMS is inserted into correct thread.
2. App detects receiving SIM where available.
3. Spam scoring runs before notification.
4. High-spam-score SMS goes to Spam folder.
5. Spam SMS does not show notification.
6. Non-contact notification behavior follows user settings.
7. Contact-specific notification behavior overrides global notification behavior unless message is spam.

## 5.4 Spam Protection

As a user, I want spam SMS moved away from my normal inbox automatically.

Acceptance criteria:

1. App downloads global spam rules from server.
2. App stores latest valid spam rule version locally.
3. App calculates spam score locally on device.
4. App supports positive and negative scores.
5. App supports OTP negative-score rules.
6. App supports number prefix rules.
7. High-score messages move to Spam folder.
8. Spam folder is accessible from drawer.
9. User can mark message/thread as Spam or Not Spam.
10. Spam status syncs to server and desktop when sync is enabled.

## 5.5 Notifications

As a user, I want control over SMS notification sound and privacy.

Acceptance criteria:

1. User can set global SMS ringtone.
2. User can set contact/thread-specific ringtone.
3. User can set contact/thread to silent.
4. User can disable sound for non-contacts.
5. User can choose whether SMS content appears in notifications.
6. Spam messages show no notification by default.
7. Blocked senders show no notification.

## 5.6 Sync

As a user, I want my SMS messages synced securely so I can use desktop.

Acceptance criteria:

1. Sync is off by default.
2. User must explicitly enable sync.
3. When sync is enabled, all current SMS messages sync.
4. Deleted message history does not sync.
5. Delete events sync after sync is enabled.
6. Spam folder syncs.
7. Pinned status syncs.
8. Archive status syncs.
9. Contact/thread notification settings sync.
10. SMS body is encrypted on client before upload.
11. Server must not be able to read SMS body.
12. Desktop can decrypt synced messages after secure pairing.

## 5.7 Desktop SMS

As a user, I want to read and send SMS from desktop.

Acceptance criteria:

1. Desktop app is built using .NET 10 and Avalonia UI.
2. Desktop supports Windows, macOS, and Linux.
3. Desktop pairs with Android using QR code.
4. Desktop shows SMS threads.
5. Desktop shows Spam folder.
6. Desktop shows pinned threads in same order as Android.
7. Desktop can request Android phone to send SMS.
8. Android phone must be online to send SMS from desktop.
9. Desktop shows “Waiting for phone” if Android is offline.
10. Desktop shows SMS send status.

## 5.8 Direct Messages

As a user, I want to send internet-based messages to other users of the app.

Acceptance criteria:

1. Direct messages are Phase 2.
2. Direct messages are not encrypted in Phase 2.
3. Direct messages support text.
4. Direct messages support pictures.
5. User clearly sees whether a message is SMS or Direct Message.
6. If recipient is not registered, app falls back to SMS.
7. Emoji-only direct messages appear without a normal bubble.

---

# 6. UX and Design Requirements

## 6.1 Visual Direction

The app should feel like a modern chat app similar to Telegram:

1. Thread list with avatars.
2. Left navigation drawer.
3. Floating compose button.
4. Message bubbles.
5. Dark mode and light mode.
6. Fast search.
7. Pinned conversations.
8. Smooth transitions.
9. Clean settings pages.
10. No unnecessary visual clutter.

The product must not copy Telegram’s logo, icons, exact color palette, or copyrighted assets.

## 6.2 Main Drawer

Drawer items:

1. All Messages
2. Spam
3. Archived
4. Pinned
5. Contacts
6. Desktop Sync
7. Settings
8. Help & Feedback

## 6.3 Thread List

Each thread row should show:

1. Avatar
2. Contact name or phone number
3. Last message preview
4. Last message time
5. Unread count
6. Pin icon if pinned
7. Silent/muted icon if muted
8. SIM badge if useful
9. Spam badge only inside Spam folder

## 6.4 Conversation Screen

Conversation screen should include:

1. Contact name or number
2. SIM indicator
3. Call button
4. Info/settings button
5. Message history
6. SMS composer
7. Send button
8. SIM selector
9. Message status
10. Search inside conversation

## 6.5 Emoji-Only Message UI

For messages containing only emoji:

1. Show emoji larger than normal text.
2. Do not show normal bubble.
3. Keep timestamp visible.
4. Keep delivery/sending status visible for outgoing messages.
5. Works for SMS and Phase 2 direct messages.

---

# 7. Technical Architecture

## 7.1 Android App

Recommended stack:

1. Kotlin
2. Jetpack Compose
3. Room database
4. WorkManager
5. Hilt
6. Retrofit or Ktor client
7. WebSocket or SignalR client
8. Android Keystore
9. Encrypted local storage where appropriate

## 7.2 Backend

Recommended stack:

1. NestJS or ASP.NET Core
2. PostgreSQL
3. Redis
4. Object storage for Phase 2 pictures
5. WebSocket or SignalR gateway
6. Background workers
7. Admin panel

## 7.3 Desktop

Required stack:

1. .NET 10
2. Avalonia UI
3. Multi-platform: Windows, macOS, Linux
4. Local encrypted storage
5. WebSocket or SignalR
6. QR pairing
7. Secure device key storage

---

# 8. Data Security Model

## 8.1 Client-Encrypted SMS Storage

SMS body must be encrypted before upload.

Requirements:

1. Android encrypts SMS body locally before sync.
2. Server stores ciphertext only.
3. Server cannot read SMS body.
4. Desktop receives encrypted SMS body and decrypts locally.
5. Device keys must be generated and stored securely.
6. Pairing must securely share or derive decryption keys.
7. Message metadata should be minimized.

## 8.2 Suggested Encryption Approach

1. Each user account has a sync encryption key generated on Android.
2. SMS body is encrypted using this key before upload.
3. Desktop receives the key during QR pairing through a secure device-to-device or encrypted server-assisted exchange.
4. Server stores encrypted payloads, sync cursors, and limited metadata.
5. If user revokes a desktop device, future sync access is blocked.
6. If user deletes cloud data, encrypted messages and sync records are removed.

## 8.3 Metadata

Server may need some metadata for sync:

1. User ID
2. Thread ID
3. Message ID
4. Timestamp
5. Direction
6. Message type
7. Spam status
8. Pin/archive/delete status
9. Encrypted sender/receiver fields or hashed phone identifiers

Where possible, phone numbers should be stored as encrypted values or salted hashes.

---

# 9. Spam Scoring System

## 9.1 Spam Scoring Location

Spam scoring happens on Android device.

The server only provides rule definitions. The server should not need to read SMS content.

## 9.2 Global Rules

Rules are global for all users.

No country-specific customization.

No operator-specific customization.

No user-created custom rules.

## 9.3 Rule Types

Supported rule types:

1. Sender exact match
2. Sender contains text
3. Number prefix
4. Message keyword
5. Message regex
6. URL detected
7. Suspicious link pattern
8. Sender not in contacts
9. Message frequency
10. Known OTP pattern with negative score
11. Known safe sender with negative score
12. Short code rules

## 9.4 Example Rule Payload

```json
{
  "version": 15,
  "threshold": 70,
  "rules": [
    {
      "id": "rule_001",
      "type": "number_prefix",
      "pattern": "+98999",
      "score": 35,
      "enabled": true
    },
    {
      "id": "rule_002",
      "type": "message_keyword",
      "pattern": "winner",
      "score": 30,
      "enabled": true
    },
    {
      "id": "rule_003",
      "type": "url_detected",
      "score": 40,
      "enabled": true
    },
    {
      "id": "rule_004",
      "type": "otp_regex",
      "pattern": "\\b\\d{4,8}\\b",
      "score": -40,
      "enabled": true
    }
  ]
}
```

## 9.5 Spam Score Threshold

Default classification:

1. Score below 40: Normal
2. Score 40 to 69: Suspicious
3. Score 70 or higher: Spam

Behavior:

1. Normal messages appear in inbox.
2. Suspicious messages appear in inbox but may have subtle warning in details.
3. Spam messages move to Spam folder.
4. Spam messages do not trigger notification.
5. User can mark Spam or Not Spam.
6. User feedback is sent to server as anonymous rule feedback where possible.

---

# 10. Phase Plan

---

# Phase 0: Foundation, Compliance, and Prototype

## 10.1 Phase Goal

Validate Android default SMS app requirements, build technical prototype, define encryption model, and finalize architecture before full development.

---

## 10.2 Android App Scope

### Features

1. Default SMS app role prototype.
2. SMS permission flow prototype.
3. Read SMS threads from device.
4. Receive test SMS.
5. Send test SMS.
6. Detect dual SIM availability.
7. Test SIM selection for sending.
8. Prototype local spam scoring.
9. Prototype notification suppression.
10. Prototype local encrypted database.

### Acceptance Criteria

1. App can become default SMS app.
2. App can read existing SMS.
3. App can send SMS.
4. App can receive SMS.
5. App can identify or select SIM on dual SIM devices.
6. App can suppress notification for a locally classified spam message.
7. App can store SMS locally.

---

## 10.3 Backend Scope

### Features

1. Basic project setup.
2. Auth architecture decision.
3. Spam rule API prototype.
4. Device registration prototype.
5. Sync architecture design.
6. Encryption architecture design.
7. QR pairing design.
8. Admin panel wireframe for spam rules.

### Acceptance Criteria

1. Backend can return a versioned spam rule payload.
2. Backend can register a test device.
3. Backend has documented encryption approach.
4. Backend has documented sync cursor design.
5. Backend has documented QR pairing flow.

---

## 10.4 Desktop Scope

### Features

1. .NET 10 Avalonia project setup.
2. Multi-platform build test.
3. Basic login/pairing screen prototype.
4. Thread list mock UI.
5. Conversation mock UI.
6. WebSocket/SignalR connectivity prototype.

### Acceptance Criteria

1. Desktop app builds on Windows.
2. Desktop app project structure supports macOS and Linux.
3. Desktop app can connect to test backend.
4. Desktop UI direction is validated.

---

## 10.5 Phase 0 Exit Criteria

Phase 0 is complete when:

1. Android default SMS flow is proven.
2. Dual SIM feasibility is proven.
3. Server spam rules prototype works.
4. Sync encryption design is approved.
5. Desktop Avalonia direction is confirmed.
6. Main risks are documented.

---

# Phase 1: MVP — SMS App, Spam Filtering, Sync, and Desktop SMS

## 11.1 Phase Goal

Launch a complete SMS replacement app with spam filtering, client-encrypted sync, and native desktop SMS access.

---

# 11.2 Phase 1 Android App Requirements

## 11.2.1 Onboarding

Features:

1. Welcome screen.
2. Explain default SMS app requirement.
3. Request default SMS role.
4. Request SMS permissions.
5. Request contacts permission.
6. Request notification permission.
7. Optional account creation/sign-in for sync.
8. Import existing SMS.
9. Download spam rules.
10. Open main inbox.

Acceptance criteria:

1. User cannot use full SMS features unless app is default SMS app.
2. App clearly explains why SMS access is needed.
3. App gracefully handles denied permissions.
4. App warns user if default SMS role is removed later.

---

## 11.2.2 SMS Thread List

Features:

1. Show SMS threads.
2. Show unread count.
3. Show pinned threads at top.
4. Sort pinned threads by pin date.
5. Sort normal threads by last message date.
6. Hide spam from main inbox.
7. Hide archived threads from main inbox.
8. Search threads.
9. Long-press actions.

Thread actions:

1. Pin
2. Unpin
3. Archive
4. Delete
5. Mark as spam
6. Mark as read
7. Mute
8. Open notification settings

Acceptance criteria:

1. Pinned threads always show above normal threads.
2. Latest pinned thread appears first among pinned threads.
3. Spam threads are not shown in All Messages.
4. Deleted threads are removed locally and deletion syncs if sync is enabled.

---

## 11.2.3 Conversation Screen

Features:

1. Show SMS conversation.
2. Send SMS.
3. Receive SMS.
4. Multipart SMS support.
5. Message status.
6. Retry failed message.
7. Copy message.
8. Delete message.
9. Mark message as spam.
10. Mark message as not spam.
11. Emoji-only display without bubble.
12. SIM selector.
13. Conversation search.

Acceptance criteria:

1. Outgoing SMS is shown immediately as sending.
2. Failed SMS shows retry action.
3. User can select SIM before sending.
4. Emoji-only messages show without normal bubble.
5. No MMS UI or MMS sending feature appears.

---

## 11.2.4 Dual SIM

Features:

1. Detect available SIM cards.
2. Show SIM label for incoming messages where available.
3. Allow SIM selection for outgoing SMS.
4. Global default SIM setting.
5. Per-thread default SIM setting.
6. Desktop SMS requests use selected/default SIM.

Acceptance criteria:

1. User can send SMS from SIM 1 or SIM 2.
2. User can set default SIM globally.
3. User can override SIM per thread.
4. If selected SIM fails, app shows clear error.
5. Desktop send requests respect thread/default SIM rules.

---

## 11.2.5 Spam Folder

Features:

1. Spam folder in drawer.
2. Spam thread list.
3. Spam message details.
4. Mark Not Spam.
5. Delete spam.
6. Spam folder sync.
7. Spam unread count.

Acceptance criteria:

1. Spam messages never appear in All Messages.
2. Spam messages do not trigger notification.
3. Mark Not Spam moves thread/message back to inbox.
4. Spam status syncs to desktop.

---

## 11.2.6 Spam Scoring

Features:

1. Download global spam rules.
2. Cache latest valid rule set.
3. Apply spam score before notification.
4. Support number prefix rules.
5. Support OTP negative-score rules.
6. Support sender and content rules.
7. Support feedback: Spam / Not Spam.

Acceptance criteria:

1. App continues using cached rules if server is unavailable.
2. Invalid rule payload is rejected.
3. OTP-like messages can reduce total spam score.
4. High-score messages move to Spam.
5. Spam decision includes reason list in message details.

---

## 11.2.7 Notifications

Features:

1. Global notification settings.
2. Contact/thread ringtone setting.
3. Contact/thread silent setting.
4. No sound for non-contacts.
5. Notification content privacy setting.
6. Mute thread.
7. Vibration setting.
8. Spam notification suppression.

Notification content options:

1. Show sender and message preview.
2. Show sender only.
3. Hide sender and message content.

Unknown sender options:

1. Normal notification.
2. Silent notification.
3. No notification.

Acceptance criteria:

1. Spam messages show no notification.
2. Contact-specific ringtone overrides global ringtone.
3. Silent contact/thread shows no sound.
4. Non-contact setting is applied correctly.
5. Notification preview respects privacy setting.

---

## 11.2.8 Sync

Features:

1. Sync off by default.
2. User can enable sync.
3. Full initial sync of all current SMS messages.
4. Incremental sync after initial sync.
5. Client-encrypted SMS body.
6. Sync Spam folder.
7. Sync pinned state.
8. Sync archive state.
9. Sync unread/read state.
10. Sync thread settings.
11. Sync delete events.
12. Do not sync deleted message history.

Acceptance criteria:

1. After enabling sync, all current SMS messages upload.
2. SMS bodies are encrypted before upload.
3. Server cannot read SMS body.
4. Spam folder appears on desktop after sync.
5. Deleted messages are deleted from server after sync.
6. Historical deleted messages are not uploaded.
7. Sync status is visible to user.

---

## 11.2.9 Settings

Settings categories:

1. Notifications
2. Unknown Senders
3. Spam Protection
4. Dual SIM
5. Sync
6. Desktop Devices
7. Privacy & Security
8. Appearance
9. About

Acceptance criteria:

1. Settings are accessible from drawer.
2. User can change all notification options.
3. User can manage sync.
4. User can view and revoke desktop devices.
5. User can see spam rule version.

---

# 11.3 Phase 1 Backend Requirements

## 11.3.1 Auth and User Accounts

Features:

1. Phone number login.
2. OTP verification.
3. User account creation.
4. Device registration.
5. Token refresh.
6. Logout.
7. Revoke device.
8. Delete account.

Acceptance criteria:

1. User can register with phone number.
2. Android device can register.
3. Desktop device can pair after approval.
4. Revoked devices lose access.

---

## 11.3.2 Spam Rules Service

Features:

1. Store global spam rules.
2. Version rules.
3. Publish latest rule version.
4. Roll back rule version.
5. Serve latest rules to Android.
6. Receive spam feedback.
7. Admin panel for rule management.

Acceptance criteria:

1. Android can fetch latest rule set.
2. Rule set includes threshold and rule version.
3. Invalid rules cannot be published.
4. Admin can roll back to earlier version.
5. Feedback is stored without requiring raw SMS body by default.

---

## 11.3.3 Sync Service

Features:

1. Store encrypted SMS payloads.
2. Store sync metadata.
3. Store thread state.
4. Store spam state.
5. Store pin state and pin timestamp.
6. Store archive state.
7. Store read/unread state.
8. Store delete events.
9. Maintain sync cursors.
10. Support initial full sync.
11. Support incremental sync.
12. Sync Spam folder.

Acceptance criteria:

1. Server stores only encrypted SMS body.
2. Server can sync messages to desktop without reading body.
3. Initial sync handles large SMS databases.
4. Incremental sync handles new incoming/outgoing SMS.
5. Delete events remove messages from server.
6. Deleted history is not retained.
7. Spam folder is synced.

---

## 11.3.4 Device and Desktop Pairing Service

Features:

1. Generate QR pairing session.
2. Android scans QR.
3. User approves desktop pairing.
4. Backend registers desktop device.
5. Desktop receives sync access.
6. User can revoke desktop device from Android.
7. Pairing session expires automatically.

Acceptance criteria:

1. QR pairing is secure and time-limited.
2. Desktop cannot access account before Android approval.
3. Revoked desktop loses access immediately.
4. Device list shows device name, platform, and last active time.

---

## 11.3.5 Desktop SMS Relay Service

Features:

1. Desktop creates SMS send request.
2. Backend sends request to Android.
3. Android sends SMS through SIM.
4. Android reports status.
5. Backend updates desktop status.
6. Queue requests while Android is temporarily offline.
7. Expire old unsent requests.

Acceptance criteria:

1. Desktop can request SMS send.
2. Android receives request in near real time.
3. If Android is offline, desktop shows waiting state.
4. If SMS fails, desktop shows failed state.
5. SIM selection is respected.

---

## 11.3.6 Admin Panel

Features:

1. Manage spam rules.
2. Publish rule versions.
3. Roll back rules.
4. View feedback metrics.
5. View sync system health.
6. View device/session counts.
7. View SMS relay success/failure metrics.

Acceptance criteria:

1. Admin can safely publish new spam rules.
2. Admin can monitor spam rule performance.
3. Admin can detect sync failures.
4. Admin can detect desktop relay failures.

---

# 11.4 Phase 1 Desktop Requirements

## 11.4.1 Platform and Stack

Requirements:

1. .NET 10.
2. Avalonia UI.
3. Windows support.
4. macOS support.
5. Linux support.
6. Local encrypted storage.
7. WebSocket or SignalR realtime connection.

Acceptance criteria:

1. App builds for Windows, macOS, and Linux.
2. UI behaves consistently across platforms.
3. App can store local encrypted synced data.

---

## 11.4.2 Pairing

Features:

1. Show QR code.
2. Wait for Android approval.
3. Receive device credentials after approval.
4. Store credentials securely.
5. Show paired account.
6. Allow logout.

Acceptance criteria:

1. Desktop cannot access SMS before pairing.
2. Pairing expires if not approved.
3. Desktop can be revoked from Android.
4. Revoked desktop logs out automatically.

---

## 11.4.3 SMS Thread UI

Features:

1. Show All Messages.
2. Show Spam folder.
3. Show Archived.
4. Show Pinned threads.
5. Pinned threads sorted by pin date.
6. Search threads.
7. Show conversation view.
8. Show emoji-only messages without bubble.

Acceptance criteria:

1. Desktop thread order matches Android.
2. Spam folder is visible.
3. Deleted messages disappear after sync.
4. Encrypted SMS bodies decrypt locally.

---

## 11.4.4 Send SMS from Desktop

Features:

1. Compose SMS.
2. Reply to thread.
3. Select SIM if available.
4. Send request to Android through backend.
5. Show sending status.
6. Show waiting-for-phone status.
7. Show failed status.
8. Retry failed sends.

Acceptance criteria:

1. SMS is sent by Android phone, not desktop directly.
2. Desktop shows accurate status.
3. SIM selection or thread default SIM is respected.
4. Failed messages can be retried.

---

## 11.4.5 Desktop Settings

Features:

1. Sync status.
2. Device info.
3. Logout.
4. Notification settings.
5. Local data delete.
6. Theme setting.

Acceptance criteria:

1. User can delete local desktop data.
2. User can log out desktop device.
3. User can see sync connection status.

---

# 11.5 Phase 1 Exit Criteria

Phase 1 is complete when:

1. Android app can be used as a full SMS app.
2. SMS send/receive works reliably.
3. Dual SIM works.
4. Spam scoring works with server rules.
5. Spam folder works.
6. Notifications respect all rules.
7. Sync uploads all current SMS after being enabled.
8. SMS body is client-encrypted.
9. Desktop app can pair, read synced SMS, show spam folder, and send SMS through Android.
10. Deleted history is not synced.
11. Product is ready for beta users.

---

# Phase 2: Direct Messages and Picture Messages

## 12.1 Phase Goal

Add app-to-app direct messaging over the internet, including text and picture messages. Direct messages are not encrypted in Phase 2.

---

# 12.2 Phase 2 Android App Requirements

## 12.2.1 Direct Message Identity

Features:

1. User account linked to phone number.
2. Discover whether a contact uses the app.
3. Show Direct Message availability.
4. Allow user to enable/disable direct messaging.

Acceptance criteria:

1. App can show whether recipient supports Direct Message.
2. User can choose SMS or Direct Message where applicable.
3. If recipient is not registered, app falls back to SMS.

---

## 12.2.2 Direct Text Messages

Features:

1. Send direct text message.
2. Receive direct text message.
3. Delivery status.
4. Read status.
5. Push notification.
6. Emoji-only message without bubble.

Acceptance criteria:

1. Direct messages are visually distinct from SMS.
2. User always knows whether message is SMS or Direct.
3. Emoji-only direct messages show without bubble.
4. Direct messages do not use carrier SMS.

---

## 12.2.3 Picture Messages

Features:

1. Pick image from gallery.
2. Capture image from camera.
3. Compress image.
4. Upload image.
5. Send image direct message.
6. Show image in conversation.

Acceptance criteria:

1. Picture messages work only for Direct Message mode.
2. Picture messages are not MMS.
3. Upload progress is visible.
4. Failed image send can be retried.

---

# 12.3 Phase 2 Backend Requirements

## 12.3.1 Direct Message Service

Features:

1. User discovery by phone hash.
2. Send direct text message.
3. Receive direct text message.
4. Delivery status.
5. Read status.
6. Offline message storage.
7. Push notifications.
8. Abuse prevention.

Acceptance criteria:

1. Messages deliver when recipient is online.
2. Offline messages are delivered later.
3. Sender receives delivery status.
4. Read status is updated when recipient opens message.
5. Rate limits prevent abuse.

---

## 12.3.2 Picture Message Service

Features:

1. Upload image.
2. Store image.
3. Generate image metadata.
4. Deliver image message.
5. Delete image when message is deleted.
6. Apply file size limits.
7. Apply content-type validation.

Acceptance criteria:

1. Only supported image types are accepted.
2. Large images are rejected or compressed on client.
3. Picture messages appear on Android and desktop.
4. Deleted picture messages remove storage object if no longer needed.

---

## 12.3.3 Direct Message Safety

Features:

1. Rate limiting.
2. Block user.
3. Report user.
4. Disable abusive accounts.
5. Basic admin moderation.

Acceptance criteria:

1. User can block a direct message sender.
2. User can report abusive direct messages.
3. Admin can disable abusive account.
4. Backend limits message spam.

---

# 12.4 Phase 2 Desktop Requirements

## 12.4.1 Direct Text Messages

Features:

1. Show Direct Message availability.
2. Send direct text message.
3. Receive direct text message.
4. Show delivery/read status.
5. Emoji-only message without bubble.

Acceptance criteria:

1. Desktop supports direct text messages.
2. User can distinguish SMS from Direct Message.
3. Direct messages sync across Android and desktop.

---

## 12.4.2 Picture Messages

Features:

1. Select image from desktop.
2. Upload image.
3. Send picture direct message.
4. View image messages.
5. Retry failed picture send.

Acceptance criteria:

1. Picture direct messages work on desktop.
2. Desktop does not send MMS.
3. Upload progress is visible.
4. Failed picture messages can be retried.

---

# 12.5 Phase 2 Exit Criteria

Phase 2 is complete when:

1. App-to-app direct text messages work.
2. Picture messages work through Direct Message mode.
3. Direct messages are clearly separated from SMS.
4. SMS fallback works.
5. Desktop supports direct messages and pictures.
6. Basic abuse prevention is live.

---

# Phase 3: Hardening, Scale, and Advanced Productivity

## 13.1 Phase Goal

Improve reliability, scalability, privacy controls, analytics, and power-user features after MVP and Phase 2 are validated.

---

# 13.2 Phase 3 Android App Requirements

## 13.2.1 Reliability Improvements

Features:

1. Better failed SMS handling.
2. Better dual SIM edge-case support.
3. Better background sync.
4. Better offline behavior.
5. Battery optimization handling.
6. Import/export local backup.

Acceptance criteria:

1. SMS sending failure reasons are clearer.
2. Sync continues reliably in background.
3. App warns user if Android battery restrictions affect sync.
4. User can export local backup.

---

## 13.2.2 Advanced Inbox Management

Features:

1. Bulk actions.
2. Advanced search filters.
3. Scheduled SMS.
4. Reminder on thread.
5. Smart categories, optional.
6. Auto-delete spam after X days.

Acceptance criteria:

1. User can bulk delete/archive/mark spam.
2. User can search by sender, date, and content locally.
3. User can schedule SMS if technically supported.
4. Spam auto-delete respects user setting.

---

## 13.2.3 Better Spam UX

Features:

1. Show spam reasons.
2. Show rule version.
3. User feedback history.
4. Safer OTP handling.
5. False-positive recovery UX.

Acceptance criteria:

1. User understands why message was spam.
2. User can easily restore false-positive spam.
3. OTP messages are less likely to be wrongly hidden.

---

# 13.3 Phase 3 Backend Requirements

## 13.3.1 Scale and Performance

Features:

1. Sync queue optimization.
2. Large account sync support.
3. Better retry system.
4. Better relay queue.
5. Monitoring dashboards.
6. Alerting.

Acceptance criteria:

1. Backend handles large initial syncs.
2. SMS relay latency is monitored.
3. Failed jobs are retried safely.
4. System health is visible to admins.

---

## 13.3.2 Analytics

Metrics:

1. Active users.
2. Default SMS activation rate.
3. Sync enabled rate.
4. Desktop pairing rate.
5. SMS sent from desktop.
6. Spam messages classified.
7. False positive feedback.
8. Rule performance.
9. Direct message usage.
10. Picture message usage.

Acceptance criteria:

1. Product team can see core metrics.
2. Admin can evaluate spam rules.
3. System team can monitor failures.

---

## 13.3.3 Privacy Tools

Features:

1. Delete cloud data.
2. Export cloud data.
3. Revoke all devices.
4. Rotate encryption keys, if feasible.
5. Data retention controls.

Acceptance criteria:

1. User can delete synced server data.
2. User can revoke all desktop devices.
3. Data deletion is completed and logged.
4. Privacy actions are clear to user.

---

# 13.4 Phase 3 Desktop Requirements

## 13.4.1 Productivity Features

Features:

1. Advanced search.
2. Keyboard shortcuts.
3. Bulk actions.
4. Local notifications.
5. Multi-window support, optional.
6. Export conversation, optional.

Acceptance criteria:

1. Desktop app is comfortable for daily business usage.
2. Power users can manage many conversations quickly.
3. Search is fast on local decrypted data.

---

## 13.4.2 Reliability

Features:

1. Better offline mode.
2. Better reconnect behavior.
3. Sync conflict handling.
4. Local database repair.
5. Clear connection status.

Acceptance criteria:

1. Desktop reconnects automatically.
2. Desktop shows when Android phone is offline.
3. Desktop sync state remains consistent.

---

# 13.5 Phase 3 Exit Criteria

Phase 3 is complete when:

1. Product is stable for daily use.
2. Sync is reliable for large inboxes.
3. Desktop app is productive and stable.
4. Spam rules can be improved safely.
5. Privacy and data controls are production-grade.

---

# 14. Core Data Model

## 14.1 User

Fields:

1. id
2. phone_number_hash
3. encrypted_phone_number
4. created_at
5. last_active_at
6. sync_enabled
7. direct_message_enabled
8. deleted_at

## 14.2 Device

Fields:

1. id
2. user_id
3. device_name
4. device_type: android, desktop
5. platform
6. public_key
7. push_token
8. last_active_at
9. revoked_at

## 14.3 Thread

Fields:

1. id
2. user_id
3. encrypted_phone_number
4. phone_number_hash
5. encrypted_contact_name
6. last_message_at
7. unread_count
8. is_spam
9. is_archived
10. is_pinned
11. pinned_at
12. is_muted
13. notification_mode
14. custom_ringtone
15. default_sim_id
16. created_at
17. updated_at
18. deleted_at

## 14.4 Message

Fields:

1. id
2. user_id
3. thread_id
4. client_message_id
5. encrypted_body
6. encrypted_sender
7. encrypted_receiver
8. direction: incoming, outgoing
9. message_type: sms, direct_text, direct_picture
10. status: received, sending, sent, delivered, failed, read
11. timestamp
12. sim_id
13. spam_score
14. spam_reasons
15. is_spam
16. is_deleted
17. created_at
18. updated_at
19. deleted_at

## 14.5 Spam Rule

Fields:

1. id
2. version
3. type
4. pattern
5. score
6. enabled
7. created_at
8. updated_at

## 14.6 Sync Cursor

Fields:

1. id
2. user_id
3. device_id
4. cursor
5. last_synced_at

## 14.7 Desktop SMS Request

Fields:

1. id
2. user_id
3. desktop_device_id
4. android_device_id
5. encrypted_message_body
6. encrypted_target_number
7. sim_id
8. status
9. created_at
10. expires_at
11. completed_at
12. failure_reason

---

# 15. Required APIs

## 15.1 Spam Rules

### GET /api/v1/spam-rules/latest

Returns latest global spam rules.

Response:

1. version
2. threshold
3. rules
4. created_at

---

## 15.2 Spam Feedback

### POST /api/v1/spam-feedback

Payload:

1. sender_hash
2. message_hash
3. rule_version
4. spam_score
5. user_action: spam or not_spam
6. triggered_rule_ids

---

## 15.3 Device Registration

### POST /api/v1/devices

Payload:

1. device_name
2. device_type
3. platform
4. push_token
5. public_key

---

## 15.4 Initial Sync

### POST /api/v1/sync/initial

Payload:

1. device_id
2. encrypted_threads
3. encrypted_messages
4. sync_started_at

---

## 15.5 Incremental Sync Upload

### POST /api/v1/sync/upload

Payload:

1. device_id
2. cursor
3. changes

Change types:

1. message_created
2. message_updated
3. message_deleted
4. thread_updated
5. thread_deleted
6. spam_status_changed
7. pin_changed
8. archive_changed
9. read_state_changed
10. notification_setting_changed

---

## 15.6 Sync Download

### GET /api/v1/sync/updates?cursor={cursor}

Response:

1. changes
2. next_cursor
3. has_more

---

## 15.7 QR Pairing

### POST /api/v1/pairing/session

Creates pairing session for desktop.

### POST /api/v1/pairing/approve

Android approves desktop pairing.

### POST /api/v1/pairing/revoke

Revokes desktop device.

---

## 15.8 Desktop SMS Send

### POST /api/v1/desktop/sms/send

Payload:

1. desktop_device_id
2. encrypted_target_number
3. encrypted_message_body
4. sim_id
5. thread_id
6. client_message_id

---

## 15.9 Direct Messages — Phase 2

### POST /api/v1/direct/messages

Payload:

1. recipient_phone_hash
2. message_type
3. body
4. attachment_id
5. client_message_id

---

# 16. Success Metrics

## 16.1 Product Metrics

1. Default SMS activation rate
2. Day 1 retention
3. Day 7 retention
4. Day 30 retention
5. SMS sent per active user
6. Desktop pairing rate
7. SMS sent from desktop
8. Sync enabled rate
9. Direct message activation rate in Phase 2

## 16.2 Spam Metrics

1. Spam messages classified
2. Spam false positive rate
3. Mark Not Spam actions
4. Mark Spam actions
5. Top triggered spam rules
6. OTP false-positive rate
7. Number prefix rule performance

## 16.3 Sync Metrics

1. Initial sync success rate
2. Initial sync duration
3. Incremental sync latency
4. Desktop sync success rate
5. Delete event success rate
6. Sync failure rate

## 16.4 Desktop Metrics

1. Desktop app activation
2. QR pairing success rate
3. Desktop send success rate
4. Waiting-for-phone rate
5. Desktop crash rate
6. Desktop daily active users

---

# 17. Key Risks

## 17.1 Android Default SMS Risk

The app depends on becoming the default SMS app. If users do not accept this, the app cannot function fully.

Mitigation:

1. Clear onboarding explanation.
2. Show benefits before asking.
3. Graceful limited mode if not default.

## 17.2 Google Play Policy Risk

SMS permissions are sensitive. The app must clearly be a default SMS app and must not use SMS data outside declared purposes.

Mitigation:

1. Strong privacy policy.
2. Clear permission declaration.
3. Sync opt-in.
4. Client-side encryption.
5. No unnecessary SMS data upload.

## 17.3 Dual SIM Complexity

Dual SIM behavior can vary by Android version and manufacturer.

Mitigation:

1. Test on Samsung, Xiaomi, Pixel, OnePlus, and low-end Android devices.
2. Add fallback SIM selection behavior.
3. Log SIM-related errors locally.

## 17.4 Client Encryption Complexity

Client encryption makes search and server processing harder.

Mitigation:

1. Search locally on Android and desktop.
2. Keep server metadata minimal.
3. Design key exchange carefully in Phase 0.

## 17.5 Desktop SMS Reliability

Desktop sending depends on Android phone being online.

Mitigation:

1. Show clear status.
2. Queue temporarily.
3. Expire old requests.
4. Push Android reliably.

---

# 18. Final MVP Definition

MVP is successful when the product can replace the user’s default SMS app and provide:

1. SMS inbox.
2. SMS sending and receiving.
3. Dual SIM support.
4. Telegram-like UI.
5. Spam folder.
6. Server-managed global spam rules.
7. Number prefix spam scoring.
8. OTP negative-score rules.
9. No notification for spam.
10. Contact-based ringtone/silent settings.
11. Silent or no notification for non-contacts.
12. Notification content privacy setting.
13. Pin thread sorted by pin date.
14. Client-encrypted sync.
15. Full initial SMS sync after enabling sync.
16. Spam folder sync.
17. No deleted history sync.
18. Native .NET 10 Avalonia desktop app.
19. Desktop SMS reading.
20. Desktop SMS sending through Android phone.
