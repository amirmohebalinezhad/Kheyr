package com.kheyr.sms.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainDrawerModelTest {
    @Test fun defaultDrawerMatchesPrdOrder() {
        assertEquals(
            listOf("All Messages", "Spam", "Archived", "Pinned", "Contacts", "Desktop Sync", "Settings", "Help & Feedback"),
            MainDrawerModel.defaultItems().map { it.title },
        )
    }
}
