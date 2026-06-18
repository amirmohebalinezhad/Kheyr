package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kheyr.sms.util.CopyableSegment
import com.kheyr.sms.util.CopyableSegmentKind

@Composable
fun MessageCopyMenuDialog(
    body: String,
    segments: List<CopyableSegment>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کپی") },
        text = {
            Column {
                TextButton(onClick = { onCopy(body) }) {
                    Text("کپی متن کامل")
                }
                segments.forEach { segment ->
                    TextButton(onClick = { onCopy(segment.normalized) }) {
                        Text("${segmentLabel(segment)}: ${segment.normalized}")
                    }
                }
            }
        },
        confirmButton = {},
    )
}

private fun segmentLabel(segment: CopyableSegment): String = when (segment.kind) {
    CopyableSegmentKind.Phone -> "کپی شماره"
    CopyableSegmentKind.Iban -> "کپی شبا"
    CopyableSegmentKind.CardNumber -> "کپی شماره کارت"
}
