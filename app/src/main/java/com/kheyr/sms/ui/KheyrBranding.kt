package com.kheyr.sms.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kheyr.sms.R

@Composable
fun KheyrMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String = "Kheyr",
) {
    Image(
        painter = painterResource(R.drawable.ic_kheyr_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

@Composable
fun KheyrBrandHeader(
    modifier: Modifier = Modifier,
    title: String = "Kheyr",
    subtitle: String? = null,
    iconSize: Dp = 40.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KheyrMark(size = iconSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
