package com.kheyr.sms.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun KheyrBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    GlassSurface {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            MainNavigationModel.defaultTabs().forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = tabIcon(tab, selectedTab == tab),
                            contentDescription = tab.title,
                        )
                    },
                    label = { Text(tab.title) },
                )
            }
        }
    }
}

private fun tabIcon(tab: MainTab, @Suppress("UNUSED_PARAMETER") selected: Boolean): ImageVector = when (tab) {
    MainTab.Chats -> Icons.Default.Email
    MainTab.Contacts -> Icons.Default.Person
    MainTab.Settings -> Icons.Default.Settings
    MainTab.Profile -> Icons.Default.Info
}
