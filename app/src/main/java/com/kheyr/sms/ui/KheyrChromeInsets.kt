package com.kheyr.sms.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TopAppBarHeight = 64.dp
private val NavigationBarHeight = 80.dp

object KheyrChromeInsets {
    @Composable
    fun shellTop(): Dp {
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        return statusBar + TopAppBarHeight
    }

    @Composable
    fun bottomNav(): Dp {
        val systemNav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        return systemNav + NavigationBarHeight
    }
}
