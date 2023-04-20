package com.blockchain.componentlib.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val START_TRADING = Color(0XFFFF0095)
val END_TRADING = Color(0XFF7C33B9)
val START_DEFI = Color(0XFF7137BB)
val END_DEFI = Color(0XFF2960D0)

val BackgroundMuted = Color(0XFFF1F2F7)

val GOOGLE_PAY_BUTTON_BORDER = Color(0x7FFFFFFF)
val GOOGLE_PAY_BUTTON_DIVIDER = Color(0xFF3C4043)

val Blue700 = Color(0XFF1656B9)
val Blue600 = Color(0XFF0C6CF2)
val Blue400 = Color(0XFF619FF7)
val Blue200 = Color(0XFFBBDBFC)
val Blue000 = Color(0XFFECF5FE)

val Purple0000 = Color(0XFF5322E5)

val Green900 = Color(0XFF003319)
val Green800 = Color(0XFF0C8868)
val Green700 = Color(0XFF00B083)
val Green600 = Color(0XFF06D6A0)
val Green400 = Color(0XFF17CE73)
val Green300 = Color(0XFF69ECCA)
val Green100 = Color(0XFFD1F0DB)
val Green000 = Color(0xFFEDFFFA)

val Red900 = Color(0XFF790606)
val Red700 = Color(0XFFA50D0D)
val Red600 = Color(0XFFCF1726)
val Red400 = Color(0XFFFF3344)
val Red100 = Color(0XFFFFD9D6)
val Red000 = Color(0XFFFFECEB)

val Pink700 = Color(0XFFDE0082)
val Pink600 = Color(0XFFF00699)

val Orange600 = Color(0XFFD46A00)
val Orange500 = Color(0XFFFFA133)
val Orange400 = Color(0XFFFFA133)
val Orange100 = Color(0XFFFFECD6)
val Orange000 = Color(0XFFFFF6EB)

val Grey900 = Color(0XFF121D33)
val Grey800 = Color(0XFF353F52)
val Grey700 = Color(0XFF50596B)
val Grey600 = Color(0XFF677184)
val Grey500 = Color(0XFF828B9E)
val Grey400 = Color(0XFF98A1B2)
val Grey300 = Color(0XFFB1B8C7)
val Grey200 = Color(0XFFCCD2DE)
val Grey100 = Color(0XFFDFE3EB)
val Grey000 = Color(0XFFF0F2F7)

val White = Color(0xFFFFFFFF)

val Dark900 = Color(0XFF0E121B)
val Dark800 = Color(0XFF20242C)
val Dark700 = Color(0XFF2C3038)
val Dark600 = Color(0XFF3B3E46)
val Dark500 = Color(0XFF4D515B)
val Dark400 = Color(0XFF63676F)
val Dark300 = Color(0XFF797D84)
val Dark200 = Color(0XFF989BA1)
val Dark100 = Color(0XFFB8B9BD)
val Dark000 = Color(0XFFD2D4D6)

val TierGold = Color(0XFFF5B73D)
val TierSilver = Color(0XFFC2C9D6)

val Overlay800 = Dark800.copy(0.8f)
val Overlay600 = Dark600.copy(0.64f)
val Overlay400 = Dark400.copy(0.4f)

val White600 = Color.White.copy(0.6f)
val White800 = Color.White.copy(0.8f)

val UltraLight = Color(0XFFFAFBFF)
val CowboysDark = Color(0XFF07080D)

val defLightColors = SemanticColors(
    title = Grey900,
    body = Grey700,
    overlay = Overlay600,
    muted = Grey700,
    dark = Grey300,
    medium = Grey100,
    light = Grey000,
    background = Color.White,
    backgroundMuted = BackgroundMuted,
    primary = Blue600,
    primaryMuted = Blue400,
    success = Green700,
    successMuted = Green300,
    warning = Orange600,
    warningMuted = Orange400,
    error = Red600,
    errorMuted = Red400,
    isLight = true
)

val defDarkColors = SemanticColors(
    title = Color.White,
    body = Dark200,
    overlay = Overlay600,
    muted = Dark400,
    dark = Dark700,
    medium = Dark600,
    light = Dark800,
    background = Dark900,
    backgroundMuted = Dark900,
    primary = Blue400,
    primaryMuted = Blue400, // todo unknown atm
    success = Green400,
    successMuted = Green400, // todo unknown atm
    warning = Orange400,
    warningMuted = Orange400, // todo unknown atm
    error = Red400,
    errorMuted = Red400, // todo unknown atm
    isLight = false
)

val LocalLightColors = compositionLocalOf { defLightColors }
val LocalDarkColors = compositionLocalOf { defDarkColors }
