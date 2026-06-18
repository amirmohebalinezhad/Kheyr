package com.kheyr.sms.util

data class MessageLink(
    val url: String,
    val start: Int,
    val end: Int,
)

object MessageLinkDetector {
    private val urlPattern = Regex("""(?i)(https?://[^\s<>"']+|www\.[^\s<>"']+)""")
    // e.g. example.com/path — host chars before / are [a-z0-9.\-], TLD is 2–6 letters
    private val barePathPattern = Regex("""(?i)([a-z0-9][a-z0-9.\-]*\.[a-z]{2,6}/[^\s<>"']+)""")
    private val trailingPunctuation = Regex("""[.,;:!?)}\]"']+$""")

    fun findAll(text: String): List<MessageLink> {
        val links = mutableListOf<MessageLink>()
        urlPattern.findAll(text).forEach { match ->
            toLink(match.value, match.range.first)?.let { links += it }
        }
        barePathPattern.findAll(text).forEach { match ->
            if (links.none { overlaps(it, match.range.first, match.range.last + 1) }) {
                toLink(match.value, match.range.first, prefixHttps = true)?.let { links += it }
            }
        }
        return links.sortedBy { it.start }
    }

    private fun toLink(raw: String, start: Int, prefixHttps: Boolean = false): MessageLink? {
        val trimmed = raw.replace(trailingPunctuation, "")
        if (trimmed.isEmpty()) return null
        val url = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
            prefixHttps -> "https://$trimmed"
            else -> trimmed
        }
        return MessageLink(url = url, start = start, end = start + trimmed.length)
    }

    private fun overlaps(link: MessageLink, start: Int, end: Int): Boolean =
        start < link.end && end > link.start
}
