# Android app: known bugs

Audit of the `:app` module at commit `74e3fdb` (`main`, 2026-07-07), performed 2026-09-04 by reading the
source and, where possible, verifying with the JVM unit-test toolchain (see the appendix). Line numbers refer
to that commit.

Severity scale: **Critical** = nothing ships / crash loop, **High** = feature does not work or loses data,
**Medium** = wrong behaviour users will notice, **Low** = robustness, polish, dead code.

**Status.** All of these have since been fixed on this branch except B-24, which is deliberately deferred
(applying downloaded sync changes needs decryption, merge semantics and a live backend to validate against).
Three further bugs found while fixing the rest are recorded below as B-38 to B-40, and more that the
fixes themselves introduced — caught by two adversarial reviews of the branch diff — as B-41 to B-45,
and by the pull request's automated reviewer as B-46 to B-49.
B-41 is worth reading even if nothing else here is: the fix for a data-loss bug created a worse one, in
which a message the user sent went out over the air and never appeared in the conversation. Reviewing
the fixes turned out to matter as much as writing them.

Each entry keeps its original description so the reasoning behind the fix stays readable; the **Status**
column above says what was actually done. Fixing the list took the unit suite from a source set that did
not compile to **225 passing tests**, `:app:lintDebug` to zero errors, and `:app:assembleDebug` back to
producing an APK.

## Summary

| ID | Severity | Area | Title | Status |
| --- | --- | --- | --- | --- |
| [B-01](#b-01) | Critical | Build | `SmsDao` declares `syncedTelephonyIdsInRange` four times, so the app does not compile | fixed |
| [B-02](#b-02) | High | CI | CI build depends on the Aliyun Maven mirror and fails when it returns 502 | fixed |
| [B-03](#b-03) | High | Sync | Nothing ever writes to the sync queue, so sync never uploads a message | fixed |
| [B-04](#b-04) | High | Account | Access token is never refreshed; every authenticated feature dies after one hour | fixed |
| [B-05](#b-05) | High | Storage | Cloud backup restore (or Keystore invalidation) turns into a crash loop on launch | fixed |
| [B-06](#b-06) | High | Sync | A failed upload deletes the queued events instead of retrying them | fixed |
| [B-07](#b-07) | Medium | Dates | Jalali dates are one day early for March 1 to December 31 of every Gregorian leap year | fixed |
| [B-08](#b-08) | Medium | Dual SIM | Incoming SMS store the SIM *slot index* in the column the app reads as a *subscription id* | fixed |
| [B-09](#b-09) | Medium | Composer | After a failed send the Send button stays disabled until the chat is reopened | fixed |
| [B-10](#b-10) | Medium | Composer | Retrying a failed message has no error handling and can crash the app | fixed |
| [B-11](#b-11) | Medium | Settings | "Delete cloud data" and "Export cloud data" always fail (network call on the main thread) | fixed |
| [B-12](#b-12) | Medium | Intents | `sms:` / `smsto:` links open the inbox instead of a conversation with the recipient | fixed |
| [B-13](#b-13) | Medium | Delete | Deleting a chat while not the default SMS app brings it back on the next refresh | fixed |
| [B-14](#b-14) | Medium | Delete | "Undo" after deleting a chat restores only the app copy; the system SMS rows are already gone | fixed |
| [B-15](#b-15) | Medium | Onboarding | Onboarding dead-ends when contacts or notification permission is permanently denied | fixed |
| [B-16](#b-16) | Medium | Permissions | No way to recover when SMS permission or the default-SMS role is lost after onboarding | fixed |
| [B-17](#b-17) | Medium | Receive | Incoming SMS are dated with the carrier timestamp, so delayed messages sort into the past | fixed |
| [B-18](#b-18) | Medium | Spam | "Auto-delete spam after N days" setting does nothing | fixed |
| [B-19](#b-19) | Medium | Blocking | Blocked-sender suppression exists but nothing can block a sender | fixed |
| [B-20](#b-20) | Medium | Account | Logging out of the optional sync account forces the whole onboarding again | fixed |
| [B-21](#b-21) | Medium | Dual SIM | SIM badge is computed for thread rows but never drawn | fixed |
| [B-22](#b-22) | Low | Receive | The `SecurityException` guard around telephony sync does not cover the new gap-window query | fixed |
| [B-23](#b-23) | Medium | Spam | Any spam-scored message re-flags the whole thread, overriding the user's "Not spam" | fixed |
| [B-24](#b-24) | Low | Sync | Downloaded sync changes are counted, then thrown away | not fixed |
| [B-25](#b-25) | Low | Sync | Sync cursor is put in the URL without encoding | fixed |
| [B-26](#b-26) | Low | Spam | Unknown rule type from the server throws inside `SpamRulesWorker` | fixed |
| [B-27](#b-27) | Low | Send | `SmsSendStatusReceiver` worker thread has no exception guard | fixed |
| [B-28](#b-28) | Low | Send | Delivery reports are treated as "delivered" without reading the report status | fixed |
| [B-29](#b-29) | Low | Import | Drafts from other SMS apps are imported as outgoing messages with status Received | fixed |
| [B-30](#b-30) | Low | Data | Re-syncing an existing message resets the thread row (`displayName`, `createdAt`) | fixed |
| [B-31](#b-31) | Low | Contacts | Contact name cache is never invalidated while the process lives | fixed |
| [B-32](#b-32) | Low | UI | Thread rows decode the full-size contact photo for every avatar | fixed |
| [B-33](#b-33) | Low | UI | Notification is still posted for the conversation currently on screen | fixed |
| [B-34](#b-34) | Low | UI | Call button is shown for alphanumeric senders (dials `tel:VERIFY`) | fixed |
| [B-35](#b-35) | Low | UI | Search highlighting can index past the end of the string for some Unicode input | fixed |
| [B-36](#b-36) | Low | Docs | `AGENTS.md` describes failures that no longer match the code | fixed |
| [B-37](#b-37) | High | Tests | Unit-test source set does not compile (test for a class deleted in `c924f87`) | fixed |
| [B-38](#b-38) | Low | CI | Job-summary step runs the artifact name as a shell command | fixed |
| [B-39](#b-39) | High | Build | No Gradle heap configured, so dex merging dies with OutOfMemoryError | fixed |
| [B-40](#b-40) | Medium | UI | The per-thread action menu is dead UI: `showThreadMenu` is never assigned | fixed |
| [B-46](#b-46) | High | Sync | Undoing a delete re-uploaded the messages under the ids the delete removed | fixed |
| [B-47](#b-47) | High | Data loss | The startup database recovery raced the first query, deleting the file underneath it | fixed |
| [B-48](#b-48) | Medium | Sync | A refused upload reported success, so WorkManager never backed off and retried | fixed |
| [B-49](#b-49) | Medium | Account | Per-instance refresh lock let two API instances spend the same single-use refresh token | fixed |

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
Confirmed by a Robolectric test with an OkHttp interceptor answering 503: the uploader still calls
`deleteUploaded([1])` (appendix). Today this is masked by B-03; once the queue is actually fed, this
becomes data loss.

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
all of 2028. Confirmed by a unit test against the Kotlin implementation (appendix), and a Python port of
the function cross-checked against the `jdatetime` library disagrees on 918 of the 4,383 days between 2020
and 2031 (306 days in each leap year, none elsewhere). The existing unit test only checks Nowruz 2026, a
non-leap year, so it passes.

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
replies out the wrong SIM. Confirmed by a Robolectric test: persisting an `IncomingSms` with
`simSlot = 1, subscriptionId = 7` stores `1` (appendix).

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
nothing else writes `displayName`. Confirmed by a Room test (appendix): after `upsertThread(..., "Alice", ...)`
a second `upsertTelephonyMessage` of the same telephony id resets `displayName` to the address.

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
`inboxThreads()` computes `unreadCount` with a `SUM` over a single-row join, and that "the other 51 unit
tests pass". The current query (`SmsDao.kt:58-70`) already uses a correlated `COUNT(*)` sub-select, that
test passes, and the suite has 174 tests (once it compiles again, see B-37). The lint error it cites at
`SmsReceiver.kt:135` refers to a file that is now 24 lines long (the notifier carries
`@SuppressLint("MissingPermission")`). See the appendix for the actual test results.

---

## Found while fixing the list

<a id="b-38"></a>
### B-38. The CI job summary executes the artifact name (Low)

`.github/workflows/android-apk.yml` (step "Write install instructions to job summary")

The step wrote its markdown with backticks inside a double-quoted bash string:

```bash
echo "Download the artifact named `kheyr-debug-apk-${{ github.sha }}`, unzip it, ..."
```

Bash performs command substitution inside double quotes, so it tried to *execute*
`kheyr-debug-apk-<sha>` as a command and spliced its (empty) output into the summary. The install
instructions came out mangled on every run.

**Fixed:** emit the summary from a heredoc with the markdown backticks escaped, and pass the artifact
name through an environment variable.

<a id="b-39"></a>
### B-39. No Gradle heap is configured, so dex merging runs out of memory (High)

`gradle.properties`

`gradle.properties` contained only `android.useAndroidX=true`, leaving the build JVM on its default
heap. Once B-01 was fixed and compilation got far enough to matter, `:app:mergeExtDexDebug` failed on
CI with `OutOfMemoryError: Java heap space` inside R8/D8, so the workflow still produced no APK:

```
Execution failed for task ':app:mergeExtDexDebug'.
> com.android.builder.dexing.DexArchiveMergerException: Error while merging dex archives:
Caused by: java.lang.OutOfMemoryError: Java heap space
```

The app's external dependencies (Compose, SignalR with RxJava, Firebase, ZXing) are simply more than
the default heap can merge. This is invisible to `compileDebugKotlin` and to the unit tests, which is
why it only appeared once the build got past the compiler.

**Fixed:** set `org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m` (and a 2 GB Kotlin daemon
heap). GitHub's `ubuntu-latest` runners have 16 GB. Verified by building a 27 MB `app-debug.apk`.

<a id="b-40"></a>
### B-40. The per-thread action menu is unreachable (Medium)

`app/src/main/java/com/kheyr/sms/ui/KheyrAppShell.kt` (`showThreadMenu`, `ThreadActionDialog`)

`ThreadActionDialog` renders Pin, Mark read, Archive, Mark spam, Mute and Delete for a single thread,
and is driven by `showThreadMenu`. Nothing ever assigned that state: `showThreadMenu` was only ever
read and set back to null. The whole dialog was dead code, so the per-thread menu the PRD describes
(11.2.2 "Long-press actions") could not be opened at all. Long-press instead starts multi-select,
whose toolbar covers most of the same actions, which is why the gap was easy to miss.

This also blocked the fix for B-19: adding Block to that dialog alone would have left it just as
unreachable.

**Fixed:** a second long-press on the single selected row opens its menu, which now also carries
Block/Unblock. Worth a second opinion on the exact entry point - the alternative is to move Block
into the existing multi-select overflow and delete the dialog.

---

## Found while reviewing the fixes

Bugs the fixes above introduced, caught by an adversarial review of the branch diff and fixed on it.
They are recorded because they explain why some of the code is shaped the way it is, and because the
first one is a reminder that a fix for a data-loss bug can create a worse one.

<a id="b-41"></a>
### B-41. A tombstone could swallow a newly sent message (Critical)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt`

The B-13 tombstone filter was placed inside the shared `syncTelephonyMessages(...)`, which also serves
`syncTelephonyMessagesByIds(...)` — and that is exactly how a message the user just sent enters Room:
`persistOutgoing()` writes it to the provider and hands back its `_id`, and the caller imports that one
row. The SMS provider does not use `AUTOINCREMENT`, so SQLite reuses the rowid of a deleted message.
Delete the newest thread, send a new message, and it can be assigned an id that is still tombstoned:
the message goes out over the air and never appears in the conversation.

**Fixed:** only the discovery paths (the "newer than" scan and the gap-window backfill) consult
tombstones. An explicit id list is the caller asserting those rows are live, so it bypasses the filter
and clears any stale tombstone on those ids.

<a id="b-42"></a>
### B-42. A thread restored with "Not spam" was silenced forever (High)

`app/src/main/java/com/kheyr/sms/receiver/SmsReceiveHandler.kt`

B-23 gave `autoMarkSpam` the good half of the behaviour — it refuses to re-hide a thread the user
corrected — but `SmsReceiveHandler` still branched on the score alone and returned `SpamSuppressed`.
Every later spam-scoring message from that sender therefore landed in the inbox with no notification.
The correction left the user worse off than before: the thread was visible but silent.

**Fixed:** `persistSpam` reports whether the thread really is spam, and the handler notifies when the
user's correction stands. Covered by a regression test.

<a id="b-43"></a>
### B-43. The undo window reopened the B-13 hole (High)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt`, `.../ui/KheyrAppShell.kt`

Splitting the delete for B-14 left the snackbar window in exactly the state B-13 describes: rows gone
from Room, still present in the provider, and no tombstone in between. A refresh during those seconds —
or simply the next launch, if the process died before the deferred commit ran — re-imported the thread
the user had just deleted. Undo also left other devices believing the thread was gone, because the
queued sync delete events uploaded and the restored rows were never re-queued.

**Fixed:** tombstone at delete time, lift the tombstones on Undo, and re-queue the restored messages.

<a id="b-44"></a>
### B-44. Smaller defects in the same review (Low)

- **Notification not dismissed (B-33's other half).** Suppressing notifications for the conversation on
  screen does nothing about one posted a moment earlier, and since nothing will replace a suppressed
  notification, the stale one simply stayed there. Opening a conversation now cancels it.
- **CDMA delivery reports read with GSM ranges.** `SmsMessage.getStatus()` shifts a 3GPP2 status into
  bits 16-23; read as a GSM TP-Status it looks like a permanent failure, marking the message Failed and
  offering a Retry that would send it twice. Anything outside the single-byte GSM range is now treated
  as "no usable verdict".
- **Duplicate spam sync events.** `autoMarkSpam` enqueued an identical event for every message on an
  already-flagged thread. It now enqueues only on the transition.
- **Tombstone write race.** The read-modify-write is reached from the UI coroutine, the bulk-delete loop
  and the notification-action thread at once, so concurrent deletes dropped each other's ids. Serialised.
- **Database recovery stranded its own DAOs.** Swapping the `AppDatabase` singleton would leave a DAO
  already captured by `SmsRepository` bound to a closed database, crashing the launch the recovery
  exists to heal. It reopens underneath the same instance instead.

<a id="b-45"></a>
### B-45. Second review round: the rest of the diff (High)

The first review reached only the data layer and the receivers before it was cut short. A second pass
covered sync, the shell, the widgets, the build config, the tests and the cross-group seams. The
serious findings were again in the fixes rather than in the original code.

- **A send that threw left the message dead (High).** `persistOutgoing`/`markSending` had already
  stored the row as "sending", but `send()` threw before the telephony stack accepted it, so no
  `PendingIntent` was ever registered and `SmsSendStatusReceiver` could never resolve it. Because
  `showRetry` keys off `MessageStatus.Failed`, the bubble spun on "Sending" forever with no way to
  retry — and the retry path was worse, turning a retryable Failed message into a permanent Sending.
  Both catches now mark the row failed and reload.
- **Backgrounded messages were silently marked read (High).** The mark-read added alongside B-33 keyed
  off `selectedThread`, which survives backgrounding because the composition is not destroyed. A
  message arriving while the user was in another app was marked read in Room *and* in the system store
  before they had seen it. Now gated on `ActiveConversation`, which `ON_STOP` already clears.
- **The backup rules had a seam (High).** They were written one commit before `SmsRepository`
  introduced its tombstone preferences file, so the tombstones were backed up while the database they
  refer to was excluded. On a restored phone the provider reassigns rowids, and every restored SMS
  whose new id collided with a stale tombstone would be silently and permanently hidden. Excluded now,
  along with the multipart send-progress file, which is keyed the same way.
  The invariant is written into both rule files: **state keyed by row ids must be excluded together
  with the database that gave those ids meaning, never independently of it.**
- **B-41 was still open on two paths.** The desktop relay and respond-via-message never imported the
  row they had just written, so a reused rowid could still swallow the message there.
- **Sign-out leaked one account's messages to the next (Medium).** Queued events holding plaintext
  bodies survived sign-out and would upload under whoever signed in next. The queue and cursor are
  cleared now.
- **Disabling sync never re-armed the backfill (Medium).** Everything received while sync was off
  stayed permanently absent from the cloud.
- **The message upload dropped most of its fields (Medium).** Timestamp, direction, status and hashed
  address were all discarded, so a receiving device would render every synced message as "incoming,
  received now".
- **Blocking usually missed (Medium).** The block list stored the raw thread address while the receiver
  checks the carrier's formatting of the same number. Keyed by `PhoneNumberNormalizer.matchKey` now,
  with tests.
- **Smaller ones:** the SIM badge was drawn on single-SIM phones, where it says the same thing on every
  row and steals width from the preview; `sms:` bodies were percent-decoded twice, corrupting any body
  containing an escaped `&` or `%`; and the sync worker stamped "last synced" even on a failed upload.

### Checked and not a bug

- **`type NOT IN (3, 0)` excluding NULL-type provider rows.** Reported as silently dropping messages.
  It does not: `cursor.getInt` returns 0 for a NULL column and `isImportableType(0)` is already false,
  so those rows are skipped identically either way.
- **"The real send path enqueues nothing for sync."** Reported as outgoing messages never syncing.
  `markSent`/`markFailed` enqueue via `enqueueMessageChangeByTelephonyId` moments later, so they do
  reach the queue. The related test criticism stands — `insertOutgoingSms` and `updateSendStatus` have
  no production callers and their test proves nothing about the real path — but the functional claim
  does not.

### Still open from that review (Low)

- **Synced messages always report `isSpam = false`.** `SmsMessageEntity` has no spam column — it is
  thread state — and the private `toModel()` takes the model default. Fixing it means a thread-state
  lookup per enqueued message. Left alone because nothing consumes the field yet (B-24 is not done).
- **Initial backfill queues messages only.** Existing pin/archive/spam/mute state is never uploaded, so
  a user who enables sync after months of curation backfills the messages but none of the organisation.
  It only corrects itself when the user next toggles each thread.

---

## Found in review of the pull request

A third review pass, this one by the automated reviewer on the pull request rather than by me. All four
findings were checked against the code before anything was changed; all four turned out to be real, and
all four are fixed. B-48 sits on top of a defect recorded above as fixed, which is a fair reminder that
patching the symptom a review named is not the same as fixing everything wrong at that line.

<a id="b-46"></a>
### B-46. Undoing a delete re-uploaded the messages under their old ids (High)

`app/src/main/java/com/kheyr/sms/data/SmsRepository.kt`

`restoreThreadMessages` puts the snapshot back with `insertSmsBatch(messages.map { it.copy(id = 0) })`,
so every row returns under a **new** `AUTOINCREMENT` primary key. The sync events, though, were built
from the snapshot objects, which still carried the ids the rows had before the delete:

```kotlin
smsDao.insertSmsBatch(messages.map { it.copy(id = 0) })
enqueueForSync { store -> messages.forEach { store.enqueueMessage(it.toModel()) } }
```

The delete events queued moments earlier name those same old ids. A receiving device therefore applies
"delete message 41" and then "here is message 41 again", and any backend that processes the queue in
order — or that treats a delete as a tombstone — ends up with the thread still gone, which is exactly
the outcome the re-queue exists to prevent. Locally the message is back, so the user sees no sign of it.

`insertSmsBatch` now returns the row id each message actually received and the enqueue loop uses them.
Covered by `SmsRepositorySyncQueueTest.undoingADeleteQueuesTheRestoredRowsUnderTheirNewIds`.

<a id="b-47"></a>
### B-47. The database recovery raced the first query (High)

`app/src/main/java/com/kheyr/sms/KheyrApplication.kt`, `data/AppDatabase.kt`

The B-05 fix runs `ensureOpenOrRecreate` on a detached thread from `onCreate` — it has to, because
key derivation and a rebuild are far too slow to hold up process start. Nothing waited for it. When the
store really is undecryptable, that thread closes the open helper, deletes the database file and reopens
it, and any query issued in that window runs against a file being deleted underneath it. A cold start
triggered by an incoming SMS is the likely case: the receiver and the recovery start at the same moment.
The failure mode is the crash B-05 exists to heal, now caused by the healing.

`AppDatabase` now arms a latch for the duration of the startup check and `getInstance` waits on it, so a
caller is only handed a DAO once the store is known good. The wait is skipped on the main thread (Room
will not run a query there anyway, and stalling it on key derivation would risk an ANR) and bounded at
20 seconds so a wedged check degrades to the old behaviour rather than hanging a worker for good. On a
normal launch the latch is null and nothing waits.

<a id="b-48"></a>
### B-48. A rejected sync upload reported success to WorkManager (Medium)

`app/src/main/java/com/kheyr/sms/worker/SyncWorker.kt`, `sync/SyncUploader.kt`

`uploadPending` returned a plain count, and 0 meant both "the backend refused the upload" and "there was
nothing queued". `doWork` could not tell them apart, so it returned `Result.success()` either way and
WorkManager applied no backoff. A refused upload then waited out a full periodic interval before being
retried, even if the network came back seconds later.

`uploadPending` now returns a `SyncUploadOutcome` carrying `rejected`, and `doWork` maps that to
`Result.retry()`. Only a refused upload retries: an empty queue is a success, and a null download is how
an unconfigured backend reads, which must not put the worker into a permanent retry loop. This is the
one recorded above as "the sync worker stamped 'last synced' even on a failed upload" — the stamping was
fixed then, the retry semantics were not.

<a id="b-49"></a>
### B-49. Two API instances could spend the same single-use refresh token (Medium)

`app/src/main/java/com/kheyr/sms/api/KheyrApiService.kt`

The lock serialising token refreshes was a per-instance field, but the UI (`KheyrAppShell`) and each of
the two workers call `KheyrApiService.create(preferences)` for themselves, over the same persisted token
pair. Two of those instances hitting a 401 at the same time both entered `refreshAccessToken`, both saw
an unchanged access token, and both spent the refresh token. The second spend is rejected, and on a backend that rotates the whole token
family it invalidates the pair the first call had just stored — signing the device out.

The lock is now a companion-object (process-wide) one, so the loser of the race finds the freshly stored
access token in the guard that was already there and replays with it instead of refreshing again. The
guard was written for exactly this race in the B-04 fix; making it instance-scoped is what defeated it.

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
- **Compile:** `gradle :app:compileDebugKotlin` at `74e3fdb` fails with the four `Conflicting overloads`
  errors (B-01).
- **Unit tests:** `gradle :app:testDebugUnitTest` fails to compile the test sources (B-37). On a working
  tree with the three duplicate DAO declarations removed and `SpamClassificationThresholdsTest.kt` set
  aside, the existing suite is **174 tests, all passing**, including
  `SmsDaoTest.insertGroupsMessagesByThreadWithLatestPreviewAndUnreadCount`, which `AGENTS.md` lists as a
  known failure.
- **Throwaway verification tests** (kept out of the repository) were run against that same tree. Each one
  asserts the behaviour the app *should* have, so a failure confirms the bug:

  | Finding | Assertion | Result |
  | --- | --- | --- |
  | B-07 | `gregorianToJalali(2024-03-20)` is 1403/01/01 | fails: 1402/12/29 (2024-03-01 gives 1402/12/10, 2024-12-31 gives 1403/10/10; the non-leap 2025-03-21 case passes) |
  | B-08 | `RoomIncomingSmsStore.persistInbox(IncomingSms(simSlot = 1, subscriptionId = 7))` stores 7 in `simSlot` | fails: stores 1 |
  | B-30 | `upsertTelephonyMessage` of an existing row keeps the thread's `displayName` "Alice" | fails: reset to the address |
  | B-06 | `SyncUploader` with an OkHttp client that answers 503 keeps the queue row | fails: `deleteUploaded([1])` is called anyway |

- **B-07:** the Kotlin function was also ported line-for-line to Python and compared with both the
  reference algorithm and the `jdatetime` library over 2000 to 2099; mismatches occur only on days 61 to
  366 of leap years.
- **Environment:** Gradle 8.14.3, JDK 21, Android SDK platform 35, Robolectric SDK 34 (CI uses JDK 17; the
  Kotlin errors are the same either way).

## Appendix: how the fixes were verified

- **The suite grew from a source set that would not compile to 218 passing tests**, and
  `:app:assembleDebug` packages a 27 MB `app-debug.apk`. Both were run after every group of fixes, not
  only at the end.
- **CI is green**, and now actually runs the tests: the workflow gained a `:app:testDebugUnitTest` step,
  so the class of failure that hid B-37 for months cannot recur silently.
- **B-07** was re-checked the same way it was found: the corrected Kotlin was ported back to Python and
  compared against `jdatetime` for every day from 2000 to 2099. It now matches on all 36,525 days, where
  the original missed 7,650. The four leap-year cases are also pinned in `JalaliDateFormatterTest`.
- **B-23** ships a schema change, so `AppDatabaseMigrationTest` builds a real v2 database from the
  committed `2.json`, runs `MIGRATION_2_3` and lets Room re-validate the result against the v3 entities.
  It also pins the behaviour the column exists for. Worth stating because it is easy to get wrong: the
  migration adds the column with `DEFAULT 0` while a fresh v3 table has no SQL default, and Room only
  compares defaults when the entity declares one.
- **B-05** turned out to need a second pass. The first fix caught `Throwable` and deleted the SMS store on
  any open failure, which would have converted a forgotten migration into silent data loss for every user
  on upgrade. Recovery is now limited to a store that genuinely cannot be decrypted, with
  `AppDatabaseRecoveryTest` covering the missing-migration and full-disk cases that must never delete.
- **Not verified on a device.** There is no emulator here (no KVM), so nothing UI-facing has been run
  against a real screen: the B-16 permission banner, the B-21 SIM badge, the B-19/B-40 long-press entry
  point and the B-12 `sms:` deep link are all reasoned from the code and covered only by unit tests where
  the logic is pure. Those are the first things to exercise by hand on a phone.
