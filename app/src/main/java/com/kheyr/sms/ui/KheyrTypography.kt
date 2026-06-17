package com.kheyr.sms.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kheyr.sms.R

object KheyrTypography {
    private val Vazirmatn = FontFamily(
        Font(R.font.vazirmatn_regular, FontWeight.Normal),
        Font(R.font.vazirmatn_medium, FontWeight.Medium),
        Font(R.font.vazirmatn_bold, FontWeight.Bold),
    )

    val typography: Typography
        get() {
            val base = Typography()
            fun TextStyle.withVazirmatn() = copy(fontFamily = Vazirmatn)
            return Typography(
                displayLarge = base.displayLarge.withVazirmatn(),
                displayMedium = base.displayMedium.withVazirmatn(),
                displaySmall = base.displaySmall.withVazirmatn(),
                headlineLarge = base.headlineLarge.withVazirmatn(),
                headlineMedium = base.headlineMedium.withVazirmatn(),
                headlineSmall = base.headlineSmall.withVazirmatn(),
                titleLarge = base.titleLarge.withVazirmatn(),
                titleMedium = base.titleMedium.withVazirmatn(),
                titleSmall = base.titleSmall.withVazirmatn(),
                bodyLarge = base.bodyLarge.withVazirmatn(),
                bodyMedium = base.bodyMedium.withVazirmatn(),
                bodySmall = base.bodySmall.withVazirmatn(),
                labelLarge = base.labelLarge.withVazirmatn(),
                labelMedium = base.labelMedium.withVazirmatn(),
                labelSmall = base.labelSmall.withVazirmatn(),
            )
        }
}
