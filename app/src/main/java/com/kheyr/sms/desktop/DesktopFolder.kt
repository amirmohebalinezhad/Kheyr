package com.kheyr.sms.desktop

enum class DesktopFolder { AllMessages, Spam, Archived, Pinned }
object DesktopFolderLabels {
    fun label(folder: DesktopFolder): String = when (folder) {
        DesktopFolder.AllMessages -> "All Messages"
        DesktopFolder.Spam -> "Spam"
        DesktopFolder.Archived -> "Archived"
        DesktopFolder.Pinned -> "Pinned"
    }
}
