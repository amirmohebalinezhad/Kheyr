package com.kheyr.sms.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Floating capsule navigation. A soft indicator glides between tabs instead of Material's
 * rectangular highlight, which keeps the inbox feeling airy and app-like rather than toolbar-like.
 */
@Composable
fun KheyrBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = MainNavigationModel.defaultTabs()
    val scheme = MaterialTheme.colorScheme
    val dark = LocalKheyrDarkTheme.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(60.dp),
        shape = RoundedCornerShape(26.dp),
        color = if (dark) KheyrColors.GlassSurfaceDark else KheyrColors.GlassSurfaceLight,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = if (dark) 0.5f else 0.7f)),
        shadowElevation = if (dark) 0.dp else 14.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = (maxWidth - 8.dp) / tabs.size
            val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val targetOffset = if (rtl) {
                maxWidth - itemWidth * (selectedIndex + 1) - 4.dp
            } else {
                itemWidth * selectedIndex + 4.dp
            }
            val offset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "navIndicatorOffset",
            )
            Box(
                Modifier
                    .offset { IntOffset(x = offset.roundToPx(), y = 0) }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.primary.copy(alpha = if (dark) 0.17f else 0.10f)),
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        label = "navContent",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                        label = "navScale",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            onClick = { onTabSelected(tab) },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(vertical = 1.dp, horizontal = 2.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = tabIcon(tab),
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                )
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.Chats -> KheyrIcons.Chat
    MainTab.Contacts -> KheyrIcons.Contacts
    MainTab.Settings -> Icons.Default.Settings
    MainTab.Profile -> KheyrIcons.Profile
}
