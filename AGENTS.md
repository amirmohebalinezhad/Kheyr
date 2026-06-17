# Kheyr

Kheyr is a single-module Android application (`:app`) — a modern SMS client with on-device
spam filtering, dual-SIM support, end-to-end-encrypted sync, and desktop SMS relay. It is
written in Kotlin with Jetpack Compose UI and Room for local persistence. Most of the code is
plain Kotlin domain/logic classes covered by an extensive JVM unit-test suite
(JUnit4 + Robolectric); the Android Activity/UI layer is a thin shell on top.

There is no application server or web frontend — the deliverable is a debug APK.

## Cursor Cloud specific instructions

Toolchain (JDK 17, Android SDK, Gradle 8.14.4) is pre-installed in the VM image; the startup
update script only re-points `local.properties` at the SDK. Notes below are the non-obvious bits.

### Build / test / lint commands

There is **no Gradle wrapper** (`./gradlew` does not exist). Use the system `gradle` (8.14.4),
matching `.github/workflows/android-apk.yml`:

- Build debug APK: `gradle --no-daemon :app:assembleDebug`
  (output: `app/build/outputs/apk/debug/app-debug.apk`)
- Unit tests: `gradle --no-daemon :app:testDebugUnitTest`
- Lint: `gradle --no-daemon :app:lintDebug`

### Gotchas

- **Use JDK 17, not 21.** The VM has both; default `java` is 21 but the project targets 17 and
  CI builds with 17. `~/.bashrc` sets `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` and prepends
  it to `PATH`, so a normal login shell is already correct. If you spawn a non-login shell, set
  `JAVA_HOME` to JDK 17 explicitly or the build behavior may differ from CI.
- **SDK location:** `ANDROID_HOME`/`ANDROID_SDK_ROOT` point at `~/android-sdk` (set in
  `~/.bashrc`). `local.properties` (`sdk.dir=$HOME/android-sdk`) is git-ignored and recreated by
  the startup update script.
- **No emulator / GUI run.** This VM has no KVM (`/dev/kvm` absent, no `vmx`/`svm`), so the
  Android emulator cannot boot and the app cannot be launched with a UI here. Validate changes
  via the unit-test suite (it exercises the real domain logic: spam scoring, the incoming-SMS
  receive pipeline, Room DAO queries, sync, thread sorting, etc.). Build the APK to confirm it
  compiles/packages.

### Known pre-existing failures (NOT environment issues)

These fail on a clean checkout regardless of setup — do not treat them as broken environment, and
do not "fix" them unless that is the actual task:

- `:app:testDebugUnitTest` — `SmsDaoTest.insertGroupsMessagesByThreadWithLatestPreviewAndUnreadCount`
  fails (`expected:<1> but was:<0>`): the `inboxThreads()` query computes `unreadCount` with a
  `SUM(...)` over a join that only keeps the single newest message, so older unread messages are
  never counted. The other 51 unit tests pass.
- `:app:lintDebug` — fails the build with 1 error: `MissingPermission` at
  `app/src/main/java/com/kheyr/sms/receiver/SmsReceiver.kt:135` (plus 13 warnings). Lint itself
  runs fine; the error is in app code.
