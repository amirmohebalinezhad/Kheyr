# Message Classification & Sender Categories — Plan

Status: proposal
Basis: analysis of 33,077 received SMS on a single device, classified from raw message text only — no existing Kheyr spam tags were used as input.

> **Privacy note.** This document reports aggregate measurements only. No message bodies, sender numbers, or personal identifiers are reproduced here, and none are embedded in the rule pack.

---

## 1. What the data says

### 1.1 `لغو11` is not a spam signal

Iranian regulation forces every registered bulk line to append an opt-out footer, so the footer marks *infrastructure*, not *intent*.

| Measure | Value |
|---|---|
| Messages containing `لغو11` | 6,443 (19.5% of received) |
| ...that are actually promotional | 53.2% |
| ...that are transactional only (OTP, receipts, service alerts) | 6.0% |
| ...neither (informational/service) | 40.9% |
| Promotional messages **without** `لغو11` | 35.6% of all promos |

As a standalone rule that is ~53% precision / ~64% recall. It ships only as `fa-laghv-prior` (+5) — too weak to classify anything alone, but enough to carry a message that already matches one structural rule over the line.

The false positives it would cause on its own are exactly the messages that must never be hidden: one-time passwords, vendor invoices, and automated infrastructure alerts all carry the same footer, because the business sending them buys the same bulk line.

### 1.2 The sender is not the unit of classification

Of 347 senders with ≥10 messages, **79 (23%) are mixed** — between 15% and 85% promotional. Blocking or trusting the line is wrong in both directions.

| Sender (anonymized) | Msgs | Ads | Protected | Character |
|---|---|---|---|---|
| Bank, alphanumeric ID | 4,632 | 0 | 4,101 | pure transactional |
| Corporate food-ordering line | 963 | 210 | 674 | delivery notices **and** weekly promos |
| Large e-commerce brand | 241 | 12 | 142 | mostly transactional |
| Food-delivery brand | 140 | 63 | 52 | genuinely half and half |
| Mobile operator | 144 | 23 | 15 | mixed |
| Grocery-supplier ad line | 213 | 193 | 3 | pure ad line |

Both extremes exist, so the design must be: **per-message classification, with the sender as a prior — never as the verdict.**

Note where the mixing concentrates: the bank line is 0% promotional, while the mixed lines are all commercial senders. This matters for the filtering design in §5.3.

### 1.3 Ads that carry no discount vocabulary

A large class of Iranian SMS ads has no `تخفیف`, no percentage, and no promo code — just an offer, a phone number or link, and the opt-out footer. Typical shape (synthetic example, not from the corpus):

```
✈️ تور <مقصد> / ☎ ۰۲۱xxxxxxx / example.ir / لغو11
```

Keyword rules score these near zero. What identifies them is *structural*: a bulk line, **no reference to anything of the user's** (no order, no amount, no OTP), plus a contact channel. That composite is the highest-yield rule in the pack — it fires on 4,493 messages and is the deciding factor for 2,038.

---

## 2. Proposed model: two independent axes

The current engine collapses everything into one spam score. That cannot express "this is a bank message" or "this is an ad but not spam". Split it:

| Axis | Question | Consumes | Outcome |
|---|---|---|---|
| **Spam score** | Is this harmful/unwanted? | existing `score` | Normal / Suspicious / Spam → Spam folder + notification suppression |
| **Category** | What kind of message is this? | new `category` + `categoryScore` | بانکی / تبلیغات / تخفیف / سفارش / رمز / دولتی / شخصی / custom → filtering |

A bank SMS is `category = financial` with **high** confidence and a **strongly negative** spam score. The two must not be the same number.

### 2.1 Category precedence

1. User assignment on the sender (custom category) — always wins
2. User assignment on the thread
3. Highest `categoryScore` among matched rules
4. Sender is a contact → `personal`
5. Fallback → `other`

Every rule in the pack carries an explicit non-zero `CategoryScore` (currently `|Score|`), so step 3 always resolves deterministically. A rule that omits it must be treated as `|Score|`, never as 0 — otherwise multi-category messages fall through to an arbitrary rule-order choice.

### 2.2 Mapping onto existing tiers

`SpamClassification.Suspicious` currently has no production consumer (`SmsReceiveHandler.kt:20` acts only on `Spam`). Give it one:

- **≥ 70 → Spam**: subscription bait, scams. Spam folder, no notification.
- **40–69 → Suspicious**: advertising. **Stays in the inbox**, categorized, eligible for silent notification.
- **< 40 → Normal**: inbox as today.

Ads must not set `isSpam` — that would hide a delivery thread that also carries promos, which §1.2 shows is the common case.

---

## 3. Rule pack

`backend/spam-rules-fa-v2.json` — 21 `message_regex` rules.

**Protective (negative spam score):** `fa-otp` −70, `fa-official` −70, `fa-money-txn` −55, `fa-acct-event` −50, `fa-service-alert` −50, `fa-order-track` −45, `fa-appointment` −45, `fa-card-acct` −35.

**Offers:** `fa-promo-code` +35, `fa-pct-off` +25.

**Advertising:** `fa-bulk-ad-composite` +35, `fa-promo-word` +30, `fa-digit-cta` +30, `fa-urgency` +20, `fa-gift-prize` +18, `fa-shortlink` +18, `fa-cta-arrow` +15, `fa-price-list` +15, `fa-phone-cta` +15, `fa-laghv-prior` +5.

**Scam:** `fa-vas-bait` +45 — the recurring "your subscription lapsed, text a number to rejoin" template, observed from 129 distinct sender numbers over ~3 years.

### 3.1 Publishing format

The file is a **bare JSON array with PascalCase keys**, because that is what the publishing surface accepts: `SpamRules.razor:101` calls `JsonSerializer.Deserialize<List<SpamRuleDto>>` on the textarea contents with default (case-sensitive) options, and `SpamRuleDto` is `(Id, Type, Pattern, Score, Enabled)`. An object wrapping `version`/`threshold`/`rules`, or lowercase keys, would not bind.

`version` and `threshold` (70) are **separate fields on the publish form**, not part of the file.

`Category` and `CategoryScore` are additional keys. `System.Text.Json` ignores unmapped members by default, so the file publishes cleanly today — but those two keys are **silently dropped** until the backend carries them (§4.5). Detection works without them; categorization does not.

### 3.2 Two rules worth explaining

**`fa-promo-code` — code shape separates ads from OTPs perfectly.** An alphanumeric code (`کد: SUMMER25`) is a promo; a purely numeric code (`کد تایید: 123456`) is an OTP. Measured across 33k messages: 987 alphanumeric-code messages, 96.7% promotional; 1,217 numeric-OTP messages, 99.8% non-promotional; **zero overlap**.

**`fa-bulk-ad-composite` — expressed as one regex via lookaheads**, so it needs no new rule type:

```
(?is)^(?=[\s\S]*لغو\s*=?\s*1)                          # bulk line
      (?![\s\S]*(?:کد\s*تایید|مانده|واریز|سفارش\s*شما|مرسوله|ابلاغ))   # nothing of the user's referenced
      (?=[\s\S]*(?:https?://|[a-z0-9\-]{2,}\.(?:ir|com)|☎|📞|\b0(?:21|9\d{2})\d{7,8}\b))  # a contact channel
      [\s\S]*$
```

At +35 it cannot classify alone by design; paired with `fa-laghv-prior` it reaches exactly 40. 358 messages in the corpus match that pair and nothing else — terse ads with no offer vocabulary at all, which is precisely the class §1.3 describes.

**Deliberately no owner-name term.** An earlier draft excluded messages that addressed the recipient by name, on the theory that personalized messages are transactional. Measurement disproved it: bulk marketing personalizes heavily, and the term suppressed ~200 genuine ads on a single line. Removing it raised detection with no loss of OTP safety — and keeps personal data out of a globally distributed rule set. If an owner-name signal is ever wanted, it must be supplied at runtime by the client, never baked into a published rule.

### 3.3 Measured behaviour

| Band | Messages | Share |
|---|---|---|
| Spam ≥70 | 3,316 | 10.0% |
| Advertising 40–69 | 2,425 | 7.3% |
| Grey 20–39 | 1,130 | 3.4% |
| Normal 0–19 | 12,951 | 39.2% |
| Protected <0 | 13,255 | 40.1% |

Total flagged at ≥40: **5,741 (17.4%)**. Of those, 2,319 are driven by an offer rule and would land in `تخفیف` rather than `تبلیغات`.

**Safety — the metric that matters most:** of 1,949 messages matching the OTP rule, **zero** score ≥40. No OTP-, bank-, or government-looking message reaches the advertising band.

These figures are produced by exactly the 21 rules in the file — no feature is used in measurement that the pack does not ship.

An earlier OTP regex required the code to follow the keyword immediately, so it broke whenever words intervened (`کد ورود به <سرویس>: ۵۱۷۰۷`). Allowing up to three intervening tokens found 732 additional OTPs — a silent, high-cost gap worth a regression test.

### 3.4 Known residue

~840 bulk-line messages still score under 20 — mostly webinar invitations and event announcements carrying no offer vocabulary. Better left to user feedback than over-fitted now.

Emoji density (3+ emoji) tested as a useful ad signal but is **not shipped**: expressing it portably across the Java and .NET regex dialects was not worth the divergence risk, and the measurements above exclude it.

---

## 4. Required code changes

### 4.1 Persian normalization before matching (blocking, small)

The rules were tuned against normalized text; the app matches raw PDU text. Without normalization the pack loses **526 detections (9.2%)**.

Add `util/PersianTextNormalizer.kt`: map `ي→ی`, `ك→ک`, strip ZWNJ, and fold Persian/Arabic-Indic digits `۰-۹`/`٠-٩` → `0-9`. Apply once per message in `SpamScorer.score()`, before any rule runs.

Do **not** apply it to the sender string — sender matching already has `PhoneNumberNormalizer.matchKey`.

### 4.2 Client rule schema + scorer

- `SpamRule`: add `category: String? = null`, `categoryScore: Int = 0` (`domain/SpamRule.kt:17`).
- `SpamScore`: add `categoryScores: Map<String, Int>` and `category: String?` (`domain/SpamRule.kt:31`).
- `SpamScorer.score()`: accumulate both axes in the existing single pass (`domain/SpamScorer.kt:15-20`); treat a missing `categoryScore` as `|score|` (§2.1).
- `KnownSafeSender` short-circuits and returns before any other rule runs (`SpamScorer.kt:7-12`) — it must still resolve a **category**, or safe senders lose categorization entirely.
- Wire format: extend `parseSpamRule` (`api/KheyrApiService.kt:305`) for the two new keys.
- Cache: `AppPreferences.saveSpamRuleSet` (`:110-119`) uses a `|`-joined, `Uri.encode`d format. `Uri.encode` percent-encodes `|`, so regex alternation survives the round-trip — **verified, no change needed for the current pack** — but adding two fields means the `parts.size != 5` guard in `decodeRule` (`:161`) must become 7, and previously cached strings must be dropped wholesale rather than silently discarded rule-by-rule.
- Hoist the scorer: `SmsReceiveHandler.kt:19` constructs `SpamScorer` per message, discarding `regexCache` every time. With 21 regexes — several with whole-body lookaheads — this now runs on every incoming SMS. Build it once per rule-set version.

### 4.3 Persist the verdict

`RoomIncomingSmsStore.persistSpam` accepts `score` and `triggeredRuleIds` and **discards both** (`receiver/IncomingSmsServices.kt:53-59`). Categories are per-message, so the thread-level `SyncSpamMetadataEntity` is not sufficient.

Add to `messages` (`data/SmsEntities.kt:17`):

```kotlin
val categoryId: String? = null,
val spamScore: Int = 0,
```

### 4.4 Migration

`AppDatabase` is at **version 3** with **no destructive fallback** (`data/AppDatabase.kt:100-105`) — a missing migration crashes at launch. Ship `MIGRATION_3_4`, bump to 4, and commit `app/schemas/com.kheyr.sms.data.AppDatabase/4.json`.

```sql
ALTER TABLE messages ADD COLUMN categoryId TEXT;
ALTER TABLE messages ADD COLUMN spamScore INTEGER NOT NULL DEFAULT 0;
CREATE TABLE categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL,
  isBuiltIn INTEGER NOT NULL DEFAULT 0, colorKey TEXT, sortOrder INTEGER NOT NULL DEFAULT 0,
  createdAt INTEGER NOT NULL, deletedAt INTEGER);
CREATE TABLE sender_category (senderKey TEXT NOT NULL PRIMARY KEY, categoryId TEXT NOT NULL,
  source TEXT NOT NULL, assignedAt INTEGER NOT NULL);
```

`sender_category.senderKey` must be `PhoneNumberNormalizer.matchKey(address)`, matching how blocked senders are keyed (`AppPreferences.kt:139-148`) — thread addresses and PDU addresses differ in formatting far more often than not, and an exact string match quietly fails.

### 4.5 Backend must carry the category fields

The category axis dies server-side unless all three of these change together — the client work in §4.2 is useless without them:

1. **DTO** — `SpamRuleDto` (`Kheyr.Infrastructure/Services/CoreServices.cs:232`) is `(Id, Type, Pattern, Score, Enabled)`. Add `Category` and `CategoryScore`.
2. **Persistence** — `SpamRuleService.PublishAsync` stores `RulesJson`; confirm the new members round-trip through both `PublishAsync` and `ParseRules`.
3. **API projection** — `SpamRulesController.Latest` (`Kheyr.Api/Controllers/ApiControllers.cs:167`) explicitly projects five fields:

   ```csharp
   rules = rules.Select(r => new { id = r.Id, type = r.Type, pattern = r.Pattern, score = r.Score, enabled = r.Enabled })
   ```

   Anything not listed here never reaches the device, no matter what was published.

Until this lands, the pack still ships useful **detection** (§3.3 numbers hold, since they depend only on `Score`), but every message resolves to no category.

---

## 5. Category filtering feature

### 5.1 Today

`ThreadListFilter { All, Unread, Contacts }` (`ui/ThreadListFilter.kt:5-15`), rendered as chips at `KheyrAppShell.kt:1655-1670`, shown **only in the All folder** (`:914 showFilters = chatFolder == ChatFolder.All`). `Contacts` is a display-name heuristic, not a contacts lookup.

### 5.2 Built-in categories

| id | Label | Driven by | Surface |
|---|---|---|---|
| `financial` | بانکی و مالی | `fa-money-txn`, `fa-card-acct` | thread filter |
| `otp` | رمز و کد تأیید | `fa-otp` | thread filter |
| `delivery` | سفارش و ارسال | `fa-order-track` | thread filter |
| `official` | دولتی و رسمی | `fa-official` | thread filter |
| `personal` | شخصی | sender is a contact | thread filter |
| `advertising` | تبلیغات | promo rules without a usable offer | **message list** |
| `offers` | تخفیف و کد تخفیف | `fa-promo-code`, `fa-pct-off` | **message list** |

`تبلیغات` vs `تخفیف` is a deliberate split: ads you want gone, versus discount codes with an expiry that you may actually want to browse.

### 5.3 Why commercial categories are message-level

A thread-level filter cannot express "hide this sender's ads but keep its delivery notices" — the thread is one row. Filtering by the latest message's category fails both ways: if the newest message is an ad, hiding the row also hides the delivery notices behind it; if it is a delivery notice, the older ads stay represented by that row.

The data says where this matters. Mixing is concentrated in commercial senders (§1.2): the bank line is 0% promotional, while food-delivery and e-commerce lines are the mixed ones. So:

- **`advertising` and `offers` are message-level views** — flat, cross-thread lists of matching messages, newest first, with the promo code surfaced for `offers`. This is the only shape that satisfies "see the ads without losing the receipts".
- **The remaining categories stay thread-level filters**, because their senders are effectively unmixed, and a thread row is the right unit for them.

Suppressing ad messages *inside* an otherwise-transactional thread is a rendering concern (a collapsed "N promotional messages" separator), deferred to Phase 5.

### 5.4 Custom categories

- Create/rename/delete from Settings, and inline from the assignment sheet.
- Assign via long-press → "انتقال فرستنده به دسته…" in `ThreadActionDialog` (`KheyrAppShell.kt:2240-2295`, an 8th action after Block).
- Assignment is keyed on the sender, applies retroactively to that sender's threads and to future messages, and overrides rule-derived categories (§2.1).
- Bulk assignment from the existing multi-select app bar (`:2388-2399`, `thread/ThreadBulkAction.kt`).

Custom categories are sender-scoped, so they are thread-level by nature. A user who assigns a mixed commercial sender to a custom category should be warned that its promotional messages still appear in the `تبلیغات` view.

### 5.5 UI changes

- Extend `ThreadListFilter` from an enum to a sealed type: `All | Unread | Contacts | Category(id)`.
- Lift `showFilters` so chips render in every folder, not just All.
- Make the chip row horizontally scrollable with a trailing `+` opening category management.
- Selecting `تبلیغات` or `تخفیف` switches the list to the message-level view rather than filtering thread rows (§5.3).
- Switch the `Contacts` filter to the real `AndroidContactLookup` already used by the receiver (`SmsReceiveHandlerFactory.kt:12-28`) instead of the display-name heuristic.
- Add a category badge to the thread row via `ThreadRowPresentation` (`ui/ThreadRowPresentation.kt:25-33`).

**Two structural cautions:** the folder predicate is currently written three times — SQL (`SmsDao.kt:70/84/98/290`), Kotlin (`domain/ThreadSorter.kt:7`), and optimistic-update Kotlin (`ui/ThreadListOptimisticUpdate.kt:28-33`) — and `ChatFolder`/`ThreadFolder` must move in lockstep through `toThreadFolder()`. Categories should therefore be a **filter/view over threads**, not a fourth folder, specifically to avoid a fourth copy of that predicate.

### 5.6 Sync

Add `category_changed` to the queue event types (`sync/RoomSyncQueueStore.kt:47-68`) carrying `{thread_id, sender_key, category_id, source}`. Custom category definitions sync as user data; rule-derived categories are recomputed per device and are not synced. Category names are user content and must be encrypted like message bodies, not sent as plaintext metadata.

---

## 6. Phasing

| Phase | Scope | Ships | Acceptance |
|---|---|---|---|
| **1** | Normalization (§4.1) + rule pack + hoist scorer | rules as data + one small util | On a 33k-message corpus: ≥17% flagged ≥40; **zero** OTP/bank/official ≥40 |
| **2** | Backend category fields (§4.5) + client schema (§4.2) | API carries `category`/`categoryScore` | A published rule's category reaches the device |
| **3** | Persist `categoryId`/`spamScore`, `MIGRATION_3_4`, `4.json` | schema v4 | Fresh install and 3→4 upgrade both open; category populated on new messages |
| **4** | Thread-level category chips, chips in all folders, real contact lookup | UI | Selecting `بانکی` lists only financial threads |
| **5** | Message-level `تبلیغات` / `تخفیف` views | UI | For a mixed sender, the ads appear in `تبلیغات` while its delivery notices stay in the inbox thread |
| **6** | Custom categories, sender assignment, bulk assign, sync event | UI + sync | Assignment overrides rules and survives restart |
| **7** | In-thread ad collapsing, auto-expire old ads, feedback loop into scoring | polish | Mark-not-spam adjusts future categorization for that sender |

Phase 1 is independently valuable and carries no migration or backend risk — a rule-set publish plus one utility class.

---

## 7. Risks

1. **Migration is unforgiving.** No `fallbackToDestructiveMigration` (`AppDatabase.kt:100-105`); a missing or wrong `MIGRATION_3_4` crashes every existing install at launch. Test the 3→4 upgrade against a populated encrypted database, not just a fresh one.
2. **Regex cost on the receive path.** 21 regexes, several with whole-body lookaheads, running on `goAsync()` with a raw `Thread` (`SmsReceiver.kt:9-23`). Hoisting the scorer (§4.2) is required, not optional.
3. **Rules are tuned to one inbox**, skewed toward fintech, e-commerce and infrastructure alerts. Treat the weights as a starting point and let the existing feedback plumbing (`domain/SpamFeedback.kt`) tune them — that surface is currently unit-tested but never invoked from production.
4. **Over-categorizing is worse than under-categorizing.** Protective rules should stay aggressive. A missed ad costs one swipe; a hidden OTP or tax notice costs real money.
5. **Cache format is fragile.** `decodeRule` silently drops any rule that fails to parse (`AppPreferences.kt:159-171`), so a format change can partially degrade the rule set with no error surfaced. Version the cache and invalidate wholesale.
6. **Never embed personal data in a rule.** Rule packs are published globally; §3.2 documents one term removed for this reason. Any recipient-specific signal must be substituted at runtime by the client.
7. **Regex dialect divergence.** Patterns are authored once and executed by Kotlin/Java on device. Anything relying on `\p{...}` classes or `\x{...}` escapes should be validated on device before shipping; §3.4 records one signal dropped for this reason.
