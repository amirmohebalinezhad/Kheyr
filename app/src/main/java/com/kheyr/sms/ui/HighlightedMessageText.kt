package com.kheyr.sms.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun HighlightedMessageText(
    text: String,
    highlight: String?,
    modifier: Modifier = Modifier,
) {
    if (highlight.isNullOrBlank()) {
        Text(text, modifier = modifier)
        return
    }
    val lowerText = text.lowercase()
    val lowerHighlight = highlight.lowercase()
    val annotated = buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = lowerText.indexOf(lowerHighlight, start)
            if (index < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))) {
                append(text.substring(index, index + highlight.length))
            }
            start = index + highlight.length
        }
    }
    Text(annotated, modifier = modifier)
}
