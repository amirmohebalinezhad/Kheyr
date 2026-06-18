package com.kheyr.sms

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity used by the "Copy code" notification action. Activities (unlike background
 * broadcast receivers) are allowed to write to the clipboard on Android 10+, so the copy happens
 * here and the activity finishes immediately.
 */
class CopyCodeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = intent.getStringExtra(EXTRA_CODE)
        if (!code.isNullOrBlank()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.notification_code_clip_label), code))
            Toast.makeText(this, getString(R.string.notification_code_copied), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        const val EXTRA_CODE = "code"
    }
}
