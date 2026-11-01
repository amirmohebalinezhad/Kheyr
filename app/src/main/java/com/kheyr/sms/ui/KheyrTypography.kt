package com.kheyr.sms.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kheyr.sms.R

object KheyrTypography {
    private val Vazirmatn = FontFamily(
        Font(R.font.vazirmatn_regular, FontWeight.Normal),
        Font(R.font.vazirmatn_medium, FontWeight.Medium),
        Font(R.font.vazirmatn_bold, FontWeight.Bold),
    )

    val typography: Typography
        get() = Typography(
            displayLarge = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 60.sp,
                letterSpacing = (-1).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                letterSpacing = (-0.8).sp,
            ),
            displaySmall = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.6).sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.4).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 33.sp,
                letterSpacing = (-0.3).sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.2).sp,
            ),
            titleLarge = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.1).sp,
            ),
            titleMedium = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.1.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.2.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = Vazirmatn,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.4.sp,
            ),
        )
}
