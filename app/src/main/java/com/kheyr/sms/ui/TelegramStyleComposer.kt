package com.kheyr.sms.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.kheyr.sms.telephony.SimCard
import kotlin.math.ceil

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
    val scheme = MaterialTheme.colorScheme
    val dark = LocalKheyrDarkTheme.current
    val appLayoutDirection = LocalLayoutDirection.current
    val draftLayoutDirection = MessageTextDirection.resolveLayoutDirection(body, appLayoutDirection)
    val draftTextAlign = if (draftLayoutDirection == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
    val canSend = body.isNotBlank() && !sending
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "sendScale",
    )
    val sendColor by animateColorAsState(
        targetValue = if (canSend) scheme.primary else scheme.surfaceContainerHighest,
        label = "sendColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = if (dark) 0.55f else 0.65f)),
        shadowElevation = if (dark) 0.dp else 9.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SimSelectorButton(
                    sims = sims,
                    selectedSubscriptionId = selectedSubscriptionId,
                    onSimSelected = onSimSelected,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f))
                        .padding(horizontal = 15.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (body.isEmpty()) {
                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides draftLayoutDirection) {
                        BasicTextField(
                            value = body,
                            onValueChange = onBodyChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = scheme.onSurface,
                                textAlign = draftTextAlign,
                                lineHeight = 22.sp,
                            ),
                            cursorBrush = SolidColor(scheme.primary),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        )
                    }
                }
                Surface(
                    onClick = onSend,
                    enabled = canSend,
                    shape = CircleShape,
                    color = sendColor,
                    contentColor = if (canSend) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(45.dp)
                        .graphicsLayer {
                            scaleX = sendScale
                            scaleY = sendScale
                        },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = scheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = KheyrIcons.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            if (error != null) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, top = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = error.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error,
                    )
                }
            } else if (body.length > 160) {
                Text(
                    text = "${ceil(body.length / 160.0).toInt()} SMS · ${body.length} characters",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
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
    val scheme = MaterialTheme.colorScheme

    Box {
        Surface(
            onClick = { if (hasMultipleSims) expanded = true },
            enabled = hasMultipleSims,
            shape = RoundedCornerShape(15.dp),
            color = scheme.primary.copy(alpha = 0.10f),
            contentColor = scheme.primary,
            modifier = Modifier.size(44.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = activeLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIM",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = scheme.primary.copy(alpha = 0.8f),
                    )
                    if (hasMultipleSims) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Choose SIM",
                            modifier = Modifier.size(9.dp),
                            tint = scheme.primary.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = scheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            properties = PopupProperties(focusable = true),
        ) {
            sims.forEach { sim ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                "SIM ${activeSimNumberLabel(listOf(sim), sim.subscriptionId)}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                sim.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(KheyrIcons.Sim, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
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
