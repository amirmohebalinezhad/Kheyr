package com.kheyr.sms.ui

object MainNavigationModel {
    fun defaultTabs(): List<MainTab> = MainTab.entries.toList()

    fun chatFolders(): List<ChatFolder> = ChatFolder.entries.toList()

    fun overflowActions(): List<ChatsOverflowAction> = ChatsOverflowAction.entries.toList()
}

enum class MainTab(val title: String) {
    Chats("Chats"),
    Contacts("Contacts"),
    Settings("Settings"),
    Profile("Profile"),
}

enum class ChatFolder(val title: String) {
    All("All Messages"),
    Spam("Spam"),
    Archived("Archived"),
    Pinned("Pinned"),
}

enum class ChatsOverflowAction(val title: String) {
    DesktopSync("Desktop Sync"),
    HelpFeedback("Help & Feedback"),
    Compose("New message"),
}

fun ChatFolder.toThreadFolder(): ThreadFolder = when (this) {
    ChatFolder.All -> ThreadFolder.Inbox
    ChatFolder.Spam -> ThreadFolder.Spam
    ChatFolder.Archived -> ThreadFolder.Archived
    ChatFolder.Pinned -> ThreadFolder.Pinned
}
