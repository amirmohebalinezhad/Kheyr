package com.kheyr.sms.util

data class MessageLink(
    val url: String,
    val start: Int,
    val end: Int,
)

object MessageLinkDetector {
    private val urlPattern = Regex("""(?i)(https?://[^\s<>"']+|www\.[^\s<>"']+)""")
    private val trailingPunctuation = Regex("""[.,;:!?)}\]"']+$""")

    fun findAll(text: String): List<MessageLink> = urlPattern.findAll(text).mapNotNull { match ->
        val trimmed = match.value.replace(trailingPunctuation, "")
        if (trimmed.isEmpty()) return@mapNotNull null
        val url = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
        MessageLink(url = url, start = match.range.first, end = match.range.first + trimmed.length)
    }.toList()
}
