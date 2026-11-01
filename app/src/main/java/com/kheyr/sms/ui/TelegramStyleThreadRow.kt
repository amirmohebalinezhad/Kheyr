package com.kheyr.sms.ui

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramStyleThreadRow(
    title: String,
    preview: String,
    timeLabel: String,
    photoUri: Uri?,
    unreadBadge: String?,
    showPinned: Boolean,
    showMuted: Boolean,
    simBadge: String?,
    showSpamBadge: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val unread = unreadBadge != null
    val background by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.09f) else Color.Transparent,
        label = "threadRowBackground",
    )

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 20.dp, end = 20.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ContactAvatar(
                    displayName = title,
                    photoUri = photoUri,
                    size = 52.dp,
                )
                if (selected) {
                    Surface(shape = CircleShape, color = scheme.primary) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp).padding(3.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showPinned) {
                        Icon(
                            imageVector = KheyrIcons.Pin,
                            contentDescription = "Pinned",
                            tint = KheyrColors.Amber,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (showMuted) {
                        Icon(
                            imageVector = KheyrIcons.Mute,
                            contentDescription = "Muted",
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (showSpamBadge) {
                        SpamPill()
                    }
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = if (unread) scheme.onSurface.copy(alpha = 0.82f) else scheme.onSurfaceVariant,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    simBadge?.let { badge -> SimBadgePill(label = badge) }
                    unreadBadge?.let { badge -> UnreadCountPill(count = badge) }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp, end = 20.dp),
            thickness = 0.6.dp,
            color = scheme.outlineVariant.copy(alpha = if (selected) 0f else 0.45f),
        )
    }
}

@Composable
private fun SpamPill() {
    val dark = LocalKheyrDarkTheme.current
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (dark) KheyrColors.Amber.copy(alpha = 0.18f) else KheyrColors.AmberSoft,
        contentColor = if (dark) KheyrColors.Amber.copy(alpha = 0.95f) else KheyrColors.Amber,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(10.dp))
            Text(
                text = "Spam",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SimBadgePill(label: String) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun UnreadCountPill(count: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = count,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramStyleContactRow(
    displayName: String,
    phoneNumber: String,
    photoUri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ContactAvatar(
                displayName = displayName,
                photoUri = photoUri,
                size = 46.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = CircleShape,
                color = scheme.primary.copy(alpha = 0.10f),
                contentColor = scheme.primary,
                modifier = Modifier.size(36.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        KheyrIcons.Chat,
                        contentDescription = "Message",
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp, end = 20.dp),
            thickness = 0.6.dp,
            color = scheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}
