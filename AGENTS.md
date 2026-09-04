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

### Build and test status

As of the current commit the build is green: `:app:assembleDebug` compiles and packages, and
`:app:testDebugUnitTest` passes all 176 unit tests. There are no known pre-existing test
failures — if a test fails for you, treat it as a real regression, not as environment noise.

The two failures previously listed here are gone: `inboxThreads()` now computes `unreadCount`
with a correlated `COUNT(*)` sub-select (so `SmsDaoTest` passes), and the `SmsReceiver.kt:135`
lint error no longer exists (that file is now 24 lines long).

- **Known issues:** see `docs/android-app-bugs.md` for the current list of open bugs.
- **Lint:** `:app:lintDebug` has NOT been re-verified since the code it flagged changed. Run it
  yourself before relying on it; do not assume it is clean.
