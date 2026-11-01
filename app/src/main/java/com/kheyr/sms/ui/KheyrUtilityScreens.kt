package com.kheyr.sms.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.kheyr.sms.api.ApiConfig
import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.api.RemoteDevice
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.desktop.PairingQrCodec
import com.kheyr.sms.desktop.PairingQrResult
import com.kheyr.sms.desktop.PairingQrValidator
import com.kheyr.sms.settings.HelpFeedbackModel
import com.kheyr.sms.thread.ThreadBulkAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopSyncScreen(api: KheyrApiService, onStatus: (String) -> Unit) {
    val topInset = KheyrChromeInsets.shellTop()
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<RemoteDevice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun refresh() {
        if (!ApiConfig.isConfigured) return
        loading = true
        scope.launch(Dispatchers.IO) {
            val result = api.listDevices().filter { it.isDesktop }
            withContext(Dispatchers.Main) {
                devices = result
                loading = false
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw == null) {
            onStatus("Pairing cancelled")
            return@rememberLauncherForActivityResult
        }
        when (val parsed = PairingQrValidator.validate(PairingQrCodec.parse(raw), System.currentTimeMillis() / 1000)) {
            is PairingQrResult.Invalid -> onStatus(parsed.reason)
            is PairingQrResult.Valid -> scope.launch(Dispatchers.IO) {
                val ok = api.approvePairing(parsed.sessionId, "Desktop") != null
                withContext(Dispatchers.Main) {
                    onStatus(if (ok) "Desktop paired" else "Pairing failed — check connection")
                    if (ok) refresh()
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset + 8.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(KheyrGradients.BrandDeep)
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(KheyrIcons.Monitor, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text(
                    text = "Your phone, on your desktop",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Scan the QR code shown on the Kheyr desktop app to read and send SMS from your computer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    onClick = {
                        if (!ApiConfig.isConfigured) {
                            onStatus("Configure the backend URL first")
                        } else {
                            scanLauncher.launch(
                                ScanOptions()
                                    .setOrientationLocked(false)
                                    .setBeepEnabled(false)
                                    .setPrompt("Scan the desktop pairing QR"),
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    contentColor = Color(0xFF04231F),
                    modifier = Modifier.height(48.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(KheyrIcons.Monitor, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Scan pairing QR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        KheyrSectionHeader("Paired desktops")
        when {
            !ApiConfig.isConfigured -> InfoCard(
                text = "Configure the backend URL to pair and manage desktop devices.",
            )
            loading && devices.isEmpty() -> KheyrCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    KheyrLoadingDots()
                    Spacer(Modifier.width(10.dp))
                    Text("Looking for paired devices…", style = MaterialTheme.typography.bodySmall)
                }
            }
            devices.isEmpty() -> KheyrCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KheyrIconTile(icon = KheyrIcons.Monitor, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("No desktops paired yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Pair one to mirror conversations on a bigger screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> KheyrCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                devices.forEachIndexed { index, device ->
                    if (index > 0) KheyrMenuDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KheyrIconTile(icon = KheyrIcons.Monitor)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                device.name.ifBlank { "Desktop" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                device.platform,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val ok = api.revokeDevice(device.id)
                                    withContext(Dispatchers.Main) {
                                        onStatus(if (ok) "Device revoked" else "Revoke failed")
                                        if (ok) refresh()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Revoke", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        InfoCard(
            text = "A paired desktop can read and send SMS until you revoke it. Revoking takes effect immediately.",
        )
    }
}

@Composable
fun HelpScreen() {
    val topInset = KheyrChromeInsets.shellTop()
    val context = LocalContext.current
    val help = HelpFeedbackModel(helpUrl = "https://kheyr.app/help", supportEmail = "support@kheyr.app")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset + 8.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KheyrCard(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KheyrIconTile(icon = KheyrIcons.Help, size = 46.dp)
                Text(
                    "How can we help?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Find a guide, ask a question, or tell us what would make Kheyr better.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        KheyrSectionHeader("Get in touch")
        KheyrCard(modifier = Modifier.fillMaxWidth()) {
            KheyrNavRow(
                icon = KheyrIcons.Help,
                title = "Help center",
                subtitle = help.helpUrl.removePrefix("https://"),
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(help.helpUrl))) } },
            )
            KheyrMenuDivider()
            KheyrNavRow(
                icon = KheyrIcons.Chat,
                title = "Email support",
                subtitle = help.supportEmail,
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${help.supportEmail}"))) } },
            )
        }
        InfoCard(text = "Kheyr is built by a small team. We read every message and usually reply within one working day.")
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadActionDialog(
    thread: SmsThread,
    blocked: Boolean,
    onDismiss: () -> Unit,
    onToggleBlock: () -> Unit,
    onAction: (ThreadBulkAction) -> Unit,
    onPin: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = thread.displayName.ifBlank { thread.address }
    val subtitle = if (title == thread.address) "Not in contacts" else thread.address

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContactAvatar(displayName = title, photoUri = thread.contactPhotoUri, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            KheyrCard(modifier = Modifier.fillMaxWidth()) {
                ThreadActionItem(
                    icon = KheyrIcons.Pin,
                    label = if (thread.isPinned) "Unpin conversation" else "Pin conversation",
                    tint = KheyrColors.Amber,
                    onClick = onPin,
                )
                KheyrMenuDivider()
                ThreadActionItem(
                    icon = Icons.Default.Check,
                    label = "Mark as read",
                    onClick = { onAction(ThreadBulkAction.MarkRead) },
                )
                KheyrMenuDivider()
                ThreadActionItem(
                    icon = KheyrIcons.Archive,
                    label = if (thread.isArchived) "Unarchive" else "Archive",
                    onClick = { onAction(ThreadBulkAction.Archive) },
                )
                KheyrMenuDivider()
                ThreadActionItem(
                    icon = Icons.Default.Warning,
                    label = if (thread.isSpam) "Not spam" else "Mark as spam",
                    tint = KheyrColors.Coral,
                    onClick = { onAction(ThreadBulkAction.MarkSpam) },
                )
                KheyrMenuDivider()
                ThreadActionItem(
                    icon = KheyrIcons.Mute,
                    label = if (thread.isMuted) "Unmute" else "Mute notifications",
                    tint = KheyrColors.Violet,
                    onClick = { onAction(ThreadBulkAction.Mute) },
                )
                KheyrMenuDivider()
                ThreadActionItem(
                    icon = KheyrIcons.Block,
                    label = if (blocked) "Unblock sender" else "Block sender",
                    onClick = onToggleBlock,
                )
            }
            KheyrCard(modifier = Modifier.fillMaxWidth(), borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) {
                ThreadActionItem(
                    icon = Icons.Default.Delete,
                    label = "Delete conversation",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onAction(ThreadBulkAction.Delete) },
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ThreadActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KheyrIconTile(icon = icon, tint = tint, size = 38.dp)
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
