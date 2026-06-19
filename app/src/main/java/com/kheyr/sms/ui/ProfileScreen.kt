package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kheyr.sms.settings.HelpFeedbackModel
import com.kheyr.sms.settings.ThemePreference

@Composable
fun ProfileScreen(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onHelpClick: () -> Unit,
    phoneNumber: String?,
    signedIn: Boolean,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val help = HelpFeedbackModel(helpUrl = "https://kheyr.app/help", supportEmail = "support@kheyr.app")
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset, bottom = bottomInset)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Kheyr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("SMS Client", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsSection(title = "Account") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (signedIn) phoneNumber ?: "Signed in" else "Not signed in",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (signedIn) {
                    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
                    Button(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete account") }
                }
            }
        }

        SettingsSection(title = "Appearance") {
            ThemePreference.entries.forEach { pref ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(pref.name)
                    RadioButton(
                        selected = themePreference == pref,
                        onClick = { onThemeChange(pref) },
                    )
                }
            }
        }

        SettingsSection(title = "Support") {
            SettingsRow(title = "Help & Feedback", subtitle = help.supportEmail, onClick = onHelpClick)
        }

        SettingsSection(title = "About") {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Kheyr SMS v0.1.0", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Modern SMS with spam filtering, sync, and desktop relay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and all synced cloud data. SMS already on this phone are not removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAccount()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
