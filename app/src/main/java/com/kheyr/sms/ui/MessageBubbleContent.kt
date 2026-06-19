package com.kheyr.sms.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.util.MessageCopyableSegmentDetector

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleContent(
    row: ConversationMessageRow,
    highlight: String?,
    onRetry: () -> Unit,
    emojiStyle: Boolean,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showCopyMenu by remember { mutableStateOf(false) }
    val copyableSegments = remember(row.body) { MessageCopyableSegmentDetector.findAll(row.body) }
    val bubbleModifier = Modifier
        .widthIn(max = 300.dp)
        .combinedClickable(onClick = {}, onLongClick = { showCopyMenu = true })

    if (showCopyMenu) {
        MessageCopyMenuDialog(
            body = row.body,
            segments = copyableSegments,
            onDismiss = { showCopyMenu = false },
            onCopy = { text ->
                clipboard.setText(AnnotatedString(text))
                showCopyMenu = false
            },
            onCall = { number ->
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                showCopyMenu = false
            },
            onSms = { number ->
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                showCopyMenu = false
            },
        )
    }

    val contentColor = LocalContentColor.current

    Column(bubbleModifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (emojiStyle) {
            Text(
                row.body,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = contentColor,
                    textDirection = MessageTextDirection.resolve(row.body),
                ),
            )
        } else {
            HighlightedMessageText(text = row.body, highlight = highlight)
        }
        Text(
            row.timeLabel,
            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Rtl),
            color = contentColor.copy(alpha = 0.72f),
        )
        row.copyableCode?.let { code ->
            TextButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                contentPadding = PaddingValues(0.dp),
            ) { Text("کپی کد") }
        }
        if (row.showRetry) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("Retry") }
        }
    }
}
