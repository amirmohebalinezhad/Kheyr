package com.kheyr.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.kheyr.sms.data.MessageDirection
import com.kheyr.sms.data.MessageStatus
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.domain.ThreadSorter
import com.kheyr.sms.telephony.SimCard
import com.kheyr.sms.telephony.SimRepository
import com.kheyr.sms.telephony.SmsSendRequest
import com.kheyr.sms.telephony.SmsSender
import com.kheyr.sms.ui.BubbleAlignment
import com.kheyr.sms.ui.ConversationMessageRow
import com.kheyr.sms.ui.ConversationScreenMapper
import com.kheyr.sms.ui.SmsComposerEvent
import com.kheyr.sms.ui.SmsComposerState
import com.kheyr.sms.ui.SmsComposerStateReducer
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KheyrApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KheyrApp() {
    val context = LocalContext.current
    val repository = remember { SmsRepository(context) }
    val simRepository = remember { SimRepository(context) }
    val sender = remember { SmsSender(context) }
    val screenMapper = remember { ConversationScreenMapper() }
    val composerReducer = remember { SmsComposerStateReducer() }
    val coroutineScope = rememberCoroutineScope()
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var composerState by remember { mutableStateOf(SmsComposerState()) }
    var isDefaultSms by remember { mutableStateOf(Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) }
    var sims by remember { mutableStateOf<List<SimCard>>(emptyList()) }
    fun hasSmsReadAccess(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    fun refreshThreads() {
        if (!hasSmsReadAccess()) return
        coroutineScope.launch {
            val loadedThreads = repository.loadThreads()
            threads = loadedThreads
            selectedThread?.let { current ->
                val threadId = current.id
                val loadedMessages = repository.loadMessages(threadId)
                if (selectedThread?.id == threadId) {
                    messages = loadedMessages
                    selectedThread = loadedThreads.firstOrNull { it.id == threadId } ?: current
                }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results[Manifest.permission.READ_SMS] == true) {
            refreshThreads()
        }
        sims = simRepository.activeSims()
        composerState = composerState.copy(requiresSimSelection = sims.size > 1)
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultSms = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        permissionLauncher.launch(requiredPermissions())
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0F8B8D))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedThread?.displayName?.ifBlank { selectedThread?.address.orEmpty() } ?: "Kheyr SMS") },
                    navigationIcon = {
                        if (selectedThread != null) {
                            TextButton(onClick = {
                                selectedThread = null
                                composerState = SmsComposerState(requiresSimSelection = sims.size > 1)
                                refreshThreads()
                            }) { Text("Back") }
                        }
                    },
                )
            },
            floatingActionButton = { if (selectedThread == null) FloatingActionButton(onClick = {}) { Text("+") } },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (!isDefaultSms) {
                    DefaultSmsCard(onRequestDefault = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        } else {
                            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(
                                Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                context.packageName,
                            )
                        }
                        roleLauncher.launch(intent)
                    })
                } else {
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(requiredPermissions())
                        sims = simRepository.activeSims()
                        composerState = composerState.copy(requiresSimSelection = sims.size > 1)
                    }
                }
                if (selectedThread == null) {
                    if (sims.size > 1) {
                        Text("${sims.size} active SIMs detected", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    ThreadList(threads = threads, onThreadSelected = { thread ->
                        selectedThread = thread
                        messages = emptyList()
                        composerState = SmsComposerState(
                            selectedSubscriptionId = thread.simSlot ?: sims.firstOrNull()?.subscriptionId,
                            requiresSimSelection = sims.size > 1,
                        )
                        coroutineScope.launch {
                            val loadedMessages = repository.loadMessages(thread.id)
                            if (selectedThread?.id == thread.id) {
                                messages = loadedMessages
                            }
                        }
                    })
                } else {
                    val screen = screenMapper.map(selectedThread!!, messages, sims, composerState)
                    ConversationScreen(
                        screen = screen,
                        sims = sims,
                        onBodyChange = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.BodyChanged(it)) },
                        onSimSelected = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.SubscriptionSelected(it)) },
                        onRefresh = { refreshThreads() },
                        onSend = {
                            val state = composerReducer.reduce(composerState, SmsComposerEvent.SendRequested)
                            composerState = state
                            if (state.error != null) return@ConversationScreen
                            val text = state.body.trim()
                            val subscriptionId = state.selectedSubscriptionId
                            coroutineScope.launch {
                                val messageId = repository.persistOutgoing(selectedThread!!.address, text, subscriptionId)
                                repository.markSending(messageId)
                                sender.send(SmsSendRequest(selectedThread!!.address, text, subscriptionId, messageId))
                                composerState = composerReducer.reduce(state, SmsComposerEvent.SendCompleted)
                                refreshThreads()
                            }
                        },
                        onRetry = { messageId ->
                            val message = messages.firstOrNull { it.id == messageId } ?: return@ConversationScreen
                            coroutineScope.launch {
                                repository.markSending(message.id)
                                sender.send(SmsSendRequest(message.address, message.body, message.simSlot, message.id))
                                refreshThreads()
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
private fun DefaultSmsCard(onRequestDefault: () -> Unit) {
    ElevatedCard(Modifier.padding(16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Default SMS access required", fontWeight = FontWeight.Bold)
            Text("Kheyr needs to be your default SMS app to read existing threads, receive new messages, send replies, and suppress spam notifications.")
            Button(onClick = onRequestDefault) { Text("Make default SMS app") }
        }
    }
}

@Composable
private fun ThreadList(threads: List<SmsThread>, onThreadSelected: (SmsThread) -> Unit) {
    val inboxThreads = remember(threads) { ThreadSorter.inboxThreads(threads) }
    if (inboxThreads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Grant SMS access to load conversations") }
    } else {
        LazyColumn {
            items(inboxThreads, key = { it.id }, contentType = { "thread" }) { thread ->
                ListItem(
                    headlineContent = { Text(thread.displayName.ifBlank { thread.address }) },
                    supportingContent = { Text(thread.lastMessage, maxLines = 1) },
                    trailingContent = { if (thread.unreadCount > 0) Badge { Text(thread.unreadCount.toString()) } },
                    leadingContent = { AssistChip(onClick = {}, label = { Text(thread.displayName.take(1).ifBlank { "?" }) }) },
                    modifier = Modifier.fillMaxWidth().clickable { onThreadSelected(thread) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    overlineContent = { Text(formatMessageTime(thread.lastMessageAt)) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ConversationScreen(
    screen: com.kheyr.sms.ui.ConversationScreenModel,
    sims: List<SimCard>,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSend: () -> Unit,
    onRetry: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(screen.messages.size) {
        if (screen.messages.isNotEmpty()) listState.animateScrollToItem(screen.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(screen.header.title, style = MaterialTheme.typography.titleMedium)
            screen.header.subtitle?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(screen.messages, key = { it.id }, contentType = { "message" }) { row ->
                ConversationMessageRowView(row = row, onRetry = { onRetry(row.id) })
            }
        }
        MessageComposer(
            body = screen.composer.body,
            sims = sims,
            selectedSubscriptionId = screen.composer.selectedSubscriptionId,
            sending = screen.composer.sending,
            error = screen.composer.error?.name,
            onBodyChange = onBodyChange,
            onSimSelected = onSimSelected,
            onRefresh = onRefresh,
            onSend = onSend,
        )
    }
}

@Composable
private fun ConversationMessageRowView(row: ConversationMessageRow, onRetry: () -> Unit) {
    val alignment = if (row.layout.alignment == BubbleAlignment.End) Arrangement.End else Arrangement.Start
    Row(Modifier.fillMaxWidth(), horizontalArrangement = alignment) {
        if (row.layout.showBubble) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (row.layout.alignment == BubbleAlignment.End) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                MessageContent(row = row, onRetry = onRetry)
            }
        } else {
            MessageContent(row = row, onRetry = onRetry, emojiStyle = true)
        }
    }
}

@Composable
private fun MessageContent(row: ConversationMessageRow, onRetry: () -> Unit, emojiStyle: Boolean = false) {
    Column(Modifier.padding(12.dp).widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.body, fontSize = if (emojiStyle) 28.sp else MaterialTheme.typography.bodyLarge.fontSize, textAlign = if (emojiStyle) TextAlign.Center else TextAlign.Start)
        Text(row.status.statusLabel, style = MaterialTheme.typography.labelSmall)
        if (row.status.showRetry) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text(row.status.retryLabel) }
        }
    }
}

@Composable
private fun MessageComposer(
    body: String,
    sims: List<SimCard>,
    selectedSubscriptionId: Int?,
    sending: Boolean,
    error: String?,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sims.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sims.forEach { sim ->
                        FilterChip(
                            selected = selectedSubscriptionId == sim.subscriptionId,
                            onClick = { onSimSelected(sim.subscriptionId) },
                            label = { Text(sim.displayName) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = body, onValueChange = onBodyChange, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, minLines = 1, maxLines = 5)
                Button(onClick = onSend, enabled = body.isNotBlank() && !sending) { Text(if (sending) "Sending" else "Send") }
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private fun formatMessageTime(instant: java.time.Instant): String = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(instant)
