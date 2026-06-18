package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier,
) {
    val help = HelpFeedbackModel(helpUrl = "https://kheyr.app/help", supportEmail = "support@kheyr.app")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Kheyr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("SMS Client", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
