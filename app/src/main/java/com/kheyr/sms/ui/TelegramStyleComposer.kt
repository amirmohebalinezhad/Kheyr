package com.kheyr.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.kheyr.sms.telephony.SimCard

@Composable
fun TelegramStyleComposer(
    body: String,
    sims: List<SimCard>,
    selectedSubscriptionId: Int?,
    sending: Boolean,
    error: String?,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val appLayoutDirection = LocalLayoutDirection.current
            val draftLayoutDirection = MessageTextDirection.resolveLayoutDirection(body, appLayoutDirection)
            val draftTextAlign = if (draftLayoutDirection == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SimSelectorButton(
                        sims = sims,
                        selectedSubscriptionId = selectedSubscriptionId,
                        onSimSelected = onSimSelected,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalLayoutDirection provides draftLayoutDirection) {
                            TextField(
                                value = body,
                                onValueChange = onBodyChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Message") },
                                minLines = 1,
                                maxLines = 5,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = draftTextAlign),
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                    IconButton(
                        onClick = onSend,
                        enabled = body.isNotBlank() && !sending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(KheyrColors.Primary, CircleShape),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = KheyrColors.OnPrimary,
                        )
                    }
                }
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SimSelectorButton(
    sims: List<SimCard>,
    selectedSubscriptionId: Int?,
    onSimSelected: (Int) -> Unit,
) {
    if (sims.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val activeLabel = activeSimNumberLabel(sims, selectedSubscriptionId)
    val hasMultipleSims = sims.size > 1

    Box {
        TextButton(
            onClick = { if (hasMultipleSims) expanded = true },
            enabled = true,
            modifier = Modifier
                .size(48.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            shape = CircleShape,
        ) {
            Text(
                text = activeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            sims.forEach { sim ->
                DropdownMenuItem(
                    text = { Text("${activeSimNumberLabel(listOf(sim), sim.subscriptionId)} · ${sim.displayName}") },
                    onClick = {
                        expanded = false
                        onSimSelected(sim.subscriptionId)
                    },
                )
            }
        }
    }
}

internal fun activeSimNumberLabel(sims: List<SimCard>, selectedSubscriptionId: Int?): String {
    val activeSim = sims.firstOrNull { it.subscriptionId == selectedSubscriptionId } ?: sims.firstOrNull()
    return activeSim?.let { (it.slotIndex + 1).toString() } ?: "SIM"
}
