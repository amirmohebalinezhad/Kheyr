package com.kheyr.sms.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette. Material color roles are mapped from these in [KheyrTheme]; screens should
 * prefer `MaterialTheme.colorScheme` so both themes stay in sync.
 */
object KheyrColors {
    // Brand
    val Primary = Color(0xFF0C7A6D)
    val PrimaryBright = Color(0xFF14B8A6)
    val PrimaryDeep = Color(0xFF085B52)
    val PrimaryInk = Color(0xFF042F2A)
    val Mint = Color(0xFF6FE7CF)
    val OnPrimary = Color(0xFFFFFFFF)

    // Accents used for category tiles, pinned badges and small moments of delight.
    val Amber = Color(0xFFC88A2E)
    val AmberSoft = Color(0xFFFBF0DC)
    val Coral = Color(0xFFC9675B)
    val CoralSoft = Color(0xFFFBE7E3)
    val Violet = Color(0xFF7A6BD6)
    val VioletSoft = Color(0xFFEEEAFC)
    val Blue = Color(0xFF3F87C4)
    val BlueSoft = Color(0xFFE6F1FA)

    // Conversation bubbles
    val OutgoingStart = Color(0xFF0C7F73)
    val OutgoingEnd = Color(0xFF06544C)
    val OutgoingText = Color(0xFFFFFFFF)
    val IncomingBubbleLight = Color(0xFFFFFFFF)
    val OnIncomingBubbleLight = Color(0xFF14201C)
    val IncomingBubbleDark = Color(0xFF17221E)
    val OnIncomingBubbleDark = Color(0xFFE7F1EC)
    val OutgoingBubbleLight = Color(0xFF0C7A6D)
    val OnOutgoingBubbleLight = Color(0xFFFFFFFF)
    val OutgoingBubbleDark = Color(0xFF0E8A7C)
    val OnOutgoingBubbleDark = Color(0xFFFFFFFF)

    // Legacy aliases kept for call sites that have not moved to color roles yet.
    val BackgroundLight = Color(0xFFF5F8F6)
    val BackgroundDark = Color(0xFF070C0B)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF0D1412)
    val SurfaceContainerLight = Color(0xFFEDF2EF)
    val SurfaceContainerDark = Color(0xFF131D1A)
    val SurfaceVariantLight = Color(0xFFE3EAE6)
    val SurfaceVariantDark = Color(0xFF24322D)

    val GlassSurfaceLight = Color(0xF2FFFFFF)
    val GlassSurfaceDark = Color(0xF2111816)
    val GlassContainerLight = Color(0xE8EDF2EF)
    val GlassContainerDark = Color(0xE816211D)

    val SearchFieldLight = Color(0xFFEDF2EF)
    val SearchFieldDark = Color(0xFF18231F)

    val UnreadBadge = Primary
}

object KheyrGradients {
    val Brand = Brush.linearGradient(listOf(Color(0xFF0D8478), Color(0xFF06544C)))
    val BrandDeep = Brush.linearGradient(listOf(Color(0xFF0B3B36), Color(0xFF0A5F57), Color(0xFF0C7A6D)))
    val Outgoing = Brush.linearGradient(listOf(KheyrColors.OutgoingStart, KheyrColors.OutgoingEnd))
    val MintGlow = Brush.radialGradient(
        colors = listOf(Color(0x336FE7CF), Color(0x006FE7CF)),
    )
    val WarmGlow = Brush.radialGradient(
        colors = listOf(Color(0x22C88A2E), Color(0x00C88A2E)),
    )
}
