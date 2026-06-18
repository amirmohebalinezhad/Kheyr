# Kheyr

Kheyr is a modern Android SMS client built with Kotlin and Jetpack Compose. It handles everyday text messaging on your phone while adding on-device spam filtering and optional encrypted sync so you can keep conversations on your other devices — without giving the server readable message content.

## Features

### SMS

- Full default-SMS-app support: send, receive, and manage text messages
- Conversation threads with search, pinning, archiving, and blocking
- Contact integration, delivery status, and customizable notifications
- Local encrypted database — message data is protected at rest on your device

### Spam filtering

- On-device spam scoring using configurable rules (sender patterns, keywords, links, short codes, and more)
- Automatic classification into normal, suspicious, and spam
- User feedback to correct mistakes and improve filtering over time
- Optional auto-delete for old spam messages

### Optional encrypted sync

Sync is **entirely optional**. Kheyr works fully offline as a local SMS app; you only enable sync if you want messages available on other paired devices.

When you opt in:

- **Client-side encryption** — message bodies and sensitive fields are encrypted on your phone before anything leaves the device. The sync server stores ciphertext only; it never decrypts your messages.
- **Hashed identifiers** — phone numbers used for sync lookups are salted and hashed rather than stored in plain text.
- **You stay in control** — sync remains disabled until you explicitly turn it on during onboarding or in settings. You can skip it, pair or revoke devices, and sign out at any time.

### Desktop SMS relay

Pair a Windows, macOS, or Linux desktop client with your phone to read synced threads and send SMS through your device when it is online. Desktop access uses the same optional, client-encrypted sync model.

### Coming in future versions

- **Direct messaging** — Kheyr-to-Kheyr messages over data instead of carrier SMS (not ready yet)
- **Group messaging** — multi-recipient conversations beyond standard SMS groups (not ready yet)

These features are planned for upcoming releases. The current version focuses on reliable SMS, spam protection, and optional encrypted sync.

## Getting started

### Install

Download the latest debug APK from [GitHub Actions artifacts](https://github.com/amirmohebalinezhad/kheyr/actions) (workflow: **Android APK**), or build locally (see below).

Set Kheyr as your **default SMS app** when prompted so it can read, send, and receive messages.

### Build from source

Requires JDK 17, Android SDK, and Gradle 8.14+.

```bash
gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Run unit tests:

```bash
gradle --no-daemon :app:testDebugUnitTest
```

To use sync or desktop relay, configure `API_BASE_URL` in `app/build.gradle` and run the [backend](backend/README.md) locally or against your deployment.

## Contributing

Kheyr is **free to use**. If you find it useful, please consider contributing — bug reports, feature ideas, documentation improvements, and pull requests are all welcome.

1. Fork the repository
2. Create a branch for your change
3. Make your edits and run the unit tests
4. Open a pull request with a clear description of what you changed and why

See [AGENTS.md](AGENTS.md) for project structure and development notes.

## License

Kheyr is free to use. You may use, modify, and share it with others. Contributions back to the project are encouraged so everyone can benefit.

## Related

- [Backend & admin API](backend/README.md) — ASP.NET Core sync server, SignalR realtime hub, and admin panel
