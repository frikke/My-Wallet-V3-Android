package com.blockchain.componentlib.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val Blue600 = Color(0XFF0C6CF2)
internal val Blue400 = Color(0XFF619FF7)
internal val Blue000 = Color(0XFFECF5FE)

internal val Green600 = Color(0XFF00994C)
internal val Green400 = Color(0XFF17CE73)
internal val Green100 = Color(0XFFD1F0DB)

internal val Red600 = Color(0XFFCF1726)
internal val Red400 = Color(0XFFFF3344)
internal val Red100 = Color(0XFFFFD9D6)

internal val Orange600 = Color(0XFFD46A00)
internal val Orange400 = Color(0XFFFFA133)
internal val Orange100 = Color(0XFFFFECD6)

internal val Grey900 = Color(0XFF121D33)
internal val Grey800 = Color(0XFF353F52)
internal val Grey700 = Color(0XFF50596B)
internal val Grey600 = Color(0XFF677184)
internal val Grey500 = Color(0XFF828B9E)
internal val Grey400 = Color(0XFF98A1B2)
internal val Grey300 = Color(0XFFB1B8C7)
internal val Grey200 = Color(0XFFCCD2DE)
internal val Grey100 = Color(0XFFDFE3EB)
internal val Grey000 = Color(0XFFF0F2F7)

internal val Dark900 = Color(0XFF0E121B)
internal val Dark800 = Color(0XFF20242C)
internal val Dark700 = Color(0XFF2C3038)
internal val Dark600 = Color(0XFF3B3E46)
internal val Dark500 = Color(0XFF4D515B)
internal val Dark400 = Color(0XFF63676F)
internal val Dark300 = Color(0XFF797D84)
internal val Dark200 = Color(0XFF989BA1)
internal val Dark100 = Color(0XFFB8B9BD)
internal val Dark000 = Color(0XFFD2D4D6)

internal val TierGold = Color(0XFFF5B73D)
internal val TierSilver = Color(0XFFC2C9D6)

internal val Overlay800 = Dark800.copy(0.8f)
internal val Overlay600 = Dark600.copy(0.64f)
internal val Overlay400 = Dark400.copy(0.4f)

internal val White600 = Color.White.copy(0.6f)

fun getLightColors() = SemanticColors(
    title = Grey900,
    body = Grey800,
    overlay = Overlay600,
    muted = Grey400,
    dark = Grey300,
    medium = Grey100,
    light = Grey000,
    background = Color.White,
    primary = Blue600,
    success = Green600,
    warning = Orange600,
    error = Red600,

    isLight = true
)

fun getDarkColors() = SemanticColors(
    title = Color.White,
    body = Dark200,
    overlay = Overlay600,
    muted = Dark400,
    dark = Dark700,
    medium = Dark600,
    light = Dark800,
    background = Dark900,
    primary = Blue400,
    success = Green400,
    warning = Orange400,
    error = Red400,

    isLight = false
)

val LocalColors = staticCompositionLocalOf { getLightColors() }
