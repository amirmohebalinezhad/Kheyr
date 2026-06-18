package com.kheyr.sms.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.kheyr.sms.util.MessageLink
import com.kheyr.sms.util.MessageLinkDetector

@Composable
fun HighlightedMessageText(
    text: String,
    highlight: String?,
    modifier: Modifier = Modifier,
) {
    val links = MessageLinkDetector.findAll(text)
    val highlightRanges = highlightRanges(text, highlight)
    val textDirection = MessageTextDirection.resolve(text)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = textDirection)

    if (links.isEmpty() && highlightRanges.isEmpty()) {
        Text(text, modifier = modifier, style = textStyle)
        return
    }

    val annotated = buildAnnotatedString {
        segments(text, links, highlightRanges).forEach { segment ->
            val content = text.substring(segment.start, segment.end)
            if (segment.linkUrl != null) {
                pushLink(
                    LinkAnnotation.Url(
                        url = segment.linkUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ),
                )
            }
            if (segment.highlighted) {
                withStyle(SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))) {
                    append(content)
                }
            } else {
                append(content)
            }
            if (segment.linkUrl != null) {
                pop()
            }
        }
    }
    Text(annotated, modifier = modifier, style = textStyle)
}

private data class TextSegment(
    val start: Int,
    val end: Int,
    val linkUrl: String?,
    val highlighted: Boolean,
)

private fun segments(text: String, links: List<MessageLink>, highlightRanges: List<IntRange>): List<TextSegment> {
    val breakpoints = sortedSetOf(0, text.length)
    links.forEach { breakpoints += it.start; breakpoints += it.end }
    highlightRanges.forEach { breakpoints += it.first; breakpoints += it.last + 1 }
    val points = breakpoints.toList()
    return (0 until points.lastIndex).mapNotNull { index ->
        val start = points[index]
        val end = points[index + 1]
        if (start >= end) return@mapNotNull null
        val link = links.firstOrNull { it.start <= start && it.end >= end }
        val highlighted = highlightRanges.any { it.first <= start && it.last + 1 >= end }
        TextSegment(start, end, link?.url, highlighted)
    }
}

private fun highlightRanges(text: String, highlight: String?): List<IntRange> {
    if (highlight.isNullOrBlank()) return emptyList()
    val lowerText = text.lowercase()
    val lowerHighlight = highlight.lowercase()
    val ranges = mutableListOf<IntRange>()
    var start = 0
    while (start < text.length) {
        val index = lowerText.indexOf(lowerHighlight, start)
        if (index < 0) break
        ranges += index until index + highlight.length
        start = index + highlight.length
    }
    return ranges
}
