package com.kheyr.sms.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.telephony.SimCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadFolderScreen(
    threads: List<SmsThread>,
    folder: ThreadFolder,
    showFilters: Boolean,
    threadListFilter: ThreadListFilter,
    onThreadListFilterChange: (ThreadListFilter) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sims: List<SimCard>,
    mapper: ThreadRowPresentationMapper,
    loading: Boolean,
    selectedThreadIds: Set<Long>,
    onThreadClick: (SmsThread) -> Unit,
    onThreadLongPress: (SmsThread) -> Unit,
    onCompose: () -> Unit,
    listState: LazyListState,
    searchFocusRequester: androidx.compose.ui.focus.FocusRequester,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    showFab: Boolean = true,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    banner: @Composable () -> Unit = {},
) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val sections = remember(threads, folder) { ThreadSectionBuilder.build(threads, folder) }
    val selectionMode = selectedThreadIds.isNotEmpty()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(topInset))
            banner()
            KheyrSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search messages",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                focusRequester = searchFocusRequester,
            )
            if (showFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThreadListFilter.entries.forEach { filter ->
                        KheyrFilterChip(
                            label = threadListFilterLabel(filter),
                            selected = threadListFilter == filter,
                            onClick = { onThreadListFilterChange(filter) },
                        )
                    }
                }
            }
            when {
                loading && threads.isEmpty() -> KheyrSkeletonList(
                    modifier = Modifier.padding(top = 10.dp),
                )
                threads.isEmpty() -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    KheyrEmptyState(
                        icon = if (!showFilters && folder == ThreadFolder.Spam) KheyrIcons.Shield else KheyrIcons.Chat,
                        title = emptyTitle,
                        message = emptyMessage,
                        actionLabel = emptyActionLabel,
                        onAction = onEmptyAction,
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = bottomInset + 84.dp),
                ) {
                    sections.forEach { section ->
                        stickyHeader(key = "section:${section.title}") {
                            SectionLabel(section.title)
                        }
                        items(section.threads, key = { it.id }) { thread ->
                            val row = remember(thread, folder, sims) { mapper.map(thread, folder, sims) }
                            val timeLabel = remember(thread.lastMessageAt) { formatMessageTime(thread.lastMessageAt) }
                            TelegramStyleThreadRow(
                                title = row.title,
                                preview = row.preview,
                                timeLabel = timeLabel,
                                photoUri = thread.contactPhotoUri,
                                unreadBadge = row.unreadBadge,
                                showPinned = row.showPinned,
                                showMuted = row.showMuted,
                                simBadge = row.simBadge,
                                showSpamBadge = row.showSpamBadge,
                                onClick = { onThreadClick(thread) },
                                onLongClick = { onThreadLongPress(thread) },
                                selected = thread.id in selectedThreadIds,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        if (!selectionMode && folder != ThreadFolder.Archived) {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn(tween(150)) + scaleIn(tween(170), initialScale = 0.82f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.82f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = bottomInset + 14.dp),
            ) {
                KheyrComposeFab(onClick = onCompose)
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun KheyrComposeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = Color.White,
        modifier = modifier
            .size(58.dp)
            .shadow(elevation = 16.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KheyrGradients.Brand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = KheyrIcons.Compose,
                contentDescription = "New message",
                modifier = Modifier.size(23.dp),
                tint = Color.White,
            )
        }
    }
}
