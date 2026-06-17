package com.kheyr.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.domain.ThreadSorter
import com.kheyr.sms.telephony.SimRepository

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
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var isDefaultSms by remember { mutableStateOf(Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) }
    var simCount by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        threads = repository.loadThreads()
        simCount = simRepository.activeSims().size
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultSms = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        permissionLauncher.launch(requiredPermissions())
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0F8B8D))) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Kheyr SMS") }) },
            floatingActionButton = { FloatingActionButton(onClick = {}) { Text("+") } },
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
                        simCount = simRepository.activeSims().size
                    }
                }
                if (simCount > 1) {
                    Text(
                        text = "$simCount active SIMs detected",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                ThreadList(threads = threads)
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
private fun ThreadList(threads: List<SmsThread>) {
    if (threads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Grant SMS access to load conversations")
        }
    } else {
        LazyColumn {
            items(ThreadSorter.inboxThreads(threads)) { thread ->
                ListItem(
                    headlineContent = { Text(thread.displayName.ifBlank { thread.address }) },
                    supportingContent = { Text(thread.lastMessage, maxLines = 1) },
                    trailingContent = { if (thread.unreadCount > 0) Badge { Text(thread.unreadCount.toString()) } },
                    leadingContent = { AssistChip(onClick = {}, label = { Text(thread.displayName.take(1).ifBlank { "?" }) }) },
                )
                HorizontalDivider()
            }
        }
    }
}
