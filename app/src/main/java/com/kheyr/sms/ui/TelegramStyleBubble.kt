package com.kheyr.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TelegramStyleBubble(
    isOutgoing: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bubbleColor = when {
        isOutgoing && darkTheme -> KheyrColors.OutgoingBubbleDark
        isOutgoing -> KheyrColors.OutgoingBubbleLight
        darkTheme -> KheyrColors.IncomingBubbleDark
        else -> KheyrColors.IncomingBubbleLight
    }
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(shape)
            .background(bubbleColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        content()
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
                modifier = Modifier.widthIn(max = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MessageBubbleContent(row, highlight, onRetry, emojiStyle = true)
            }
        }
    }
}
