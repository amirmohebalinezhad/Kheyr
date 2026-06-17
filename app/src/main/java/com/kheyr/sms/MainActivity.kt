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
import androidx.compose.ui.unit.dp
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
    val coroutineScope = rememberCoroutineScope()
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
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
                            TextButton(onClick = { selectedThread = null; refreshThreads() }) { Text("Back") }
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
                    }
                }
                if (selectedThread == null) {
                    if (sims.size > 1) {
                        Text("${sims.size} active SIMs detected", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    ThreadList(threads = threads, onThreadSelected = { thread ->
                        selectedThread = thread
                        messages = emptyList()
                        coroutineScope.launch {
                            val loadedMessages = repository.loadMessages(thread.id)
                            if (selectedThread?.id == thread.id) {
                                messages = loadedMessages
                            }
                        }
                    })
                } else {
                    ConversationScreen(
                        thread = selectedThread!!,
                        messages = messages,
                        sims = sims,
                        onRefresh = { refreshThreads() },
                        onSend = { recipient, body, subscriptionId ->
                            coroutineScope.launch {
                                val messageId = repository.persistOutgoing(recipient, body, subscriptionId)
                                repository.markSending(messageId)
                                sender.send(SmsSendRequest(recipient, body, subscriptionId, messageId))
                                refreshThreads()
                            }
                        },
                        onRetry = { message ->
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
    thread: SmsThread,
    messages: List<SmsMessage>,
    sims: List<SimCard>,
    onRefresh: () -> Unit,
    onSend: (String, String, Int?) -> Unit,
    onRetry: (SmsMessage) -> Unit,
) {
    var body by remember(thread.id) { mutableStateOf("") }
    var selectedSubscriptionId by remember(thread.id, sims) { mutableStateOf(thread.simSlot ?: sims.firstOrNull()?.subscriptionId) }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }, contentType = { "message" }) { message ->
                MessageBubble(message = message, onRetry = { onRetry(message) })
            }
        }
        MessageComposer(
            recipient = thread.address,
            body = body,
            sims = sims,
            selectedSubscriptionId = selectedSubscriptionId,
            onBodyChange = { body = it },
            onSimSelected = { selectedSubscriptionId = it },
            onRefresh = onRefresh,
            onSend = {
                val text = body.trim()
                if (text.isNotEmpty()) {
                    onSend(thread.address, text, selectedSubscriptionId)
                    body = ""
                }
            },
        )
    }
}

@Composable
private fun MessageBubble(message: SmsMessage, onRetry: () -> Unit) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start) {
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp).widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.body)
                Text("${formatMessageTime(message.timestamp)} • ${message.status.name}", style = MaterialTheme.typography.labelSmall)
                if (message.status == MessageStatus.Failed) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    recipient: String,
    body: String,
    sims: List<SimCard>,
    selectedSubscriptionId: Int?,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int?) -> Unit,
    onRefresh: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("To: $recipient", style = MaterialTheme.typography.labelLarge)
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
                Button(onClick = onSend, enabled = body.isNotBlank()) { Text("Send") }
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }
    }
}

private fun formatMessageTime(instant: java.time.Instant): String = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(instant)
