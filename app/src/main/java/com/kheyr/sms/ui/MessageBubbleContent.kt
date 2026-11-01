package com.kheyr.sms.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
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
    val outgoing = row.direction == MessageDirection.Outgoing
    val metaColor = contentColor.copy(alpha = 0.72f)
    val horizontal = if (outgoing) Alignment.End else Alignment.Start

    Column(
        modifier = bubbleModifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = horizontal,
    ) {
        if (emojiStyle) {
            Text(
                row.body,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = contentColor,
                    textDirection = MessageTextDirection.resolve(row.body),
                ),
            )
        } else {
            HighlightedMessageText(
                text = row.body,
                highlight = highlight,
                linkColor = if (outgoing) contentColor else MaterialTheme.colorScheme.primary,
                highlightColor = if (outgoing) {
                    contentColor.copy(alpha = 0.24f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = metaColor,
            )
            if (outgoing) {
                when (row.status) {
                    MessageStatus.Sending -> CircularProgressIndicator(
                        modifier = Modifier.size(9.dp),
                        strokeWidth = 1.2.dp,
                        color = metaColor,
                    )
                    MessageStatus.Sent -> MessageTicks(color = metaColor, double = false)
                    MessageStatus.Delivered -> MessageTicks(color = metaColor, double = true)
                    MessageStatus.Failed -> Text(
                        text = "Not sent",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = contentColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                    )
                    MessageStatus.Received -> Unit
                }
            }
        }
        row.copyableCode?.let { code ->
            Surface(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                shape = RoundedCornerShape(9.dp),
                color = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = "Copy code  ·  $code",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (row.showRetry) {
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
