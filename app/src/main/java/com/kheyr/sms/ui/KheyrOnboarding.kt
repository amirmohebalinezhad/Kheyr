package com.kheyr.sms.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kheyr.sms.api.ApiConfig
import com.kheyr.sms.onboarding.OnboardingCopy
import com.kheyr.sms.onboarding.OnboardingGateState

private val OnboardingInk = Color(0xFF04231F)
private val OnboardingMint = Color(0xFF8BF0DA)

@Composable
fun KheyrOnboardingFlow(
    step: Int,
    gate: OnboardingGateState,
    signedIn: Boolean,
    otpPhone: String,
    otpCode: String,
    appDarkTheme: Boolean,
    onStepChange: (Int) -> Unit,
    onOtpPhoneChange: (String) -> Unit,
    onOtpCodeChange: (String) -> Unit,
    onRequestDefault: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkipSync: () -> Unit,
    onEnableSync: () -> Unit,
    onVerifyOtp: () -> Unit,
    onRequestOtp: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = OnboardingCopy.steps
    val scrollState = rememberScrollState()
    UseLightSystemBarsWhileVisible(restoreDark = appDarkTheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(KheyrGradients.BrandDeep)
                val wide = size.minDimension * 1.15f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3D6FE7CF), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.02f),
                        radius = wide,
                    ),
                    radius = wide,
                    center = Offset(size.width * 0.88f, size.height * 0.02f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x24C88A2E), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.72f),
                        radius = wide * 0.8f,
                    ),
                    radius = wide * 0.8f,
                    center = Offset(size.width * 0.05f, size.height * 0.72f),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    KheyrMark(size = 34.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Kheyr",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Your messages, beautifully quiet",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${step + 1} / ${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(steps.size) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= step) Color.White else Color.White.copy(alpha = 0.22f),
                            ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        ((fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 10 }) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 12 }))
                            .using(SizeTransform(clip = false))
                    },
                    label = "onboardingStep",
                ) { currentStep ->
                    val copy = steps.getOrElse(currentStep) { steps.last() }
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            text = copy.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                            color = OnboardingMint,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = copy.headline,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = copy.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                        GlassInfoCard(text = copy.detail)
                        if (currentStep == 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OnboardingFeatureChip(KheyrIcons.Shield, "Spam shield")
                                OnboardingFeatureChip(Icons.Default.Lock, "Encrypted")
                                OnboardingFeatureChip(KheyrIcons.Sim, "Dual SIM")
                            }
                        }
                        when (currentStep) {
                            1 -> OnboardingStatusCard(
                                complete = gate.isDefaultSmsApp,
                                completeLabel = "Kheyr is your default SMS app",
                                pendingLabel = "Kheyr is not the default SMS app yet",
                            )
                            2 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OnboardingPermissionRow("SMS messages", gate.smsPermissionGranted)
                                OnboardingPermissionRow("Contacts", gate.contactsPermissionGranted)
                                OnboardingPermissionRow("Notifications", gate.notificationPermissionGranted)
                            }
                            3 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OnboardingField(
                                    value = otpPhone,
                                    onValueChange = onOtpPhoneChange,
                                    label = "Phone number",
                                    placeholder = "+98 912 000 0000",
                                    keyboardType = KeyboardType.Phone,
                                )
                                OnboardingField(
                                    value = otpCode,
                                    onValueChange = onOtpCodeChange,
                                    label = "Verification code",
                                    placeholder = "6-digit code",
                                    keyboardType = KeyboardType.NumberPassword,
                                )
                                Text(
                                    text = "Server: ${ApiConfig.baseUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                )
                            }
                            4 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OnboardingPermissionRow("Import existing messages", complete = true)
                                OnboardingPermissionRow("Download spam rules", complete = true)
                                Text(
                                    text = "Both run automatically in the background once you open your inbox.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC0A3B35)),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (step in 1..3) {
                        TextButton(
                            onClick = { onStepChange(step - 1) },
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("Back", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    OnboardingActions(
                        step = step,
                        gate = gate,
                        signedIn = signedIn,
                        otpCode = otpCode,
                        onStepChange = onStepChange,
                        onRequestDefault = onRequestDefault,
                        onRequestPermissions = onRequestPermissions,
                        onOpenSettings = onOpenSettings,
                        onSkipSync = onSkipSync,
                        onEnableSync = onEnableSync,
                        onVerifyOtp = onVerifyOtp,
                        onRequestOtp = onRequestOtp,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    step: Int,
    gate: OnboardingGateState,
    signedIn: Boolean,
    otpCode: String,
    onStepChange: (Int) -> Unit,
    onRequestDefault: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkipSync: () -> Unit,
    onEnableSync: () -> Unit,
    onVerifyOtp: () -> Unit,
    onRequestOtp: () -> Unit,
    onFinish: () -> Unit,
) {
    when (step) {
        0 -> OnboardingPrimary(label = "Get started", onClick = { onStepChange(1) })
        1 -> {
            if (!gate.isDefaultSmsApp) {
                OnboardingSecondary(label = "Make Kheyr the default", onClick = onRequestDefault)
            }
            OnboardingPrimary(
                label = "Continue",
                onClick = { onStepChange(2) },
                enabled = gate.isDefaultSmsApp,
            )
        }
        2 -> {
            if (gate.missingRequirements.isNotEmpty()) {
                OnboardingSecondary(label = "Grant permissions", onClick = onRequestPermissions)
                OnboardingTextAction(label = "Open system settings", onClick = onOpenSettings)
            }
            OnboardingPrimary(
                label = "Continue",
                onClick = { onStepChange(3) },
                enabled = gate.smsPermissionGranted,
            )
        }
        3 -> {
            OnboardingSecondary(label = "Send verification code", onClick = onRequestOtp)
            if (otpCode.isNotBlank()) {
                OnboardingPrimary(label = "Verify code", onClick = onVerifyOtp)
            }
            OnboardingPrimary(
                label = "Enable encrypted sync",
                onClick = onEnableSync,
                enabled = signedIn,
            )
            if (!signedIn) {
                Text(
                    text = "Verify your phone number first — sync needs an account to upload to.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            OnboardingTextAction(label = "Skip for now", onClick = onSkipSync)
        }
        else -> OnboardingPrimary(label = "Open my inbox", onClick = onFinish, icon = Icons.Default.Check)
    }
}

@Composable
private fun OnboardingPrimary(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = OnboardingInk,
            disabledContainerColor = Color.White.copy(alpha = 0.16f),
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OnboardingSecondary(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun OnboardingTextAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GlassInfoCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(
            imageVector = KheyrIcons.Shield,
            contentDescription = null,
            tint = OnboardingMint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun OnboardingFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = OnboardingMint, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun OnboardingStatusCard(
    complete: Boolean,
    completeLabel: String,
    pendingLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        StatusGlyph(complete = complete)
        Text(
            text = if (complete) completeLabel else pendingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun OnboardingPermissionRow(label: String, complete: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        StatusGlyph(complete = complete)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
        Text(
            text = if (complete) "Granted" else "Needed",
            style = MaterialTheme.typography.labelSmall,
            color = if (complete) OnboardingMint else Color.White.copy(alpha = 0.55f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusGlyph(complete: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (complete) OnboardingMint else Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (complete) Icons.Default.Check else Icons.Default.Info,
            contentDescription = null,
            tint = if (complete) OnboardingInk else Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun OnboardingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            cursorBrush = SolidColor(OnboardingMint),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.35f),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun UseLightSystemBarsWhileVisible(restoreDark: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, restoreDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !restoreDark
            controller?.isAppearanceLightNavigationBars = !restoreDark
        }
    }
}
