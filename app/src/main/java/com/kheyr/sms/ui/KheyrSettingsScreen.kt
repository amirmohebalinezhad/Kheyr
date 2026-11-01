package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kheyr.sms.R
import com.kheyr.sms.api.ApiConfig
import com.kheyr.sms.settings.NotificationContentMode
import com.kheyr.sms.settings.NotificationSettings
import com.kheyr.sms.settings.SettingsCategory
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.UnknownSenderNotificationMode
import com.kheyr.sms.telephony.SimCard
import kotlin.math.roundToInt

@Composable
fun SettingsListScreen(onCategoryClick: (SettingsCategory) -> Unit) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val sections = listOf(
        "Notifications" to listOf(SettingsCategory.Notifications, SettingsCategory.UnknownSenders),
        "Privacy & safety" to listOf(SettingsCategory.SpamProtection, SettingsCategory.PrivacySecurity),
        "Messaging" to listOf(SettingsCategory.DualSim),
        "Cloud & sync" to listOf(SettingsCategory.Sync, SettingsCategory.DesktopDevices),
        "Personalization" to listOf(SettingsCategory.Appearance),
        "About" to listOf(SettingsCategory.About),
    )

    LazyColumn(
        contentPadding = PaddingValues(top = topInset + 4.dp, bottom = bottomInset + 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "settings-hero") {
            KheyrCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    KheyrIconTile(icon = KheyrIcons.Shield, tint = MaterialTheme.colorScheme.primary, size = 44.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Private by design",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Your messages stay on this phone unless you turn on encrypted sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        sections.forEach { (title, categories) ->
            item(key = "section:$title") {
                KheyrSectionHeader(title)
            }
            item(key = "card:$title") {
                KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    categories.forEachIndexed { index, category ->
                        if (index > 0) KheyrMenuDivider()
                        KheyrNavRow(
                            icon = settingsIcon(category),
                            title = settingsTitle(category),
                            subtitle = settingsCategoryDescription(category),
                            tint = settingsTint(category),
                            onClick = { onCategoryClick(category) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDetailScreen(
    category: SettingsCategory,
    notificationSettings: NotificationSettings,
    themePreference: ThemePreference,
    syncEnabled: Boolean,
    signedIn: Boolean,
    defaultSub: Int?,
    sims: List<SimCard>,
    directMessagesEnabled: Boolean,
    spamAutoDeleteDays: Int,
    onNotificationChange: (NotificationSettings) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onSyncChange: (Boolean) -> Unit,
    onDefaultSimChange: (Int?) -> Unit,
    onDirectMessagesChange: (Boolean) -> Unit,
    onSpamAutoDeleteChange: (Int) -> Unit,
    onDeleteCloudData: () -> Unit,
    onExportCloudData: () -> Unit,
) {
    val topInset = KheyrChromeInsets.shellTop()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset + 4.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailHeader(category = category)
        when (category) {
            SettingsCategory.Notifications -> {
                SettingsGroup("Message previews") {
                    NotificationContentMode.entries.forEach { mode ->
                        KheyrRadioRow(
                            title = notificationContentLabel(mode),
                            subtitle = notificationContentDescription(mode),
                            selected = notificationSettings.contentMode == mode,
                            onClick = { onNotificationChange(notificationSettings.copy(contentMode = mode)) },
                        )
                    }
                }
                SettingsGroup("Alerts") {
                    KheyrSwitchRow(
                        title = "Vibrate",
                        subtitle = "Vibrate when a message arrives",
                        checked = notificationSettings.vibrate,
                        onCheckedChange = { onNotificationChange(notificationSettings.copy(vibrate = it)) },
                        icon = KheyrIcons.Bell,
                    )
                }
            }
            SettingsCategory.UnknownSenders -> {
                InfoNote(
                    icon = KheyrIcons.Help,
                    text = "Choose how Kheyr behaves when a number outside your contacts sends a message.",
                )
                SettingsGroup("Unknown sender alerts") {
                    UnknownSenderNotificationMode.entries.forEach { mode ->
                        KheyrRadioRow(
                            title = unknownSenderLabel(mode),
                            subtitle = unknownSenderDescription(mode),
                            selected = notificationSettings.unknownSenderMode == mode,
                            onClick = { onNotificationChange(notificationSettings.copy(unknownSenderMode = mode)) },
                        )
                    }
                }
            }
            SettingsCategory.SpamProtection -> {
                InfoNote(
                    icon = KheyrIcons.Shield,
                    text = "Spam rules are downloaded from Kheyr and still protect you offline. Flagged messages move to the Spam folder.",
                )
                SettingsGroup("Cleanup") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Auto-delete spam", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (spamAutoDeleteDays == 0) "Never" else "$spamAutoDeleteDays days",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Slider(
                            value = spamAutoDeleteDays.toFloat(),
                            onValueChange = { onSpamAutoDeleteChange(it.roundToInt()) },
                            valueRange = 0f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                        )
                        Text(
                            text = "Spam older than this is removed automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SettingsCategory.DualSim -> {
                if (sims.isEmpty()) {
                    InfoNote(
                        icon = KheyrIcons.Sim,
                        text = "No SIM cards detected yet. Once a SIM is active you can choose which one sends new messages.",
                    )
                } else {
                    SettingsGroup("Default sending SIM") {
                        sims.forEach { sim ->
                            KheyrRadioRow(
                                title = "SIM ${sim.slotIndex + 1} · ${sim.displayName}",
                                subtitle = if (defaultSub == sim.subscriptionId) "Used for new messages" else "Tap to make default",
                                icon = KheyrIcons.Sim,
                                selected = defaultSub == sim.subscriptionId,
                                onClick = { onDefaultSimChange(sim.subscriptionId) },
                            )
                        }
                    }
                }
            }
            SettingsCategory.Sync -> {
                SettingsGroup("Encrypted backup") {
                    KheyrSwitchRow(
                        title = "Enable sync",
                        subtitle = "Back up messages with end-to-end encryption",
                        checked = syncEnabled && signedIn,
                        onCheckedChange = onSyncChange,
                        enabled = signedIn,
                        icon = KheyrIcons.Cloud,
                    )
                }
                if (!signedIn) {
                    InfoNote(
                        icon = Icons.Default.Warning,
                        text = "Sign in first — there is nowhere to upload your backup until this phone is linked to an account.",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                InfoNote(
                    icon = Icons.Default.Info,
                    text = "Sync endpoint: ${ApiConfig.baseUrl}",
                )
            }
            SettingsCategory.DesktopDevices -> {
                InfoNote(
                    icon = KheyrIcons.Monitor,
                    text = "Pair a desktop from the Chats menu to read and send SMS from your computer. Each paired device can be revoked at any time.",
                )
            }
            SettingsCategory.PrivacySecurity -> {
                SettingsGroup("Conversations") {
                    KheyrSwitchRow(
                        title = "Direct messages",
                        subtitle = "Let Kheyr users message you without a phone number",
                        checked = directMessagesEnabled,
                        onCheckedChange = onDirectMessagesChange,
                        icon = KheyrIcons.Chat,
                    )
                }
                SettingsGroup("Your cloud data") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Export or permanently erase everything Kheyr has stored for this account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onExportCloudData,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(KheyrIcons.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export cloud data")
                        }
                        Button(
                            onClick = onDeleteCloudData,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete cloud data")
                        }
                    }
                }
            }
            SettingsCategory.Appearance -> {
                InfoNote(
                    icon = KheyrIcons.Sun,
                    text = "Pick a theme. System follows your phone's light or dark setting.",
                )
                SettingsGroup("Theme") {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ThemePreference.entries.forEach { preference ->
                            KheyrThemeOption(
                                preference = preference,
                                selected = themePreference == preference,
                                onClick = { onThemeChange(preference) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            SettingsCategory.About -> {
                KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KheyrMark(size = 64.dp)
                        Text(
                            text = "Kheyr",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Version 0.1.0",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "A calmer inbox: modern SMS with spam filtering, encrypted sync, and desktop relay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SettingsGroup("Legal") {
                    KheyrNavRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy policy",
                        subtitle = PRIVACY_POLICY_URL,
                        onClick = {
                            context.safeStartActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(PRIVACY_POLICY_URL),
                            ))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(category: SettingsCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KheyrIconTile(icon = settingsIcon(category), tint = settingsTint(category), size = 52.dp)
        Text(
            text = settingsCategoryDescription(category),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    KheyrSectionHeader(title)
    KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        content()
    }
}

@Composable
private fun InfoNote(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun settingsTitle(category: SettingsCategory): String = when (category) {
    SettingsCategory.Notifications -> "Notifications"
    SettingsCategory.UnknownSenders -> "Unknown senders"
    SettingsCategory.SpamProtection -> "Spam protection"
    SettingsCategory.DualSim -> "Dual SIM"
    SettingsCategory.Sync -> "Encrypted sync"
    SettingsCategory.DesktopDevices -> "Desktop devices"
    SettingsCategory.PrivacySecurity -> "Privacy & security"
    SettingsCategory.Appearance -> "Appearance"
    SettingsCategory.About -> "About Kheyr"
}

internal fun settingsDisplayTitle(category: SettingsCategory): String = settingsTitle(category)

private fun settingsCategoryDescription(category: SettingsCategory): String = when (category) {
    SettingsCategory.Notifications -> "Previews, vibration and alert style"
    SettingsCategory.UnknownSenders -> "Alerts from numbers not in contacts"
    SettingsCategory.SpamProtection -> "Filtering rules and automatic cleanup"
    SettingsCategory.DualSim -> "Choose the SIM that sends messages"
    SettingsCategory.Sync -> "Encrypted backup of your conversations"
    SettingsCategory.DesktopDevices -> "Send and read SMS from your computer"
    SettingsCategory.PrivacySecurity -> "Cloud data and direct messages"
    SettingsCategory.Appearance -> "Light, dark, or follow the system"
    SettingsCategory.About -> "Version and privacy information"
}

private fun settingsIcon(category: SettingsCategory) = when (category) {
    SettingsCategory.Notifications -> KheyrIcons.Bell
    SettingsCategory.UnknownSenders -> KheyrIcons.Help
    SettingsCategory.SpamProtection -> KheyrIcons.Shield
    SettingsCategory.DualSim -> KheyrIcons.Sim
    SettingsCategory.Sync -> KheyrIcons.Cloud
    SettingsCategory.DesktopDevices -> KheyrIcons.Monitor
    SettingsCategory.PrivacySecurity -> Icons.Default.Lock
    SettingsCategory.Appearance -> KheyrIcons.Sun
    SettingsCategory.About -> Icons.Default.Info
}

@Composable
private fun settingsTint(category: SettingsCategory) = when (category) {
    SettingsCategory.Notifications -> KheyrColors.Violet
    SettingsCategory.UnknownSenders -> KheyrColors.Blue
    SettingsCategory.SpamProtection -> KheyrColors.Coral
    SettingsCategory.DualSim -> MaterialTheme.colorScheme.primary
    SettingsCategory.Sync -> MaterialTheme.colorScheme.primary
    SettingsCategory.DesktopDevices -> KheyrColors.Blue
    SettingsCategory.PrivacySecurity -> KheyrColors.Amber
    SettingsCategory.Appearance -> KheyrColors.Amber
    SettingsCategory.About -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun notificationContentLabel(mode: NotificationContentMode): String = when (mode) {
    NotificationContentMode.ShowSenderAndPreview -> "Sender and message"
    NotificationContentMode.ShowSenderOnly -> "Sender only"
    NotificationContentMode.HideSenderAndPreview -> "Hide everything"
}

private fun notificationContentDescription(mode: NotificationContentMode): String = when (mode) {
    NotificationContentMode.ShowSenderAndPreview -> "Show who wrote and what they said"
    NotificationContentMode.ShowSenderOnly -> "Keep the message body private"
    NotificationContentMode.HideSenderAndPreview -> "Only show that a new message arrived"
}

private fun unknownSenderLabel(mode: UnknownSenderNotificationMode): String = when (mode) {
    UnknownSenderNotificationMode.Normal -> "Notify normally"
    UnknownSenderNotificationMode.Silent -> "Silent notification"
    UnknownSenderNotificationMode.None -> "Don't notify"
}

private fun unknownSenderDescription(mode: UnknownSenderNotificationMode): String = when (mode) {
    UnknownSenderNotificationMode.Normal -> "Sound and vibrate like any other message"
    UnknownSenderNotificationMode.Silent -> "Delivered quietly to your inbox"
    UnknownSenderNotificationMode.None -> "Only visible when you open Kheyr"
}

private const val PRIVACY_POLICY_URL = "https://kheyr.app/privacy"
