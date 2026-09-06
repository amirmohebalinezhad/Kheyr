package com.kheyr.sms.api

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The device only ever sees what SpamRulesController.Latest projects, so this pins the exact shape
 * that endpoint emits - including the explicit nulls System.Text.Json writes for absent members,
 * which optString() would otherwise hand back as the string "null".
 */
@RunWith(RobolectricTestRunner::class)
class SpamRuleWireFormatTest {
    private val service = KheyrApiService()

    @Test fun categoryAndCategoryScoreReachTheDevice() {
        val parsed = service.parseSpamRuleSet(
            JSONObject(
                """
                {"version": 2, "threshold": 70, "rules": [
                  {"id": "fa-otp", "type": "message_regex", "pattern": "کد تایید", "score": -70,
                   "enabled": true, "category": "otp", "category_score": 70}
                ], "created_at": 1}
                """.trimIndent(),
            ),
        )

        val rule = parsed?.rules?.single()
        assertEquals("otp", rule?.category)
        assertEquals(70, rule?.categoryScore)
        assertEquals(70, rule?.categoryWeight)
    }

    @Test fun anExplicitJsonNullCategoryParsesAsNoCategory() {
        val parsed = service.parseSpamRuleSet(
            JSONObject(
                """
                {"version": 2, "threshold": 70, "rules": [
                  {"id": "url", "type": "message_regex", "pattern": "https", "score": 35,
                   "enabled": true, "category": null, "category_score": null}
                ]}
                """.trimIndent(),
            ),
        )

        val rule = parsed?.rules?.single()
        assertNull("a null category must not become the string 'null'", rule?.category)
        assertEquals(0, rule?.categoryScore)
        // Falls back to the magnitude of the spam score rather than to zero.
        assertEquals(35, rule?.categoryWeight)
    }

    @Test fun aRuleSetPublishedBeforeTheCategoryAxisStillParses() {
        val parsed = service.parseSpamRuleSet(
            JSONObject("""{"version": 1, "threshold": 70, "rules": [{"id": "url", "type": "message_regex", "pattern": "https", "score": 35, "enabled": true}]}"""),
        )

        assertEquals(1, parsed?.rules?.size)
        assertNull(parsed?.rules?.single()?.category)
    }

    @Test fun anUnknownRuleTypeIsDroppedRatherThanFailingTheWholeSet() {
        val parsed = service.parseSpamRuleSet(
            JSONObject("""{"version": 3, "threshold": 70, "rules": [{"id": "future", "type": "sender_reputation", "score": 10}, {"id": "url", "type": "message_regex", "pattern": "https", "score": 35}]}"""),
        )

        assertEquals(listOf("url"), parsed?.rules?.map { it.id })
    }
}
