# Kheyr SMS Interactive Demo

A browser-based interactive demo of the Kheyr SMS app UI. It runs without an Android device or emulator.

## Run locally

```bash
cd demo
python3 -m http.server 8765
```

Open http://localhost:8765/index.html

## What it demonstrates

- Thread list with pinned-first ordering, unread badges, and dual-SIM indicators
- Compose conversation UI with message bubbles and emoji-only rendering
- Retryable failed send status
- Dual-SIM chip selector in the composer
- Navigation drawer (All Messages, Spam, Archived, Pinned, Contacts, Desktop Sync, Settings, Help)
- Spam folder with suppressed notifications and spam score details
- OTP negative-score handling in Bank Alerts thread
- Light and dark theme toggle
- Live event log of user actions

## Demo recording

A walkthrough video is available at `/opt/cursor/artifacts/kheyr-sms-demo-walkthrough.mp4` when running in Cursor Cloud.
