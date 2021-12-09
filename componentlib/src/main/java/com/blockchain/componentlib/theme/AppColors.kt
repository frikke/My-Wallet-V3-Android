package com.blockchain.componentlib.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Blue700 = Color(0XFF1656B9)
val Blue600 = Color(0XFF0C6CF2)
val Blue400 = Color(0XFF619FF7)
val Blue000 = Color(0XFFECF5FE)

val Green900 = Color(0XFF003319)
val Green700 = Color(0XFF006633)
val Green600 = Color(0XFF00994C)
val Green400 = Color(0XFF17CE73)
val Green100 = Color(0XFFD1F0DB)

val Red900 = Color(0XFF790606)
val Red700 = Color(0XFFA50D0D)
val Red600 = Color(0XFFCF1726)
val Red400 = Color(0XFFFF3344)
val Red100 = Color(0XFFFFD9D6)
val Red000 = Color(0XFFFFECEB)

val Orange600 = Color(0XFFD46A00)
val Orange400 = Color(0XFFFFA133)
val Orange100 = Color(0XFFFFECD6)

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
