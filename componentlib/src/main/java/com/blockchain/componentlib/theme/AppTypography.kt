package com.blockchain.componentlib.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.R

private val interMedium = FontFamily(Font(R.font.inter_medium, FontWeight.Normal))
private val interSemiBold = FontFamily(Font(R.font.inter_semi_bold, FontWeight.SemiBold))
private val interBold = FontFamily(Font(R.font.inter_bold, FontWeight.Bold))
private const val monoFontFeatures = "tnum, lnum, zero, ss01"
private const val slashedZeroFontFeatures = "zero, ss01"

data class AppTypography(
    val display: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 40.sp,
        lineHeight = 50.sp
    ),
    val title1: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    val title2: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    val title2Mono: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 24.sp,
        fontFeatureSettings = monoFontFeatures,
        lineHeight = 32.sp
    ),
    val title2SlashedZero: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 24.sp,
        fontFeatureSettings = slashedZeroFontFeatures,
        lineHeight = 32.sp
    ),
    val title3: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    val title4: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 24.sp,
        letterSpacing = 12.sp,
        lineHeight = 30.sp
    ),
    val subheading: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    val bodyMono: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 16.sp,
        fontFeatureSettings = monoFontFeatures,
        lineHeight = 24.sp
    ),
    val bodySlashedZero: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 16.sp,
        fontFeatureSettings = slashedZeroFontFeatures,
        lineHeight = 24.sp
    ),
    val body1: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    val body2: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    val paragraphMono: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 14.sp,
        fontFeatureSettings = monoFontFeatures,
        lineHeight = 20.sp
    ),
    val paragraph1: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val paragraph2: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val paragraph2SlashedZero: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 14.sp,
        fontFeatureSettings = slashedZeroFontFeatures,
        lineHeight = 20.sp
    ),
    val caption1: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        platformStyle = PlatformTextStyle(
            includeFontPadding = false
        )
    ),
    val caption2: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    val overline: TextStyle = TextStyle(
        fontFamily = interBold,
        fontSize = 12.sp,
        fontFeatureSettings = "ss01, zero",
        letterSpacing = 0.1.sp,
        lineHeight = 18.sp
    ),
    val micro1: TextStyle = TextStyle(
        fontFamily = interSemiBold,
        fontSize = 10.sp,
        fontFeatureSettings = "ss01, zero",
        letterSpacing = 0.1.sp,
        lineHeight = 15.sp
    ),
    val micro2: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 10.sp,
        lineHeight = 15.sp
    ),
    val micro2SlashedZero: TextStyle = TextStyle(
        fontFamily = interMedium,
        fontSize = 10.sp,
        fontFeatureSettings = slashedZeroFontFeatures,
        lineHeight = 15.sp
    )
)

internal val LocalTypography = staticCompositionLocalOf { AppTypography() }
