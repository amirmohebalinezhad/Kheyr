package com.kheyr.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.res.painterResource
import com.kheyr.sms.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kheyr.sms.util.CopyableSegment
import com.kheyr.sms.util.CopyableSegmentKind

@Composable
fun MessageCopyMenuDialog(
    body: String,
    segments: List<CopyableSegment>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onCall: (String) -> Unit = {},
    onSms: (String) -> Unit = {},
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
                    when (segment.kind) {
                        CopyableSegmentKind.Phone -> PhoneSegmentRow(
                            segment = segment,
                            onCopy = onCopy,
                            onCall = onCall,
                            onSms = onSms,
                        )
                        else -> TextButton(onClick = { onCopy(segment.normalized) }) {
                            Text("${segmentLabel(segment)}: ${segment.normalized}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun PhoneSegmentRow(
    segment: CopyableSegment,
    onCopy: (String) -> Unit,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(segment.normalized, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onCopy(segment.normalized) }) {
                Icon(painterResource(R.drawable.ic_content_copy), contentDescription = "کپی شماره")
            }
            IconButton(onClick = { onCall(segment.normalized) }) {
                Icon(Icons.Default.Call, contentDescription = "تماس")
            }
            IconButton(onClick = { onSms(segment.normalized) }) {
                Icon(Icons.Default.Send, contentDescription = "پیام")
            }
        }
    }
}

private fun segmentLabel(segment: CopyableSegment): String = when (segment.kind) {
    CopyableSegmentKind.Phone -> "کپی شماره"
    CopyableSegmentKind.Iban -> "کپی شبا"
    CopyableSegmentKind.CardNumber -> "کپی شماره کارت"
}
