package com.kheyr.sms.ui

import com.kheyr.sms.data.ThreadActionModel

data class ThreadLongPressMenuModel(val actions: List<ThreadActionModel>) {
    val hasActions: Boolean get() = actions.isNotEmpty()
}
