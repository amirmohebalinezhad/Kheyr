package com.kheyr.sms.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.BuildConfig
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
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset, bottom = bottomInset + 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProfileHero(
            phoneNumber = phoneNumber,
            signedIn = signedIn,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        KheyrSectionHeader("Account")
        KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            KheyrNavRow(
                icon = KheyrIcons.Profile,
                title = if (signedIn) phoneNumber ?: "Signed in" else "Not signed in",
                subtitle = if (signedIn) {
                    "Encrypted sync and desktop relay are available"
                } else {
                    "Your messages stay on this phone"
                },
                onClick = {},
                showChevron = false,
            )
            if (signedIn) {
                KheyrMenuDivider()
                KheyrNavRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Log out",
                    subtitle = "Stop syncing this phone",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onLogout,
                )
                KheyrMenuDivider()
                KheyrNavRow(
                    icon = Icons.Default.Delete,
                    title = "Delete account",
                    subtitle = "Erase this account and its cloud data",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteConfirm = true },
                )
            }
        }

        KheyrSectionHeader("Appearance")
        KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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

        KheyrSectionHeader("Support")
        KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            KheyrNavRow(
                icon = KheyrIcons.Help,
                title = "Help & feedback",
                subtitle = "Guides, tips, and a direct line to us",
                onClick = onHelpClick,
            )
            KheyrMenuDivider()
            KheyrNavRow(
                icon = KheyrIcons.Shield,
                title = "Privacy policy",
                subtitle = "How Kheyr handles your data",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kheyr.app/privacy")))
                    }
                },
            )
        }

        KheyrSectionHeader("About")
        KheyrCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KheyrMark(size = 46.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Kheyr SMS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} · Made for calm conversations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = "Messages are encrypted on this device before they ever leave it.",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes your account and all synced cloud data. SMS already on this phone are not removed. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfileHero(
    phoneNumber: String?,
    signedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(KheyrGradients.BrandDeep)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x4D6FE7CF), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.1f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.92f, size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33C88A2E), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.95f),
                        radius = size.width * 0.5f,
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.05f, size.height * 0.95f),
                )
            }
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (signedIn) AvatarInitials.from(phoneNumber ?: "K") else "K",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Kheyr account",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = if (signedIn) phoneNumber ?: "Signed in" else "Not signed in",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (signedIn) "Your inbox follows you across devices" else "Everything stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroChip(
                    icon = KheyrIcons.Shield,
                    label = if (signedIn) "Encrypted sync" else "Private by design",
                )
                HeroChip(
                    icon = Icons.Default.Lock,
                    label = "On-device keys",
                )
            }
        }
    }
}

@Composable
private fun HeroChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.10f),
        contentColor = Color.White.copy(alpha = 0.85f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp))
        }
    }
}
