# Android app: known bugs

Audit of the `:app` module at commit `74e3fdb` (`main`, 2026-07-07), performed 2026-09-04 by reading the
source and, where possible, verifying with the JVM unit-test toolchain (see the appendix). Line numbers refer
to that commit.

Severity scale: **Critical** = nothing ships / crash loop, **High** = feature does not work or loses data,
**Medium** = wrong behaviour users will notice, **Low** = robustness, polish, dead code.

## Summary

| ID | Severity | Area | Title |
| --- | --- | --- | --- |
| [B-01](#b-01) | Critical | Build | `SmsDao` declares `syncedTelephonyIdsInRange` four times, so the app does not compile |
| [B-02](#b-02) | High | CI | CI build depends on the Aliyun Maven mirror and fails when it returns 502 |
| [B-03](#b-03) | High | Sync | Nothing ever writes to the sync queue, so sync never uploads a message |
| [B-04](#b-04) | High | Account | Access token is never refreshed; every authenticated feature dies after one hour |
| [B-05](#b-05) | High | Storage | Cloud backup restore (or Keystore invalidation) turns into a crash loop on launch |
| [B-06](#b-06) | High | Sync | A failed upload deletes the queued events instead of retrying them |
| [B-07](#b-07) | Medium | Dates | Jalali dates are one day early for March 1 to December 31 of every Gregorian leap year |
| [B-08](#b-08) | Medium | Dual SIM | Incoming SMS store the SIM *slot index* in the column the app reads as a *subscription id* |
| [B-09](#b-09) | Medium | Composer | After a failed send the Send button stays disabled until the chat is reopened |
| [B-10](#b-10) | Medium | Composer | Retrying a failed message has no error handling and can crash the app |
| [B-11](#b-11) | Medium | Settings | "Delete cloud data" and "Export cloud data" always fail (network call on the main thread) |
| [B-12](#b-12) | Medium | Intents | `sms:` / `smsto:` links open the inbox instead of a conversation with the recipient |
| [B-13](#b-13) | Medium | Delete | Deleting a chat while not the default SMS app brings it back on the next refresh |
| [B-14](#b-14) | Medium | Delete | "Undo" after deleting a chat restores only the app copy; the system SMS rows are already gone |
| [B-15](#b-15) | Medium | Onboarding | Onboarding dead-ends when contacts or notification permission is permanently denied |
| [B-16](#b-16) | Medium | Permissions | No way to recover when SMS permission or the default-SMS role is lost after onboarding |
| [B-17](#b-17) | Medium | Receive | Incoming SMS are dated with the carrier timestamp, so delayed messages sort into the past |
| [B-18](#b-18) | Medium | Spam | "Auto-delete spam after N days" setting does nothing |
| [B-19](#b-19) | Medium | Blocking | Blocked-sender suppression exists but nothing can block a sender |
| [B-20](#b-20) | Medium | Account | Logging out of the optional sync account forces the whole onboarding again |
| [B-21](#b-21) | Medium | Dual SIM | SIM badge is computed for thread rows but never drawn |
| [B-22](#b-22) | Low | Receive | The `SecurityException` guard around telephony sync does not cover the new gap-window query |
| [B-23](#b-23) | Medium | Spam | Any spam-scored message re-flags the whole thread, overriding the user's "Not spam" |
| [B-24](#b-24) | Low | Sync | Downloaded sync changes are counted, then thrown away |
| [B-25](#b-25) | Low | Sync | Sync cursor is put in the URL without encoding |
| [B-26](#b-26) | Low | Spam | Unknown rule type from the server throws inside `SpamRulesWorker` |
| [B-27](#b-27) | Low | Send | `SmsSendStatusReceiver` worker thread has no exception guard |
| [B-28](#b-28) | Low | Send | Delivery reports are treated as "delivered" without reading the report status |
| [B-29](#b-29) | Low | Import | Drafts from other SMS apps are imported as outgoing messages with status Received |
| [B-30](#b-30) | Low | Data | Re-syncing an existing message resets the thread row (`displayName`, `createdAt`) |
| [B-31](#b-31) | Low | Contacts | Contact name cache is never invalidated while the process lives |
| [B-32](#b-32) | Low | UI | Thread rows decode the full-size contact photo for every avatar |
| [B-33](#b-33) | Low | UI | Notification is still posted for the conversation currently on screen |
| [B-34](#b-34) | Low | UI | Call button is shown for alphanumeric senders (dials `tel:VERIFY`) |
| [B-35](#b-35) | Low | UI | Search highlighting can index past the end of the string for some Unicode input |
| [B-36](#b-36) | Low | Docs | `AGENTS.md` describes failures that no longer match the code |
| [B-37](#b-37) | High | Tests | Unit-test source set does not compile (test for a class deleted in `c924f87`) |

---

## Build and CI

<a id="b-01"></a>
### B-01. `SmsDao` declares `syncedTelephonyIdsInRange` four times (Critical)

`app/src/main/java/com/kheyr/sms/data/SmsDao.kt:55`, `:72`, `:89`, `:247`

The same `@Query` method, with an identical signature, is declared four times in the `SmsDao` interface.
Kotlin rejects this with `Conflicting overloads`, so `:app:compileDebugKotlin` fails and no APK can be
built from `main`. The duplicates were introduced by commit `74e3fdb` ("Improve system SMS
synchronization"), which appears to have been committed without compiling.

Verified by running `gradle :app:compileDebugKotlin` at this commit (KSP/Room passes, the Kotlin
compiler fails):

```
e: SmsDao.kt:55:5 Conflicting overloads: ...
e: SmsDao.kt:72:5 Conflicting overloads: ...
e: SmsDao.kt:89:5 Conflicting overloads: ...
e: SmsDao.kt:247:5 Conflicting overloads: ...
e: SmsRepository.kt:110:41 Overload resolution ambiguity between candidates: ...
BUILD FAILED
```

CI did not flag it because the same push failed earlier for an unrelated reason (B-02), before reaching
the compiler.

**Fix:** keep one declaration (the one at line 247 sits with the other telephony-id queries) and delete
the other three.

<a id="b-02"></a>
### B-02. CI build depends on the Aliyun Maven mirror (High)

`settings.gradle:3-6`, `settings.gradle:25-26`, `.github/workflows/android-apk.yml`

Commit `f4c7b83` put `https://maven.aliyun.com/...` mirrors ahead of `google()` and `mavenCentral()` in
both `pluginManagement` and `dependencyResolutionManagement`. Gradle only falls through to the next
repository when an artifact is *missing* (404); on a transport or server error it disables the repository
and fails the whole resolution. That is exactly what happened on the last `main` build (Actions run
#121): `Could not GET '.../bcutil-jdk18on-1.77.pom'. Received status code 502` followed by
`There are 56 more failures with identical causes`, and the job failed without ever compiling anything.

Anyone building from a network where the mirror is slow, blocked, or flaky gets the same failure. The
workflow also runs only `assembleDebug`; `testDebugUnitTest` and `lintDebug` are never executed in CI, so a
red unit-test suite (see B-36) goes unnoticed.

**Fix:** remove the mirrors from `settings.gradle` (or gate them behind a Gradle property for developers
who need them) so `google()` and `mavenCentral()` are used by default, and add the unit-test task to the
workflow.

<a id="b-37"></a>
### B-37. Unit-test source set does not compile (High)

`app/src/test/java/com/kheyr/sms/spam/SpamClassificationThresholdsTest.kt:8-9`

Commit `c924f87` ("remove dead duplicate SpamClassification/threshold logic") deleted
`SpamClassificationThresholds` (and the duplicate `SpamClassification` enum in the `spam` package) from the
main source set but left this test behind. Kotlin reports `Unresolved reference 'SpamClassification'` and
`Unresolved reference 'SpamClassificationThresholds'`, so `:app:compileDebugUnitTestKotlin` fails and not a
single unit test has been runnable since 2026-06-19, independently of B-01. Because CI never runs the test
task (B-02), nobody noticed; `AGENTS.md` still claims 51 tests pass (B-36).

Verified by running `gradle :app:testDebugUnitTest` on a working tree with B-01 fixed:

```
e: SpamClassificationThresholdsTest.kt:8:22 Unresolved reference 'SpamClassification'.
e: SpamClassificationThresholdsTest.kt:8:53 Unresolved reference 'SpamClassificationThresholds'.
BUILD FAILED
```

**Fix:** delete the test (its behaviour is covered by `SpamScorerTest`) or port it to
`com.kheyr.sms.domain.SpamScorer`.

---

## Data loss and crashes

<a id="b-03"></a>
### B-03. Sync never uploads a message (High)

`app/src/main/java/com/kheyr/sms/sync/RoomSyncQueueStore.kt:24`, `:35`;
`app/src/main/java/com/kheyr/sms/worker/SyncWorker.kt:27-32`

`SyncWorker` drains `sync_queue` through `RoomSyncQueueStore.pendingRecords()`, but the only two ways to
put anything into that table, `enqueueMessage()` and `enqueueThreadState()`, have no callers anywhere in
`app/src/main`. Receiving, sending, deleting, pinning, archiving, muting and marking spam all bypass the
queue, and there is no initial backfill when sync is switched on. The queue is therefore always empty and
`uploadPending()` returns 0 on every run.

Consequences: "Enable sync" in onboarding and in Settings appears to work but nothing ever reaches the
backend, a paired desktop never sees any thread, and PRD 5.6 items 3 to 9 (initial backfill, delete,
spam, pin, archive and notification-setting sync) are all unmet. The toggle can also be switched on
without an account, in which case `SyncSettings.canUpload` is false and the 15-minute worker runs forever
doing nothing, with no status shown to the user.

**Fix:** enqueue a `message_change` (or `initial_backfill`) record from `SmsRepository` whenever a
message is inserted or its status changes, enqueue thread-state records from the pin/archive/spam/mute
paths, run an initial backfill when sync is enabled, and require sign-in before the toggle can be turned on.

<a id="b-04"></a>
### B-04. Access token is never refreshed (High)

`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt:43`, `:196-207`;
`app/src/main/java/com/kheyr/sms/preferences/AppPreferences.kt:91-99`

`KheyrApiService.refreshToken()` exists but is never called, `authorized()` simply attaches whatever
access token is in `SharedPreferences`, and `executeJson()` turns any non-2xx response, including 401,
into `null` with no retry. The backend issues 60-minute access tokens (`AccessTokenMinutes` in
`backend/src/Kheyr.Api/appsettings.json`; the client's `parseAuthTokens` also defaults `expires_in` to
3600), so one hour after signing in the sync worker, device list, pairing approval, revoke, logout and
account deletion all silently fail, and `KheyrRealtimeClient` can no longer connect. The only way back is
to log out and sign in again, which also triggers B-20.

**Fix:** on a 401 (or before expiry, using the stored `expires_in`), call `refreshToken()` with the
stored refresh token, persist the new pair, and retry the request once.

<a id="b-05"></a>
### B-05. Backup restore or Keystore invalidation becomes a crash loop (High)

`app/src/main/AndroidManifest.xml:15-17`; `app/src/main/res/xml/backup_rules.xml`;
`app/src/main/res/xml/data_extraction_rules.xml`;
`app/src/main/java/com/kheyr/sms/data/LocalDatabasePassphraseStore.kt:28-39`, `:102-110`;
`app/src/main/java/com/kheyr/sms/data/AppDatabase.kt:48-57`

The manifest enables Android Auto Backup with empty include/exclude rules, so the SQLCipher database
`kheyr_sms.db` and every preferences file are backed up to the user's Google account. The database
passphrase lives in `EncryptedSharedPreferences`, which is wrapped by the device's Keystore master key and
cannot be decrypted on another device. On restore to a new phone (or after any master-key invalidation,
which the class comment lists: credential change, biometric enrolment, OS upgrade) this happens:

1. `LocalDatabasePassphraseStore.openPreferencesResilient()` fails to open the restored prefs file,
   deletes it and the master key, and generates a **new random passphrase**.
2. Room opens the restored `kheyr_sms.db` with that passphrase. SQLCipher fails with
   `file is not a database`; there is no `fallbackToDestructiveMigration()` and no catch around the open.
3. Every launch, every incoming SMS (`SmsReceiver`) and every notification action crashes.

The class comment claims the database "will be reset/re-keyed on the next open", but no code does that.
Deleting `MasterKey.DEFAULT_MASTER_KEY_ALIAS` also destroys the master key that
`SyncEncryptionKeyStore` shares, so the sync content key is lost as collateral (paired desktops can no
longer decrypt anything).

**Fix:** exclude `kheyr_sms.db*` and the two `EncryptedSharedPreferences` files from backup in both
`backup_rules.xml` and `data_extraction_rules.xml`; additionally catch the open failure in
`AppDatabase.buildEncryptedDatabase` and either delete and recreate the database or surface a clear
"local data is unreadable" screen instead of crashing.

<a id="b-06"></a>
### B-06. A failed sync upload deletes the queued events (High)

`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt:129-136`;
`app/src/main/java/com/kheyr/sms/sync/SyncUploader.kt:55-58`; `SyncWorker.kt:36-38`

`SyncApiClient.upload()` returns `Unit`, and `KheyrApiService.upload()` discards the result of
`postJson()` (which is `null` on any HTTP error, timeout or exception). `SyncUploader.uploadPending()`
therefore has no way to learn that the upload failed and always calls `deleteUploaded()` right after
`apiClient.upload()`, despite the comment "Delete only after a confirmed successful upload". Any offline
period or 5xx permanently drops those events, and `SyncWorker` then records `lastSuccessfulUploadAt`.
(Today this is masked by B-03; once the queue is actually fed, this becomes data loss.)

**Fix:** make `upload()` return a `Boolean` (or throw) and only delete rows on success; leave them
pending otherwise so the next run retries.

<a id="b-13"></a>
### B-13. Deleting a chat while not the default SMS app brings it back (Medium)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt:104-135`, `:228-250`;
`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:376-407`, `:412-444`

Delete actions are available regardless of the default-SMS role, and they delete from Room and then from
`Telephony.Sms`. Android silently ignores provider writes from a non-default SMS app, so the system rows
survive. The next thread refresh runs `syncNewTelephonyMessages()`, whose new gap-window backfill
(`findMissingTelephonyIds`, 1000-id window) treats those rows as "missing locally" and re-imports them.
The conversation the user just deleted reappears within seconds.

**Fix:** refuse (or clearly warn on) delete when `DefaultSmsRoleChecker.isDefaultSmsApp()` is false, or
keep a local tombstone table that the backfill respects.

<a id="b-14"></a>
### B-14. "Undo" after deleting a chat restores only the app copy (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:376-407`;
`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt:238-259`

`deleteThreadWithUndo` calls `deleteMessagesByIds()` immediately, and since commit `74e3fdb` that method
also deletes the rows from the system SMS provider. `restoreThreadMessages()` on Undo re-inserts the
snapshot into Room only. After an Undo the messages exist in Kheyr but no longer in the system SMS store,
so they are gone for any other SMS app, for carrier or Google backups, and for a future re-import.

**Fix:** delete only from Room during the undo window and perform the provider delete in
`commitPendingDelete()`.

---

## Messaging correctness

<a id="b-07"></a>
### B-07. Jalali dates are one day early after February in leap years (Medium)

`app/src/main/java/com/kheyr/sms/util/JalaliDateFormatter.kt:63-79`

`gregorianToJalali` is the well-known Borkowski/jalaali algorithm, but it drops the leap-day term: the
reference computes the leap-year corrections with `gy2 = if (gm > 2) gy + 1 else gy`, whereas this
implementation uses the unadjusted year for every term. As a result every date from March 1 to
December 31 of a Gregorian leap year is converted to the previous Jalali day. Examples:

| Gregorian | App shows | Correct |
| --- | --- | --- |
| 2024-03-01 | ۱۰ اسفند ۱۴۰۲ | ۱۱ اسفند ۱۴۰۲ |
| 2024-03-20 (Nowruz) | ۲۹ اسفند ۱۴۰۲ | ۱ فروردین ۱۴۰۳ |
| 2024-12-31 | ۱۰ دی ۱۴۰۳ | ۱۱ دی ۱۴۰۳ |

Every message received in March to December 2024 shows the wrong date today, and the same will happen for
all of 2028. A Python port of the function cross-checked against the `jdatetime` library disagrees on
918 of the 4,383 days between 2020 and 2031 (306 days in each leap year, none elsewhere). The existing
unit test only checks Nowruz 2026, a non-leap year, so it passes.

**Fix:** add the `gm > 2` adjustment (`val gy2 = if (gm > 2) gy + 1 else gy` and use `gy2` for the three
leap-year terms while keeping `365 * gy` on the unadjusted year), and add leap-year test cases.

<a id="b-08"></a>
### B-08. Incoming SMS store the slot index where the app expects a subscription id (Medium)

`app/src/main/java/com/kheyr/sms/receiver/IncomingSmsServices.kt:89-97`;
`app/src/main/java/com/kheyr/sms/receiver/SmsReceiveHandlerFactory.kt:38-42`;
`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt:206`;
`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:241-242`

The `messages.simSlot` column is populated from two sources with different meanings:

- `SmsRepository.syncTelephonyMessages()` fills it from the provider's `sub_id` (a subscription id).
- `RoomIncomingSmsStore.persist()` fills it with `message.simSlot`, the physical slot index parsed from the
  `SMS_DELIVER` intent, even though it correctly writes `message.subscriptionId` into the provider row.

Everything that reads the column treats it as a subscription id: `ComposerSimResolver.resolve()` uses the
thread's value to pick the reply SIM, `SimBadgeResolver` matches it against `SimCard.subscriptionId`, and
`onRetry` passes it straight to `SmsManager.getSmsManagerForSubscriptionId`. Because a freshly received
row already carries its telephony id, it is never re-read from the provider, so the wrong value is
permanent. On a dual-SIM phone the slot index `1` (second SIM) is easily a valid subscription id for the
*first* SIM, and the comment in `ComposerSimResolver` shows the authors were aware this confusion routes
replies out the wrong SIM.

**Fix:** pass `message.subscriptionId` to `insertIncomingSms()` (and ideally rename the column to
`subscriptionId`).

<a id="b-17"></a>
### B-17. Incoming SMS are dated with the carrier timestamp (Medium)

`app/src/main/java/com/kheyr/sms/receiver/SmsReceiveHandlerFactory.kt:37`;
`app/src/main/java/com/kheyr/sms/receiver/IncomingSmsServices.kt:77`, `:93`

`receivedAtMillis` is `SmsMessage.getTimestampMillis()`, the SMSC timestamp from the PDU, and it is written
to both `Telephony.Sms.DATE` and the local `timestamp`. The platform convention (and what every other SMS
app does) is `DATE` = device receive time and `DATE_SENT` = SMSC time. With the current code a message
delayed by the network (or coming from a carrier whose clock is off) sorts into the past inside the
conversation, the thread does not move to the top of the inbox, and messages can appear to arrive
"before" ones the user already read.

**Fix:** use `System.currentTimeMillis()` for `DATE`/`timestamp` and store the SMSC value in
`Telephony.Sms.DATE_SENT`.

<a id="b-22"></a>
### B-22. The `SecurityException` guard does not cover the gap-window query (Low)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt:118-135` versus `:170-182`

`syncTelephonyMessages(...)` wraps its provider query in a `SecurityException` guard with the comment
"READ_SMS can be withdrawn when the app is no longer the default SMS handler", but the new
`findMissingTelephonyIds()` that `syncNewTelephonyMessages()` calls immediately afterwards runs the same
kind of query with no guard (and so does `loadMessages()`). Whatever condition the first guard protects
against reaches the unguarded query a few lines later and throws out of `syncTelephonyMessages()`.

**Fix:** apply the same guard in `findMissingTelephonyIds()` (or catch once in `syncNewTelephonyMessages()`).

<a id="b-23"></a>
### B-23. Spam re-flags the whole thread and overrides "Not spam" (Medium)

`app/src/main/java/com/kheyr/sms/receiver/IncomingSmsServices.kt:98`;
`app/src/main/java/com/kheyr/sms/receiver/SmsReceiveHandler.kt:20-22`

`persistSpam()` calls `repository.updateSpam(threadId, true)` for every message that scores as spam. A
thread the user explicitly restored with "Not spam" is hidden again by the next message that trips the
rules, and a known contact's whole conversation disappears from the inbox after one message containing,
say, "winner", "prize" and a link (35 + 35 + 35 = 105 with the default rules, threshold 70). The
`UserSpamCorrection` / `SpamFeedbackHistory` models exist but nothing records or consults a correction,
and the spam path emits no `SmsRefreshEvents` so the Spam folder does not update while open.

**Fix:** remember per-thread "not spam" corrections (or a `KnownSafeSender` entry) and skip the automatic
flag for those threads; emit a refresh event from the spam path.

<a id="b-29"></a>
### B-29. Drafts are imported as outgoing messages with status Received (Low)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt:196-200`, `:366-372`;
`app/src/main/java/com/kheyr/sms/data/TelephonyDirectionMapper.kt`

`directionFromType()` maps every type other than `MESSAGE_TYPE_INBOX` to `Outgoing`, and `messageStatus()`
maps `MESSAGE_TYPE_DRAFT` (3) to the `else -> Received` branch. Drafts left behind by the previous SMS app
show up as sent bubbles.

**Fix:** skip `MESSAGE_TYPE_DRAFT`/`MESSAGE_TYPE_ALL` rows in the import projection.

---

## Composer and sending

<a id="b-09"></a>
### B-09. After a failed send the Send button stays disabled (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:849-872`;
`app/src/main/java/com/kheyr/sms/ui/SmsComposerState.kt:5`

`onSend` reduces `SendRequested` (which sets `sending = true`) and stores the result in `composerState`
*before* the `!isDefaultSms` check returns early, and the `catch` block only sets `statusMessage`; neither
path dispatches `SendFailed`. `BodyChanged` clears `error` but not `sending`, and the composer's Send
button is `enabled = body.isNotBlank() && !sending`. So one failed send (or one tap while not the
default app) leaves the conversation with a permanently disabled Send button until it is closed and
reopened.

**Fix:** move `composerState = state` after the default-SMS check, and reduce `SendFailed` in the `catch`.

<a id="b-10"></a>
### B-10. Retry has no error handling (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:874-883`

`onRetry` launches `markSending()` + `sender.send()` + `syncTelephonyMessagesByIds()` on the composition
scope with no `try`/`catch` and no default-SMS check, unlike `onSend`. `SmsSender.send()` throws
`IllegalArgumentException` for an undialable recipient, `SecurityException` when `SEND_SMS` has been
revoked, and the provider write fails when the app is not the default SMS app. An exception from a
`rememberCoroutineScope().launch` is uncaught and kills the process.

**Fix:** reuse the guard and `try`/`catch` from `onSend` (and `SendFailed`) in `onRetry`.

<a id="b-27"></a>
### B-27. `SmsSendStatusReceiver` thread has no exception guard (Low)

`app/src/main/java/com/kheyr/sms/receiver/SmsSendStatusReceiver.kt:18-45`

`SmsReceiver` and `NotificationActionReceiver` catch `Throwable` on their worker threads and log; the
send-status receiver only has `finally`. Any failure (database open, provider write when the role was
lost) is an uncaught exception on a bare `Thread` and crashes the process.

<a id="b-28"></a>
### B-28. Delivery reports are trusted without reading their status (Low)

`app/src/main/java/com/kheyr/sms/receiver/SmsSendStatusReceiver.kt:35-40`

For `ACTION_SMS_DELIVERED` the platform always delivers `RESULT_OK`; the actual outcome is in the report PDU
(`SmsMessage.createFromPdu(intent.getByteArrayExtra("pdu")).status`). A negative delivery report is
therefore shown as "Delivered".

---

## Sync, account and desktop

<a id="b-11"></a>
### B-11. "Delete cloud data" and "Export cloud data" always fail (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:906-907`;
`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt:201-207`

Both callbacks call `api.deleteCloudData()` / `api.exportCloudData()` directly from the Compose click
handler, i.e. on the main thread. OkHttp's synchronous `execute()` throws `NetworkOnMainThreadException`,
`executeJson()` swallows it via `runCatching` and returns `null`, and the UI then shows "Configure API base
URL first" even though the URL is configured. Every other API call in the shell correctly uses
`Dispatchers.IO`.

**Fix:** wrap both in `scope.launch(Dispatchers.IO) { ... }` like `onLogout`, and report the real outcome.

<a id="b-20"></a>
### B-20. Logging out forces the whole onboarding again (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:1766-1771`, `:723-746`

`clearLocalSession()` sets `preferences.onboardingComplete = false`, so logging out of (or deleting) the
optional sync account sends the user back to step 0 of onboarding: welcome, default-SMS role, permissions,
and so on, none of which changed. It also leaves `sync_queue` rows and the sync encryption key in place,
although `LogoutPlan` says both should be cleared.

**Fix:** keep `onboardingComplete`, only reset the sync-related preferences, and clear the queue/key.

<a id="b-24"></a>
### B-24. Downloaded sync changes are discarded (Low)

`app/src/main/java/com/kheyr/sms/worker/SyncWorker.kt:40-44`;
`app/src/main/java/com/kheyr/sms/sync/SyncDownloader.kt`

`SyncWorker` downloads `/api/v1/sync/updates`, passes only `changes.length()` to `SyncDownloader.parse()`,
saves the new cursor and drops the payload. Any change made on another device (desktop pin, archive, spam
correction) is acknowledged by advancing the cursor and never applied locally.

<a id="b-25"></a>
### B-25. Sync cursor is not URL-encoded (Low)

`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt:64`

`"/api/v1/sync/updates?cursor=$cursor"` interpolates the opaque cursor directly. A cursor containing `+`,
`=`, `&` or `/` (typical for base64) is corrupted in transit.

---

## Onboarding, permissions and navigation

<a id="b-12"></a>
### B-12. `sms:` / `smsto:` links open the inbox instead of the recipient (Medium)

`app/src/main/AndroidManifest.xml:27-34`; `app/src/main/java/com/kheyr/sms/MainActivity.kt:16-30`;
`app/src/main/java/com/kheyr/sms/ui/MessageBubbleContent.kt:60-63`

The manifest registers `MainActivity` for `ACTION_SENDTO` with the `sms`, `smsto`, `mms` and `mmsto`
schemes (required for the default-SMS role), but the activity only reads its own `open_thread_id` extra.
The recipient in `intent.data` and any `sms_body` / `EXTRA_TEXT` are ignored. Tapping "Message" on a
contact, a phone number in the dialer, or a link on a web page just opens the inbox. The app's own copy
menu "send SMS to this number" action fires the same `smsto:` intent and therefore also lands on the inbox.

**Fix:** parse `intent.data.schemeSpecificPart` and the body extras in `MainActivity`, and route them
through `openConversationForRecipient`.

<a id="b-15"></a>
### B-15. Onboarding dead-ends on permanently denied permissions (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:1298-1311`;
`app/src/main/java/com/kheyr/sms/onboarding/OnboardingGate.kt:4`

Step 2's Continue button is enabled only when `canUseFullSmsFeatures` is true, which requires SMS *and*
contacts *and* (on Android 13+) notification permission. The only affordance is "Grant permissions", which
re-launches the runtime dialog; after a "Don't allow" with "don't ask again" (or two denials on newer
Android) that dialog no longer appears and the user is stuck with no way to continue or to reach app
settings. Contacts and notifications are treated as optional everywhere else in the app (the inbox,
Contacts tab and New message screen all degrade gracefully), and PRD 11.2.1 acceptance 3 requires
graceful handling of denied permissions.

**Fix:** require only the SMS permissions to continue, and offer an "Open settings" button
(`openAppPermissionSettings()` already exists) when a request comes back denied.

<a id="b-16"></a>
### B-16. No recovery when SMS permission or the default role is lost later (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:809-816`, `:852-857`;
`app/src/main/java/com/kheyr/sms/onboarding/DefaultRoleMonitor.kt`

Once onboarding is complete the app never re-requests anything. If the user later changes the default SMS
app or revokes SMS permission, the inbox shows the text "Grant SMS access to load conversations" with no
button, sending only shows a status message, and `DefaultRoleMonitorState.shouldWarnRoleRemoved` is never
evaluated. PRD 11.2.1 acceptance 4 ("warn user if default SMS role is removed later") is unmet.

**Fix:** show a banner with "Make default" / "Grant permission" actions when `isDefaultSms` or
`smsPermissionGranted` flips to false on resume.

<a id="b-21"></a>
### B-21. SIM badge is never drawn in the thread list (Medium)

`app/src/main/java/com/kheyr/sms/ui/TelegramStyleThreadRow.kt:43`;
`app/src/main/java/com/kheyr/sms/ui/ThreadRowPresentation.kt:16`

`ThreadRowPresentationMapper` resolves `simBadge` and `ThreadFolderScreen` passes it in, but
`TelegramStyleThreadRow` never renders the parameter. Dual-SIM users get no indication which SIM a thread
belongs to (PRD 5.1.6 and 11.2.4.2).

<a id="b-18"></a>
### B-18. "Auto-delete spam after N days" does nothing (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:1628-1634`;
`app/src/main/java/com/kheyr/sms/preferences/AppPreferences.kt:51-53`;
`app/src/main/java/com/kheyr/sms/thread/SpamAutoDeletePolicy.kt`

The Spam protection screen exposes a 0 to 30 day slider that is persisted in `spamAutoDeleteDays`, but
`SpamAutoDeletePolicy` has no callers and no worker ever deletes old spam. The README advertises
"Optional auto-delete for old spam messages".

<a id="b-19"></a>
### B-19. Nothing can block a sender (Medium)

`app/src/main/java/com/kheyr/sms/preferences/AppPreferences.kt:123-129`;
`app/src/main/java/com/kheyr/sms/receiver/IncomingSmsServices.kt:134`

The notifier suppresses alerts from `isBlockedSender()` addresses (PRD 5.5.7), but `setBlockedSender()`
is never called from any screen, dialog or menu, so the set is always empty. The README lists "blocking"
as a feature.

<a id="b-26"></a>
### B-26. Unknown spam rule type throws inside the worker (Low)

`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt:34`, `:236-258`

`fetchSpamRules()` runs `parseSpamRuleSet()` outside the `runCatching` in `executeJson()`; a rule with a
`type` the client does not know (`SpamRuleType.valueOf` throws), a missing `id`, or a non-integer `score`
throws out of `SpamRulesWorker.doWork()`. WorkManager records a failure rather than crashing the app, but
the client can never accept a rule set that contains a single new rule type, and nothing is logged.

---

## Low-severity items

<a id="b-30"></a>
### B-30. Re-syncing an existing message resets the thread row (Low)

`app/src/main/java/com/kheyr/sms/data/SmsDao.kt:235` versus `:30-45`

Commit `74e3fdb` introduced `upsertThreadInitial`, which keeps an existing `displayName` and the earliest
`createdAt`, but the existing-row branch of `upsertTelephonyMessage()` still calls the `REPLACE`
`upsertThread(...)` with `displayName = address` and `createdAt = message.timestamp`, so the fifty most
recent outgoing messages re-synced on every refresh keep wiping those columns. Harmless today only because
nothing else writes `displayName`.

<a id="b-31"></a>
### B-31. Contact cache is never invalidated (Low)

`app/src/main/java/com/kheyr/sms/contacts/ContactRepository.kt:22-27`, `:111-116`

`cachedContactData` is built once per process and only cleared when a permission is granted. A contact
added or renamed while Kheyr is running keeps showing as a bare number until the process is killed. A
`ContentObserver` on `ContactsContract` (or invalidating on resume) would fix it.

<a id="b-32"></a>
### B-32. Thread rows decode the full-size contact photo (Low)

`app/src/main/java/com/kheyr/sms/ui/ContactAvatar.kt:38-48`;
`app/src/main/java/com/kheyr/sms/contacts/ContactRepository.kt:159-176`

`loadPhotoUriByContactId` uses `Contacts.PHOTO_URI` (full resolution) and `ContactAvatar` decodes it with
`BitmapFactory.decodeStream` for every row each time it enters composition, with no downsampling or cache.
Scrolling a long inbox with photo contacts re-decodes large bitmaps repeatedly. Use
`PHOTO_THUMBNAIL_URI` or `inSampleSize`.

<a id="b-33"></a>
### B-33. Notification is posted for the conversation currently on screen (Low)

`app/src/main/java/com/kheyr/sms/receiver/SmsReceiveHandler.kt:24-28`

The receive pipeline has no notion of which thread is open, so a reply arriving while the user is reading
that conversation still produces a heads-up notification, and it stays until the user leaves the chat
(`markThreadRead` only runs on open and on back).

<a id="b-34"></a>
### B-34. Call button shown for alphanumeric senders (Low)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt:1532-1535`;
`app/src/main/java/com/kheyr/sms/ui/ConversationHeaderModel.kt:11`

`ConversationHeaderMapper` computes `callEnabled = address.any(Char::isDigit)` but `ConversationTopBar`
always shows the Call and Search actions, so a "VERIFY" or bank-name thread offers to dial `tel:VERIFY`.

<a id="b-35"></a>
### B-35. Search highlight ranges use lowercase-string indices (Low)

`app/src/main/java/com/kheyr/sms/ui/HighlightedMessageText.kt:85-98`

`highlightRanges` searches in `text.lowercase()` but applies the indices to the original `text`. For
characters whose lowercase form has a different length (e.g. `İ`, some ligatures) the ranges shift and
`substring()` in `segments()` can throw `StringIndexOutOfBoundsException`.

<a id="b-36"></a>
### B-36. `AGENTS.md` no longer matches the code (Low)

`AGENTS.md` (section "Known pre-existing failures")

It says `SmsDaoTest.insertGroupsMessagesByThreadWithLatestPreviewAndUnreadCount` fails because
`inboxThreads()` computes `unreadCount` with a `SUM` over a single-row join; the current query
(`SmsDao.kt:58-70`) already uses a correlated `COUNT(*)` sub-select, and the lint error it cites at
`SmsReceiver.kt:135` refers to a file that is now 24 lines long (the notifier carries
`@SuppressLint("MissingPermission")`). See the appendix for the actual test results.

---

## Not bugs, but worth knowing

- **MMS is silently discarded.** `MmsReceiver` is a no-op and the manifest does not request
  `RECEIVE_MMS`, so as the default SMS app Kheyr swallows every incoming MMS (group texts, pictures) with no
  notification or placeholder. This is by design per PRD 11.2.3 ("No MMS UI"), but users will see it as
  lost messages.
- **Privacy claim on hashed numbers.** `SyncUploader` salts the counterpart number with the device id,
  which the backend also knows. Phone numbers have roughly 10^10 possible values, so the server can trivially
  brute-force every hash; the README's "salted and hashed rather than stored in plain text" is technically
  true but offers little protection.
- **Tokens in plain preferences.** Access and refresh tokens are stored in the unencrypted `kheyr_prefs`
  file and are included in cloud backup (see B-05).
- **Dead API surface.** `registerDevice`, `uploadInitialSync`, `submitSpamFeedback`,
  `createPairingSession`, `sendDirectMessage`, `SmsRepository.loadMessages`, `LogoutPlan`,
  `DefaultRoleMonitorState`, `SpamFolderCounter`, `PhoneLoginRequest` and about twenty
  `id/title/description` placeholder data classes have no callers.
- **Direct messages toggle.** Settings > Privacy shows a "Direct messages" switch although the feature is
  not implemented (README: "not ready yet"). Consider hiding it until it does something.
- **Persian digits.** `JalaliDateFormatter.format` builds the time with `String.format("%02d:%02d")` using
  the default locale, then converts ASCII digits to Persian. In an Arabic locale `%d` already produces
  Arabic-Indic digits, which the converter does not map, so times mix digit sets.

---

## Appendix: how the findings were verified

- **Static reading** of every file under `app/src/main`, the manifest, resources, Room schema and the
  unit tests; usage of suspicious APIs confirmed with `grep` (B-03, B-04, B-18, B-19, B-24).
- **GitHub Actions run #121** (`main`, commit `74e3fdb`): failed in dependency resolution against
  `maven.aliyun.com` with HTTP 502 before compiling (B-02).
- **B-07:** the Kotlin function was ported line-for-line to Python and compared with both the reference
  algorithm and the `jdatetime` library over 2000 to 2099; mismatches occur only on days 61 to 366 of leap
  years.
- **Build and tests** were run in this environment with Gradle 8.14.3, JDK 21 and Android SDK platform 35
  (CI uses JDK 17; the Kotlin errors are the same either way).
