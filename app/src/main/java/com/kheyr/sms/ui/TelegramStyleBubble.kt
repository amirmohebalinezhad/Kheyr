package com.kheyr.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TelegramStyleBubble(
    isOutgoing: Boolean,
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalKheyrDarkTheme.current
    val bubbleColor = when {
        isOutgoing -> Color.Transparent
        dark -> KheyrColors.IncomingBubbleDark
        else -> KheyrColors.IncomingBubbleLight
    }
    val bubbleContentColor = when {
        isOutgoing -> KheyrColors.OutgoingText
        dark -> KheyrColors.OnIncomingBubbleDark
        else -> KheyrColors.OnIncomingBubbleLight
    }
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    }

    Box(
        modifier = modifier
            .widthIn(max = 314.dp)
            .clip(shape)
            .then(
                if (isOutgoing) {
                    Modifier.background(KheyrGradients.Outgoing)
                } else {
                    Modifier
                        .background(bubbleColor)
                        .border(1.dp, scheme.outlineVariant.copy(alpha = if (dark) 0.5f else 0.55f), shape)
                },
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides bubbleContentColor) {
            content()
        }
    }
}

@Composable
fun ConversationBubbleRow(
    row: ConversationMessageRow,
    darkTheme: Boolean,
    highlight: String?,
    onRetry: () -> Unit,
) {
    val isOutgoing = row.layout.alignment == BubbleAlignment.End
    val alignment = if (isOutgoing) Arrangement.End else Arrangement.Start
    Row(Modifier.fillMaxWidth(), horizontalArrangement = alignment) {
        if (row.layout.showBubble) {
            TelegramStyleBubble(isOutgoing = isOutgoing, darkTheme = darkTheme) {
                MessageBubbleContent(row, highlight, onRetry, emojiStyle = false)
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            ) {
                MessageBubbleContent(row, highlight, onRetry, emojiStyle = true)
            }
        }
    }
}
