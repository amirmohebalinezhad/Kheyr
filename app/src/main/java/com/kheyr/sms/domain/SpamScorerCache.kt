package com.kheyr.sms.domain

/**
 * Process-wide memo of the [SpamScorer] built for a rule set.
 *
 * The receiver builds a fresh [com.kheyr.sms.receiver.SmsReceiveHandler] per broadcast, so caching
 * inside the handler would never hit. Compiling the pack's regexes - several of which scan the whole
 * body through lookaheads - on every incoming SMS is work the receive path cannot afford, and it
 * runs on a raw thread under `goAsync()`.
 *
 * Keyed on the rule set by value rather than by version alone, so republishing a version in place
 * still takes effect. Comparing 21 short strings is far cheaper than recompiling 21 regexes.
 */
object SpamScorerCache {
    private class Entry(val ruleSet: SpamRuleSet, val scorer: SpamScorer)

    @Volatile private var entry: Entry? = null

    fun scorerFor(ruleSet: SpamRuleSet): SpamScorer {
        val cached = entry
        if (cached != null && cached.ruleSet == ruleSet) return cached.scorer
        // Two messages can arrive at once; the worst case is building the scorer twice, never
        // handing out one that does not match the rule set the caller asked for.
        synchronized(this) {
            val current = entry
            if (current != null && current.ruleSet == ruleSet) return current.scorer
            val scorer = SpamScorer(ruleSet)
            entry = Entry(ruleSet, scorer)
            return scorer
        }
    }
}
