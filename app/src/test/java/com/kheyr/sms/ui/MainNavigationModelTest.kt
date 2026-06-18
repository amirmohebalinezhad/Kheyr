package com.kheyr.sms.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationModelTest {
    @Test
    fun defaultTabsMatchTelegramLayout() {
        assertEquals(
            listOf("Chats", "Contacts", "Settings", "Profile"),
            MainNavigationModel.defaultTabs().map { it.title },
        )
    }

    @Test
    fun chatFoldersMatchSmsFolders() {
        assertEquals(
            listOf("All Messages", "Spam", "Archived", "Pinned"),
            MainNavigationModel.chatFolders().map { it.title },
        )
    }

    @Test
    fun overflowActionsIncludeDesktopSyncHelpCompose() {
        assertEquals(
            listOf("Desktop Sync", "Help & Feedback", "New message"),
            MainNavigationModel.overflowActions().map { it.title },
        )
    }

    @Test
    fun chatFolderMapsToThreadFolder() {
        assertEquals(ThreadFolder.Inbox, ChatFolder.All.toThreadFolder())
        assertEquals(ThreadFolder.Spam, ChatFolder.Spam.toThreadFolder())
        assertEquals(ThreadFolder.Archived, ChatFolder.Archived.toThreadFolder())
        assertEquals(ThreadFolder.Pinned, ChatFolder.Pinned.toThreadFolder())
    }
}
