package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.telephony.SimCard
import kotlinx.coroutines.launch

@Composable
fun ConversationScreenContent(
    screen: ConversationScreenModel,
    sims: List<SimCard>,
    darkTheme: Boolean,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchingIds: List<Long>,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int) -> Unit,
    onSend: () -> Unit,
    onRetry: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val visibleMessages = remember(searchActive, searchQuery, matchingIds, screen.messages) {
        if (searchActive && searchQuery.isNotBlank()) screen.messages.filter { it.id in matchingIds } else screen.messages
    }
    val timeline = remember(visibleMessages) { ConversationTimeline.build(visibleMessages) }
    val highlightQuery = if (searchActive && searchQuery.isNotBlank()) searchQuery else null
    val topInset = KheyrChromeInsets.shellTop()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    LaunchedEffect(timeline.lastOrNull()?.let { (it as? ConversationTimelineItem.Message)?.row?.id }, searchActive) {
        if (!searchActive && timeline.isNotEmpty()) {
            listState.scrollToItem(timeline.lastIndex)
        }
    }

    LaunchedEffect(searchActive, searchQuery, matchingIds) {
        if (searchActive && searchQuery.isNotBlank() && matchingIds.isNotEmpty()) {
            val index = timeline.indexOfFirst { item ->
                item is ConversationTimelineItem.Message && item.row.id == matchingIds.first()
            }
            if (index >= 0) listState.scrollToItem(index)
        }
    }

    KheyrScreenBackground {
        Column(Modifier.fillMaxSize()) {
            if (searchActive) {
                KheyrSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Search in conversation",
                    modifier = Modifier.padding(top = topInset, start = 12.dp, end = 12.dp, bottom = 8.dp),
                    focusRequester = searchFocusRequester,
                )
            }
            Box(Modifier.weight(1f)) {
                if (timeline.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        KheyrEmptyState(
                            icon = KheyrIcons.Chat,
                            title = if (searchActive && searchQuery.isNotBlank()) "No matches" else "Start the conversation",
                            message = if (searchActive && searchQuery.isNotBlank()) {
                                "No messages contain “$searchQuery”."
                            } else {
                                "Messages with ${screen.header.title} will appear here, private to this phone."
                            },
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = if (searchActive) 6.dp else topInset + 4.dp,
                            bottom = 10.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        items(
                            items = timeline,
                            key = { item ->
                                when (item) {
                                    is ConversationTimelineItem.Day -> "day:${item.label}"
                                    is ConversationTimelineItem.Message -> "msg:${item.row.id}"
                                }
                            },
                        ) { item ->
                            when (item) {
                                is ConversationTimelineItem.Day -> DaySeparator(item.label)
                                is ConversationTimelineItem.Message -> ConversationBubbleRow(
                                    row = item.row,
                                    darkTheme = darkTheme,
                                    highlight = if (item.row.id in matchingIds) highlightQuery else null,
                                    onRetry = { onRetry(item.row.id) },
                                )
                            }
                        }
                    }
                }
                if (canScrollForward && !searchActive) {
                    Surface(
                        onClick = {
                            scope.launch {
                                if (timeline.isNotEmpty()) listState.animateScrollToItem(timeline.lastIndex)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 12.dp),
                    ) {
                        Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(
                                imageVector = KheyrIcons.ArrowDown,
                                contentDescription = "Scroll to latest",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            TelegramStyleComposer(
                body = screen.composer.body,
                sims = sims,
                selectedSubscriptionId = screen.composer.selectedSubscriptionId,
                sending = screen.composer.sending,
                error = screen.composer.error?.name,
                onBodyChange = onBodyChange,
                onSimSelected = onSimSelected,
                onSend = onSend,
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.3.sp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ConversationTopBar(
    thread: SmsThread,
    headerSubtitle: String?,
    callEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onSearchToggle: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = thread.displayName.ifBlank { thread.address }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KheyrIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onNavigateBack,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(6.dp))
        ContactAvatar(
            displayName = title,
            photoUri = thread.contactPhotoUri,
            size = 40.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = headerSubtitle?.let { "SMS · $it" } ?: "SMS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(4.dp))
        KheyrIconButton(
            icon = Icons.Default.Search,
            contentDescription = "Search in conversation",
            onClick = onSearchToggle,
        )
        if (callEnabled) {
            Spacer(Modifier.width(2.dp))
            KheyrIconButton(
                icon = Icons.Default.Call,
                contentDescription = "Call",
                onClick = onCall,
            )
        }
    }
}
