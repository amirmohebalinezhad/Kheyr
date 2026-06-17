package com.kheyr.sms.ui

object MainDrawerModel {
    fun defaultItems(): List<DrawerItem> = listOf(
        DrawerItem.AllMessages,
        DrawerItem.Spam,
        DrawerItem.Archived,
        DrawerItem.Pinned,
        DrawerItem.Contacts,
        DrawerItem.DesktopSync,
        DrawerItem.Settings,
        DrawerItem.HelpFeedback,
    )
}

enum class DrawerItem(val title: String) {
    AllMessages("All Messages"),
    Spam("Spam"),
    Archived("Archived"),
    Pinned("Pinned"),
    Contacts("Contacts"),
    DesktopSync("Desktop Sync"),
    Settings("Settings"),
    HelpFeedback("Help & Feedback"),
}
